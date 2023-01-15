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
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.device.Rsp2DeviceParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Rsp2AmPort;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Rsp2Antenna;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Rsp2TunerParameters;

/**
 * RSP2 Tuner
 */
public class Rsp2Tuner extends RspTuner<Rsp2DeviceParameters, Rsp2TunerParameters>
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
    public Rsp2Tuner(Device device, SDRplay sdrplay, TunerSelect tunerSelect, Rsp2DeviceParameters deviceParameters,
                     Rsp2TunerParameters tunerParameters, ControlParameters controlParameters)
    {
        super(device, sdrplay, tunerSelect, deviceParameters, tunerParameters, controlParameters);
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
        update(UpdateReason.RSP2_EXT_REF_CONTROL);
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
        update(UpdateReason.RSP2_BIAS_T_CONTROL);
    }

    /**
     * Indicates if the RF notch is enabled
     */
    public boolean isRfNotch()
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
        update(UpdateReason.RSP2_RF_NOTCH_CONTROL);
    }

    /**
     * AM port selection
     */
    public Rsp2AmPort getAmPort()
    {
        return getTunerParameters().getAmPort();
    }

    /**
     * Sets the AM port
     * @param port to select
     * @throws SDRPlayException if there is an error
     */
    public void setAmPort(Rsp2AmPort port) throws SDRPlayException
    {
        getTunerParameters().setAmPort(port);
        update(UpdateReason.RSP2_AM_PORT_SELECT);
    }

    /**
     * Antenna selection
     */
    public Rsp2Antenna getAntenna()
    {
        return getTunerParameters().getAntenna();
    }

    /**
     * Sets the antenna
     * @param antenna to select
     * @throws SDRPlayException if there is an error
     */
    public void setAntenna(Rsp2Antenna antenna) throws SDRPlayException
    {
        getTunerParameters().setAntenna(antenna);
        update(UpdateReason.RSP2_ANTENNA_CONTROL);
    }
}
