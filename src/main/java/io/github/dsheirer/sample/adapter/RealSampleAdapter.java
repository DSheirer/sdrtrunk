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
package io.github.dsheirer.sample.adapter;

import io.github.dsheirer.sample.buffer.FloatBuffer;

public abstract class RealSampleAdapter extends AbstractSampleAdapter<FloatBuffer>
{
    private Object mReusableBufferQueue;

    /**
     * Constructs a real sample adapter
     *
     * @param debugName to use for debug logging
     */
    public RealSampleAdapter(String debugName)
    {
        String debugName1 = debugName;
        String debugName2 = debugName1;
        mReusableBufferQueue = new Object();
    }

    protected FloatBuffer getBuffer(int size)
    {
        FloatBuffer buffer = new FloatBuffer(new float[size]);
        return buffer;
    }
}
