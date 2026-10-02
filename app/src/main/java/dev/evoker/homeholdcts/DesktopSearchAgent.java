// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

import android.content.AttributionSource;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** ADB shell entry point, loaded from this signed APK; never runs as an app service. */
public final class DesktopSearchAgent {
    private static final String SELF = "dev.evoker.homeholdcts";
    private static final String DESC = "android.app.IActivityController";
    private final ExecutorService launches = Executors.newSingleThreadExecutor();
    private final DesktopSearchRule rule = new DesktopSearchRule();
    private final int appUid;
    private final int launcherUid;
    private final Method callingUid;
    private final Object taskManager;
    private final Method setController;
    private final Object controller;
    private final IBinder controllerBinder;
    private Object desktopProvider;
    private volatile boolean enabled;
    private volatile boolean quit;
    private String controllerIdentity = "";
    private volatile String error = "";
    private volatile int redirects;
    private volatile long lastTrigger;
    private IBinder providerBinder;
    private IBinder wakeProviderBinder;
    private final IBinder wakeProviderToken = new Binder();
    private volatile SingleDirectLogdSession wakeSession;
    private volatile boolean wakeActive;
    private volatile String wakeError = "";
    private volatile int wakeGeneration;
    private final IBinder providerToken = new Binder();
    private final Object activityManager;
    private final Class<?> managerInterface;

