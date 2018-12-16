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
package io.github.dsheirer.module.decode;

import io.github.dsheirer.protocol.Protocol;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Enumeration of decoder types
 */
public enum DecoderType
{
    //Primary Decoders
    AM("AM", "AM", Protocol.UNKNOWN),
    LTR_STANDARD("LTR-Standard", "LTR", Protocol.LTR_STANDARD),
    LTR_NET("LTR-Net", "LTR-Net", Protocol.LTR_NET),
    MPT1327("MPT1327", "MPT1327", Protocol.MPT1327),
    NBFM("NBFM", "NBFM", Protocol.UNKNOWN),
    PASSPORT("Passport", "Passport", Protocol.PASSPORT),
    P25_PHASE1("P25 Phase I", "P25-1", Protocol.APCO25),

    //Auxiliary Decoders
    FLEETSYNC2("Fleetsync II", "Fleetsync2", Protocol.FLEETSYNC),
    LJ_1200("LJ1200 173.075", "LJ1200", Protocol.LOJACK),
    MDC1200("MDC1200", "MDC1200", Protocol.MDC1200),
    TAIT_1200("Tait 1200", "Tait 1200", Protocol.TAIT1200);

    private String mDisplayString;
    private String mShortDisplayString;
    private Protocol mProtocol;

    DecoderType(String displayString, String shortDisplayString, Protocol protocol)
    {
        mDisplayString = displayString;
        mShortDisplayString = shortDisplayString;
        mProtocol = protocol;
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

    public Protocol getProtocol()
    {
        return mProtocol;
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
