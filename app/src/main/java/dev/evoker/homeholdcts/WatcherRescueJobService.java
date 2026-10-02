// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Independent system-owned rescue path for the watcher.
 *
 * START_STICKY and onDestroy() are not sufficient when ColorOS/LMK kills a
 * process without calling lifecycle callbacks. A persisted JobScheduler job
 * gives Android another reason to recreate the package and ensure the watcher
 * service exists.
 */
public final class WatcherRescueJobService extends JobService {

    private static final int JOB_ID = 7043;
    private static final long PERIOD_MS = 15L * 60L * 1000L;

    static void ensureScheduled(Context context) {
        try {
            JobScheduler scheduler =
                    context.getSystemService(JobScheduler.class);
            if (scheduler == null) {
                return;
            }

            JobInfo existing = scheduler.getPendingJob(JOB_ID);
            if (existing != null) {
                return;
            }

            JobInfo.Builder builder = new JobInfo.Builder(
                    JOB_ID,
                    new ComponentName(context, WatcherRescueJobService.class))
                    .setPersisted(true)
                    .setPeriodic(PERIOD_MS);

            scheduler.schedule(builder.build());
        } catch (Throwable ignored) {
        }
    }

    static void cancelScheduled(Context context) {
        try {
            JobScheduler scheduler = context.getSystemService(JobScheduler.class);
            if (scheduler != null) {
                scheduler.cancel(JOB_ID);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        boolean enabled = getSharedPreferences(
                MainActivity.PREFS,
                MODE_PRIVATE)
                .getBoolean(MainActivity.PREF_ENABLED, false);

        if (enabled) {
            Intent service = new Intent(this, HomeHoldService.class);
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(service);
                } else {
                    startService(service);
                }
            } catch (Throwable ignored) {
            }
        }

        jobFinished(params, false);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
