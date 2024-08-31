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
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_RspDuoParamsT;
import java.lang.foreign.MemorySegment;

/**
 * RSPduo Device Parameters structure
 */
public class RspDuoDeviceParameters extends DeviceParameters
{
    private final MemorySegment mRspDuoParams;

    /**
     * Constructs an instance
     * @param devParams for an allocated sdrplay_api_DevParamsT structure
     */
    public RspDuoDeviceParameters(MemorySegment devParams)
    {
        super(devParams);
        mRspDuoParams = sdrplay_api_DevParamsT.rspDuoParams(devParams);
    }

    /**
     * Foreign memory segment representing this structure
     */
    private MemorySegment getRspDuoParams()
    {
        return mRspDuoParams;
    }

    /**
     * Indicates if the external reference output is enabled
     */
    public boolean isExternalReferenceOutput()
    {
        return Flag.evaluate(sdrplay_api_RspDuoParamsT.extRefOutputEn(getRspDuoParams()));
    }

    /**
     * Enables or disables the external reference output
     */
    public void setExternalReferenceOutput(boolean enable)
    {
        sdrplay_api_RspDuoParamsT.extRefOutputEn(getRspDuoParams(), Flag.of(enable));
    }
}
