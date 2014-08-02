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
package source.config;

import javax.xml.bind.annotation.XmlAttribute;

import source.SourceType;

public class SourceConfigRecording extends SourceConfiguration
{
	private String mRecordingAlias;
	private long mFrequency;
	
	public SourceConfigRecording()
    {
	    super( SourceType.RECORDING );
    }
	
	@XmlAttribute( name = "recording_alias" )
	public String getRecordingAlias()
	{
		return mRecordingAlias;
	}
	
	public void setRecordingAlias( String alias )
	{
		mRecordingAlias = alias;
	}

	@XmlAttribute( name = "frequency" )
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
}
