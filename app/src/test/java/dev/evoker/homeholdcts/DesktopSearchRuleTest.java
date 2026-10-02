// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;
import org.junit.Test;
import static org.junit.Assert.*;
public class DesktopSearchRuleTest {
    private boolean match(int uid, String pkg, String component, String action, String uri) {
        return DesktopSearchRule.matches(uid, 10222, pkg, component, action, uri);
    }
    @Test public void exactDesktopRequestMatches() {
        assertTrue(match(10222, DesktopSearchRule.TARGET, DesktopSearchRule.COMPONENT,
                "android.intent.action.VIEW", "gs://search/gsearch?source=dock"));
    }
    @Test public void otherCallerCannotTriggerEvenWithIdenticalIntent() {
        assertFalse(match(2000, DesktopSearchRule.TARGET, DesktopSearchRule.COMPONENT,
                "android.intent.action.VIEW", "gs://search/gsearch?source=dock"));
    }
    @Test public void otherSearchEntriesAndPackagesPassThrough() {
        assertFalse(match(10222, DesktopSearchRule.TARGET, DesktopSearchRule.COMPONENT,
                "android.intent.action.VIEW", "gs://search/gsearch?source=swipe"));
        assertFalse(match(10222, "com.google.android.googlequicksearchbox", DesktopSearchRule.COMPONENT,
                "android.intent.action.VIEW", "gs://search/gsearch?source=dock"));
        assertFalse(match(10222, DesktopSearchRule.TARGET, "unrelated.Activity",
                "android.intent.action.VIEW", "gs://search/gsearch?source=dock"));
        assertFalse(match(10222, DesktopSearchRule.TARGET, DesktopSearchRule.COMPONENT,
                "android.intent.action.MAIN", "gs://search/gsearch?source=dock"));
        assertFalse(match(10222, DesktopSearchRule.TARGET, DesktopSearchRule.COMPONENT,
                "android.intent.action.VIEW", null));
    }
    @Test public void duplicateDoesNotExtendDebounce() {
        DesktopSearchRule r = new DesktopSearchRule();
        assertTrue(r.accept(0)); assertFalse(r.accept(10)); assertFalse(r.accept(799));
        assertTrue(r.accept(800)); assertFalse(r.accept(1599)); assertTrue(r.accept(1600));
    }
}
