// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;
import android.os.Bundle;
import dev.evoker.homeholdcts.IAdbWakeListener;
interface IDesktopSearchAgent {
    Bundle getStatus();
    Bundle setEnabled(boolean enabled);
    void setWakeListener(IAdbWakeListener listener);
    void shutdown();
}
