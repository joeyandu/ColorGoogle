// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse

package dev.evoker.homeholdcts;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * V5 overlay guardian.
 *
 * TYPE_APPLICATION_OVERLAY already gives Android a reason to raise process
 * importance. The old watcher attached a 1x1 overlay once and then assumed the
 * window would stay attached forever. ColorOS lockscreen/screen transitions
 * can invalidate or detach OEM-managed windows without killing the service.
 *
 * This guardian keeps the same tiny, non-interactive overlay but verifies and
 * re-attaches it around screen/keyguard transitions and with a very low-rate
 * sanity check. FLAG_SHOW_WHEN_LOCKED asks WindowManager to keep the window
 * eligible during keyguard; it does NOT turn the screen on and it cannot
 * consume touch/focus.
 */
final class OverlayGuardian {

    private static final String TAG = "HomeHoldCTS";
    private static final long SANITY_INTERVAL_MS = 60_000L;
    private static final long TRANSITION_RECHECK_FAST_MS = 300L;
    private static final long TRANSITION_RECHECK_SLOW_MS = 1_800L;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();

    private volatile boolean running;
    private WindowManager windowManager;
    private View overlay;
    private WindowManager.LayoutParams layoutParams;

    private final Runnable sanityRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            ensureNow("periodic-sanity");
            handler.postDelayed(this, SANITY_INTERVAL_MS);
        }
    };

    OverlayGuardian(Context context) {
        this.context = context.getApplicationContext();
    }

    void start() {
        if (running) {
            ensureNow("start-again");
            return;
        }

        running = true;
        ensureNow("start");
        handler.removeCallbacks(sanityRunnable);
        handler.postDelayed(sanityRunnable, SANITY_INTERVAL_MS);
    }

    void onSystemTransition(String action) {
        if (!running) {
            return;
        }

        ensureNow(action == null ? "system-transition" : action);

        // Keyguard/WindowManager transitions are asynchronous on ColorOS.
        // Recheck twice after the framework has finished rearranging windows.
        handler.postDelayed(
                () -> ensureNow("transition+300ms"),
                TRANSITION_RECHECK_FAST_MS);
        handler.postDelayed(
                () -> ensureNow("transition+1800ms"),
                TRANSITION_RECHECK_SLOW_MS);
    }

    void ensureNow(String reason) {
        if (!running || !canDrawOverlays()) {
            return;
        }

        synchronized (lock) {
            if (overlay != null && overlay.isAttachedToWindow()) {
                return;
            }

            removeLocked();

            try {
                if (windowManager == null) {
                    windowManager =
                            (WindowManager) context.getSystemService(
                                    Context.WINDOW_SERVICE);
                }
                if (windowManager == null) {
                    return;
                }

                View v = new View(context);
                v.setAlpha(0.01f);
                v.setImportantForAccessibility(
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

                int type = Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE;

                int flags =
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

                WindowManager.LayoutParams lp =
                        new WindowManager.LayoutParams(
                                1,
                                1,
                                type,
                                flags,
                                PixelFormat.TRANSLUCENT);

                lp.gravity = Gravity.TOP | Gravity.START;
                lp.x = 0;
                lp.y = 0;
                lp.setTitle("MindTrigger OverlayGuardian");

                windowManager.addView(v, lp);
                overlay = v;
                layoutParams = lp;

                Log.d(TAG, "OverlayGuardian attached: " + reason);
            } catch (Throwable t) {
                overlay = null;
                layoutParams = null;
                Log.w(TAG, "OverlayGuardian attach failed: " + reason, t);
            }
        }
    }

    boolean isAttached() {
        synchronized (lock) {
            return overlay != null && overlay.isAttachedToWindow();
        }
    }

    void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        synchronized (lock) {
            removeLocked();
        }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context);
    }

    private void removeLocked() {
        View v = overlay;
        overlay = null;
        layoutParams = null;

        if (v != null && windowManager != null) {
            try {
                windowManager.removeViewImmediate(v);
            } catch (Throwable ignored) {
                try {
                    windowManager.removeView(v);
                } catch (Throwable ignoredAgain) {
                }
            }
        }
    }
}
