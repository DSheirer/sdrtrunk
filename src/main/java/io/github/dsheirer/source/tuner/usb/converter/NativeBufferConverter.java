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
package io.github.dsheirer.source.tuner.usb.converter;

import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public abstract class NativeBufferConverter
{
    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("NativeBufferConverter");

    /**
     * Converts native byte buffers into complex float samples and produces reusable complex sample buffers.  Tracks
     * each reusable buffer until the downstream consumer(s) are finished with the buffer and then reuses the buffer.
     */
    public NativeBufferConverter()
    {
    }

    /**
     * Converts the native byte buffer to complex float samples and returns the samples in a reusable buffer.  Internally
     * tracks each reusable buffer until all consumers indicate they are finished processing the buffer and then reuses
     * the buffer.
     *
     * @param byteBuffer containing native memory byte samples
     * @return native buffer samples converted to complex floats loaded into a reusable buffer
     */
    public ReusableComplexBuffer convert(ByteBuffer byteBuffer, int length)
    {
        FloatBuffer floatBuffer = convertSamples(byteBuffer, length);

        ReusableComplexBuffer reusableComplexBuffer = mReusableComplexBufferQueue.getBuffer(floatBuffer.capacity());

        //The reusable buffer will rewind the float buffer to the beginning
        reusableComplexBuffer.reloadFrom(floatBuffer, System.currentTimeMillis());
        reusableComplexBuffer.incrementUserCount();

        return reusableComplexBuffer;
    }

    /**
     * Converts the native byte buffer bytes into complex float samples.
     *
     * @param buffer containing native byte buffer samples
     * @return a buffer with complex float samples.  This float buffer will not be modified, therefore sub-class
     * implementations can reuse the float buffer.
     */
    protected abstract FloatBuffer convertSamples(ByteBuffer buffer, int length);
}
