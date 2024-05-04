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
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.IDeviceEventListener;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.IStreamListener;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.StreamCallbackParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.event.EventType;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.event.GainCallbackParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.event.PowerOverloadCallbackParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.event.RspDuoModeCallbackParameters;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract/Base RSP tuner controller
 */
public abstract class RspTunerController<I extends IControlRsp> extends TunerController
        implements IDeviceEventListener, IStreamListener
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
        mControlRsp.resister(this, this);

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
            catch(SDRPlayException se)
            {
                mLog.error("Error setting RSP sample rate to " + rtc.getSampleRate(), se);
            }

            try
            {
                int lna = rtc.getLNA();
                int gr = rtc.getBasebandGainReduction();
                getControlRsp().setGain(lna, gr);
            }
            catch(SDRPlayException se)
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

    @Override
    public TunerSelect getTunerSelect()
    {
        return mControlRsp.getTunerSelect();
    }

    @Override
    public void processStream(short[] inphase, short[] quadrature,
                              StreamCallbackParameters parameters, boolean reset)
    {
        //The native buffer factory repackages the samples into buffers with the largest power-of-2 length that is
        //smaller than the incoming buffer length and no smaller than 128 to ensure that down-stream vector optimized
        //functions can process the data.
        List<RspNativeBuffer> buffers = mNativeBufferFactory.get(inphase, quadrature, System.currentTimeMillis());

        for(RspNativeBuffer buffer: buffers)
        {
            mNativeBufferBroadcaster.broadcast(buffer);
        }

        if(reset)
        {
//            mLog.info(getControlRsp().getDeviceDescriptor().getDeviceType() + " - sample stream was reset");
        }
        if(parameters.isGainReductionChanged())
        {
//            mLog.info("^^^ Got a gain reduction change");
        }
        if(parameters.isRfFrequencyChanged())
        {
//            mLog.info("^^^ Got a frequency changed");
        }
        if(parameters.isSampleRateChanged())
        {
//            mLog.info("^^^ Got a sample rate changed");
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
        catch(SDRPlayException se)
        {
            throw new SourceException("Unable to select tuner - in use");
        }
    }

    @Override
    public void stop()
    {
        try
        {
            mControlRsp.stop();
        }
        catch(SDRPlayException se)
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
        catch(SDRPlayException se)
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
        catch(SDRPlayException se)
        {
            throw new SourceException("Unable to set center frequency [" + frequency + "]", se);
        }
    }

    /**
     * Sets the sample rate
     * @param rspSampleRate to apply
     * @throws SDRPlayException if there is an error
     */
    public void setSampleRate(RspSampleRate rspSampleRate) throws SDRPlayException
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

    //Device Event Listener interface methods ...
    @Override
    public void processEvent(EventType eventType, TunerSelect tunerSelect) {}
    @Override
    public void processGainChange(TunerSelect tunerSelect, GainCallbackParameters parameters) {}
    @Override
    public void processRspDuoModeChange(TunerSelect tunerSelect, RspDuoModeCallbackParameters parameters) {}
    @Override
    public void processPowerOverload(TunerSelect tunerSelect, PowerOverloadCallbackParameters parameters)
    {
        if(getControlRsp() != null)
        {
            try
            {
                getControlRsp().acknowledgePowerOverload(tunerSelect);
            }
            catch(SDRPlayException se)
            {
                mLog.error("Unable to acknowledge power overload for [" + tunerSelect + "]", se);
            }
        }
    }

    @Override
    public void processDeviceRemoval(TunerSelect tunerSelect)
    {
        mLog.info("Processing device removal for tuner [" + tunerSelect + "] controller class [" + getClass().getSimpleName() + "]");
        //Command the control RSP to nullify the device so that we cease all attempts to control the device via the API
        if(getControlRsp() != null)
        {
            getControlRsp().deviceRemoved();
        }

        //Signal that tuner is removed so parent tuner and discovered tuner wrapper can do graceful removal/cleanup
        tunerRemoved();
    }
}
