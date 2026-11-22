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

package io.github.dsheirer.dsp.squelch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.dsp.filter.ChannelFilterDesigner;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;
import io.github.dsheirer.dsp.fm.ScalarFMDemodulator;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the noise squelch normalization across channel bandwidths and sample rates.
 */
class NoiseSquelchTest
{
    private static final double[][] CHANNELS = {{7500.0, 25000.0}, {7500.0, 25600.0}, {7500.0, 18750.0},
            {12500.0, 25000.0}, {12500.0, 37500.0}, {12500.0, 48828.125}, {25000.0, 50000.0}};
    private static final int BUFFER_SIZE = 2048;
    private static final double SECONDS = 5.0;
    private static final double NOISE_LEVEL = 0.01;

    /**
     * A noise-only channel must stay squelched with the default thresholds.  A 7.5 kHz channel at 25 kHz has a noise
     * variance between the default open and close thresholds, which holds the squelch open if it's not normalized.
     */
    @Test
    void noiseOnlyChannelStaysSquelched()
    {
        for(double[] channel: CHANNELS)
        {
            double passed = process(channel[0], channel[1], null);
            assertEquals(0.0, passed, 0.01, "Noise passed for bandwidth [" + channel[0] + "] rate [" + channel[1] + "]");
        }
    }

    @Test
    void signalOpensSquelch()
    {
        for(double[] channel: CHANNELS)
        {
            double passed = process(channel[0], channel[1], 12.0);
            assertTrue(passed > 0.95, "Signal blocked [" + passed + "] for bandwidth [" + channel[0] + "] rate [" + channel[1] + "]");
        }
    }

    /**
     * The noise thresholds reference a 12.5 kHz channel at 25 kHz, so that channel must not be rescaled.
     */
    @Test
    void referenceChannelIsNotRescaled()
    {
        float[] filter = ChannelFilterDesigner.design(25000.0, 12500.0);
        assertEquals(0.298f, NoiseSquelch.getNoiseVariance(25000.0, filter), 0.01f);
    }

    /**
     * Processes an FM tone in noise, or noise only, through the channel filter, demodulator and noise squelch.
     * @param snr of the signal in the channel bandwidth in dB, or null for noise only.
     * @return fraction of the audio samples passed by the squelch
     */
    private static double process(double bandwidth, double sampleRate, Double snr)
    {
        Random random = new Random(2);
        int length = (int)(sampleRate * SECONDS);
        float[] i = new float[length];
        float[] q = new float[length];
        double amplitude = snr == null ? 0.0 :
            Math.sqrt(2.0 * NOISE_LEVEL * NOISE_LEVEL * (bandwidth / sampleRate) * Math.pow(10.0, snr / 10.0));
        double phase = 0.0;

        for(int x = 0; x < length; x++)
        {
            phase += 2.0 * Math.PI * (bandwidth * .2) * Math.sin(2.0 * Math.PI * 1000.0 * x / sampleRate) / sampleRate;
            i[x] = (float)(random.nextGaussian() * NOISE_LEVEL + amplitude * Math.cos(phase));
            q[x] = (float)(random.nextGaussian() * NOISE_LEVEL + amplitude * Math.sin(phase));
        }

        float[] coefficients = ChannelFilterDesigner.design(sampleRate, bandwidth);
        RealFIRFilter iFilter = new RealFIRFilter(coefficients.clone());
        RealFIRFilter qFilter = new RealFIRFilter(coefficients.clone());
        ScalarFMDemodulator demodulator = new ScalarFMDemodulator();
        NoiseSquelch squelch = new NoiseSquelch(NoiseSquelch.DEFAULT_NOISE_OPEN_THRESHOLD,
                NoiseSquelch.DEFAULT_NOISE_CLOSE_THRESHOLD, NoiseSquelch.DEFAULT_HYSTERESIS_OPEN_THRESHOLD,
                NoiseSquelch.DEFAULT_HYSTERESIS_CLOSE_THRESHOLD);
        squelch.setSampleRate(sampleRate, coefficients);

        long[] passed = {0};
        squelch.setAudioListener(audio -> {
            for(float sample: audio)
            {
                if(sample != 0.0f)
                {
                    passed[0]++;
                }
            }
        });

        for(int offset = 0; offset + BUFFER_SIZE <= length; offset += BUFFER_SIZE)
        {
            squelch.process(demodulator.demodulate(iFilter.filter(Arrays.copyOfRange(i, offset, offset + BUFFER_SIZE)),
                    qFilter.filter(Arrays.copyOfRange(q, offset, offset + BUFFER_SIZE))));
        }

        return (double)passed[0] / length;
    }
}
