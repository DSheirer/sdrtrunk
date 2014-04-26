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
package controller.config;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import record.config.RecordConfiguration;
import source.config.SourceConfiguration;
import decode.config.AuxDecodeConfiguration;
import decode.config.DecodeConfiguration;
import eventlog.config.EventLogConfiguration;

@XmlSeeAlso( { AuxDecodeConfiguration.class,
			   DecodeConfiguration.class,
			   EventLogConfiguration.class,
			   RecordConfiguration.class,
			   SourceConfiguration.class } )
@XmlRootElement( name = "configuration" )
public abstract class Configuration
{
	public Configuration()
	{
	}
}
