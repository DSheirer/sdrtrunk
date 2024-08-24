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

import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDxAntenna;
import io.github.dsheirer.source.tuner.sdrplay.api.util.Flag;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DevParamsT;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_RspDxParamsT;
import java.lang.foreign.MemorySegment;

/**
 * RSP-DX Device Parameters structure
 */
public class RspDxDeviceParameters extends DeviceParameters
{
    private final MemorySegment mRspDxParams;

    /**
     * Constructs an instance from the foreign memory segment.
     * @param devParams an allocated sdrplay_api_DevParamsT structure
     */
    public RspDxDeviceParameters(MemorySegment devParams)
    {
        super(devParams);
        mRspDxParams = sdrplay_api_DevParamsT.rspDxParams(devParams);
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getRspDxParams()
    {
        return mRspDxParams;
    }

    /**
     * Indicates if High Dynamic Range (HDR) is enabled
     */
    public boolean isHdr()
    {
        return Flag.evaluate(sdrplay_api_RspDxParamsT.hdrEnable(getRspDxParams()));
    }

    /**
     * Enables or disables High Dynamic Range (HDR)
     */
    public void setHdr(boolean enable)
    {
        sdrplay_api_RspDxParamsT.hdrEnable(getRspDxParams(), Flag.of(enable));
    }

    /**
     * Indicates if Bias-T is enabled
     */
    public boolean isBiasT()
    {
        return Flag.evaluate(sdrplay_api_RspDxParamsT.biasTEnable(getRspDxParams()));
    }

    /**
     * Enables or disables Bias-T
     */
    public void setBiasT(boolean enable)
    {
        sdrplay_api_RspDxParamsT.biasTEnable(getRspDxParams(), Flag.of(enable));
    }

    /**
     * RSPdx Antenna Setting
     */
    public RspDxAntenna getRspDxAntenna()
    {
        return RspDxAntenna.fromValue(sdrplay_api_RspDxParamsT.antennaSel(getRspDxParams()));
    }

    /**
     * Selects the RSPdx Antenna
     */
    public void setRspDxAntenna(RspDxAntenna rspDxAntenna)
    {
        sdrplay_api_RspDxParamsT.antennaSel(getRspDxParams(), rspDxAntenna.getValue());
    }

    /**
     * Indicates if RF notch is enabled
     */
    public boolean isRfNotch()
    {
        return Flag.evaluate(sdrplay_api_RspDxParamsT.rfNotchEnable(getRspDxParams()));
    }

    /**
     * Enables or disables RF notch
     */
    public void setRfNotch(boolean enable)
    {
        sdrplay_api_RspDxParamsT.rfNotchEnable(getRspDxParams(), Flag.of(enable));
    }

    /**
     * Indicates if the RF DAB notch is enabled
     */
    public boolean isRfDabNotch()
    {
        return Flag.evaluate(sdrplay_api_RspDxParamsT.rfDabNotchEnable(getRspDxParams()));
    }

    /**
     * Enables or disables the RF DAB notch
     */
    public void setRfDabNotch(boolean enable)
    {
        sdrplay_api_RspDxParamsT.rfDabNotchEnable(getRspDxParams(), Flag.of(enable));
    }
}
