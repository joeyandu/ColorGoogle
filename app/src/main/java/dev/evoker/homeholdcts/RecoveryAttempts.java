// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

/** Bounded per-outage retry policy. Duplicate discovery callbacks cannot reset it. */
final class RecoveryAttempts {
    private int attempts;
    private long readyAt;
    private int port;
    void reset() { attempts = 0; readyAt = 0; port = 0; }
    void endpoint(int value) {
        if (value != port) { reset(); port = value; }
    }
    boolean begin(long now) {
        if (attempts >= 3 || now < readyAt) return false;
        attempts++;
        readyAt = now + (attempts == 1 ? 3000 : 10000);
        return true;
    }
    boolean exhausted() { return attempts >= 3; }
    long delay(long now) { return Math.max(0, readyAt - now); }
}
