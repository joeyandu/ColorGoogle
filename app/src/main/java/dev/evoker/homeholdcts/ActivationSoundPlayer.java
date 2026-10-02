// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-27.

package dev.evoker.homeholdcts;

import android.content.Context;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

final class ActivationSoundPlayer {

    private static final String TAG = "HomeHoldCTS";
    private static final long MAX_CLIP_MS = 3000L;
    private static final long MAX_CLIP_US = MAX_CLIP_MS * 1000L;
    private static final int MAX_FALLBACK_BYTES = 24 * 1024 * 1024;

    private static final String FILE_CTS = "activation_cts_custom.audio";
    private static final String FILE_ASSISTANT = "activation_assistant_custom.audio";
    private static final String PREF_CTS_NAME = "beta_activation_cts_name";
    private static final String PREF_ASSISTANT_NAME = "beta_activation_assistant_name";

    static final class ImportResult {
        final boolean success;
        final boolean physicallyClipped;

        ImportResult(boolean success, boolean physicallyClipped) {
            this.success = success;
            this.physicallyClipped = physicallyClipped;
        }
    }

    private ActivationSoundPlayer() {}

    static void playCts(Context context) {
        play(context, R.raw.aura_cts, false);
    }

    static void playAssistant(Context context) {
        play(context, R.raw.aura_gemini, true);
    }

    static boolean hasCustomClip(Context context, boolean assistant) {
        File file = customFile(context, assistant);
        return file.isFile() && file.length() > 0L;
    }

    static String describeCustomClip(Context context, boolean assistant) {
        if (!hasCustomClip(context, assistant)) {
            return UiText.tr(context, "Bundled sound · max 3 seconds");
        }
        String name = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .getString(assistant ? PREF_ASSISTANT_NAME : PREF_CTS_NAME,
                        UiText.tr(context, "Custom sound"));
        return name + " · " + UiText.tr(context, "max 3 seconds");
    }

    static void resetCustomClip(Context context, boolean assistant) {
        try {
            File file = customFile(context, assistant);
            if (file.exists()) file.delete();
        } catch (Throwable ignored) {
        }
        context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(assistant ? PREF_ASSISTANT_NAME : PREF_CTS_NAME)
                .apply();
    }

    static ImportResult importCustomClip(Context context, Uri uri, boolean assistant) {
        if (context == null || uri == null) return new ImportResult(false, false);

        File target = customFile(context, assistant);
        File tempMux = new File(context.getFilesDir(), target.getName() + ".tmp.mp4");
        File tempCopy = new File(context.getFilesDir(), target.getName() + ".tmp.copy");
        if (tempMux.exists()) tempMux.delete();
        if (tempCopy.exists()) tempCopy.delete();

        boolean clipped = false;
        try {
            clipped = muxFirstThreeSeconds(context, uri, tempMux);
            if (clipped && tempMux.isFile() && tempMux.length() > 0L) {
                replaceFile(tempMux, target);
            } else {
                if (tempMux.exists()) tempMux.delete();
                // Some audio codecs/containers cannot be remuxed by MediaMuxer.
                // Import atomically into a temporary file, then enforce the same
                // 3-second cap at playback time.
                copyBounded(context, uri, tempCopy);
                replaceFile(tempCopy, target);
            }

            if (!target.isFile() || target.length() <= 0L) {
                return new ImportResult(false, clipped);
            }

            String name = displayName(context, uri);
            context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(assistant ? PREF_ASSISTANT_NAME : PREF_CTS_NAME, name)
                    .apply();
            return new ImportResult(true, clipped);
        } catch (Throwable error) {
            Log.w(TAG, "Activation sound import failed", error);
            try { if (tempMux.exists()) tempMux.delete(); } catch (Throwable ignored) {}
            try { if (tempCopy.exists()) tempCopy.delete(); } catch (Throwable ignored) {}
            return new ImportResult(false, false);
        }
    }

    private static boolean muxFirstThreeSeconds(Context context, Uri uri, File output)
            throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer = null;
        ParcelFileDescriptor pfd = null;
        boolean started = false;
        boolean wrote = false;

        try {
            pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) return false;
            extractor.setDataSource(pfd.getFileDescriptor());

