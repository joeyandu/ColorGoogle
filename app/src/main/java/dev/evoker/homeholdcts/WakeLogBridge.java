// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

/** Runs in :watcher. Only ADB shell may supply the log-source Binder. */
public final class WakeLogBridge extends ContentProvider {
    static final String AUTHORITY = "dev.evoker.homeholdcts.wakelog";
    static volatile IDesktopSearchAgent agent;
    static boolean isSelected(Context c) {
        return c.getSharedPreferences("adb_log_source", 0).getBoolean("selected", false);
    }
    @Override public boolean onCreate() { return true; }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (Binder.getCallingUid() != 2000) throw new SecurityException("ADB shell required");
        if (!"attach".equals(method)) throw new IllegalArgumentException("Unknown method");
        IBinder binder = extras == null ? null : extras.getBinder("agent");
        if (binder == null) throw new IllegalArgumentException("Missing agent");
        IDesktopSearchAgent next = IDesktopSearchAgent.Stub.asInterface(binder);
        try {
            binder.linkToDeath(() -> {
                if (agent == next) {
                    agent = null;
                    HomeHoldService.adbAgentChanged();
                    WirelessRecovery.get(getContext()).agentChanged();
                }
            }, 0);
        } catch (android.os.RemoteException e) { throw new IllegalStateException(e); }
        // Separate preferences file owned only by :watcher, avoiding cross-process overwrites.
        if (!getContext().getSharedPreferences("adb_log_source", 0).edit()
                .putBoolean("selected", true).commit()) throw new IllegalStateException("Cannot save log source");
        agent = next;
        HomeHoldService.adbAgentChanged();
        WirelessRecovery.get(getContext()).agentChanged();
        if (getContext().getSharedPreferences(MainActivity.PREFS, 0)
                .getBoolean(MainActivity.PREF_ENABLED, false)) {
            getContext().startForegroundService(new Intent(getContext(), HomeHoldService.class));
        }
        Bundle result = new Bundle();
        result.putBoolean("attached", true);
        return result;
    }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
