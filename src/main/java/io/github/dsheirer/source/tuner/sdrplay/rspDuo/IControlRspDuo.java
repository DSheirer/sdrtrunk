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

package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import io.github.dsheirer.source.tuner.sdrplay.IControlRsp;
import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import java.util.EnumSet;

/**
 * Control interface for RSPduo device (tuner 1 or 2)
 */
public interface IControlRspDuo extends IControlRsp
{
    /**
     * Set of valid sample rates to use with the RSPduo in the current operating mode
     * @return supported sample rates or an empty set if you cannot set the sample rate on this tuner.
     */
    EnumSet<RspSampleRate> getSupportedSampleRates();

    /**
     * Indicates if the RF DAB notch is enabled (DAB broadcast band filter).
     * @return true if enabled.
     * @throws exception if the device is not started (selected).
     */
    boolean isRfDabNotch() throws SDRPlayException;

    /**
     * Sets the enabled state of the RF DAB notch.
     * @param enabled true to filter the DAB broadcast band frequency range.
     * @throws exception if the device is not started (selected).
     */
    void setRfDabNotch(boolean enabled) throws SDRPlayException;

    /**
     * Indicates if the RF notch is enabled (FM broadcast band filter).
     * @return true if enabled.
     * @throws exception if the device is not started (selected).
     */
    boolean isRfNotch() throws SDRPlayException;

    /**
     * Sets the enabled state of the RF notch
     * @param enabled true to filter the FM broadcast band frequency range.
     * @throws exception if the device is not started (selected).
     */
    void setRfNotch(boolean enabled) throws SDRPlayException;

    /**
     * Indicates the enabled state of the external reference output.
     * @return true if enabled.
     * @throws exception if the device is not started (selected).
     */
    boolean isExternalReferenceOutput() throws SDRPlayException;

    /**
     * Sets the enabled state of the external reference output.
     * @param enabled true to enable.
     * @throws exception if the device is not started (selected).
     */
    void setExternalReferenceOutput(boolean enabled) throws SDRPlayException;
}
