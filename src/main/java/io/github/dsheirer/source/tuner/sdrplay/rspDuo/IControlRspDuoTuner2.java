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

import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;

/**
 * Control interface for RSPduo Tuner 2 device
 */
public interface IControlRspDuoTuner2 extends IControlRspDuo
{
    /**
     * Indicates if the Bias-T power is enabled.
     * @return true if enabled.
     * @throws SDRPlayException if the device is no started/selected
     */
    boolean isBiasT() throws SDRPlayException;

    /**
     * Sets the enabled state of the Bias-T power.
     * @param enabled true to power on the Bias-T
     * @throws SDRPlayException if the device is no started/selected
     */
    void setBiasT(boolean enabled) throws SDRPlayException;

    /**
     * Indicates if this tuner is configured for dual-tuner slave mode.
     * @return true if slave mode.
     */
    boolean isSlaveMode();
}
