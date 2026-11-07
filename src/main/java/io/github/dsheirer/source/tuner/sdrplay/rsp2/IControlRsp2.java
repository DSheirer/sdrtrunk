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

package io.github.dsheirer.source.tuner.sdrplay.rsp2;

import io.github.dsheirer.source.tuner.sdrplay.IControlRsp;
import io.github.dsheirer.source.tuner.sdrplay.RspException;

/**
 * Control interface for RSP2 device
 */
public interface IControlRsp2 extends IControlRsp
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
     * Indicates the enabled state of the external reference output.
     * @return true if enabled.
     * @throws RspException if the device is not started (selected).
     */
    boolean isExternalReferenceOutput() throws RspException;

    /**
     * Sets the enabled state of the external reference output.
     * @param enabled true to enable.
     * @throws RspException if the device is not started (selected).
     */
    void setExternalReferenceOutput(boolean enabled) throws RspException;

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
     * Antenna currently being used.
     * @return antenna being used.
     * @throws RspException if the device is not started (selected).
     */
    Rsp2AntennaSelection getAntennaSelection() throws RspException;

    /**
     * Sets the antenna selection
     * @param antenna to use
     * @throws RspException if the device is not started (selected).
     */
    void setAntennaSelection(Rsp2AntennaSelection antenna) throws RspException;
}
