// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.*;
import android.net.nsd.*;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import io.github.muntashirakon.adb.AdbStream;

/** Lives in the existing :watcher process, outside adbd's cgroup. All state belongs to main. */
final class WirelessRecovery {
    private static final String TAG = "ColorGoogleRecovery";
    private static WirelessRecovery instance;
    static synchronized WirelessRecovery get(Context context) {
        if (instance == null) instance = new WirelessRecovery(context.getApplicationContext());
        return instance;
    }
    private final Context context;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final RecoveryAttempts retries = new RecoveryAttempts();
    private final NsdManager nsd;
    private volatile String status = "Not enabled";
    private volatile boolean busy;
    private boolean running, registered, pairingMode;
    private volatile int generation;
    private int port;
    private NsdManager.DiscoveryListener discovery;
    private final Runnable retry = this::evaluate;
    private final ConnectivityManager.NetworkCallback network = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network n) { main.post(() -> { retries.reset(); evaluate(); }); }
        @Override public void onLost(Network n) { main.post(WirelessRecovery.this::evaluate); }
    };
    private final ContentObserver adbSetting = new ContentObserver(main) {
        @Override public void onChange(boolean selfChange) { retries.reset(); evaluate(); }
    };

    private WirelessRecovery(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("wireless_recovery", 0);
        nsd = context.getSystemService(NsdManager.class);
    }
    Bundle command(String method, Bundle data) {
        if (!"status".equals(method)) {
            Bundle copy = new Bundle(data);
            main.post(() -> {
                switch (method) {
                    case "enable":
                        prefs.edit().putBoolean("enabled", copy.getBoolean("enabled")).apply();
                        cancelAttempt(); retries.reset(); evaluate(); break;
                    case "retry": retries.reset(); evaluate(); break;
                    case "prepare_pair":
                        if (busy) break;
                        stopDiscovery(); pairingMode = true;
                        final int pairingGeneration = ++generation;
                        setStatus("Open Wireless debugging, choose Pair device with pairing code, then reply to the notification with the 6-digit code.");
                        startDiscovery();
                        main.postDelayed(() -> {
                            if (generation == pairingGeneration && pairingMode && !busy) { pairingMode = false; stopDiscovery(); WirelessPairingReceiver.clear(context); evaluate(); }
                        }, 300000);
                        break;
                    case "pair": pair(copy.getInt("port"), copy.getString("code", "")); break;
                    case "forget":
                        if (busy) { setStatus("An operation is still running. Wait before removing pairing."); break; }
                        prefs.edit().putBoolean("enabled", false).putBoolean("paired", false).apply();
                        cancelAttempt(); WirelessAdbIdentity.forget(context); evaluate(); break;
                    default: setStatus("Unsupported operation");
                }
            });
        }
        Bundle out = new Bundle();
        out.putBoolean("enabled", prefs.getBoolean("enabled", false));
        out.putBoolean("paired", prefs.getBoolean("paired", false));
        out.putBoolean("busy", busy);
        out.putString("status", status);
        out.putInt("recoveries", prefs.getInt("recoveries", 0));
        return out;
    }
    void watcherRunning(boolean value) {
        main.post(() -> { running = value; if (!value) cancelAttempt(); evaluate(); });
    }
    void agentChanged() { main.post(this::evaluate); }
    private boolean online() {
        IDesktopSearchAgent agent = WakeLogBridge.agent;
        return agent != null && agent.asBinder().isBinderAlive();
    }
    private void setStatus(String value) {
        if (!value.equals(status)) { status = value; Log.i(TAG, value); }
    }
    private void observe(boolean enabled) {
        if (enabled == registered) return;
        registered = enabled;
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        if (enabled) {
            cm.registerDefaultNetworkCallback(network);
            context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("adb_wifi_enabled"), false, adbSetting);
        } else {
            cm.unregisterNetworkCallback(network);
            context.getContentResolver().unregisterContentObserver(adbSetting);
        }
    }
    private void evaluate() {
        main.removeCallbacks(retry);
        if (pairingMode) return;
        boolean enabled = prefs.getBoolean("enabled", false);
        observe(enabled && running);
        if (!enabled || !running) {
            stopDiscovery(); setStatus(!enabled ? "Automatic recovery disabled" : "Main service stopped; automatic activation is disabled"); return;
        }
        if (busy) return;
        if (!prefs.getBoolean("paired", false)) { stopDiscovery(); setStatus("Pair with wireless debugging first"); return; }
        if (online()) { retries.reset(); stopDiscovery(); setStatus("Agent connected · Automatic recovery ready"); return; }
        if (Settings.Global.getInt(context.getContentResolver(), "adb_wifi_enabled", 0) != 1) {
            stopDiscovery(); setStatus("Agent stopped; enable system wireless debugging"); return;
        }
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            stopDiscovery(); setStatus("Waiting for Wi-Fi; local wireless debugging is unavailable"); return;
        }
        if (discovery == null) startDiscovery();
        if (port == 0) { setStatus("Discovering local wireless ADB; keep wireless debugging enabled"); return; }
        if (retries.exhausted()) { setStatus("Recovery failed 3 times. Check pairing, then tap Retry recovery."); return; }
        if (!retries.begin(SystemClock.elapsedRealtime())) {
            main.postDelayed(retry, retries.delay(SystemClock.elapsedRealtime())); return;
        }
        restore(port);
    }
    private void startDiscovery() {
        final boolean forPairing = pairingMode;
        final NsdManager.DiscoveryListener listener = new NsdManager.DiscoveryListener() {
            @Override public void onDiscoveryStarted(String type) { }
            @Override public void onDiscoveryStopped(String type) { }
            @Override public void onStopDiscoveryFailed(String type, int error) { Log.w(TAG, "Discovery stop " + error); }
            @Override public void onStartDiscoveryFailed(String type, int error) {
                main.post(() -> { if (discovery == this) { discovery = null; setStatus("Wireless ADB discovery failed: " + error + "; please retry"); } });
            }
            @Override public void onServiceLost(NsdServiceInfo service) { }
            @Override public void onServiceFound(NsdServiceInfo service) {
                final NsdManager.DiscoveryListener owner = this;
                nsd.resolveService(service, new NsdManager.ResolveListener() {
                    @Override public void onResolveFailed(NsdServiceInfo info, int error) { Log.w(TAG, "ADB resolve " + error); }
                    @Override public void onServiceResolved(NsdServiceInfo info) {
                        main.post(() -> {
                            if (discovery != owner) return;
                            try {
                                // Never connect to another computer/phone discovered on the LAN.
                                if (info.getHost() == null || (!info.getHost().isLoopbackAddress()
                                        && NetworkInterface.getByInetAddress(info.getHost()) == null)) return;
                                if (forPairing) {
                                    port = info.getPort();
                                    WirelessPairingReceiver.show(context, port);
                                    setStatus("Local pairing window found. Enter its code in the ColorGoogle notification.");
                                } else { port = info.getPort(); retries.endpoint(port); evaluate(); }
                            } catch (Exception e) { setStatus("Unable to verify the local ADB address"); }
                        });
                    }
                });
            }
        };
        discovery = listener;
        nsd.discoverServices(forPairing ? "_adb-tls-pairing._tcp." : "_adb-tls-connect._tcp.", NsdManager.PROTOCOL_DNS_SD, listener);
    }
    private void stopDiscovery() {
        NsdManager.DiscoveryListener old = discovery;
        discovery = null; port = 0;
        if (old != null) {
            try { nsd.stopServiceDiscovery(old); } catch (IllegalArgumentException e) { Log.w(TAG, "Discovery already stopped"); }
        }
    }
    private void cancelAttempt() {
        generation++; pairingMode = false; main.removeCallbacks(retry); stopDiscovery();
        WirelessPairingReceiver.clear(context);
        // Do not block the UI waiting for a worker's connect lock. Socket IO has fixed timeouts.
    }
    private void pair(int pairingPort, String code) {
        if (busy) return;
        if (pairingPort < 1 || pairingPort > 65535 || !code.matches("[0-9]{6}")) {
            setStatus("Enter the current pairing port and 6-digit code"); return;
        }
        if (!pairingMode || port != pairingPort) { setStatus("Pairing window changed. Start pairing again."); return; }
        busy = true; int token = ++generation;
        pairingMode = false; stopDiscovery(); WirelessPairingReceiver.clear(context);
        setStatus("Pairing with local wireless debugging…");
        io.execute(() -> {
            String failure = null;
            try (WirelessAdbIdentity identity = new WirelessAdbIdentity(context)) {
                identity.pair("127.0.0.1", pairingPort, code);
            } catch (Exception e) { failure = e.getClass().getSimpleName(); Log.w(TAG, "Local pairing failed", e); }
            final String result = failure;
            main.post(() -> {
                busy = false;
                if (token != generation) { evaluate(); return; }
                if (result != null) { setStatus("Pairing failed: " + result + "; reopen the pairing window and try again"); return; }
                prefs.edit().putBoolean("paired", true).apply(); retries.reset();
                setStatus("Local pairing successful"); evaluate();
            });
        });
    }
    private void restore(int connectPort) {
        busy = true; final int token = ++generation;
        setStatus("Agent stopped; reactivating automatically…");
        io.execute(() -> {
            String failure = null;
            try (WirelessAdbIdentity identity = new WirelessAdbIdentity(context)) {
                if (!identity.connect("127.0.0.1", connectPort)) throw new java.io.IOException("ADB connection rejected");
                if (token != generation || online()) return;
                try (AdbStream stream = identity.openStream("shell:" + DesktopSearchActivation.START_SCRIPT)) {
                    byte[] buffer = new byte[4096]; int count, total = 0;
                    while ((count = stream.read(buffer, 0, buffer.length)) >= 0) {
                        total += count;
                        if (total > 65536) throw new java.io.IOException("Unexpected activation output");
                        Log.i(TAG, "Activation: " + new String(buffer, 0, count, StandardCharsets.UTF_8).trim());
                    }
                }
            } catch (Exception e) { failure = e.getClass().getSimpleName(); Log.w(TAG, "Local ADB activation failed", e); }
            finally {
                final String result = failure;
                main.post(() -> {
                    busy = false;
                    if (token != generation) { evaluate(); return; }
                    if (online()) {
                        prefs.edit().putInt("recoveries", prefs.getInt("recoveries", 0) + 1).apply();
                        Log.i(TAG, "RECOVERY_ATTACHED generation=" + token);
                        evaluate();
                    } else {
                        setStatus("Local ADB activation did not complete" + (result == null ? "" : ": " + result));
                        main.postDelayed(retry, Math.max(3000, retries.delay(SystemClock.elapsedRealtime())));
                    }
                });
            }
        });
    }
}
