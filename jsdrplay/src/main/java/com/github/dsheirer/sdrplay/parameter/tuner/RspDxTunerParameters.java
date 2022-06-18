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

package com.github.dsheirer.sdrplay.parameter.tuner;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_RspDxTunerParamsT;

import java.lang.foreign.MemorySegment;

/**
 * RSP-Dx Tuner Parameters structure
 */
public class RspDxTunerParameters extends TunerParameters
{
    private MemorySegment mRspDxMemorySegment;

    /**
     * Constructs an instance from the foreign memory segment.
     */
    public RspDxTunerParameters(MemorySegment tunerParametersMemorySegment, MemorySegment rspDxMemorySegment)
    {
        super(tunerParametersMemorySegment);
        mRspDxMemorySegment = rspDxMemorySegment;
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getRspDxMemorySegment()
    {
        return mRspDxMemorySegment;
    }

    /**
     * Current bandwidth setting for High Dynamic Range (HDR) mode
     */
    public HdrModeBandwidth getHdrModeBandwidth()
    {
        return HdrModeBandwidth.fromValue(sdrplay_api_RspDxTunerParamsT.hdrBw$get(getRspDxMemorySegment()));
    }

    /**
     * Sets bandwidth for High Dynamic Range (HDR) mode
     */
    public void setHdrModeBandwidth(HdrModeBandwidth bandwidth)
    {
        sdrplay_api_RspDxTunerParamsT.hdrBw$set(getRspDxMemorySegment(), bandwidth.getValue());
    }
}
