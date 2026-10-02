// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

/** ColorOS setup navigation with safe fallbacks. */
final class SettingsNavigator {

    static void overlay(Activity activity) {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
        start(activity, i, new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
    }

    /**
     * Prefer the old direct confirmation flow used by the early MindTrigger Assist builds.
     * On ColorOS this normally presents a confirmation dialog instead of dumping the
     * user into a generic battery list. If the ROM refuses it for the target package,
     * fall back to the battery-optimization settings page.
     */
    static void requestBatteryExemption(Activity activity, String packageName) {
        Intent direct = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        direct.setData(Uri.parse("package:" + packageName));
        start(activity, direct,
                new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
    }

    static void batteryExemption(Activity activity, String packageName) {
        requestBatteryExemption(activity, packageName);
    }

    static void appDetails(Activity activity, String packageName) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName));
        start(activity, i, new Intent(Settings.ACTION_SETTINGS));
    }

    static void homeScreenSettings(Activity activity) {
        // ColorOS 16 / OPlus launcher Recent Tasks Manager.
        // User-confirmed activity:
        // com.android.launcher/com.oplus.quickstep.locksetting.ui.LockSettingActivity
        Intent direct = new Intent();
        direct.setComponent(new ComponentName(
                "com.android.launcher",
                "com.oplus.quickstep.locksetting.ui.LockSettingActivity"
        ));
        direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            activity.startActivity(direct);
            return;
        } catch (Throwable ignored) {
        }

        // Fallback to launcher settings / generic Settings.
        try {
            Intent launcherSettings = new Intent(Settings.ACTION_HOME_SETTINGS);
            activity.startActivity(launcherSettings);
        } catch (Throwable ignored) {
            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    static void assistantSettings(Activity activity) {
        // The AOSP/ColorOS digital-assistant picker. This is the same Settings
        // surface exposed as "Assist & voice input" on many Android builds.
        Intent direct = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
        direct.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$ManageAssistActivity"));
        direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(direct);
            return;
        } catch (Throwable ignored) {
        }

        Intent voiceInput = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
        voiceInput.setPackage("com.android.settings");
        try {
            activity.startActivity(voiceInput);
            return;
        } catch (Throwable ignored) {
        }

        try {
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
            return;
        } catch (Throwable ignored) {
        }

        try {
            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (Throwable t) {
            Toast.makeText(activity, UiText.tr(activity, "Settings page unavailable"), Toast.LENGTH_SHORT).show();
        }
    }

    static void autoLaunch(Activity activity) {
        // ColorOS 16 does not expose a stable exported Auto launch page to
        // third-party apps. Open Settings only; the bundled guide shows the path.
        try {
            Intent settings = new Intent(Settings.ACTION_SETTINGS);
            settings.setPackage("com.android.settings");
            activity.startActivity(settings);
        } catch (Throwable t) {
            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    static void developerOptions(Activity activity) {
        Intent developerOptions =
                new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        developerOptions.setPackage("com.android.settings");
        start(activity, developerOptions, new Intent(Settings.ACTION_SETTINGS));
    }


static void systemNavigation(Activity activity) {
    Intent direct = new Intent("com.android.settings.GESTURE_NAVIGATION_SETTINGS");
    direct.setPackage("com.android.settings");
    start(activity, direct, new Intent(Settings.ACTION_SETTINGS));
}

    private static Intent explicit(String pkg, String cls) {
        Intent i = new Intent();
        i.setComponent(new ComponentName(pkg, cls));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    private static void start(Activity activity, Intent primary, Intent fallback) {
        try {
            if (primary.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(primary);
                return;
            }
        } catch (Throwable ignored) {}
        try {
            activity.startActivity(fallback);
        } catch (Throwable t) {
            Toast.makeText(activity, UiText.tr(activity, "Settings page unavailable"), Toast.LENGTH_SHORT).show();
        }
    }

    private SettingsNavigator() {}
}
