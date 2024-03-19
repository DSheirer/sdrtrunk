/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.bits;

/**
 * Defines a fragmented or non-contiguous bit field within a binary message.
 * @param indices for the bits in the field.
 */
public record FragmentedIntField(int... indices)
{
    public FragmentedIntField
    {
        if(indices.length > 32)
        {
            throw new IllegalArgumentException("Integer field indices size [" + indices.length + "] cannot exceed 32-bits for an integer");
        }
    }

    /**
     * Utility constructor method.
     * @param indices (inclusive)
     * @return constructed fragmented integer field.
     */
    public static FragmentedIntField of(int... indices)
    {
        return new FragmentedIntField(indices);
    }
}
