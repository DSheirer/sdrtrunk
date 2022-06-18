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

package com.github.dsheirer.sdrplay.parameter.device;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_DevParamsT;
import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_RspDuoParamsT;
import com.github.dsheirer.sdrplay.util.Flag;

import java.lang.foreign.MemorySegment;

/**
 * RSPduo Device Parameters structure
 */
public class RspDuoDeviceParameters extends DeviceParameters
{
    private MemorySegment mRspDuoMemorySegment;

    /**
     * Constructs an instance
     * @param memorySegment for this structure
     */
    public RspDuoDeviceParameters(MemorySegment memorySegment)
    {
        super(memorySegment);
        mRspDuoMemorySegment = sdrplay_api_DevParamsT.rspDuoParams$slice(memorySegment);
    }

    /**
     * Foreign memory segment representing this structure
     */
    private MemorySegment getRspDuoMemorySegment()
    {
        return mRspDuoMemorySegment;
    }

    /**
     * Indicates if the external reference output is enabled
     */
    public boolean isExternalReferenceOutput()
    {
        return Flag.evaluate(sdrplay_api_RspDuoParamsT.extRefOutputEn$get(getRspDuoMemorySegment()));
    }

    /**
     * Enables or disables the external reference output
     */
    public void setExternalReferenceOutput(boolean enable)
    {
        sdrplay_api_RspDuoParamsT.extRefOutputEn$set(getRspDuoMemorySegment(), Flag.of(enable));
    }
}
