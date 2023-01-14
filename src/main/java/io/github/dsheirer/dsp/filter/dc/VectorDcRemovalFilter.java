/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.dsp.filter.dc;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.util.Random;

public class VectorDcRemovalFilter implements IDcRemovalFilter
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;
    private float mAverageDc;
    private float mGain;

    public VectorDcRemovalFilter(float gain)
    {
        mGain = gain;
    }

    @Override
    public float[] filter(float[] samples)
    {
        FloatVector accumulator = FloatVector.zero(VECTOR_SPECIES);

        for(int pointer = 0; pointer < samples.length; pointer += VECTOR_SPECIES.length())
        {
            accumulator = accumulator.add(FloatVector.fromArray(VECTOR_SPECIES, samples, pointer));
        }

        float sum = accumulator.reduceLanes(VectorOperators.ADD);
        float average = mAverageDc;
        float averageDCNow = (sum / samples.length) - average;
        average += (mGain * averageDCNow);

        for(int pointer = 0; pointer < samples.length; pointer += VECTOR_SPECIES.length())
        {
            //Load into vector, subtract dc, and write back to sample array
            FloatVector.fromArray(VECTOR_SPECIES, samples, pointer).add(average).intoArray(samples, pointer);
        }

        mAverageDc = average;

        return samples;
    }

    public static void main(String[] args)
    {
        System.out.println("Starting ...");
        Random random = new Random();
        VectorDcRemovalFilter filter = new VectorDcRemovalFilter(0.15f);

        float dcOffset = 0.03f;

        for(int x = 0; x < 10; x++)
        {
            float[] samples = new float[8192];

            for(int y = 0; y < samples.length; y++)
            {
                samples[y] = (random.nextFloat() * 2.0f) - 1.0f + dcOffset;
            }

            samples = filter.filter(samples);
        }

        System.out.println("Finished!");
    }
}
