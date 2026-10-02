// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * User-invoked Quick Settings recovery entry point.
 *
 * It reads session state exclusively through watcher IPC. A healthy or
 * connecting session is left alone; only a confirmed lost session opens the
 * existing foreground LogSessionBridgeActivity so Android may show its own
 * log-access confirmation.
 */
public final class LogSessionRecoveryTileService extends TileService {

    private static final long STATE_TIMEOUT_MS = 1_200L;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private Messenger watcherMessenger;
    private boolean watcherBound;
    private boolean clickPending;
    private boolean stateHandled;

    private final Messenger incomingMessenger = new Messenger(
            new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    if (message.what == WatcherIpc.MSG_STATE_CHANGED) {
                        handleSessionState(message.arg1);
                        return;
                    }
                    super.handleMessage(message);
                }
            });

    private final ServiceConnection watcherConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            watcherMessenger = new Messenger(binder);
            watcherBound = true;
            sendWatcherMessage(WatcherIpc.MSG_REGISTER_CLIENT, true);
            sendWatcherMessage(WatcherIpc.MSG_REQUEST_STATE, false);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            watcherBound = false;
            watcherMessenger = null;
            if (clickPending) {
                handleSessionState(WatcherIpc.STATE_STOPPED);
            } else {
                renderTile(WatcherIpc.STATE_STOPPED);
            }
        }
    };

    @Override
    public void onStartListening() {
        super.onStartListening();
        querySession(false);
    }

    @Override
    public void onClick() {
        super.onClick();
        querySession(true);
    }

    @Override
    public void onStopListening() {
        unbindWatcher();
        super.onStopListening();
    }

    @Override
    public void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        unbindWatcher();
        super.onDestroy();
    }

    private void querySession(boolean fromClick) {
        if (!getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).getBoolean(
                MainActivity.PREF_BETA_QUICK_SETTINGS_LOG_RECOVERY,
                MainActivity.DEFAULT_BETA_QUICK_SETTINGS_LOG_RECOVERY)) {
            renderTile(WatcherIpc.STATE_STOPPED);
            return;
        }

        clickPending |= fromClick;
        stateHandled = false;
        if (!watcherBound) {
            try {
                bindService(
                        new Intent(this, HomeHoldService.class),
                        watcherConnection,
                        Context.BIND_AUTO_CREATE);
            } catch (Throwable ignored) {
                if (clickPending) handleSessionState(WatcherIpc.STATE_STOPPED);
                else renderTile(WatcherIpc.STATE_STOPPED);
                return;
            }
        } else {
            sendWatcherMessage(WatcherIpc.MSG_REQUEST_STATE, false);
        }

        ui.removeCallbacksAndMessages(null);
        ui.postDelayed(() -> {
            if (!stateHandled) {
                if (clickPending) handleSessionState(WatcherIpc.STATE_STOPPED);
                else renderTile(WatcherIpc.STATE_STOPPED);
            }
        }, STATE_TIMEOUT_MS);
    }

    private void handleSessionState(int state) {
        if (stateHandled) return;
        stateHandled = true;
        ui.removeCallbacksAndMessages(null);
        renderTile(state);

        boolean recoverable = state == WatcherIpc.STATE_NEEDS_RECONNECT
                || state == WatcherIpc.STATE_STOPPED;
        boolean shouldRecover = clickPending && recoverable;
        clickPending = false;

        if (shouldRecover) {
            ensureWatcherAndOpenBridge();
        }
    }

    private void ensureWatcherAndOpenBridge() {
        try {
            Intent ensure = new Intent(this, HomeHoldService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(ensure);
            } else {
                startService(ensure);
            }
        } catch (Throwable ignored) {
            // The bridge can still bind to an existing watcher below.
        }

        ui.postDelayed(this::openLogSessionBridge, 140L);
    }

    private void openLogSessionBridge() {
        Intent bridge = new Intent(this, LogSessionBridgeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                PendingIntent pending = PendingIntent.getActivity(
                        this,
                        0,
                        bridge,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pending);
            } else {
                //noinspection deprecation
                startActivityAndCollapse(bridge);
            }
        } catch (Throwable ignored) {
            // A user tap may still be subject to an OEM Quick Settings policy.
        }
    }

    private void renderTile(int state) {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean online = state == WatcherIpc.STATE_ACTIVE
                || state == WatcherIpc.STATE_CONNECTING;
        tile.setState(online ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(UiText.tr(
                this,
                online ? "Log session active" : "Restore log session"));
        tile.updateTile();
    }

    private void sendWatcherMessage(int what, boolean includeReplyTo) {
        if (!watcherBound || watcherMessenger == null) return;
        Message message = Message.obtain(null, what);
        if (includeReplyTo) message.replyTo = incomingMessenger;
        try {
            watcherMessenger.send(message);
        } catch (RemoteException ignored) {
            watcherBound = false;
            watcherMessenger = null;
        }
    }

    private void unbindWatcher() {
        if (!watcherBound) return;
        sendWatcherMessage(WatcherIpc.MSG_UNREGISTER_CLIENT, false);
        try {
            unbindService(watcherConnection);
        } catch (Throwable ignored) {
        }
        watcherBound = false;
        watcherMessenger = null;
    }
}
