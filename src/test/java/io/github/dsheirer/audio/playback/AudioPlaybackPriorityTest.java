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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.preference.UserPreferences;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that a higher priority audio segment isn't displaced by lower priority audio during playback (issue #2333).
 */
public class AudioPlaybackPriorityTest
{
    private static final float[] AUDIO = new float[AudioChannel.SAMPLES_PER_INTERVAL];
    private AudioChannel mChannel;

    @BeforeEach
    public void setup()
    {
        mChannel = new AudioChannel(new UserPreferences(), "TEST");
    }

    @AfterEach
    public void teardown()
    {
        mChannel.dispose();
    }

    private static AudioSegment segment(int priority)
    {
        AudioSegment segment = new AudioSegment(new AliasList("test"), 0);
        segment.incrementConsumerCount();
        segment.monitorPriorityProperty().set(priority);
        segment.addAudio(AUDIO);
        return segment;
    }

    /**
     * Pulls audio until the buffered audio (start tone and first frame) is exhausted and the channel is waiting on the
     * audio segment for more audio.
     */
    private void drainBufferedAudio()
    {
        while(mChannel.getAudio() != null)
        {
            //Discard buffered audio
        }
    }

    private void pullIntervals(int count)
    {
        for(int x = 0; x < count; x++)
        {
            mChannel.getAudio();
        }
    }

    @Test
    public void liveSegmentSurvivesDecoderGapWithLowerPriorityQueued()
    {
        AudioSegment priority1 = segment(1);
        AudioSegment priority2 = segment(2);
        mChannel.play(priority1);
        mChannel.getAudio();
        mChannel.play(priority2);
        drainBufferedAudio();

        //300 ms without audio exceeds a missed P25 LDU
        pullIntervals(15);

        assertTrue(mChannel.hasAudioSegment(), "Live segment should still be assigned");
        assertSame(priority2, mChannel.peekQueuedAudioSegment(), "Queued lower priority segment should not have started");

        priority1.addAudio(AUDIO);
        assertTrue(mChannel.getAudio() != null, "Audio arriving after the gap should play");
    }

    @Test
    public void stalledSegmentIsAbandonedSoChannelIsReleased()
    {
        mChannel.play(segment(1));
        mChannel.getAudio();
        drainBufferedAudio();
        pullIntervals(AudioChannel.MAX_STALLED_INTERVALS + 1);

        assertFalse(mChannel.hasAudioSegment(), "Stalled segment should be released");
    }

    @Test
    public void completedSegmentIsReleasedWithoutWaitingForStallTimeout()
    {
        AudioSegment segment = segment(1);
        mChannel.play(segment);
        mChannel.getAudio();
        segment.completeProperty().set(true);
        drainBufferedAudio();
        pullIntervals(2);

        assertFalse(mChannel.hasAudioSegment(), "Completed segment should be released");
    }

    @Test
    public void higherPriorityWaitingSegmentReplacesLowerPriorityQueuedSegment()
    {
        AudioSegment priority1 = segment(1);
        AudioSegment priority3 = segment(3);
        mChannel.play(priority3);
        List<AudioSegment> waiting = new ArrayList<>(List.of(priority1));

        AudioPlaybackManager.replaceLowerPriorityQueuedSegments(waiting, List.of(mChannel));

        assertSame(priority1, mChannel.peekQueuedAudioSegment());
        assertEquals(List.of(priority3), waiting, "Displaced segment should return to the waiting list");
    }

    @Test
    public void equalAndLowerPriorityWaitingSegmentsDoNotReplaceQueuedSegment()
    {
        AudioSegment queued = segment(2);
        mChannel.play(queued);

        for(int priority: new int[]{2, 3})
        {
            AudioSegment other = segment(priority);
            List<AudioSegment> waiting = new ArrayList<>(List.of(other));
            AudioPlaybackManager.replaceLowerPriorityQueuedSegments(waiting, List.of(mChannel));

            assertSame(queued, mChannel.peekQueuedAudioSegment());
            assertEquals(List.of(other), waiting);
        }
    }

    @Test
    public void noReplacementWhenNothingIsQueued()
    {
        List<AudioSegment> waiting = new ArrayList<>(List.of(segment(1)));
        AudioPlaybackManager.replaceLowerPriorityQueuedSegments(waiting, List.of(mChannel));

        assertNull(mChannel.peekQueuedAudioSegment());
        assertEquals(1, waiting.size());
    }
}
