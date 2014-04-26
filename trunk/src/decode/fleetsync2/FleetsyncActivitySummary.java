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
package decode.fleetsync2;

import java.util.Iterator;
import java.util.TreeSet;

import message.Message;
import sample.Listener;
import alias.Alias;
import alias.AliasList;
import controller.activity.ActivitySummaryProvider;

public class FleetsyncActivitySummary implements ActivitySummaryProvider, 
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
	public FleetsyncActivitySummary( AliasList list )
	{
		mAliasList = list;
	}
	
	public void dispose()
	{
		mIdents.clear();
		mEmergencyIdents.clear();
	}
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof FleetsyncMessage )
		{
			FleetsyncMessage fs = ((FleetsyncMessage)message);
			
			if( fs.isValid() )
			{
				String from = fs.getFromID();
				mIdents.add( from );
				
				String to = fs.getToID();
				
				if( !to.startsWith( "xxx" ))
				{
					mIdents.add( to );
				}
			}
			
			FleetsyncMessageType type = fs.getMessageType();
			
			switch( type )
			{
				case EMERGENCY:
				case LONE_WORKER_EMERGENCY:
					mEmergencyIdents.add( fs.getFromID() );
					break;
			}
		}
    }

	@Override
    public String getSummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "=============================\n" );
		sb.append( "Decoder:\tFleetsync II\n\n" );
		sb.append( "Fleetsync Idents\n" );

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
				sb.append( formatIdent( ident ) );
				sb.append( " " );
				
				Alias alias = mAliasList.getFleetsyncAlias( ident );
				
				if( alias != null )
				{
					sb.append( alias.getName() );
				}
				
				sb.append( "\n" );
			}
		}
		
		sb.append( "\nFleetsync Emergency Activations\n" );

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
				sb.append( formatIdent( ident ) );
				sb.append( " " );
				
				Alias alias = mAliasList.getFleetsyncAlias( ident );
				
				if( alias != null )
				{
					sb.append( alias.getName() );
				}
				
				sb.append( "\n" );
			}
		}

		return sb.toString();
    }

	public static String formatIdent( String ident )
	{
		StringBuilder sb = new StringBuilder();
		
		if( ident.length() == 7 )
		{
			sb.append( ident.substring( 0, 3 ) );
			sb.append( "-" );
			sb.append( ident.substring( 3, 7 ) );

			return sb.toString();
		}
		else
		{
			return ident;
		}
	}
}
