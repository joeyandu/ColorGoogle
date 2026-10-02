// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * V3 in-process logd reader.
 *
 * This intentionally does not execute /system/bin/logcat. The watcher process
 * opens logd's reserved SOCK_SEQPACKET reader socket directly and speaks the
 * same streaming protocol used by Android liblog.
 *
 * Consequences:
 *  - no child/phantom process;
 *  - no stdout pipe owned by a child process;
 *  - the privileged-log session belongs to the :watcher PID itself;
 *  - closing the LocalSocket from another thread interrupts a blocked read.
 */
final class DirectLogdReader implements Closeable {

    private static final int LOGGER_ENTRY_MAX_LEN = 5 * 1024;
    private static final int LOGGER_ENTRY_V4_SIZE = 28;

    // Text buffers used by the old `logcat -b all` trigger path.
    // 0=main, 1=radio, 3=system, 4=crash. Event/stats/security buffers are
    // binary or irrelevant to the trigger classifier, so do not stream them.
    private static final String LOGD_COMMAND =
            "stream lids=0,1,3,4 tail=1";

    private final Object socketLock = new Object();
    private final byte[] packet = new byte[LOGGER_ENTRY_MAX_LEN + 1];

    private LocalSocket socket;
    private InputStream input;
    private int lastEntryUid = -1;

    int getLastEntryUid() { return lastEntryUid; }

    void open() throws IOException {
        LocalSocket created = new LocalSocket(LocalSocket.SOCKET_SEQPACKET);

        try {
            created.connect(
                    new LocalSocketAddress(
                            "logdr",
                            LocalSocketAddress.Namespace.RESERVED));

            created.setReceiveBufferSize(64 * 1024);

            OutputStream output = created.getOutputStream();
            output.write(LOGD_COMMAND.getBytes(StandardCharsets.US_ASCII));
            output.flush();

            InputStream createdInput = created.getInputStream();

            synchronized (socketLock) {
                socket = created;
                input = createdInput;
            }
        } catch (Throwable t) {
            try {
                created.close();
            } catch (Throwable ignored) {
            }

            if (t instanceof IOException) {
                throw (IOException) t;
            }

            throw new IOException("Unable to open direct logd reader", t);
        }
    }

    /**
     * Blocks until a relevant line or foreign-UID health proof is available.
     * Empty strings prove health without exposing unrelated log content.
     * Returns null only when logd closes the stream cleanly.
     */
    String readRelevantLine() throws IOException {
        while (true) {
            InputStream current;

            synchronized (socketLock) {
                current = input;
            }

            if (current == null) {
                return null;
            }

            int count = current.read(packet, 0, LOGGER_ENTRY_MAX_LEN);
            if (count < 0) {
                return null;
            }

            String line = decodeSessionEntry(packet, count, android.os.Process.myUid());
            if (line != null) {
                lastEntryUid = entryUid(packet, count);
                return line;
            }
        }
    }

    @Override
    public void close() {
        LocalSocket toClose;

        synchronized (socketLock) {
            toClose = socket;
            socket = null;
            input = null;
        }

        if (toClose != null) {
            try {
                toClose.close();
            } catch (Throwable ignored) {
            }
        }
    }

    static String decodeSessionEntry(byte[] data, int count, int readerUid) {
        String relevant = decodeRelevantTextEntry(data, count);
        if (relevant != null) return relevant;
        int uid = entryUid(data, count);
        // logd supplies the packet UID. Any complete foreign-UID packet proves
        // device-log access; do not wait for a user trigger to establish health.
        return uid >= 0 && uid != readerUid ? "" : null;
    }

    static int entryUid(byte[] data, int packetLength) {
        if (packetLength < LOGGER_ENTRY_V4_SIZE || packetLength > data.length) return -1;
        int header = readUnsignedShortLE(data, 2);
        int payload = readUnsignedShortLE(data, 0);
        if (header < LOGGER_ENTRY_V4_SIZE || header + payload > packetLength) return -1;
        return readIntLE(data, 24);
    }

    static String decodeRelevantTextEntry(byte[] data, int packetLength) {
        if (packetLength < LOGGER_ENTRY_V4_SIZE) {
            return null;
        }

        int payloadLength = readUnsignedShortLE(data, 0);
        int headerSize = readUnsignedShortLE(data, 2);

        if (headerSize < 20
                || headerSize >= packetLength
                || payloadLength <= 1
                || headerSize + payloadLength > packetLength) {
            return null;
        }

        // logger_entry_v4.lid lives at offset 20. On the Android versions
        // targeted by this project the header is v4 (28 bytes).
        int logId = headerSize >= 24
                ? readIntLE(data, 20)
                : -1;

        if (logId != 0 && logId != 1 && logId != 3 && logId != 4) {
            return null;
        }

        int payloadStart = headerSize;
        int payloadEnd = headerSize + payloadLength;

        // Text payload: [priority byte][tag\0][message\0]
        int tagStart = payloadStart + 1;
        if (tagStart >= payloadEnd) {
            return null;
        }

        int tagEnd = findZero(data, tagStart, payloadEnd);
        if (tagEnd <= tagStart) {
            return null;
        }

        String tag = new String(
                data,
                tagStart,
                tagEnd - tagStart,
                StandardCharsets.UTF_8);

        if (!isRelevantTag(tag)) {
            return null;
        }

        int messageStart = tagEnd + 1;
        if (messageStart >= payloadEnd) {
            return tag + ":";
        }

        int messageEnd = findZero(data, messageStart, payloadEnd);
        if (messageEnd < messageStart) {
            messageEnd = payloadEnd;
        }

        String message = new String(
                data,
                messageStart,
                Math.max(0, messageEnd - messageStart),
                StandardCharsets.UTF_8);

        return tag + ": " + message;
    }

    private static boolean isRelevantTag(String tag) {
        return "ActivityManager".equals(tag)
                || "KEYLOG_SingleKeyGesture".equals(tag)
                || "KEYLOG_SinglePowerKeyMonitor".equals(tag)
                || "OVMS-OVMS_StartSpeechAssist".equals(tag)
                || "HomeHoldCTS".equals(tag);
    }

    private static int findZero(byte[] data, int start, int end) {
        for (int i = start; i < end; i++) {
            if (data[i] == 0) {
                return i;
            }
        }
        return end;
    }

    private static int readUnsignedShortLE(byte[] data, int offset) {
        return (data[offset] & 0xff)
                | ((data[offset + 1] & 0xff) << 8);
    }

    private static int readIntLE(byte[] data, int offset) {
        return (data[offset] & 0xff)
                | ((data[offset + 1] & 0xff) << 8)
                | ((data[offset + 2] & 0xff) << 16)
                | ((data[offset + 3] & 0xff) << 24);
    }
}
