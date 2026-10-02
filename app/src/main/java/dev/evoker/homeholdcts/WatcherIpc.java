// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

final class WatcherIpc {
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_RECONNECT_LOGCAT = 3;
    static final int MSG_STOP_WATCHER = 4;
    static final int MSG_SYNC_PREFS = 5;
    static final int MSG_REQUEST_STATE = 6;

    static final int MSG_STATE_CHANGED = 100;

    static final int STATE_STOPPED = 0;
    static final int STATE_CONNECTING = 1;
    static final int STATE_ACTIVE = 2;
    static final int STATE_NEEDS_RECONNECT = 3;
    static final int STATE_NO_PERMISSION = 4;

    static final String KEY_SHELL_LOGS = "shell_logs";
    static final String KEY_FORCE_RECONNECT = "force_reconnect";
    static final String KEY_DELAY_MS = "delay_ms";
    static final String KEY_VIBRATE = "vibrate";
    static final String KEY_SOUND = "sound";
    static final String KEY_POWER_GEMINI = "power_gemini";
    static final String KEY_VOICE_WAKE_ASSISTANT = "voice_wake_assistant";
    static final String KEY_SWAP_CTS_ASSISTANT = "swap_cts_assistant";

    private WatcherIpc() {}
}
