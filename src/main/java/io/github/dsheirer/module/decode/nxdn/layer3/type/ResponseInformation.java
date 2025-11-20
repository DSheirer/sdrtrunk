/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

import org.jspecify.annotations.NonNull;

/**
 * 16-bit response information field.
 * @param value of the 16-bit field
 */
public record ResponseInformation(int value)
{
    private static final int MASK_CLASS_TYPE = 0x3E00;
    private static final int MASK_RX_FRAGMENT_COUNT = 0x1FF;

    @NonNull
    @Override
    public String toString()
    {
        return getResponseClassType() + " FRAGMENTS RECEIVED:" + getRxFragmentCount();
    }

    /**
     * Response class and type.
     */
    public ResponseClassType getResponseClassType()
    {
        return ResponseClassType.fromValue(value & MASK_CLASS_TYPE);
    }

    /**
     * Count of received fragments
     * @return count
     */
    public int getRxFragmentCount()
    {
        return value & MASK_RX_FRAGMENT_COUNT;
    }
}
