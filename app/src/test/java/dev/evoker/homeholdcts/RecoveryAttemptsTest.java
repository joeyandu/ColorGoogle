// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecoveryAttemptsTest {
    @Test public void duplicateDiscoveryCannotRestartRetryBudget() {
        RecoveryAttempts p = new RecoveryAttempts(); p.endpoint(41000);
        assertTrue(p.begin(0)); p.endpoint(41000);
        assertFalse(p.begin(100)); assertTrue(p.begin(3000));
        p.endpoint(41000); assertFalse(p.begin(4000)); assertTrue(p.begin(13000));
        assertTrue(p.exhausted()); p.endpoint(41000); assertFalse(p.begin(90000));
    }
    @Test public void newAdbEndpointStartsFreshBudget() {
        RecoveryAttempts p = new RecoveryAttempts(); p.endpoint(41000);
        p.begin(0); p.begin(3000); p.begin(13000);
        p.endpoint(42000); assertFalse(p.exhausted()); assertTrue(p.begin(14000));
    }
    @Test public void manualResetRearmsAfterFailure() {
        RecoveryAttempts p = new RecoveryAttempts(); p.endpoint(41000);
        p.begin(0); p.begin(3000); p.begin(13000); p.reset();
        assertTrue(p.begin(14000)); assertFalse(p.begin(14001));
    }
    @Test public void waitTimeNeverGoesNegative() {
        RecoveryAttempts p = new RecoveryAttempts(); p.begin(10000);
        assertEquals(3000, p.delay(10000)); assertEquals(0, p.delay(13001));
    }
}
