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

import java.nio.ByteBuffer;

/**
 * Factory for creating native buffer wrapper instances
 */
public interface INativeBufferFactory
{
    /**
     * Create a native buffer implementation that wraps the samples argument.
     *
     * Note: implementations of this factory should treat the samples argument as read-only and only make data copies.
     *
     * @param samples byte array copied from native memory
     * @param timestamp of the samples
     * @param samplesPerMillisecond to calculate timestamp offset for child buffers.
     * @return instance
     */
    INativeBuffer getBuffer(ByteBuffer samples, long timestamp);

    /**
     * Sets the samples per millisecond rate based on the current sample rate.
     *
     * @param samplesPerMillisecond to calculate timestamp offset for child buffers.
     */
    void setSamplesPerMillisecond(float samplesPerMillisecond);
}
