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

import message.Message;
import sample.Listener;
import util.TimeStamp;

public class MessageEventLogger extends EventLogger implements Listener<Message>
{
	public enum Type { BINARY, DECODED };

	private Type mType;
	
	public MessageEventLogger( Path logDirectory, String fileNameSuffix, Type type )
	{
		super( logDirectory, fileNameSuffix );
		
		mType = type;
	}
	
	@Override
    public void receive( Message message )
    {
		StringBuilder sb = new StringBuilder();
		sb.append( TimeStamp.getTimeStamp( " " ) );
		sb.append( "," );
		sb.append( ( message.isValid() ? "PASSED" : "FAILED" ) );
		sb.append( "," );
		
		if( mType == Type.BINARY )
		{
			sb.append( message.getBinaryMessage() );
		}
		else
		{
			sb.append( message.getMessage() );
		}
		
		write( sb.toString() );
    }

	@Override
    public String getHeader()
    {
	    return mType.toString() + " Message Logger\n";
    }
}
