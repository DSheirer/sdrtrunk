/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.signalhound;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;

/**
 * Signal Hound tuner configuration, for devices of all Signal Hound families.
 */
public class SignalHoundTunerConfiguration extends TunerConfiguration
{
    private int mSampleRate = SignalHoundSampleRate.DEFAULT_RATE;
    private int mReferenceLevel = SignalHoundTunerController.REFERENCE_LEVEL_DEFAULT;
    private int mAttenuation = SignalHoundTunerController.AUTOMATIC;
    private int mGain = SignalHoundTunerController.AUTOMATIC;
    private boolean mPreselector = false;

    /**
     * Default constructor for Jackson
     */
    public SignalHoundTunerConfiguration()
    {
        super(SignalHoundTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ,
                SignalHoundTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    /**
     * Constructs an instance
     * @param uniqueID for the tuner
     */
    public SignalHoundTunerConfiguration(String uniqueID)
    {
        super(uniqueID);
    }

    @Override
    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public TunerType getTunerType()
    {
        return TunerType.SIGNAL_HOUND;
    }

    /**
     * Sample rate in Hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "sample_rate")
    public int getSampleRate()
    {
        return mSampleRate;
    }

    public void setSampleRate(int sampleRate)
    {
        mSampleRate = sampleRate;
    }

    /**
     * Reference level in dBm: the strongest expected input level, which sets the gain and attenuation automatically.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "reference_level")
    public int getReferenceLevel()
    {
        return mReferenceLevel;
    }

    public void setReferenceLevel(int referenceLevel)
    {
        mReferenceLevel = referenceLevel;
    }

    /**
     * Attenuator setting, or -1 for automatic.  The step size depends on the device family.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "attenuation")
    public int getAttenuation()
    {
        return mAttenuation;
    }

    public void setAttenuation(int attenuation)
    {
        mAttenuation = attenuation;
    }

    /**
     * Gain setting (BB60 only), or -1 for automatic.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "gain")
    public int getGain()
    {
        return mGain;
    }

    public void setGain(int gain)
    {
        mGain = gain;
    }

    /**
     * RF preselector (SM200/SM435 and SP145B) enabled.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "preselector")
    public boolean isPreselector()
    {
        return mPreselector;
    }

    public void setPreselector(boolean preselector)
    {
        mPreselector = preselector;
    }
}
