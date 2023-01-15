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

package io.github.dsheirer.source.tuner.sdrplay.api.device;

import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRplay;
import io.github.dsheirer.source.tuner.sdrplay.api.UpdateReason;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.ControlParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.device.RspDuoDeviceParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDuoTunerParameters;

/**
 * RSPduo Tuner 2
 */
public class RspDuoTuner2 extends RspDuoTuner
{
    /**
     * Constructs an instance
     *
     * @param device parent for this tuner
     * @param sdrplay api
     * @param deviceParameters for this device
     * @param tunerParameters for this tuner
     * @param controlParameters for this device
     */
    public RspDuoTuner2(Device device, SDRplay sdrplay, RspDuoDeviceParameters deviceParameters,
                        RspDuoTunerParameters tunerParameters, ControlParameters controlParameters)
    {
        super(device, sdrplay, TunerSelect.TUNER_2, deviceParameters, tunerParameters, controlParameters);
    }

    /**
     * Indicates if the Bias-T is enabled
     */
    public boolean isBiasT()
    {
        return getTunerParameters().isBiasT();
    }

    /**
     * Enables or disables the Bias-T
     * @param enable value
     * @throws SDRPlayException if there is an error
     */
    public void setBiasT(boolean enable) throws SDRPlayException
    {
        getTunerParameters().setBiasT(enable);
        update(UpdateReason.RSP_DUO_BIAS_T_CONTROL);
    }
}
