/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.SourceType;
import io.github.dsheirer.source.tuner.channel.TunerChannel;

public class SourceConfigRecording extends SourceConfiguration
{
	private String mRecordingAlias;
	private long mFrequency;
	
	public SourceConfigRecording()
    {
	    super( SourceType.RECORDING );
    }

	@JacksonXmlProperty(isAttribute=true,localName= "recording_alias" )
	public String getRecordingAlias()
	{
		return mRecordingAlias;
	}
	
	public void setRecordingAlias( String alias )
	{
		mRecordingAlias = alias;
	}

	@JacksonXmlProperty(isAttribute=true,localName= "frequency" )
	public long getFrequency()
	{
		return mFrequency;
	}
	
	public void setFrequency( long frequency )
	{
		mFrequency = frequency;
	}

	@Override
    public String getDescription()
    {
	    return getRecordingAlias();
    }

    public TunerChannel getTunerChannel()
    {
        /* Create a tuner channel with a default bandwidth of 12.5 kHz */
        return new TunerChannel(mFrequency, 12500);
    }

}
