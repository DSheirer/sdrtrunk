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
package io.github.dsheirer.source.tuner.rtl.e4k;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerController.E4KEnhanceGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerController.E4KGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerController.E4KLNAGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerController.E4KMixerGain;

public class E4KTunerConfiguration extends TunerConfiguration
{
    private E4KGain mMasterGain = E4KGain.MANUAL;
    private E4KMixerGain mMixerGain = E4KMixerGain.GAIN_4;
    private E4KLNAGain mLNAGain = E4KLNAGain.GAIN_PLUS_200;
    private E4KEnhanceGain mEnhanceGain = E4KEnhanceGain.GAIN_3;
    private double mFrequencyCorrection = 0.0d;
    private RTL2832TunerController.SampleRate mSampleRate = RTL2832TunerController.SampleRate.RATE_2_400MHZ;

    /**
     * Default constructor for JAXB
     */
    public E4KTunerConfiguration()
    {
    }

    public E4KTunerConfiguration(String uniqueID, String name)
    {
        super(uniqueID, name);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public TunerType getTunerType()
    {
        return TunerType.ELONICS_E4000;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "master_gain")
    public E4KGain getMasterGain()
    {
        return mMasterGain;
    }

    public void setMasterGain(E4KGain gain)
    {
        mMasterGain = gain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mixer_gain")
    public E4KMixerGain getMixerGain()
    {
        return mMixerGain;
    }

    public void setMixerGain(E4KMixerGain mixerGain)
    {
        mMixerGain = mixerGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
    public E4KLNAGain getLNAGain()
    {
        return mLNAGain;
    }

    public void setLNAGain(E4KLNAGain lnaGain)
    {
        mLNAGain = lnaGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "enhance_gain")
    public E4KEnhanceGain getEnhanceGain()
    {
        return mEnhanceGain;
    }

    public void setEnhanceGain(E4KEnhanceGain enhanceGain)
    {
        mEnhanceGain = enhanceGain;
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

}
