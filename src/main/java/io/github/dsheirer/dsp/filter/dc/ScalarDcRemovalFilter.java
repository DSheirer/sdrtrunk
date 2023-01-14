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

import java.util.Random;

public class ScalarDcRemovalFilter implements IDcRemovalFilter
{
    private float mAverage;
    private float mGain;

    public ScalarDcRemovalFilter(float gain)
    {
        mGain = gain;
    }

    @Override
    public float[] filter(float[] samples)
    {
        float accumulator = 0.0f;
        float average = mAverage;

        for(float sample: samples)
        {
            accumulator += sample;
        }

        float averageDCNow = (accumulator / samples.length) - average;

        average += (mGain * averageDCNow);

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] -= average;
        }

        mAverage = average;

        return samples;
    }

    public static void main(String[] args)
    {
        System.out.println("Starting ...");
        Random random = new Random();
        ScalarDcRemovalFilter scalarFilter = new ScalarDcRemovalFilter(0.15f);
        VectorDcRemovalFilter vectorFilter = new VectorDcRemovalFilter(0.15f);

        float dcOffset = 0.03f;
        float[] samples = new float[8192];

        for(int y = 0; y < samples.length; y++)
        {
            samples[y] = (random.nextFloat() * 2.0f) - 1.0f + dcOffset;
        }

        long start = System.currentTimeMillis();

        for(int x = 0; x < 1_000_000; x++)
        {
//            scalarFilter.filter(samples);
            vectorFilter.filter(samples);
        }

        long duration = System.currentTimeMillis() - start;

        System.out.println("Finished - Duration: " + duration);
    }
}
