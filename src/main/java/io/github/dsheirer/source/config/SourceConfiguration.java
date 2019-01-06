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
package io.github.dsheirer.source.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.controller.config.Configuration;
import io.github.dsheirer.source.SourceType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SourceConfigMixer.class, name = "sourceConfigMixer"),
    @JsonSubTypes.Type(value = SourceConfigNone.class, name = "sourceConfigNone"),
    @JsonSubTypes.Type(value = SourceConfigRecording.class, name = "sourceConfigRecording"),
    @JsonSubTypes.Type(value = SourceConfigTuner.class, name = "sourceConfigTuner"),
    @JsonSubTypes.Type(value = SourceConfigTunerMultipleFrequency.class, name = "sourceConfigTunerMultipleFrequency")})
@JacksonXmlRootElement(localName = "source_configuration")
public class SourceConfiguration extends Configuration
{
    protected SourceType mSourceType;

    public SourceConfiguration()
    {
        this(SourceType.NONE);
    }

    public SourceConfiguration(SourceType source)
    {
        mSourceType = source;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "source_type")
    public SourceType getSourceType()
    {
        return mSourceType;
    }

    public void setSourceType(SourceType source)
    {
        mSourceType = source;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    private String getType()
    {
        return null;
    }

    private void setType(String type)
    {
    }

    public String getDescription()
    {
        return "No Source";
    }
}
