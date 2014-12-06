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

import message.Message;
import alias.Alias;
import alias.AliasList;
import audio.SquelchListener;
import audio.SquelchListener.SquelchState;
import controller.channel.ProcessingChain;
import controller.state.AuxChannelState;
import controller.state.ChannelState;
import decode.p25.message.P25Message;
import decode.p25.message.pdu.osp.control.RFSSStatusBroadcastExtended;
import decode.p25.message.tsbk.osp.control.RFSSStatusBroadcast;

public class P25ChannelState extends ChannelState
{
	private P25ActivitySummary mActivitySummary;
	private String mNAC;
	private String mSystem;
	private String mSite;
	private String mSiteAlias;
	private String mFromTalkgroup;
	private String mFromAlias;
	private String mToTalkgroup;
	private String mToAlias;
	
	public P25ChannelState( ProcessingChain chain, AliasList aliasList )
	{
		super( chain, aliasList );
		
		mActivitySummary = new P25ActivitySummary( aliasList );
	}
	
	public void addListener( SquelchListener listener )
	{
		super.addListener( listener );
		
		super.setSquelchState( SquelchState.UNSQUELCH );
	}

	public void receive( Message message )
	{
		super.receive( message );

		if( message instanceof P25Message )
		{
			mActivitySummary.receive( (P25Message)message );
		}
		
		if( message instanceof RFSSStatusBroadcast )
		{
			RFSSStatusBroadcast rfss = (RFSSStatusBroadcast)message;
			
			if( mNAC == null || !mNAC.contentEquals( rfss.getNAC() ) )
			{
				mNAC = rfss.getNAC();
				
				broadcastChange( ChangedAttribute.NAC );
			}
			
			if( mSystem == null || !mSystem.contentEquals( rfss.getSystemID() ) )
			{
				mSystem = rfss.getSystemID();
				
				broadcastChange( ChangedAttribute.SYSTEM );
			}
			
			String site = rfss.getRFSubsystemID() + "-" + rfss.getSiteID();
			
			if( mSite == null || !mSite.contentEquals( site ) )
			{
				mSite = site;
				
				broadcastChange( ChangedAttribute.SITE );
			}
		}
		else if( message instanceof RFSSStatusBroadcastExtended )
		{
			RFSSStatusBroadcastExtended rfss = (RFSSStatusBroadcastExtended)message;
			
			if( mNAC == null || !mNAC.contentEquals( rfss.getNAC() ) )
			{
				mNAC = rfss.getNAC();
				
				broadcastChange( ChangedAttribute.NAC );
			}
			
			if( mSystem == null || !mSystem.contentEquals( rfss.getSystemID() ) )
			{
				mSystem = rfss.getSystemID();
				
				broadcastChange( ChangedAttribute.SYSTEM );
			}
			
			String site = rfss.getRFSubsystemID() + "-" + rfss.getSiteID();
			
			if( mSite == null || !mSite.contentEquals( site ) )
			{
				mSite = site;
				
				if( mAliasList != null )
				{
					Alias alias = mAliasList.getSiteID( mSite );
							
					if( alias != null )
					{
						mSiteAlias = alias.getName();
						
						broadcastChange( ChangedAttribute.SITE_ALIAS );
					}
				}
				
				broadcastChange( ChangedAttribute.SITE );
			}
		}
	}

	
	@Override
	public void setSquelchState( SquelchState state )
	{
		//do nothing ... we want the squelch state always unsquelched (for now)
	}
	
	@Override
    public String getActivitySummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( mActivitySummary.getSummary() );
		
		for( AuxChannelState state: mAuxChannelStates )
		{
			sb.append( state.getActivitySummary() );
			sb.append( "\n\n" );
		}
		
		return sb.toString();
    }
	
	public String getNAC()
	{
		return mNAC;
	}
	
	public String getSystem()
	{
		return mSystem;
	}
	
	public String getSiteAlias()
	{
		return mSiteAlias;
	}
	
	public String getSite()
	{
		return mSite;
	}
	
	public String getFromTalkgroup()
	{
		return mFromTalkgroup;
	}
	
	public String getFromAlias()
	{
		return mFromAlias;
	}
	
	public String getToTalkgroup()
	{
		return mToTalkgroup;
	}
	
	public String getToAlias()
	{
		return mToAlias;
	}
}
