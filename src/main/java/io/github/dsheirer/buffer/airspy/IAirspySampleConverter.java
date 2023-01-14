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

package io.github.dsheirer.buffer.airspy;

import java.nio.ByteBuffer;

/**
 * Interface to convert airspy samples from a byte buffer to an array of shorts.
 */
public interface IAirspySampleConverter
{
    /**
     * Converts the airspy byte samples contained in the byte buffer to their short-valued representation
     * @param buffer of airspy samples
     * @return converted samples
     */
    short[] convert(ByteBuffer buffer);

    /**
     * Current DC average of the samples that have been processed thus far.
     */
    float getAverageDc();
}
