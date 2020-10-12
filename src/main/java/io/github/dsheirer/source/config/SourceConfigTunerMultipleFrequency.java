/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.source.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.SourceType;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.rotation.ChannelRotationMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Multiple frequency tuner source configuration is used for a system with a rolling/rotating control
 * channel where the control channel rotates across a pre-defined set of channel frequencies.
 */
@JsonSubTypes.Type(value = SourceConfigTunerMultipleFrequency.class, name = "sourceConfigTunerMultipleFrequency")
public class SourceConfigTunerMultipleFrequency extends SourceConfiguration
{
    private List<Long> mFrequencies = new ArrayList<>();
    private String mPreferredTuner;
    private Long mPreferredFrequency;
    private int mFrequencyRotationDelay = ChannelRotationMonitor.CHANNEL_ROTATION_DELAY_MINIMUM;

    public SourceConfigTunerMultipleFrequency()
    {
        super(SourceType.TUNER_MULTIPLE_FREQUENCIES);
    }

    /**
     * List of frequencies for this configuration
     */
    @JacksonXmlProperty(isAttribute = false, localName = "frequency")
    public List<Long> getFrequencies()
    {
        return mFrequencies;
    }

    /**
     * Sets the list of frequencies for this configuration
     */
    public void setFrequencies(List<Long> frequencies)
    {
        mFrequencies = frequencies;
    }

    /**
     * Indicates if this configuration has more than one frequency specified
     */
    @JsonIgnore
    public boolean hasMultipleFrequencies()
    {
        return mFrequencies.size() > 1;
    }

    /**
     * Preferred frequency to start first for this configuration
     */
    @JsonIgnore
    public long getPreferredFrequency()
    {
        if(mPreferredFrequency != null)
        {
            return mPreferredFrequency;
        }
        else if(mFrequencies.size() > 0)
        {
            return mFrequencies.get(0);
        }

        return 0l;
    }

    /**
     * Sets which is the preferred frequency to use for this config when it's started
     * @param frequency to use first
     */
    public void setPreferredFrequency(long frequency)
    {
        //Only set the frequency if it is one of the frequencies in the list
        if(mFrequencies.contains(frequency))
        {
            mPreferredFrequency = frequency;
        }
    }

    /**
     * Adds a frequency to the list
     */
    public void addFrequency(long frequency)
    {
        mFrequencies.add(frequency);
    }

    /**
     * Preferred tuner to use for this configuration.
     * @return tuner name or null
     */
    @JacksonXmlProperty(isAttribute = true, localName = "preferred_tuner")
    public String getPreferredTuner()
    {
        return mPreferredTuner;
    }

    /**
     * Indicates if this configuration has a specified preferred tuner
     */
    @JsonIgnore
    public boolean hasPreferredTuner()
    {
        return mPreferredTuner != null;
    }

    /**
     * Specifies the preferred tuner to use for this configuration
     * @param preferredTuner to use, if available
     */
    public void setPreferredTuner(String preferredTuner)
    {
        mPreferredTuner = preferredTuner;
    }

    @JsonIgnore
    @Override
    public String getDescription()
    {
        return "Frequencies [" + mFrequencies.size() + "]";
    }

    @JsonIgnore
    public TunerChannel getTunerChannel(int bandwidth)
    {
        return new TunerChannel(getPreferredFrequency(), bandwidth);
    }

    /**
     * Channel rotation delay.  This setting is used when multiple channel frequencies are defined in the source
     * config and controls how long the decoder will remaining on each frequency until the channel is identified as
     * active or the channel is identified as inactive and a change frequency request is issued.
     *
     * Note: this value is ignored when the source config contains a single frequency.
     *
     * @return channel rotation delay in milliseconds.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "frequency_rotation_delay")
    public int getFrequencyRotationDelay()
    {
        return mFrequencyRotationDelay;
    }

    /**
     * Sets the channel rotation delay.
     * @param frequencyRotationDelay in milliseconds.
     */
    public void setFrequencyRotationDelay(int frequencyRotationDelay)
    {
        if(frequencyRotationDelay < ChannelRotationMonitor.CHANNEL_ROTATION_DELAY_MINIMUM)
        {
            mFrequencyRotationDelay = ChannelRotationMonitor.CHANNEL_ROTATION_DELAY_MINIMUM;
        }
        else if(frequencyRotationDelay > ChannelRotationMonitor.CHANNEL_ROTATION_DELAY_MAXIMUM)
        {
            mFrequencyRotationDelay = ChannelRotationMonitor.CHANNEL_ROTATION_DELAY_MAXIMUM;
        }
        else
        {
            mFrequencyRotationDelay = frequencyRotationDelay;
        }
    }
}
