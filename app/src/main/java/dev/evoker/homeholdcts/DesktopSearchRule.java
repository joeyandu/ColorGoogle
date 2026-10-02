// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

/** Exact OEM dock route; no foreground-history heuristic or log parsing. */
final class DesktopSearchRule {
    static final String TARGET = "com.heytap.quicksearchbox";
    static final String COMPONENT = TARGET + ".ui.activity.SearchHomeActivity";
    private long lastAccepted = Long.MIN_VALUE;

    static boolean matches(int callerUid, int launcherUid, String packageName,
                           String component, String action, String uri) {
        return launcherUid >= 10000 && callerUid == launcherUid
                && TARGET.equals(packageName) && COMPONENT.equals(component)
                && "android.intent.action.VIEW".equals(action)
                && "gs://search/gsearch?source=dock".equals(uri);
    }

    synchronized boolean accept(long now) {
        if (lastAccepted != Long.MIN_VALUE && now - lastAccepted < 800) return false;
        lastAccepted = now;
        return true;
    }
}
