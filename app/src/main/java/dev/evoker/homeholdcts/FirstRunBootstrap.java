// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

/** Shizuku-assisted first-run setup and maintenance command runner. */
final class FirstRunBootstrap {

    interface Callback {
        void onState(String state);
        void onLog(String log);
    }

    static final int REQUEST_CODE = 7505;
    private static final String TAG = "HomeHoldCTS";
    // Legacy preference names are intentionally retained so upgrading from V5
    // does not discard an already completed shell bootstrap or its last log.
    private static final String PREF_BOOTSTRAP_DONE = "bootstrap_done_v5_pre1";
    private static final String PREF_COMMAND_LOG = "bootstrap_command_log_v5_pre1";
    private static final String PREF_LAST_EXECUTION_MS =
            "bootstrap_last_execution_ms_v72";

    private final Context context;
    private final Callback callback;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean busy;

    FirstRunBootstrap(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    boolean isDone() {
        SharedPreferences prefs = context.getSharedPreferences(
                MainActivity.PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_BOOTSTRAP_DONE, false)
                && hasReadLogs()
                && hasKeepAliveProvisioning();
    }

    boolean hasReadLogs() {
        return context.checkSelfPermission("android.permission.READ_LOGS")
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasKeepAliveProvisioning() {
        try {
            PowerManager power = context.getSystemService(PowerManager.class);
            return power != null
                    && power.isIgnoringBatteryOptimizations(
                            context.getPackageName());
        } catch (Throwable t) {
            Log.w(TAG, "KeepAlive Doze check failed", t);
            return false;
        }
    }

    String getLastCommandLog() {
        String log = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .getString(PREF_COMMAND_LOG, "No privileged setup has run yet.");
        return sanitizeUiLog(log);
    }

    long getLastExecutionTimeMs() {
        return context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .getLong(PREF_LAST_EXECUTION_MS, 0L);
    }

    void beginIfNeeded() {
        begin(false);
    }

    void runNow() {
        begin(true);
    }

    private void begin(boolean force) {
        if (!force && isDone()) {
            emitState("READY");
            emitLog(getLastCommandLog());
            return;
        }
        try {
            if (!Shizuku.pingBinder()) {
                emitState("SHIZUKU_NOT_RUNNING");
                return;
            }
            if (Shizuku.isPreV11()) {
                emitState("SHIZUKU_TOO_OLD");
                return;
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                executeSetup();
                return;
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                emitState("SHIZUKU_DENIED");
                return;
            }
            emitState("REQUESTING_SHIZUKU");
            Shizuku.requestPermission(REQUEST_CODE);
        } catch (IllegalStateException e) {
            emitState("SHIZUKU_NOT_RUNNING");
        } catch (Throwable t) {
            Log.w(TAG, "Bootstrap init failed", t);
            emitState("BOOTSTRAP_ERROR");
        }
    }

    void onPermissionResult(int requestCode, int grantResult) {
        if (requestCode != REQUEST_CODE) return;
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            executeSetup();
        } else {
            emitState("SHIZUKU_DENIED");
        }
    }

    private void executeSetup() {
        if (busy) return;
        busy = true;
        emitState("RUNNING_SETUP");

        executor.execute(() -> {
            String self = context.getPackageName();
            StringBuilder report = new StringBuilder();
            int ok = 0;
            int fail = 0;
            int skip = 0;

            try {
                for (SetupCommands.Command c : SetupCommands.shizukuAll(self)) {
                    if (c.requiresInstalledPackage
                            && !self.equals(c.packageName)
                            && !isInstalled(c.packageName)) {
                        appendReportRow(
                                report,
                                "SKIPPED",
                                c.label,
                                c.action,
                                "package not installed");
                        skip++;
                        continue;
                    }

                    ShellResult r = shell(c.command);
                    String detail = "exit=" + r.exitCode;
                    if (!r.stdout.isEmpty()) detail += " · out=" + oneLine(r.stdout);
                    if (!r.stderr.isEmpty()) detail += " · err=" + oneLine(r.stderr);
                    appendReportRow(
                            report,
                            r.success() ? "SUCCESS" : "FAILED",
                            c.label,
                            c.action,
                            detail);
                    if (r.success()) ok++; else fail++;
                }

                report.insert(0,
                        "COMMAND RESULTS\n"
                                + "Status | Target | Action | Detail\n"
                                + "------------------------------------------------\n");
                report.insert(0, "Setup result: " + ok + " successful · "
                        + fail + " failed · " + skip + " skipped\n");

                saveReport(sanitizeUiLog(report.toString()));
                emitLog(sanitizeUiLog(report.toString()));

                refreshWatcherForegroundTypeIfEnabled();

                if (hasReadLogs()) {
                    context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                            .edit().putBoolean(PREF_BOOTSTRAP_DONE, true).commit();
                    emitState(fail == 0 ? "READY" : "READY_WITH_WARNINGS");
                } else {
                    emitState("READ_LOGS_NOT_GRANTED");
                }
            } catch (Throwable t) {
                Log.e(TAG, "Shizuku setup failed", t);
                report.append("\nFATAL: ")
                        .append(t.getClass().getSimpleName())
                        .append(": ").append(t.getMessage());
                saveReport(sanitizeUiLog(report.toString()));
                emitLog(sanitizeUiLog(report.toString()));
                emitState("BOOTSTRAP_FAILED");
            } finally {
                busy = false;
            }
        });
    }

    private void refreshWatcherForegroundTypeIfEnabled() {
        try {
            boolean enabled = context.getSharedPreferences(
                    MainActivity.PREFS,
                    Context.MODE_PRIVATE)
                    .getBoolean(MainActivity.PREF_ENABLED, false);
            if (!enabled) return;

            Intent refresh = new Intent(context, HomeHoldService.class)
                    .setAction(HomeHoldService.ACTION_REFRESH_FGS);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(refresh);
            } else {
                context.startService(refresh);
            }
        } catch (Throwable t) {
            Log.w(TAG, "KeepAlive V1 watcher FGS refresh failed", t);
        }
    }

