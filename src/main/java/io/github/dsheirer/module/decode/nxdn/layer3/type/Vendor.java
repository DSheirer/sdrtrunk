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
 * NXDN vendor enumeration
 */
public enum Vendor
{
    //Unknown vendor values - have sample for vendor 0x68 and guessing it's JVC/Kenwood
    ICOM(0x00, "ICOM"),
    JVC(0x68, "JVC KENWOOD"),
    UNKNOWN(0x00, "UNKNOWN VENDOR");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value or id for the vendor
     * @param label display string
     */
    Vendor(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Vendor ID value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Lookup a vendor from the vendor ID.
     * @param id of the vendor
     * @return vendor or UNKNOWN
     */
    public static Vendor fromValue(int id)
    {
        return switch (id)
        {
            case 0x00 -> ICOM;
            case 0x68 -> JVC;
            default -> UNKNOWN;
        };
    }

    /**
     * Pretty print.
     */
    @Override
    public String toString()
    {
        return mLabel;
    }
}
