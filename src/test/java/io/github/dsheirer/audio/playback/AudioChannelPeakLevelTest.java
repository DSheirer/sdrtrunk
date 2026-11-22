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

import io.github.dsheirer.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Peak level measurement used by the audio level meters.
 */
public class AudioChannelPeakLevelTest
{
    private AudioChannel mChannel;

    @BeforeEach
    public void setup()
    {
        mChannel = new AudioChannel(new Settings(), "TEST");
    }

    private static float[] interval(float... values)
    {
        float[] samples = new float[AudioChannel.SAMPLES_PER_INTERVAL];
        System.arraycopy(values, 0, samples, 0, values.length);
        return samples;
    }

    @Test
    public void peakIsMaximumMagnitudeAndResets()
    {
        mChannel.playTest(interval(0.25f, -0.5f, 0.1f));
        assertNotNull(mChannel.getAudio());
        assertEquals(0.5f, mChannel.getAndResetPeakLevel(), 1e-6f);
        assertEquals(0.0f, mChannel.getAndResetPeakLevel(), 0.0f, "peak resets after it is read");
    }

    @Test
    public void peakAccumulatesAcrossIntervalsUntilRead()
    {
        mChannel.playTest(interval(0.3f));
        mChannel.getAudio();
        mChannel.playTest(interval(-0.7f));
        mChannel.getAudio();
        mChannel.playTest(interval(0.2f));
        mChannel.getAudio();
        assertEquals(0.7f, mChannel.getAndResetPeakLevel(), 1e-6f);
    }

    @Test
    public void mutedChannelPlaysSilenceButMeasuresAudio()
    {
        mChannel.setMuted(true);
        mChannel.playTest(interval(0.6f, -0.4f));
        float[] audio = mChannel.getAudio();
        assertArrayEquals(new float[AudioChannel.SAMPLES_PER_INTERVAL], audio, "muted channel plays silence");
        assertEquals(0.6f, mChannel.getAndResetPeakLevel(), 1e-6f, "level is measured before muting");
    }

    @Test
    public void noAudioLeavesPeakAtZero()
    {
        mChannel.getAudio();
        assertEquals(0.0f, mChannel.getAndResetPeakLevel(), 0.0f);
    }
}
