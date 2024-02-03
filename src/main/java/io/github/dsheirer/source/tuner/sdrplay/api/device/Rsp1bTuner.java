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

package io.github.dsheirer.source.tuner.sdrplay.api.device;

import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRplay;
import io.github.dsheirer.source.tuner.sdrplay.api.UpdateReason;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.ControlParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.device.Rsp1aDeviceParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Rsp1aTunerParameters;

/**
 * RSP1B Tuner
 *
 * Note: the RSP1B uses the same device and tuner parameters structures as the RSP1A
 */
public class Rsp1bTuner extends RspTuner<Rsp1aDeviceParameters, Rsp1aTunerParameters>
{
    /**
     * Constructs an instance
     * @param device parent for this tuner
     * @param sdrplay api
     * @param tunerSelect to specify which tuner
     * @param deviceParameters for this device
     * @param tunerParameters for this tuner
     * @param controlParameters for this device
     */
    public Rsp1bTuner(Device device, SDRplay sdrplay, TunerSelect tunerSelect, Rsp1aDeviceParameters deviceParameters,
                      Rsp1aTunerParameters tunerParameters, ControlParameters controlParameters)
    {
        super(device, sdrplay, tunerSelect, deviceParameters, tunerParameters, controlParameters);
    }

    /**
     * Indicates if the RF notch is enabled
     */
    public boolean isRfNotch()
    {
        return getDeviceParameters().isRFNotch();
    }

    /**
     * Enables or disables the RF notch
     * @param enable setting
     * @throws SDRPlayException if there is an error
     */
    public void setRfNotch(boolean enable) throws SDRPlayException
    {
        getDeviceParameters().setRFNotch(enable);
        update(UpdateReason.RSP1A_RF_NOTCH_CONTROL);
    }

    /**
     * Indicates if the RF DAB notch is enabled
     */
    public boolean isRfDabNotch()
    {
        return getDeviceParameters().isRfDabNotch();
    }

    /**
     * Enables or disables the RF DAB notch
     * @param enable value
     * @throws SDRPlayException if there is an error
     */
    public void setRfDabNotch(boolean enable) throws SDRPlayException
    {
        getDeviceParameters().setRfDabNotch(enable);
        update(UpdateReason.RSP1A_RF_DAB_NOTCH_CONTROL);
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
        update(UpdateReason.RSP1A_BIAS_T_CONTROL);
    }
}
