// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;

/** Classifies live ColorOS log events. Called under the watcher's trigger lock. */
final class TriggerClassifier {
    enum Source { NONE, POWER, HOME_GESTURE, VOICE_WAKE }
    static final long DEBOUNCE_MS = 2500L;
    private static final long POWER_MARKER_WINDOW_MS = 500L;
    private static final long VOICE_WAKE_MARKER_WINDOW_MS = 1000L;
    private long lastTrigger = -1L;
    private long lastPowerMarker = -1L;
    private long lastVoiceMarker = -1L;

    static boolean isCandidateLine(String line) {
        return line != null && (line.contains("Detect long press KEYCODE_POWER")
                || line.contains("PowerKey:onLongPress")
                || line.contains("wakeUpSpeechAssist caller_package: com.oplus.ovoicemanager.wakeup")
                || (line.contains("act=heytap.intent.action.ACTIVATE_SPEECH_ASSIST")
                    && line.contains("cmp=com.heytap.speechassist/.core.SpeechService")));
    }

    Source accept(String line, long now) {
        if (line == null || line.isEmpty()) return Source.NONE;
        if (line.contains("wakeUpSpeechAssist caller_package: com.oplus.ovoicemanager.wakeup")) {
            lastVoiceMarker = now;
            return Source.NONE;
        }
        boolean power = line.contains("Detect long press KEYCODE_POWER")
                || line.contains("PowerKey:onLongPress");
        if (power) lastPowerMarker = now;
        else if (!line.contains("act=heytap.intent.action.ACTIVATE_SPEECH_ASSIST")
                || !line.contains("cmp=com.heytap.speechassist/.core.SpeechService")) {
            return Source.NONE;
        }

        boolean voice = !power && recent(lastVoiceMarker, now, VOICE_WAKE_MARKER_WINDOW_MS);
        if (voice) lastVoiceMarker = -1L;
        // Only an accepted activation moves the cooldown. Duplicate OEM markers
        // and the following speech-service attempt must never schedule another.
        if (recent(lastTrigger, now, DEBOUNCE_MS - 1L)) return Source.NONE;
        if (!power && !voice && recent(lastPowerMarker, now, POWER_MARKER_WINDOW_MS)) {
            return Source.NONE;
        }
        lastTrigger = now;
        return power ? Source.POWER : (voice ? Source.VOICE_WAKE : Source.HOME_GESTURE);
    }

    static boolean targetsAssistant(Source source, boolean powerGemini,
            boolean voiceAssistant, boolean swap) {
        if (source == Source.NONE) return false;
        if (source == Source.VOICE_WAKE) return voiceAssistant;
        return swap ? source == Source.HOME_GESTURE : source == Source.POWER && powerGemini;
    }

    private static boolean recent(long marker, long now, long window) {
        return marker >= 0L && now >= marker && now - marker <= window;
    }
}
