// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

/**
 * Shared activation pipeline for both branches.
 *
 * Common path:
 *   trigger classified
 *       -> shared haptic
 *       -> shared configurable delay
 *       -> Google revive/prime
 *       -> branch
 *          CTS               -> contextual-search voice session
 *          ASSISTANT_SESSION -> generic Assistant voice session
 *
 * The legacy preference key PREF_CTS_DELAY_MS is intentionally retained so
 * upgrades keep the user's existing delay value. The setting now applies to
 * both activation branches.
 */
final class ActivationRunner {

    enum Target {
        CTS,
        ASSISTANT_SESSION
    }

    private static final String TAG = "HomeHoldCTS";
    private static final int ENTRY_POINT_HOME = 1;

    private static final long RETRY_1_MS = 140L;
    private static final long RETRY_2_MS = 320L;
    // Google Assistant can need a short process-start settle after it was
    // reclaimed. CTS keeps its existing timing; this applies only to the
    // generic Assistant session branch.
    private static final long ASSISTANT_PRIME_SETTLE_MS = 240L;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean active;
    private int attempt;
    private Target target;
    private boolean soundEnabled;
    private GoogleReviver.PrimeLease assistantWarmupLease;

    ActivationRunner(Context context) {
        this.context = context.getApplicationContext();
    }

    synchronized void activateCts(int delayMs, boolean vibrate, boolean sound) {
        activate(Target.CTS, delayMs, vibrate, sound);
    }

    synchronized void activateAssistantSession(int delayMs, boolean vibrate, boolean sound) {
        activate(Target.ASSISTANT_SESSION, delayMs, vibrate, sound);
    }

    private synchronized void activate(
            Target requestedTarget,
            int requestedDelayMs,
            boolean vibrateEnabled,
            boolean soundEnabled) {
        if (active) {
            Log.d(TAG, "Activation already active; duplicate ignored. target="
                    + requestedTarget);
            return;
        }

        active = true;
        attempt = 0;
        target = requestedTarget;
        this.soundEnabled = soundEnabled;

        int delayMs = Math.max(
                0,
                Math.min(MainActivity.MAX_CTS_DELAY_MS, requestedDelayMs));

        if (vibrateEnabled) {
            vibrate();
        }

        Log.d(TAG, "Activation scheduled target=" + target
                + " delay=" + delayMs + " ms");

        if (target == Target.ASSISTANT_SESSION) {
            // Start Google before the user-visible activation delay elapses.
            // This is the same safe provider-based primer CTS already uses;
            // it neither opens Google UI nor changes trigger classification.
            handler.post(this::primeAssistantBeforeActivation);
        }
        handler.postDelayed(this::primeAndRun, delayMs);
    }

    private void primeAssistantBeforeActivation() {
        if (!active || target != Target.ASSISTANT_SESSION) return;
        closeAssistantWarmupLease();
        assistantWarmupLease = GoogleReviver.acquireWarmupLease(context);
        GoogleReviver.PrimeResult prime = assistantWarmupLease.result;
        Log.d(TAG, "Assistant pre-prime acquired=" + prime.acquired
                + " attempted=" + prime.attempted);
    }

    private void primeAndRun() {
        if (!active || target == null) return;

        GoogleReviver.PrimeResult prime = target == Target.ASSISTANT_SESSION
                && assistantWarmupLease != null
                ? assistantWarmupLease.result
                : GoogleReviver.prime(context);
        Log.d(TAG, "Activation primer target=" + target
                + " acquired=" + prime.acquired
                + " attempted=" + prime.attempted);

        if (target == Target.ASSISTANT_SESSION) {
            handler.postDelayed(this::attemptTarget, ASSISTANT_PRIME_SETTLE_MS);
        } else {
            attemptTarget();
        }
    }

    private void attemptTarget() {
        if (!active || target == null) return;

        attempt++;

        CtsProtocol.Result result =
                target == Target.ASSISTANT_SESSION
                        ? CtsProtocol.requestAssistantSession()
                        : CtsProtocol.requestCircleToSearch(ENTRY_POINT_HOME);

        Log.i(TAG, "Activation attempt target=" + target
                + " attempt=" + attempt
                + " success=" + result.success
                + " detail=" + result.detail);

        if (result.success) {
            if (soundEnabled) {
                if (target == Target.ASSISTANT_SESSION) {
                    ActivationSoundPlayer.playAssistant(context);
                } else {
                    ActivationSoundPlayer.playCts(context);
                }
            }

            finish();
            return;
        }

        GoogleReviver.prime(context);

        if (attempt == 1) {
            handler.postDelayed(this::attemptTarget, RETRY_1_MS);
        } else if (attempt == 2) {
            handler.postDelayed(this::attemptTarget, RETRY_2_MS);
        } else {
            Toast.makeText(
                    context,
                    target == Target.ASSISTANT_SESSION
                            ? UiText.tr(context, "Assistant voice session failed")
                            : UiText.tr(context, "Circle to Search activation failed"),
                    Toast.LENGTH_SHORT
            ).show();
            finish();
        }
    }

    private void vibrate() {
        try {
            Vibrator vibrator =
                    (Vibrator) context.getSystemService(
                            Context.VIBRATOR_SERVICE);

            if (vibrator == null || !vibrator.hasVibrator()) {
                return;
            }

            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(
                        VibrationEffect.createOneShot(
                                32L,
                                VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //noinspection deprecation
                vibrator.vibrate(32L);
            }
        } catch (Throwable error) {
            Log.w(TAG, "Activation haptic failed", error);
        }
    }

    synchronized void shutdown() {
        handler.removeCallbacksAndMessages(null);
        closeAssistantWarmupLease();
        active = false;
        attempt = 0;
        target = null;
        soundEnabled = false;
    }

    private synchronized void finish() {
        closeAssistantWarmupLease();
        active = false;
        attempt = 0;
        target = null;
        soundEnabled = false;
    }

    private void closeAssistantWarmupLease() {
        if (assistantWarmupLease != null) {
            assistantWarmupLease.close();
            assistantWarmupLease = null;
        }
    }
}
