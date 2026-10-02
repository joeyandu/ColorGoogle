// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.os.SystemClock;
import android.util.Log;

import java.io.Closeable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * V5 low-overhead in-process logd session.
 *
 * Exactly one SOCK_SEQPACKET connection is kept from :watcher to logd. There
 * is no /system/bin/logcat process, no phantom child and no stdout pipe.
 *
 * A sequence heartbeat proves that the socket/reader is still moving. The
 * heartbeat uses fixed delay (not fixed rate), so a ColorOS freeze/thaw does
 * not generate a catch-up storm or falsely condemn a socket merely because
 * the entire process was suspended for a long time.
 *
 * If the direct socket really dies, EOF/exception terminates immediately. A
 * silent stall must miss three heartbeat proofs before HomeHoldService uses its
 * user-present/TOP recovery path. We intentionally do not respawn a fresh log
 * reader silently hours later because Android may require a new log-access
 * confirmation for a new reader request.
 */
final class SingleDirectLogdSession implements Closeable {

    interface Listener {
        void onActive();
        void onLine(String line);
        void onAllLanesLost(String reason);
    }

    private static final String TAG = "HomeHoldCTS";
    private static final String HEARTBEAT_PREFIX =
            "MINDTRIGGER_DIRECT_LOGD_HEARTBEAT_";
    private static final long HEARTBEAT_PERIOD_MS = 30_000L;
    private static final long HEARTBEAT_SETTLE_MS = 12_000L;
    private static final int MISSES_BEFORE_FAILURE = 3;
    // A connected socket that never yields its first packet cannot be
    // diagnosed by the heartbeat path, because activation has not happened.
    // Keep this one-shot guard long enough for Android's log-access dialog.
    private static final long ACTIVATION_TIMEOUT_MS = 35_000L;

    private final Listener listener;
    private final ExecutorService readerExecutor =
            Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService healthExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private final CountDownLatch terminated = new CountDownLatch(1);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean activeAnnounced = new AtomicBoolean(false);
    private final AtomicBoolean failureAnnounced = new AtomicBoolean(false);
    private final AtomicLong heartbeatSeq = new AtomicLong(0L);

    private volatile DirectLogdReader reader;
    private volatile long lastHeartbeatSeq;
    private volatile int consecutiveMisses;

    SingleDirectLogdSession(Listener listener) {
        this.listener = listener;
    }

    void start() {
        readerExecutor.execute(this::runReader);

        healthExecutor.schedule(
                this::emitHeartbeat,
                350L,
                TimeUnit.MILLISECONDS);

        healthExecutor.scheduleWithFixedDelay(
                this::emitHeartbeat,
                1_500L,
                HEARTBEAT_PERIOD_MS,
                TimeUnit.MILLISECONDS);

        healthExecutor.schedule(
                this::checkActivationTimeout,
                ACTIVATION_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
    }

    boolean wasEverActive() {
        return activeAnnounced.get();
    }

    void awaitTermination() throws InterruptedException {
        terminated.await();
    }

    private void runReader() {
        DirectLogdReader local = new DirectLogdReader();
        reader = local;

        try {
            local.open();

            while (!closed.get()) {
                String line = local.readRelevantLine();
                if (line == null) {
                    failSession("direct-logd-eof");
                    return;
                }

                int sourceUid = local.getLastEntryUid();
                boolean ownLog = sourceUid == android.os.Process.myUid();
                long heartbeat = parseHeartbeat(line);
                if (ownLog && heartbeat > 0L) {
                    lastHeartbeatSeq = Math.max(lastHeartbeatSeq, heartbeat);
                    consecutiveMisses = 0;
                    // Own-UID logs are readable even when Android denies device logs.
                    // This proves socket liveness only, not privileged access.
                    continue;
                }

                if (sourceUid < 0 || ownLog) continue;
                announceActiveOnce();
                if (listener != null && !line.isEmpty()) {
                    listener.onLine(line);
                }
            }
        } catch (Throwable t) {
            if (!closed.get()) {
                Log.w(TAG, "Single direct-logd reader ended", t);
                failSession("direct-logd-exception");
            }
        } finally {
            reader = null;
            try {
                local.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private void announceActiveOnce() {
        if (activeAnnounced.compareAndSet(false, true) && listener != null) {
            Log.i(TAG, "External device-log UID verified for this session");
            listener.onActive();
        }
    }

    private void emitHeartbeat() {
        if (closed.get()) {
            return;
        }

        long seq = heartbeatSeq.incrementAndGet();
        Log.i(TAG, HEARTBEAT_PREFIX + seq);

        healthExecutor.schedule(
                () -> validateHeartbeat(seq),
                HEARTBEAT_SETTLE_MS,
                TimeUnit.MILLISECONDS);
    }

    private void validateHeartbeat(long seq) {
        if (closed.get() || !activeAnnounced.get()) {
            // Before Android grants the session, do not manufacture a timeout.
            // Denial/closure is handled by the reader path itself.
            return;
        }

        if (lastHeartbeatSeq >= seq) {
            consecutiveMisses = 0;
            return;
        }

        consecutiveMisses++;
        if (consecutiveMisses >= MISSES_BEFORE_FAILURE) {
            failSession("direct-logd-heartbeat-stalled");
        }
    }

    /**
     * Fires once after ACTIVATION_TIMEOUT_MS. If the session has still not
     * received data, close the zombie socket so the existing bridge recovery
     * path can request a new log-access session.
     */
    private void checkActivationTimeout() {
        if (closed.get() || activeAnnounced.get()) {
            return;
        }

        Log.w(TAG, "Direct logd session never activated within "
                + ACTIVATION_TIMEOUT_MS + " ms; zombie socket, failing");
        failSession("activation-timeout");
    }

    private void failSession(String reason) {
        if (!failureAnnounced.compareAndSet(false, true)) {
            return;
        }

        if (listener != null) {
            listener.onAllLanesLost(reason);
        }
        close();
    }

    private static long parseHeartbeat(String line) {
        int p = line.indexOf(HEARTBEAT_PREFIX);
        if (p < 0) {
            return -1L;
        }

        p += HEARTBEAT_PREFIX.length();
        int end = p;
        while (end < line.length()) {
            char c = line.charAt(end);
            if (c < '0' || c > '9') {
                break;
            }
            end++;
        }

        if (end <= p) {
            return -1L;
        }

        try {
            return Long.parseLong(line.substring(p, end));
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        DirectLogdReader local = reader;
        if (local != null) {
            try {
                local.close();
            } catch (Throwable ignored) {
            }
        }

        readerExecutor.shutdownNow();
        healthExecutor.shutdownNow();
        terminated.countDown();
    }
}
