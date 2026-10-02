// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

/** Shell-only bootstrap. Normal apps cannot install a privileged Binder here. */
public final class DesktopSearchBridge extends ContentProvider {
    static final String AUTHORITY = "dev.evoker.homeholdcts.desktopsearch";
    static final String PREF = "desktop_search_google";
    static volatile IDesktopSearchAgent agent;
    private static IBinder lastController;
    private static String lastControllerIdentity = "";

    @Override public boolean onCreate() { return true; }

    @Override public synchronized Bundle call(String method, String arg, Bundle extras) {
        int uid = Binder.getCallingUid();
        if (uid != 2000) throw new SecurityException("ADB shell required");
        Bundle out = new Bundle();
        try {
            if ("attach".equals(method)) {
                IBinder binder = extras == null ? null : extras.getBinder("agent");
                if (binder == null) throw new IllegalArgumentException("Missing agent");
                IDesktopSearchAgent next = IDesktopSearchAgent.Stub.asInterface(binder);
                binder.linkToDeath(() -> { if (agent == next) agent = null; }, 0);
                agent = next;
                out.putBoolean("enabled", getContext().getSharedPreferences(
                        MainActivity.PREFS, 0).getBoolean(PREF, false));
            } else if ("remember_controller".equals(method)) {
                lastController = extras == null ? null : extras.getBinder("controller");
                lastControllerIdentity = extras == null ? "" : extras.getString("identity", "");
                out.putBoolean("saved", true);
            } else if ("dead_controller".equals(method)) {
                // isBinderAlive alone may be cached until this particular proxy transacts.
                if (lastController != null && !lastController.pingBinder())
                    out.putString("identity", lastControllerIdentity);
            } else if ("status".equals(method)) {
                if (agent == null) out.putString("error", "Agent not connected");
                else return agent.getStatus();
            } else if ("stop".equals(method)) {
                if (agent != null) agent.shutdown();
                out.putBoolean("stopped", true);
            } else throw new IllegalArgumentException("Unknown method");
        } catch (android.os.RemoteException e) {
            throw new IllegalStateException("Agent disconnected", e);
        }
        return out;
    }

    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
