// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

import android.app.*;
import android.content.*;
import android.os.Bundle;

/** Reply directly from Settings so Android does not close its pairing server. */
public final class WirelessPairingReceiver extends BroadcastReceiver {
    private static final int ID = 7044;
    private static final String CHANNEL = "wireless_pairing";
    static void show(Context c, int port) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "ColorGoogle wireless pairing", NotificationManager.IMPORTANCE_HIGH));
        Intent intent = new Intent(c, WirelessPairingReceiver.class).putExtra("port", port);
        PendingIntent reply = PendingIntent.getBroadcast(c, ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        RemoteInput input = new RemoteInput.Builder("code").setLabel("Enter 6-digit pairing code").build();
        Notification.Action action = new Notification.Action.Builder(null, "Enter pairing code", reply)
                .addRemoteInput(input).setSemanticAction(Notification.Action.SEMANTIC_ACTION_REPLY).build();
        nm.notify(ID, new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_watch)
                .setContentTitle("ColorGoogle: wireless debugging pairing code")
                .setContentText("Keep the system pairing window open and reply here with its 6-digit code")
                .setOngoing(true).setOnlyAlertOnce(true).addAction(action).build());
    }
    static void clear(Context c) { c.getSystemService(NotificationManager.class).cancel(ID); }
    @Override public void onReceive(Context context, Intent intent) {
        Bundle reply = RemoteInput.getResultsFromIntent(intent);
        if (reply == null) return;
        CharSequence code = reply.getCharSequence("code");
        Bundle data = new Bundle(); data.putInt("port", intent.getIntExtra("port", 0));
        data.putString("code", code == null ? "" : code.toString().trim());
        WirelessRecovery.get(context).command("pair", data);
    }
}
