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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import message.Message;
import alias.Alias;
import alias.AliasList;
import audio.SquelchListener;
import audio.SquelchListener.SquelchState;
import controller.ThreadPoolManager;
import controller.activity.CallEvent;
import controller.activity.CallEvent.CallEventType;
import controller.channel.ProcessingChain;
import controller.state.AuxChannelState;
import controller.state.ChannelState;
import decode.p25.message.P25Message;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.message.pdu.osp.control.CallAlertExtended;
import decode.p25.message.pdu.osp.control.GroupAffiliationQueryExtended;
import decode.p25.message.pdu.osp.control.GroupAffiliationResponseExtended;
import decode.p25.message.pdu.osp.control.MessageUpdateExtended;
import decode.p25.message.pdu.osp.control.RFSSStatusBroadcastExtended;
import decode.p25.message.pdu.osp.control.RoamingAddressUpdateExtended;
import decode.p25.message.pdu.osp.control.StatusQueryExtended;
import decode.p25.message.pdu.osp.control.StatusUpdateExtended;
import decode.p25.message.pdu.osp.control.UnitRegistrationResponseExtended;
import decode.p25.message.pdu.osp.data.GroupDataChannelGrantExtended;
import decode.p25.message.pdu.osp.data.IndividualDataChannelGrantExtended;
import decode.p25.message.pdu.osp.voice.GroupVoiceChannelGrantExplicit;
import decode.p25.message.pdu.osp.voice.TelephoneInterconnectChannelGrantExplicit;
import decode.p25.message.pdu.osp.voice.UnitToUnitAnswerRequestExplicit;
import decode.p25.message.pdu.osp.voice.UnitToUnitVoiceChannelGrantExtended;
import decode.p25.message.pdu.osp.voice.UnitToUnitVoiceChannelGrantUpdateExtended;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.message.tsbk.osp.control.AcknowledgeResponse;
import decode.p25.message.tsbk.osp.control.AuthenticationCommand;
import decode.p25.message.tsbk.osp.control.CallAlert;
import decode.p25.message.tsbk.osp.control.DenyResponse;
import decode.p25.message.tsbk.osp.control.ExtendedFunctionCommand;
import decode.p25.message.tsbk.osp.control.GroupAffiliationQuery;
import decode.p25.message.tsbk.osp.control.GroupAffiliationResponse;
import decode.p25.message.tsbk.osp.control.LocationRegistrationResponse;
import decode.p25.message.tsbk.osp.control.MessageUpdate;
import decode.p25.message.tsbk.osp.control.ProtectionParameterUpdate;
import decode.p25.message.tsbk.osp.control.QueuedResponse;
import decode.p25.message.tsbk.osp.control.RFSSStatusBroadcast;
import decode.p25.message.tsbk.osp.control.RadioUnitMonitorCommand;
import decode.p25.message.tsbk.osp.control.RoamingAddressCommand;
import decode.p25.message.tsbk.osp.control.StatusQuery;
import decode.p25.message.tsbk.osp.control.StatusUpdate;
import decode.p25.message.tsbk.osp.control.UnitDeregistrationAcknowledge;
import decode.p25.message.tsbk.osp.control.UnitRegistrationCommand;
import decode.p25.message.tsbk.osp.control.UnitRegistrationResponse;
import decode.p25.message.tsbk.osp.data.GroupDataChannelAnnouncement;
import decode.p25.message.tsbk.osp.data.GroupDataChannelAnnouncementExplicit;
import decode.p25.message.tsbk.osp.data.GroupDataChannelGrant;
import decode.p25.message.tsbk.osp.data.IndividualDataChannelGrant;
import decode.p25.message.tsbk.osp.data.SNDCPDataChannelGrant;
import decode.p25.message.tsbk.osp.data.SNDCPDataPageRequest;
import decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrant;
import decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdate;
import decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdateExplicit;
import decode.p25.message.tsbk.osp.voice.TelephoneInterconnectAnswerRequest;
import decode.p25.message.tsbk.osp.voice.TelephoneInterconnectVoiceChannelGrant;
import decode.p25.message.tsbk.osp.voice.TelephoneInterconnectVoiceChannelGrantUpdate;
import decode.p25.message.tsbk.osp.voice.UnitToUnitAnswerRequest;
import decode.p25.message.tsbk.osp.voice.UnitToUnitVoiceChannelGrant;
import decode.p25.message.tsbk.osp.voice.UnitToUnitVoiceChannelGrantUpdate;
import decode.p25.reference.Opcode;
import decode.p25.reference.Response;
import decode.p25.reference.Vendor;

public class P25ChannelState extends ChannelState
{
	private HashMap<String,Long> mRegistrations = new HashMap<String,Long>();
	private String mLastRegistrationEventID;

	private ArrayList<ActiveCall> mActiveCalls = new ArrayList<ActiveCall>();
	
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
		
