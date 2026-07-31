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

/**
 * Access method
 */
public enum Duplex
{
    HALF_DUPLEX("HALF DUPLEX"),
    DUPLEX("FULL DUPLEX");

    private final String mLabel;

    /**
     * Constructs an instance
     * @param label to display
     */
    Duplex(String label)
    {
        mLabel = label;
    }

    /**
     * Pretty format
     * @return format
     */
    @Override
    public String toString()
    {
        return mLabel;
    }
}
