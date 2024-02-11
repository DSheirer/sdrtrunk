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
package io.github.dsheirer.source.tuner.airspy;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerController.Gain;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;

public class AirspyTunerConfiguration extends TunerConfiguration
{
    private Gain mGain = AirspyTunerController.LINEARITY_GAIN_DEFAULT;
    private int mSampleRate = AirspyTunerController.DEFAULT_SAMPLE_RATE.getRate();
    private int mIFGain = AirspyTunerController.IF_GAIN_DEFAULT;
    private int mMixerGain = AirspyTunerController.MIXER_GAIN_DEFAULT;
    private int mLNAGain = AirspyTunerController.LNA_GAIN_DEFAULT;
    private boolean mMixerAGC = false;
    private boolean mLNAAGC = false;

    /**
     * Default constructor for JAXB
     */
    public AirspyTunerConfiguration()
    {
        super(AirspyTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ, AirspyTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    @Override
    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public TunerType getTunerType()
    {
        return TunerType.AIRSPY_R820T;
    }

    public AirspyTunerConfiguration(String uniqueID)
    {
        super(uniqueID);
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

    @JacksonXmlProperty(isAttribute = true, localName = "gain")
    public Gain getGain()
    {
        return mGain;
    }

    public void setGain(Gain gain)
    {
        mGain = gain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "if_gain")
    public int getIFGain()
    {
        return mIFGain;
    }

    public void setIFGain(int gain)
    {
        mIFGain = gain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mixer_gain")
    public int getMixerGain()
    {
        return mMixerGain;
    }

    public void setMixerGain(int gain)
    {
        mMixerGain = gain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
    public int getLNAGain()
    {
        return mLNAGain;
    }

    public void setLNAGain(int gain)
    {
        mLNAGain = gain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mixer_agc")
    public boolean isMixerAGC()
    {
        return mMixerAGC;
    }

    public void setMixerAGC(boolean enabled)
    {
        mMixerAGC = enabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_agc")
    public boolean isLNAAGC()
    {
        return mLNAAGC;
    }

    public void setLNAAGC(boolean enabled)
    {
        mLNAAGC = enabled;
    }
}
