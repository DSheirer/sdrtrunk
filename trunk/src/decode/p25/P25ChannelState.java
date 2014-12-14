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
import controller.activity.CallEvent.CallEventType;
import controller.channel.ProcessingChain;
import controller.state.AuxChannelState;
import controller.state.ChannelState;
import decode.p25.message.P25Message;
import decode.p25.message.pdu.osp.control.RFSSStatusBroadcastExtended;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.message.tsbk.osp.control.LocationRegistrationResponse;
import decode.p25.message.tsbk.osp.control.RFSSStatusBroadcast;
import decode.p25.message.tsbk.osp.control.UnitDeregistrationAcknowledge;
import decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrant;
import decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdate;
import decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdateExplicit;
import decode.p25.message.tsbk.osp.voice.UnitToUnitAnswerRequest;
import decode.p25.reference.Vendor;

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
					}
					else
					{
						mSiteAlias = null;
					}
				}

				broadcastChange( ChangedAttribute.SITE );
				
				broadcastChange( ChangedAttribute.SITE_ALIAS );
			}
		}
		else if( message instanceof TSBKMessage )
		{
			TSBKMessage tsbk = (TSBKMessage)message;
			
			setState( State.CONTROL );
			
			if( tsbk.getVendor() == Vendor.STANDARD )
			{
				switch( tsbk.getOpcode() )
				{
					case GROUP_VOICE_CHANNEL_GRANT:
						GroupVoiceChannelGrant gvcg = (GroupVoiceChannelGrant)tsbk;

						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( gvcg.getChannelIdentifier() + "-" + gvcg.getChannel() )
								.details( ( gvcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( gvcg.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( gvcg.getDownlinkFrequency() )
								.from( gvcg.getSourceAddress() )
								.to( gvcg.getGroupAddress() )
								.build() );
						break;
					case GROUP_VOICE_CHANNEL_GRANT_UPDATE:
						GroupVoiceChannelGrantUpdate gvcgu =
								(GroupVoiceChannelGrantUpdate)tsbk;
						mCallEventModel.add( 
								new P25CallEvent.Builder( CallEventType.CALL )
									.aliasList( mAliasList )
									.channel( gvcgu.getChannelID1() + "-" + gvcgu.getChannelNumber1() )
									.details( ( gvcgu.isEncrypted() ? "ENCRYPTED" : "" ) + 
											  ( gvcgu.isEmergency() ? " EMERGENCY" : "") )
									.frequency( gvcgu.getDownlinkFrequency() )
									.to( gvcgu.getGroupAddress1() )
									.build() );
						if( gvcgu.getChannelNumber2() != 0 )
						{
							mCallEventModel.add( 
									new P25CallEvent.Builder( CallEventType.CALL )
										.aliasList( mAliasList )
										.channel( gvcgu.getChannelID2() + "-" + gvcgu.getChannelNumber2() )
										.details( ( gvcgu.isEncrypted() ? "ENCRYPTED" : "" ) + 
												  ( gvcgu.isEmergency() ? " EMERGENCY" : "") )
										.frequency( gvcgu.getDownlinkFrequency() )
										.to( gvcgu.getGroupAddress2() )
										.build() );
						}
						break;
					case GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
						GroupVoiceChannelGrantUpdateExplicit gvcgue = 
							(GroupVoiceChannelGrantUpdateExplicit)tsbk;

						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( gvcgue.getTransmitChannelID() + "-" + 
										  gvcgue.getTransmitChannelNumber() )
								.details( ( gvcgue.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( gvcgue.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( gvcgue.getDownlinkFrequency() )
								.to( gvcgue.getGroupAddress() )
								.build() );
						break;
					case UNIT_TO_UNIT_ANSWER_REQUEST:
						UnitToUnitAnswerRequest utuar = 
									(UnitToUnitAnswerRequest)tsbk;
						mCallEventModel.add( 
								new P25CallEvent.Builder( CallEventType.PAGE )
								.aliasList( mAliasList )
								.details( ( utuar.isEmergency() ? "EMERGENCY" : "" ) ) 
								.from( utuar.getSourceAddress() )
								.to( utuar.getTargetAddress() )
								.build() );
						break;
					case LOCATION_REGISTRATION_RESPONSE:
						LocationRegistrationResponse lrr = 
										(LocationRegistrationResponse)tsbk;
						
						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.REGISTER )
								.aliasList( mAliasList )
								.details( "REG:" + lrr.getResponse().name() +
										  " SITE: " + lrr.getRFSSID() + "-" + 
										  lrr.getSiteID() )
								.from( lrr.getTargetAddress() )
								.to( lrr.getGroupAddress() )
								.build() );
						break;
					case UNIT_DEREGISTRATION_ACKNOWLEDGE:
						UnitDeregistrationAcknowledge udr =
								(UnitDeregistrationAcknowledge)tsbk;
						
						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.DEREGISTER )
								.aliasList( mAliasList )
								.from( udr.getSourceID() )
								.build() );
						break;
				}
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
