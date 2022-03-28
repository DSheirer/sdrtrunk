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

package io.github.dsheirer.module.decode.dmr.message.type;

/**
 * DMR Tier III service functions
 */
public enum ServiceFunction
{
    ALL("ALL"),
    REGISTRATION_AND_CHANNEL_GRANTS("REGISTRATION/CHANNEL GRANTS ONLY"),
    REGISTRATION_NO_CHANNEL_GRANTS("REGISTRATION/NO CHANNEL GRANTS"),
    REGISTRATION_ONLY("REGISTRATION ONLY"),
    UNKNOWN("UNKNOWN");

    private String mLabel;

    ServiceFunction(String label)
    {
        mLabel = label;
    }

    /**
     * Utility method to lookup the model type from the integer value
     * @param value 0-3
     * @return entry or UNKNOWN
     */
    public static ServiceFunction fromValue(int value)
    {
        if(0 <= value && value <= 3)
        {
            return ServiceFunction.values()[value];
        }

        return UNKNOWN;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
