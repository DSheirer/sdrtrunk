/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.dsp.gain.complex;

/**
 * Applies gain to the I/Q values to achieve unity gain on at least one sample.
 */
public class UnityGain
{
    /**
     * Processes the I/Q samples and applies gain to achieve close to unity gain across the values.
     * @param iSamples to process
     * @param qSamples to process
     */
    public static void process(float[] iSamples, float[] qSamples)
    {
        float peak = 0;

        for(int x = 0; x < iSamples.length; x++)
        {
            float sample = Math.abs(iSamples[x]);
            peak = Math.max(peak, sample);
            sample = Math.abs(qSamples[x]);
            peak = Math.max(peak, sample);
        }

        float gain = 1.0f / peak;

        for(int x = 0; x < iSamples.length; x++)
        {
            iSamples[x] *= gain;
            qSamples[x] *= gain;
        }
    }

    /**
     * Processes the samples and applies gain to achieve close to unity gain across the values.
     * @param samples to process
     */
    public static void process(float[] samples)
    {
        float peak = 0;

        for(int x = 0; x < samples.length; x++)
        {
            float sample = Math.abs(samples[x]);
            peak = Math.max(peak, sample);
        }

        float gain = 1.0f / peak;

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] *= gain;
        }
    }
}
