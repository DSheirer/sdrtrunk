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
package io.github.dsheirer.audio.convert;

import java.util.List;

public interface IAudioConverter
{
    /**
     * Converts the PCM audio packets to converted audio format.  May produce partial audio frame data.
     */
    List<byte[]> convert(List<float[]> audioBuffers);

    /**
     * Finalizes audio conversion by fully converting any partial frames left in the buffer and returning the
     * remaining bytes to produce a full frame.
     */
    List<byte[]> flush();
}
