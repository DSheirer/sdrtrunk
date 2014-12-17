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
package decode.p25;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import alias.Alias;
import alias.AliasList;
import controller.activity.ActivitySummaryProvider;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.message.pdu.osp.control.NetworkStatusBroadcastExtended;
import decode.p25.message.pdu.osp.control.RFSSStatusBroadcastExtended;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.message.tsbk.osp.control.AdjacentStatusBroadcast;
import decode.p25.message.tsbk.osp.control.NetworkStatusBroadcast;
import decode.p25.message.tsbk.osp.control.RFSSStatusBroadcast;
import decode.p25.message.tsbk.osp.control.SecondaryControlChannelBroadcast;
import decode.p25.message.tsbk.osp.control.SystemService;
import decode.p25.reference.Service;

public class P25ActivitySummary implements ActivitySummaryProvider, 
											   Listener<Message>
{
	private static final int sINT_NULL_VALUE = -1;

	private final static Logger mLog = 
			LoggerFactory.getLogger( P25ActivitySummary.class );

	private static final DecimalFormat mFrequencyFormatter = 
			new DecimalFormat( "0.000000" );

	private AliasList mAliasList;
	
	private NetworkStatusBroadcast mNetworkStatus;
	private NetworkStatusBroadcastExtended mNetworkStatusExtended;
	private RFSSStatusBroadcast mRFSSStatusMessage;
	private RFSSStatusBroadcastExtended mRFSSStatusMessageExtended;
	private HashMap<String,AdjacentStatusBroadcast> mNeighborMap = 
						new HashMap<String,AdjacentStatusBroadcast>();

	private Set<SecondaryControlChannelBroadcast> mSecondaryControlChannels = 
			new TreeSet<SecondaryControlChannelBroadcast>();
	
	public P25ActivitySummary( AliasList list )
	{
		mAliasList = list;
	}
	
	/**
	 * Cleanup method
	 */
	public void dispose()
	{
	}

	@Override
    public void receive( Message message )
    {
		if( !message.isValid() )
		{
			mLog.debug( "Got an invalid message!" );
		}
		
		if( message instanceof TSBKMessage )
		{
			if( message instanceof AdjacentStatusBroadcast )
			{
				AdjacentStatusBroadcast neighborMessage = 
						(AdjacentStatusBroadcast)message;
				
				mNeighborMap.put( neighborMessage.getUniqueID(), neighborMessage );
			}
			else if( message instanceof SecondaryControlChannelBroadcast )
			{
				SecondaryControlChannelBroadcast sccb = 
						(SecondaryControlChannelBroadcast)message;
				
				if( sccb.getDownlinkFrequency1() > 0 )
				{
					mSecondaryControlChannels.add( 
							(SecondaryControlChannelBroadcast)message );
				}
			}
			else if( message instanceof NetworkStatusBroadcast )
			{
				mNetworkStatus = (NetworkStatusBroadcast)message;
			}
			else if( message instanceof RFSSStatusBroadcast )
			{
				mRFSSStatusMessage = (RFSSStatusBroadcast)message;
			}
		}
		else if( message instanceof PDUMessage )
		{
			if( message instanceof NetworkStatusBroadcastExtended )
			{
				mNetworkStatusExtended = (NetworkStatusBroadcastExtended)message;
			}
			else if( message instanceof RFSSStatusBroadcastExtended )
			{
				mRFSSStatusMessageExtended = (RFSSStatusBroadcastExtended)message;
			}
		}
    }

	@Override
    public String getSummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "Activity Summary\n" );
		sb.append( "Decoder:\tP25\n" );
		sb.append( "===================== THIS SITE ======================" );
		
		if( mNetworkStatus != null )
		{
			sb.append( "\nNAC:\t" + mNetworkStatus.getNAC() );
			sb.append( "\nSYSTEM:\t" + mNetworkStatus.getSystemID() );
			sb.append( "\nWACN:\t" + mNetworkStatus.getWACN() );
			sb.append( "\nLRA:\t" + mNetworkStatus.getLocationRegistrationArea() );
		}
		else if( mNetworkStatusExtended != null )
		{
			sb.append( "\nNAC:\t" + mNetworkStatusExtended.getNAC() );
			sb.append( "\nSYSTEM:\t" + mNetworkStatusExtended.getSystemID() );
			sb.append( "\nWACN:\t" + mNetworkStatusExtended.getWACN() );
			sb.append( "\nLRA:\t" + mNetworkStatusExtended.getLocationRegistrationArea() );
		}

		String site = null;
		
		if( mRFSSStatusMessage != null )
		{
			site = mRFSSStatusMessage.getRFSubsystemID() + "-" + mRFSSStatusMessage.getSiteID();
		}
		else if( mRFSSStatusMessageExtended != null )
		{
			site = mRFSSStatusMessageExtended.getRFSubsystemID() + "-" +
				   mRFSSStatusMessageExtended.getSiteID();
		}

		sb.append( "\nRFSS-SITE:\t" + site );
		
		if( mAliasList != null )
		{
			Alias siteAlias = mAliasList.getSiteID( site );
			
			if( siteAlias != null )
			{
				sb.append( " " + siteAlias.getName() );
			}
		}

		if( mNetworkStatus != null )
		{
			sb.append( "\nSERVICES:\t" + SystemService.toString( 
					mNetworkStatus.getSystemServiceClass() ) );
			sb.append( "\nPCCH:\tDNLINK " + mFrequencyFormatter.format( 
					(double)mNetworkStatus.getDownlinkFrequency() / 1E6d ) + 
					" [" + mNetworkStatus.getIdentifier() + "-" + 
					mNetworkStatus.getChannel() + "]\n" );
			sb.append( "\tUPLINK " + mFrequencyFormatter.format( 
					(double)mNetworkStatus.getUplinkFrequency() / 1E6d ) + " [" + 
					mNetworkStatus.getIdentifier() + "-" +
					mNetworkStatus.getChannel() + "]\n" );
		}
		else if( mNetworkStatusExtended != null )
		{
			sb.append( "\nSERVICES:\t" + SystemService.toString( 
					mNetworkStatusExtended.getSystemServiceClass() ) );
			sb.append( "\nPCCH:\tDNLINK " + mFrequencyFormatter.format( 
					(double)mNetworkStatusExtended.getDownlinkFrequency() / 1E6d ) + 
					" [" + mNetworkStatusExtended.getTransmitIdentifier() + "-" + 
					mNetworkStatusExtended.getTransmitChannel() + "]\n" );
			sb.append( "\tUPLINK " + mFrequencyFormatter.format( 
					(double)mNetworkStatusExtended.getUplinkFrequency() / 1E6d ) + 
					" [" + mNetworkStatusExtended.getReceiveIdentifier() + "-" +
					mNetworkStatusExtended.getReceiveChannel() + "]" );
		}
		
		if( mSecondaryControlChannels.isEmpty() )
		{
			sb.append( "\nSCCH:\tNONE" );
		}
		else
		{
			for( SecondaryControlChannelBroadcast sec: mSecondaryControlChannels )
			{
				sb.append( "\nSCCH:\tDNLINK " + mFrequencyFormatter.format( 
						(double)sec.getDownlinkFrequency1() / 1E6d ) +
						" [" + sec.getIdentifier1() + "-" + sec.getChannel1() + "]\n" );
				sb.append( "\tUPLINK " + mFrequencyFormatter.format( 
						(double)sec.getUplinkFrequency1() / 1E6d ) + " [" +
						sec.getIdentifier1() + "-" + sec.getChannel1() + "]\n" );
				
				if( sec.hasChannel2() )
				{
					sb.append( "\nSCCH:\tDNLINK " + mFrequencyFormatter.format( 
							(double)sec.getDownlinkFrequency2() / 1E6d ) +
							" [" + sec.getIdentifier2() + "-" + sec.getChannel2() + "]\n" );
					sb.append( "\tUPLINK " + mFrequencyFormatter.format( 
							(double)sec.getUplinkFrequency2() / 1E6d ) + " [" +
							sec.getIdentifier2() + "-" + sec.getChannel2() + "]" );
				}
			}
		}

		
		
		sb.append( "\n\n=================== NEIGHBORS ======================" );
		
		if( mNeighborMap.isEmpty() )
		{
			sb.append( "\n\tNONE\n" );
			sb.append( "\n----------------------------------------------------" );
		}
		else
		{
			for( AdjacentStatusBroadcast neighbor: mNeighborMap.values() )
			{
				sb.append( "\nNAC:\t" + neighbor.getNAC() );
				sb.append( "\nSYSTEM:\t" + neighbor.getSystemID()  );
				sb.append( "\nLRA:\t" + neighbor.getLocationRegistrationArea() );
				
				String neighborID = neighbor.getRFSS() + "-" + neighbor.getSiteID();
				sb.append( "\nRFSS-SITE:\t" + neighborID  );
				
				if( mAliasList != null )
				{
					Alias siteAlias = mAliasList.getSiteID( neighborID );
					
					if( siteAlias != null )
					{
						sb.append( " " + siteAlias.getName() );
					}
				}
				
				sb.append( "\nPCCH:\tDNLINK " + mFrequencyFormatter.format( 
						(double)neighbor.getDownlinkFrequency() / 1E6d ) +
						" [" + neighbor.getIdentifier() + "-" + neighbor.getChannel() + "]" );
				sb.append( "\n\tUPLINK:" + mFrequencyFormatter.format(  
						(double)neighbor.getUplinkFrequency() / 1E6d ) + "\n" );
				sb.append( "\nSERVICES:\t" + Service.getServices( neighbor.getSystemServiceClass() ) );
				sb.append( "\n----------------------------------------------------" );
			}
		}
		
	    return sb.toString();
    }
}
