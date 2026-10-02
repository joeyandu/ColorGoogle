// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Tiny translucent foreground bridge used only to create a TOP UID state long
 * enough for Android's mandatory privileged-log confirmation dialog to appear.
 *
 * It never renders the main MindTrigger Assist UI, never enters Recents, and
 * never bypasses Android's own confirmation dialog.
 */
public final class LogSessionBridgeActivity extends Activity {

    private static final long REQUEST_DELAY_MS = 420L;
    private static final long STATE_FALLBACK_MS = 1200L;
    private static final long VISIBLE_TIMEOUT_MS = 6500L;
    private static final long ABSOLUTE_TIMEOUT_MS = 12_000L;

    private final Handler ui =
            new Handler(Looper.getMainLooper());

    private Messenger watcherMessenger;
    private boolean watcherBound;
    private boolean reconnectSent;
    private boolean sawConnecting;
    private boolean resumed;
    private boolean windowFocused;
    private boolean finished;

    private final Messenger incomingMessenger =
            new Messenger(
                    new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            if (msg.what
                                    != WatcherIpc.MSG_STATE_CHANGED) {
                                super.handleMessage(msg);
                                return;
                            }

                            switch (msg.arg1) {
                                case WatcherIpc.STATE_ACTIVE:
                                    finishQuietly();
                                    break;

                                case WatcherIpc.STATE_CONNECTING:
                                    sawConnecting = true;
                                    break;

                                case WatcherIpc.STATE_NO_PERMISSION:
                                case WatcherIpc.STATE_STOPPED:
                                    finishQuietly();
                                    break;

                                case WatcherIpc.STATE_NEEDS_RECONNECT:
                                    /*
                                     * Initial registration normally reports
                                     * NEEDS_RECONNECT before we issue the
                                     * request. Only treat a later transition
                                     * back from CONNECTING as a failed/denied
                                     * attempt.
                                     */
                                    if (reconnectSent
                                            && sawConnecting
                                            && resumed) {
                                        ui.postDelayed(
                                                LogSessionBridgeActivity.this
                                                        ::finishQuietly,
                                                220L);
                                    }
                                    break;

                                default:
                                    break;
                            }
                        }
                    });

    private final ServiceConnection watcherConnection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(
                        ComponentName name,
                        IBinder binder) {

                    watcherMessenger = new Messenger(binder);
                    watcherBound = true;

                    sendWatcherMessage(
                            WatcherIpc.MSG_REGISTER_CLIENT,
                            true);

                    maybeScheduleReconnectFromTop();

                    ui.postDelayed(
                            () -> {
                                if (watcherBound
                                        && !finished) {
                                    sendWatcherMessage(
                                            WatcherIpc.MSG_REQUEST_STATE,
                                            false);
                                }
                            },
                            STATE_FALLBACK_MS);
                }

                @Override
                public void onServiceDisconnected(
                        ComponentName name) {
                    watcherBound = false;
                    watcherMessenger = null;
                    finishQuietly();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.clearFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.addFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        View transparent = new View(this);
        transparent.setBackgroundColor(Color.TRANSPARENT);
        setContentView(transparent);

        // Hard ceiling: never leave a transparent bridge task hanging if an
        // OEM suppresses focus or blocks the SystemUI log-access dialog.
        ui.postDelayed(this::finishQuietly, ABSOLUTE_TIMEOUT_MS);

        try {
            overridePendingTransition(0, 0);
        } catch (Throwable ignored) {
        }

        Intent watcher =
                new Intent(this, HomeHoldService.class);

        try {
            bindService(
                    watcher,
                    watcherConnection,
                    Context.BIND_AUTO_CREATE);
        } catch (Throwable ignored) {
            finishQuietly();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;

        maybeScheduleReconnectFromTop();

        // Start the visible timeout only after this bridge really owns window
        // focus. onResume() alone does not guarantee PROCESS_STATE_TOP on
        // ColorOS, and requesting logd too early is silently auto-declined by
        // Android's LogcatManagerService.
        if (windowFocused) {
            ui.removeCallbacks(visibleTimeout);
            ui.postDelayed(visibleTimeout, VISIBLE_TIMEOUT_MS);
        }
    }

    @Override
    protected void onPause() {
        resumed = false;
        windowFocused = false;
        ui.removeCallbacks(visibleTimeout);
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        windowFocused = hasFocus;

        if (hasFocus && resumed) {
            ui.removeCallbacks(visibleTimeout);
            ui.postDelayed(visibleTimeout, VISIBLE_TIMEOUT_MS);
            maybeScheduleReconnectFromTop();
        }
    }

    private void maybeScheduleReconnectFromTop() {
        if (finished
                || reconnectSent
                || !watcherBound
                || !resumed
                || !windowFocused) {
            return;
        }

        ui.postDelayed(
                this::requestReconnectOnce,
                REQUEST_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);

        if (watcherBound) {
            sendWatcherMessage(
                    WatcherIpc.MSG_UNREGISTER_CLIENT,
                    false);

            try {
                unbindService(watcherConnection);
            } catch (Throwable ignored) {
            }
        }

        watcherBound = false;
        watcherMessenger = null;
        super.onDestroy();
    }

    private final Runnable visibleTimeout =
            this::finishQuietly;

    private void requestReconnectOnce() {
        if (finished
                || reconnectSent
                || !watcherBound
                || !resumed
                || !windowFocused) {
            return;
        }

        reconnectSent = true;

        if (!sendWatcherMessage(
                WatcherIpc.MSG_RECONNECT_LOGCAT,
                false)) {
            finishQuietly();
        }
    }

    private boolean sendWatcherMessage(
            int what,
            boolean registerReply) {

        Messenger target = watcherMessenger;

        if (!watcherBound || target == null) {
            return false;
        }

        Message msg = Message.obtain(null, what);

        if (registerReply) {
            msg.replyTo = incomingMessenger;
        }

        try {
            target.send(msg);
            return true;
        } catch (RemoteException e) {
            watcherBound = false;
            watcherMessenger = null;
            return false;
        }
    }

    private void finishQuietly() {
        if (finished) {
            return;
        }

        finished = true;

        try {
            finishAndRemoveTask();
        } catch (Throwable ignored) {
            finish();
        }

        try {
            overridePendingTransition(0, 0);
        } catch (Throwable ignored) {
        }
    }
}
