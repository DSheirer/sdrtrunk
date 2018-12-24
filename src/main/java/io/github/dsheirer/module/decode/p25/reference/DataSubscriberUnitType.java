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

package io.github.dsheirer.module.decode.p25.reference;

/**
 * Data Subscriber (ie mobile radio/terminal) Unit Type
 */
public enum DataSubscriberUnitType
{
    DATA_ONLY("DATA ONLY"),
    DATA_OR_VOICE("DATA/VOICE"),
    RESERVED("RESERVED");

    private String mLabel;

    DataSubscriberUnitType(String label)
    {
        mLabel = label;
    }

    public String getLabel()
    {
        return mLabel;
    }

    public static DataSubscriberUnitType fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return DATA_ONLY;
            case 1:
                return DATA_OR_VOICE;
            default:
                return RESERVED;
        }
    }
}