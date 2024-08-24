/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.device;

import io.github.dsheirer.source.tuner.sdrplay.api.util.Flag;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DevParamsT;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_Rsp1aParamsT;
import java.lang.foreign.MemorySegment;

/**
 * RSP-1A Device Parameters structure
 */
public class Rsp1aDeviceParameters extends DeviceParameters
{
    private final MemorySegment mRsp1aParams;

    /**
     * Constructs an instance
     * @param devParams for an allocated sdrplay_api_DevParamsT structure
     */
    public Rsp1aDeviceParameters(MemorySegment devParams)
    {
        super(devParams);
        mRsp1aParams = sdrplay_api_DevParamsT.rsp1aParams(devParams);
    }

    /**
     * Foreign memory segment representing this structure
     */
    private MemorySegment getRsp1aParams()
    {
        return mRsp1aParams;
    }

    /**
     * Indicates if RF notch is enabled.
     */
    public boolean isRFNotch()
    {
        return Flag.evaluate(sdrplay_api_Rsp1aParamsT.rfNotchEnable(getRsp1aParams()));
    }

    /**
     * Enables or disables the RF notch.
     */
    public void setRFNotch(boolean enable)
    {
        sdrplay_api_Rsp1aParamsT.rfNotchEnable(getRsp1aParams(), Flag.of(enable));
    }

    /**
     * Indicates if DAB RF notch is enabled
     */
    public boolean isRfDabNotch()
    {
        return Flag.evaluate(sdrplay_api_Rsp1aParamsT.rfDabNotchEnable(getRsp1aParams()));
    }

    /**
     * Enables or disables the DAB RF notch
     */
    public void setRfDabNotch(boolean enable)
    {
        sdrplay_api_Rsp1aParamsT.rfDabNotchEnable(getRsp1aParams(), Flag.of(enable));
    }
}
