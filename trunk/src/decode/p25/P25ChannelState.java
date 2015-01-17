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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import decode.p25.message.hdu.HDUMessage;
import decode.p25.message.ldu.LDU1Message;
import decode.p25.message.ldu.LDU2Message;
import decode.p25.message.ldu.LDUMessage;
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
import decode.p25.message.tdu.lc.TDULinkControlMessage;
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
	private final static Logger mLog = LoggerFactory.getLogger( P25ChannelState.class );
	
	private final static String CURRENT_CHANNEL = "CHANNEL";

	private HashMap<String,Long> mRegistrations = new HashMap<String,Long>();
	private String mLastCommandEventID;
	private String mLastPageEventID;
	private String mLastQueryEventID;
	private String mLastRegistrationEventID;
	private String mLastResponseEventID;

	private CopyOnWriteArrayList<ActiveCall> mActiveCalls = 
							new CopyOnWriteArrayList<ActiveCall>();
	
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

	/**
	 * Message processor.  All messages are received and processed via this method.
	 */
	public void receive( Message message )
	{
		super.receive( message );

		if( message instanceof P25Message )
		{
			mActivitySummary.receive( (P25Message)message );

			if( message instanceof HDUMessage )
			{
				/* Header Data Unit Message - preceeds voice LDUs */
				processHDU( (HDUMessage)message );
			}
			if( message instanceof LDUMessage )
			{
				/* Voice Vocoder messages */
				processLDU( (LDUMessage)message );
			}
			else if( message instanceof PDUMessage )
			{
				/* Packet Data Unit Messages */
				processPDU( (PDUMessage)message );
			}
			else if( message instanceof TDULinkControlMessage )
			{
				/* Terminator Data Unit with Link Control Message */
				processTDULC( (TDULinkControlMessage)message );
			}
			else if( message instanceof TSBKMessage )
			{
				/* Trunking Signalling Block Messages - indicates Control Channel */
				setState( State.CONTROL );

				processTSBK( (TSBKMessage)message );
			}
		}
	}

	private void processHDU( HDUMessage hdu )
	{
		
	}
	
	private void processTDULC( TDULinkControlMessage tdulc )
	{
		switch( tdulc.getOpcode() )
		{
			case CALL_ALERT:
				decode.p25.message.tdu.lc.CallAlert ca = 
							(decode.p25.message.tdu.lc.CallAlert)tdulc;
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.PAGE )
							.aliasList( mAliasList )
							.from( ca.getSourceAddress() )
							.to( ca.getTargetAddress() )
							.details( "CALL ALERT" )
							.build() );
				break;
			case CALL_TERMINATION_OR_CANCELLATION:
				//TODO: terminate all active calls for this channel state
				break;
			case EXTENDED_FUNCTION_COMMAND:
				decode.p25.message.tdu.lc.ExtendedFunctionCommand efc = 
					(decode.p25.message.tdu.lc.ExtendedFunctionCommand)tdulc;
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.to( efc.getTargetAddress() )
							.details( "FUNCTION:" + efc.getExtendedFunction().getLabel() + 
									  " ARG:" + efc.getArgument() )
						  .build() );
				break;
			case GROUP_AFFILIATION_QUERY:
				decode.p25.message.tdu.lc.GroupAffiliationQuery gaq = 
						(decode.p25.message.tdu.lc.GroupAffiliationQuery)tdulc;
				mCallEventModel.add( 
					new P25CallEvent.Builder( CallEventType.QUERY )
						.aliasList( mAliasList )
						.details( "GROUP AFFILIATION QUERY" )
						.from( gaq.getSourceAddress() )
						.to( gaq.getTargetAddress() )
						.build() );
				break;
			case GROUP_VOICE_CHANNEL_UPDATE:
				//TODO: process call here
				break;
			case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
				//TODO: process call here
				break;
			case GROUP_VOICE_CHANNEL_USER:
				if( !( tdulc instanceof decode.p25.message.tdu.lc.GroupVoiceChannelUser ) )
				{
					mLog.error( "TDULC message has opcode of group voice channel user but is not instance " + tdulc.toString() + " " + tdulc.getBinaryMessage().toString() );
				}
				else
				{
					decode.p25.message.tdu.lc.GroupVoiceChannelUser gvcuser = 
							(decode.p25.message.tdu.lc.GroupVoiceChannelUser)tdulc;
					
					if( isActiveCall( CURRENT_CHANNEL, gvcuser.getGroupAddress() ) )
					{
						if( gvcuser.getFromID() != null &&
							!gvcuser.getFromID().contentEquals( "0000" ) &&
							!gvcuser.getFromID().contentEquals( "000000" ) )
						{
							updateCall( CURRENT_CHANNEL, gvcuser.getSourceAddress(), 
									gvcuser.getGroupAddress() );
						}
						else
						{
							updateCall( CURRENT_CHANNEL, null, gvcuser.getGroupAddress() );
						}
					}
					else
					{
						CallEvent gvcuserEvent = 
								new P25CallEvent.Builder( CallEventType.CALL )
									.aliasList( mAliasList )
									.channel( CURRENT_CHANNEL )
									.details( ( gvcuser.isEncrypted() ? "ENCRYPTED" : "" ) + 
											  ( gvcuser.isEmergency() ? " EMERGENCY" : "") )
								    .from( gvcuser.getSourceAddress() )
									.to( gvcuser.getGroupAddress() )
									.build();
						
						mCallEventModel.add( gvcuserEvent );
						
						addCall( gvcuserEvent );
						
						setState( State.CALL );
					}
				}
				break;
			case MESSAGE_UPDATE:
				decode.p25.message.tdu.lc.MessageUpdate mu = 
								(decode.p25.message.tdu.lc.MessageUpdate)tdulc;
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.SDM )
							.aliasList( mAliasList )
							.from( mu.getSourceAddress() )
							.to( mu.getTargetAddress() )
							.details( "MSG: " + mu.getShortDataMessage() )
							.build() );
				break;
			case PROTECTION_PARAMETER_BROADCAST:
				decode.p25.message.tdu.lc.ProtectionParameterBroadcast ppb = 
				(decode.p25.message.tdu.lc.ProtectionParameterBroadcast)tdulc;
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.to( ppb.getTargetAddress() )
							.details( "ENCRYPTION: " + 
									ppb.getEncryption().name() + " KEY:" + 
									ppb.getEncryptionKey() )
							.build() );
				break;
			case STATUS_QUERY:
				decode.p25.message.tdu.lc.StatusQuery sq = 
						(decode.p25.message.tdu.lc.StatusQuery)tdulc;
				mCallEventModel.add( 
					new P25CallEvent.Builder( CallEventType.QUERY )
						.aliasList( mAliasList )
						.details( "STATUS QUERY" )
						.from( sq.getSourceAddress() )
						.to( sq.getTargetAddress() )
						.build() );
				break;
			case STATUS_UPDATE:
				decode.p25.message.tdu.lc.StatusUpdate su = 
						(decode.p25.message.tdu.lc.StatusUpdate)tdulc;
				mCallEventModel.add( 
					new P25CallEvent.Builder( CallEventType.STATUS )
						.aliasList( mAliasList )
						.details( "STATUS UNIT:" + su.getUnitStatus() + 
								  " USER:" + su.getUserStatus() )
						.from( su.getSourceAddress() )
						.to( su.getTargetAddress() )
						.build() );
				break;
			case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
				decode.p25.message.tdu.lc.TelephoneInterconnectAnswerRequest tiar = 
				(decode.p25.message.tdu.lc.TelephoneInterconnectAnswerRequest)tdulc;
				mCallEventModel.add( 
					new P25CallEvent.Builder( CallEventType.PAGE )
						.aliasList( mAliasList )
						.from( tiar.getTelephoneNumber() )
						.to( tiar.getTargetAddress() )
						.details( "TELEPHONE CALL ALERT" )
						.build() );
				break;
			case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
				//TODO: process call
				break;
			case UNIT_AUTHENTICATION_COMMAND:
				decode.p25.message.tdu.lc.UnitAuthenticationCommand uac = 
					(decode.p25.message.tdu.lc.UnitAuthenticationCommand)tdulc;
				mCallEventModel.add( 
					new P25CallEvent.Builder( CallEventType.COMMAND )
						.aliasList( mAliasList )
						.to( uac.getCompleteTargetAddress() )
						.details( "AUTHENTICATE" )
						.build() );
				break;
			case UNIT_REGISTRATION_COMMAND:
				decode.p25.message.tdu.lc.UnitRegistrationCommand urc = 
				(decode.p25.message.tdu.lc.UnitRegistrationCommand)tdulc;
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.to( urc.getCompleteTargetAddress() )
							.details( "REGISTER" )
							.build() );
				break;
			case UNIT_TO_UNIT_ANSWER_REQUEST:
				decode.p25.message.tdu.lc.UnitToUnitAnswerRequest uuar = 
				(decode.p25.message.tdu.lc.UnitToUnitAnswerRequest)tdulc;
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.PAGE )
							.aliasList( mAliasList )
							.from( uuar.getSourceAddress() )
							.to( uuar.getTargetAddress() )
							.details( "UNIT TO UNIT CALL ALERT" )
							.build() );
				break;
			case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
				//TODO: process call here
				break;
			case ADJACENT_SITE_STATUS_BROADCAST:
			case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
			case CHANNEL_IDENTIFIER_UPDATE:
			case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
			case NETWORK_STATUS_BROADCAST:
			case NETWORK_STATUS_BROADCAST_EXPLICIT:
			case RFSS_STATUS_BROADCAST:
			case RFSS_STATUS_BROADCAST_EXPLICIT:
			case SECONDARY_CONTROL_CHANNEL_BROADCAST:
			case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
			case SYSTEM_SERVICE_BROADCAST:
			default:
				break;
		}
	}
	
	private void processLDU( LDUMessage ldu )
	{
		if( ldu instanceof LDU1Message )
		{
			switch( ((LDU1Message)ldu).getOpcode() )
			{
				case CALL_ALERT:
					decode.p25.message.ldu.lc.CallAlert ca = 
								(decode.p25.message.ldu.lc.CallAlert)ldu;
					mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.PAGE )
								.aliasList( mAliasList )
								.from( ca.getSourceAddress() )
								.to( ca.getTargetAddress() )
								.details( "CALL ALERT" )
								.build() );
					break;
				case CALL_TERMINATION_OR_CANCELLATION:
					//TODO: terminate all active calls for this channel state
					break;
				case EXTENDED_FUNCTION_COMMAND:
					decode.p25.message.ldu.lc.ExtendedFunctionCommand efc = 
						(decode.p25.message.ldu.lc.ExtendedFunctionCommand)ldu;
					mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.COMMAND )
								.aliasList( mAliasList )
								.to( efc.getTargetAddress() )
								.details( "FUNCTION:" + efc.getExtendedFunction().getLabel() + 
										  " ARG:" + efc.getArgument() )
							  .build() );
					break;
				case GROUP_AFFILIATION_QUERY:
					decode.p25.message.ldu.lc.GroupAffiliationQuery gaq = 
							(decode.p25.message.ldu.lc.GroupAffiliationQuery)ldu;
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.details( "GROUP AFFILIATION QUERY" )
							.from( gaq.getSourceAddress() )
							.to( gaq.getTargetAddress() )
							.build() );
					break;
				case GROUP_VOICE_CHANNEL_UPDATE:
					//TODO: process call here
					break;
				case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
					//TODO: process call here
					break;
				case GROUP_VOICE_CHANNEL_USER:
					//TODO: process call here
					break;
				case MESSAGE_UPDATE:
					decode.p25.message.ldu.lc.MessageUpdate mu = 
									(decode.p25.message.ldu.lc.MessageUpdate)ldu;
					mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.SDM )
								.aliasList( mAliasList )
								.from( mu.getSourceAddress() )
								.to( mu.getTargetAddress() )
								.details( "MSG: " + mu.getShortDataMessage() )
								.build() );
					break;
				case PROTECTION_PARAMETER_BROADCAST:
					decode.p25.message.ldu.lc.ProtectionParameterBroadcast ppb = 
					(decode.p25.message.ldu.lc.ProtectionParameterBroadcast)ldu;
					mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.COMMAND )
								.aliasList( mAliasList )
								.to( ppb.getTargetAddress() )
								.details( "ENCRYPTION: " + 
										ppb.getEncryption().name() + " KEY:" + 
										ppb.getEncryptionKey() )
								.build() );
					break;
				case STATUS_QUERY:
					decode.p25.message.ldu.lc.StatusQuery sq = 
							(decode.p25.message.ldu.lc.StatusQuery)ldu;
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.details( "STATUS QUERY" )
							.from( sq.getSourceAddress() )
							.to( sq.getTargetAddress() )
							.build() );
					break;
				case STATUS_UPDATE:
					decode.p25.message.ldu.lc.StatusUpdate su = 
							(decode.p25.message.ldu.lc.StatusUpdate)ldu;
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.STATUS )
							.aliasList( mAliasList )
							.details( "STATUS UNIT:" + su.getUnitStatus() + 
									  " USER:" + su.getUserStatus() )
							.from( su.getSourceAddress() )
							.to( su.getTargetAddress() )
							.build() );
					break;
				case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
					decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest tiar = 
					(decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest)ldu;
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.PAGE )
							.aliasList( mAliasList )
							.from( tiar.getTelephoneNumber() )
							.to( tiar.getTargetAddress() )
							.details( "TELEPHONE CALL ALERT" )
							.build() );
					break;
				case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
					//TODO: process call
					break;
				case UNIT_AUTHENTICATION_COMMAND:
					decode.p25.message.ldu.lc.UnitAuthenticationCommand uac = 
						(decode.p25.message.ldu.lc.UnitAuthenticationCommand)ldu;
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.to( uac.getCompleteTargetAddress() )
							.details( "AUTHENTICATE" )
							.build() );
					break;
				case UNIT_REGISTRATION_COMMAND:
					decode.p25.message.ldu.lc.UnitRegistrationCommand urc = 
					(decode.p25.message.ldu.lc.UnitRegistrationCommand)ldu;
						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.COMMAND )
								.aliasList( mAliasList )
								.to( urc.getCompleteTargetAddress() )
								.details( "REGISTER" )
								.build() );
					break;
				case UNIT_TO_UNIT_ANSWER_REQUEST:
					decode.p25.message.ldu.lc.UnitToUnitAnswerRequest uuar = 
					(decode.p25.message.ldu.lc.UnitToUnitAnswerRequest)ldu;
						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.PAGE )
								.aliasList( mAliasList )
								.from( uuar.getSourceAddress() )
								.to( uuar.getTargetAddress() )
								.details( "UNIT TO UNIT CALL ALERT" )
								.build() );
					break;
				case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
					//TODO: process call here
					break;
				case ADJACENT_SITE_STATUS_BROADCAST:
				case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
				case CHANNEL_IDENTIFIER_UPDATE:
				case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
				case NETWORK_STATUS_BROADCAST:
				case NETWORK_STATUS_BROADCAST_EXPLICIT:
				case RFSS_STATUS_BROADCAST:
				case RFSS_STATUS_BROADCAST_EXPLICIT:
				case SECONDARY_CONTROL_CHANNEL_BROADCAST:
				case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
				case SYSTEM_SERVICE_BROADCAST:
				default:
					break;
			}
		}
		else if( ldu instanceof LDU2Message )
		{
			
		}
		
	}
	
	/**
	 * Process a Trunking Signalling Block message 
	 */
	private void processTSBK( TSBKMessage tsbk )
	{
		if( tsbk.getVendor() == Vendor.STANDARD )
		{
			switch( tsbk.getOpcode() )
			{
				case ACKNOWLEDGE_RESPONSE:
					processTSBKResponse( tsbk );
					break;
				case AUTHENTICATION_COMMAND:
					processTSBKCommand( tsbk );
					break;
				case CALL_ALERT:
					processTSBKCall( tsbk );
					break;
				case DENY_RESPONSE:
					processTSBKResponse( tsbk );
					break;
				case EXTENDED_FUNCTION_COMMAND:
					processTSBKCommand( tsbk );
					break;
				case GROUP_AFFILIATION_QUERY:
					processTSBKQuery( tsbk );
					break;
				case GROUP_AFFILIATION_RESPONSE:
					processTSBKResponse( tsbk );
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
					processTSBKCall( tsbk );
					break;
				case LOCATION_REGISTRATION_RESPONSE:
				case UNIT_DEREGISTRATION_ACKNOWLEDGE:
					processTSBKResponse( tsbk );
					break;
				case MESSAGE_UPDATE:
					processTSBKMessage( tsbk );
					break;
				case PROTECTION_PARAMETER_UPDATE:
					processTSBKResponse( tsbk );
					break;
				case QUEUED_RESPONSE:
					processTSBKResponse( tsbk );
					break;
				case RADIO_UNIT_MONITOR_COMMAND:
					processTSBKCommand( tsbk );
					break;
				case RFSS_STATUS_BROADCAST:
					processTSBKRFSSStatus( (RFSSStatusBroadcast)tsbk );
					break;
				case ROAMING_ADDRESS_COMMAND:
					processTSBKCommand( tsbk );
					break;
				case SNDCP_DATA_CHANNEL_GRANT:
					processTSBKCall( tsbk );
					break;
				case STATUS_QUERY:
					processTSBKQuery( tsbk );
					break;
				case STATUS_UPDATE:
					processTSBKResponse( tsbk );
					break;
				case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
				case UNIT_TO_UNIT_ANSWER_REQUEST:
					processTSBKPage( tsbk );
					break;
				case UNIT_REGISTRATION_COMMAND:
					processTSBKCommand( tsbk );
					break;
				case UNIT_REGISTRATION_RESPONSE:
					processTSBKResponse( tsbk );
					break;
				default:
					break;
			}
		}
	}
	
	/**
	 * Process a Packet Data Unit message 
	 */
	private void processPDU( PDUMessage message )
	{
		switch( message.getOpcode() )
		{
			case CALL_ALERT:
				processPDUCall( message );
				break;
			case GROUP_AFFILIATION_QUERY:
				processQuery( message );
				break;
			case GROUP_AFFILIATION_RESPONSE:
				processResponse( message );
				break;
			case GROUP_DATA_CHANNEL_GRANT:
			case GROUP_VOICE_CHANNEL_GRANT:
			case INDIVIDUAL_DATA_CHANNEL_GRANT:
				processPDUCall( message );
				break;
			case MESSAGE_UPDATE:
				processMessage( message );
				break;
			case RFSS_STATUS_BROADCAST:
				processRFSSStatus( (RFSSStatusBroadcastExtended)message );
				break;
			case ROAMING_ADDRESS_UPDATE:
				processResponse( message );
				break;
			case STATUS_QUERY:
				processQuery( message );
				break;
			case STATUS_UPDATE:
				processResponse( message );
				break;
			case UNIT_TO_UNIT_ANSWER_REQUEST:
				processPage( message );
				break;
			case UNIT_REGISTRATION_RESPONSE:
				processResponse( message );
				break;
			default:
				break;
		}
	}
	
	private void processTSBKCommand( TSBKMessage message )
	{
		CallEvent event = null;
		
		switch( message.getOpcode() )
		{
			case AUTHENTICATION_COMMAND:
				AuthenticationCommand ac = (AuthenticationCommand)message;
				
				if( mLastCommandEventID == null || !mLastCommandEventID
						.contentEquals( ac.getFullTargetID() ))
				{
					event = new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.details( "AUTHENTICATE" )
							.to( ac.getWACN() + "-" + ac.getSystemID() + "-" +
							     ac.getTargetID() )
							.build();	
					
					mLastCommandEventID = ac.getFullTargetID();
				}
				break;
			case EXTENDED_FUNCTION_COMMAND:
				ExtendedFunctionCommand efc = (ExtendedFunctionCommand)message;

				if( mLastCommandEventID == null || !mLastCommandEventID
						.contentEquals( efc.getTargetAddress() ) )
				{
					event = new P25CallEvent.Builder( CallEventType.FUNCTION )
					.aliasList( mAliasList )
					.details( "EXTENDED FUNCTION: " + 
							efc.getExtendedFunction().getLabel() )
					.from( efc.getSourceAddress() )
					.to( efc.getTargetAddress() )
					.build();	
					
					mLastCommandEventID = efc.getTargetAddress();
				}
				break;
			case RADIO_UNIT_MONITOR_COMMAND:
				RadioUnitMonitorCommand rumc = (RadioUnitMonitorCommand)message;
				
				if( mLastCommandEventID == null || !mLastCommandEventID
						.contentEquals( rumc.getTargetAddress() ) )
				{
					event = new P25CallEvent.Builder( CallEventType.COMMAND )
					.aliasList( mAliasList )
					.details( "RADIO UNIT MONITOR" )
					.from( rumc.getSourceAddress() )
					.to( rumc.getTargetAddress() )
					.build();
					
					mLastCommandEventID = rumc.getTargetAddress();
				}
				break;
			case ROAMING_ADDRESS_COMMAND:
				RoamingAddressCommand rac = (RoamingAddressCommand)message;
				
				if( mLastCommandEventID == null || !mLastCommandEventID
						.contentEquals( rac.getTargetID() ) )
				{
					event = new P25CallEvent.Builder( CallEventType.COMMAND )
						.aliasList( mAliasList )
						.details( rac.getStackOperation().name() +
								" ROAMING ADDRESS " + rac.getWACN() + "-" + 
								rac.getSystemID() )
						.to( rac.getTargetID() )
						.build();	
					
					mLastCommandEventID = rac.getTargetID();
				}
				break;
			case UNIT_REGISTRATION_COMMAND:
				UnitRegistrationCommand urc = (UnitRegistrationCommand)message;
				
				if( mLastCommandEventID == null || !mLastCommandEventID
						.contentEquals( urc.getTargetAddress() ) )
				{
					event = new P25CallEvent.Builder( CallEventType.COMMAND )
						.aliasList( mAliasList )
						.details( "REGISTER" )
						.from( urc.getSourceAddress() )
						.to( urc.getTargetAddress() )
						.build();	
					
					mLastCommandEventID = urc.getTargetAddress();
				}
				break;
			default:
				break;
		}
		
		if( event != null )
		{
			mCallEventModel.add( event );
		}
	}
	
	private void processTSBKMessage( TSBKMessage message )
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
	
	private void processTSBKQuery( TSBKMessage message )
	{
		CallEvent event = null;

		switch( message.getOpcode() )
		{
			case GROUP_AFFILIATION_QUERY:
				GroupAffiliationQuery gaq = (GroupAffiliationQuery)message;

				if( mLastQueryEventID == null || !mLastQueryEventID
						.contentEquals( gaq.getTargetAddress() ) )
				{
					event = new P25CallEvent.Builder( CallEventType.QUERY )
					.aliasList( mAliasList )
					.details( "GROUP AFFILIATION" )
					.from( gaq.getSourceAddress() )
					.to( gaq.getTargetAddress() )
					.build();	
				}
				break;
			case STATUS_QUERY:
				StatusQuery sq = (StatusQuery)message;
				
				if( mLastQueryEventID == null || !mLastQueryEventID
						.contentEquals( sq.getTargetAddress() ) )
				{
					mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.QUERY )
								.aliasList( mAliasList )
								.details( "STATUS QUERY" )
								.from( sq.getSourceAddress() )
								.to( sq.getTargetAddress() )
								.build() );	
				}
				break;
			default:
				break;
		}
		
		if( event != null )
		{
			mCallEventModel.add( event );
			
			mLastQueryEventID = event.getToID();
		}
	}
	
	private void processQuery( PDUMessage message )
	{
		CallEvent event = null;
		
		switch( message.getOpcode() )
		{
			case GROUP_AFFILIATION_QUERY:
				GroupAffiliationQueryExtended gaqe = 
							(GroupAffiliationQueryExtended)message;
				
				if( mLastQueryEventID == null || !gaqe.getTargetAddress()
						.contentEquals( mLastQueryEventID ) )
				{
					event = new P25CallEvent.Builder( CallEventType.QUERY )
					.aliasList( mAliasList )
					.details( "GROUP AFFILIATION" )
					.from( gaqe.getWACN() + "-" + gaqe.getSystemID() + 
							"-" + gaqe.getSourceID() )
					.to( gaqe.getTargetAddress() )
					.build();	
				}
				break;
			case STATUS_QUERY:
				StatusQueryExtended sq = (StatusQueryExtended)message;
				
				if( mLastQueryEventID == null || !sq.getTargetAddress()
						.contentEquals( mLastQueryEventID ) )
				{
					event = new P25CallEvent.Builder( CallEventType.QUERY )
					.aliasList( mAliasList )
					.details( "STATUS QUERY" )
					.from( sq.getSourceWACN() + "-" + 
						   sq.getSourceSystemID() + "-" + 
						   sq.getSourceID() )
					.to( sq.getTargetAddress() )
					.build();	
				}
				break;
			default:
				break;
		}

		if( event != null )
		{
			mCallEventModel.add( event );
			
			mLastQueryEventID = event.getToID();
		}
	}
	
	private void processTSBKResponse( TSBKMessage message )
	{
		CallEvent event = null;
		
		switch( message.getOpcode() )
		{
			case ACKNOWLEDGE_RESPONSE:
				AcknowledgeResponse ar = (AcknowledgeResponse)message;
				
				if( mLastResponseEventID == null || !ar.getTargetAddress()
						.contentEquals( mLastResponseEventID ) )
				{
					event =  new P25CallEvent.Builder( CallEventType.RESPONSE )
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
					
					mLastResponseEventID = ar.getTargetAddress();
				}
				break;
			case DENY_RESPONSE:
				DenyResponse dr = (DenyResponse)message;
				
				if( mLastResponseEventID == null || !dr.getTargetAddress()
						.contentEquals( mLastResponseEventID ) )
				{
					event = new P25CallEvent.Builder( CallEventType.RESPONSE )
								.aliasList( mAliasList )
								.details( "DENY REASON: " + dr.getReason().name() + 
										  " REQUESTED: " + dr.getServiceType().name() )
								.from( dr.getSourceAddress() )
								.to( dr.getTargetAddress() )
								.build();	
					
					mLastResponseEventID = dr.getTargetAddress();
				}
				break;
			case GROUP_AFFILIATION_RESPONSE:
				GroupAffiliationResponse gar = (GroupAffiliationResponse)message;
				
				if( mLastResponseEventID == null || !gar.getTargetAddress()
						.contentEquals( mLastResponseEventID ) )
				{
					event = new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( "AFFILIATION:" + gar.getResponse().name() + 
									  " FOR " + gar.getAffiliationScope() + 
									  " GROUP:" + gar.getGroupAddress() + 
									  " ANNOUNCEMENT GROUP:" + 
									  gar.getAnnouncementGroupAddress() )
							.to( gar.getTargetAddress() )
							.build();	

					mLastResponseEventID = gar.getTargetAddress();
				}
				break;
			case LOCATION_REGISTRATION_RESPONSE:
				LocationRegistrationResponse lrr = 
								(LocationRegistrationResponse)message;

				if( lrr.getResponse() == Response.ACCEPT )
				{
					mRegistrations.put( lrr.getTargetAddress(), 
										System.currentTimeMillis() );
				}

				if( mLastRegistrationEventID == null || 
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
					
					mLastRegistrationEventID = lrr.getTargetAddress();
				}
				break;
			case PROTECTION_PARAMETER_UPDATE:
				ProtectionParameterUpdate ppu = (ProtectionParameterUpdate)message;
				
				if( mLastResponseEventID == null || !ppu.getTargetAddress()
						.contentEquals( mLastResponseEventID ) )
				{
					event = new P25CallEvent.Builder( CallEventType.RESPONSE )
					.aliasList( mAliasList )
					.details( "USE ENCRYPTION ALGORITHM:" + 
						ppu.getAlgorithm().name() + " KEY:" + 
						ppu.getKeyID() )
					.to( ppu.getTargetAddress() )
					.build();	
					
					mLastResponseEventID = ppu.getTargetAddress();
				}
				break;
			case QUEUED_RESPONSE:
				QueuedResponse qr = (QueuedResponse)message;
				
				if( mLastResponseEventID == null || !qr.getTargetAddress()
						.contentEquals( mLastResponseEventID ) )
				{
					event = new P25CallEvent.Builder( CallEventType.RESPONSE )
					.aliasList( mAliasList )
					.details( "QUEUED REASON: " + qr.getReason().name() + 
							  " REQUESTED: " + qr.getServiceType().name() )
					.from( qr.getSourceAddress() )
					.to( qr.getTargetAddress() )
					.build();	
					
					mLastResponseEventID = qr.getTargetAddress();
				}
				break;
			case STATUS_UPDATE:
				StatusUpdate su = (StatusUpdate)message;
				
				if( mLastResponseEventID == null || !su.getTargetAddress()
						.contentEquals( mLastResponseEventID ) )
				{
					event = new P25CallEvent.Builder( CallEventType.RESPONSE )
					.aliasList( mAliasList )
					.details( "STATUS USER: " + su.getUserStatus() + 
							  " UNIT: " + su.getUnitStatus() )
					.from( su.getSourceAddress() )
					.to( su.getTargetAddress() )
					.build();	
					
					mLastResponseEventID = su.getTargetAddress();
				}
				
				break;
			case UNIT_REGISTRATION_RESPONSE:
				UnitRegistrationResponse urr = (UnitRegistrationResponse)message;

				if( urr.getResponse() == Response.ACCEPT )
				{
					mRegistrations.put( urr.getSourceAddress(), 
										System.currentTimeMillis() );
				}

				if( mLastRegistrationEventID == null || 
					!mLastRegistrationEventID.contentEquals( urr.getSourceAddress() ) )
				{
					mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.REGISTER )
								.aliasList( mAliasList )
								.details( "REGISTRATION:" + urr.getResponse().name() +
										  " SYSTEM: " + urr.getSystemID() + 
										  " SOURCE ID: " + urr.getSourceID() )
								.from( urr.getSourceAddress() )
								.build() );
					
					mLastRegistrationEventID = urr.getSourceAddress();
				}
				break;
			case UNIT_DEREGISTRATION_ACKNOWLEDGE:
				UnitDeregistrationAcknowledge udr =
						(UnitDeregistrationAcknowledge)message;

				if( mLastRegistrationEventID == null || 
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

					mLastRegistrationEventID = udr.getSourceID();
				}
				break;
			default:
				break;
		}
		
		if( event != null )
		{
			mCallEventModel.add( event );
		}
	}
	
	private void processResponse( PDUMessage message )
	{
		CallEvent event = null;
		
		switch( message.getOpcode() )
		{
			case GROUP_AFFILIATION_RESPONSE:
				GroupAffiliationResponseExtended gar = 
							(GroupAffiliationResponseExtended)message;
				
				if( mLastResponseEventID == null || !gar.getTargetAddress()
						.contentEquals( mLastResponseEventID ) )
				{
					event = new P25CallEvent.Builder( CallEventType.RESPONSE )
						.aliasList( mAliasList )
						.details( "AFFILIATION:" + gar.getResponse().name() + 
								  " FOR GROUP:" + gar.getGroupWACN() + "-" +
								  gar.getGroupSystemID() + "-" + 
								  gar.getGroupID() + " ANNOUNCEMENT GROUP:" + 
								  gar.getAnnouncementGroupID() )
					    .from( gar.getSourceWACN() + "-" + 
							   gar.getSourceSystemID() + "-" + 
					    	   gar.getSourceID() )
						.to( gar.getTargetAddress() )
						.build();	
					
					mLastResponseEventID = gar.getTargetAddress();
				}
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
				
				event = new P25CallEvent.Builder( CallEventType.RESPONSE )
							.aliasList( mAliasList )
							.details( sb.toString() )
							.from( raue.getSourceID() )
							.to( raue.getTargetAddress() )
							.build();	
				break;
			case STATUS_UPDATE:
				StatusUpdateExtended su = (StatusUpdateExtended)message;

				if( mLastResponseEventID == null || !mLastResponseEventID
						.contentEquals( su.getTargetAddress() ) )
				{
					event = new P25CallEvent.Builder( CallEventType.RESPONSE )
					.aliasList( mAliasList )
					.details( "STATUS USER: " + su.getUserStatus() + 
							  " UNIT: " + su.getUnitStatus() )
					.from( su.getSourceWACN() + "-" + 
						   su.getSourceSystemID() + "-" + 
						   su.getSourceID() )
					.to( su.getTargetAddress() )
					.build();	
		
					mLastResponseEventID = su.getTargetAddress();
				}
				break;
			case UNIT_REGISTRATION_RESPONSE:
				UnitRegistrationResponseExtended urr = 
								(UnitRegistrationResponseExtended)message;

				if( urr.getResponse() == Response.ACCEPT )
				{
					mRegistrations.put( urr.getAssignedSourceAddress(), 
										System.currentTimeMillis() );
				}

				if( mLastRegistrationEventID == null || !mLastRegistrationEventID
						.contentEquals( urr.getAssignedSourceAddress() ) )
				{
					event = new P25CallEvent.Builder( CallEventType.REGISTER )
								.aliasList( mAliasList )
								.details( "REGISTRATION:" + urr.getResponse().name() +
										  " FOR EXTERNAL SYSTEM ADDRESS: " +
										  urr.getWACN() + "-" + 
										  urr.getSystemID() + "-" +
										  urr.getSourceAddress() +
										  " SOURCE ID: " + urr.getSourceID() )
								.from( urr.getAssignedSourceAddress() )
								.build();
					
					mLastRegistrationEventID = urr.getAssignedSourceAddress();
				}
				break;
			default:
				break;
		}
		
		if( event != null )
		{
			mCallEventModel.add( event );
		}
	}
	
	private void processTSBKRFSSStatus( RFSSStatusBroadcast message )
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
	
	private void processPDUCall( PDUMessage message )
	{
		switch( message.getOpcode() )
		{
			case CALL_ALERT:
				CallAlertExtended ca = (CallAlertExtended)message;
				
				mCallEventModel.add(
						new P25CallEvent.Builder( CallEventType.PAGE )
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
					updateCall( gdcge.getTransmitChannel(), null, gdcge.getGroupAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( gdcge.getTransmitChannel() )
								.details( ( gdcge.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( gdcge.isEmergency() ? " EMERGENCY" : "") )
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
					updateCall( gvcge.getTransmitChannel(), null, gvcge.getGroupAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( gvcge.getTransmitChannel() )
								.details( ( gvcge.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( gvcge.isEmergency() ? " EMERGENCY" : "") )
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
					updateCall( idcge.getTransmitChannel(), null, idcge.getTargetAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( idcge.getTransmitChannel() )
								.details( ( idcge.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( idcge.isEmergency() ? " EMERGENCY" : "") )
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
					updateCall( ticge.getTransmitChannel(), null, ticge.getAddress() );
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
					updateCall( uuvcge.getTransmitChannel(), null, uuvcge.getTargetAddress() );
				}
				else
				{
					CallEvent event = 
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( uuvcge.getTransmitChannel() )
								.details( ( uuvcge.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( uuvcge.isEmergency() ? " EMERGENCY" : "") )
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
					updateCall( uuvcgue.getTransmitChannel(), null,
								uuvcgue.getTargetAddress() );
				}
				else
				{
					CallEvent event = 
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( uuvcgue.getTransmitChannel() )
								.details( ( uuvcgue.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( uuvcgue.isEmergency() ? " EMERGENCY" : "") )
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
	private void processTSBKCall( TSBKMessage message )
	{
		switch( message.getOpcode() )
		{
			case CALL_ALERT:
				CallAlert ca = (CallAlert)message;
				
				mCallEventModel.add(
						new P25CallEvent.Builder( CallEventType.PAGE )
							.aliasList( mAliasList )
							.from( ca.getSourceID() )
							.to( ca.getTargetAddress() )
							.build() );	
				break;
			case GROUP_DATA_CHANNEL_ANNOUNCEMENT:
				GroupDataChannelAnnouncement gdca = (GroupDataChannelAnnouncement)message;

				if( isActiveCall( gdca.getChannel1(), gdca.getGroupAddress1() ) )
				{
					updateCall( gdca.getChannel1(), null, gdca.getGroupAddress1() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( gdca.getChannel1() )
								.details( ( gdca.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( gdca.isEmergency() ? " EMERGENCY" : "") )
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
						updateCall( gdca.getChannel2(), null, gdca.getGroupAddress2() );
					}
					else
					{
						CallEvent event =  
								new P25CallEvent.Builder( CallEventType.DATA_CALL )
									.aliasList( mAliasList )
									.channel( gdca.getChannel2() )
									.details( ( gdca.isEncrypted() ? "ENCRYPTED" : "" ) + 
											  ( gdca.isEmergency() ? " EMERGENCY" : "") )
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
					updateCall( gdcae.getTransmitChannel(), null, gdcae.getGroupAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( gdcae.getTransmitChannel() )
								.details( ( gdcae.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( gdcae.isEmergency() ? " EMERGENCY" : "") )
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
					updateCall( gdcg.getChannel(), null, gdcg.getGroupAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( gdcg.getChannel() )
								.details( ( gdcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( gdcg.isEmergency() ? " EMERGENCY" : "") )
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
					updateCall( gvcg.getChannel(), null, gvcg.getGroupAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( gvcg.getChannel() )
								.details( ( gvcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( gvcg.isEmergency() ? " EMERGENCY" : "") )
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
					updateCall( gvcgu.getChannel1(), null, gvcgu.getGroupAddress1() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( gvcgu.getChannel1() )
								.details( ( gvcgu.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( gvcgu.isEmergency() ? " EMERGENCY" : "") )
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
						updateCall( gvcgu.getChannel2(), null, gvcgu.getGroupAddress2() );
					}
					else
					{
						CallEvent event = 
								new P25CallEvent.Builder( CallEventType.CALL )
									.aliasList( mAliasList )
									.channel( gvcgu.getChannel2() )
									.details( ( gvcgu.isEncrypted() ? "ENCRYPTED" : "" ) + 
											  ( gvcgu.isEmergency() ? " EMERGENCY" : "") )
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
						.details( ( gvcgue.isEncrypted() ? "ENCRYPTED" : "" ) + 
								  ( gvcgue.isEmergency() ? " EMERGENCY" : "") )
					    .frequency( gvcgue.getDownlinkFrequency() )
						.to( gvcgue.getGroupAddress() )
						.build() );
				break;
			case INDIVIDUAL_DATA_CHANNEL_GRANT:
				IndividualDataChannelGrant idcg = (IndividualDataChannelGrant)message;

				if( isActiveCall( idcg.getChannel(), idcg.getTargetAddress() ) )
				{
					updateCall( idcg.getChannel(), null, idcg.getTargetAddress() );
				}
				else
				{
					CallEvent event =  
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( idcg.getChannel() )
								.details( ( idcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( idcg.isEmergency() ? " EMERGENCY" : "") )
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
				
				if( isActiveCall( sdcg.getTransmitChannel(), sdcg.getTargetAddress() ) )
				{
					updateCall( sdcg.getTransmitChannel(), null, sdcg.getTargetAddress() );
				}
				else
				{
					CallEvent event = 
							new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( sdcg.getTransmitChannel() )
								.details( "SNDCP DATA NSAPI:" + sdcg.getNSAPI() )
							    .frequency( sdcg.getDownlinkFrequency() )
							    .to( sdcg.getTargetAddress() )
								.build();

					mCallEventModel.add( event );
					
					addCall( event );
				}
				break;
			case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
				TelephoneInterconnectVoiceChannelGrant tivcg =
							(TelephoneInterconnectVoiceChannelGrant)message;
				
				if( isActiveCall( tivcg.getChannel(), tivcg.getAddress() ))
				{
					updateCall( tivcg.getChannel(), null, tivcg.getAddress() );
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
					updateCall( tivcgu.getChannel(), null, tivcgu.getAddress() );
				}
				else
				{
					CallEvent tivcguEvent = 
							new P25CallEvent.Builder( CallEventType.TELEPHONE_CALL )
								.aliasList( mAliasList )
								.channel( tivcgu.getChannelIdentifier() + "-" + 
										tivcgu.getChannelNumber() )
								.details( ( tivcgu.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( tivcgu.isEmergency() ? " EMERGENCY" : "") )
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
					updateCall( uuvcg.getChannel(), null, uuvcg.getTargetAddress() );
				}
				else
				{
					CallEvent uuvcgEvent = 
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( uuvcg.getChannelIdentifier() + "-" + 
										uuvcg.getChannelNumber() )
								.details( ( uuvcg.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( uuvcg.isEmergency() ? " EMERGENCY" : "") )
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
					updateCall( uuvcgu.getChannel(), null, uuvcgu.getTargetAddress() );
				}
				else
				{
					CallEvent uuvcguEvent = 
							new P25CallEvent.Builder( CallEventType.CALL )
								.aliasList( mAliasList )
								.channel( uuvcgu.getChannelIdentifier() + "-" + 
										uuvcgu.getChannelNumber() )
								.details( ( uuvcgu.isEncrypted() ? "ENCRYPTED" : "" ) + 
										  ( uuvcgu.isEmergency() ? " EMERGENCY" : "") )
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
	
	private void updateCall( String channel, String fromID, String toID )
	{
		for( ActiveCall call: mActiveCalls )
		{
			if( call.matches( channel, toID ) )
			{
				call.update( System.currentTimeMillis() );
				
				setState( getState() );
				
				if( fromID != null && 
					!call.getCallEvent().getFromID().contentEquals( fromID ) )
				{
					call.getCallEvent().setFromID( fromID );
				}
				
				return;
			}
		}
	}

	/**
	 * Process a unit paging event message
	 */
	private void processTSBKPage( TSBKMessage message )
	{
		CallEvent event = null;
		
		switch( message.getOpcode() )
		{
			case UNIT_TO_UNIT_ANSWER_REQUEST:
				UnitToUnitAnswerRequest utuar = (UnitToUnitAnswerRequest)message;
				
				if( mLastPageEventID == null || !mLastPageEventID
						.contentEquals( utuar.getTargetAddress() ) )
				{
					event = new P25CallEvent.Builder( CallEventType.PAGE )
							.aliasList( mAliasList )
							.details( ( utuar.isEmergency() ? "EMERGENCY" : "" ) ) 
							.from( utuar.getSourceAddress() )
							.to( utuar.getTargetAddress() )
							.build();
					
					mLastPageEventID = utuar.getTargetAddress();
				}
				break;
			case SNDCP_DATA_PAGE_REQUEST:
				SNDCPDataPageRequest sdpr = (SNDCPDataPageRequest)message;
				
				if( mLastPageEventID == null || !mLastPageEventID
						.contentEquals( sdpr.getTargetAddress() ) )
				{
					event = new P25CallEvent.Builder( CallEventType.PAGE )
							.aliasList( mAliasList )
							.details( "SNDCP DATA DAC: " + 
									sdpr.getDataAccessControl() + 
									" NSAPI:" + sdpr.getNSAPI() ) 
							.to( sdpr.getTargetAddress() )
							.build();
					
					mLastPageEventID = sdpr.getTargetAddress();
				}
				break;
			case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
				TelephoneInterconnectAnswerRequest tiar = 
						(TelephoneInterconnectAnswerRequest)message;

				if( mLastPageEventID == null || !mLastPageEventID
						.contentEquals( tiar.getTargetAddress() ) )
				{
					event = new P25CallEvent.Builder( CallEventType.PAGE )
						.aliasList( mAliasList )
						.details( ( "TELEPHONE INTERCONNECT" ) ) 
						.from( tiar.getTelephoneNumber() )
						.to( tiar.getTargetAddress() )
						.build();
					
					mLastPageEventID = tiar.getTargetAddress();
				}
				break;
			default:
				break;
		}
		
		if( event != null )
		{
			mCallEventModel.add( event );
		}
	}
	
	private void processPage( PDUMessage message )
	{
		if( message.getOpcode() == Opcode.UNIT_TO_UNIT_ANSWER_REQUEST )
		{
			UnitToUnitAnswerRequestExplicit utuare = 
					(UnitToUnitAnswerRequestExplicit)message;

			if( mLastPageEventID == null || !mLastPageEventID
					.contentEquals( utuare.getTargetAddress() ) )
			{
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.PAGE )
						.aliasList( mAliasList )
						.details( ( utuare.isEmergency() ? "EMERGENCY" : "" ) ) 
						.from( utuare.getWACN() + "-" + 
							   utuare.getSystemID() + "-" + 
							   utuare.getSourceID() )
						.to( utuare.getTargetAddress() )
						.build() );
				
				mLastPageEventID = utuare.getTargetAddress();
			}
		}
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
				
//				if( mEvent.getChannel().contentEquals( CURRENT_CHANNEL ) )
//				{
//					setState( State.IDLE );
//				}
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
