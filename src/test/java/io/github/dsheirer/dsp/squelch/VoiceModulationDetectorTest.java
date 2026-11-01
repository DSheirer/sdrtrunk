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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the voice modulation detector
 */
class VoiceModulationDetectorTest
{
    private static final double[] BANDWIDTHS = {3000.0, 5000.0, 6000.0, 8333.0, 15000.0, 25000.0};

    /**
     * Fills the detector with a carrier having white envelope noise and optional modulation.
     */
    private static boolean analyze(double sampleRate, double bandwidth, double modulation, Random random)
    {
        int samples = (int)(sampleRate / 2.0);
        VoiceModulationDetector detector = new VoiceModulationDetector(sampleRate, bandwidth, samples);

        for(int x = 0; x < samples; x++)
        {
            double time = x / sampleRate;
            detector.receive((float)(1.0 + modulation * Math.cos(2.0 * Math.PI * 450.0 * time) + 0.1 * random.nextGaussian()));
        }

        return detector.analyze();
    }

    @Test
    void flatEnvelopeNoiseIsNotVoice()
    {
        Random random = new Random(1);

        for(double bandwidth: BANDWIDTHS)
        {
            for(double sampleRate: new double[]{25000.0, 39062.5, 50000.0})
            {
                for(int x = 0; x < 50; x++)
                {
                    assertFalse(analyze(sampleRate, bandwidth, 0.0, random), "Bandwidth " + bandwidth);
                }
            }
        }
    }

    @Test
    void lowFrequencyModulationIsVoice()
    {
        Random random = new Random(2);

        for(double bandwidth: BANDWIDTHS)
        {
            assertTrue(analyze(39062.5, bandwidth, 0.2, random), "Bandwidth " + bandwidth);
        }
    }

    @Test
    void partialBufferIsNotAnalyzed()
    {
        VoiceModulationDetector detector = new VoiceModulationDetector(25000.0, 6000.0, 12500);
        detector.receive(1.0f);
        assertFalse(detector.analyze());
    }
}
