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

package io.github.dsheirer.source.tuner.sdrplay;

import com.github.dsheirer.sdrplay.DeviceSelectionMode;
import com.github.dsheirer.sdrplay.SDRPlayException;
import com.github.dsheirer.sdrplay.Status;
import com.github.dsheirer.sdrplay.UpdateReason;
import com.github.dsheirer.sdrplay.callback.IDeviceEventListener;
import com.github.dsheirer.sdrplay.callback.IStreamListener;
import com.github.dsheirer.sdrplay.device.Device;
import com.github.dsheirer.sdrplay.device.TunerSelect;
import com.github.dsheirer.sdrplay.parameter.control.AgcMode;
import com.github.dsheirer.sdrplay.parameter.tuner.GainReduction;
import com.github.dsheirer.sdrplay.parameter.tuner.IfMode;
import com.github.dsheirer.sdrplay.parameter.tuner.LoMode;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base RSP control implementation
 */
public abstract class ControlRsp<T extends Device> implements IControlRsp
{
    private static final Logger mLog = LoggerFactory.getLogger(ControlRsp.class);
    private T mDevice;
    protected RspSampleRate mSampleRate = RspSampleRate.RATE_8_000;
    protected int mGain;
    private IDeviceEventListener mDeviceEventListener;
    private IStreamListener mStreamListener;

    //Streaming control lock and boolean status indicator.  Access to the boolean indicator is protected by the lock.
    protected ReentrantLock mStreamingLock = new ReentrantLock();
    protected boolean mStreaming = false;

    /**
     * Constructs an instance
     * @param device for the device, obtained from the API
     */
    public ControlRsp(T device)
    {
        mDevice = device;
    }

    @Override
    public TunerSelect getTunerSelect()
    {
        return TunerSelect.TUNER_1;
    }

    @Override
    public void start() throws SDRPlayException
    {
        if(hasDevice())
        {
            getDevice().select();

            //Enable automatic DC and I/Q correction
            getDevice().getCompositeParameters().getControlAParameters().getDcOffset().setDC(true);
            getDevice().getCompositeParameters().getControlAParameters().getDcOffset().setIQ(true);

            //Setup IF, LO, and AGC Mode
            getDevice().getCompositeParameters().getControlAParameters().getAgc().setAgcMode(AgcMode.DISABLE);
            getDevice().setIfMode(IfMode.IF_ZERO);
            getDevice().setLoMode(LoMode.AUTO);
        }
        else
        {
            throw new SDRPlayException("Unable to start - device is null");
        }
    }

    @Override
    public void stop() throws SDRPlayException
    {
        if(hasDevice())
        {
            stopStream();
            getDevice().release();
            clearDevice();
        }
    }

    @Override
    public void startStream()
    {
        mStreamingLock.lock();

        try
        {
            if(hasDevice())
            {
                if(!mStreaming)
                {
                    getDevice().initStreamA(getDeviceEventListener(), getStreamListener());
                    mStreaming = true;
                }
            }
            else
            {
                mLog.error("Unable to start RSP device sample stream - device not started");
            }
        }
        catch(SDRPlayException se)
        {
            if(se.getStatus() == Status.FAIL)
            {
                mLog.error("Unable to start RSP streaming - device may have been unplugged - removing.");
                if(mDeviceEventListener != null)
                {
                    mDeviceEventListener.processDeviceRemoval(getTunerSelect());
                }
            }
            else
            {
                mLog.error("Unable to initialize/start streaming for RSP device", se);
            }
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
            if(hasDevice() && mStreaming)
            {
                getDevice().uninitialize();
            }
        }
        catch(SDRPlayException se)
        {
            mLog.error("Unable to uninitialize/stop streaming for RSP device");
        }
        finally
        {
            mStreaming = false;
            mStreamingLock.unlock();
        }
    }

    @Override
    public DeviceSelectionMode getDeviceSelectionMode()
    {
        //Default device selection mode for all tuners except the RSPduo
        return DeviceSelectionMode.SINGLE_TUNER_1;
    }

    /**
     * Device descriptor for this device
     */
    public T getDevice()
    {
        return mDevice;
    }

