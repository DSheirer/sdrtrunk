/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.audio.playback;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.protocol.Protocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Talkgroup hold matching and hold-aware assignment of calls to audio channels.
 */
public class TalkgroupHoldTest
{
    private static final float[] AUDIO = new float[AudioChannel.SAMPLES_PER_INTERVAL];
    private AudioChannel mLeft;
    private AudioChannel mRight;

    @BeforeEach
    public void setup()
    {
        Settings settings = new Settings();
        mLeft = new AudioChannel(settings, "LEFT");
        mRight = new AudioChannel(settings, "RIGHT");
    }

    @AfterEach
    public void teardown()
    {
        mLeft.dispose();
        mRight.dispose();
    }

    private static AudioSegment call(int talkgroup)
    {
        AudioSegment segment = new AudioSegment(new AliasList("test"), 0);
        segment.incrementConsumerCount();
        segment.addIdentifier(APCO25Talkgroup.create(talkgroup));
        segment.addAudio(AUDIO);
        return segment;
    }

    private static TalkgroupHold hold(int talkgroup)
    {
        return new TalkgroupHold(Protocol.APCO25, talkgroup, String.valueOf(talkgroup));
    }

    @Test
    public void matchesTalkgroupAndProtocol()
    {
        TalkgroupHold hold = hold(100);
        assertTrue(hold.matches(call(100)));
        assertFalse(hold.matches(call(101)));

        AudioSegment dmr = new AudioSegment(new AliasList("test"), 0);
        dmr.addIdentifier(DMRTalkgroup.create(100));
        assertFalse(hold.matches(dmr), "same value on a different protocol");

        TalkgroupHold phase2 = new TalkgroupHold(Protocol.APCO25_PHASE2, 100, "100");
        assertTrue(phase2.matches(call(100)), "P25 phase 1 and phase 2 share talkgroups");
    }

    @Test
    public void matchesPatchGroupAndMembers()
    {
        PatchGroup patchGroup = new PatchGroup(APCO25Talkgroup.create(500));
        patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(100));
        AudioSegment segment = new AudioSegment(new AliasList("test"), 0);
        segment.addIdentifier(APCO25PatchGroup.create(patchGroup));

        assertTrue(hold(500).matches(segment), "patch group talkgroup");
        assertTrue(hold(100).matches(segment), "patched member talkgroup");
        assertFalse(hold(101).matches(segment));
    }

    @Test
    public void createFromCallRecord()
    {
        CallRecord call = new CallRecord();
        call.setProtocol(Protocol.APCO25.name());
        call.setToId("100");
        call.setToValue(100L);
        call.setToForm(CallRecord.FORM_TALKGROUP);
        TalkgroupHold hold = TalkgroupHold.from(call, "Fire");
        assertNotNull(hold);
        assertEquals("100 Fire", hold.label());
        assertTrue(hold.matches(call(100)));

        call.setToForm(CallRecord.FORM_RADIO);
        assertNull(TalkgroupHold.from(call, null), "private call to a radio isn't a talkgroup");
    }

    @Test
    public void heldTalkgroupIsSteeredToHoldingChannel()
    {
        mRight.setHold(hold(100));
        AudioSegment held = call(100);
        List<AudioSegment> waiting = new ArrayList<>(List.of(held));

        AudioPlaybackManager.assignAudioSegments(waiting, List.of(mLeft, mRight));

        assertTrue(waiting.isEmpty());
        assertSame(held, mRight.peekQueuedAudioSegment(), "held call goes to the holding channel");
        assertNull(mLeft.peekQueuedAudioSegment(), "even though the left channel is free");
    }

    @Test
    public void otherCallsAreKeptOffHoldingChannel()
    {
        mLeft.setHold(hold(100));
        AudioSegment first = call(200);
        AudioSegment second = call(300);
        List<AudioSegment> waiting = new ArrayList<>(List.of(first, second));

        AudioPlaybackManager.assignAudioSegments(waiting, List.of(mLeft, mRight));

        assertNull(mLeft.peekQueuedAudioSegment(), "held channel stays free for the held talkgroup");
        assertSame(first, mRight.peekQueuedAudioSegment());
        assertEquals(List.of(second), waiting, "second call waits for the unheld channel");
    }

    @Test
    public void heldChannelPlaysDoNotMonitorCallOfHeldTalkgroupOnly()
    {
        mLeft.setHold(hold(100));
        AudioSegment held = call(100);
        held.monitorPriorityProperty().set(-1);
        assertTrue(held.isDoNotMonitor());
        AudioSegment other = call(200);

        mLeft.play(other);
        mLeft.play(held);

        //The non-matching call is thrown away and the held do-not-monitor call plays
        assertNotNull(mLeft.getAudio());
        assertTrue(mLeft.isPlayingHeldTalkgroup());
    }

    @Test
    public void noHoldKeepsExistingAssignment()
    {
        AudioSegment first = call(200);
        AudioSegment second = call(300);
        List<AudioSegment> waiting = new ArrayList<>(List.of(first, second));

        AudioPlaybackManager.assignAudioSegments(waiting, List.of(mLeft, mRight));

        assertSame(first, mLeft.peekQueuedAudioSegment());
        assertSame(second, mRight.peekQueuedAudioSegment());
        assertTrue(waiting.isEmpty());
    }
}
