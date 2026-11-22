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
package io.github.dsheirer.dsp.filter.decimate;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.window.WindowType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies decimation rate selection and the alias rejection of the decimation filters.
 */
public class DecimationFilterFactoryTest
{
    private static final double BANDWIDTH = 12500.0;
    private static final double DECIMATED_SAMPLE_RATE = 25000.0;
    private static final double MINIMUM_ALIAS_REJECTION_DB = 68.0;

    /**
     * Calculates the filter's gain for a real tone, from the output power after the filter settles.
     */
    private static double gainDb(int decimation, double frequency, double sampleRate)
    {
        IRealDecimationFilter filter = DecimationFilterFactory.getRealDecimationFilter(decimation);
        int blockSize = Math.max(4096, decimation * 8);
        int blocks = Math.max(64, 262144 / blockSize);
        double phaseStep = 2.0 * Math.PI * frequency / sampleRate;
        long sample = 0;
        double power = 0.0;
        int count = 0;

        for(int block = 0; block < blocks; block++)
        {
            float[] input = new float[blockSize];

            for(int x = 0; x < blockSize; x++)
            {
                input[x] = (float)Math.cos(phaseStep * sample++);
            }

            float[] output = filter.decimateReal(input);

            if(block > blocks / 2)
            {
                for(float value : output)
                {
                    power += value * value;
                    count++;
                }
            }
        }

        //A real tone with unity amplitude has an average power of 0.5
        return 10.0 * Math.log10(power / count / 0.5);
    }

    /**
     * Test: the requested rate is decimated by the largest supported rate that is less than the requested rate.
     */
    @Test
    void decimationRate()
    {
        assertEquals(2, DecimationFilterFactory.getDecimationRate(3));
        assertEquals(2, DecimationFilterFactory.getDecimationRate(4));
        assertEquals(4, DecimationFilterFactory.getDecimationRate(5));
        assertEquals(64, DecimationFilterFactory.getDecimationRate(96));
        assertEquals(1024, DecimationFilterFactory.getDecimationRate(2048));
        assertEquals(2048, DecimationFilterFactory.getDecimationRate(4096));
        assertEquals(4096, DecimationFilterFactory.getDecimationRate(4097));
        assertEquals(4096, DecimationFilterFactory.getDecimationRate(100000));
    }

    /**
     * Test: each decimation rate reduces the sample count by that rate and rejects a length that isn't a multiple.
     */
    @Test
    void highestRatesDecimateAndValidateLength()
    {
        for(int decimation : new int[]{1024, 2048, 4096})
        {
            assertEquals(1, DecimationFilterFactory.getRealDecimationFilter(decimation)
                .decimateReal(new float[decimation]).length);
            assertThrows(IllegalArgumentException.class, () -> DecimationFilterFactory
                .getRealDecimationFilter(decimation).decimateReal(new float[decimation / 2]));
        }
    }

    /**
     * Test: rates that are too small to decimate apply no decimation, instead of a zero rate or a rate that decimates
     * below the requested sample rate.
     */
    @Test
    void noDecimationWhenRateIsTooSmall()
    {
        for(int requestedRate : new int[]{0, 1, 2})
        {
            assertEquals(1, DecimationFilterFactory.getDecimationRate(requestedRate));
        }
    }

    /**
     * Test: negative rates are rejected.
     */
    @Test
    void negativeRateIsRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> DecimationFilterFactory.getDecimationRate(-1));
    }

    /**
     * Test: half-band filters have unity gain at DC and a null at the Nyquist frequency, across filter lengths.
     */
    @Test
    void halfBandDcGainAndNyquistNull()
    {
        for(int length : new int[]{11, 15, 23, 63})
        {
            float[] coefficients = FilterFactory.getHalfBand(length, WindowType.BLACKMAN);
            double dc = 0.0;
            double nyquist = 0.0;

            for(int x = 0; x < coefficients.length; x++)
            {
                dc += coefficients[x];
                nyquist += (x % 2 == 0) ? coefficients[x] : -coefficients[x];
            }

            assertEquals(1.0, dc, 1e-6);
            assertEquals(0.0, nyquist, 1e-6);
        }
    }

    /**
     * Test: tones that alias into the channel are rejected and tones within the channel are passed, for the band edge
     * of the final stage and for tones near the Nyquist frequency of the first stage.
     */
    @Test
    void aliasRejection()
    {
        for(int decimation = 2; decimation <= DecimationFilterFactory.MAXIMUM_DECIMATION_RATE; decimation *= 2)
        {
            double sampleRate = DECIMATED_SAMPLE_RATE * decimation;

            assertEquals(0.0, gainDb(decimation, BANDWIDTH / 2.0, sampleRate), 0.1);

            //Closest alias into the channel at the final stage
            double finalStageAlias = DECIMATED_SAMPLE_RATE - (BANDWIDTH / 2.0);
            assertTrue(gainDb(decimation, finalStageAlias, sampleRate) < -MINIMUM_ALIAS_REJECTION_DB);

            if(decimation > 2)
            {
                double firstStageAlias = (sampleRate / 2.0) - (BANDWIDTH / 2.0);
                assertTrue(gainDb(decimation, firstStageAlias, sampleRate) < -MINIMUM_ALIAS_REJECTION_DB);
            }
        }
    }
}
