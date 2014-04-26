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
package decode.mpt1327;

import java.util.Iterator;
import java.util.TreeSet;

import message.Message;
import sample.Listener;
import alias.Alias;
import alias.AliasList;
import controller.activity.ActivitySummaryProvider;

public class MPT1327ActivitySummary implements ActivitySummaryProvider, 
											   Listener<Message>
{
	private static final int sINT_NULL_VALUE = -1;

	private AliasList mAliasList;
	private TreeSet<String> mIdents = new TreeSet<String>();
	private int mSite;

	/**
	 * Compiles a summary of active talkgroups, unique radio ids encountered
	 * in the decoded messages.  Talkgroups have to be heard a minimum of twice
	 * to be considered valid ... in order to weed out the invalid ones.
	 * 
	 * Returns a textual summary of the activity
	 */
	public MPT1327ActivitySummary( AliasList list )
	{
		mAliasList = list;
	}
	
	/**
	 * Cleanup method
	 */
	public void dispose()
	{
		mIdents.clear();
	}

	@Override
    public void receive( Message message )
    {
		if( message instanceof MPT1327Message )
		{
			MPT1327Message mpt = ((MPT1327Message)message);
			
			if( mpt.isValid() )
			{
				switch( mpt.getMessageType() )
				{
					case ACK:
						mIdents.add( mpt.getFromID() );
						break;
					case ACKI:
						mIdents.add( mpt.getFromID() );
						mIdents.add( mpt.getToID() );
						break;
					case ALH:
						mSite = mpt.getSiteID();
						break;
					case GTC:
						if( mpt.hasFromID() )
						{
							mIdents.add( mpt.getFromID() );
						}
						if( mpt.hasToID() )
						{
							mIdents.add( mpt.getToID() );
						}
						break;
				}
			}
		}
    }

	@Override
    public String getSummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "Activity Summary\n" );
		sb.append( "Decoder:\tMPT-1327\n" );
		sb.append( "Site:\t" );
		
		if( mSite != 0 )
		{
			sb.append( mSite );
			
			if( mAliasList != null )
			{
				Alias siteAlias = mAliasList.getSiteID( mSite );
				
				if( siteAlias != null )
				{
					sb.append( " " );
					sb.append( siteAlias.getName() );
					sb.append( "\n" );
				}
			}
			
			sb.append( "Network:\t" );
			
			if( ( mSite & 0x4000 ) == 0x4000 )
			{
				sb.append( "National #" );
				int net = (int)Long.rotateRight( ( mSite & 0x3000 ) , 12 );
				sb.append( net );
				sb.append( "\n" );
			}
			else
			{
				sb.append( "Regional\n" );
			}
			
		}
		else
		{
			sb.append( "Unknown\n" );
		}
		
		sb.append( "\n" );
		
		sb.append( "Talkgroups\n" );
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
				
				Alias alias = mAliasList.getMPT1327Alias( ident );
				
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
