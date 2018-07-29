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
package io.github.dsheirer.source.tuner.rtl.r820t;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;

public class R820TTunerConfiguration extends TunerConfiguration
{
    private R820TTunerController.R820TGain mMasterGain = R820TTunerController.R820TGain.GAIN_327;
    private R820TTunerController.R820TMixerGain mMixerGain = R820TTunerController.R820TMixerGain.GAIN_105;
    private R820TTunerController.R820TLNAGain mLNAGain = R820TTunerController.R820TLNAGain.GAIN_222;
    private R820TTunerController.R820TVGAGain mVGAGain = R820TTunerController.R820TVGAGain.GAIN_210;
    private double mFrequencyCorrection = 0.0d;
    private RTL2832TunerController.SampleRate mSampleRate = RTL2832TunerController.SampleRate.RATE_2_400MHZ;
    private boolean mAutoPPMCorrection = true;

    /**
     * Default constructor for JAXB
     */
    public R820TTunerConfiguration()
    {
    }

    public R820TTunerConfiguration(String uniqueID, String name)
    {
        super(uniqueID, name);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public TunerType getTunerType()
    {
        return TunerType.RAFAELMICRO_R820T;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "master_gain")
    public R820TTunerController.R820TGain getMasterGain()
    {
        return mMasterGain;
    }

    public void setMasterGain(R820TTunerController.R820TGain gain)
    {
        mMasterGain = gain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mixer_gain")
    public R820TTunerController.R820TMixerGain getMixerGain()
    {
        return mMixerGain;
    }

    public void setMixerGain(R820TTunerController.R820TMixerGain mixerGain)
    {
        mMixerGain = mixerGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
    public R820TTunerController.R820TLNAGain getLNAGain()
    {
        return mLNAGain;
    }

    public void setLNAGain(R820TTunerController.R820TLNAGain lnaGain)
    {
        mLNAGain = lnaGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "vga_gain")
    public R820TTunerController.R820TVGAGain getVGAGain()
    {
        return mVGAGain;
    }

    public void setVGAGain(R820TTunerController.R820TVGAGain vgaGain)
    {
        mVGAGain = vgaGain;
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
    public RTL2832TunerController.SampleRate getSampleRate()
    {
        return mSampleRate;
    }

    public void setSampleRate(RTL2832TunerController.SampleRate sampleRate)
    {
        mSampleRate = sampleRate;
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
}
