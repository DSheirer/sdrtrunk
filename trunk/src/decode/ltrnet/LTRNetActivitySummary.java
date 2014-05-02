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
package decode.ltrnet;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import message.Message;
import alias.Alias;
import alias.AliasList;
import controller.activity.ActivitySummaryProvider;

public class LTRNetActivitySummary implements ActivitySummaryProvider
{
	private static final int sINT_NULL_VALUE = -1;
	private DecimalFormat mDecimalFormatter = new DecimalFormat("0.00000");

	private AliasList mAliasList;
	private HashSet<String> mTalkgroupsFirstHeard = new HashSet<String>();
	private TreeSet<String> mTalkgroups = new TreeSet<String>();
	private TreeSet<String> mESNs = new TreeSet<String>();
	private TreeSet<Integer> mUniqueIDs = new TreeSet<Integer>();
	private TreeSet<Integer> mNeighborIDs = new TreeSet<Integer>();
	private TreeSet<Integer> mSiteIDs = new TreeSet<Integer>();
	private HashMap<Integer,Double> mReceiveFrequencies = 
			new HashMap<Integer,Double>();
	private HashMap<Integer,Double> mTransmitFrequencies = 
			new HashMap<Integer,Double>();
	private int mMonitoredLCN;

	/**
	 * Compiles a summary of active talkgroups, unique radio ids encountered
	 * in the decoded messages.  Talkgroups have to be heard a minimum of twice
	 * to be considered valid ... in order to weed out the invalid ones.
	 * 
	 * Returns a textual summary of the activity
	 */
	public LTRNetActivitySummary( AliasList list )
	{
		mAliasList = list;
	}
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof LTRNetOSWMessage )
		{
			LTRNetOSWMessage ltr = ((LTRNetOSWMessage)message);
			
			if( ltr.isValid() )
			{
				switch( ltr.getMessageType() )
				{
					case CA_STRT:
					case CA_ENDD:
						String talkgroup = ltr.getTalkgroupID();
						
						if( mTalkgroupsFirstHeard.contains( talkgroup ) )
						{
							mTalkgroups.add( talkgroup );
						}
						else
						{
							mTalkgroupsFirstHeard.add( talkgroup );
						}
						break;
					case SY_IDLE:
						mMonitoredLCN = ltr.getChannel();
						break;
					case MA_CHNH:
						break;
					case MA_CHNL:
						break;
					case FQ_RXHI:
						mReceiveFrequencies.put( ltr.getHomeRepeater(), 
								ltr.getFrequency() );
						break;
					case FQ_RXLO:
						break;
					case FQ_TXHI:
						mTransmitFrequencies.put( ltr.getHomeRepeater(), 
								ltr.getFrequency() );
						break;
					case FQ_TXLO:
						break;
					case ID_ESNH:
						break;
					case ID_ESNL:
						break;
					case ID_NBOR:
						int neighborID = ltr.getNeighborID();
						
						if( neighborID != sINT_NULL_VALUE )
						{
							mNeighborIDs.add( neighborID );
						}
						break;
					case ID_UNIQ:
						int uniqueid = ltr.getRadioUniqueID();
						
						if( uniqueid != sINT_NULL_VALUE )
						{
							mUniqueIDs.add( uniqueid );
						}
						
						break;
					case ID_SITE:
						int siteID = ltr.getSiteID();
						
						if( siteID != sINT_NULL_VALUE )
						{
							mSiteIDs.add( siteID );
						}
						break;
					case UN_KNWN:
						break;
					default:
						break;
				}
			}
		}
		else if( message instanceof LTRNetISWMessage )
		{
			LTRNetISWMessage ltr = ((LTRNetISWMessage)message);
			
			if( ltr.isValid() )
			{
				switch( ltr.getMessageType() )
				{
					case CA_STRT:
					case CA_ENDD:
						String talkgroup = ltr.getTalkgroupID();
						
						if( mTalkgroupsFirstHeard.contains( talkgroup ) )
						{
							mTalkgroups.add( talkgroup );
						}
						else
						{
							mTalkgroupsFirstHeard.add( talkgroup );
						}
						break;
					case ID_ESNH:
					case ID_ESNL:
						String esn = ltr.getESN();
						
						if( !esn.contains( "xxxx" ) )
						{
							mESNs.add( ltr.getESN() );
						}
 						break;
					case ID_UNIQ:
						int uniqueid = ltr.getRadioUniqueID();
						
						if( uniqueid != sINT_NULL_VALUE )
						{
							mUniqueIDs.add( uniqueid );
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
		sb.append( "Decoder:\tLTR-Net\n\n" );

		if( mSiteIDs.isEmpty() )
		{
			sb.append( "Site:\tUnknown\n" );
		}
		else
		{
			Iterator<Integer> it = mSiteIDs.iterator();
			
			while( it.hasNext() )
			{
				sb.append( "Site:\t" );
				
				int siteID = it.next();

				sb.append( siteID );
				
				Alias siteAlias = mAliasList.getSiteID( siteID );

				if( siteAlias != null )
				{
					sb.append( " " );
					sb.append( siteAlias.getName() );
				}

				sb.append( "\n" );
			}
		}
		
		sb.append( "\nLCNs (transmit | receive)\n" );
		
		if( mReceiveFrequencies.isEmpty() && mTransmitFrequencies.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			for( int x = 1; x < 21; x++ )
			{
				double rcv = 0.0d;
				
				if( mReceiveFrequencies.containsKey( x ) )
				{
					rcv = mReceiveFrequencies.get( x );
				}
				
				double xmt = 0.0d;
				
				if( mTransmitFrequencies.containsKey( x ) )
				{
					xmt = mTransmitFrequencies.get( x );
				}

				if( rcv > 0.0d || xmt > 0.0d )
				{
					if( x < 10 )
					{
						sb.append( " " );
					}

					sb.append( x );
					sb.append( ": " );

					if( xmt == -1.0d )
					{
						sb.append( "---.-----" );
					}
					else
					{
						sb.append( mDecimalFormatter.format( xmt ) );
					}
					sb.append( " | " );
					
					if( rcv == -1.0d )
					{
						sb.append( "---.-----" );
					}
					else
					{
						sb.append( mDecimalFormatter.format( rcv ) );
					}
					
					if( x == mMonitoredLCN )
					{
						sb.append( " **" );
					}
					
					sb.append( "\n" );
				}
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
		
		sb.append( "\nRadio Unique IDs\n" );
		
		if( mUniqueIDs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<Integer> it = mUniqueIDs.iterator();
			
			while( it.hasNext() )
			{
				int uid = it.next();
				
				sb.append( "  " );
				sb.append( uid );
				sb.append( " " );
				
				Alias alias = mAliasList.getUniqueID( uid );
				
				if( alias != null )
				{
					sb.append( alias.getName() );
				}
				
				sb.append( "\n" );
			}
		}

		sb.append( "\nESNs\n" );
		
		if( mESNs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mESNs.iterator();
			
			while( it.hasNext() )
			{
				String esn = it.next();
				
				sb.append( "  " );
				sb.append( esn );
				sb.append( " " );
				
				Alias alias = mAliasList.getESNAlias( esn );
				
				if( alias != null )
				{
					sb.append( alias.getName() );
				}
				
				sb.append( "\n" );
			}
		}
		
		sb.append( "\nNeighbor Sites\n" );
		
		if( mNeighborIDs.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<Integer> it = mNeighborIDs.iterator();
			
			while( it.hasNext() )
			{
				int neighbor = it.next();
				
				sb.append( "  " );
				sb.append( neighbor );
				sb.append( " " );
				
				Alias alias = mAliasList.getSiteID( neighbor );
				
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
