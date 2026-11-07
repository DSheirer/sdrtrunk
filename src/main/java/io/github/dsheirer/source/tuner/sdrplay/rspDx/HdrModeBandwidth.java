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

package io.github.dsheirer.source.tuner.sdrplay.rspDx;

import io.github.dsheirer.sdrplay.RspDxHdrBandwidth;
import java.util.EnumSet;

/**
 * RSPdx High Dynamic Range (HDR) mode bandwidth.  Entry names are persisted in tuner configurations.
 */
public enum HdrModeBandwidth
{
    BANDWIDTH_0_200(RspDxHdrBandwidth.KHZ_200, ".2 MHz"),
    BANDWIDTH_0_500(RspDxHdrBandwidth.KHZ_500, ".5 MHz"),
    BANDWIDTH_1_200(RspDxHdrBandwidth.KHZ_1200, "1.2 MHz"),
    BANDWIDTH_1_700(RspDxHdrBandwidth.KHZ_1700, "1.7 MHz"),
    UNKNOWN(null, "UNKNOWN");

    /**
     * Valid (selectable) bandwidth entries
     */
    public static final EnumSet<HdrModeBandwidth> VALID = EnumSet.range(BANDWIDTH_0_200, BANDWIDTH_1_700);

    private final RspDxHdrBandwidth mBandwidth;
    private final String mDescription;

    HdrModeBandwidth(RspDxHdrBandwidth bandwidth, String description)
    {
        mBandwidth = bandwidth;
        mDescription = description;
    }

    /**
     * SDRplay API HDR bandwidth for this entry, or null for UNKNOWN.
     */
    public RspDxHdrBandwidth getBandwidth()
    {
        return mBandwidth;
    }

    /**
     * Lookup the entry that matches the SDRplay API HDR bandwidth
     */
    public static HdrModeBandwidth fromBandwidth(RspDxHdrBandwidth bandwidth)
    {
        for(HdrModeBandwidth entry: VALID)
        {
            if(entry.getBandwidth() == bandwidth)
            {
                return entry;
            }
        }

        return UNKNOWN;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
