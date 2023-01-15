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
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDuoAmPort;

/**
 * Control interface for RSPduo Tuner 1 device
 */
public interface IControlRspDuoTuner1 extends IControlRspDuo
{
    /**
     * Indicates if the AM notch is enabled (AM broadcast band filter)
     * @return true if enabled.
     * @throws exception if the device is not started (selected).
     */
    boolean isAmNotch() throws SDRPlayException;

    /**
     * Sets the enabled state of the AM notch filter.
     * @param enabled true to enable
     * @throws exception if the device is not started (selected).
     */
    void setAmNotch(boolean enabled) throws SDRPlayException;

    /**
     * Current port selection setting for the AM port.
     * @return selected AM port
     * @throws exception if the device is not started (selected).
     */
    RspDuoAmPort getAmPort() throws SDRPlayException;

    /**
     * Sets the AM port.
     * @param amPort to use.
     * @throws exception if the device is not started (selected).
     */
    void setAmPort(RspDuoAmPort amPort) throws SDRPlayException;
}