            int audioTrack = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat candidate = extractor.getTrackFormat(i);
                String mime = candidate.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrack = i;
                    format = candidate;
                    break;
                }
            }
            if (audioTrack < 0 || format == null) return false;

            extractor.selectTrack(audioTrack);
            muxer = new MediaMuxer(
                    output.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int outTrack = muxer.addTrack(format);
            muxer.start();
            started = true;

            int bufferSize = 1024 * 1024;
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                try {
                    bufferSize = Math.max(64 * 1024,
                            Math.min(2 * 1024 * 1024,
                                    format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)));
                } catch (Throwable ignored) {
                }
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long firstSampleUs = -1L;

            while (true) {
                long sampleTimeUs = extractor.getSampleTime();
                if (sampleTimeUs < 0L) break;
                if (firstSampleUs < 0L) firstSampleUs = sampleTimeUs;

                long relativeUs = Math.max(0L, sampleTimeUs - firstSampleUs);
                if (relativeUs > MAX_CLIP_US) break;

                buffer.clear();
                int size = extractor.readSampleData(buffer, 0);
                if (size < 0) break;

                info.offset = 0;
                info.size = size;
                info.presentationTimeUs = relativeUs;
                info.flags =  extractor.getSampleFlags();
                muxer.writeSampleData(outTrack, buffer, info);
                wrote = true;

                if (!extractor.advance()) break;
            }
            return wrote;
        } finally {
            try { extractor.release(); } catch (Throwable ignored) {}
            if (muxer != null) {
                if (started) {
                    try { muxer.stop(); } catch (Throwable ignored) {}
                }
                try { muxer.release(); } catch (Throwable ignored) {}
            }
            if (pfd != null) {
                try { pfd.close(); } catch (Throwable ignored) {}
            }
            if (!wrote && output.exists()) {
                try { output.delete(); } catch (Throwable ignored) {}
            }
        }
    }

    private static void copyBounded(Context context, Uri uri, File target) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(target, false)) {
            if (in == null) throw new IllegalStateException("Unable to open selected audio");
            byte[] buffer = new byte[64 * 1024];
            int total = 0;
            int n;
            while ((n = in.read(buffer)) >= 0) {
                if (n == 0) continue;
                total += n;
                if (total > MAX_FALLBACK_BYTES) {
                    throw new IllegalArgumentException("Selected audio is too large");
                }
                out.write(buffer, 0, n);
            }
            out.flush();
        }
    }

    private static void replaceFile(File source, File target) throws Exception {
        if (target.exists() && !target.delete()) {
            throw new IllegalStateException("Cannot replace old activation clip");
        }
        if (source.renameTo(target)) return;
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(target, false)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) >= 0) {
                if (n > 0) out.write(buffer, 0, n);
            }
            out.flush();
        }
        source.delete();
    }

    private static String displayName(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    uri,
                    new String[] { OpenableColumns.DISPLAY_NAME },
                    null,
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.trim().isEmpty()) return value.trim();
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return UiText.tr(context, "Custom sound");
    }

    private static File customFile(Context context, boolean assistant) {
        return new File(context.getFilesDir(), assistant ? FILE_ASSISTANT : FILE_CTS);
    }

    private static void play(Context context, int rawResId, boolean assistant) {
        MediaPlayer player = new MediaPlayer();
        Handler main = new Handler(Looper.getMainLooper());
        Runnable[] timeoutHolder = new Runnable[1];

        try {
            AudioAttributes attributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
            player.setAudioAttributes(attributes);

            File custom = customFile(context, assistant);
            if (custom.isFile() && custom.length() > 0L) {
                player.setDataSource(custom.getAbsolutePath());
            } else {
                android.content.res.AssetFileDescriptor afd =
                        context.getResources().openRawResourceFd(rawResId);
                if (afd == null) {
                    player.release();
                    return;
                }
                try {
                    player.setDataSource(
                            afd.getFileDescriptor(),
                            afd.getStartOffset(),
                            afd.getLength());
                } finally {
                    afd.close();
                }
            }

            player.setOnCompletionListener(mp -> {
                if (timeoutHolder[0] != null) main.removeCallbacks(timeoutHolder[0]);
                safeRelease(mp);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                if (timeoutHolder[0] != null) main.removeCallbacks(timeoutHolder[0]);
                safeRelease(mp);
                return true;
            });

            player.prepare();

            timeoutHolder[0] = () -> {
                try {
                    if (player.isPlaying()) player.stop();
                } catch (Throwable ignored) {
                }
                safeRelease(player);
            };
            main.postDelayed(timeoutHolder[0], MAX_CLIP_MS);
            player.start();
        } catch (Throwable error) {
            if (timeoutHolder[0] != null) main.removeCallbacks(timeoutHolder[0]);
            safeRelease(player);
            Log.w(TAG, "Activation ringtone sound failed", error);
        }
    }

    private static void safeRelease(MediaPlayer player) {
        if (player == null) return;
        try { player.release(); } catch (Throwable ignored) {}
    }
}
