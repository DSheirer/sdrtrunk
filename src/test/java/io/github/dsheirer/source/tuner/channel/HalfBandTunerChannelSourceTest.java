/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.channel;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the heterodyne channel source applies frequency updates on the sample processing thread.
 */
public class HalfBandTunerChannelSourceTest
{
    private static final long CHANNEL_FREQUENCY = 460_000_000L;
    private static final long TUNER_FREQUENCY = 460_400_000L;
    private static final double SAMPLE_RATE = 2_400_000.0;

    private static HalfBandTunerChannelSource<INativeBuffer> source() throws Exception
    {
        HalfBandTunerChannelSource<INativeBuffer> source = new HalfBandTunerChannelSource<>(sourceEvent -> {},
            new TunerChannel(CHANNEL_FREQUENCY, 12500), SAMPLE_RATE,
            new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0), "test", null);
        source.setListener(samples -> {});
        return source;
    }

    private static void process(HalfBandTunerChannelSource<INativeBuffer> source)
    {
        source.new NativeBufferProcessor().receive(new FloatNativeBuffer(new float[8192], 0L, 0.0f));
    }

    /**
     * Test: the mixer only changes on the sample processing thread, using both the tuner frequency and the correction.
     */
    @Test
    void frequencyUpdatesAreAppliedDuringSampleProcessing() throws Exception
    {
        HalfBandTunerChannelSource<INativeBuffer> source = source();

        source.setFrequency(TUNER_FREQUENCY);
        source.setFrequencyCorrection(100);
        assertEquals(0, source.getMixerFrequency());

        process(source);
        assertEquals(TUNER_FREQUENCY - CHANNEL_FREQUENCY + 100, source.getMixerFrequency());
    }

    /**
     * Test: a sample rate that is too low to decimate passes through at the original sample rate.
     */
    @Test
    void lowSampleRateIsNotDecimated() throws Exception
    {
        HalfBandTunerChannelSource<INativeBuffer> source = new HalfBandTunerChannelSource<>(sourceEvent -> {},
            new TunerChannel(CHANNEL_FREQUENCY, 12500), 50000.0,
            new ChannelSpecification(50000.0, 12500, 6500.0, 7200.0), "test", null);

        assertEquals(50000.0, source.getSampleRate());
    }

    /**
     * Test: concurrent tuner frequency and channel correction updates are both reflected in the mixer.
     */
    @Test
    void concurrentUpdatesAreNotLost() throws Exception
    {
        for(int trial = 0; trial < 1000; trial++)
        {
            HalfBandTunerChannelSource<INativeBuffer> source = source();
            Thread tuner = new Thread(() -> source.setFrequency(TUNER_FREQUENCY));
            Thread correction = new Thread(() -> source.setFrequencyCorrection(100));
            tuner.start();
            correction.start();
            tuner.join();
            correction.join();

            process(source);
            assertEquals(TUNER_FREQUENCY - CHANNEL_FREQUENCY + 100, source.getMixerFrequency());
        }
    }
}
