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
package decode.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import decode.DecoderType;
import decode.mpt1327.MPT1327Decoder.Sync;

public class DecodeConfigMPT1327 extends DecodeConfiguration
{
	private String mChannelMapName;
	private Sync mSync = Sync.NORMAL;
	
	public DecodeConfigMPT1327()
    {
	    super( DecoderType.MPT1327 );
    }

	@XmlElement
	public String getChannelMapName()
	{
		return mChannelMapName;
	}
	
	public void setChannelMapName( String name )
	{
		mChannelMapName = name;
	}

	@XmlAttribute( name="sync" )
	public Sync getSync()
	{
		return mSync;
	}
	
	public void setSync( Sync sync )
	{
		mSync = sync;
	}
}
