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
package io.github.dsheirer.record.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.controller.config.Configuration;
import io.github.dsheirer.record.RecorderType;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the types of recordings specified for a channel
 */
public class RecordConfiguration extends Configuration
{
    /**
     * Recording types requested for this configuration
     */
    private List<RecorderType> mRecorders = new ArrayList<>();

    /**
     * Constructs a recording configuration instance
     */
    public RecordConfiguration()
    {
        //Empty constructor required for deserialization
    }

    /**
     * List of recorder types specified in this configuration
     */
    @JacksonXmlProperty(isAttribute = false, localName = "recorder")
    public List<RecorderType> getRecorders()
    {
        return mRecorders;
    }

    /**
     * Sets the (complete) list of recorder types for this configuration, erasing any existing recording types.
     */
    public void setRecorders(List<RecorderType> recorders)
    {
        mRecorders = recorders;
    }

    /**
     * Adds the recorder type to the configuration
     */
    public void addRecorder(RecorderType recorder)
    {
        mRecorders.add(recorder);
    }

    /**
     * Clears all recorder types from this configuration
     */
    public void clearRecorders()
    {
        mRecorders.clear();
    }

    /**
     * Indicates if this configuration has the specified recorder type
     * @param recorderType to check
     * @return true if this configuration contains the specified recorder type
     */
    public boolean contains(RecorderType recorderType)
    {
        return mRecorders.contains(recorderType);
    }
}
