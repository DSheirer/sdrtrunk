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

import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;

public abstract class ComplexSampleAdapter extends AbstractSampleAdapter<ReusableComplexBuffer>
{
    private ReusableComplexBufferQueue mReusableBufferQueue;

    /**
     * Constructs a real sample adapter
     *
     * @param debugName to use for debug logging
     */
    public ComplexSampleAdapter(String debugName)
    {
        mReusableBufferQueue = new ReusableComplexBufferQueue(debugName);
    }

    protected ReusableComplexBuffer getBuffer(int size)
    {
        return mReusableBufferQueue.getBuffer(size);
    }
}
