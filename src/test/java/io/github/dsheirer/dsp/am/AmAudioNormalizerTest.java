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
package io.github.dsheirer.dsp.am;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the AM audio normalizer
 */
class AmAudioNormalizerTest
{
    private static final double SAMPLE_RATE = 25000.0;
    private static final float MODULATION = 0.8f;

    private static float envelope(float carrier, int sample)
    {
        return carrier * (1.0f + MODULATION * (float)Math.cos(2.0 * Math.PI * 1000.0 / SAMPLE_RATE * sample));
    }

    @Test
    void outputFollowsModulationDepthAtAnyCarrierLevel()
    {
        for(float carrier: new float[]{0.0005f, 0.002f, 0.01f, 0.03f, 0.1f})
        {
            checkOutput(carrier);
        }
    }

    private static void checkOutput(float carrier)
    {
        AmAudioNormalizer normalizer = new AmAudioNormalizer(SAMPLE_RATE);
        double sum = 0.0;
        float peak = 0.0f;
        int settle = (int)SAMPLE_RATE;
        int count = (int)SAMPLE_RATE;

        for(int x = 0; x < settle + count; x++)
        {
            float audio = normalizer.process(envelope(carrier, x));

            if(x >= settle)
            {
                sum += audio;
                peak = Math.max(peak, Math.abs(audio));
            }
        }

        assertEquals(0.6f, peak, 0.02f, "Peak");
        assertEquals(0.0, sum / count, 0.002, "DC");
    }

    @Test
    void transmissionStartHasNoTransient()
    {
        AmAudioNormalizer normalizer = new AmAudioNormalizer(SAMPLE_RATE);

        for(int x = 0; x < (int)SAMPLE_RATE; x++)
        {
            assertTrue(Math.abs(normalizer.process(envelope(0.01f, x))) < 0.8f, "Transient at sample " + x);
        }
    }

    @Test
    void noiseAfterCarrierDropIsNotAmplifiedToCarrierLevel()
    {
        AmAudioNormalizer normalizer = new AmAudioNormalizer(SAMPLE_RATE);

        for(int x = 0; x < (int)SAMPLE_RATE; x++)
        {
            normalizer.process(envelope(0.01f, x));
        }

        java.util.Random random = new java.util.Random(1);
        float peak = 0.0f;

        //Noise is 40 dB below the carrier.  Skip the carrier estimate decay.
        for(int x = 0; x < (int)(SAMPLE_RATE * 0.5); x++)
        {
            float audio = normalizer.process(0.0001f * (1.0f + random.nextFloat()));

            if(x > (int)(SAMPLE_RATE * 0.2))
            {
                peak = Math.max(peak, Math.abs(audio));
            }
        }

        assertTrue(peak < 0.05f, "Noise peak " + peak);
    }

    @Test
    void outputIsLimited()
    {
        AmAudioNormalizer normalizer = new AmAudioNormalizer(SAMPLE_RATE);

        for(int x = 0; x < 1000; x++)
        {
            normalizer.process(0.001f);
        }

        assertTrue(normalizer.process(1.0f) <= 0.95f);
    }
}
