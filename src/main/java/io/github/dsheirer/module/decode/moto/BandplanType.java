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

package io.github.dsheirer.module.decode.moto;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BandplanType
{
    EIGHT_HUNDRED_REBANDED("800 MHz Rebanded"),
    EIGHT_HUNDRED_DOMESTIC("800 MHz Domestic"),
    EIGHT_HUNDRED_DOMESTIC_SPLINTER("800 MHz Splinter"),
    EIGHT_HUNDRED_INTERNATIONAL("800 MHz International"),
    EIGHT_HUNDRED_INTERNATIONAL_SPLINTER("800 MHz Intl Splinter"),
    NINE_HUNDRED("900 MHz"),
    OBT("Other Band Trunking");

    private final String mLabel;

    BandplanType(String label)
    {
        mLabel = label;
    }

    public String getLabel()
    {
        return mLabel;
    }

    @JsonCreator
    public static BandplanType fromValue(String value)
    {
        if(value != null)
        {
            if(value.equals("EIGHT_HUNDRED_REBANTED"))
            {
                return EIGHT_HUNDRED_REBANDED;
            }

            for(BandplanType type : values())
            {
                if(type.name().equals(value) || type.getLabel().equals(value))
                {
                    return type;
                }
            }
        }

        return EIGHT_HUNDRED_REBANDED;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
