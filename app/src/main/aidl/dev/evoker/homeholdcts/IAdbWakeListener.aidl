// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;
oneway interface IAdbWakeListener {
    void onConnected();
    void onLine(String line, long eventTime);
    void onDisconnected(String reason);
}
