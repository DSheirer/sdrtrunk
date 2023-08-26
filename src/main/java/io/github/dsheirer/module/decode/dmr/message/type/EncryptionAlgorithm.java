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

package io.github.dsheirer.module.decode.dmr.message.type;

/**
 * Enumeration of encryption algorithms
 */
public enum EncryptionAlgorithm
{
    NO_ENCRYPTION(0x00, "NO ENCRYPTION"),
    HYTERA_BASIC_PRIVACY(0x01, "HYTERA BP"),
    HYTERA_ENHANCED_PRIVACY(0x02, "HYTERA RC4/EP"),
    DMRA_RC4(0x21, "DMRA RC4/EP"),
    DMRA_AES128(0x24, "DMRA AES128"),
    DMRA_AES256(0x25, "DMRA AES256" ),
    HYTERA_ENHANCED_PRIVACY_2(0x26, "HYTERA RC4/EP"),
    UNKNOWN(-1, "UNKNOWN");

    private final int mValue;
    private final String mLabel;

    /**
     * Constructor
     * @param value for the algorithm
     * @param label to display
     */
    EncryptionAlgorithm(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Numeric value for the algorithm
     * @return value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Utility method to lookup the encryption from the value.
     * @param value to lookup
     * @return matching entry or UNKNOWN
     */
    public static EncryptionAlgorithm fromValue(int value)
    {
        for(EncryptionAlgorithm algorithm: EncryptionAlgorithm.values())
        {
            if(algorithm.getValue() == value)
            {
                return algorithm;
            }
        }

        return UNKNOWN;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
