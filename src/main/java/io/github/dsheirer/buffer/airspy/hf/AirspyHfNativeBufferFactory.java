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

package io.github.dsheirer.buffer.airspy.hf;

import io.github.dsheirer.buffer.AbstractNativeBufferFactory;
import io.github.dsheirer.buffer.DcCorrectionManager;
import io.github.dsheirer.buffer.INativeBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Airspy HF+ native buffer factory.
 *
 * Note: the tuner provides 1024 samples in each transfer where samples are 16-bit interleaved complex.
 */
public class AirspyHfNativeBufferFactory extends AbstractNativeBufferFactory
{
    private DcCorrectionManager mDcCorrectionManager = new DcCorrectionManager();

    /**
     * Constructs an instance
     */
    public AirspyHfNativeBufferFactory()
    {
    }

    /**
     * Converts the samples byte buffer into a native buffer and calculates DC offset
     * @param samples byte array copied from native memory
     * @param timestamp of the samples
     * @return constructed native buffer
     */
    @Override
    public INativeBuffer getBuffer(ByteBuffer samples, long timestamp)
    {
        ShortBuffer shortBuffer = samples.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] converted = new short[shortBuffer.capacity()];
        shortBuffer.get(converted);

        if(mDcCorrectionManager.shouldCalculateDc())
        {
            calculateDc(converted);
        }

        return new AirspyHfNativeBuffer(timestamp, getSamplesPerMillisecond(), mDcCorrectionManager.getAverageDc(), converted);
    }

    /**
     * Calculates the average DC in the sample stream so that it can be subtracted from the samples when the
     * native buffer is used.
     * @param samples containing DC offset
     */
    private void calculateDc(short[] samples)
    {
        float dcAccumulator = 0;

        for(short sample: samples)
        {
            dcAccumulator += sample;
        }

        dcAccumulator /= samples.length;
        mDcCorrectionManager.adjust(dcAccumulator * AirspyHfNativeBuffer.SCALE);
    }
}
