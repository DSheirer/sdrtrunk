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

package io.github.dsheirer.dsp.fm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit tests for the FM discriminator soft symbol reliability.
 */
public class FMDiscriminatorReliabilityTest
{
    private static final int BUFFER_LENGTH = 256;

    /**
     * Test: inverse power filter taps are the squared differences of the post-discriminator filter taps.
     */
    @Test
    void testInversePowerFilterTaps()
    {
        float[] taps = FMDiscriminatorReliability.getInversePowerFilterTaps(new float[]{0.25f, 0.5f, 0.25f});
        assertArrayEquals(new float[]{0.0625f, 0.0625f, 0.0625f, 0.0625f}, taps, 1e-7f, "Taps");

        //An integrate and dump (flat) filter depends only on the samples at either end of the filter span
        float[] flat = FMDiscriminatorReliability.getInversePowerFilterTaps(new float[]{0.2f, 0.2f, 0.2f, 0.2f, 0.2f});
        assertArrayEquals(new float[]{0.04f, 0f, 0f, 0f, 0f, 0.04f}, flat, 1e-7f, "Flat filter taps");
    }

    /**
     * Test: each reliability is 1 / sum(g[k]^2 / |x[n - k]|^2), including across the sample buffer boundary.
     */
    @Test
    void testReliabilities()
    {
        float[] filter = {0.1f, 0.2f, 0.4f, 0.2f, 0.1f};
        float[] g2 = FMDiscriminatorReliability.getInversePowerFilterTaps(filter);
        FMDiscriminatorReliability reliability = new FMDiscriminatorReliability(filter);
        float[] amplitudes = new float[2 * BUFFER_LENGTH];

        for(int x = 0; x < amplitudes.length; x++)
        {
            amplitudes[x] = 0.1f + 0.9f * (float)Math.abs(Math.sin(x * 0.37));
        }

        float[] reliabilities = new float[amplitudes.length];

        for(int buffer = 0; buffer < 2; buffer++)
        {
            float[] i = new float[BUFFER_LENGTH];
            float[] q = new float[BUFFER_LENGTH];

            for(int x = 0; x < BUFFER_LENGTH; x++)
            {
                //Rotate the phase to show that the reliability depends only on the sample power
                double phase = x * 0.5;
                float amplitude = amplitudes[buffer * BUFFER_LENGTH + x];
                i[x] = (float)(amplitude * Math.cos(phase));
                q[x] = (float)(amplitude * Math.sin(phase));
            }

            System.arraycopy(reliability.calculate(i, q), 0, reliabilities, buffer * BUFFER_LENGTH, BUFFER_LENGTH);
        }

        for(int x = g2.length; x < amplitudes.length; x++)
        {
            double sum = 0.0;

            for(int k = 0; k < g2.length; k++)
            {
                sum += g2[k] / (amplitudes[x - k] * amplitudes[x - k]);
            }

            assertEquals(1.0 / sum, reliabilities[x], 1e-4 / sum, "Reliability at " + x);
        }
    }
}
