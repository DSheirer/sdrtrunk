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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner;

import io.github.dsheirer.source.tuner.sdrplay.api.util.Flag;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_Rsp2TunerParamsT;
import java.lang.foreign.MemorySegment;

/**
 * RSP2 Tuner Parameters structure
 */
public class Rsp2TunerParameters extends TunerParameters
{
    private final MemorySegment mRsp2TunerParams;

    public Rsp2TunerParameters(MemorySegment rxChannelParams, MemorySegment rsp2TunerParams)
    {
        super(rxChannelParams);
        mRsp2TunerParams = rsp2TunerParams;
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getRsp2TunerParams()
    {
        return mRsp2TunerParams;
    }

    /**
     * Indicates if the Bias-T is enabled
     */
    public boolean isBiasT()
    {
        return Flag.evaluate(sdrplay_api_Rsp2TunerParamsT.biasTEnable(getRsp2TunerParams()));
    }

    /**
     * Enables or disables the Bias-T
     */
    public void setBiasT(boolean enable)
    {
        sdrplay_api_Rsp2TunerParamsT.biasTEnable(getRsp2TunerParams(), Flag.of(enable));
    }

    /**
     * Current AM port setting
     */
    public Rsp2AmPort getAmPort()
    {
        return Rsp2AmPort.fromValue(sdrplay_api_Rsp2TunerParamsT.amPortSel(getRsp2TunerParams()));
    }

    /**
     * Sets the AM port to use
     */
    public void setAmPort(Rsp2AmPort amPort)
    {
        sdrplay_api_Rsp2TunerParamsT.amPortSel(getRsp2TunerParams(), amPort.getValue());
    }

    /**
     * Antenna setting
     */
    public Rsp2Antenna getAntenna()
    {
        return Rsp2Antenna.fromValue(sdrplay_api_Rsp2TunerParamsT.antennaSel(getRsp2TunerParams()));
    }

    /**
     * Sets the antenna
     */
    public void setAntenna(Rsp2Antenna rsp2Antenna)
    {
        sdrplay_api_Rsp2TunerParamsT.antennaSel(getRsp2TunerParams(), rsp2Antenna.getValue());
    }

    /**
     * Indicates if the RF notch is enabled
     */
    public boolean isRfNotch()
    {
        return Flag.evaluate(sdrplay_api_Rsp2TunerParamsT.rfNotchEnable(getRsp2TunerParams()));
    }

    /**
     * Enables or disables the RF notch
     */
    public void setRfNotch(boolean enable)
    {
        sdrplay_api_Rsp2TunerParamsT.rfNotchEnable(getRsp2TunerParams(), Flag.of(enable));
    }
}
