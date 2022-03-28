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

package io.github.dsheirer.source.recording;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.io.File;

@JacksonXmlRootElement(localName = "recording_configuration")
@Deprecated //No longer used -- retained for backward compatibility with existing user configuration files.
public class RecordingConfiguration
{
    private File mRecording;
    private String mFilePath;
    private String mAlias;
    private long mCenterFrequency;

    public RecordingConfiguration()
    {
        mAlias = "New Recording";
    }

    public RecordingConfiguration(String filePath, String alias, long centerFrequency)
    {
        mFilePath = filePath;
        mAlias = alias;
        mCenterFrequency = centerFrequency;
    }

    @JsonIgnore
    public File getRecording()
    {
        return mRecording;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "file_path")
    public String getFilePath()
    {
        return mFilePath;
    }

    public void setFilePath(String filePath)
    {
        mFilePath = filePath;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "alias")
    public String getAlias()
    {
        return mAlias;
    }

    public void setAlias(String alias)
    {
        mAlias = alias;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "center_frequency")
    public long getCenterFrequency()
    {
        return mCenterFrequency;
    }

    public void setCenterFrequency(long frequency)
    {
        mCenterFrequency = frequency;
    }
}
