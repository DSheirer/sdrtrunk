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

import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.api.DeviceSelectionMode;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.device.RspDuoDevice;
import java.util.EnumSet;

/**
 * Control wrapper for an RSPduo Tuner 2 operating in dual-tuner mode as the slave device
 */
public class ControlRspDuoTuner2Slave extends ControlRspDuoTuner2
{
    private MasterSlaveBridge mMasterSlaveBridge;

    /**
     * Constructs an instance
     *
     * @param device to control
     */
    public ControlRspDuoTuner2Slave(RspDuoDevice device, MasterSlaveBridge bridge)
    {
        super(device);
        mMasterSlaveBridge = bridge;
    }

    /**
     * Overrides the default method to request that master starts streaming so that this slave can also
     * start streaming.  Uses the bridge to put master in a continuous streaming mode where master is
     * started and a flag is set to prevent master from stopping streaming.
     */
    @Override
    public void startStream()
    {
        if(mMasterSlaveBridge != null)
        {
            mMasterSlaveBridge.startMasterStreamContinuously();
        }

        super.startStream();
    }

    @Override
    public boolean isSlaveMode()
    {
        return true;
    }

    @Override
    public DeviceSelectionMode getDeviceSelectionMode()
    {
        return DeviceSelectionMode.SLAVE_TUNER_2;
    }

    @Override
    public EnumSet<RspSampleRate> getSupportedSampleRates()
    {
        //In dual-tuner mode only the master device can set the sample rate
        return EnumSet.noneOf(RspSampleRate.class);
    }

    @Override
    public void setSampleRate(RspSampleRate sampleRate) throws SDRPlayException
    {
        if(hasDevice())
        {
            mSampleRate = sampleRate;
            getDevice().getTuner().setBandwidth(sampleRate.getBandwidth());
            getControlParameters().getDecimation().setWideBandSignal(true);
            getDevice().setDecimation(sampleRate.getDecimation());
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setExternalReferenceOutput(boolean enabled) throws SDRPlayException
    {
        //Ignore ... tuner 2 configured for slave mode does not control the external reference output
    }
}
