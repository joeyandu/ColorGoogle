// SPDX-License-Identifier: GPL-3.0-only
package dev.evoker.homeholdcts;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class LogPacketTest {
    private byte[] packet(int uid, String tag, String msg) {
        byte[] tagBytes=tag.getBytes(StandardCharsets.UTF_8), body=msg.getBytes(StandardCharsets.UTF_8);
        ByteBuffer b=ByteBuffer.allocate(28+1+tagBytes.length+1+body.length+1).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short)(b.capacity()-28)).putShort((short)28);
        b.putInt(3461).putInt(3665).putInt(100).putInt(0).putInt(3).putInt(uid);
        b.put((byte)4).put(tagBytes).put((byte)0).put(body).put((byte)0);
        return b.array();
    }
    @Test public void preservesSystemUidAndPowerMarker() {
        byte[] p=packet(1000,"KEYLOG_SingleKeyGesture","Detect long press KEYCODE_POWER");
        assertEquals(1000,DirectLogdReader.entryUid(p,p.length));
        String line=DirectLogdReader.decodeRelevantTextEntry(p,p.length);
        assertTrue(TriggerClassifier.isCandidateLine(line));
        assertEquals(TriggerClassifier.Source.POWER,new TriggerClassifier().accept(line,5000));
    }
    @Test public void heartbeatKeepsActualUidAndNeverBecomesTrigger() {
        for(int uid:new int[]{2000,10606}) {
            byte[] p=packet(uid,"HomeHoldCTS","MINDTRIGGER_DIRECT_LOGD_HEARTBEAT_12");
            assertEquals(uid,DirectLogdReader.entryUid(p,p.length));
            assertFalse(TriggerClassifier.isCandidateLine(DirectLogdReader.decodeRelevantTextEntry(p,p.length)));
        }
    }
    @Test public void truncatedOrLegacyPacketCannotProveForeignUid() {
        byte[] p=packet(1000,"ActivityManager","message");
        assertEquals(-1,DirectLogdReader.entryUid(p,20));
        assertEquals(-1,DirectLogdReader.entryUid(p,p.length-1));
        p[2]=20;
        assertEquals(-1,DirectLogdReader.entryUid(p,p.length));
    }
    @Test public void unrelatedMessagesAreNotForwarded() {
        byte[] p=packet(1000,"UnrelatedTag","Detect long press KEYCODE_POWER");
        assertNull(DirectLogdReader.decodeRelevantTextEntry(p,p.length));
        assertFalse(TriggerClassifier.isCandidateLine("ActivityManager: starting unrelated service"));
        assertTrue(TriggerClassifier.isCandidateLine("ActivityManager: act=heytap.intent.action.ACTIVATE_SPEECH_ASSIST cmp=com.heytap.speechassist/.core.SpeechService"));
        assertTrue(TriggerClassifier.isCandidateLine("OVMS-OVMS_StartSpeechAssist: wakeUpSpeechAssist caller_package: com.oplus.ovoicemanager.wakeup"));
    }
    @Test public void quietSessionUsesForeignPacketWithoutForwardingItsText() {
        byte[] p=packet(1000,"UnrelatedTag","private unrelated contents");
        assertEquals("", DirectLogdReader.decodeSessionEntry(p,p.length,2000));
        assertFalse(TriggerClassifier.isCandidateLine(DirectLogdReader.decodeSessionEntry(p,p.length,2000)));
        assertNull(DirectLogdReader.decodeSessionEntry(p,p.length,1000));
    }
    @Test public void invalidPacketCannotSupplyHealthProof() {
        byte[] p=packet(1000,"UnrelatedTag","message");
        assertNull(DirectLogdReader.decodeSessionEntry(p,p.length-1,2000));
        p[2]=20;
        assertNull(DirectLogdReader.decodeSessionEntry(p,p.length,2000));
    }
}
