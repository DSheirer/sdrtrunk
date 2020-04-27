/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.sample.buffer;

public class ComplexBuffer extends FloatBuffer
{
    /**
     * Creates a reusable, timestamped complex buffer using the specified time in milliseconds.
     *
     * NOTE: reusability of this buffer requires strict user count tracking.  Each component that receives this
     * buffer should not modify the buffer contents.  Each component should also increment the user count before
     * sending this buffer to another component and should decrement the user count when finished using this buffer.
     *  @param samples of data
     * @param timestamp in millis for the buffer
     */
    ComplexBuffer(float[] samples, long timestamp)
    {
        super(samples, timestamp);
    }

    /**
     * Constructs a timestamped complex buffer using the current system time in milliseconds.
     *
     * NOTE: reusability of this buffer requires strict user count tracking.  Each component that receives this
     * buffer should not modify the buffer contents.  Each component should also increment the user count before
     * sending this buffer to another component and should decrement the user count when finished using this buffer.
     *
     * @param samples of data
     */
    public ComplexBuffer(float[] samples)
    {
        this(samples, System.currentTimeMillis());
    }

    /**
     * Number of complex samples contained in this buffer
     */
    @Override
    public int getSampleCount()
    {
        return getSamples().length / 2;
    }

    /**
     * Applies the gain value to the samples contained in this buffer
     */
    public void applyGain(double gain)
    {
        float[] samples = getSamples();

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] *= gain;
        }
    }
}
