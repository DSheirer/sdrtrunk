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

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD implementation of the channel DFT using the preferred vector size for the hardware.
 */
public class VectorChannelDFT extends ChannelDFT
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;

    /**
     * Break-even with an IFFT scales with the vector size and is about 20-40 channels with 16 lanes.  Using the lane
     * count stays below the break-even at each vector size.
     */
    private static final int MAXIMUM_CHANNELS = VECTOR_SPECIES.length();

    /**
     * Constructs an instance
     * @param channelCount as the size of the transform
     */
    public VectorChannelDFT(int channelCount)
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
        FloatVector inphaseAccumulator = FloatVector.zero(VECTOR_SPECIES);
        FloatVector quadratureAccumulator = FloatVector.zero(VECTOR_SPECIES);
        int laneCount = VECTOR_SPECIES.length();
        int vectorLength = VECTOR_SPECIES.loopBound(length);
        int x = 0;

        for(; x < vectorLength; x += laneCount)
        {
            FloatVector sample = FloatVector.fromArray(VECTOR_SPECIES, samples, offset + x);
            inphaseAccumulator = sample.fma(FloatVector.fromArray(VECTOR_SPECIES, inphaseCoefficients, x), inphaseAccumulator);
            quadratureAccumulator = sample.fma(FloatVector.fromArray(VECTOR_SPECIES, quadratureCoefficients, x), quadratureAccumulator);
        }

        float inphase = inphaseAccumulator.reduceLanes(VectorOperators.ADD);
        float quadrature = quadratureAccumulator.reduceLanes(VectorOperators.ADD);

        for(; x < length; x++)
        {
            float sample = samples[offset + x];
            inphase += sample * inphaseCoefficients[x];
            quadrature += sample * quadratureCoefficients[x];
        }

        results[resultsOffset] = inphase;
        results[resultsOffset + 1] = quadrature;
    }
}
