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
package io.github.dsheirer.source.tuner.rtl;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc0012.FC0012TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc0013.FC0013TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc2580.FC2580TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r8x.r820t.R820TTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r8x.r828d.R828DTunerConfiguration;

/**
 * RTL2832 tuner configuration
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = E4KTunerConfiguration.class, name = "e4KTunerConfiguration"),
        @JsonSubTypes.Type(value = FC0012TunerConfiguration.class, name = "fc0012TunerConfiguration"),
        @JsonSubTypes.Type(value = FC0013TunerConfiguration.class, name = "fc0013TunerConfiguration"),
        @JsonSubTypes.Type(value = FC2580TunerConfiguration.class, name = "fc2580TunerConfiguration"),
        @JsonSubTypes.Type(value = R820TTunerConfiguration.class, name = "r820TTunerConfiguration"),
        @JsonSubTypes.Type(value = R828DTunerConfiguration.class, name = "r828DTunerConfiguration"),
})
@JacksonXmlRootElement(localName = "tuner_configuration")
public abstract class RTL2832TunerConfiguration extends TunerConfiguration
{
    private RTL2832TunerController.SampleRate mSampleRate = RTL2832TunerController.SampleRate.RATE_2_400MHZ;
    private boolean mBiasTEnabled = false;
    private int mGain;
    private boolean mAGC = false;

    /**
     * Default constructor to support Jackson
     * @param minimumFrequency tunable
     * @param maximumFrequency tunable
     * @param defaultGain in tenths of a dB
     */
    public RTL2832TunerConfiguration(long minimumFrequency, long maximumFrequency, int defaultGain)
    {
        super(minimumFrequency, maximumFrequency);
        mGain = defaultGain;
    }

    /**
     * Constructs an instance
     * @param uniqueID for the tuner
     * @param defaultGain in tenths of a dB
     */
    public RTL2832TunerConfiguration(String uniqueID, int defaultGain)
    {
        super(uniqueID);
        mGain = defaultGain;
    }

    /**
     * Tuner gain, applied when AGC is off.
     * @return gain in tenths of a dB.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "gain")
    public int getGain()
    {
        return mGain;
    }

    /**
     * Sets the tuner gain, applied when AGC is off.
     * @param gain in tenths of a dB
     */
    public void setGain(int gain)
    {
        mGain = gain;
    }

    /**
     * Indicates if the tuner's automatic gain control is enabled.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "agc")
    public boolean isAGC()
    {
        return mAGC;
    }

    /**
     * Sets the enabled state of the tuner's automatic gain control.
     */
    public void setAGC(boolean agc)
    {
        mAGC = agc;
    }

    /**
     * Parses a gain value from the name of a gain enumeration entry used by legacy tuner configurations, for
     * example GAIN_327, GAIN_PLUS_200, GAIN_MINUS_50, PLUS_240 or MINUS_10.
     * @param name of the legacy enumeration entry
     * @return gain value, or null if the name doesn't contain a gain value.
     */
    protected static Integer parseLegacyGain(String name)
    {
        if(name != null)
        {
            String value = name.substring(name.lastIndexOf('_') + 1);

            try
            {
                int gain = Integer.parseInt(value);
                return name.contains("MINUS") ? -gain : gain;
            }
            catch(NumberFormatException nfe)
            {
                //Not a gain value
            }
        }

        return null;
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
     * Sets the enabled state of the Bias-T
     * @param enabled true to turn-on the bias-T
     */
    public void setBiasT(boolean enabled)
    {
        mBiasTEnabled = enabled;
    }

    /**
     * Indicates if the Bias-T is enabled.
     * @return true if enabled.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "bias_t")
    public boolean isBiasT()
    {
        return mBiasTEnabled;
    }
}
