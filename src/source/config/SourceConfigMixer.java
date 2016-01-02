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
import source.mixer.MixerChannel;

public class SourceConfigMixer extends SourceConfiguration
{
	protected String mMixer;
	protected MixerChannel mChannel = MixerChannel.LEFT; //default
	
	public SourceConfigMixer()
    {
	    super( SourceType.MIXER );
    }
	
	@Override
    public String getDescription()
    {
	    return getName();
    }

	public String getName()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "Mixer: " );
		sb.append( mMixer );
		sb.append( "-" );
		sb.append( mChannel.toString() );
		
		return sb.toString();
	}
	
	@XmlAttribute( name= "mixer" )
	public String getMixer()
	{
		return mMixer;
	}
	
	public void setMixer( String mixer )
	{
		mMixer = mixer;
	}

	@XmlAttribute( name = "channel" )
	public MixerChannel getChannel()
	{
		return mChannel;
	}
	
	public void setChannel( MixerChannel channel )
	{
		mChannel = channel;
	}
}
