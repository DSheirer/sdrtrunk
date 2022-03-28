/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
import io.github.dsheirer.source.tuner.rtl.r820t.R820TTunerConfiguration;

/**
 * RTL2832 tuner configuration
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = E4KTunerConfiguration.class, name = "e4KTunerConfiguration"),
        @JsonSubTypes.Type(value = R820TTunerConfiguration.class, name = "r820TTunerConfiguration"),
})
@JacksonXmlRootElement(localName = "tuner_configuration")
public abstract class RTL2832TunerConfiguration extends TunerConfiguration
{
    private RTL2832TunerController.SampleRate mSampleRate = RTL2832TunerController.SampleRate.RATE_2_400MHZ;

    /**
     * Default constructor to support Jackson
     */
    public RTL2832TunerConfiguration()
    {
    }

    public RTL2832TunerConfiguration(String uniqueID)
    {
        super(uniqueID);
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
