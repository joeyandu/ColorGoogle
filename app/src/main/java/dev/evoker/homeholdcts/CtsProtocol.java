// SPDX-License-Identifier: GPL-3.0-only
// MindTrigger Assist modifications Copyright (C) 2026 EvokerUniverse
// Modified for MindTrigger Assist on 2026-08-17.

package dev.evoker.homeholdcts;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

/**
 * Circle to Search voice-interaction bridge.
 *
 * This stable branch retains CTS invocation behavior developed with reference
 * to the public MiCTS project (GPL-3.0) and Android voice-interaction APIs.
 * MindTrigger Assist supplies the ColorOS trigger, watcher, setup, retry and
 * process-readiness layers around this bridge.
 *
 * See NOTICE.md for upstream attribution.
 */
final class CtsProtocol {

    private static final String TAG = "HomeHoldCTS";
    private static final String SERVICE_NAME = "voiceinteraction";

    // SHOW_WITH_ASSIST | SHOW_WITH_SCREENSHOT | SHOW_SOURCE_ASSIST_GESTURE
    private static final int VOICE_SESSION_FLAGS = 7;

    private CtsProtocol() {}

    static Result requestCircleToSearch(int entryPoint) {
        return requestVoiceSession(
                buildCtsInvocationArgs(entryPoint),
                VOICE_SESSION_FLAGS,
                Build.VERSION.SDK_INT >= 34 ? "hyperOS_home" : null,
                "CTS"
        );
    }

    /**
     * Experimental Gemini/Assistant route through the SAME voiceinteraction
     * transport as CTS.
     *
     * No omni.entry_point / micts_trigger here: this stays on the normal active
     * VoiceInteractionService session path instead of the CTS/contextual-search
     * branch.
     */
    static Result requestAssistantSession() {
        return requestVoiceSession(
                buildAssistantInvocationArgs(),
                VOICE_SESSION_FLAGS,
                null,
                "ASSISTANT"
        );
    }

    private static Result requestVoiceSession(
            Bundle args,
            int flags,
            String attributionTag,
            String routeName) {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Object raw = HiddenApiBypass.invoke(
                    serviceManagerClass, null, "getService", SERVICE_NAME);
            if (!(raw instanceof IBinder)) {
                return Result.fail(routeName + ": voiceinteraction binder unavailable", null);
            }

            Class<?> stubClass = Class.forName(
                    "com.android.internal.app.IVoiceInteractionManagerService$Stub");
            Object manager = HiddenApiBypass.invoke(stubClass, null, "asInterface", raw);
            if (manager == null) {
                return Result.fail(routeName + ": voiceinteraction interface unavailable", null);
            }

            Class<?> interfaceClass = Class.forName(
                    "com.android.internal.app.IVoiceInteractionManagerService");
            Object response;
            if (Build.VERSION.SDK_INT >= 34) {
                response = HiddenApiBypass.invoke(
                        interfaceClass,
                        manager,
                        "showSessionFromSession",
                        new Object[] { null, args, flags, attributionTag });
            } else {
                response = HiddenApiBypass.invoke(
                        interfaceClass,
                        manager,
                        "showSessionFromSession",
                        new Object[] { null, args, flags });
            }

            boolean accepted = response instanceof Boolean && (Boolean) response;
            return accepted
                    ? Result.ok(routeName)
                    : Result.fail(routeName + ": voiceinteraction returned false", null);
        } catch (Throwable t) {
            Log.w(TAG, routeName + " protocol request failed", t);
            return Result.fail(
                    routeName + ": " + t.getClass().getSimpleName() + ": " + t.getMessage(),
                    t);
        }
    }

    private static Bundle buildCtsInvocationArgs(int entryPoint) {
        Bundle args = new Bundle();
        args.putLong("invocation_time_ms", SystemClock.elapsedRealtime());
        args.putInt("omni.entry_point", entryPoint);
        args.putBoolean("micts_trigger", true);
        return args;
    }

    private static Bundle buildAssistantInvocationArgs() {
        Bundle args = new Bundle();
        args.putLong("invocation_time_ms", SystemClock.elapsedRealtime());
        return args;
    }

    static final class Result {
        final boolean success;
        final String detail;
        final Throwable error;

        private Result(boolean success, String detail, Throwable error) {
            this.success = success;
            this.detail = detail;
            this.error = error;
        }

        static Result ok(String routeName) {
            return new Result(true, routeName + ": accepted", null);
        }

        static Result fail(String detail, Throwable error) {
            return new Result(false, detail, error);
        }
    }
}