    private DesktopSearchAgent() throws Exception {
        appUid = packageUid(SELF);
        launcherUid = packageUid("com.android.launcher");
        callingUid = Intent.class.getMethod("getCallingUid");
        taskManager = Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
        Class<?> ctl = Class.forName(DESC);
        setController = Class.forName("android.app.IActivityTaskManager")
                .getMethod("setActivityController", ctl, boolean.class);
        Binder callback = new Binder() {
            @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                if (code == INTERFACE_TRANSACTION) { reply.writeString(DESC); return true; }
                if (code < 1 || code > 6) return super.onTransact(code, data, reply, flags);
                data.enforceInterface(DESC);
                int result = 1;
                if (code == 1) {
                    Intent intent = data.readInt() != 0 ? Intent.CREATOR.createFromParcel(data) : null;
                    String pkg = data.readString();
                    if (enabled && isDockRequest(intent, pkg)) {
                        result = 0;
                        if (rule.accept(SystemClock.elapsedRealtime())) {
                            lastTrigger = SystemClock.elapsedRealtime();
                            System.out.println("BLOCK desktop search uid=" + launcherUid + " t=" + lastTrigger);
                            launches.execute(DesktopSearchAgent.this::launchGoogle);
                        }
                    }
                } else if (code == 4 || code == 5) result = 0;
                else if (code == 6) result = -1;
                // Resumes are allowed; crash/ANR callbacks retain normal system handling.
                reply.writeNoException(); reply.writeInt(result); return true;
            }
        };
        controllerBinder = callback;
        controller = Proxy.newProxyInstance(ctl.getClassLoader(), new Class<?>[]{ctl}, (o,m,a) -> {
            if (m.getName().equals("asBinder")) return callback;
            throw new UnsupportedOperationException(m.getName());
        });
        activityManager = Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null);
        managerInterface = Class.forName("android.app.IActivityManager");
    }

    private boolean isDockRequest(Intent intent, String pkg) {
        if (intent == null || intent.getComponent() == null) return false;
        try {
            return DesktopSearchRule.matches((int) callingUid.invoke(intent), launcherUid, pkg,
                    intent.getComponent().getClassName(), intent.getAction(), intent.getDataString());
        } catch (ReflectiveOperationException e) {
            error = "Cannot read ColorOS caller UID: " + e;
            return false;
        }
    }

    private void checkCaller() {
        int uid = Binder.getCallingUid();
        if (uid != appUid && uid != 2000) throw new SecurityException("ColorGoogle or ADB required");
    }

    private synchronized Bundle status() {
        if (enabled) {
            try {
                if (!controllerIdentity.equals(readControllerIdentity())) {
                    enabled = false;
                    error = "Another debugging monitor replaced desktop search; disable that monitor before enabling again";
                }
            } catch (Exception e) { error = "Cannot verify controller: " + e; }
        }
        Bundle b = new Bundle();
        b.putInt("version", BuildConfig.VERSION_CODE);
        b.putInt("pid", android.os.Process.myPid());
        b.putBoolean("enabled", enabled);
        b.putString("error", error);
        b.putBoolean("wakeActive", wakeActive);
        b.putString("wakeError", wakeError);
        b.putInt("redirects", redirects);
        b.putLong("lastTrigger", lastTrigger);
        return b;
    }

    private synchronized void enable(boolean value) throws Exception {
        if (value == enabled) return;
        // Never synchronously launch another activity from activityStarting: system holds its lock.
        String current = readControllerIdentity();
        if (value && !current.isEmpty()) {
            Bundle dead = callDesktopProvider("dead_controller", null);
            if (!current.equals(dead.getString("identity", "")))
                throw new IllegalStateException("Another activity monitor is already running");
            System.out.println("REPLACE_DEAD_OWN_CONTROLLER " + current);
        }
        if (!value && !current.equals(controllerIdentity)) {
            enabled = false;
            error = "Controller was replaced; leaving the other monitor untouched";
            return;
        }
        if (value) {
            closeOldSearchTasks();
            String latest = readControllerIdentity();
            if (!latest.isEmpty() && !latest.equals(current))
                throw new IllegalStateException("Activity monitor changed during activation");
        }
        setController.invoke(taskManager, value ? controller : null, false);
        enabled = value;
        controllerIdentity = value ? readControllerIdentity() : "";
        Bundle record = new Bundle();
        record.putBinder("controller", value ? controllerBinder : null);
        record.putString("identity", controllerIdentity);
        callDesktopProvider("remember_controller", record);
        error = "";
        System.out.println("CONTROLLER enabled=" + enabled);
    }

    private final IDesktopSearchAgent.Stub rpc = new IDesktopSearchAgent.Stub() {
        @Override public Bundle getStatus() { checkCaller(); return status(); }
        @Override public Bundle setEnabled(boolean value) {
            checkCaller();
            try { enable(value); } catch (Exception e) { error = "Controller change failed: " + e; }
            return status();
        }
        @Override public void setWakeListener(IAdbWakeListener listener) {
            checkCaller();
            replaceWakeSession(listener);
        }
        @Override public void shutdown() {
            checkCaller();
            quit = true;
        }
    };

    private Bundle attachProvider(String authority, IBinder token) throws Exception {
        Object holder = managerInterface.getMethod("getContentProviderExternal", String.class,
                int.class, IBinder.class, String.class).invoke(activityManager,
                authority, 0, token, "ColorGoogle ADB agent");
        if (holder == null) throw new IllegalStateException("Bridge unavailable: " + authority);
        Object provider = holder.getClass().getField("provider").get(holder);
        if (authority.equals(DesktopSearchBridge.AUTHORITY)) {
            providerBinder = ((IInterface) provider).asBinder(); desktopProvider = provider;
        }
        else wakeProviderBinder = ((IInterface) provider).asBinder();
        ((IInterface) provider).asBinder().linkToDeath(() -> {
            System.err.println("BRIDGE_DIED " + authority);
            quit = true;
        }, 0);
        Bundle extras = new Bundle(); extras.putBinder("agent", rpc);
        AttributionSource source = new AttributionSource.Builder(2000).setPackageName("com.android.shell").build();
        Bundle result = (Bundle) Class.forName("android.content.IContentProvider").getMethod("call",
                AttributionSource.class, String.class, String.class, String.class, Bundle.class)
                .invoke(provider, source, authority, "attach", null, extras);
        if (result == null) throw new IllegalStateException("No bridge response");
        return result;
    }

    private Bundle callDesktopProvider(String method, Bundle extras) throws Exception {
        AttributionSource source = new AttributionSource.Builder(2000).setPackageName("com.android.shell").build();
        return (Bundle) Class.forName("android.content.IContentProvider").getMethod("call",
                AttributionSource.class, String.class, String.class, String.class, Bundle.class)
                .invoke(desktopProvider, source, DesktopSearchBridge.AUTHORITY, method, null, extras);
    }

    private void attach() throws Exception {
        Bundle result = attachProvider(DesktopSearchBridge.AUTHORITY, providerToken);
        enable(result.getBoolean("enabled", false));
        attachProvider(WakeLogBridge.AUTHORITY, wakeProviderToken);
        System.out.println("ATTACHED appUid=" + appUid + " launcherUid=" + launcherUid);
    }

    private void releaseProvider(String authority, IBinder token) throws Exception {
        managerInterface.getMethod("removeContentProviderExternalAsUser", String.class,
                IBinder.class, int.class).invoke(activityManager, authority, token, 0);
    }

    private synchronized void replaceWakeSession(IAdbWakeListener sink) {
        int generation = ++wakeGeneration;
        if (wakeSession != null) wakeSession.close();
        wakeSession = null;
        wakeActive = false;
        wakeError = "";
        if (sink == null) return;
        final long started = SystemClock.elapsedRealtime();
        wakeSession = new SingleDirectLogdSession(new SingleDirectLogdSession.Listener() {
            private boolean current() { return wakeGeneration == generation && !quit; }
            @Override public void onActive() {
                if (!current()) return;
                try {
                    wakeActive = true;
                    sink.onConnected();
                    System.out.println("WAKE_LOGS external UID verified");
                } catch (RemoteException e) { onAllLanesLost("Wake listener disconnected"); }
            }
            @Override public void onLine(String line) {
                long now = SystemClock.elapsedRealtime();
                if (!current() || now - started < 700 || !TriggerClassifier.isCandidateLine(line)) return;
                try {
                    sink.onLine(line, now);
                    System.out.println("WAKE_EVENT " + line);
                } catch (RemoteException e) { onAllLanesLost("Wake listener disconnected"); }
            }
            @Override public void onAllLanesLost(String reason) {
                if (!current()) return;
                wakeActive = false;
                wakeError = reason;
                try { sink.onDisconnected(reason); } catch (RemoteException ignored) { }
                System.err.println("WAKE_LOGS_LOST " + reason);
            }
        });
        wakeSession.start();
    }

    private void closeOldSearchTasks() throws Exception {
        Class<?> api = Class.forName("android.app.IActivityTaskManager");
        // Close only the old ColorOS search page when opting in. Preserve its package/data
        // and other apps' tasks. A pre-existing OEM task can be resurfaced before START_ABORTED.
        java.util.List<?> tasks = (java.util.List<?>) api.getMethod("getTasks", int.class,
                boolean.class, boolean.class, int.class).invoke(taskManager, 100, false, false, 0);
        for (Object item : tasks) {
            android.app.ActivityManager.RunningTaskInfo task = (android.app.ActivityManager.RunningTaskInfo) item;
            int userId = item.getClass().getField("userId").getInt(item);
            if (userId == 0 && task.baseActivity != null
                    && DesktopSearchRule.TARGET.equals(task.baseActivity.getPackageName())
                    && DesktopSearchRule.COMPONENT.equals(task.baseActivity.getClassName())) {
                boolean removed = (boolean) api.getMethod("removeTask", int.class).invoke(taskManager, task.taskId);
                if (!removed) throw new IllegalStateException("Could not close previous system search page");
                System.out.println("CLOSED_OLD_SEARCH_TASK " + task.taskId);
            }
        }
    }

    private static String readControllerIdentity() throws Exception {
        String dump = command("/system/bin/sh", "-c",
                "dumpsys activity processes | sed -n '/mController=/p; /mGoingToSleepWakeLock=/p'");
        if (!dump.contains("mGoingToSleepWakeLock=")) throw new IllegalStateException("Cannot inspect activity monitor ownership");
        for (String line : dump.split("\\n")) {
            if (line.trim().startsWith("mController=")) return line.trim();
        }
        return "";
    }

    private void launchGoogle() {
        if (!enabled || quit) return;
        try {
            String output = command("/system/bin/am", "start", "-W", "--user", "0", "-a",
                    "android.search.action.GLOBAL_SEARCH", "-p", "com.google.android.googlequicksearchbox");
            if (!output.contains("Status: ok")) throw new IllegalStateException(output);
            redirects++;
            error = "";
            System.out.println("GOOGLE request=" + redirects + " " + output.replace('\n', ' '));
        } catch (Exception e) {
            error = "Google launch failed: " + e;
            System.err.println(error);
        }
    }

    private static String command(String... args) throws Exception {
        java.lang.Process p = new ProcessBuilder(args).redirectErrorStream(true).start();
        if (!p.waitFor(8, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new IllegalStateException("Command timed out"); }
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.InputStream input = p.getInputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
        }
        String out = bytes.toString(StandardCharsets.UTF_8.name());
        if (p.exitValue() != 0) throw new IllegalStateException(out);
        return out;
    }

    private static int packageUid(String pkg) throws Exception {
        String output = command("/system/bin/cmd", "package", "list", "packages", "-U", "--user", "0", pkg);
        for (String line : output.split("\\n")) {
            String prefix = "package:" + pkg + " uid:";
            if (line.startsWith(prefix)) return Integer.parseInt(line.substring(prefix.length()).trim());
        }
        throw new IllegalStateException("Package missing: " + pkg);
    }

    public static void main(String[] args) {
        int exitCode = 0;
        try {
            runAgent();
        } catch (Throwable failure) {
            failure.printStackTrace();
            exitCode = 1;
        }
        // app_process Binder threads must not retain the file lock after main fails.
        System.exit(exitCode);
    }

    private static void runAgent() throws Exception {
        if (android.os.Process.myUid() != 2000) throw new SecurityException("Start using ADB");
        android.os.Looper.prepareMainLooper();
        try (RandomAccessFile file = new RandomAccessFile("/data/local/tmp/colorgoogle-search.lock", "rw");
             FileLock lock = file.getChannel().tryLock()) {
            if (lock == null) throw new IllegalStateException("Agent already running");
            DesktopSearchAgent agent = new DesktopSearchAgent();
            try {
                agent.attach();
                while (!agent.quit) Thread.sleep(1000);
            } finally {
                // Each release is independent: a dead provider must not prevent the
                // controller, worker threads and process lock from being released.
                try { agent.replaceWakeSession(null); } catch (Exception e) { e.printStackTrace(); }
                try { agent.enable(false); } catch (Exception e) { e.printStackTrace(); }
                agent.launches.shutdownNow();
                if (agent.providerBinder != null) {
                    try { agent.releaseProvider(DesktopSearchBridge.AUTHORITY, agent.providerToken); }
                    catch (Exception e) { e.printStackTrace(); }
                }
                if (agent.wakeProviderBinder != null) {
                    try { agent.releaseProvider(WakeLogBridge.AUTHORITY, agent.wakeProviderToken); }
                    catch (Exception e) { e.printStackTrace(); }
                }
            }
        }
    }
}
