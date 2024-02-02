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
import io.github.dsheirer.source.tuner.sdrplay.api.v3_14.sdrplay_api_RspDuoTunerParamsT;
import java.lang.foreign.MemorySegment;

/**
 * RSPduo Tuner Parameters structure for API Versions 3.14+.
 *
 * Note: version 3.14 is different from version 3.07-3.12 because it adds the RspDuo_ResetSlaveFlagsT structure and
 * methods, however these are not used/implemented in this class or in the parent abstract class.
 */
public class RspDuoTunerParametersV3_14 extends RspDuoTunerParameters
{
    /**
     * Constructs an instance from the foreign memory segment
     * @param tunerParametersMemorySegment of foreign memory structure
     * @param rspDuoMemorySegment of foreign memory structure
     */
    RspDuoTunerParametersV3_14(MemorySegment tunerParametersMemorySegment, MemorySegment rspDuoMemorySegment)
    {
        super(tunerParametersMemorySegment, rspDuoMemorySegment);
    }

    /**
     * Indicates if the Bias-T is enabled
     */
    public boolean isBiasT()
    {
        return Flag.evaluate(sdrplay_api_RspDuoTunerParamsT.biasTEnable$get(getRspDuoMemorySegment()));
    }

    /**
     * Enables or disables the Bias-T
     */
    public void setBiasT(boolean enable)
    {
        sdrplay_api_RspDuoTunerParamsT.biasTEnable$set(getRspDuoMemorySegment(), Flag.of(enable));
    }

    /**
     * Tuner 1 AM port setting
     */
    public RspDuoAmPort getTuner1AmPort()
    {
        return RspDuoAmPort.fromValue(sdrplay_api_RspDuoTunerParamsT.tuner1AmPortSel$get(getRspDuoMemorySegment()));
    }

    /**
     * Set Tuner 1 AM port setting
     */
    public void setTuner1AmPort(RspDuoAmPort amPort)
    {
        sdrplay_api_RspDuoTunerParamsT.tuner1AmPortSel$set(getRspDuoMemorySegment(), amPort.getValue());
    }

    /**
     * Indicates if Tuner 1 AM notch is enabled
     */
    public boolean isTuner1AmNotch()
    {
        return Flag.evaluate(sdrplay_api_RspDuoTunerParamsT.tuner1AmNotchEnable$get(getRspDuoMemorySegment()));
    }

    /**
     * Enables or disables the Tuner 1 AM notch
     */
    public void setTuner1AmNotch(boolean enable)
    {
        sdrplay_api_RspDuoTunerParamsT.tuner1AmNotchEnable$set(getRspDuoMemorySegment(), Flag.of(enable));
    }

    /**
     * Indicates if the RF notch is enabled
     */
    public boolean isRfNotch()
    {
        return Flag.evaluate(sdrplay_api_RspDuoTunerParamsT.rfNotchEnable$get(getRspDuoMemorySegment()));
    }

    /**
     * Enables or disables the RF notch
     */
    public void setRfNotch(boolean enable)
    {
        sdrplay_api_RspDuoTunerParamsT.rfNotchEnable$set(getRspDuoMemorySegment(), Flag.of(enable));
    }

    /**
     * Indicates if the RF DAB notch is enabled
     */
    public boolean isRfDabNotch()
    {
        return Flag.evaluate(sdrplay_api_RspDuoTunerParamsT.rfDabNotchEnable$get(getRspDuoMemorySegment()));
    }

    /**
     * Enables or disables the RF DAB notch
     */
    public void setRfDabNotch(boolean enable)
    {
        sdrplay_api_RspDuoTunerParamsT.rfDabNotchEnable$set(getRspDuoMemorySegment(), Flag.of(enable));
    }
}
