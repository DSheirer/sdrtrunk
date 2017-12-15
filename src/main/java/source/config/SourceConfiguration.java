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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import source.SourceType;
import controller.config.Configuration;

@XmlSeeAlso( { SourceConfigMixer.class, SourceConfigNone.class,
					SourceConfigTuner.class } )
@XmlRootElement( name = "source_configuration" )
public class SourceConfiguration extends Configuration
{
	protected SourceType mSourceType;

	public SourceConfiguration()
	{
		this( SourceType.NONE );
	}
	
	public SourceConfiguration( SourceType source )
	{
		mSourceType = source;
	}

	@XmlAttribute( name = "source_type" )
	public SourceType getSourceType()
	{
		return mSourceType;
	}
	
	public void setSourceType( SourceType source )
	{
		mSourceType = source;
	}
	
	public String getDescription()
	{
		return "No Source";
	}
}
