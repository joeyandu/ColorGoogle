// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;
import org.junit.Test;
import static org.junit.Assert.*;
import static dev.evoker.homeholdcts.TriggerClassifier.Source.*;

public class TriggerClassifierTest {
    private static final String KEY = "KEYLOG_SingleKeyGesture: Detect long press KEYCODE_POWER";
    private static final String MONITOR = "KEYLOG_SinglePowerKeyMonitor: PowerKey:onLongPress, underWaterCameraStatus = false";
    private static final String SERVICE = "ActivityManager: Unable to start service Intent { act=heytap.intent.action.ACTIVATE_SPEECH_ASSIST cmp=com.heytap.speechassist/.core.SpeechService }: not found";
    private static final String VOICE = "wakeUpSpeechAssist caller_package: com.oplus.ovoicemanager.wakeup";
    @Test public void screenOffPowerNeedsNoSpeechService() {
        assertEquals(POWER,new TriggerClassifier().accept(KEY,0));
        assertEquals(POWER,new TriggerClassifier().accept(MONITOR,100));
    }
    @Test public void oemPairAndSpeechAttemptOnlyActivateOnce() {
        TriggerClassifier c=new TriggerClassifier();
        assertEquals(POWER,c.accept(KEY,10000));
        assertEquals(NONE,c.accept(MONITOR,10002));
        assertEquals(NONE,c.accept(SERVICE,10003));
        assertEquals(NONE,c.accept(SERVICE,12000));
    }
    @Test public void reverseOrderAlsoDeduplicates() {
        TriggerClassifier c=new TriggerClassifier();
        assertEquals(POWER,c.accept(MONITOR,10000));
        assertEquals(NONE,c.accept(KEY,10001));
        assertEquals(NONE,c.accept(SERVICE,10002));
    }
    @Test public void duplicateDoesNotExtendCooldown() {
        TriggerClassifier c=new TriggerClassifier();
        assertEquals(POWER,c.accept(KEY,0));
        assertEquals(NONE,c.accept(MONITOR,2499));
        assertEquals(POWER,c.accept(KEY,2500));
    }
    @Test public void suppressedPowerTailDoesNotBecomeHomeAfterCooldown() {
        TriggerClassifier c=new TriggerClassifier();
        assertEquals(POWER,c.accept(KEY,0));
        assertEquals(NONE,c.accept(KEY,2400));
        assertEquals(NONE,c.accept(SERVICE,2600));
        assertEquals(HOME_GESTURE,c.accept(SERVICE,3000));
    }
    @Test public void gestureAndVoiceRetainTheirRoutes() {
        TriggerClassifier c=new TriggerClassifier();
        assertEquals(HOME_GESTURE,c.accept(SERVICE,0));
        assertEquals(NONE,c.accept(VOICE,3000));
        assertEquals(VOICE_WAKE,c.accept(SERVICE,3001));
        assertEquals(HOME_GESTURE,c.accept(SERVICE,6000));
    }
    @Test public void powerTakesPriorityOverStaleVoiceMarker() {
        TriggerClassifier c=new TriggerClassifier();
        c.accept(VOICE,100);
        assertEquals(POWER,c.accept(KEY,101));
        assertEquals(NONE,c.accept(SERVICE,102));
    }
    @Test public void unrelatedAndIncompleteLogsDoNothing() {
        TriggerClassifier c=new TriggerClassifier();
        assertEquals(NONE,c.accept(null,0));
        assertEquals(NONE,c.accept("",0));
        assertEquals(NONE,c.accept("act=heytap.intent.action.ACTIVATE_SPEECH_ASSIST",0));
        assertEquals(NONE,c.accept("PowerKey:onShortPress",0));
    }
    @Test public void preferencesAndSwapPreserved() {
        assertTrue(TriggerClassifier.targetsAssistant(POWER,true,false,false));
        assertFalse(TriggerClassifier.targetsAssistant(HOME_GESTURE,true,false,false));
        assertFalse(TriggerClassifier.targetsAssistant(POWER,false,false,false));
        assertFalse(TriggerClassifier.targetsAssistant(POWER,true,false,true));
        assertTrue(TriggerClassifier.targetsAssistant(HOME_GESTURE,true,false,true));
        assertTrue(TriggerClassifier.targetsAssistant(VOICE_WAKE,false,true,true));
        assertFalse(TriggerClassifier.targetsAssistant(VOICE_WAKE,true,false,false));
    }
}
