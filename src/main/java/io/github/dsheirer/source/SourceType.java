/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source;

/**
 * Source Configuration Types enumeration.
 */
public enum SourceType
{
    NONE("No Source"),
    MIXER("Sound Card"),
    TUNER("Tuner"),
    TUNER_MULTIPLE_FREQUENCIES("Tuner - Multiple Frequencies"),
    RECORDING("IQ Recording");

    private String mDisplayString;

    SourceType(String displayString)
    {
        mDisplayString = displayString;
    }

    public static SourceType[] getTypes()
    {
        SourceType[] types = new SourceType[3];
        types[0] = SourceType.TUNER;
        types[1] = SourceType.TUNER_MULTIPLE_FREQUENCIES;
        types[2] = SourceType.MIXER;

        return types;
    }

    @Override
    public String toString()
    {
        return mDisplayString;
    }
}
