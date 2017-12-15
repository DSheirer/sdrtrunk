/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package source.tuner.airspy;

import dsp.filter.dc.DCRemovalFilter_RB;
import dsp.filter.hilbert.HilbertTransform;
import sample.adapter.ISampleAdapter;

public class AirspySampleAdapter implements ISampleAdapter
{
    private static final float SCALE_SIGNED_12_BIT_TO_FLOAT = 1.0f / 2048.0f;

    private DCRemovalFilter_RB mDCFilter = new DCRemovalFilter_RB(0.01f);
    private HilbertTransform mHilbertTransform = new HilbertTransform();
    private boolean mSamplePacking = false;


    /**
     * Adapter to translate byte buffers received from the airspy tuner into
     * float buffers for processing.
     */
    public AirspySampleAdapter()
    {
    }

    /**
     * Sample packing places two 12-bit samples into 3 bytes when enabled or
     * places two 12-bit samples into 4 bytes when disabled.
     *
     * @param enabled
     */
    public void setSamplePacking(boolean enabled)
    {
        mSamplePacking = enabled;
    }

    @Override
    public float[] convert(byte[] samples)
    {
        float[] realSamples;

        if(mSamplePacking)
        {
            realSamples = convertPacked(samples);
        }
        else
        {
            realSamples = convertUnpacked(samples);
        }

        mDCFilter.filter(realSamples);

        return mHilbertTransform.filter(realSamples);
    }

    /**
     * Converts the byte array containing unsigned 12-bit short values into
     * signed float values in the range -1 to 1;
     *
     * @param data - byte array of unsigned 16-bit values
     * @return converted float values
     */
    private float[] convertUnpacked(byte[] data)
    {
        float[] samples = new float[data.length / 2];

        int pointer = 0;

        for(int x = 0; x < data.length; x += 2)
        {
            samples[pointer++] = scale((data[x] & 0xFF) |
                (data[x + 1] << 8));
        }

        return samples;
    }

    /**
     * Converts every 3 bytes containing a pair of 12-bit unsigned values into
     * a pair of float values in the range -1 to 1;
     *
     * @param data1 - byte array of unsigned 12-bit values
     * @return converted float values
     */
    private float[] convertPacked(byte[] data1)
    {
        byte[] data = new byte[data1.length];

        //Convert big-endian to little-endian
        for(int x = 0; x < data1.length; x += 4)
        {
            data[x] = data1[x + 3];
            data[x + 1] = data1[x + 2];
            data[x + 2] = data1[x + 1];
            data[x + 3] = data1[x];
        }

        int count = (int) ((float) data.length / 1.5f);

		/* Ensure we have an even number of samples */
        if(count % 2 == 1)
        {
            count--;
        }

        int bytes = (int) ((float) count * 1.5f);

        float[] samples = new float[count];

        int pointer = 0;

        int first;
        int second;

        for(int x = 0; x < bytes; x += 3)
        {
            first = ((data[x] << 4) & 0xFF0) |
                ((data[x + 1] >> 4) & 0xF);

            samples[pointer++] = scale(first);

            second = ((data[x + 1] << 8) & 0xF00) |
                (data[x + 2] & 0xFF);

            samples[pointer++] = scale(second);
        }

        return samples;
    }

    /**
     * Converts unsigned 12-bit values to signed 12-bit values and then scales
     * the signed value to a signed float value in range: -1.0 : +1.0
     */
    public static float scale(int value)
    {
        return (float) ((value & 0xFFF) - 2048) * SCALE_SIGNED_12_BIT_TO_FLOAT;
    }
}
