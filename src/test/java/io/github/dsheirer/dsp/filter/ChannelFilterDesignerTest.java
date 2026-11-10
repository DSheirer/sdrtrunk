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
package io.github.dsheirer.dsp.filter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the channel filter design
 */
class ChannelFilterDesignerTest
{
    private static final double[] BANDWIDTHS = {3000.0, 5000.0, 6000.0, 7500.0, 8333.0, 12500.0, 15000.0, 25000.0};

    /**
     * Each channel filter must be valid across decimated sample rates, which includes rates where the remez designer
     * previously produced a filter with a large response spike at half of the sample rate.
     */
    @Test
    void channelFilterResponseIsValidForDecimatedSampleRates()
    {
        for(double bandwidth: BANDWIDTHS)
        {
            for(double sampleRate = bandwidth; sampleRate < 4.0 * bandwidth; sampleRate += bandwidth / 32.0)
            {
                check(sampleRate, bandwidth);
            }
        }
    }

    @Test
    void tunerDerivedSampleRates()
    {
        check(11718.75, 3000.0);
        check(19531.25, 5000.0);
        check(50000.0, 15000.0);
        check(29296.875, 7500.0);
        check(48828.125, 12500.0);
        check(25000.0, 7500.0);
    }

    /**
     * These sample rates use the fallback filter design, which must keep the same bandwidth as the remez design.
     */
    @Test
    void fallbackFilterKeepsChannelBandwidth()
    {
        checkPassBand(11718.75, 3000.0);
        checkPassBand(19531.25, 5000.0);
        checkPassBand(50000.0, 15000.0);
    }

    private static void checkPassBand(double sampleRate, double bandwidth)
    {
        float[] coefficients = ChannelFilterDesigner.design(sampleRate, bandwidth);
        double response = ChannelFilterDesigner.getResponseDb(coefficients, bandwidth * .4, sampleRate) -
            ChannelFilterDesigner.getResponseDb(coefficients, 0.0, sampleRate);
        assertTrue(response > -1.0, "Pass band edge response [" + response + " dB] at sample rate [" + sampleRate + "]");
    }

    private static void check(double sampleRate, double bandwidth)
    {
        float[] coefficients = ChannelFilterDesigner.design(sampleRate, bandwidth);
        assertTrue(ChannelFilterDesigner.isValidFilterResponse(coefficients, sampleRate, (int)(bandwidth * .4), (int)(bandwidth * .5)),
            "Filter response for sample rate [" + sampleRate + "] bandwidth [" + bandwidth + "]");
    }

    @Test
    void responseCheckRejectsSpikeAtNyquist()
    {
        //Low-pass moving average with an added alternating sequence that peaks at half of the sample rate.
        float[] coefficients = new float[31];

        for(int x = 0; x < coefficients.length; x++)
        {
            coefficients[x] = 1.0f / coefficients.length + (x % 2 == 0 ? 0.1f : -0.1f);
        }

        assertTrue(!ChannelFilterDesigner.isValidFilterResponse(coefficients, 25000.0, 3000, 4000));
    }
}
