/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.sdrplay.IfMode;
import io.github.dsheirer.sdrplay.RspDevice;
import io.github.dsheirer.sdrplay.RspDuoDevice;
import io.github.dsheirer.sdrplay.RxChannel;
import io.github.dsheirer.sdrplay.SDRplay;
import io.github.dsheirer.sdrplay.SDRplayException;
import io.github.dsheirer.sdrplay.Tuner;
import io.github.dsheirer.source.tuner.sdrplay.ControlRsp;
import io.github.dsheirer.source.tuner.sdrplay.DeviceSelectionMode;
import io.github.dsheirer.source.tuner.sdrplay.RspException;
import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;

/**
 * Base control wrapper for an RSPduo operating in either single-tuner or master or slave mode.
 */
public abstract class ControlRspDuo extends ControlRsp<RspDuoDevice> implements IControlRspDuo
{
    /**
     * Sample rate for both tuners when tuner 1 is selected as master.
     */
    protected static final double MASTER_SAMPLE_RATE = 8_000_000;

    /**
     * Constructs an instance
     * @param deviceInfo for the device, obtained from the API
     */
    public ControlRspDuo(DeviceInfo deviceInfo)
    {
        super(deviceInfo, RspDuoDevice.class);
    }

    /**
     * Device selection mode - tuner 1 or tuner 2, single or master/slave
     * @return mode
     */
    public abstract DeviceSelectionMode getDeviceSelectionMode();

    @Override
    public Tuner getSdrplayTuner()
    {
        return getDeviceSelectionMode().getTuner();
    }

    /**
     * Selects the RSPduo in the mode and tuner specified by the device selection mode.
     */
    @Override
    protected RspDevice select(SDRplay session, DeviceInfo deviceInfo) throws SDRplayException
    {
        DeviceSelectionMode mode = getDeviceSelectionMode();
        return session.selectRspDuo(deviceInfo, mode.getRspDuoMode(), mode.getTuner(),
                mode.isMasterMode() ? MASTER_SAMPLE_RATE : 0);
    }

    /**
     * In master/slave dual-tuner mode use IF 2.048 MHz, otherwise use zero IF
     */
    @Override
    protected IfMode getIfMode()
    {
        return getDeviceSelectionMode().isSingleTunerMode() ? IfMode.ZERO : IfMode.KHZ_2048;
    }

    /**
     * Sets the sample rate.  In single tuner mode, sets the ADC sample rate.  In master/slave mode, the ADC sample
     * rate (8 MHz) is shared by both tuners and is fixed when the master is selected, so this only sets the bandwidth
     * and the decimation.
     * @param sampleRate enumeration value.
     * @throws RspException if the device is not started/initialized
     */
    @Override
    public void setSampleRate(RspSampleRate sampleRate) throws RspException
    {
        command("set sample rate " + sampleRate, device -> {
            RxChannel channel = device.channel();
            channel.setBandwidth(sampleRate.getBandwidth());

            if(getDeviceSelectionMode().isSingleTunerMode())
            {
                device.setSampleRate(sampleRate.getSampleRate());
            }

            channel.setDecimation(sampleRate.getDecimation(), true);
        });

        mSampleRate = sampleRate;
    }

    @Override
    public boolean isRfDabNotch() throws RspException
    {
        return query("get RF DAB notch", device -> device.isRfDabNotch(getSdrplayTuner()));
    }

    @Override
    public void setRfDabNotch(boolean enabled) throws RspException
    {
        command("set RF DAB notch", device -> device.setRfDabNotch(getSdrplayTuner(), enabled));
    }

    @Override
    public boolean isRfNotch() throws RspException
    {
        return query("get RF notch", device -> device.isRfNotch(getSdrplayTuner()));
    }

    @Override
    public void setRfNotch(boolean enabled) throws RspException
    {
        command("set RF notch", device -> device.setRfNotch(getSdrplayTuner(), enabled));
    }

    @Override
    public boolean isExternalReferenceOutput() throws RspException
    {
        return query("get external reference output", RspDuoDevice::isExternalReferenceOutput);
    }

    @Override
    public void setExternalReferenceOutput(boolean enabled) throws RspException
    {
        command("set external reference output", device -> device.setExternalReferenceOutput(enabled));
    }
}