    private boolean isInstalled(String pkg) {
        try {
            context.getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isAssistantSecureSetting(String command) {
        if (command == null) return false;
        return command.contains(" put secure assistant ")
                || command.contains(" put secure voice_interaction_service ");
    }

    private static String sanitizeUiLog(String value) {
        if (value == null || value.isEmpty()) return value == null ? "" : value;
        String[] lines = value.split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            String clean = line;
            if (isAssistantSecureSetting(line)) {
                clean = line.startsWith("✗")
                        ? "✗ Assistant configuration [internal]"
                        : "✓ Assistant configuration [internal]";
            }
            if (out.length() > 0) out.append('\n');
            out.append(clean);
        }
        return out.toString();
    }

    private static String oneLine(String value) {
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static void appendReportRow(
            StringBuilder report,
            String status,
            String target,
            String action,
            String detail) {
        report.append(status)
                .append(" | ")
                .append(target)
                .append(" | ")
                .append(action)
                .append(" | ")
                .append(detail)
                .append('\n');
    }

    private ShellResult shell(String command) throws Exception {
        Method newProcess = Shizuku.class.getDeclaredMethod(
                "newProcess", String[].class, String[].class, String.class);
        newProcess.setAccessible(true);
        Object remote = newProcess.invoke(null, new Object[] {
                new String[] { "/system/bin/sh", "-c", command }, null, null
        });

        String out = read((InputStream) remote.getClass()
                .getMethod("getInputStream").invoke(remote));
        String err = read((InputStream) remote.getClass()
                .getMethod("getErrorStream").invoke(remote));
        int code = ((Number) remote.getClass()
                .getMethod("waitFor").invoke(remote)).intValue();
        try {
            remote.getClass().getMethod("destroy").invoke(remote);
        } catch (Throwable ignored) {}
        return new ShellResult(code, out, err);
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (out.length() > 0) out.append('\n');
                out.append(line);
            }
        }
        return out.toString().trim();
    }

    private void saveReport(String log) {
        context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_COMMAND_LOG, log)
                .putLong(PREF_LAST_EXECUTION_MS, System.currentTimeMillis())
                .commit();
    }

    private void emitState(String state) {
        if (callback != null) callback.onState(state);
    }

    private void emitLog(String log) {
        if (callback != null) callback.onLog(log);
    }

    private static final class ShellResult {
        final int exitCode;
        final String stdout;
        final String stderr;

        ShellResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout == null ? "" : stdout;
            this.stderr = stderr == null ? "" : stderr;
        }

        boolean success() { return exitCode == 0; }
    }
}
