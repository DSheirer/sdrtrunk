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

package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that two adjacent M2 channelizer outputs synthesized by {@link TwoChannelSynthesizerM2} recover a tone
 * anywhere across the channel boundary with a flat, symmetric response.  This requires that adjacent channelizer
 * outputs are phase coherent (for both odd and even taps per channel) and that the synthesizer applies the same
 * response to each channel.
 */
public class TwoChannelSynthesizerM2Test
{
    private static final int CHANNEL_COUNT = 8;
    private static final double CHANNEL_BANDWIDTH = 25000.0;
    private static final double SAMPLE_RATE = CHANNEL_COUNT * CHANNEL_BANDWIDTH;
    private static final int LOWER_CHANNEL = 2;
    private static final int SAMPLES = 2048 * CHANNEL_COUNT; //Produces 4096 M2 channel results (4 dispatch batches)
    private static final int SETTLE = 100;

    /**
     * Test: tones at mirrored offsets from the boundary between two channels produce equal output levels, and a tone
     * exactly on the boundary produces the same level as one near the center of the composite.
     */
    @Test
    void boundaryResponseIsFlatAndSymmetric() throws Exception
    {
        for(int tapsPerChannel : new int[]{8, 9})
        {
            double center = gain(0.0, tapsPerChannel);
            assertEquals(1.0, center, 0.01, "Unity gain at channel boundary (taps per channel=" + tapsPerChannel + ")");

            for(double offset : new double[]{0.1, 0.2, 0.3, 0.4})
            {
                double below = gain(-offset, tapsPerChannel);
                double above = gain(offset, tapsPerChannel);
                assertEquals(below, above, 0.01, "Asymmetric response at +/-" + offset +
                    " channel bandwidths from boundary (taps per channel=" + tapsPerChannel + ")");

                //Synthesis filter cutoff is 0.55 channel bandwidths, so only check flatness for the inner region
                if(offset <= 0.25)
                {
                    assertTrue(below > 0.98, "Passband droop at " + offset + " channel bandwidths: " + below);
                }
            }
        }
    }

    /**
     * Channelizes a tone at the specified offset from the boundary between two channels, synthesizes the two channels
     * and returns the output tone amplitude, normalized for the channelizer gain.
     * @param offset from the channel boundary as a fraction of the channel bandwidth
     * @param tapsPerChannel for the channelizer filter
     */
    private static double gain(double offset, int tapsPerChannel) throws Exception
    {
        List<float[]> results = Collections.synchronizedList(new ArrayList<>());
        float[] taps = FilterFactory.getSincM2Channelizer(CHANNEL_BANDWIDTH, CHANNEL_COUNT, tapsPerChannel, false);
        ComplexPolyphaseChannelizerM2 channelizer = new ComplexPolyphaseChannelizerM2(taps, (int)SAMPLE_RATE,
            CHANNEL_COUNT)
        {
            @Override
            protected void dispatch(List<float[]> channelResultsList)
            {
                results.addAll(channelResultsList);
            }
        };

        double frequency = (LOWER_CHANNEL + 0.5 + offset) * CHANNEL_BANDWIDTH;
        float[] samples = new float[SAMPLES * 2];
        for(int x = 0; x < SAMPLES; x++)
        {
            double phase = 2.0 * Math.PI * frequency * x / SAMPLE_RATE;
            samples[2 * x] = (float)Math.cos(phase);
            samples[2 * x + 1] = (float)Math.sin(phase);
        }

        channelizer.start();
        channelizer.receive(new InterleavedComplexSamples(samples, 0));
        int expected = SAMPLES * 2 / CHANNEL_COUNT;
        long timeout = System.currentTimeMillis() + 5000;
        while(results.size() < expected && System.currentTimeMillis() < timeout)
        {
            Thread.sleep(10);
        }
        channelizer.stop();
        assertEquals(expected, results.size(), "Channelizer results");

        //Extract both channels and translate the upper channel by FS/2 using the absolute block index parity
        float[] i1 = new float[expected], q1 = new float[expected], i2 = new float[expected], q2 = new float[expected];
        for(int x = 0; x < expected; x++)
        {
            float[] result = results.get(x);
            float sign = (x & 1) == 0 ? 1.0f : -1.0f;
            i1[x] = result[2 * LOWER_CHANNEL];
            q1[x] = result[2 * LOWER_CHANNEL + 1];
            i2[x] = sign * result[2 * (LOWER_CHANNEL + 1)];
            q2[x] = sign * result[2 * (LOWER_CHANNEL + 1) + 1];
        }

        float[] filter = FilterFactory.getSincM2Synthesizer(2.0 * CHANNEL_BANDWIDTH, CHANNEL_BANDWIDTH, 2, 9);
        ComplexSamples output = new TwoChannelSynthesizerM2(filter).process(new ComplexSamples(i1, q1, 0),
            new ComplexSamples(i2, q2, 0));

        //Correlate against the expected tone, centered at +FS/4 + offset/2 in the composite output
        double outputFrequency = 0.25 + offset / 2.0;
        double re = 0, im = 0;
        int count = 0;
        for(int x = SETTLE; x < expected; x++, count++)
        {
            double angle = -2.0 * Math.PI * outputFrequency * x;
            re += output.i()[x] * Math.cos(angle) - output.q()[x] * Math.sin(angle);
            im += output.i()[x] * Math.sin(angle) + output.q()[x] * Math.cos(angle);
        }

        return Math.sqrt(re * re + im * im) / count * CHANNEL_COUNT;
    }
}
