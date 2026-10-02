// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

/**
 * Process-death rescue path.
 *
 * V4 keeps a "lease" alarm armed while the watcher is healthy. The watcher
 * renews the lease before it fires. If ColorOS/LMK kills the process without
 * calling onDestroy(), the last lease survives in AlarmManager and starts the
 * watcher again. This is intentionally independent of START_STICKY.
 */
public class RestartReceiver extends BroadcastReceiver {

    public static final String ACTION_RESTART =
            "dev.evoker.homeholdcts.RESTART";

    private static final int REQUEST_CODE = 22;
    private static final long LEASE_TIMEOUT_MS = 4L * 60L * 1000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean enabled = context
                .getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .getBoolean(MainActivity.PREF_ENABLED, false);

        if (!enabled) {
            return;
        }

        startWatcher(context);
        WatcherRescueJobService.ensureScheduled(context);
    }

    static void startWatcher(Context context) {
        Intent service = new Intent(context, HomeHoldService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void scheduleRestart(Context context, long delayMs) {
        armAt(context, Math.max(250L, delayMs));
    }

    /**
     * Arm a watchdog lease. While the watcher is healthy it renews this before
     * expiry. If it disappears abruptly, the pending alarm is left behind and
     * becomes a system-owned resurrection attempt.
     */
    public static void armLease(Context context) {
        armAt(context, LEASE_TIMEOUT_MS);
    }

    public static void cancelRestart(Context context) {
        try {
            Intent i = new Intent(context, RestartReceiver.class);
            i.setAction(ACTION_RESTART);
            int flags = PendingIntent.FLAG_NO_CREATE;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getBroadcast(
                    context, REQUEST_CODE, i, flags);
            if (pi == null) {
                return;
            }
            AlarmManager am =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.cancel(pi);
            }
            pi.cancel();
        } catch (Throwable ignored) {
        }
    }

    private static void armAt(Context context, long delayMs) {
        try {
            Intent i = new Intent(context, RestartReceiver.class);
            i.setAction(ACTION_RESTART);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    i,
                    flags);

            AlarmManager am =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                return;
            }

            long when = SystemClock.elapsedRealtime() + delayMs;
            if (Build.VERSION.SDK_INT >= 23) {
                am.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        when,
                        pi);
            } else {
                am.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        when,
                        pi);
            }
        } catch (Throwable ignored) {
        }
    }
}
