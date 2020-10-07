/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.sammy1am.sdrplay.ApiException;
import io.github.sammy1am.sdrplay.ApiException.AlreadyInitialisedException;
import io.github.sammy1am.sdrplay.ApiException.NotInitialisedException;
import io.github.sammy1am.sdrplay.SDRplayDevice;
import io.github.sammy1am.sdrplay.StreamsReceiver;
import io.github.sammy1am.sdrplay.jnr.CallbackFnsT;
import io.github.sammy1am.sdrplay.jnr.SDRplayAPIJNR;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.Bw_MHzT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.If_kHzT;
import java.nio.FloatBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SDRplayTunerController extends TunerController implements StreamsReceiver
{
    private final static Logger mLog = LoggerFactory.getLogger(SDRplayTunerController.class);

    public static final long MIN_FREQUENCY = 10000000l;
    public static final long MAX_FREQUENCY = 6000000000l;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.95;
    public static final int DC_SPIKE_AVOID_BUFFER = 5000;

    private final SDRplayDevice mDevice;

    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("SDRplayTunerController");
    
    public SDRplayTunerController(SDRplayDevice device) throws SourceException
    {
        super(MIN_FREQUENCY, MAX_FREQUENCY, DC_SPIKE_AVOID_BUFFER, USABLE_BANDWIDTH_PERCENT);
        mDevice = device;
        mDevice.setStreamsReceiver(this);
        //mDevice.debugEnable(SDRplayAPIJNR.DbgLvl_t.DbgLvl_Verbose);
        
        // Set controller to match device defaults
        mFrequencyController.setFrequency((long) mDevice.getRfHz());
        mFrequencyController.setSampleRate((int) mDevice.getSampleRate());
        mFrequencyController.setFrequencyCorrection(mDevice.getPPM());
    }

    @Override
    public int getBufferSampleCount()
    {
        //return 1008;
        return 262144 / 2; // TODO: *shrug*
    }

    @Override
    public void dispose()
    {
        try {
            mDevice.uninit();
            mDevice.release();
        } catch (ApiException ae) {
            // Report, but don't throw-- we're disposing anyway
            mLog.warn("Exception while disposing", ae);
        }
    }

    /**
     * SDRplay serial number
     */
    public String getSerial()
    {
        return mDevice.getSerialNumber();
    }
    
    /**
     * SDRplay serial number
     */
    public String getModel()
    {
        return mDevice.getHWModel().name();
    }

    /**
     * 
     */
    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mFrequencyController.getTunedFrequency();
    }

    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        mDevice.setRfHz((double)frequency);
    }

    @Override
    public double getCurrentSampleRate()
    {
        return mDevice.getSampleRate();
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        if(config instanceof SDRplayTunerConfiguration)
        {
            SDRplayTunerConfiguration sdrPlayConfig = (SDRplayTunerConfiguration)config;
            
            setSampleRate(sdrPlayConfig.getSampleRate());
            
            //TODO
            //setSampleRate(sdrPlayConfig.getSampleRate());
            //sdrPlayConfig.getFrequency();
//
//            //Convert legacy sample rate setting to new sample rates
//            if(!sdrPlayConfig.getSampleRate().isValidSampleRate())
//            {
//                mLog.warn("Changing legacy HackRF Sample Rates Setting [" + sdrPlayConfig.getSampleRate().name() + "] to current valid setting");
//                //TODO sdrPlayConfig.setSampleRate(HackRFSampleRate.RATE_5_0);
//            }
//
//            try
//            {
//                //setSampleRate(sdrPlayConfig.getSampleRate());
//                setFrequencyCorrection(sdrPlayConfig.getFrequencyCorrection());
//                setAmplifierEnabled(sdrPlayConfig.getAmplifierEnabled());
//                //setLNAGain(sdrPlayConfig.getLNAGain());
//                //setVGAGain(sdrPlayConfig.getVGAGain());
//                setFrequency(getFrequency());
//            }
//            catch(UsbException e)
//            {
//                throw new SourceException("Error while applying tuner "
//                    + "configuration", e);
//            }
//
//            try
//            {
//                setFrequency(sdrPlayConfig.getFrequency());
//            }
//            catch(SourceException se)
//            {
//                //Do nothing, we couldn't set the frequency
//            }
        }
        else
        {
            throw new IllegalArgumentException("Invalid tuner configuration "
                + "type [" + config.getClass() + "]");
        }
    }

    /**
     * Sample Rate
     *
     */
    public void setSampleRate(int newSampleRate) throws SourceException
    {
        mDevice.setSampleRate(newSampleRate);
        mFrequencyController.setSampleRate(newSampleRate);
        
        //TODO: HackRF set some sort of baseband filter based on the sample rate-- maybe try that?
    }
    
    /**
     * Adds the IQ buffer listener and automatically starts buffer transfer processing, if not already started.
     */
    @Override
    public void addBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        if(!hasBufferListeners())
        {
            try {
                mDevice.init();
            } catch (AlreadyInitialisedException aie) {
                mLog.info("Attempted to initialized already initialized SDRplay device");
            }
        }
        
        super.addBufferListener(listener);
    }
    
    /**
     * Removes the IQ buffer listener and stops buffer transfer processing if there are no more listeners.
     */
    @Override
    public void removeBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        super.removeBufferListener(listener);

        if(!hasBufferListeners())
        {
            try {
                mDevice.uninit();
            } catch (ApiException ae) {
                // Report this, but no need to throw (we're trying to uninitialize anyway)
                mLog.warn("Exception will uninitializing", ae);
            }
        }
    }
    
    public void setIfType(If_kHzT ifType) {
        mDevice.setIfType(ifType);    
    }
    
    public If_kHzT getIfType() {
        return mDevice.getIfType();
    }
    
    public void setIFBandwidth(Bw_MHzT bwMode) {
        mDevice.setBwType(bwMode);
    }
    
    public Bw_MHzT getIFBandwidth() {
        return mDevice.getBwType();
    }

    @Override
    public void receiveStreamA(short[] xi, short[] xq, CallbackFnsT.StreamCbParamsT params, int numSamples, int reset) {
        ReusableComplexBuffer buffer = mReusableComplexBufferQueue.getBuffer(numSamples*2);

            float[] primitiveFloatBuffer = new float[numSamples*2]; // TODO Not the most efficient, ideall we'd have a ReusableBuffer with separate I/Q buffers

            for (int s=0;s<numSamples;s++) {
                primitiveFloatBuffer[s*2] = (float)xi[s]/4095; // Divide to bring value between -1 and 1
                primitiveFloatBuffer[(s*2)+1] = (float)xq[s]/4095;
            }

            buffer.reloadFrom(FloatBuffer.wrap(primitiveFloatBuffer), System.currentTimeMillis());

            mReusableBufferBroadcaster.broadcast(buffer);
    }

    @Override
    public void receiveEvent(CallbackFnsT.EventT eventId, TunerParamsT.TunerSelectT tuner, CallbackFnsT.EventParamsT params) {
        System.out.println("Event: " + eventId);
    }
}