    /**
     * RSP device was unplugged from USB bus.  Clear the device and do not attempt any further interaction with the
     * device via the API.
     */
    @Override
    public void deviceRemoved()
    {
        clearDevice();

        mStreamingLock.lock();

        try
        {
            mStreaming = false;
        }
        finally
        {
            mStreamingLock.unlock();
        }
    }

    /**
     * Clears/nullifies the device.  This should be invoked after stopping the device to prevent reuse.
     */
    public void clearDevice()
    {
        mDevice = null;
    }

    /**
     * Indicates if this control has a non-null device.
     * @return true if non-null device.
     */
    public boolean hasDevice()
    {
        return getDevice() != null;
    }

    /**
     * Sets the sample rate
     * @param sampleRate enumeration value.
     * @throws SDRPlayException
     */
    @Override
    public void setSampleRate(RspSampleRate sampleRate) throws SDRPlayException
    {
        if(hasDevice())
        {
            mSampleRate = sampleRate;
            getDevice().getTuner().setBandwidth(sampleRate.getBandwidth());
            getDevice().getCompositeParameters().getDeviceParameters().getSamplingFrequency()
                    .setSampleRate(sampleRate.getSampleRate());
            getDevice().update(getTunerSelect(), UpdateReason.DEVICE_SAMPLE_RATE);
            getDevice().getCompositeParameters().getControlAParameters().getDecimation().setWideBandSignal(true);
            getDevice().setDecimation(sampleRate.getDecimation());
        }
        else
        {
            throw new SDRPlayException("RSP tuner has not been started - can't apply sample rate");
        }
    }

    /**
     * Sets the gain index
     * @param gain index value (0 - 28)
     * @throws SDRPlayException
     */
    @Override
    public void setGain(int gain) throws SDRPlayException
    {
        validateGain(gain);

        if(gain != mGain)
        {
            mGain = gain;
            getDevice().getTuner().setGain(mGain);
        }
    }


    @Override
    public void acknowledgePowerOverload(TunerSelect tunerSelect) throws SDRPlayException
    {
        if(hasDevice())
        {
            getDevice().acknowledgePowerOverload(tunerSelect);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public long getTunedFrequency() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getDevice().getTuner().getFrequency();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setTunedFrequency(long frequency) throws SDRPlayException
    {
        if(hasDevice())
        {
            getDevice().getTuner().setFrequency(frequency);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    /**
     * Current sample rate setting
     */
    @Override
    public RspSampleRate getSampleRateEnumeration()
    {
        return mSampleRate;
    }

    /**
     * Current sample rate
     */
    @Override
    public double getCurrentSampleRate()
    {
        return getSampleRateEnumeration().getEffectiveSampleRate();
    }

    /**
     * Current gain index
     * @return gain index
     */
    @Override
    public int getGain()
    {
        return mGain;
    }

    /**
     * Verifies that the requested gain index setting is within the range of valid gain values.
     * @param gain to validate
     * @throws SDRPlayException if the gain index value is outside the range of valid values.
     */
    protected void validateGain(int gain) throws SDRPlayException
    {
        if(gain < GainReduction.MIN_GAIN_INDEX || gain > GainReduction.MAX_GAIN_INDEX)
        {
            throw new SDRPlayException("Invalid gain index value [" + gain + "].  Valid range is " +
                    GainReduction.MIN_GAIN_INDEX + " - " + GainReduction.MAX_GAIN_INDEX);
        }
    }

    /**
     * Registers listeners to receive device events and sample streams.
     * @param deviceEventListener to receive device events
     * @param streamListener to receive sample stream and related parameters.
     */
    @Override
    public void resister(IDeviceEventListener deviceEventListener, IStreamListener streamListener)
    {
        mDeviceEventListener = deviceEventListener;
        mStreamListener = streamListener;
    }

    /**
     * Registered device event listener
     * @return device event listener or null if a listener has not been registered.
     */
    protected IDeviceEventListener getDeviceEventListener()
    {
        return mDeviceEventListener;
    }

    /**
     * Registered stream listener
     * @return stream listener or null if a listener has not been registered.
     */
    protected IStreamListener getStreamListener()
    {
        return mStreamListener;
    }
}
