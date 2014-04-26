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
package decode.ltrstandard;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import message.Message;
import sample.Listener;
import alias.Alias;
import alias.AliasList;
import controller.activity.ActivitySummaryProvider;

public class LTRStandardActivitySummary implements ActivitySummaryProvider, 
												   Listener<Message>
{
	private static final int sINT_NULL_VALUE = -1;
	private DecimalFormat mDecimalFormatter = new DecimalFormat("0.00000");

	private AliasList mAliasList;
	private HashSet<String> mTalkgroupsFirstHeard = new HashSet<String>();
	private TreeSet<String> mTalkgroups = new TreeSet<String>();
	private TreeSet<Integer> mActiveLCNs = new TreeSet<Integer>();
	private int mMonitoredLCN;

	/**
	 * Compiles a summary of active talkgroups, unique radio ids encountered
	 * in the decoded messages.  Talkgroups have to be heard a minimum of twice
	 * to be considered valid ... in order to weed out the invalid ones.
	 * 
	 * Returns a textual summary of the activity
	 */
	public LTRStandardActivitySummary( AliasList list )
	{
		mAliasList = list;
	}
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof LTRStandardOSWMessage )
		{
			LTRStandardOSWMessage ltr = ((LTRStandardOSWMessage)message);
			
			if( ltr.isValid() )
			{
				switch( ltr.getMessageType() )
				{
					case CA_STRT:
					case CA_ENDD:
					    int home = ltr.getHomeRepeater();
					    
					    if( 0 < home && home <= 20 )
					    {
	                        String talkgroup = ltr.getTalkgroupID( false );
	                        
	                        if( mTalkgroupsFirstHeard.contains( talkgroup ) )
	                        {
	                            mTalkgroups.add( talkgroup );
	                        }
	                        else
	                        {
	                            mTalkgroupsFirstHeard.add( talkgroup );
	                        }
					    }

					    int channel = ltr.getChannel();
					    
					    if( 0 < channel && channel <= 20 )
					    {
	                        if( channel == home )
	                        {
	                            mMonitoredLCN = channel;
	                            mActiveLCNs.add( channel );
	                        }
	                        
	                        int free = ltr.getFree();
	                        
	                        if( 0 < free && free <= 20 )
	                        {
	                            mActiveLCNs.add( free );
	                        }
					    }

						break;
					case SY_IDLE:
					    int idleChannel = ltr.getChannel();
					    
					    if( idleChannel == ltr.getHomeRepeater() &&
					        idleChannel == ltr.getFree() )
					    {
	                        mMonitoredLCN = idleChannel;
	                        mActiveLCNs.add( idleChannel );
					    }
						break;
					case UN_KNWN:
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

		sb.append( "Activity Summary\n\n" );
		
		sb.append( "Decoder:\tLTR-Standard\n" );
		
		sb.append( "Monitored LCN: " );
		
		if( mMonitoredLCN > 0 )
		{
	        sb.append( mMonitoredLCN );
		}
		else
		{
		    sb.append( "unknown" );
		}
		
		sb.append( "\n" );
		
		Integer[] lcns = mActiveLCNs.toArray( new Integer[0] );
		
		sb.append( "Active LCNs:\t" );
		
		if( lcns.length > 0 )
		{
	        sb.append(  Arrays.toString( lcns ) );
		}
		else
		{
		    sb.append( "none" );
		}
		sb.append( "\n\n" );

		sb.append( "Talkgroups\n" );
		
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
				sb.append( formatTalkgroup( tgid ) );
				sb.append( " " );
				
				Alias alias = mAliasList.getTalkgroupAlias( tgid );
				
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
