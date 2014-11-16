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
package decode.lj1200;

import message.Message;
import sample.Listener;
import alias.AliasList;
import controller.activity.ActivitySummaryProvider;

public class LJ1200ActivitySummary implements ActivitySummaryProvider, 
												 Listener<Message>
{
	private AliasList mAliasList;

	public LJ1200ActivitySummary( AliasList list )
	{
		mAliasList = list;
	}
	
	public void dispose()
	{
	}
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof LJ1200Message )
		{
			LJ1200Message lj = ((LJ1200Message)message);
			
			//Do something here in the future
		}
    }

	@Override
    public String getSummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "=============================\n" );
		sb.append( "Decoder:\tLJ-1200I\n\n" );

		return sb.toString();
    }
}
