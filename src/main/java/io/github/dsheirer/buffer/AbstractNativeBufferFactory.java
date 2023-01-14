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

package io.github.dsheirer.buffer;

/**
 * Base class for native buffer factories.
 */
public abstract class AbstractNativeBufferFactory implements INativeBufferFactory
{
    private float mSamplesPerMillisecond = 0.0f;

    @Override
    public void setSamplesPerMillisecond(float samplesPerMillisecond)
    {
        mSamplesPerMillisecond = samplesPerMillisecond;
    }

    /**
     * Quantity of I/Q sample pairs per milli-second at the current sample rate to use in calculating an accurate
     * timestamp for sub-buffer that are generated from the native buffer.
     * @return samples per millisecond.
     */
    public float getSamplesPerMillisecond()
    {
        return mSamplesPerMillisecond;
    }
}
