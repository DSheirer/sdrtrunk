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
import io.github.dsheirer.source.tuner.FrequencyErrorCorrectionManager;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.sammy1am.sdrplay.ApiException;
import io.github.sammy1am.sdrplay.ApiException.AlreadyInitialisedException;
import io.github.sammy1am.sdrplay.ApiException.NotInitialisedException;
import io.github.sammy1am.sdrplay.EventParameters;
import io.github.sammy1am.sdrplay.EventParameters.GainCbParam;
import io.github.sammy1am.sdrplay.EventParameters.PowerOverloadCbParam;
import io.github.sammy1am.sdrplay.SDRplayDevice;
import io.github.sammy1am.sdrplay.StreamsReceiver;
import io.github.sammy1am.sdrplay.jnr.CallbackFnsT;
import io.github.sammy1am.sdrplay.jnr.CallbackFnsT.EventT;
import io.github.sammy1am.sdrplay.jnr.SDRplayAPIJNR;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.Bw_MHzT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.If_kHzT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.LoModeT;
import io.github.sammy1am.sdrplay.model.RSP1A;
import io.github.sammy1am.sdrplay.model.RSPDuo;
import io.github.sammy1am.sdrplay.SDRplayAPI;

import java.nio.FloatBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SDRplayTunerController extends TunerController implements StreamsReceiver
{
    private final static Logger mLog = LoggerFactory.getLogger(SDRplayTunerController.class);

    public static final long MIN_FREQUENCY = 10_000_000l;
    public static final long MAX_FREQUENCY = 2_000_000_000l;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.95;
    public static final int DC_SPIKE_AVOID_BUFFER = 5000;

    private final SDRplayDevice mDevice;
    private GainChangeListener mUIGainChangeListener;
    private OverloadListener mUIOverloadListener;

    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("SDRplayTunerController");
    
    public FrequencyErrorCorrectionManager mFrequencyErrorCorrectionManager;
    
    public SDRplayTunerController(SDRplayDevice device) throws SourceException
    {
        super(MIN_FREQUENCY, MAX_FREQUENCY, DC_SPIKE_AVOID_BUFFER, USABLE_BANDWIDTH_PERCENT);
        mDevice = device;
        
        //tell device to send SDRPlay events to this object's implementation of StreamsReceiver
        mDevice.setStreamsReceiver(this);
        
        mDevice.debugEnable(SDRplayAPIJNR.DbgLvl_t.DbgLvl_Verbose);
        //mDevice.debugEnable(SDRplayAPIJNR.DbgLvl_t.DbgLvl_Error);
        //mDevice.debugEnable(SDRplayAPIJNR.DbgLvl_t.DbgLvl_Message);
        //mDevice.debugEnable(SDRplayAPIJNR.DbgLvl_t.DbgLvl_Warning);
        
        mFrequencyController.setFrequency((long) mDevice.getRfHz());
        mFrequencyController.setSampleRate((int) mDevice.getSampleRate());
        mFrequencyController.setFrequencyCorrection(mDevice.getPPM());
        mFrequencyErrorCorrectionManager = new FrequencyErrorCorrectionManager(this);
        
        //Set Local Oscillator to Auto - only used for AM?
        mDevice.setLoMode(LoModeT.LO_Auto);
        
        mUIGainChangeListener = new GainChangeListener() {};
        mUIOverloadListener = new OverloadListener() {};
        
        
    }
    
    public interface GainChangeListener {
    	default void gainChange (GainCbParam params) {}
    }
    
    public void addGainChangeListener(GainChangeListener gainL) {
    	this.mUIGainChangeListener = gainL;
    }
    
    public interface OverloadListener {
    	default void overloadEvent(PowerOverloadCbParam ol) {}
    }
    
    public void addOverloadListener(OverloadListener ovrLdListener) {
    	this.mUIOverloadListener = ovrLdListener;
    }

    /**
     * Manager for applying automatic frequency error PPM adjustments to the tuner controller based on
     * frequency error measurements received from certain downstream decoders (e.g. P25).
     * @return manager
     */
    public FrequencyErrorCorrectionManager getFrequencyErrorCorrectionManager()
    {
        return mFrequencyErrorCorrectionManager;
    }
    
    /**
     * Overrides updates for measured frequency error so that the updates can also be applied to the
     * frequency error correction manager for automatic PPM updating.
     * @param measuredFrequencyError in hertz averaged over a 5 second interval.
     */
    @Override
    public void setMeasuredFrequencyError(int measuredFrequencyError)
    {
        super.setMeasuredFrequencyError(measuredFrequencyError);
        getFrequencyErrorCorrectionManager().updatePPM(getPPMFrequencyError());
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
            setFrequencyCorrection(sdrPlayConfig.getFrequencyCorrection());
            getFrequencyErrorCorrectionManager().setEnabled(sdrPlayConfig.getAutoPPMCorrectionEnabled());
            
        }
        else
        {
            throw new IllegalArgumentException("Invalid tuner configuration "
                + "type [" + config.getClass() + "]");
        }
    }
    
    /**
     * Sample Rate
     * Sets the Sample Rate and the IF Bandwidth to match.
     * Also forces IF Mode to Zero IF for all sample rates.
     * These are optimum settings for sdrtrunk P25 demodulation,
     * any other combination of values do not produce results.
     * These combinations were reverse engineered from SDRuno.
     * @param newSampleRate
     */
    public void setSampleRate(int newSampleRate) throws SourceException
    {
        mDevice.setSampleRate(newSampleRate);
        mFrequencyController.setSampleRate(newSampleRate);
        
        Bw_MHzT newbwType;
        switch(newSampleRate) {
        case 2_000_000: case 3_000_000: case 4_000_000:
        	newbwType = Bw_MHzT.BW_1_536;
        	break;
        case 5_000_000:
        	newbwType = Bw_MHzT.BW_5_000;
        	break;
        case 6_000_000:
        	newbwType = Bw_MHzT.BW_6_000;
        	break;
        case 7_000_000:
        	newbwType = Bw_MHzT.BW_7_000;
        	break;
        case 8_000_000: case 9_000_000: case 10_000_000:
        	newbwType = Bw_MHzT.BW_8_000;
        	break;
        default:
        	newbwType = Bw_MHzT.BW_1_536;
        }
        mDevice.setSampleRateAndIF(newSampleRate, newbwType, If_kHzT.IF_Zero);
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
                mLog.warn("Exception while uninitializing", ae);
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
    
    public boolean getAGCEnabled() {
    	return mDevice.getAGCEnabled();
    }
    public void setAGCEnabled(boolean newAGCstate) {
    	mDevice.setAGCEnabled(newAGCstate);
    }
    
    public int getNumLNAStates() {
        return mDevice.getNumLNAStates();
    }
    
    public void setLNAState(int lnaState) {
        mDevice.setLNAState((byte) lnaState);
    }
    
    public int getLNAState() {
        return mDevice.getLNAState();
    }
    
    public void setIFGain(int ifGain) {
    	mDevice.setIFGain(ifGain);
    }
    
    public int getIFGain() {
    	return mDevice.getIFGain();
    }
    
    public boolean getRfNotch() {
    		return  mDevice.getRfNotch();
    }
    
    public void setRfNotch(boolean rfNotch) {
    	mDevice.setRfNotch(rfNotch);
    }

    public boolean getDABNotch() {
    		return  mDevice.getDABNotch();
    }
    
    public void setDABNotch(boolean DABNotch) {
    	mDevice.setDABNotch(DABNotch);
    }
    
    public boolean getBiasT() {
		return  mDevice.getBiasT();
    }

    public void setBiasT(boolean biasT) {
    	mDevice.setBiasT(biasT);
    }    
    
    public int getDecFactor() {
    	return mDevice.getDecFactor();
    }
    public void setDecFactor(int newDecFactor) {
    	mDevice.setDecFactor(newDecFactor);
    }
    
    public float getTotalGain() {
    	return mDevice.getTotalGain();
    }
    
    public int getAntenna() {
    	return mDevice.getAntenna();
    }
    public void setAntenna(int antSel) {
    	mDevice.setAntenna(antSel);
    }
    
    @Override
    public void receiveStreamA(short[] xi, short[] xq, CallbackFnsT.StreamCbParamsT params, int numSamples, int reset) {
        ReusableComplexBuffer buffer = mReusableComplexBufferQueue.getBuffer(numSamples*2);

            float[] primitiveFloatBuffer = new float[numSamples*2]; // TODO Not the most efficient, ideall we'd have a ReusableBuffer with separate I/Q buffers

            for (int s=0;s<numSamples;s++) {
                primitiveFloatBuffer[s*2] = (float)xi[s]/2047; // Divide 2^11 to bring value between -1 and 1
                primitiveFloatBuffer[(s*2)+1] = (float)xq[s]/2047;
            }
            
            buffer.reloadFrom(FloatBuffer.wrap(primitiveFloatBuffer), System.currentTimeMillis());
            
            broadcast(buffer);
    }

    @Override
    public void receiveEvent(EventT eventId, TunerParamsT.TunerSelectT tuner, EventParameters params) {
        switch (eventId) 
        {
            case PowerOverloadChange:
                mDevice.acknowledgeOverload();
                mUIOverloadListener.overloadEvent(params.powerOverloadParams);
                //mLog.info("Power Overload: " + params.powerOverloadParams.powerOverloadChangeType.toString());
                break;
            case GainChange:
            	mUIGainChangeListener.gainChange(params.gainParams);
            	//mLog.info("Gain Change:" + Double.toString( params.gainParams.currGain ));
            	break;
            default:
                mLog.info("Event: " + eventId.name());
        }
    }
}