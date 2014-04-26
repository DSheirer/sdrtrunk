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
package eventlog;

import java.nio.file.Path;

import properties.SystemProperties;
import controller.channel.ProcessingChain;
import eventlog.MessageEventLogger.Type;

public class EventLogManager
{
	private Path mDirectory;
	
	public EventLogManager()
	{
		mDirectory = SystemProperties.getInstance()
				.getApplicationFolder( "event_logs" );
	}
	
	public EventLogger getLogger( ProcessingChain chain, EventLogType eventLogType )
	{
		StringBuilder sb = new StringBuilder();

		sb.append( chain.getChannel().getSystem() );
		sb.append( "_" );
		sb.append( chain.getChannel().getSite() );
		sb.append( "_" );
		sb.append( chain.getChannel().getName() );
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
