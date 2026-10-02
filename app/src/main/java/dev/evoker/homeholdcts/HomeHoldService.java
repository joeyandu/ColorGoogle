// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.content.pm.ServiceInfo;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeHoldService extends Service {

    private static final String TAG = "HomeHoldCTS";
    private static volatile HomeHoldService instance;
    private volatile boolean useShellLogs;
    private IDesktopSearchAgent adbLogAgent;
    private int adbLogGeneration;

    static void adbAgentChanged() {
        HomeHoldService current = instance;
        if (current != null) current.mainHandler.post(() -> current.connectAdbLogs(false));
    }

    private void connectAdbLogs(boolean force) {
        if (!running || !isEnabled() || !WakeLogBridge.isSelected(this)) return;
        useShellLogs = true;
        closeCurrentLogdSession();
        IDesktopSearchAgent next = WakeLogBridge.agent;
        if (!force && next != null && next == adbLogAgent
                && (logSessionState == WatcherIpc.STATE_ACTIVE || logSessionState == WatcherIpc.STATE_CONNECTING)) return;
        final int generation = ++adbLogGeneration;
        adbLogAgent = next;
        reconnectInFlight = false;
        if (next == null) {
            Log.w(TAG, "ADB log agent disconnected; no automatic app-UID fallback");
            setLogSessionState(WatcherIpc.STATE_NEEDS_RECONNECT);
            sendStateToClient();
            return;
        }
        setLogSessionState(WatcherIpc.STATE_CONNECTING);
        sendStateToClient();
        try {
            next.setWakeListener(new IAdbWakeListener.Stub() {
                private void dispatch(Runnable action) {
                    if (android.os.Binder.getCallingUid() != 2000) throw new SecurityException("ADB log source required");
                    mainHandler.post(() -> {
                        if (generation == adbLogGeneration && running && isEnabled()) action.run();
                    });
                }
                @Override public void onConnected() {
                    dispatch(() -> {
                        Log.i(TAG, "ADB external log source verified");
                        setLogSessionState(WatcherIpc.STATE_ACTIVE);
                    });
                }
                @Override public void onLine(String line, long eventTime) {
                    dispatch(() -> {
                        long age = SystemClock.elapsedRealtime() - eventTime;
                        if (age >= 0 && age < 1500) handleTriggerLine(line);
                    });
                }
                @Override public void onDisconnected(String reason) {
                    dispatch(() -> {
                        Log.w(TAG, "ADB log source lost: " + reason);
                        setLogSessionState(WatcherIpc.STATE_NEEDS_RECONNECT);
                    });
                }
            });
            // The shell reader must prove it can receive another UID's logs even
            // while the device is idle. This app-UID line is never a trigger.
            Log.i(TAG, "ADB log listener registered; verifying cross-UID delivery");
        } catch (RemoteException e) {
            adbLogAgent = null;
            setLogSessionState(WatcherIpc.STATE_NEEDS_RECONNECT);
            Log.e(TAG, "ADB source connection failed", e);
        }
    }

    public static final String ACTION_STOP =
            "dev.evoker.homeholdcts.STOP";
    public static final String ACTION_RECONNECT_LOGCAT =
            "dev.evoker.homeholdcts.RECONNECT_LOGCAT";
    public static final String ACTION_REFRESH_FGS =
            "dev.evoker.homeholdcts.REFRESH_FGS";

    private static final String CHANNEL =
            WatcherNotificationHelper.CHANNEL;
    private static final int NOTIFICATION_ID =
            WatcherNotificationHelper.WATCHER_NOTIFICATION_ID;

    private static final long AUTO_RECOVERY_SURFACE_CHECK_MS = 1400L;
    private static final long RECOVERY_RETRY_ARM_MS = 15_000L;
    private static final int MAX_AUTO_RECOVERY_ATTEMPTS = 3;
    private static final long LEASE_RENEW_MS = 2L * 60L * 1000L;

    private final Object reconnectLock = new Object();
    private final Object logSessionLock = new Object();
    private final Object triggerLock = new Object();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean running;
    private volatile boolean reconnectRequested;
    private volatile boolean reconnectInFlight;
    private volatile boolean autoRecoveryAttemptedForCurrentLoss;
    private volatile int autoRecoveryAttempts;

    private volatile int logSessionState =
            WatcherIpc.STATE_STOPPED;

    private volatile SingleDirectLogdSession directLogdSession;
    private final TriggerClassifier triggerClassifier = new TriggerClassifier();

    private volatile int runtimeDelayMs =
            MainActivity.DEFAULT_CTS_DELAY_MS;
    private volatile boolean runtimeVibrate =
            MainActivity.DEFAULT_VIBRATE_ON_CTS;
    private volatile boolean runtimeSound =
            MainActivity.DEFAULT_SOUND_ON_ACTIVATION;
    private volatile boolean runtimePowerGemini =
            MainActivity.DEFAULT_POWER_GEMINI_EXPERIMENTAL;
    private volatile boolean runtimeVoiceWakeAssistant =
            MainActivity.DEFAULT_VOICE_WAKE_ASSISTANT_EXPERIMENTAL;
    private volatile boolean runtimeSwapCtsAssistant =
            MainActivity.DEFAULT_SWAP_CTS_ASSISTANT_EXPERIMENTAL;

    private ExecutorService executor;
    private ActivationRunner activationRunner;
    private OverlayGuardian overlayGuardian;
    private boolean wakeReceiverRegistered;

    private final Runnable leaseRenewRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running || !isEnabled()) {
                return;
            }
            RestartReceiver.armLease(HomeHoldService.this);
            WatcherRescueJobService.ensureScheduled(HomeHoldService.this);
            mainHandler.postDelayed(this, LEASE_RENEW_MS);
        }
    };

    private final BroadcastReceiver wakeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!running || !isEnabled() || intent == null) {
                return;
            }

            String action = intent.getAction();
            if (overlayGuardian != null) {
                overlayGuardian.onSystemTransition(action);
            } else {
                ensureKeepAliveOverlay();
            }
            startWatcherForeground(
                    WatcherNotificationHelper.buildWatcherNotification(
                            HomeHoldService.this,
                            logSessionState));

            // SCREEN_ON is used as a health nudge only. USER_PRESENT is the
            // safe moment to surface Android's mandatory log-access dialog if
            // the watcher/socket died while the phone was asleep.
            if (Intent.ACTION_USER_PRESENT.equals(action)
                    && logSessionState == WatcherIpc.STATE_NEEDS_RECONNECT) {
                autoRecoveryAttemptedForCurrentLoss = false;
                autoRecoveryAttempts = 0;
                mainHandler.postDelayed(
                        () -> maybeAutoRecoverLogSession("user-present"),
                        180L);
            }
        }
    };

    private Messenger clientMessenger;

    private final Messenger incomingMessenger =
            new Messenger(new IncomingHandler());

    private final class IncomingHandler extends Handler {
        IncomingHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WatcherIpc.MSG_REGISTER_CLIENT:
                    clientMessenger = msg.replyTo;
                    sendStateToClient();
                    break;

                case WatcherIpc.MSG_UNREGISTER_CLIENT:
                    clientMessenger = null;
                    break;

                case WatcherIpc.MSG_RECONNECT_LOGCAT:
                    requestLogSessionReconnect(msg.getData().getBoolean(WatcherIpc.KEY_FORCE_RECONNECT));
                    break;

                case WatcherIpc.MSG_STOP_WATCHER:
                    stopWatcherFromCommand();
                    break;

                case WatcherIpc.MSG_SYNC_PREFS:
                    applyRuntimePrefs(msg.getData());
                    break;

                case WatcherIpc.MSG_REQUEST_STATE:
                    sendStateToClient();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!isEnabled()) {
            stopSelf();
            return;
        }

        instance = this;
        useShellLogs = WakeLogBridge.isSelected(this);
        loadRuntimePrefsFromDisk();
        createNotificationChannel();

        setLogSessionState(
                WatcherIpc.STATE_NEEDS_RECONNECT);

        startWatcherForeground(
                WatcherNotificationHelper.buildWatcherNotification(
                        this,
                        WatcherIpc.STATE_NEEDS_RECONNECT));

        overlayGuardian = new OverlayGuardian(this);
        overlayGuardian.start();
        registerWakeReceiver();
        RestartReceiver.armLease(this);
        WatcherRescueJobService.ensureScheduled(this);
        mainHandler.removeCallbacks(leaseRenewRunnable);
        mainHandler.postDelayed(leaseRenewRunnable, LEASE_RENEW_MS);

        activationRunner = new ActivationRunner(this);
        executor = Executors.newSingleThreadExecutor();

        running = true;
        WirelessRecovery.get(this).watcherRunning(true);
        executor.execute(this::watchForever);
        connectAdbLogs(false);

        // V5 recovery: if the watcher was recreated while the screen is
        // interactive, surface the tiny TOP bridge. If the screen is off the
        // request is deferred until USER_PRESENT.
        mainHandler.postDelayed(
                () -> maybeAutoRecoverLogSession("service-start"),
                900L);
    }

    /**
     * Retain specialUse as the safe baseline and add systemExempted when the
     * package is currently exempt from battery optimizations via the one-shot
     * Doze allowlist setup. Device Admin is intentionally not used.
     */
    private void startWatcherForeground(android.app.Notification notification) {
        if (Build.VERSION.SDK_INT < 34) {
            startForeground(NOTIFICATION_ID, notification);
            return;
        }

        int serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
        if (isSystemExemptedEligible()) {
            serviceType |= ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED;
        }

        try {
            startForeground(NOTIFICATION_ID, notification, serviceType);
        } catch (RuntimeException systemExemptedFailure) {
            if ((serviceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED) == 0) {
                throw systemExemptedFailure;
            }

            // OEM device-policy/power state can race service startup. Never
            // sacrifice the existing watcher merely because the stronger type
            // was temporarily rejected.
            Log.w(TAG, "systemExempted FGS rejected; falling back to specialUse",
                    systemExemptedFailure);
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        }
    }

    private boolean isSystemExemptedEligible() {
        try {
            PowerManager power = getSystemService(PowerManager.class);
            return power != null
                    && power.isIgnoringBatteryOptimizations(getPackageName());
        } catch (Throwable t) {
            Log.w(TAG, "Doze allowlist eligibility check failed", t);
            return false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null
                && ACTION_STOP.equals(intent.getAction())) {
            stopWatcherFromCommand();
            return START_NOT_STICKY;
        }

        if (!isEnabled()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        ensureKeepAliveOverlay();
        RestartReceiver.armLease(this);
        WatcherRescueJobService.ensureScheduled(this);

        if (intent != null
                && ACTION_REFRESH_FGS.equals(intent.getAction())) {
            startWatcherForeground(
                    WatcherNotificationHelper.buildWatcherNotification(
                            this,
                            logSessionState));
        }

        if (!running
                && executor != null
                && !executor.isShutdown()) {
            running = true;
            executor.execute(this::watchForever);
        }

        if (intent != null
                && ACTION_RECONNECT_LOGCAT.equals(
                        intent.getAction())) {
            requestLogSessionReconnect(intent.getBooleanExtra(WatcherIpc.KEY_FORCE_RECONNECT, false));
        }
        connectAdbLogs(false);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return incomingMessenger.getBinder();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (isEnabled()) {
            RestartReceiver.scheduleRestart(this, 1200L);
            WatcherRescueJobService.ensureScheduled(this);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        running = false;
        WirelessRecovery.get(this).watcherRunning(false);
        if (instance == this) instance = null;
        adbLogGeneration++;
        if (adbLogAgent != null) {
            try { adbLogAgent.setWakeListener(null); } catch (RemoteException ignored) { }
            adbLogAgent = null;
        }

        synchronized (reconnectLock) {
            reconnectRequested = true;
            reconnectLock.notifyAll();
        }

        if (activationRunner != null) {
            activationRunner.shutdown();
            activationRunner = null;
        }

        closeCurrentLogdSession();

        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(
                        300,
                        TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        unregisterWakeReceiver();
        mainHandler.removeCallbacks(leaseRenewRunnable);
        removeKeepAliveOverlay();
        setLogSessionState(WatcherIpc.STATE_STOPPED);

        if (isEnabled()) {
            RestartReceiver.scheduleRestart(this, 1500L);
        }

        super.onDestroy();
    }

    private void stopWatcherFromCommand() {
        running = false;
        getSharedPreferences(
                MainActivity.PREFS,
                MODE_PRIVATE)
                .edit()
                .putBoolean(
                        MainActivity.PREF_ENABLED,
                        false)
                .commit();
        RestartReceiver.cancelRestart(this);
        WatcherRescueJobService.cancelScheduled(this);
        stopSelf();
    }

    private boolean isEnabled() {
        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        MainActivity.PREFS,
                        MODE_PRIVATE);

        return prefs.getBoolean(
                        MainActivity.PREF_ENABLED,
                        false)
                && prefs.getBoolean(
                        MainActivity.PREF_TERMS_ACCEPTED,
                        false);
    }

    private boolean hasReadLogsPermission() {
        return checkSelfPermission(
                "android.permission.READ_LOGS")
                == android.content.pm.PackageManager
                        .PERMISSION_GRANTED;
    }

    private boolean isAutomaticRecoveryAvailable() {
        return Build.VERSION.SDK_INT < 23
                || Settings.canDrawOverlays(this);
    }

    private void loadRuntimePrefsFromDisk() {
        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        MainActivity.PREFS,
                        MODE_PRIVATE);

        runtimeDelayMs =
                clampDelay(
                        prefs.getInt(
                                MainActivity.PREF_CTS_DELAY_MS,
                                MainActivity.DEFAULT_CTS_DELAY_MS));

        runtimeVibrate =
                prefs.getBoolean(
                        MainActivity.PREF_VIBRATE_ON_CTS,
                        MainActivity.DEFAULT_VIBRATE_ON_CTS);

        runtimeSound =
                prefs.getBoolean(
                        MainActivity.PREF_SOUND_ON_ACTIVATION,
                        MainActivity.DEFAULT_SOUND_ON_ACTIVATION);

        runtimePowerGemini =
                prefs.getBoolean(
                        MainActivity.PREF_POWER_GEMINI_EXPERIMENTAL,
                        MainActivity.DEFAULT_POWER_GEMINI_EXPERIMENTAL);

        runtimeVoiceWakeAssistant =
                prefs.getBoolean(
                        MainActivity.PREF_VOICE_WAKE_ASSISTANT_EXPERIMENTAL,
                        MainActivity.DEFAULT_VOICE_WAKE_ASSISTANT_EXPERIMENTAL);

        runtimeSwapCtsAssistant =
                prefs.getBoolean(
                        MainActivity.PREF_SWAP_CTS_ASSISTANT_EXPERIMENTAL,
                        MainActivity.DEFAULT_SWAP_CTS_ASSISTANT_EXPERIMENTAL);
    }

    private void applyRuntimePrefs(Bundle data) {
        if (data == null) {
            return;
        }

        runtimeDelayMs =
                clampDelay(
                        data.getInt(
                                WatcherIpc.KEY_DELAY_MS,
                                runtimeDelayMs));

        runtimeVibrate =
                data.getBoolean(
                        WatcherIpc.KEY_VIBRATE,
                        runtimeVibrate);

        runtimeSound =
                data.getBoolean(
                        WatcherIpc.KEY_SOUND,
                        runtimeSound);

        runtimePowerGemini =
                data.getBoolean(
                        WatcherIpc.KEY_POWER_GEMINI,
                        runtimePowerGemini);

        runtimeVoiceWakeAssistant =
                data.getBoolean(
                        WatcherIpc.KEY_VOICE_WAKE_ASSISTANT,
                        runtimeVoiceWakeAssistant);

        runtimeSwapCtsAssistant =
                data.getBoolean(
                        WatcherIpc.KEY_SWAP_CTS_ASSISTANT,
                        runtimeSwapCtsAssistant);
    }

    private int clampDelay(int value) {
        return Math.max(
                0,
                Math.min(
                        MainActivity.MAX_CTS_DELAY_MS,
                        value));
    }

    private void requestLogSessionReconnect() { requestLogSessionReconnect(false); }

    private void requestLogSessionReconnect(boolean force) {
        if (useShellLogs || WakeLogBridge.isSelected(this)) {
            connectAdbLogs(force);
            return;
        }
        if (!hasReadLogsPermission()) {
            reconnectInFlight = false;
            setLogSessionState(
                    WatcherIpc.STATE_NO_PERMISSION);
            return;
        }

        int current = logSessionState;

        if ((!force && current == WatcherIpc.STATE_ACTIVE)
                || current == WatcherIpc.STATE_CONNECTING
                || reconnectInFlight) {
            return;
        }

        reconnectInFlight = true;
        closeCurrentLogdSession();

        synchronized (reconnectLock) {
            reconnectRequested = true;
            reconnectLock.notifyAll();
        }

        setLogSessionState(
                WatcherIpc.STATE_CONNECTING);
    }

    private void watchForever() {
        while (running && isEnabled()) {
            waitForReconnectRequest();

            if (!running || !isEnabled()) {
                break;
            }

            if (!hasReadLogsPermission()) {
                reconnectInFlight = false;
                setLogSessionState(
                        WatcherIpc.STATE_NO_PERMISSION);
                continue;
            }

            if (!useShellLogs) runOneLogSession();
        }

        if (!running || !isEnabled()) {
            setLogSessionState(
                    WatcherIpc.STATE_STOPPED);
        }
    }

    private void waitForReconnectRequest() {
        synchronized (reconnectLock) {
            while (running
                    && isEnabled()
                    && !reconnectRequested) {
                try {
                    reconnectLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            reconnectRequested = false;
        }
    }

    private void runOneLogSession() {
        final long listenerStarted = SystemClock.elapsedRealtime();
        final boolean[] sessionWasActive = new boolean[] { false };

        SingleDirectLogdSession session = new SingleDirectLogdSession(
                new SingleDirectLogdSession.Listener() {
                    @Override
                    public void onActive() {
                        sessionWasActive[0] = true;
                        reconnectInFlight = false;
                        setLogSessionState(WatcherIpc.STATE_ACTIVE);
                    }

                    @Override
                    public void onLine(String line) {
                        if (SystemClock.elapsedRealtime()
                                - listenerStarted < 700L) {
                            return;
                        }
                        handleTriggerLine(line);
                    }

                    @Override
                    public void onAllLanesLost(String reason) {
                        if (running && isEnabled()) {
                            Log.w(TAG, "Direct logd session lost: " + reason);
                        }
                    }
                });

        try {
            synchronized (logSessionLock) {
                if (useShellLogs) { session.close(); return; }
                directLogdSession = session;
            }

            session.start();
            session.awaitTermination();

        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            if (running && isEnabled()) {
                Log.e(TAG, "Single direct-logd watcher failed", e);
            }
        } finally {
            closeCurrentLogdSession();
            reconnectInFlight = false;

            if (running && isEnabled() && !useShellLogs) {
                setLogSessionState(WatcherIpc.STATE_NEEDS_RECONNECT);

                final boolean wasEverActive =
                        sessionWasActive[0] || session.wasEverActive();
                if (!wasEverActive) {
                    // The bridge marks this loss as attempted before asking
                    // logd to connect. A never-active socket must clear that
                    // per-attempt gate so bounded recovery can continue.
                    autoRecoveryAttemptedForCurrentLoss = false;
                }

                // A connection failure before the first packet also needs the
                // existing TOP/focus-gated recovery path. Otherwise the
                // watcher waits indefinitely for an external reconnect.
                mainHandler.post(
                        () -> maybeAutoRecoverLogSession(
                                wasEverActive
                                        ? "single-direct-logd-session-lost"
                                        : "session-never-active"));
            }
        }
    }

    private void handleTriggerLine(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }

        synchronized (triggerLock) {
            TriggerClassifier.Source source = triggerClassifier.accept(
                    line, SystemClock.elapsedRealtime());
            if (source == TriggerClassifier.Source.NONE) return;
            final boolean targetAssistant = TriggerClassifier.targetsAssistant(
                    source, runtimePowerGemini, runtimeVoiceWakeAssistant,
                    runtimeSwapCtsAssistant);

            Log.i(TAG,
                    "Trigger classified source=" + source
                            + " target=" + (targetAssistant ? "ASSISTANT" : "CTS")
                            + " swap=" + runtimeSwapCtsAssistant);

            if (activationRunner != null) {
                if (targetAssistant) {
                    activationRunner.activateAssistantSession(
                            runtimeDelayMs,
                            runtimeVibrate,
                            runtimeSound);
                } else {
                    activationRunner.activateCts(
                            runtimeDelayMs,
                            runtimeVibrate,
                            runtimeSound);
                }
            }
        }
    }

    private void maybeAutoRecoverLogSession(String reason) {
        if (useShellLogs) return;
        if (!running
                || !isEnabled()
                || !hasReadLogsPermission()
                || !isAutomaticRecoveryAvailable()
                || logSessionState
                        != WatcherIpc.STATE_NEEDS_RECONNECT
                || autoRecoveryAttemptedForCurrentLoss
                || autoRecoveryAttempts >= MAX_AUTO_RECOVERY_ATTEMPTS) {
            return;
        }

        try {
            PowerManager power = getSystemService(PowerManager.class);
            if (power != null && !power.isInteractive()) {
                // Do not burn the one recovery attempt behind a sleeping or
                // locked screen. USER_PRESENT will retry when the user wakes it.
                return;
            }
        } catch (Throwable ignored) {
        }

        autoRecoveryAttemptedForCurrentLoss = true;
        autoRecoveryAttempts++;

        try {
            Intent open =
                    new Intent(
                            this,
                            LogSessionBridgeActivity.class);

            open.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            | Intent.FLAG_ACTIVITY_NO_ANIMATION);

            startActivity(open);

            Log.i(
                    TAG,
                    "Automatic log recovery surfaced LogSessionBridgeActivity once. reason="
                            + reason);

        } catch (Throwable e) {
            Log.w(
                    TAG,
                    "Automatic recovery Activity start failed; "
                            + "notification fallback remains.",
                    e);
        }

        mainHandler.postDelayed(
                () -> {
                    if (running
                            && isEnabled()
                            && logSessionState
                                    == WatcherIpc.STATE_NEEDS_RECONNECT) {
                        updateWatcherNotification();
                    }
                },
                AUTO_RECOVERY_SURFACE_CHECK_MS);

        // If OEM background-activity policy swallowed the bridge, allow a
        // later USER_PRESENT event to make another attempt. Never loop-launch
        // Activities on a timer.
        mainHandler.postDelayed(
                () -> {
                    if (running
                            && isEnabled()
                            && logSessionState
                                    == WatcherIpc.STATE_NEEDS_RECONNECT) {
                        autoRecoveryAttemptedForCurrentLoss = false;
                        maybeAutoRecoverLogSession(
                                "retry-" + autoRecoveryAttempts);
                    }
                },
                RECOVERY_RETRY_ARM_MS);
    }

    private void closeCurrentLogdSession() {
        SingleDirectLogdSession session;

        synchronized (logSessionLock) {
            session = directLogdSession;
            directLogdSession = null;
        }

        if (session != null) {
            session.close();
        }
    }

    private void setLogSessionState(int state) {
        if (logSessionState == state) {
            return;
        }

        logSessionState = state;

        if (state == WatcherIpc.STATE_ACTIVE) {
            autoRecoveryAttemptedForCurrentLoss = false;
            autoRecoveryAttempts = 0;
        }

        sendStateToClient();
        updateWatcherNotification();
    }

    private void sendStateToClient() {
        Messenger client = clientMessenger;

        if (client == null) {
            return;
        }

        Message msg =
                Message.obtain(
                        null,
                        WatcherIpc.MSG_STATE_CHANGED);

        msg.arg1 = logSessionState;
        Bundle stateData = new Bundle();
        stateData.putBoolean(WatcherIpc.KEY_SHELL_LOGS, useShellLogs);
        msg.setData(stateData);

        try {
            client.send(msg);
        } catch (RemoteException e) {
            clientMessenger = null;
        }
    }

    private void updateWatcherNotification() {
        try {
            NotificationManager nm =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE);

            if (nm != null) {
                nm.notify(
                        NOTIFICATION_ID,
                        WatcherNotificationHelper
                                .buildWatcherNotification(
                                        this,
                                        logSessionState));
            }
        } catch (Throwable ignored) {
        }
    }

    private void registerWakeReceiver() {
        if (wakeReceiverRegistered) {
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(
                        wakeReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(wakeReceiver, filter);
            }
            wakeReceiverRegistered = true;
        } catch (Throwable t) {
            Log.w(TAG, "Unable to register wake recovery receiver", t);
        }
    }

    private void unregisterWakeReceiver() {
        if (!wakeReceiverRegistered) {
            return;
        }
        wakeReceiverRegistered = false;
        try {
            unregisterReceiver(wakeReceiver);
        } catch (Throwable ignored) {
        }
    }

    private void ensureKeepAliveOverlay() {
        if (overlayGuardian == null) {
            overlayGuardian = new OverlayGuardian(this);
            overlayGuardian.start();
        } else {
            overlayGuardian.ensureNow("service-nudge");
        }
    }

    private void removeKeepAliveOverlay() {
        OverlayGuardian guardian = overlayGuardian;
        overlayGuardian = null;
        if (guardian != null) {
            guardian.stop();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (running && isEnabled()) {
            ensureKeepAliveOverlay();
            RestartReceiver.armLease(this);
            WatcherRescueJobService.ensureScheduled(this);

            // Re-assert the foreground state after memory-pressure callbacks.
            // This does not create a new service; it only refreshes the current
            // watcher notification/type if ColorOS has re-evaluated it.
            startWatcherForeground(
                    WatcherNotificationHelper.buildWatcherNotification(
                            this,
                            logSessionState));
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (running && isEnabled()) {
            ensureKeepAliveOverlay();
            RestartReceiver.armLease(this);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE);

            if (nm != null) {
                NotificationChannel ch =
                        new NotificationChannel(
                                CHANNEL,
                                UiText.tr(this, "ColorGoogle watcher"),
                                NotificationManager.IMPORTANCE_LOW);

                ch.setDescription(
                        UiText.tr(
                                this,
                                "Keeps the isolated foreground-service watcher available for trigger detection and log-session recovery."));
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
    }
}
