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
 * Tests that a new call is assigned to an idle audio channel ahead of a channel that is playing another call.
 */
public class AudioChannelAssignmentTest
{
    private static final float[] AUDIO = new float[AudioChannel.SAMPLES_PER_INTERVAL];
    private AudioChannel mLeft;
    private AudioChannel mRight;
    private List<AudioChannel> mChannels;

    @BeforeEach
    public void setup()
    {
        UserPreferences userPreferences = new UserPreferences();
        mLeft = new AudioChannel(userPreferences, "LEFT");
        mRight = new AudioChannel(userPreferences, "RIGHT");
        mChannels = List.of(mLeft, mRight);
    }

    @AfterEach
    public void teardown()
    {
        mLeft.dispose();
        mRight.dispose();
    }

    private static AudioSegment segment()
    {
        AudioSegment segment = new AudioSegment(new AliasList("test"), 0);
        segment.incrementConsumerCount();
        segment.addAudio(AUDIO);
        return segment;
    }

    /**
     * Assigns the audio segment to the channel and starts playback, which takes the segment out of the channel's queue.
     */
    private static void startPlayback(AudioChannel channel, AudioSegment segment)
    {
        channel.play(segment);
        channel.getAudio();
        assertTrue(channel.hasAudioSegment());
        assertTrue(channel.isEmpty());
        assertFalse(channel.isIdle());
    }

    @Test
    public void secondCallGoesToTheIdleChannelWhileTheFirstIsPlaying()
    {
        startPlayback(mLeft, segment());
        AudioSegment second = segment();
        List<AudioSegment> waiting = new ArrayList<>(List.of(second));

        AudioPlaybackManager.assignToAudioChannels(waiting, mChannels);

        assertTrue(waiting.isEmpty());
        assertNull(mLeft.peekQueuedAudioSegment(), "Second call should not queue behind the call that is playing");
        assertSame(second, mRight.peekQueuedAudioSegment());
    }

    @Test
    public void secondCallGoesToTheLeftChannelWhenOnlyTheRightIsPlaying()
    {
        startPlayback(mRight, segment());
        AudioSegment second = segment();
        List<AudioSegment> waiting = new ArrayList<>(List.of(second));

        AudioPlaybackManager.assignToAudioChannels(waiting, mChannels);

        assertSame(second, mLeft.peekQueuedAudioSegment());
        assertNull(mRight.peekQueuedAudioSegment());
    }

    @Test
    public void callsQueueBehindPlaybackWhenEveryChannelIsPlaying()
    {
        startPlayback(mLeft, segment());
        startPlayback(mRight, segment());
        AudioSegment third = segment();
        AudioSegment fourth = segment();
        AudioSegment fifth = segment();
        List<AudioSegment> waiting = new ArrayList<>(List.of(third, fourth, fifth));

        AudioPlaybackManager.assignToAudioChannels(waiting, mChannels);

        assertSame(third, mLeft.peekQueuedAudioSegment());
        assertSame(fourth, mRight.peekQueuedAudioSegment());
        assertEquals(List.of(fifth), waiting, "A channel holds one queued call, the rest keep waiting");
    }

    @Test
    public void twoNewCallsTakeOneIdleChannelEach()
    {
        AudioSegment first = segment();
        AudioSegment second = segment();
        List<AudioSegment> waiting = new ArrayList<>(List.of(first, second));

        AudioPlaybackManager.assignToAudioChannels(waiting, mChannels);

        assertTrue(waiting.isEmpty());
        assertSame(first, mLeft.peekQueuedAudioSegment());
        assertSame(second, mRight.peekQueuedAudioSegment());
    }
}
