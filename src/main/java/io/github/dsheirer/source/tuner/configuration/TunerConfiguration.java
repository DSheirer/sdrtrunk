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
package io.github.dsheirer.source.tuner.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerConfiguration;
import io.github.dsheirer.source.tuner.airspy.hf.AirspyHfTunerConfiguration;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerConfiguration;
import io.github.dsheirer.source.tuner.fcd.proplusV2.FCD2TunerConfiguration;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerConfiguration;
import io.github.dsheirer.source.tuner.recording.RecordingTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.fc0013.FC0013TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r8x.r820t.R820TTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r8x.r828d.R828DTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerConfiguration;

/**
 * Abstract class to hold a configuration for a specific type of tuner
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AirspyTunerConfiguration.class, name = "airspyTunerConfiguration"),
        @JsonSubTypes.Type(value = AirspyHfTunerConfiguration.class, name = "airspyHfTunerConfiguration"),
        @JsonSubTypes.Type(value = E4KTunerConfiguration.class, name = "e4KTunerConfiguration"),
        @JsonSubTypes.Type(value = FC0013TunerConfiguration.class, name = "fc0013TunerConfiguration"),
        @JsonSubTypes.Type(value = FCD1TunerConfiguration.class, name = "fcd1TunerConfiguration"),
        @JsonSubTypes.Type(value = FCD2TunerConfiguration.class, name = "fcd2TunerConfiguration"),
        @JsonSubTypes.Type(value = HackRFTunerConfiguration.class, name = "hackRFTunerConfiguration"),
        @JsonSubTypes.Type(value = RecordingTunerConfiguration.class, name = "recordingTunerConfiguration"),
        @JsonSubTypes.Type(value = R820TTunerConfiguration.class, name = "r820TTunerConfiguration"),
        @JsonSubTypes.Type(value = R828DTunerConfiguration.class, name = "r828DTunerConfiguration"),
        @JsonSubTypes.Type(value = RspTunerConfiguration.class, name = "rspTunerConfiguration"),
})
@JacksonXmlRootElement(localName = "tuner_configuration")
public abstract class TunerConfiguration
{
    public static final long DEFAULT_FREQUENCY = 101_100_000;
    private String mUniqueID;
    private long mFrequency = DEFAULT_FREQUENCY;
    private long mMinimumFrequency;
    private long mMaximumFrequency;
    private double mFrequencyCorrection = 0.0d;
    private boolean mAutoPPMCorrection = true;

    /**
     * Default constructor to support Jackson
     */
    public TunerConfiguration(long minimumFrequency, long maximumFrequency)
    {
        mMinimumFrequency = minimumFrequency;
        mMaximumFrequency = maximumFrequency;
    }

    /**
     * Identifies the tuner type for this configuration
     */
    @JsonIgnore
    public abstract TunerType getTunerType();

    /**
     * Normal constructor
     */
    public TunerConfiguration(String uniqueID)
    {
        mUniqueID = uniqueID;
    }

    public String toString()
    {
        return getTunerType() + " " + getUniqueID();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "unique_id")
    public String getUniqueID()
    {
        return mUniqueID;
    }

    public void setUniqueID(String id)
    {
        mUniqueID = id;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "frequency")
    public long getFrequency()
    {
        return mFrequency;
    }

    public void setFrequency(long frequency)
    {
        mFrequency = frequency;
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

    /**
     * Indicates if automatic correction of PPM from measured frequency error is enabled/disabled.
     *
     * @return true if autocorrection is enabled.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "auto_ppm_correction_enabled")
    public boolean getAutoPPMCorrectionEnabled()
    {
        return mAutoPPMCorrection;
    }

    /**
     * Sets the enabled state for auto-correction of PPM from measured frequency error values.
     *
     * @param enabled
     */
    public void setAutoPPMCorrectionEnabled(boolean enabled)
    {
        mAutoPPMCorrection = enabled;
    }

    /**
     * Minimum tunable frequency.
     * @return minimum frequency
     */
    @JacksonXmlProperty(isAttribute = true, localName = "min_frequency")
    public long getMinimumFrequency()
    {
        return mMinimumFrequency;
    }

    /**
     * Sets the minimum tunable frequency
     * @param minimumFrequency to set
     */
    public void setMinimumFrequency(long minimumFrequency)
    {
        mMinimumFrequency = minimumFrequency;
    }

    /**
     * Maximum tunable frequency.
     * @return maximum frequency
     */
    @JacksonXmlProperty(isAttribute = true, localName = "max_frequency")
    public long getMaximumFrequency()
    {
        return mMaximumFrequency;
    }

    /**
     * Sets the maximum tunable frequency
     * @param maximumFrequency to set
     */
    public void setMaximumFrequency(long maximumFrequency)
    {
        mMaximumFrequency = maximumFrequency;
    }
}
