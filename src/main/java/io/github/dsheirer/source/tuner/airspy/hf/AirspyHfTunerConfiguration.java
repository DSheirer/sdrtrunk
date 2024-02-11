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
package io.github.dsheirer.source.tuner.airspy.hf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;

/**
 * Tuner configuration for Airspy HF+ & Discovery tuners
 */
public class AirspyHfTunerConfiguration extends TunerConfiguration
{
    private int mSampleRate;
    private boolean mAgc;
    private boolean mLna;
    private int mAttenuation;

    /**
     * Default constructor for JAXB
     */
    public AirspyHfTunerConfiguration()
    {
        super(AirspyHfTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ, AirspyHfTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    /**
     * Constructs an instance for the unique ID.
     * @param uniqueID of the tuner (ie serial number)
     */
    public AirspyHfTunerConfiguration(String uniqueID)
    {
        super(uniqueID);
    }

    @Override
    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public TunerType getTunerType()
    {
        return TunerType.AIRSPY_HF_PLUS;
    }

    /**
     * Saved sample rate setting
     * @return sample rate in Hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "sample_rate")
    public int getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Sets the sample rate
     * @param sampleRate in Hertz
     */
    public void setSampleRate(int sampleRate)
    {
        mSampleRate = sampleRate;
    }

    /**
     * Indicates if the AGC is enabled
     * @return AGC enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "agc")
    public boolean isAgc()
    {
        return mAgc;
    }

    /**
     * Sets the AGC enabled state.
     * @param agc enabled
     */
    public void setAgc(boolean agc)
    {
        mAgc = agc;
    }

    /**
     * Indicates if the LNA is enabled
     * @return LNA enabled state
     */
    @JacksonXmlProperty(isAttribute = true, localName = "lna")
    public boolean isLna()
    {
        return mLna;
    }

    /**
     * Sets the LNA enabled state
     * @param lna enabled
     */
    public void setLna(boolean lna)
    {
        mLna = lna;
    }

    /**
     * Attenuation setting.
     * @return attenuation setting index value.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "attenuation")
    public int getAttenuationValue()
    {
        return mAttenuation;
    }

    /**
     * Sets the attenuation index value.
     * @param attenuation
     */
    public void setAttenuationValue(int attenuation)
    {
        mAttenuation = attenuation;
    }

    /**
     * Attenuation setting
     * @return attenuation
     */
    @JsonIgnore
    public Attenuation getAttenuation()
    {
        return Attenuation.fromValue(getAttenuationValue());
    }

    /**
     * Sets the attentuation
     * @param attenuation to set
     */
    @JsonIgnore
    public void setAttenuation(Attenuation attenuation)
    {
        setAttenuationValue(attenuation.getValue());
    }
}
