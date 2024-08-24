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
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_Rsp2ParamsT;
import java.lang.foreign.MemorySegment;

/**
 * RSP-2 Device Parameters structure
 */
public class Rsp2DeviceParameters extends DeviceParameters
{
    private final MemorySegment mRsp2Params;

    /**
     * Constructs an instance
     * @param devParams for an allocated sdrplay_api_DevParamsT structure
     */
    public Rsp2DeviceParameters(MemorySegment devParams)
    {
        super(devParams);
        mRsp2Params = sdrplay_api_DevParamsT.rsp2Params(devParams);
    }

    /**
     * Foreign memory segment representing this structure
     */
    private MemorySegment getRsp2Params()
    {
        return mRsp2Params;
    }

    /**
     * Indicates if the external reference output is enabled
     */
    public boolean isExternalReferenceOutput()
    {
        return Flag.evaluate(sdrplay_api_Rsp2ParamsT.extRefOutputEn(getRsp2Params()));
    }

    /**
     * Enables or disables the external reference output
     */
    public void setExternalReferenceOutput(boolean enable)
    {
        sdrplay_api_Rsp2ParamsT.extRefOutputEn(getRsp2Params(), Flag.of(enable));
    }
}
