/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Enumeration of decoder types
 */
public enum DecoderType
{
    //Primary Decoders
    AM("AM", "AM"),
    LTR_STANDARD("LTR-Standard", "LTR"),
    LTR_NET("LTR-Net", "LTR-Net"),
    MPT1327("MPT1327", "MPT1327"),
    NBFM("NBFM", "NBFM"),
    PASSPORT("Passport", "Passport"),
    P25_PHASE1("P25 Phase I", "P25-1"),

    //Auxiliary Decoders
    FLEETSYNC2("Fleetsync II", "Fleetsync2"),
    LJ_1200("LJ1200 173.075", "LJ1200"),
    MDC1200("MDC1200", "MDC1200"),
    TAIT_1200("Tait 1200", "Tait 1200");

    private String mDisplayString;
    private String mShortDisplayString;

    DecoderType(String displayString, String shortDisplayString)
    {
        mDisplayString = displayString;
        mShortDisplayString = shortDisplayString;
    }

    /**
     * Primary decoders
     */
    public static EnumSet<DecoderType> getPrimaryDecoders()
    {
        return EnumSet.of(DecoderType.AM,
            DecoderType.LTR_NET,
            DecoderType.LTR_STANDARD,
            DecoderType.MPT1327,
            DecoderType.NBFM,
            DecoderType.P25_PHASE1,
            DecoderType.PASSPORT);
    }

    /**
     * Available auxiliary decoders.
     */
    public static ArrayList<DecoderType> getAuxDecoders()
    {
        ArrayList<DecoderType> decoders = new ArrayList<DecoderType>();

        decoders.add(DecoderType.FLEETSYNC2);
        decoders.add(DecoderType.LJ_1200);
        decoders.add(DecoderType.MDC1200);
        decoders.add(DecoderType.TAIT_1200);

        return decoders;
    }

    public String getDisplayString()
    {
        return mDisplayString;
    }

    public String getShortDisplayString()
    {
        return mShortDisplayString;
    }

    @Override
    public String toString()
    {
        return mDisplayString;
    }
}
