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
package decode.passport;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import message.Message;
import sample.Listener;
import alias.Alias;
import alias.AliasList;
import controller.activity.ActivitySummaryProvider;

public class PassportActivitySummary implements ActivitySummaryProvider, 
  												Listener<Message>
{
	private static final int sINT_NULL_VALUE = -1;
	private DecimalFormat mDecimalFormatter = new DecimalFormat("0.00000");

	private AliasList mAliasList;
	private HashSet<String> mTalkgroupsFirstHeard = new HashSet<String>();
	private TreeSet<String> mTalkgroups = new TreeSet<String>();
	private TreeSet<String> mMobileIDs = new TreeSet<String>();
	private HashMap<Integer,String> mSiteLCNs = new HashMap<Integer,String>(); 
	private HashMap<Integer,String> mNeighborLCNs = new HashMap<Integer,String>(); 

	/**
	 * Compiles a summary of active talkgroups, unique radio ids encountered
	 * in the decoded messages.  Talkgroups have to be heard a minimum of twice
	 * to be considered valid ... in order to weed out the invalid ones.
	 * 
	 * Returns a textual summary of the activity
	 */
	public PassportActivitySummary( AliasList list )
	{
		mAliasList = list;
	}
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof PassportMessage )
		{
			PassportMessage passport = ((PassportMessage)message);
			
			if( passport.isValid() )
			{
				switch( passport.getMessageType() )
				{
					case CA_STRT:
						mSiteLCNs.put( passport.getLCN(), 
									   passport.getLCNFrequencyFormatted() );
					case CA_ENDD:
						String talkgroup = 
							String.valueOf( passport.getTalkgroupID() );
						
						if( mTalkgroupsFirstHeard.contains( talkgroup ) )
						{
							mTalkgroups.add( talkgroup );
						}
						else
						{
							mTalkgroupsFirstHeard.add( talkgroup );
						}
						break;
					case ID_RDIO:
						String min = passport.getMobileID();
						
						if( min != null )
						{
							mMobileIDs.add( min );
						}
						break;
					case SY_IDLE:
						if( passport.getFree() != 0 )
						{
							mNeighborLCNs.put( passport.getFree(), 
									   passport.getFreeFrequencyFormatted() );
						}
						break;
					default:
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
		sb.append( "Decoder:\tPassport\n\n" );
		sb.append( "Site Channels\n" );

		if( mSiteLCNs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			ArrayList<Integer> channels = new ArrayList( mSiteLCNs.keySet() );
			Collections.sort( channels );
			
			for( Integer channel: channels )
			{
				sb.append( "  " + channel );
				sb.append( "\t" + mSiteLCNs.get( channel ) );
				sb.append( "\n" );
			}
		}
		
		sb.append( "\nNeighbor Channels\n" );

		if( mNeighborLCNs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			ArrayList<Integer> channels = new ArrayList( mNeighborLCNs.keySet() );
			Collections.sort( channels );
			
			for( Integer channel: channels )
			{
				sb.append( "  " + channel );
				sb.append( "\t" + mNeighborLCNs.get( channel ) );
				sb.append( "\n" );
			}
		}
		
		sb.append( "\nTalkgroups\n" );
		
		if( mTalkgroups.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mTalkgroups.iterator();
			
			while( it.hasNext() )
			{
				String tgid = it.next();
				
				sb.append( "  " );
				sb.append( tgid );
				sb.append( " " );
				
				Alias alias = mAliasList.getTalkgroupAlias( tgid );
				
				if( alias != null )
				{
					sb.append( alias.getName() );
				}
				
				sb.append( "\n" );
			}
		}
		
		sb.append( "\nMobile ID Numbers\n" );
		
		if( mMobileIDs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mMobileIDs.iterator();
			
			while( it.hasNext() )
			{
				String min = it.next();
				
				sb.append( "  " );
				sb.append( min );
				sb.append( " " );
				
				Alias alias = mAliasList.getMobileIDNumberAlias( min );
				
				if( alias != null )
				{
					sb.append( alias.getName() );
				}
				
				sb.append( "\n" );
			}
		}
		
	    return sb.toString();
    }
	
	public static String formatTalkgroup( String talkgroup )
	{
		StringBuilder sb = new StringBuilder();
		
		if( talkgroup.length() == 6 )
		{
			sb.append( talkgroup.substring( 0, 1 ) );
			sb.append( "-" );
			sb.append( talkgroup.substring( 1, 3 ) );
			sb.append( "-" );
			sb.append( talkgroup.substring( 3, 6 ) );

			return sb.toString();
		}
		else
		{
			return talkgroup;
		}
	}
}
