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

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;

import java.util.Iterator;

/**
 * A buffer of raw samples copied from native (ie outside of the JVM) memory into the JVM heap and
 * convertible to an iterable stream of interleaved or non-interleaved sample instances.
 */
public interface INativeBuffer
{
    /**
     * Provides an iterator that converts the raw samples to complex samples format with separate (non-interleaved) I and Q sample arrays.
     */
    Iterator<ComplexSamples> iterator();

    /**
     * Provides an iterator that converts the raw samples to interleaved complex samples format.
     */
    Iterator<InterleavedComplexSamples> iteratorInterleaved();

    /**
     * Total number of complex samples for this buffer
     */
    int sampleCount();

    /**
     * Timestamp for this buffer
     * @return millis since epoch
     */
    long getTimestamp();
}
