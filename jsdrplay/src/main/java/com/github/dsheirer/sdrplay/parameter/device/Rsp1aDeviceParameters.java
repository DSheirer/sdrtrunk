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
import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_Rsp1aParamsT;
import com.github.dsheirer.sdrplay.util.Flag;

import java.lang.foreign.MemorySegment;

/**
 * RSP-1A Device Parameters structure
 */
public class Rsp1aDeviceParameters extends DeviceParameters
{
    private MemorySegment mRsp1AMemorySegment;

    /**
     * Constructs an instance
     * @param memorySegment for this structure
     */
    public Rsp1aDeviceParameters(MemorySegment memorySegment)
    {
        super(memorySegment);
        mRsp1AMemorySegment = sdrplay_api_DevParamsT.rsp1aParams$slice(memorySegment);
    }

    /**
     * Foreign memory segment representing this structure
     */
    private MemorySegment getRsp1AMemorySegment()
    {
        return mRsp1AMemorySegment;
    }

    /**
     * Indicates if RF notch is enabled.
     */
    public boolean isRFNotch()
    {
        return Flag.evaluate(sdrplay_api_Rsp1aParamsT.rfNotchEnable$get(getRsp1AMemorySegment()));
    }

    /**
     * Enables or disables the RF notch.
     */
    public void setRFNotch(boolean enable)
    {
        sdrplay_api_Rsp1aParamsT.rfNotchEnable$set(getRsp1AMemorySegment(), Flag.of(enable));
    }

    /**
     * Indicates if DAB RF notch is enabled
     */
    public boolean isRfDabNotch()
    {
        return Flag.evaluate(sdrplay_api_Rsp1aParamsT.rfDabNotchEnable$get(getRsp1AMemorySegment()));
    }

    /**
     * Enables or disables the DAB RF notch
     */
    public void setRfDabNotch(boolean enable)
    {
        sdrplay_api_Rsp1aParamsT.rfDabNotchEnable$set(getRsp1AMemorySegment(), Flag.of(enable));
    }
}
