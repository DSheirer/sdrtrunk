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
package module.log;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import module.Module;
import module.log.MessageEventLogger.Type;
import module.log.config.EventLogConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import properties.SystemProperties;

public class EventLogManager
{
	private final static Logger mLog = LoggerFactory.getLogger( EventLogManager.class );

	private Path mDirectory;
	
	public EventLogManager()
	{
		mDirectory = SystemProperties.getInstance()
				.getApplicationFolder( "event_logs" );
	}

	public List<Module> getLoggers( EventLogConfiguration config, String prefix )
	{
		List<Module> loggers = new ArrayList<Module>();

		for( EventLogType type: config.getLoggers() )
		{
			loggers.add( getLogger( type, prefix ) );
		}

		return loggers;
	}
	
	public EventLogger getLogger( EventLogType eventLogType, String prefix )
	{
		StringBuilder sb = new StringBuilder();

		sb.append( prefix );
		sb.append( eventLogType.getFileSuffix() );
		sb.append( ".log" );

		switch( eventLogType )
		{
			case BINARY_MESSAGE:
				return new MessageEventLogger( mDirectory, sb.toString(), Type.BINARY );
			case DECODED_MESSAGE:
				return new MessageEventLogger( mDirectory, sb.toString(), Type.DECODED );
			case CALL_EVENT:
				return new CallEventLogger( mDirectory, sb.toString() );
			default:
				return null;
		}
	}
}
