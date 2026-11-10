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

package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.sdrplay.DeviceEvent;
import io.github.dsheirer.sdrplay.DeviceEventListener;
import io.github.dsheirer.sdrplay.SampleBuffer;
import io.github.dsheirer.sdrplay.SampleListener;
import io.github.dsheirer.sdrplay.Tuner;
import io.github.dsheirer.util.ThreadPool;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract/Base RSP tuner controller
 */
public abstract class RspTunerController<I extends IControlRsp> extends TunerController
        implements DeviceEventListener, SampleListener
{
    private static final Logger mLog = LoggerFactory.getLogger(RspTunerController.class);
    protected static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 100_000;
    protected static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 2_000_000_000;
    protected static final int MIDDLE_UNUSABLE_BANDWIDTH = 0;
    private I mControlRsp;
    private RspNativeBufferFactory mNativeBufferFactory = new RspNativeBufferFactory(RspSampleRate.RATE_8_000);

    /**
     * Abstract tuner controller class.  The tuner controller manages frequency bandwidth and currently tuned channels
     * that are being fed samples from the tuner.
     *
     * @param controlRsp that is the RSP tuner
     * @param tunerErrorListener to monitor errors produced from this tuner controller
     */
    public RspTunerController(I controlRsp, ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        mControlRsp = controlRsp;

        //Register this controller to receive device events and sample streams when startStream() is invoked.
        mControlRsp.register(this, this);

        setMinimumFrequency(MINIMUM_TUNABLE_FREQUENCY_HZ);
        setMaximumFrequency(MAXIMUM_TUNABLE_FREQUENCY_HZ);
        setMiddleUnusableHalfBandwidth(MIDDLE_UNUSABLE_BANDWIDTH);
        setUsableBandwidthPercentage(1.0); //Initial value
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        if(config instanceof RspTunerConfiguration rtc)
        {
            //This sets the frequency and software PPM
            super.apply(config);

            try
            {
                setSampleRate(rtc.getSampleRate());
            }
            catch(RspException se)
            {
                mLog.error("Error setting RSP sample rate to " + rtc.getSampleRate(), se);
            }

            try
            {
                int lna = rtc.getLNA();
                int gr = rtc.getBasebandGainReduction();
                getControlRsp().setGain(lna, gr);
            }
            catch(RspException se)
            {
                mLog.error("Error setting RSP LNA [" + rtc.getLNA() + "] and baseband [" + rtc.getBasebandGainReduction() + "]");
            }
        }
    }

    /**
     * The RSP control for this tuner controller
     */
    public I getControlRsp()
    {
        return mControlRsp;
    }

    /**
     * SDRplay tuner (A or B) for this controller.
     */
    public Tuner getSdrplayTuner()
    {
        return mControlRsp.getSdrplayTuner();
    }

    /**
     * Processes samples from the API.  Invoked on the API's stream callback thread.
     * @param sampleBuffer containing the samples, valid only until this method returns.
     */
    @Override
    public void onSamples(SampleBuffer sampleBuffer)
    {
        //The native buffer factory repackages the samples into buffers with the smallest power-of-2 length that is
        //no smaller than the incoming buffer length (minimum 128) to ensure that down-stream vector optimized
        //functions can process the data.
        List<RspNativeBuffer> buffers = mNativeBufferFactory.get(sampleBuffer.copyI(), sampleBuffer.copyQ(),
                System.currentTimeMillis());

        for(RspNativeBuffer buffer: buffers)
        {
            mNativeBufferBroadcaster.broadcast(buffer);
        }
    }

    /**
     * Processes device events from the API.  Invoked on the API's event callback thread.  The jsdrplay library
     * acknowledges power overload events to the API before invoking this method.
     * @param event from the API
     */
    @Override
    public void onEvent(DeviceEvent event)
    {
        switch(event)
        {
            case DeviceEvent.PowerOverload overload ->
            {
                if(overload.overloaded())
                {
                    getControlRsp().processPowerOverload(overload.tuner());
                }
            }
            //The device can't be closed from an API callback thread, so process the removal on another thread
            case DeviceEvent.DeviceRemoved removed -> ThreadPool.CACHED.submit(() -> processDeviceRemoval(removed.tuner()));
            case DeviceEvent.DeviceFailure failure -> ThreadPool.CACHED.submit(() -> processDeviceRemoval(failure.tuner()));
            default -> {} //Ignore gain change and RSPduo mode change events
        }
    }

    /**
     * Starts this RSP tuner and attempts to claim the device via the API
     * @throws SourceException
     */
    @Override
    public void start() throws SourceException
    {
        try
        {
            mControlRsp.start();
        }
        catch(RspException se)
        {
            throw new SourceException("Unable to select tuner - " + se.getMessage(), se);
        }
    }

    @Override
    public void stop()
    {
        try
        {
            mControlRsp.stop();
        }
        catch(RspException se)
        {
            mLog.error("Error releasing RSP tuner device", se);
        }
    }

    /**
     * Adds the listener to receive native buffers and starts the sample stream when this is the first listener.
     * @param listener to add
     */
    @Override
    public void addBufferListener(Listener<INativeBuffer> listener)
    {
        getLock().lock();

        try
        {
            if(!hasBufferListeners())
            {
                mControlRsp.startStream();
            }

            super.addBufferListener(listener);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Removes the listener from receiving native buffers and stops the sample stream when this is the last listener.
     * @param listener to remove
     */
    @Override
    public void removeBufferListener(Listener<INativeBuffer> listener)
    {
        getLock().lock();

        try
        {
            super.removeBufferListener(listener);

            if(!hasBufferListeners())
            {
                mControlRsp.stopStream();
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Currently tuned frequency
     * @return frequency in Hertz
     * @throws SourceException if there is an error
     */
    @Override
    public long getTunedFrequency() throws SourceException
    {
        try
        {
            return getControlRsp().getTunedFrequency();
        }
        catch(RspException se)
        {
            throw new SourceException("Error setting tuned frequency", se);
        }
    }

    /**
     * Sets the center tuned frequency.
     * @param frequency in Hertz
     * @throws SourceException if there is an error
     */
    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        try
        {
            getControlRsp().setTunedFrequency(frequency);
        }
        catch(RspException se)
        {
            throw new SourceException("Unable to set center frequency [" + frequency + "]", se);
        }
    }

    /**
     * Sets the sample rate
     * @param rspSampleRate to apply
     * @throws RspException if there is an error
     */
    public void setSampleRate(RspSampleRate rspSampleRate) throws RspException
    {
        getControlRsp().setSampleRate(rspSampleRate);

        try
        {
            mFrequencyController.setSampleRate((int) rspSampleRate.getEffectiveSampleRate());
        }
        catch(SourceException se)
        {
            mLog.error("Error setting sample rate in frequency controller");
        }

        //Update the usable bandwidth based on the sample rate and filtered bandwidth
        setUsableBandwidthPercentage(rspSampleRate.getUsableBandwidth());
        mNativeBufferFactory.setSampleRate(rspSampleRate);
    }

    @Override
    public double getCurrentSampleRate() throws SourceException
    {
        return getControlRsp().getCurrentSampleRate();
    }

    @Override
    public int getBufferSampleCount()
    {
        return 128;
    }

    /**
     * Processes a device removal (or failure) event.
     * @param tuner that was removed
     */
    public void processDeviceRemoval(Tuner tuner)
    {
        mLog.info("Processing device removal for tuner [" + tuner + "] controller class [" + getClass().getSimpleName() + "]");
        //Command the control RSP to release the device so that we cease all attempts to control the device via the API
        if(getControlRsp() != null)
        {
            getControlRsp().deviceRemoved();
        }

        //Signal that tuner is removed so parent tuner and discovered tuner wrapper can do graceful removal/cleanup
        tunerRemoved();
    }
}
