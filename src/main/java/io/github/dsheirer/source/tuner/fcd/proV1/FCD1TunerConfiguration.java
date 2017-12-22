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
package io.github.dsheirer.source.tuner.fcd.proV1;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAEnhance;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAGain;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.MixerGain;

public class FCD1TunerConfiguration extends TunerConfiguration
{
    private double mFrequencyCorrection = 22.0d;
    private double mInphaseDCCorrection = 0.0d;
    private double mQuadratureDCCorrection = 0.0d;
    private double mPhaseCorrection = 0.0d;
    private double mGainCorrection = 0.0d;

    private LNAGain mLNAGain = LNAGain.LNA_GAIN_PLUS_20_0;
    private LNAEnhance mLNAEnhance = LNAEnhance.LNA_ENHANCE_OFF;
    private MixerGain mMixerGain = MixerGain.MIXER_GAIN_PLUS_12_0;

    /**
     * Default constructor for JAXB
     */
    public FCD1TunerConfiguration()
    {
    }

    public FCD1TunerConfiguration(String uniqueID, String name)
    {
        super(uniqueID, name);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public TunerType getTunerType()
    {
        return TunerType.FUNCUBE_DONGLE_PRO;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
    public LNAGain getLNAGain()
    {
        return mLNAGain;
    }

    public void setLNAGain(LNAGain gain)
    {
        mLNAGain = gain;
    }

    public LNAEnhance getLNAEnhance()
    {
        return mLNAEnhance;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_enhance")
    public void setLNAEnhance(LNAEnhance enhance)
    {
        mLNAEnhance = enhance;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mixer_gain")
    public MixerGain getMixerGain()
    {
        return mMixerGain;
    }

    public void setMixerGain(MixerGain gain)
    {
        mMixerGain = gain;
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

    @JacksonXmlProperty(isAttribute = true, localName = "inphase_dc_correction")
    public double getInphaseDCCorrection()
    {
        return mInphaseDCCorrection;
    }

    public void setInphaseDCCorrection(double value)
    {
        mInphaseDCCorrection = value;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "quadrature_dc_correction")
    public double getQuadratureDCCorrection()
    {
        return mQuadratureDCCorrection;
    }

    public void setQuadratureDCCorrection(double value)
    {
        mQuadratureDCCorrection = value;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "phase_correction")
    public double getPhaseCorrection()
    {
        return mPhaseCorrection;
    }

    public void setPhaseCorrection(double value)
    {
        mPhaseCorrection = value;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "gain_correction")
    public double getGainCorrection()
    {
        return mGainCorrection;
    }

    public void setGainCorrection(double value)
    {
        mGainCorrection = value;
    }
}
