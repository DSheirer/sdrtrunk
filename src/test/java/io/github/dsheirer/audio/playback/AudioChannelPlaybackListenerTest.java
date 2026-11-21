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
import io.github.dsheirer.preference.UserPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The audio channel reports the call (audio segment) that it starts and stops playing, which the Calls tab uses to
 * show which calls are playing on each channel.
 */
public class AudioChannelPlaybackListenerTest
{
    private static final float[] AUDIO = new float[AudioChannel.SAMPLES_PER_INTERVAL];
    private AudioChannel mChannel;
    private final List<AudioSegment> mEvents = new ArrayList<>();

    @BeforeEach
    public void setup()
    {
        mChannel = new AudioChannel(new UserPreferences(), "LEFT");
        mChannel.setPlaybackListener(() -> mEvents.add(mChannel.getCurrentAudioSegment()));
    }

    @AfterEach
    public void teardown()
    {
        mChannel.dispose();
    }

    private static AudioSegment segment()
    {
        AudioSegment segment = new AudioSegment(new AliasList("test"), 0);
        segment.incrementConsumerCount();
        segment.addAudio(AUDIO);
        return segment;
    }

    @Test
    public void reportsStartAndStopOfEachCall()
    {
        AudioSegment first = segment();
        AudioSegment second = segment();
        assertNotEquals(first.getId(), second.getId(), "audio segment ids are unique");

        mChannel.play(first);
        mChannel.play(second);
        mChannel.getAudio();
        assertSame(first, mChannel.getCurrentAudioSegment());
        assertEquals(List.of(first), mEvents, "first call started");

        first.completeProperty().set(true);
        second.completeProperty().set(true);

        for(int x = 0; x < 10; x++)
        {
            mChannel.getAudio();
        }

        assertSame(first, mEvents.get(0));
        assertNull(mEvents.get(1), "first call stopped");
        assertSame(second, mEvents.get(2), "second call started");
        assertNull(mEvents.get(3), "second call stopped");
        assertNull(mChannel.getCurrentAudioSegment());
    }

    @Test
    public void replayTakesOverChannelAndLiveCallsResumeAfter()
    {
        AudioSegment live = segment();
        mChannel.play(live);
        mChannel.getAudio();
        assertSame(live, mChannel.getCurrentAudioSegment());

        AudioSegment queued = segment();
        mChannel.play(queued);

        float[] samples = new float[AudioChannel.SAMPLES_PER_INTERVAL * 3];
        java.util.Arrays.fill(samples, 0.25f);
        ReplaySource replay = new ReplaySource(samples, 42, "Replay");
        mChannel.startReplay(replay);
        assertEquals(true, mChannel.isReplaying());

        float[] audio = mChannel.getAudio();
        assertEquals(0.25f, audio[0], 0.0001f, "replay audio replaces the live call");
        assertNull(mChannel.getCurrentAudioSegment(), "live call dropped");
        assertSame(replay, mChannel.getReplay());

        replay.setPaused(true);
        assertEquals(0.0f, mChannel.getAudio()[0], 0.0001f, "paused replay is silent");
        replay.setPaused(false);
        mChannel.getAudio();
        mChannel.getAudio();
        assertEquals(true, replay.isCompleted(), "replay played to the end");

        //Next interval releases the replay and the queued live call plays
        mChannel.getAudio();
        assertNull(mChannel.getReplay());
        assertSame(queued, mChannel.getCurrentAudioSegment(), "queued live call plays after the replay");
    }

    @Test
    public void replayingChannelIsSkippedForLiveCalls()
    {
        AudioChannel right = new AudioChannel(new UserPreferences(), "RIGHT");

        try
        {
            mChannel.startReplay(new ReplaySource(new float[8000], 7, "Replay"));
            AudioSegment live = segment();
            List<AudioSegment> waiting = new ArrayList<>(List.of(live));

            AudioPlaybackManager.assignAudioSegments(waiting, List.of(mChannel, right));

            assertNull(mChannel.peekQueuedAudioSegment(), "replaying channel isn't assigned live calls");
            assertSame(live, right.peekQueuedAudioSegment());
        }
        finally
        {
            right.dispose();
        }
    }
}
