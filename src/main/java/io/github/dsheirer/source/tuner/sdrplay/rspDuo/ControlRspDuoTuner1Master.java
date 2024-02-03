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

package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.api.DeviceSelectionMode;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.device.RspDuoDevice;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RSPduo operating in dual-tuner mode with tuner 1 configured as master.
 */
public class ControlRspDuoTuner1Master extends ControlRspDuoTuner1
{
    private static final Logger mLog = LoggerFactory.getLogger(ControlRspDuoTuner1Master.class);

    /**
     * Bridge to tuner 2 slave device.
     */
    private MasterSlaveBridge mMasterSlaveBridge;

    /**
     * Flag to indicate that continuous streaming is required because the slave device is streaming.
     */
    private boolean mContinuousStream = false;

    /**
     * Constructs an instance
     *
     * @param device to control
     */
    public ControlRspDuoTuner1Master(RspDuoDevice device, MasterSlaveBridge bridge)
    {
        super(device);
        mMasterSlaveBridge = bridge;
    }

    @Override
    public DeviceSelectionMode getDeviceSelectionMode()
    {
        return DeviceSelectionMode.MASTER_TUNER_1;
    }

    @Override
    public EnumSet<RspSampleRate> getSupportedSampleRates()
    {
        return RspSampleRate.DUAL_TUNER_SAMPLE_RATES;
    }

    @Override
    public void setSampleRate(RspSampleRate sampleRate) throws SDRPlayException
    {
        if(!sampleRate.isDualTunerSampleRate())
        {
            throw new SDRPlayException("Use dual-tuner sample rates only");
        }

        super.setSampleRate(sampleRate);

        //Notify tuner 2 slave device to configure for this sample rate
        if(mMasterSlaveBridge != null)
        {
            mMasterSlaveBridge.notifySampleRate(sampleRate);
        }
    }

    /**
     * Overrides parent stop method to disable continous streaming flag before stopping this device so that the
     * stream can be stopped.
     * @throws SDRPlayException if there is an error
     */
    @Override
    public void stop() throws SDRPlayException
    {
        mStreamingLock.lock();

        try
        {
            if(mMasterSlaveBridge != null)
            {
                mMasterSlaveBridge.stopSlave();
            }
        }
        catch(Exception e)
        {
            mLog.error("Error stopping slave device while preparing to stop the master device.");
        }

        try
        {
            mContinuousStream = false;
            super.stop();
        }
        finally
        {
            mStreamingLock.unlock();
        }
    }

    @Override
    public void stopStream()
    {
        mStreamingLock.lock();

        try
        {
            if(!mContinuousStream)
            {
                super.stopStream();
            }
        }
        finally
        {
            mStreamingLock.unlock();
        }
    }

    /**
     * Starts this tuner streaming continuously.  Streaming will only stop once stop() is invoked.  This method is
     * invoked by a tuner 2 slave device to ensure that this master tuner 1 device is streaming continously while the
     * slave is streaming.
     */
    public void startStreamContinuously()
    {
        mStreamingLock.lock();

        try
        {
            mContinuousStream = true;
            startStream();
        }
        finally
        {
            mStreamingLock.unlock();
        }
    }
}
