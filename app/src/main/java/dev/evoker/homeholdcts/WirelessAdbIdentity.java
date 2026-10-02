// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

import android.content.Context;
import android.os.Build;
import android.util.AtomicFile;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import io.github.muntashirakon.adb.AbsAdbConnectionManager;

/** One locally generated identity, never imported from the computer. Watcher owns this file. */
final class WirelessAdbIdentity extends AbsAdbConnectionManager {
    private final PrivateKey key;
    private final Certificate certificate;
    WirelessAdbIdentity(Context context) throws Exception {
        AtomicFile file = new AtomicFile(new File(context.getNoBackupFilesDir(), "wireless-adb-identity"));
        if (!file.getBaseFile().exists()) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            java.security.KeyPair pair = generator.generateKeyPair();
            long now = System.currentTimeMillis();
            byte[] cert = new JcaX509v3CertificateBuilder(new X500Name("CN=ColorGoogle"),
                    new BigInteger(128, new SecureRandom()), new Date(now - 86400000L),
                    new Date(now + 10L * 365 * 86400000L), new X500Name("CN=ColorGoogle"), pair.getPublic())
                    .build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())).getEncoded();
            FileOutputStream raw = file.startWrite();
            try {
                DataOutputStream out = new DataOutputStream(raw);
                byte[] privateBytes = pair.getPrivate().getEncoded();
                out.writeInt(privateBytes.length); out.write(privateBytes);
                out.writeInt(cert.length); out.write(cert); out.flush();
                file.finishWrite(raw);
            } catch (Exception e) { file.failWrite(raw); throw e; }
        }
        try (DataInputStream in = new DataInputStream(file.openRead())) {
            key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(readBlob(in)));
            certificate = CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(readBlob(in)));
        }
        setApi(Build.VERSION.SDK_INT);
        setHostAddress("127.0.0.1");
        setTimeout(12, TimeUnit.SECONDS);
        setThrowOnUnauthorised(true);
    }
    private static byte[] readBlob(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n < 1 || n > 16384) throw new IOException("Invalid local ADB identity");
        byte[] data = new byte[n]; in.readFully(data); return data;
    }
    @Override protected PrivateKey getPrivateKey() { return key; }
    @Override protected Certificate getCertificate() { return certificate; }
    @Override protected String getDeviceName() { return "ColorGoogle"; }
    static void forget(Context context) {
        new AtomicFile(new File(context.getNoBackupFilesDir(), "wireless-adb-identity")).delete();
    }
}
