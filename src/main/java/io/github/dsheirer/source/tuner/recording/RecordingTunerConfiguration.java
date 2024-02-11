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

package io.github.dsheirer.source.tuner.recording;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recording tuner configuration for using baseband I/Q recordings as a tuner source
 */
public class RecordingTunerConfiguration extends TunerConfiguration
{
    private static final Logger mLog = LoggerFactory.getLogger(RecordingTunerConfiguration.class);
    private String mPath;

    /**
     * Jackson constructor
     */
    public RecordingTunerConfiguration()
    {
        super(0, Long.MAX_VALUE);
    }

    /**
     * Constructs an instance
     * @param uniqueId to use for this configuration
     */
    public RecordingTunerConfiguration(String uniqueId)
    {
        this();
        setUniqueID(uniqueId);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.RECORDING;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "path")
    public String getPath()
    {
        return mPath;
    }

    public void setPath(String path)
    {
        mPath = path;
    }

    public static RecordingTunerConfiguration create()
    {
        return new RecordingTunerConfiguration("Recording " + System.currentTimeMillis());
    }
}
