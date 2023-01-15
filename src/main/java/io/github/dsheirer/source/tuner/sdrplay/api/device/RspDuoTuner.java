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
 * RSPduo Tuner
 */
public abstract class RspDuoTuner extends RspTuner<RspDuoDeviceParameters, RspDuoTunerParameters>
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
    public RspDuoTuner(Device device, SDRplay sdrplay, TunerSelect tunerSelect, RspDuoDeviceParameters deviceParameters,
                       RspDuoTunerParameters tunerParameters, ControlParameters controlParameters)
    {
        super(device, sdrplay, tunerSelect, deviceParameters, tunerParameters, controlParameters);
    }

    /**
     * Indicates if the RF notch is enabled
     */
    public boolean isRFNotch()
    {
        return getTunerParameters().isRfNotch();
    }

    /**
     * Enables or disables the RF notch
     * @param enable setting
     * @throws SDRPlayException if there is an error
     */
    public void setRfNotch(boolean enable) throws SDRPlayException
    {
        getTunerParameters().setRfNotch(enable);
        update(UpdateReason.RSP_DUO_RF_NOTCH_CONTROL);
    }

    /**
     * Indicates if the RF DAB notch is enabled
     */
    public boolean isRfDabNotch()
    {
        return getTunerParameters().isRfDabNotch();
    }

    /**
     * Enables or disables the RF DAB notch
     * @param enable value
     * @throws SDRPlayException if there is an error
     */
    public void setRfDabNotch(boolean enable) throws SDRPlayException
    {
        getTunerParameters().setRfDabNotch(enable);
        update(UpdateReason.RSP_DUO_RF_DAB_NOTCH_CONTROL);
    }

    /**
     * Indicates if the external reference output is enabled.
     */
    public boolean isExternalReferenceOutput()
    {
        return getDeviceParameters().isExternalReferenceOutput();
    }

    /**
     * Enables or disables the external reference output
     * @param enable value
     * @throws SDRPlayException if there is an error
     */
    public void setExternalReferenceOutput(boolean enable) throws SDRPlayException
    {
        getDeviceParameters().setExternalReferenceOutput(enable);
        update(UpdateReason.RSP_DUO_EXT_REF_CONTROL);
    }
}
