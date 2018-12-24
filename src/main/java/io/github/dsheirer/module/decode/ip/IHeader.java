/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.ip;

/**
 * Data Packet Header
 */
public interface IHeader
{
    /**
     * Indicates that this header is valid and that it meets the length requirements.
     */
    boolean isValid();

    /**
     * Indicates the length of this header.  An implicit understanding is that any data that follows
     * the header (ie payload) starts after the header length.
     * @return length of this header in bytes.
     */
    int getLength();
}
