/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay.rspDx;

import io.github.dsheirer.source.tuner.sdrplay.IControlRsp;
import io.github.dsheirer.source.tuner.sdrplay.RspException;

/**
 * Control interface for RSPdx device
 */
public interface IControlRspDx extends IControlRsp
{
    /**
     * Indicates if the Bias-T power is enabled.
     * @return true if enabled.
     * @throws RspException if the device is no started/selected
     */
    boolean isBiasT() throws RspException;

    /**
     * Sets the enabled state of the Bias-T power.
     * @param enabled true to power on the Bias-T
     * @throws RspException if the device is not started (selected).
     */
    void setBiasT(boolean enabled) throws RspException;

    /**
     * Indicates the enabled state of high dynamic range mode
     * @return true if enabled.
     * @throws RspException if the device is not started (selected).
     */
    boolean isHighDynamicRange() throws RspException;

    /**
     * Sets the enabled state of the high dynamic range mode
     * @param enabled true to enable.
     * @throws RspException if the device is not started (selected).
     */
    void setHighDynamicRange(boolean enabled) throws RspException;

    /**
     * Indicates if the RF notch is enabled (FM broadcast band filter).
     * @return true if enabled.
     * @throws RspException if the device is not started (selected).
     */
    boolean isRfNotch() throws RspException;

    /**
     * Sets the enabled state of the RF notch
     * @param enabled true to filter the FM broadcast band frequency range.
     * @throws RspException if the device is not started (selected).
     */
    void setRfNotch(boolean enabled) throws RspException;

    /**
     * Indicates if the RF DAB notch is enabled (DAB broadcast band filter).
     * @return true if enabled.
     * @throws RspException if the device is not started (selected).
     */
    boolean isRfDabNotch() throws RspException;

    /**
     * Sets the enabled state of the RF DAB notch.
     * @param enabled true to filter the DAB broadcast band frequency range.
     * @throws RspException if the device is not started (selected).
     */
    void setRfDabNotch(boolean enabled) throws RspException;

    /**
     * Antenna selection
     * @return currently selected antenna
     * @throws RspException if the device is not started (selected)
     */
    RspDxAntennaSelection getAntenna() throws RspException;

    /**
     * Sets the antenna selection
     * @param antenna to select
     * @throws RspException if the device is not started (selected)
     */
    void setAntenna(RspDxAntennaSelection antenna) throws RspException;

    /**
     * Sets High Dynamic Range mode bandwidth
     * @param bandwidth to apply
     * @throws RspException if the device is not started (selected)
     */
    void setHdrModeBandwidth(HdrModeBandwidth bandwidth) throws RspException;

    /**
     * High Dynamic Range mode bandwidth
     * @return bandwidth
     * @throws RspException if the device is not started (selected)
     */
    HdrModeBandwidth getHdrModeBandwidth() throws RspException;
}
