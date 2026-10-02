// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

final class DesktopSearchActivation {
    // No stop command: automatic recovery must never tear down a healthy agent.
    static final String START_SCRIPT = String.join("\n",
            "set -eu",
            "apk=$(pm path --user 0 dev.evoker.homeholdcts | sed -n \"s/^package://p\" | head -n 1)",
            "test -r \"$apk\"",
            "export CLASSPATH=\"$apk\"",
            "nohup setsid app_process / --nice-name=colorgoogle-search dev.evoker.homeholdcts.DesktopSearchAgent </dev/null >/data/local/tmp/colorgoogle-search.log 2>&1 &",
            "sleep 2",
            "content call --uri content://dev.evoker.homeholdcts.desktopsearch --method status");
    static final String SCRIPT = String.join("\n",
            "set -eu",
            "content call --uri content://dev.evoker.homeholdcts.desktopsearch --method stop",
            "i=0",
            "while pidof colorgoogle-search >/dev/null; do",
            "  i=$((i+1)); if [ \"$i\" -gt 10 ]; then echo Agent_did_not_stop; exit 1; fi",
            "  sleep 1",
            "done",
            "apk=$(pm path --user 0 dev.evoker.homeholdcts | sed -n \"s/^package://p\" | head -n 1)",
            "test -r \"$apk\"",
            "export CLASSPATH=\"$apk\"",
            "nohup app_process / --nice-name=colorgoogle-search dev.evoker.homeholdcts.DesktopSearchAgent </dev/null >/data/local/tmp/colorgoogle-search.log 2>&1 &",
            "sleep 2",
            "content call --uri content://dev.evoker.homeholdcts.desktopsearch --method status",
            "cat /data/local/tmp/colorgoogle-search.log");
    static String command() {
        return "adb -s SERIAL shell '" + SCRIPT + "'";
    }
    private DesktopSearchActivation() {}
}
