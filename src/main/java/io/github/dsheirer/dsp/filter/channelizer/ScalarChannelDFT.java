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

/**
 * Scalar implementation of the channel DFT.
 */
public class ScalarChannelDFT extends ChannelDFT
{
    /**
     * Break-even with a mixed-radix IFFT is about 3-6 channels across typical channel counts.
     */
    private static final int MAXIMUM_CHANNELS = 3;

    /**
     * Constructs an instance
     * @param channelCount as the size of the transform
     */
    public ScalarChannelDFT(int channelCount)
    {
        super(channelCount);
    }

    @Override
    public int getMaximumChannels()
    {
        return MAXIMUM_CHANNELS;
    }

    @Override
    protected void transform(float[] samples, int offset, int length, float[] inphaseCoefficients,
                             float[] quadratureCoefficients, float[] results, int resultsOffset)
    {
        float inphase = 0.0f;
        float quadrature = 0.0f;

        for(int x = 0; x < length; x++)
        {
            float sample = samples[offset + x];
            inphase += sample * inphaseCoefficients[x];
            quadrature += sample * quadratureCoefficients[x];
        }

        results[resultsOffset] = inphase;
        results[resultsOffset + 1] = quadrature;
    }
}
