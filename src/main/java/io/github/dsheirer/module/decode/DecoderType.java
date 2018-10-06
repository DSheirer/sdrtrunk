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
    AM("AM", "AM", 0),
    LTR_STANDARD("LTR-Standard", "LTR", 300),
    LTR_NET("LTR-Net", "LTR-Net", 300),
    MPT1327("MPT1327", "MPT1327", 1200),
    NBFM("NBFM", "NBFM", 0),
    PASSPORT("Passport", "Passport", 300),
    P25_PHASE1("P25 Phase I", "P25-1", 9600),

    //Auxiliary Decoders
    FLEETSYNC2("Fleetsync II", "Fleetsync2", 1200),
    LJ_1200("LJ1200 173.075", "LJ1200", 1200),
    MDC1200("MDC1200", "MDC1200", 1200),
    TAIT_1200("Tait 1200", "Tait 1200", 1200);

    private String mDisplayString;
    private String mShortDisplayString;
    private int mBitRate;

    DecoderType(String displayString, String shortDisplayString, int bitRate)
    {
        mDisplayString = displayString;
        mShortDisplayString = shortDisplayString;
        mBitRate = bitRate;
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

    public int getBitRate()
    {
        return mBitRate;
    }
}