		if( message instanceof PDUMessage )
		{
			PDUMessage pdu = (PDUMessage)message;
			
			switch( pdu.getOpcode() )
			{
				case CALL_ALERT:
					processCall( pdu );
					break;
				case GROUP_AFFILIATION_QUERY:
					processQuery( pdu );
					break;
				case GROUP_AFFILIATION_RESPONSE:
					processResponse( pdu );
					break;
				case GROUP_DATA_CHANNEL_GRANT:
				case GROUP_VOICE_CHANNEL_GRANT:
				case INDIVIDUAL_DATA_CHANNEL_GRANT:
					processCall( pdu );
					break;
				case MESSAGE_UPDATE:
					processMessage( pdu );
					break;
				case RFSS_STATUS_BROADCAST:
					processRFSSStatus( (RFSSStatusBroadcastExtended)message );
					break;
				case ROAMING_ADDRESS_UPDATE:
					processResponse( pdu );
					break;
				case STATUS_QUERY:
					processQuery( pdu );
					break;
				case STATUS_UPDATE:
					processResponse( pdu );
					break;
				case UNIT_TO_UNIT_ANSWER_REQUEST:
					processPage( pdu );
					break;
				case UNIT_REGISTRATION_RESPONSE:
					processResponse( pdu );
					break;
				default:
					break;
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
					case ACKNOWLEDGE_RESPONSE:
						processResponse( tsbk );
						break;
					case AUTHENTICATION_COMMAND:
						processCommand( tsbk );
						break;
					case CALL_ALERT:
						processCall( tsbk );
						break;
					case DENY_RESPONSE:
						processResponse( tsbk );
						break;
					case EXTENDED_FUNCTION_COMMAND:
						processCommand( tsbk );
						break;
					case GROUP_AFFILIATION_QUERY:
						processQuery( tsbk );
						break;
					case GROUP_AFFILIATION_RESPONSE:
						processResponse( tsbk );
						break;
					case GROUP_DATA_CHANNEL_ANNOUNCEMENT:
					case GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
					case GROUP_DATA_CHANNEL_GRANT:
					case GROUP_VOICE_CHANNEL_GRANT:
					case GROUP_VOICE_CHANNEL_GRANT_UPDATE:
					case GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
					case INDIVIDUAL_DATA_CHANNEL_GRANT:
					case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
					case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
					case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
					case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
						processCall( tsbk );
						break;
					case LOCATION_REGISTRATION_RESPONSE:
					case UNIT_DEREGISTRATION_ACKNOWLEDGE:
						processResponse( tsbk );
						break;
					case MESSAGE_UPDATE:
						processMessage( tsbk );
						break;
					case PROTECTION_PARAMETER_UPDATE:
						processResponse( tsbk );
						break;
					case QUEUED_RESPONSE:
						processResponse( tsbk );
						break;
					case RADIO_UNIT_MONITOR_COMMAND:
						processCommand( tsbk );
						break;
					case RFSS_STATUS_BROADCAST:
						processRFSSStatus( (RFSSStatusBroadcast)tsbk );
						break;
					case ROAMING_ADDRESS_COMMAND:
						processCommand( tsbk );
						break;
					case SNDCP_DATA_CHANNEL_GRANT:
						processCall( tsbk );
						break;
					case STATUS_QUERY:
						processQuery( tsbk );
						break;
					case STATUS_UPDATE:
						processResponse( tsbk );
						break;
					case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
					case UNIT_TO_UNIT_ANSWER_REQUEST:
						processPage( tsbk );
						break;
					case UNIT_REGISTRATION_COMMAND:
						processCommand( tsbk );
						break;
					case UNIT_REGISTRATION_RESPONSE:
						processResponse( tsbk );
						break;
					default:
						break;
				}
			}
		}
	}
	
	private void processCommand( TSBKMessage message )
	{
		switch( message.getOpcode() )
		{
			case AUTHENTICATION_COMMAND:
				AuthenticationCommand ac = (AuthenticationCommand)message;
				
						mCallEventModel.add(
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.details( "AUTHENTICATE" )
							.to( ac.getWACN() + "-" + ac.getSystemID() + "-" +
							     ac.getTargetID() )
							.build() );	
				break;
			case EXTENDED_FUNCTION_COMMAND:
				ExtendedFunctionCommand efc = (ExtendedFunctionCommand)message;
				
				mCallEventModel.add(
						new P25CallEvent.Builder( CallEventType.FUNCTION )
							.aliasList( mAliasList )
							.details( "EXTENDED FUNCTION: " + 
									efc.getExtendedFunction().getLabel() )
							.from( efc.getSourceAddress() )
							.to( efc.getTargetAddress() )
							.build() );	
				break;
			case RADIO_UNIT_MONITOR_COMMAND:
				RadioUnitMonitorCommand rumc = (RadioUnitMonitorCommand)message;
				
				mCallEventModel.add(
				new P25CallEvent.Builder( CallEventType.COMMAND )
					.aliasList( mAliasList )
					.details( "RADIO UNIT MONITOR" )
					.from( rumc.getSourceAddress() )
					.to( rumc.getTargetAddress() )
					.build() );	
				break;
			case ROAMING_ADDRESS_COMMAND:
				RoamingAddressCommand rac = (RoamingAddressCommand)message;
				
				mCallEventModel.add(
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.details( rac.getStackOperation().name() +
									" ROAMING ADDRESS " + rac.getWACN() + "-" + 
									rac.getSystemID() )
							.to( rac.getTargetID() )
							.build() );	
				break;
			case UNIT_REGISTRATION_COMMAND:
				UnitRegistrationCommand urc = (UnitRegistrationCommand)message;
				
						mCallEventModel.add(
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.details( "REGISTER" )
							.from( urc.getSourceAddress() )
							.to( urc.getTargetAddress() )
							.build() );	
				break;
			default:
				break;
		}
	}
	
	private void processMessage( TSBKMessage message )
	{
		MessageUpdate mu = (MessageUpdate)message;
		
		CallEvent event = 
				new P25CallEvent.Builder( CallEventType.SDM )
					.aliasList( mAliasList )
					.details( "MESSAGE: " + mu.getMessage() )
					.from( mu.getSourceAddress() )
					.to( mu.getTargetAddress() )
					.build();	

		mCallEventModel.add( event );
	}
	
	private void processMessage( PDUMessage message )
	{
		MessageUpdateExtended mu = (MessageUpdateExtended)message;
		
		CallEvent event = 
				new P25CallEvent.Builder( CallEventType.SDM )
					.aliasList( mAliasList )
					.details( "MESSAGE: " + mu.getMessage() )
					.from( mu.getSourceWACN() + "-" + mu.getSourceSystemID() + 
							"-" + mu.getSourceID() )
					.to( mu.getTargetAddress() )
					.build();	

		mCallEventModel.add( event );
	}
	
	private void processQuery( TSBKMessage message )
	{
		switch( message.getOpcode() )
		{
			case GROUP_AFFILIATION_QUERY:
				GroupAffiliationQuery gaq = (GroupAffiliationQuery)message;
				
				CallEvent event = 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.details( "GROUP AFFILIATION" )
							.from( gaq.getSourceAddress() )
							.to( gaq.getTargetAddress() )
							.build();	

				mCallEventModel.add( event );
				break;
			case STATUS_QUERY:
				StatusQuery sq = (StatusQuery)message;
				
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.details( "STATUS QUERY" )
							.from( sq.getSourceAddress() )
							.to( sq.getTargetAddress() )
							.build() );	
				break;
			default:
				break;
		}
	}
	
	private void processQuery( PDUMessage message )
	{
		switch( message.getOpcode() )
		{
			case GROUP_AFFILIATION_QUERY:
				GroupAffiliationQueryExtended gaqe = 
							(GroupAffiliationQueryExtended)message;
				
				CallEvent event = 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.details( "GROUP AFFILIATION" )
							.from( gaqe.getWACN() + "-" + gaqe.getSystemID() + 
									"-" + gaqe.getSourceID() )
							.to( gaqe.getTargetAddress() )
							.build();	

				mCallEventModel.add( event );
				break;
			case STATUS_QUERY:
				StatusQueryExtended sq = (StatusQueryExtended)message;
				
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.details( "STATUS QUERY" )
							.from( sq.getSourceWACN() + "-" + 
								   sq.getSourceSystemID() + "-" + 
								   sq.getSourceID() )
							.to( sq.getTargetAddress() )
							.build() );	
				break;
			default:
				break;
		}
	}
	
	private void processResponse( TSBKMessage message )
	{
		switch( message.getOpcode() )
		{
			case ACKNOWLEDGE_RESPONSE:
				AcknowledgeResponse ar = (AcknowledgeResponse)message;
				
				CallEvent event =  
						new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( "ACKNOWLEDGE" )
							.to( ar.getTargetAddress() )
							.build();	
				
				if( ar.hasAdditionalInformation() )
				{
					if( ar.hasExtendedAddress() )
					{
						event.setToID( ar.getWACN() + "-" + ar.getSystemID() + "-" +
									   ar.getTargetAddress() );
					}
					else
					{
						event.setFromID( ar.getFromID() );
					}
				}
				
				mCallEventModel.add( event );
				break;
			case DENY_RESPONSE:
				DenyResponse dr = (DenyResponse)message;
				
				mCallEventModel.add(
						new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( "DENY REASON: " + dr.getReason().name() + 
									  " REQUESTED: " + dr.getServiceType().name() )
							.from( dr.getSourceAddress() )
							.to( dr.getTargetAddress() )
							.build() );	
				break;
			case GROUP_AFFILIATION_RESPONSE:
				GroupAffiliationResponse gar = (GroupAffiliationResponse)message;
				
				mCallEventModel.add(
						new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( "AFFILIATION:" + gar.getResponse().name() + 
									  " FOR " + gar.getAffiliationScope() + 
									  " GROUP:" + gar.getGroupAddress() + 
									  " GROUP ANNOUNCEMENT:" + 
									  gar.getAnnouncementGroupAddress() )
							.to( gar.getTargetAddress() )
							.build() );	

				break;
			case LOCATION_REGISTRATION_RESPONSE:
				LocationRegistrationResponse lrr = 
								(LocationRegistrationResponse)message;

				if( lrr.getResponse() == Response.ACCEPT )
				{
					mRegistrations.put( lrr.getTargetAddress(), 
										System.currentTimeMillis() );
				}

				if( mLastRegistrationEventID != null && 
					!mLastRegistrationEventID.contentEquals( lrr.getTargetAddress() ) )
				{
					mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.REGISTER )
								.aliasList( mAliasList )
								.details( "REGISTRATION:" + lrr.getResponse().name() +
										  " SITE: " + lrr.getRFSSID() + "-" + 
										  lrr.getSiteID() )
								.from( lrr.getTargetAddress() )
								.to( lrr.getGroupAddress() )
								.build() );
				}
				
				mLastRegistrationEventID = lrr.getTargetAddress();
				break;
			case PROTECTION_PARAMETER_UPDATE:
				ProtectionParameterUpdate ppu = (ProtectionParameterUpdate)message;
				
				mCallEventModel.add(  
						new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( "USE ENCRYPTION ALGORITHM:" + 
								ppu.getAlgorithm().name() + " KEY:" + 
								ppu.getKeyID() )
							.to( ppu.getTargetAddress() )
							.build() );	
				break;
			case QUEUED_RESPONSE:
				QueuedResponse qr = (QueuedResponse)message;
				
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( "QUEUED REASON: " + qr.getReason().name() + 
									  " REQUESTED: " + qr.getServiceType().name() )
							.from( qr.getSourceAddress() )
							.to( qr.getTargetAddress() )
							.build() );	
				break;
			case STATUS_UPDATE:
				StatusUpdate su = (StatusUpdate)message;
				
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( "STATUS USER: " + su.getUserStatus() + 
									  " UNIT: " + su.getUnitStatus() )
							.from( su.getSourceAddress() )
							.to( su.getTargetAddress() )
							.build() );	
				break;
			case UNIT_REGISTRATION_RESPONSE:
				UnitRegistrationResponse urr = (UnitRegistrationResponse)message;

				if( urr.getResponse() == Response.ACCEPT )
				{
					mRegistrations.put( urr.getSourceAddress(), 
										System.currentTimeMillis() );
				}

				if( mLastRegistrationEventID != null && 
					!mLastRegistrationEventID.contentEquals( urr.getSourceAddress() ) )
				{
					mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.REGISTER )
								.aliasList( mAliasList )
								.details( "REGISTRATION:" + urr.getResponse().name() +
										  " SYSTEM: " + urr.getSystemID() + "-" + 
										  " SOURCE ID: " + urr.getSourceID() )
								.from( urr.getSourceAddress() )
								.build() );
				}
				
				mLastRegistrationEventID = urr.getSourceAddress();
				break;
			case UNIT_DEREGISTRATION_ACKNOWLEDGE:
				UnitDeregistrationAcknowledge udr =
						(UnitDeregistrationAcknowledge)message;

				if( mLastRegistrationEventID != null && 
					!mLastRegistrationEventID.contentEquals( udr.getSourceID() ) )
				{
					
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.DEREGISTER )
							.aliasList( mAliasList )
							.from( udr.getSourceID() )
							.build() );

					List<String> keysToRemove = new ArrayList<String>();

					/* Remove this radio from the registrations set */
					for( String key: mRegistrations.keySet() )
					{
						if( key.startsWith( udr.getSourceID() ) )
						{
							keysToRemove.add( key );
						}
					}
					
					for( String key: keysToRemove )
					{
						mRegistrations.remove( key );
					}
				}
				
				mLastRegistrationEventID = udr.getSourceID();
				break;
			default:
				break;
		}
	}
	
	private void processResponse( PDUMessage message )
	{
		switch( message.getOpcode() )
		{
			case GROUP_AFFILIATION_RESPONSE:
				GroupAffiliationResponseExtended gar = 
							(GroupAffiliationResponseExtended)message;
				
				CallEvent event = 
						new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( "AFFILIATION:" + gar.getResponse().name() + 
									  " FOR GROUP:" + gar.getGroupWACN() + "-" +
									  gar.getGroupSystemID() + "-" + 
									  gar.getGroupID() + " GROUP ANNOUNCEMENT:" + 
									  gar.getAnnouncementGroupID() )
						    .from( gar.getSourceWACN() + "-" + 
								   gar.getSourceSystemID() + "-" + 
						    	   gar.getSourceID() )
							.to( gar.getTargetAddress() )
							.build();	

				mCallEventModel.add( event );
				break;
			case ROAMING_ADDRESS_UPDATE:
				RoamingAddressUpdateExtended raue = 
							(RoamingAddressUpdateExtended)message;
				
				StringBuilder sb = new StringBuilder();
				sb.append( "ROAMING ADDRESS STACK A:" );
				sb.append( raue.getWACNA() + "-" + raue.getSystemIDA() );
				
				if( raue.isFormat2() )
				{
					sb.append( " B:" );
					sb.append( raue.getWACNB() + "-" + raue.getSystemIDB() );
					sb.append( " C:" );
					sb.append( raue.getWACNC() + "-" + raue.getSystemIDC() );
					sb.append( " D:" );
					sb.append( raue.getWACND() + "-" + raue.getSystemIDD() );
				}
				
				if( raue.isFormat3() )
				{
					sb.append( " E:" );
					sb.append( raue.getWACNE() + "-" + raue.getSystemIDE() );
					sb.append( " F:" );
					sb.append( raue.getWACNF() + "-" + raue.getSystemIDF() );
					sb.append( " G:" );
					sb.append( raue.getWACNG() + "-" + raue.getSystemIDG() );
				}
				
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( sb.toString() )
							.from( raue.getSourceID() )
							.to( raue.getTargetAddress() )
							.build() );	
				break;
			case STATUS_UPDATE:
				StatusUpdateExtended su = (StatusUpdateExtended)message;
				
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( "STATUS USER: " + su.getUserStatus() + 
									  " UNIT: " + su.getUnitStatus() )
							.from( su.getSourceWACN() + "-" + 
								   su.getSourceSystemID() + "-" + 
								   su.getSourceID() )
							.to( su.getTargetAddress() )
							.build() );	
				break;
			case UNIT_REGISTRATION_RESPONSE:
				UnitRegistrationResponseExtended urr = 
								(UnitRegistrationResponseExtended)message;

				if( urr.getResponse() == Response.ACCEPT )
				{
					mRegistrations.put( urr.getAssignedSourceAddress(), 
										System.currentTimeMillis() );
				}

				if( mLastRegistrationEventID != null && 
					!mLastRegistrationEventID.contentEquals( urr.getAssignedSourceAddress() ) )
				{
					mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.REGISTER )
								.aliasList( mAliasList )
								.details( "REGISTRATION:" + urr.getResponse().name() +
										  " FOR EXTERNAL SYSTEM ADDRESS: " +
										  urr.getWACN() + "-" + 
										  urr.getSystemID() + "-" +
										  urr.getSourceAddress() +
										  " SOURCE ID: " + urr.getSourceID() )
								.from( urr.getAssignedSourceAddress() )
								.build() );
				}
				
				mLastRegistrationEventID = urr.getAssignedSourceAddress();
				break;
			default:
				break;
		}
	}
	
	private void processRFSSStatus( RFSSStatusBroadcast message )
	{
		if( mNAC == null || !mNAC.contentEquals( message.getNAC() ) )
		{
			mNAC = message.getNAC();
			
			broadcastChange( ChangedAttribute.NAC );
		}
		
		if( mSystem == null || !mSystem.contentEquals( message.getSystemID() ) )
		{
			mSystem = message.getSystemID();
			
			broadcastChange( ChangedAttribute.SYSTEM );
		}
		
		String site = message.getRFSubsystemID() + "-" + message.getSiteID();
		
		if( mSite == null || !mSite.contentEquals( site ) )
		{
			mSite = site;
			
			broadcastChange( ChangedAttribute.SITE );
		}
	}
	
	private void processRFSSStatus( RFSSStatusBroadcastExtended message )
	{
		if( mNAC == null || !mNAC.contentEquals( message.getNAC() ) )
		{
			mNAC = message.getNAC();
			
			broadcastChange( ChangedAttribute.NAC );
		}
		
		if( mSystem == null || !mSystem.contentEquals( message.getSystemID() ) )
		{
			mSystem = message.getSystemID();
			
			broadcastChange( ChangedAttribute.SYSTEM );
		}
		
		String site = message.getRFSubsystemID() + "-" + message.getSiteID();
		
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
	
	private void processCall( PDUMessage message )
	{
		switch( message.getOpcode() )
		{
			case CALL_ALERT:
				CallAlertExtended ca = (CallAlertExtended)message;
				
				mCallEventModel.add(
						new P25CallEvent.Builder( CallEventType.CALL_ALERT )
							.aliasList( mAliasList )
							.from( ca.getWACN() + "-" + ca.getSystemID() + "-" + 
									ca.getSourceID() )
							.to( ca.getTargetAddress() )
							.build() );	
				break;
			case GROUP_DATA_CHANNEL_GRANT:
				GroupDataChannelGrantExtended gdcge = 
								(GroupDataChannelGrantExtended)message;

				if( isActiveCall( gdcge.getTransmitChannel(), gdcge.getGroupAddress() ) )
				{
					updateCall( gdcge.getTransmitChannel(), gdcge.getGroupAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( gdcge.getTransmitChannel() )
								.details( "GROUP DATA CHANNEL GRANT EXTENDED" )
//								.details( ( gdcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( gdcg.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( gdcge.getDownlinkFrequency() )
								.from( gdcge.getSourceAddress() )
								.to( gdcge.getGroupAddress() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}
				break;
			case GROUP_VOICE_CHANNEL_GRANT:
				GroupVoiceChannelGrantExplicit gvcge = 
							(GroupVoiceChannelGrantExplicit)message;

				if( isActiveCall( gvcge.getTransmitChannel(), gvcge.getGroupAddress() ) )
				{
					updateCall( gvcge.getTransmitChannel(), gvcge.getGroupAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( gvcge.getTransmitChannel() )
								.details( "GROUP_VOICE_CHANNEL_GRANT" )
//								.details( ( gvcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( gvcg.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( gvcge.getDownlinkFrequency() )
								.from( gvcge.getSourceAddress() )
								.to( gvcge.getGroupAddress() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}
				break;
			case INDIVIDUAL_DATA_CHANNEL_GRANT:
				IndividualDataChannelGrantExtended idcge = 
								(IndividualDataChannelGrantExtended)message;

				if( isActiveCall( idcge.getTransmitChannel(), idcge.getTargetAddress() ) )
				{
					updateCall( idcge.getTransmitChannel(), idcge.getTargetAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( idcge.getTransmitChannel() )
								.details( "INDIV DATA CHANNEL GRANT EXTENDED" )
//								.details( ( idcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( idcg.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( idcge.getDownlinkFrequency() )
								.from( idcge.getSourceWACN() + "-" +
									   idcge.getSourceSystemID() + "-" +
									   idcge.getSourceAddress() )
								.to( idcge.getTargetAddress() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}
				break;
			case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
				TelephoneInterconnectChannelGrantExplicit ticge =
							(TelephoneInterconnectChannelGrantExplicit)message;
				
				if( isActiveCall( ticge.getTransmitChannel(), ticge.getAddress() ))
				{
					updateCall( ticge.getTransmitChannel(), ticge.getAddress() );
				}
				else
				{
					CallEvent event = 
							new P25CallEvent.Builder( CallEventType.TELEPHONE_CALL )
								.aliasList( mAliasList )
								.channel( ticge.getTransmitChannel() )
								.details( ( ticge.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( ticge.isEmergency() ? " EMERGENCY" : "") +
										  " CALL TIMER:" + ticge.getCallTimer() )
							    .frequency( ticge.getDownlinkFrequency() )
							    .from( ticge.getAddress() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}
				break;
			case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
				UnitToUnitVoiceChannelGrantExtended uuvcge = 
							(UnitToUnitVoiceChannelGrantExtended)message;
				
				if( isActiveCall( uuvcge.getTransmitChannel(), uuvcge.getTargetAddress() ) )
				{
					updateCall( uuvcge.getTransmitChannel(), uuvcge.getTargetAddress() );
				}
				else
				{
					CallEvent event = 
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( uuvcge.getTransmitChannel() )
								.details( "UNIT_TO_UNIT_VOICE_CHANNEL_GRANT" )
//								.details( ( uuvcge.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( uuvcge.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( uuvcge.getDownlinkFrequency() )
							    .from( uuvcge.getSourceWACN() + "-" +
							    	   uuvcge.getSourceSystemID() + "-" +
							    	   uuvcge.getSourceID() )
								.to( uuvcge.getTargetAddress() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}
				break;
			case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
				UnitToUnitVoiceChannelGrantUpdateExtended uuvcgue = 
							(UnitToUnitVoiceChannelGrantUpdateExtended)message;
	
				if( isActiveCall( uuvcgue.getTransmitChannel(), 
								  uuvcgue.getTargetAddress() ) )
				{
					updateCall( uuvcgue.getTransmitChannel(), 
								uuvcgue.getTargetAddress() );
				}
				else
				{
					CallEvent event = 
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( uuvcgue.getTransmitChannel() )
								.details( "UNIT_TO_UNIT_VOICE_CHANNEL_GRANT" )
			//					.details( ( uuvcge.isEncrypted() ? "ENCRYPTED" : "" ) + 
			//							  ( uuvcge.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( uuvcgue.getDownlinkFrequency() )
							    .from( uuvcgue.getSourceWACN() + "-" +
							    	   uuvcgue.getSourceSystemID() + "-" +
							    	   uuvcgue.getSourceID() )
								.to( uuvcgue.getTargetAddress() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}

				break;
				
			default:
				break;
		}
		
	}
	
	/**
	 * Process a call event message
	 */
	private void processCall( TSBKMessage message )
	{
		switch( message.getOpcode() )
		{
			case CALL_ALERT:
				CallAlert ca = (CallAlert)message;
				
				mCallEventModel.add(
						new P25CallEvent.Builder( CallEventType.CALL_ALERT )
							.aliasList( mAliasList )
							.from( ca.getSourceID() )
							.to( ca.getTargetAddress() )
							.build() );	
				break;
			case GROUP_DATA_CHANNEL_ANNOUNCEMENT:
				GroupDataChannelAnnouncement gdca = (GroupDataChannelAnnouncement)message;

				if( isActiveCall( gdca.getChannel1(), gdca.getGroupAddress1() ) )
				{
					updateCall( gdca.getChannel1(), gdca.getGroupAddress1() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( gdca.getChannel1() )
								.details( "GROUP DATA CHANNEL ANNOUNCEMENT" )
//								.details( ( gdca.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( gdca.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( gdca.getDownlinkFrequency1() )
								.to( gdca.getGroupAddress1() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}
				
				if( gdca.hasChannelNumber2() )
				{
					if( isActiveCall( gdca.getChannel2(), gdca.getGroupAddress2() ) )
					{
						updateCall( gdca.getChannel2(), gdca.getGroupAddress2() );
					}
					else
					{
						CallEvent event =  
								new P25CallEvent.Builder( CallEventType.DATA_CALL )
									.aliasList( mAliasList )
									.channel( gdca.getChannel2() )
									.details( "GROUP DATA CHANNEL ANNOUNCEMENT" )
//									.details( ( gdca.isEncrypted() ? "ENCRYPTED" : "" ) + 
//											  ( gdca.isEmergency() ? " EMERGENCY" : "") )
								    .frequency( gdca.getDownlinkFrequency2() )
									.to( gdca.getGroupAddress2() )
									.build();
						
						mCallEventModel.add( event );
						
						addCall( event );
					}
				}
				break;
			case GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
				GroupDataChannelAnnouncementExplicit gdcae = 
								(GroupDataChannelAnnouncementExplicit)message;

				if( isActiveCall( gdcae.getTransmitChannel(), gdcae.getGroupAddress() ) )
				{
					updateCall( gdcae.getTransmitChannel(), gdcae.getGroupAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( gdcae.getTransmitChannel() )
								.details( "GROUP DATA CHANNEL ANNOUNCEMENT EXPLICIT" )
//								.details( ( gdca.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( gdca.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( gdcae.getDownlinkFrequency() )
								.to( gdcae.getGroupAddress() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}
				break;
			case GROUP_DATA_CHANNEL_GRANT:
				GroupDataChannelGrant gdcg = (GroupDataChannelGrant)message;

				if( isActiveCall( gdcg.getChannel(), gdcg.getGroupAddress() ) )
				{
					updateCall( gdcg.getChannel(), gdcg.getGroupAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( gdcg.getChannel() )
								.details( "GROUP DATA CHANNEL GRANT" )
//								.details( ( gdcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( gdcg.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( gdcg.getDownlinkFrequency() )
								.from( gdcg.getSourceAddress() )
								.to( gdcg.getGroupAddress() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}
				break;
			case GROUP_VOICE_CHANNEL_GRANT:
				GroupVoiceChannelGrant gvcg = (GroupVoiceChannelGrant)message;

				if( isActiveCall( gvcg.getChannel(), gvcg.getGroupAddress() ) )
				{
					updateCall( gvcg.getChannel(), gvcg.getGroupAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( gvcg.getChannel() )
								.details( "GROUP_VOICE_CHANNEL_GRANT" )
//								.details( ( gvcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( gvcg.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( gvcg.getDownlinkFrequency() )
								.from( gvcg.getSourceAddress() )
								.to( gvcg.getGroupAddress() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}
				break;
			case GROUP_VOICE_CHANNEL_GRANT_UPDATE:
				GroupVoiceChannelGrantUpdate gvcgu =
						(GroupVoiceChannelGrantUpdate)message;
				
				if( isActiveCall( gvcgu.getChannel1(), gvcgu.getGroupAddress1() ) )
				{
					updateCall( gvcgu.getChannel1(), gvcgu.getGroupAddress1() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( gvcgu.getChannel1() )
								.details( "1-GROUP_VOICE_CHANNEL_GRANT_UPDATE" )
//								.details( ( gvcgu.isEncrypted() ? "ENCRYPTED" : "" ) + 
//							  ( gvcgu.isEmergency() ? " EMERGENCY" : "") )
								.frequency( gvcgu.getDownlinkFrequency1() )
								.to( gvcgu.getGroupAddress1() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}

				if( gvcgu.hasChannelNumber2() )
				{
					if( isActiveCall( gvcgu.getChannel2(), gvcgu.getGroupAddress2() ) )
					{
						updateCall( gvcgu.getChannel2(), gvcgu.getGroupAddress2() );
					}
					else
					{
						CallEvent event = 
								new P25CallEvent.Builder( CallEventType.CALL )
									.aliasList( mAliasList )
									.channel( gvcgu.getChannel2() )
									.details( "2-GROUP_VOICE_CHANNEL_GRANT_UPDATE" )
//									.details( ( gvcgu.isEncrypted() ? "ENCRYPTED" : "" ) + 
//											  ( gvcgu.isEmergency() ? " EMERGENCY" : "") )
									.frequency( gvcgu.getDownlinkFrequency2() )
									.to( gvcgu.getGroupAddress2() )
									.build();

						mCallEventModel.add( event );
						
						addCall( event );
					}
				}
				break;
			case GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
				GroupVoiceChannelGrantUpdateExplicit gvcgue = 
					(GroupVoiceChannelGrantUpdateExplicit)message;

				mCallEventModel.add( 
					new P25CallEvent.Builder( CallEventType.CALL )
						.aliasList( mAliasList )
						.channel( gvcgue.getTransmitChannelIdentifier() + "-" + 
								  gvcgue.getTransmitChannelNumber() )
						.details( "GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT")
//						.details( ( gvcgue.isEncrypted() ? "ENCRYPTED" : "" ) + 
//								  ( gvcgue.isEmergency() ? " EMERGENCY" : "") )
					    .frequency( gvcgue.getDownlinkFrequency() )
						.to( gvcgue.getGroupAddress() )
						.build() );
				break;
			case INDIVIDUAL_DATA_CHANNEL_GRANT:
				IndividualDataChannelGrant idcg = (IndividualDataChannelGrant)message;

				if( isActiveCall( idcg.getChannel(), idcg.getTargetAddress() ) )
				{
					updateCall( idcg.getChannel(), idcg.getTargetAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( idcg.getChannel() )
								.details( "INDIV DATA CHANNEL GRANT" )
//								.details( ( idcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( idcg.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( idcg.getDownlinkFrequency() )
								.from( idcg.getSourceAddress() )
								.to( idcg.getTargetAddress() )
								.build();
					
					mCallEventModel.add( event );
					
					addCall( event );
				}
				break;
			case SNDCP_DATA_CHANNEL_GRANT:
				SNDCPDataChannelGrant sdcg = (SNDCPDataChannelGrant)message;
				
				mCallEventModel.add(   
						new P25CallEvent.Builder( CallEventType.DATA_CALL )
							.aliasList( mAliasList )
							.channel( sdcg.getTransmitChannel() )
							.details( "SNDCP DATA NSAPI:" + sdcg.getNSAPI() )
						    .frequency( sdcg.getDownlinkFrequency() )
							.to( sdcg.getTargetAddress() )
							.build() );
				break;
			case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
				TelephoneInterconnectVoiceChannelGrant tivcg =
							(TelephoneInterconnectVoiceChannelGrant)message;
				
				if( isActiveCall( tivcg.getChannel(), tivcg.getAddress() ))
				{
					updateCall( tivcg.getChannel(), tivcg.getAddress() );
				}
				else
				{
					CallEvent tivcgEvent = 
							new P25CallEvent.Builder( CallEventType.TELEPHONE_CALL )
								.aliasList( mAliasList )
								.channel( tivcg.getChannel() )
								.details( ( tivcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( tivcg.isEmergency() ? " EMERGENCY" : "") +
										  " CALL TIMER:" + tivcg.getCallTimer() )
							    .frequency( tivcg.getDownlinkFrequency() )
							    .from( tivcg.getAddress() )
								.build();
					
					mCallEventModel.add( tivcgEvent );
					
					addCall( tivcgEvent );
				}
				break;
			case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
				TelephoneInterconnectVoiceChannelGrantUpdate tivcgu =
							(TelephoneInterconnectVoiceChannelGrantUpdate)message;

				if( isActiveCall( tivcgu.getChannel(), tivcgu.getAddress() ) )
				{
					updateCall( tivcgu.getChannel(), tivcgu.getAddress() );
				}
				else
				{
					CallEvent tivcguEvent = 
							new P25CallEvent.Builder( CallEventType.TELEPHONE_CALL )
								.aliasList( mAliasList )
								.channel( tivcgu.getChannelIdentifier() + "-" + 
										tivcgu.getChannelNumber() )
								.details( "TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE" )
//								.details( ( tivcgu.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( tivcgu.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( tivcgu.getDownlinkFrequency() )
							    .from( tivcgu.getAddress() )
								.build();
					
					mCallEventModel.add( tivcguEvent );
					
					addCall( tivcguEvent );
				}
				break;
			case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
				UnitToUnitVoiceChannelGrant uuvcg = 
							(UnitToUnitVoiceChannelGrant)message;
				
				if( isActiveCall( uuvcg.getChannel(), uuvcg.getTargetAddress() ) )
				{
					updateCall( uuvcg.getChannel(), uuvcg.getTargetAddress() );
				}
				else
				{
					CallEvent uuvcgEvent = 
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( uuvcg.getChannelIdentifier() + "-" + 
										uuvcg.getChannelNumber() )
								.details( "UNIT_TO_UNIT_VOICE_CHANNEL_GRANT" )
//								.details( ( uuvcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( uuvcg.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( uuvcg.getDownlinkFrequency() )
							    .from( uuvcg.getSourceAddress() )
								.to( uuvcg.getTargetAddress() )
								.build();
					
					mCallEventModel.add( uuvcgEvent );
					
					addCall( uuvcgEvent );
				}

				break;
			case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
				UnitToUnitVoiceChannelGrantUpdate uuvcgu = 
							(UnitToUnitVoiceChannelGrantUpdate)message;

				if( isActiveCall( uuvcgu.getChannel(), uuvcgu.getTargetAddress() ) )
				{
					updateCall( uuvcgu.getChannel(), uuvcgu.getTargetAddress() );
				}
				else
				{
					CallEvent uuvcguEvent = 
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( uuvcgu.getChannelIdentifier() + "-" + 
										uuvcgu.getChannelNumber() )
								.details( "UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE" )
//								.details( ( uuvcgu.isEncrypted() ? "ENCRYPTED" : "" ) + 
//										  ( uuvcgu.isEmergency() ? " EMERGENCY" : "") )
							    .frequency( uuvcgu.getDownlinkFrequency() )
							    .from( uuvcgu.getSourceAddress() )
								.to( uuvcgu.getTargetAddress() )
								.build();
					
					mCallEventModel.add( uuvcguEvent );
					
					addCall( uuvcguEvent );
				}

				break;
			default:
				break;
		}
	}
	
	private boolean isActiveCall( String channel, String id )
	{
		for( ActiveCall call: mActiveCalls )
		{
			if( call.matches( channel, id ) )
			{
				return true;
			}
		}
		
		return false;
	}
	
	private void addCall( CallEvent event )
	{
		mActiveCalls.add( new ActiveCall( getProcessingChain()
				.getResourceManager().getThreadPoolManager(), event ) );
	}
	
	private void updateCall( String channel, String id )
	{
		for( ActiveCall call: mActiveCalls )
		{
			if( call.matches( channel, id ) )
			{
				call.update( System.currentTimeMillis() );
				
				return;
			}
		}
	}

	/**
	 * Process a unit paging event message
	 */
	private void processPage( TSBKMessage message )
	{
		switch( message.getOpcode() )
		{
			case UNIT_TO_UNIT_ANSWER_REQUEST:
				UnitToUnitAnswerRequest utuar = (UnitToUnitAnswerRequest)message;
				
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.PAGE )
						.aliasList( mAliasList )
						.details( ( utuar.isEmergency() ? "EMERGENCY" : "" ) ) 
						.from( utuar.getSourceAddress() )
						.to( utuar.getTargetAddress() )
						.build() );
				break;
			case SNDCP_DATA_PAGE_REQUEST:
				SNDCPDataPageRequest sdpr = (SNDCPDataPageRequest)message;
				
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.PAGE )
						.aliasList( mAliasList )
						.details( "SNDCP DATA DAC: " + 
								sdpr.getDataAccessControl() + 
								" NSAPI:" + sdpr.getNSAPI() ) 
						.to( sdpr.getTargetAddress() )
						.build() );
				break;
			case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
				TelephoneInterconnectAnswerRequest tiar = 
						(TelephoneInterconnectAnswerRequest)message;

				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.PAGE )
						.aliasList( mAliasList )
						.details( ( "TELEPHONE INTERCONNECT" ) ) 
						.from( tiar.getTelephoneNumber() )
						.to( tiar.getTargetAddress() )
						.build() );
				break;
			default:
				break;
		}
	}
	
	private void processPage( PDUMessage message )
	{
		if( message.getOpcode() == Opcode.UNIT_TO_UNIT_ANSWER_REQUEST )
		{
			UnitToUnitAnswerRequestExplicit utuare = 
					(UnitToUnitAnswerRequestExplicit)message;
			
			mCallEventModel.add( 
					new P25CallEvent.Builder( CallEventType.PAGE )
					.aliasList( mAliasList )
					.details( ( utuare.isEmergency() ? "EMERGENCY" : "" ) ) 
					.from( utuare.getSourceID() )
					.to( utuare.getWACN() + "-" + 
						 utuare.getSystemID() + "-" + 
							utuare.getTargetAddress() )
					.build() );
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
	
	public class ActiveCall
	{
		public static final long TIMEOUT = 2000; //milliseconds
		
		private CallEvent mEvent;
		private long mLastUpdate;
		private ThreadPoolManager mThreadPoolManager;
		
		public ActiveCall( ThreadPoolManager threadManager, CallEvent event )
		{
			mThreadPoolManager = threadManager;
			mEvent = event;
			mLastUpdate = System.currentTimeMillis();
			
			mThreadPoolManager.scheduleOnce( new CallExpirationCheck( this ), 
					TIMEOUT, TimeUnit.MILLISECONDS );
		}
		
		public CallEvent getCallEvent()
		{
			return mEvent;
		}
		
		public boolean matches( String channel, String id )
		{
			return mEvent.getChannel().contentEquals( channel ) &&
				   mEvent.getToID().contentEquals( id );
		}
		
		public void update( long timestamp )
		{
			mLastUpdate = System.currentTimeMillis();
		}
		
		public void checkExpiration()
		{
			long now = System.currentTimeMillis();
			
			if( now > mLastUpdate + TIMEOUT )
			{
				mCallEventModel.setEnd( mEvent );
				mActiveCalls.remove( this );
			}
			else
			{
				mThreadPoolManager.scheduleOnce( new CallExpirationCheck( this ), 
						TIMEOUT, TimeUnit.MILLISECONDS );
			}
		}
		
		public long getExpiration()
		{
			return mLastUpdate + 2000; //2 seconds
		}
	}

	/**
	 * Timer to check active calls for call end
	 */
	public class CallExpirationCheck implements Runnable
	{
		private ActiveCall mActiveCall;
		
		public CallExpirationCheck( ActiveCall activeCall )
		{
			mActiveCall = activeCall;
		}
		@Override
        public void run()
        {
			mActiveCall.checkExpiration();
        }
	}
}
