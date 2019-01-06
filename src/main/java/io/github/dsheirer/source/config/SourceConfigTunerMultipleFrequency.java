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
    public TunerChannel getFirstTunerChannel(int bandwidth)
    {
        if(getFrequencies().size() > 0)
        {
            return new TunerChannel(getFrequencies().get(0), bandwidth);
        }
        else
        {
            return new TunerChannel(0l, bandwidth);
        }
    }
}
