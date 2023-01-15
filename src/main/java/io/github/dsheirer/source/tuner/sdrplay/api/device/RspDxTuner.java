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
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.device.RspDxDeviceParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.HdrModeBandwidth;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDxAntenna;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDxTunerParameters;

/**
 * RSPdx Tuner
 */
public class RspDxTuner extends RspTuner<RspDxDeviceParameters, RspDxTunerParameters>
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
    public RspDxTuner(Device device, SDRplay sdrplay, TunerSelect tunerSelect, RspDxDeviceParameters deviceParameters,
                      RspDxTunerParameters tunerParameters, ControlParameters controlParameters)
    {
        super(device, sdrplay, tunerSelect, deviceParameters, tunerParameters, controlParameters);
    }

    /**
     * Indicates if HDR mode is enabled
     */
    public boolean isHdrMode()
    {
        return getDeviceParameters().isHdr();
    }

    /**
     * Enables or disables HDR mode
     * @param enable mode
     * @throws SDRPlayException if there is an error
     */
    public void setHdrMode(boolean enable) throws SDRPlayException
    {
        getDeviceParameters().setHdr(enable);
        update(UpdateReason.EXTENSION_RSP_DX_HDR_ENABLE);
    }

    /**
     * HDR mode bandwidth
     */
    public HdrModeBandwidth getHdrModeBandwidth()
    {
        return getTunerParameters().getHdrModeBandwidth();
    }

    /**
     * Sets HDR mode bandwidth
     * @param bandwidth to select
     * @throws SDRPlayException if there is an error
     */
    public void setHdrModeBandwidth(HdrModeBandwidth bandwidth) throws SDRPlayException
    {
        getTunerParameters().setHdrModeBandwidth(bandwidth);
        update(UpdateReason.EXTENSION_RSP_DX_HDR_BANDWIDTH);
    }

    /**
     * Indicates if the RF notch is enabled
     */
    public boolean isRfNotch()
    {
        return getDeviceParameters().isRfNotch();
    }

    /**
     * Enables or disables the RF notch
     * @param enable setting
     * @throws SDRPlayException if there is an error
     */
    public void setRfNotch(boolean enable) throws SDRPlayException
    {
        getDeviceParameters().setRfNotch(enable);
        update(UpdateReason.EXTENSION_RSP_DX_RF_NOTCH_CONTROL);
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
        update(UpdateReason.EXTENSION_RSP_DX_RF_DAB_NOTCH_CONTROL);
    }

    /**
     * Indicates if the Bias-T is enabled
     */
    public boolean isBiasT()
    {
        return getDeviceParameters().isBiasT();
    }

    /**
     * Enables or disables the Bias-T
     * @param enable value
     * @throws SDRPlayException if there is an error
     */
    public void setBiasT(boolean enable) throws SDRPlayException
    {
        getDeviceParameters().setBiasT(enable);
        update(UpdateReason.EXTENSION_RSP_DX_BIAS_T_CONTROL);
    }

    /**
     * Antenna selection
     */
    public RspDxAntenna getAntenna()
    {
        return getDeviceParameters().getRspDxAntenna();
    }

    /**
     * Sets the antenna selection
     * @param antenna to select
     * @throws SDRPlayException if there is an error
     */
    public void setAntenna(RspDxAntenna antenna) throws SDRPlayException
    {
        getDeviceParameters().setRspDxAntenna(antenna);
        update(UpdateReason.EXTENSION_RSP_DX_ANTENNA_CONTROL);
    }
}
