/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
 * Data protection types
 */
public enum CipherType
{
    UNENCRYPTED("UNENCRYPTED"),
    SCRAMBLE("SCRAMBLE"),
    DES("DES"),
    AES("AES");

    private String mLabel;

    /**
     * Constructs an instance
     *
     * @param label to display
     */
    CipherType(String label)
    {
        mLabel = label;
    }

    /**
     * Utility method to look up the cipher type from the transmitted value.
     *
     * @param value to look up
     * @return type
     */
    public static CipherType fromValue(int value)
    {
        return switch(value)
        {
            case 1 -> SCRAMBLE;
            case 2 -> DES;
            case 3 -> AES;
            default -> UNENCRYPTED;
        };
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
