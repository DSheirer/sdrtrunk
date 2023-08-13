/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.voice.embedded;

/**
 * Short Burst opcode
 *
 * See: https://patents.google.com/patent/US8271009B2 Page 12
 */
public enum ShortBurstOpcode
{
    NULL(0),
    ARC4_ENCRYPTION(1),
    TXI_DELAY(3),
    UNKNOWN(-1);

    private int mValue;

    /**
     * Constructor
     * @param value of the opcode
     */
    ShortBurstOpcode(int value)
    {
        mValue = value;
    }

    /**
     * Numeric value for the opcode
     * @return value.
     */
    private int getValue()
    {
        return mValue;
    }

    /**
     * Lookup the enum entry from the specified value.
     * @param value to lookup
     * @return matching entry or UNKNOWN.
     */
    public static ShortBurstOpcode fromValue(int value)
    {
        for(ShortBurstOpcode opcode: ShortBurstOpcode.values())
        {
            if(opcode.getValue() == value)
            {
                return opcode;
            }
        }

        return UNKNOWN;
    }
}
