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
package decode.tait;

import message.Message;
import sample.Listener;
import alias.AliasList;
import controller.activity.ActivitySummaryProvider;

public class Tait1200ActivitySummary implements ActivitySummaryProvider, 
												 Listener<Message>
{
	private AliasList mAliasList;

	public Tait1200ActivitySummary( AliasList list )
	{
		mAliasList = list;
	}
	
	public void dispose()
	{
	}
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof Tait1200GPSMessage )
		{
			Tait1200GPSMessage tait = ((Tait1200GPSMessage)message);
			
			//Do something here in the future
		}
    }

	@Override
    public String getSummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "=============================\n" );
		sb.append( "Decoder:\tTait-1200I\n\n" );

		return sb.toString();
    }
}
