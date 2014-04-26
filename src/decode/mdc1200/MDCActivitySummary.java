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
package decode.mdc1200;

import java.util.Iterator;
import java.util.TreeSet;

import message.Message;
import sample.Listener;
import alias.Alias;
import alias.AliasList;
import controller.activity.ActivitySummaryProvider;

public class MDCActivitySummary implements ActivitySummaryProvider, 
										   Listener<Message>
{
	private AliasList mAliasList;
	private TreeSet<String> mIdents = new TreeSet<String>();
	private TreeSet<String> mEmergencyIdents = new TreeSet<String>();

	/**
	 * Compiles a summary of active talkgroups, unique radio ids encountered
	 * in the decoded messages.  Talkgroups have to be heard a minimum of twice
	 * to be considered valid ... in order to weed out the invalid ones.
	 * 
	 * Returns a textual summary of the activity
	 */
	public MDCActivitySummary( AliasList list )
	{
		mAliasList = list;
	}
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof MDCMessage )
		{
			MDCMessage mdc = ((MDCMessage)message);
			
			mIdents.add( mdc.getUnitID() );
			
			if( mdc.isEmergency() )
			{
				mEmergencyIdents.add( mdc.getUnitID() );
			}
		}
    }

	@Override
    public String getSummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "=============================\n" );
		sb.append( "Decoder:\tMDC-1200\n\n" );
		sb.append( "MDC-1200 Idents\n" );

		if( mIdents.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mIdents.iterator();
			
			while( it.hasNext() )
			{
				String ident = it.next();
				
				sb.append( "  " );
				sb.append( ident );
				sb.append( " " );
				
				Alias alias = mAliasList.getMDC1200Alias( ident );
				
				if( alias != null )
				{
					sb.append( alias.getName() );
				}
				
				sb.append( "\n" );
			}
		}

		sb.append( "MDC-1200 Emergency Idents\n" );

		if( mEmergencyIdents.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mEmergencyIdents.iterator();
			
			while( it.hasNext() )
			{
				String ident = it.next();
				
				sb.append( "  " );
				sb.append( ident );
				sb.append( " " );
				
				Alias alias = mAliasList.getMDC1200Alias( ident );
				
				if( alias != null )
				{
					sb.append( alias.getName() );
				}
				
				sb.append( "\n" );
			}
		}

		return sb.toString();
    }
}
