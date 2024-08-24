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

import java.lang.foreign.MemorySegment;

/**
 * RSPduo abstract Tuner Parameters structure
 */
public abstract class RspDuoTunerParameters extends TunerParameters
{
    private MemorySegment mRspDuoTunerParams;

    /**
     * Constructs an instance from the foreign memory segment
     * @param rxChannelParams of foreign memory structure
     * @param rspDuoTunerParams of foreign memory structure
     */
    RspDuoTunerParameters(MemorySegment rxChannelParams, MemorySegment rspDuoTunerParams)
    {
        super(rxChannelParams);
        mRspDuoTunerParams = rspDuoTunerParams;
    }

    /**
     * Foreign memory segment for this structure
     */
    MemorySegment getRspDuoTunerParams()
    {
        return mRspDuoTunerParams;
    }

    /**
     * Indicates if the Bias-T is enabled
     */
    public abstract boolean isBiasT();

    /**
     * Enables or disables the Bias-T
     */
    public abstract void setBiasT(boolean enable);

    /**
     * Tuner 1 AM port setting
     */
    public abstract RspDuoAmPort getTuner1AmPort();

    /**
     * Set Tuner 1 AM port setting
     */
    public abstract void setTuner1AmPort(RspDuoAmPort amPort);

    /**
     * Indicates if Tuner 1 AM notch is enabled
     */
    public abstract boolean isTuner1AmNotch();

    /**
     * Enables or disables the Tuner 1 AM notch
     */
    public abstract void setTuner1AmNotch(boolean enable);

    /**
     * Indicates if the RF notch is enabled
     */
    public abstract boolean isRfNotch();

    /**
     * Enables or disables the RF notch
     */
    public abstract void setRfNotch(boolean enable);

    /**
     * Indicates if the RF DAB notch is enabled
     */
    public abstract boolean isRfDabNotch();

    /**
     * Enables or disables the RF DAB notch
     */
    public abstract void setRfDabNotch(boolean enable);
}
