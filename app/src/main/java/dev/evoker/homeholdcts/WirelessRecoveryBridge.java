// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;

/** Internal UI IPC into :watcher; no external pairing or command endpoint. */
public final class WirelessRecoveryBridge extends ContentProvider {
    static final Uri URI = Uri.parse("content://dev.evoker.homeholdcts.wireless");
    @Override public boolean onCreate() { return true; }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (Binder.getCallingUid() != android.os.Process.myUid()) throw new SecurityException("Same application required");
        return WirelessRecovery.get(getContext()).command(method, extras == null ? Bundle.EMPTY : extras);
    }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
