// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import java.util.ArrayList;
import java.util.List;

/** Single source of truth for Shizuku and PC setup commands. */
final class SetupCommands {

    static final String GOOGLE = "com.google.android.googlequicksearchbox";
    static final String GEMINI = "com.google.android.apps.bard";
    static final String GSF = "com.google.android.gsf";
    static final String GMS = "com.google.android.gms";
    static final String GOOGLE_ASSISTANT_SERVICE =
            GOOGLE + "/com.google.android.voiceinteraction.GsaVoiceInteractionService";

    static final String SPEECH_ASSIST = "com.heytap.speechassist";
    static final String COLOR_DIRECT = "com.coloros.colordirectservice";

    static final class Target {
        final String label;
        final String packageName;
        final boolean self;

        Target(String label, String packageName, boolean self) {
            this.label = label;
            this.packageName = packageName;
            this.self = self;
        }
    }

    static final class Command {
        final String label;
        final String action;
        final String packageName;
        final String command;
        final boolean requiresInstalledPackage;

        Command(String label, String packageName, String command,
                boolean requiresInstalledPackage) {
            this(label, command, packageName, command, requiresInstalledPackage);
        }

        Command(String label, String action, String packageName, String command,
                boolean requiresInstalledPackage) {
            this.label = label;
            this.action = action;
            this.packageName = packageName;
            this.command = command;
            this.requiresInstalledPackage = requiresInstalledPackage;
        }
    }

    static Target[] targets(String self) {
        return new Target[] {
                new Target("ColorGoogle", self, true),
                new Target("Google", GOOGLE, false),
                new Target("Gemini", GEMINI, false),
                new Target("Google Services Framework", GSF, false),
                new Target("Google Play services", GMS, false)
        };
    }

    static List<Command> all(String self) {
        ArrayList<Command> out = new ArrayList<>();
        out.add(new Command(
                "ColorGoogle",
                "Grant READ_LOGS",
                self,
                "pm grant " + self + " android.permission.READ_LOGS",
                true
        ));

        out.add(new Command(
                "Google",
                "Set default assistant",
                GOOGLE,
                "settings --user 0 put secure assistant "
                        + GOOGLE_ASSISTANT_SERVICE,
                true
        ));
        out.add(new Command(
                "Google",
                "Set voice interaction service",
                GOOGLE,
                "settings --user 0 put secure voice_interaction_service "
                        + GOOGLE_ASSISTANT_SERVICE,
                true
        ));

        // These are the ColorOS/Android system switches discovered for the
        // background-retention path. They are included in both one-shot setup
        // routes; the existing package keep-alive commands remain below.
        addInternal(out, self, "Disable phantom-process monitoring",
                "settings put global settings_enable_monitor_phantom_procs false");
        addInternal(out, self, "Disable cached-app freezer",
                "settings put global cached_apps_freezer disabled");
        addInternal(out, self, "Disable ColorOS app disable switch",
                "settings --user 0 put secure app_disable_switch 0");
        addInternal(out, self, "Disable ColorOS auto switch",
                "settings --user 0 put secure auto_switch 0");
        addInternal(out, self, "Disable ColorOS auto-freeze timer",
                "settings --user 0 put secure auto_frozen_time 0");
        addInternal(out, self, "Disable ColorOS auto power protection",
                "settings put system auto_power_protect_state 0");

        for (Target target : targets(self)) {
            String p = target.packageName;
            add(out, target.label, "Add Doze whitelist", p,
                    "dumpsys deviceidle whitelist +" + p);
            add(out, target.label, "Mark app active", p,
                    "am set-inactive " + p + " false");
            add(out, target.label, "Set active standby bucket", p,
                    "am set-standby-bucket " + p + " active");
            add(out, target.label, "Allow background execution", p,
                    "cmd appops set " + p + " RUN_IN_BACKGROUND allow");
            add(out, target.label, "Allow unrestricted background execution", p,
                    "cmd appops set " + p + " RUN_ANY_IN_BACKGROUND allow");
            add(out, target.label, "Allow wake locks", p,
                    "cmd appops set " + p + " WAKE_LOCK allow");
            add(out, target.label, "Allow foreground service", p,
                    "cmd appops set " + p + " START_FOREGROUND allow");
            add(out, target.label, "Allow notifications", p,
                    "cmd appops set " + p + " POST_NOTIFICATION allow");
        }
        return out;
    }

    /**
     * Shizuku-only first-run preparation.
     *
     * ColorOS keeps the relevant gesture state behind OEM behavior that is not
     * reliably queryable here. The setup UI therefore requires explicit user
     * confirmation of the ColorOS navigation switch.
     */
    static List<Command> shizukuAll(String self) {
        ArrayList<Command> out = new ArrayList<>(all(self));


        out.add(new Command(
                "Remove SpeechAssist from user 0",
                "Remove competing ColorOS component",
                SPEECH_ASSIST,
                "pm uninstall --user 0 " + SPEECH_ASSIST,
                true
        ));
        out.add(new Command(
                "Remove ColorDirectService from user 0",
                "Remove competing ColorOS component",
                COLOR_DIRECT,
                "pm uninstall --user 0 " + COLOR_DIRECT,
                true
        ));

        // ColorOS keeps an OEM-owned package list in this secure setting. Do
        // not replace it: retain the device's current text and append this
        // package only when it is absent. This complex shell snippet stays in
        // the Shizuku-only route, so the legacy PC one-shot remains simple.
        out.add(new Command(
                "ColorGoogle",
                "Exclude from ColorOS app freeze list",
                self,
                "current=$(settings --user 0 get secure app_frozen_switch_close); "
                        + "case \"$current\" in "
                        + "null|'') next='" + self + "' ;; "
                        + "*'" + self + "'*) next=\"$current\" ;; "
                        + "*';'*) next=\"$current;" + self + "\" ;; "
                        + "*'|'*) next=\"$current|" + self + "\" ;; "
                        + "*) next=\"$current," + self + "\" ;; "
                        + "esac; settings --user 0 put secure app_frozen_switch_close \"$next\"",
                true
        ));

        return out;
    }

    private static void add(
            List<Command> list, String label, String action, String pkg, String cmd) {
        list.add(new Command(label, action, pkg, cmd, true));
    }

    private static void addInternal(
            List<Command> list, String self, String action, String cmd) {
        list.add(new Command("ColorOS system", action, self, cmd, false));
    }

    static String speechAssistRestoreCommands() {
        return "adb shell cmd package install-existing --user 0 " + SPEECH_ASSIST
                + "\n"
                + "adb shell pm enable --user 0 " + SPEECH_ASSIST;
    }

    static String speechAssistRemoveCommand() {
        return "adb shell pm uninstall --user 0 " + SPEECH_ASSIST;
    }

    /**
     * PC one-shot stays non-destructive; package removal is Shizuku-only.
     */
    static String pcOneShot(String self) {
        StringBuilder body = new StringBuilder();
        for (Command c : all(self)) {
            if (body.length() > 0) body.append("; ");
            body.append(c.command);
        }
        body.append("; am start -n ")
                .append(self)
                .append("/.MainActivity");
        return "adb shell \"" + body + "\"";
    }

    private SetupCommands() {}
}
