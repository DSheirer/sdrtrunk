/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.source.tuner.sdrplay;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.Bw_MHzT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.If_kHzT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.LoModeT;


public class SDRplayTunerConfiguration extends TunerConfiguration
{
    private int mSampleRate = 10_000_000;
    private double mFrequencyCorrection = 0.0d;
    private boolean mAutoPPMCorrection = true;
    
    private If_kHzT mIfType = If_kHzT.IF_Zero;
    private Bw_MHzT mBwType = Bw_MHzT.BW_8_000;
    
    private boolean mAGCEnabled = false;
    
    private int mLNAState = 0;
    
 
	private boolean mRfNotch = true;
	private boolean mDABNotch = true;
	private boolean mBiasT = false;
	private int mDecFactor = 1;
    

    /**
     * Default constructor for JAXB
     */
    public SDRplayTunerConfiguration()
    {
    }

    public SDRplayTunerConfiguration(String uniqueID, String name)
    {
        super(uniqueID, name);
    }
    
    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public TunerType getTunerType()
    {
        return TunerType.SDRPLAY_RSP1;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_state")
    public int getLNAState()
    {
        return mLNAState;
    }

    public void setLNAState(int lnaState)
    {
        mLNAState = lnaState;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "frequency_correction")
    public double getFrequencyCorrection()
    {
        return mFrequencyCorrection;
    }

    public void setFrequencyCorrection(double value)
    {
        mFrequencyCorrection = value;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "sample_rate")
    public int getSampleRate()
    {
        return mSampleRate;
    }

    public void setSampleRate(int sampleRate)
    {
        mSampleRate = sampleRate;
    }
    
    @JacksonXmlProperty(isAttribute = true, localName = "if_type")
    public If_kHzT getIfType()
    {
        return mIfType;
    }

    public void setIfType(If_kHzT ifType)
    {
        mIfType = ifType;
    }
    
    @JacksonXmlProperty(isAttribute = true, localName = "bw_type")
    public Bw_MHzT getBwType()
    {
        return mBwType;
    }

    public void setBwType(Bw_MHzT bwType)
    {
        mBwType = bwType;
    }
    
    
    @JacksonXmlProperty(isAttribute = true, localName = "agc_enabled")
    public boolean getAGCEnabled()
    {
        return mAGCEnabled;
    }

    public void setAGCEnabled(boolean enabled)
    {
        mAGCEnabled = enabled;
    }
    
    /**
     * FM Broadcast Notch
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rf_notch")
    public boolean getRfNotch() {
    	return mRfNotch;
    }
    
    public void setRfNotch(boolean rfNotch) {
    	mRfNotch = rfNotch;
    }
    
    /**
     * Digital Audio Broadcast(DAB) Notch
     */
    @JacksonXmlProperty(isAttribute = true, localName = "dab_notch")
    public boolean getDABNotch() {
    	return mDABNotch;
    }
    
    public void setDABNotch(boolean dabNotch) {
    	mDABNotch = dabNotch;
    }    

    /**
     * BiasT 
     */
    @JacksonXmlProperty(isAttribute = true, localName = "bias_t")
    public boolean getBiasT() {
    	return mBiasT;
    }
    
    public void setBiasT(boolean biasT) {
    	mBiasT = biasT;
    }    
    
    /**
     * Indicates if automatic correction of PPM from measured frequency error is enabled/disabled.
     * @return true if auto-correction is enabled.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "auto_ppm_correction_enabled")
    public boolean getAutoPPMCorrectionEnabled()
    {
        return mAutoPPMCorrection;
    }

    /**
     * Sets the enabled state for auto-correction of PPM from measured frequency error values.
     * @param enabled
     */
    public void setAutoPPMCorrectionEnabled(boolean enabled)
    {
        mAutoPPMCorrection = enabled;
    }
    
    /**
     * Decimation Factor
     */
    
    @JacksonXmlProperty(isAttribute = true, localName = "dec_factor")
    public int getDecFactor() {
    	return mDecFactor;
    }
    public void setDecFactor(int newDecFactor) {
    	mDecFactor = newDecFactor;
    }
}
