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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.config.SourceConfigTuner;
import source.config.SourceConfiguration;
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
import decode.p25.message.ldu.LDUMessage;
import decode.p25.message.ldu.lc.CallTermination;
import decode.p25.message.ldu.lc.TelephoneInterconnectVoiceChannelUser;
import decode.p25.message.ldu.lc.UnitToUnitVoiceChannelUser;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.message.pdu.confirmed.PDUConfirmedMessage;
import decode.p25.message.pdu.confirmed.SNDCPActivateTDSContextAccept;
import decode.p25.message.pdu.confirmed.SNDCPActivateTDSContextReject;
import decode.p25.message.pdu.confirmed.SNDCPActivateTDSContextRequest;
import decode.p25.message.pdu.confirmed.SNDCPDeactivateTDSContext;
import decode.p25.message.pdu.confirmed.SNDCPUserData;
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
import decode.p25.message.tdu.lc.AdjacentSiteStatusBroadcast;
import decode.p25.message.tdu.lc.GroupVoiceChannelUpdate;
import decode.p25.message.tdu.lc.GroupVoiceChannelUpdateExplicit;
import decode.p25.message.tdu.lc.NetworkStatusBroadcast;
import decode.p25.message.tdu.lc.NetworkStatusBroadcastExplicit;
import decode.p25.message.tdu.lc.SecondaryControlChannelBroadcast;
import decode.p25.message.tdu.lc.SecondaryControlChannelBroadcastExplicit;
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
import decode.p25.reference.Encryption;
import decode.p25.reference.IPProtocol;
import decode.p25.reference.Response;
import decode.p25.reference.Vendor;

public class P25ChannelState extends ChannelState
{
	private final static Logger mLog = LoggerFactory.getLogger( P25ChannelState.class );
	

	private HashMap<String,Long> mRegistrations = new HashMap<String,Long>();
	private String mLastCommandEventID;
	private String mLastPageEventID;
	private String mLastQueryEventID;
	private String mLastRegistrationEventID;
	private String mLastResponseEventID;

	private ActiveCall mCurrentActiveCall;
	private Map<String,ActiveCall> mCallDetects = new HashMap<String,ActiveCall>();
	
	private P25ActivitySummary mActivitySummary;
	private String mNAC;
	private String mSystem;
	private String mSite;
	private String mSiteAlias;
	private String mFromTalkgroup;
	private Alias mFromAlias;
	private String mToTalkgroup;
	private Alias mToAlias;

	/* The currently tuned traffic channel - dynamically updated */
	private String mCurrentChannel = "CURRENT";
	private long mCurrentChannelFrequency = 0;
	
	public P25ChannelState( ProcessingChain chain, AliasList aliasList )
	{
		super( chain, aliasList );
		
		mActivitySummary = new P25ActivitySummary( aliasList );

		/* Get the channel frequency from the source config */
		SourceConfiguration source = chain.getChannel().getSourceConfiguration();
		
		if( source instanceof SourceConfigTuner )
		{
			mCurrentChannelFrequency = ((SourceConfigTuner)source).getFrequency();
		}
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
			updateNAC( ( (P25Message) message ).getNAC() );
			
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
		if( isActiveCall( mCurrentChannel, hdu.getToID() ) )
		{
			updateCall( State.CALL, mCurrentChannel, null, hdu.getTalkgroupID() );
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			
			if( hdu.getEncryption() != Encryption.UNENCRYPTED )
			{
				sb.append( "ENCRYPTED WITH " );
				sb.append( hdu.getEncryption().name() );
				sb.append( " KEY:" );
				sb.append( hdu.getKeyID() );
			}
			
			P25CallEvent event = new P25CallEvent.Builder( CallEventType.CALL )
						.aliasList( mAliasList )
						.channel( mCurrentChannel )
						.frequency( mCurrentChannelFrequency )
						.to( hdu.getToID() )
						.details( sb.toString() )
						.build();
			
			addCall( event );
		}
	}

	/**
	 * Processes Terminator Data Unit with Link Control messages that occur on
	 * the traffic channel
	 */
	private void processTDULC( TDULinkControlMessage tdulc )
	{
		switch( tdulc.getOpcode() )
		{
			case ADJACENT_SITE_STATUS_BROADCAST:
				if( tdulc instanceof AdjacentSiteStatusBroadcast )
				{
					AdjacentSiteStatusBroadcast assb = 
								(AdjacentSiteStatusBroadcast)tdulc;
					
					updateSystem( assb.getSystemID() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
				/* This message doesn't provide anything we need for channel state */
				break;
			case CALL_ALERT:
				if( tdulc instanceof decode.p25.message.tdu.lc.CallAlert )
				{
					decode.p25.message.tdu.lc.CallAlert ca = 
							(decode.p25.message.tdu.lc.CallAlert)tdulc;
				mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.PAGE )
							.aliasList( mAliasList )
							.from( ca.getSourceAddress() )
							.to( ca.getTargetAddress() )
							.details( "CALL ALERT" )
							.build() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case CALL_TERMINATION_OR_CANCELLATION:
				endAllCalls();
				break;
			case CHANNEL_IDENTIFIER_UPDATE:
				/* This message is handled by the P25MessageProcessor and 
				 * inserted into any channels needing frequency band info */
				break;
			case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
				/* This message is handled by the P25MessageProcessor and 
				 * inserted into any channels needing frequency band info */
				break;
			case EXTENDED_FUNCTION_COMMAND:
				if( tdulc instanceof decode.p25.message.tdu.lc.ExtendedFunctionCommand )
				{
					decode.p25.message.tdu.lc.ExtendedFunctionCommand efc = 
							(decode.p25.message.tdu.lc.ExtendedFunctionCommand)tdulc;
						mCallEventModel.add( 
								new P25CallEvent.Builder( CallEventType.COMMAND )
									.aliasList( mAliasList )
									.to( efc.getTargetAddress() )
									.details( "FUNCTION:" + efc.getExtendedFunction().getLabel() + 
											  " ARG:" + efc.getArgument() )
								  .build() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case GROUP_AFFILIATION_QUERY:
				if( tdulc instanceof decode.p25.message.tdu.lc.GroupAffiliationQuery )
				{
					decode.p25.message.tdu.lc.GroupAffiliationQuery gaq = 
							(decode.p25.message.tdu.lc.GroupAffiliationQuery)tdulc;
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.details( "GROUP AFFILIATION QUERY" )
							.from( gaq.getSourceAddress() )
							.to( gaq.getTargetAddress() )
							.build() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case GROUP_VOICE_CHANNEL_UPDATE:
				/* Reflects other call activity on the system: CALL DETECT */
				if( tdulc instanceof GroupVoiceChannelUpdate )
				{
					GroupVoiceChannelUpdate gvcu = (GroupVoiceChannelUpdate)tdulc;
					
					if( isActiveCall( gvcu.getChannelA(), 
									  gvcu.getGroupAddressA() ) )
					{
						updateCall( State.CALL, 
								    gvcu.getChannelA(), 
								    gvcu.getGroupAddressA(), 
								    gvcu.getGroupAddressB() );
					}
					else
					{
						P25CallEvent gvcuEvent = 
								new P25CallEvent.Builder( CallEventType.CALL_DETECT )
									.aliasList( mAliasList )
									.channel( gvcu.getChannelA() )
									.details( ( gvcu.isEncrypted() ? "ENCRYPTED" : "" ) )
									.frequency( gvcu.getDownlinkFrequencyA() )
								    .from( gvcu.getGroupAddressA() )
									.to( gvcu.getGroupAddressB() )
									.build();
						
						addCall( gvcuEvent );
					}
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
				/* Reflects other call activity on the system: CALL DETECT */
				
				if( tdulc instanceof GroupVoiceChannelUpdateExplicit )
				{
					GroupVoiceChannelUpdateExplicit gvcue = 
							(GroupVoiceChannelUpdateExplicit)tdulc;
					if( isActiveCall( gvcue.getTransmitChannel(), 
							  		  gvcue.getGroupAddress() ) )
					{
						updateCall( State.CALL, 
								    gvcue.getTransmitChannel(), 
								    null, 
								    gvcue.getGroupAddress() );
					}
					else
					{
						P25CallEvent gvcueEvent = 
							new P25CallEvent.Builder( CallEventType.CALL_DETECT )
								.aliasList( mAliasList )
								.channel( gvcue.getTransmitChannel() )
								.details( ( gvcue.isEncrypted() ? "ENCRYPTED" : "" ) )
								.frequency( gvcue.getDownlinkFrequency() )
								.to( gvcue.getGroupAddress() )
								.build();
						
						addCall( gvcueEvent );
					}
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case GROUP_VOICE_CHANNEL_USER:
				if( tdulc instanceof decode.p25.message.tdu.lc.GroupVoiceChannelUser )
				{
					decode.p25.message.tdu.lc.GroupVoiceChannelUser gvcuser = 
						(decode.p25.message.tdu.lc.GroupVoiceChannelUser)tdulc;
					
					if( isActiveCall( mCurrentChannel, gvcuser.getGroupAddress() ) )
					{
						updateCall( State.CALL, 
									mCurrentChannel, 
									gvcuser.getSourceAddress(), 
									gvcuser.getGroupAddress() );
					}
					else
					{
						P25CallEvent gvcuserEvent = 
								new P25CallEvent.Builder( CallEventType.CALL )
									.aliasList( mAliasList )
									.channel( mCurrentChannel )
									.details( ( gvcuser.isEncrypted() ? "ENCRYPTED" : "" ) + 
											  ( gvcuser.isEmergency() ? " EMERGENCY" : "") )
									.frequency( mCurrentChannelFrequency )
								    .from( gvcuser.getSourceAddress() )
									.to( gvcuser.getGroupAddress() )
									.build();
						
						addCall( gvcuserEvent );
					}
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case MESSAGE_UPDATE:
				if( tdulc instanceof decode.p25.message.tdu.lc.MessageUpdate )
				{
					decode.p25.message.tdu.lc.MessageUpdate mu = 
							(decode.p25.message.tdu.lc.MessageUpdate)tdulc;
					
					mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.SDM )
								.aliasList( mAliasList )
								.from( mu.getSourceAddress() )
								.to( mu.getTargetAddress() )
								.details( "MSG: " + mu.getShortDataMessage() )
								.build() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case NETWORK_STATUS_BROADCAST:
				if( tdulc instanceof NetworkStatusBroadcast )
				{
					updateSystem( ((NetworkStatusBroadcast)tdulc).getSystem() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case NETWORK_STATUS_BROADCAST_EXPLICIT:
				if( tdulc instanceof NetworkStatusBroadcastExplicit )
				{
					updateSystem( ((NetworkStatusBroadcastExplicit)tdulc).getSystem() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case PROTECTION_PARAMETER_BROADCAST:
				if( tdulc instanceof decode.p25.message.tdu.lc.ProtectionParameterBroadcast )
				{
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
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case RFSS_STATUS_BROADCAST:
				if( tdulc instanceof decode.p25.message.tdu.lc.RFSSStatusBroadcast )
				{
					decode.p25.message.tdu.lc.RFSSStatusBroadcast rfsssb =
						(decode.p25.message.tdu.lc.RFSSStatusBroadcast)tdulc;
					
					updateSystem( rfsssb.getSystem() );

					String site = rfsssb.getRFSubsystemID() + "-" + 
							rfsssb.getSiteID();

					updateSite( site );
					
//					if( mCurrentChannel == null || 
//						!mCurrentChannel.contentEquals( rfsssb.getChannel() ) )
//					{
//						mCurrentChannel = rfsssb.getChannel();
//						mCurrentChannelFrequency = rfsssb.getDownlinkFrequency();
//					}
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case RFSS_STATUS_BROADCAST_EXPLICIT:
				if( tdulc instanceof decode.p25.message.tdu.lc.RFSSStatusBroadcastExplicit )
				{
					decode.p25.message.tdu.lc.RFSSStatusBroadcastExplicit rfsssbe =
						(decode.p25.message.tdu.lc.RFSSStatusBroadcastExplicit)tdulc;
					
					String site = rfsssbe.getRFSubsystemID() + "-" + 
							rfsssbe.getSiteID();

					updateSite( site );
					
//					if( mCurrentChannel == null || 
//						!mCurrentChannel.contentEquals( rfsssbe.getTransmitChannel() ) )
//					{
//						mCurrentChannel = rfsssbe.getTransmitChannel();
//						mCurrentChannelFrequency = rfsssbe.getDownlinkFrequency();
//					}
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case SECONDARY_CONTROL_CHANNEL_BROADCAST:
				if( tdulc instanceof SecondaryControlChannelBroadcast )
				{
					SecondaryControlChannelBroadcast sccb = 
								(SecondaryControlChannelBroadcast)tdulc;
					
					String site = sccb.getRFSubsystemID() + "-" + 
							sccb.getSiteID();

					updateSite( site );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
				if( tdulc instanceof SecondaryControlChannelBroadcastExplicit )
				{
					SecondaryControlChannelBroadcastExplicit sccb = 
							(SecondaryControlChannelBroadcastExplicit)tdulc;
					
					String site = sccb.getRFSubsystemID() + "-" + 
							sccb.getSiteID();

					updateSite( site );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case STATUS_QUERY:
				if( tdulc instanceof decode.p25.message.tdu.lc.StatusQuery )
				{
					decode.p25.message.tdu.lc.StatusQuery sq = 
							(decode.p25.message.tdu.lc.StatusQuery)tdulc;

					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.details( "STATUS QUERY" )
							.from( sq.getSourceAddress() )
							.to( sq.getTargetAddress() )
							.build() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case STATUS_UPDATE:
				if( tdulc instanceof decode.p25.message.tdu.lc.StatusUpdate )
				{
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
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case SYSTEM_SERVICE_BROADCAST:
				/* This message doesn't provide anything we need for channel state */
				break;
			case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
				if( tdulc instanceof decode.p25.message.tdu.lc.TelephoneInterconnectAnswerRequest )
				{
					decode.p25.message.tdu.lc.TelephoneInterconnectAnswerRequest tiar = 
							(decode.p25.message.tdu.lc.TelephoneInterconnectAnswerRequest)tdulc;

					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.PAGE )
							.aliasList( mAliasList )
							.from( tiar.getTelephoneNumber() )
							.to( tiar.getTargetAddress() )
							.details( "TELEPHONE CALL ALERT" )
							.build() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
				if( tdulc instanceof decode.p25.message.tdu.lc.TelephoneInterconnectVoiceChannelUser )
				{
					decode.p25.message.tdu.lc.TelephoneInterconnectVoiceChannelUser tivcu = 
						(decode.p25.message.tdu.lc.TelephoneInterconnectVoiceChannelUser)tdulc;
					
					if( isActiveCall( mCurrentChannel, tivcu.getAddress() ) )
					{
						updateCall( State.CALL, 
									mCurrentChannel, 
									null, 
									tivcu.getAddress() );
					}
					else
					{
						P25CallEvent tivcuEvent = 
								new P25CallEvent.Builder( CallEventType.CALL )
									.aliasList( mAliasList )
									.channel( mCurrentChannel )
									.details( "TELEPHONE INTERCONNECT " + 
											  ( tivcu.isEncrypted() ? "ENCRYPTED" : "" ) + 
											  ( tivcu.isEmergency() ? " EMERGENCY" : "") )
									.frequency( mCurrentChannelFrequency )
								    .from( null )
									.to( tivcu.getAddress() )
									.build();
						
						addCall( tivcuEvent );
					}
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case UNIT_AUTHENTICATION_COMMAND:
				if( tdulc instanceof decode.p25.message.tdu.lc.UnitAuthenticationCommand )
				{
					decode.p25.message.tdu.lc.UnitAuthenticationCommand uac = 
							(decode.p25.message.tdu.lc.UnitAuthenticationCommand)tdulc;
					
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.to( uac.getCompleteTargetAddress() )
							.details( "AUTHENTICATE" )
							.build() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case UNIT_REGISTRATION_COMMAND:
				if( tdulc instanceof decode.p25.message.tdu.lc.UnitRegistrationCommand )
				{
					decode.p25.message.tdu.lc.UnitRegistrationCommand urc = 
							(decode.p25.message.tdu.lc.UnitRegistrationCommand)tdulc;

					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.to( urc.getCompleteTargetAddress() )
							.details( "REGISTER" )
							.build() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case UNIT_TO_UNIT_ANSWER_REQUEST:
				if( tdulc instanceof decode.p25.message.tdu.lc.UnitToUnitAnswerRequest )
				{
					decode.p25.message.tdu.lc.UnitToUnitAnswerRequest uuar = 
							(decode.p25.message.tdu.lc.UnitToUnitAnswerRequest)tdulc;

					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.PAGE )
							.aliasList( mAliasList )
							.from( uuar.getSourceAddress() )
							.to( uuar.getTargetAddress() )
							.details( "UNIT TO UNIT CALL ALERT" )
							.build() );
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
				if( tdulc instanceof decode.p25.message.tdu.lc.UnitToUnitVoiceChannelUser )
				{
					decode.p25.message.tdu.lc.UnitToUnitVoiceChannelUser uuvcu = 
						(decode.p25.message.tdu.lc.UnitToUnitVoiceChannelUser)tdulc;
					
					if( isActiveCall( mCurrentChannel, uuvcu.getTargetAddress() ) )
					{
						updateCall( State.CALL, 
									mCurrentChannel, 
									uuvcu.getSourceAddress(), 
									uuvcu.getTargetAddress() );
					}
					else
					{
						P25CallEvent tivcuEvent = 
								new P25CallEvent.Builder( CallEventType.CALL )
									.aliasList( mAliasList )
									.channel( mCurrentChannel )
									.details( ( uuvcu.isEncrypted() ? "ENCRYPTED" : "" ) + 
											  ( uuvcu.isEmergency() ? " EMERGENCY" : "" ) )
									.frequency( mCurrentChannelFrequency )
								    .from( uuvcu.getSourceAddress() )
									.to( uuvcu.getTargetAddress() )
									.build();
						
						addCall( tivcuEvent );
					}
				}
				else
				{
					logAlternateVendorMessage( tdulc );
				}
				break;
			default:
				break;
		}
	}
	
	/**
	 * Log optional vendor format messages that we don't yet support so that we
	 * can understand those messages and eventually add support.
	 */
	private void logAlternateVendorMessage( P25Message p25 )
	{
		if( p25 instanceof TDULinkControlMessage )
		{
			TDULinkControlMessage tdulc = (TDULinkControlMessage)p25;
			
			mLog.debug( "TDULC - VENDOR [" + tdulc.getVendor() +
					"] SPECIFIC FORMAT FOR " + tdulc.getVendorOpcode().getLabel() + 
					" " + tdulc.getBinaryMessage() );
		}
		else if( p25 instanceof LDU1Message )
		{
			LDU1Message ldu1 = (LDU1Message)p25;
			
			mLog.debug( "LDU1 - VENDOR [" + ldu1.getVendor() +
					"] SPECIFIC FORMAT FOR " + ldu1.getVendorOpcode().getLabel() + 
					" " + ldu1.getBinaryMessage() );
		}
		else if( p25 instanceof PDUMessage )
		{
			PDUMessage pdu = (PDUMessage)p25;
			
			mLog.debug( "PDU - VENDOR [" + pdu.getVendor() +
					"] SPECIFIC FORMAT FOR " + pdu.getVendorOpcode().getLabel() + 
					" " + pdu.getBinaryMessage() );
		}
		else
		{
			mLog.debug( "P25 - ALTERNATE VENDOR MESSAGE [" + p25.getClass() + "]" );
		}
	}
	
	private void processLDU( LDUMessage ldu )
	{
		if( ldu instanceof LDU1Message )
		{
			switch( ((LDU1Message)ldu).getOpcode() )
			{
				case ADJACENT_SITE_STATUS_BROADCAST:
					if( ldu instanceof decode.p25.message.ldu.lc.AdjacentSiteStatusBroadcast )
					{
						decode.p25.message.ldu.lc.AdjacentSiteStatusBroadcast assb =
							(decode.p25.message.ldu.lc.AdjacentSiteStatusBroadcast)ldu;
						
						updateSystem( assb.getSystemID() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}

					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
					/* This message doesn't provide anything we need for channel state */
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case CALL_ALERT:
					if( ldu instanceof decode.p25.message.ldu.lc.CallAlert )
					{
						decode.p25.message.ldu.lc.CallAlert ca = 
								(decode.p25.message.ldu.lc.CallAlert)ldu;
						mCallEventModel.add( 
								new P25CallEvent.Builder( CallEventType.PAGE )
									.aliasList( mAliasList )
									.from( ca.getSourceAddress() )
									.to( ca.getTargetAddress() )
									.details( "CALL ALERT" )
									.build() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case CALL_TERMINATION_OR_CANCELLATION:
					if( ldu instanceof CallTermination )
					{
						endAllCalls();
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					break;
				case CHANNEL_IDENTIFIER_UPDATE:
					/* This message is handled by the P25MessageProcessor and 
					 * inserted into any channels needing frequency band info */
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
					/* This message is handled by the P25MessageProcessor and 
					 * inserted into any channels needing frequency band info */
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case EXTENDED_FUNCTION_COMMAND:
					if( ldu instanceof decode.p25.message.ldu.lc.ExtendedFunctionCommand )
					{
						decode.p25.message.ldu.lc.ExtendedFunctionCommand efc = 
								(decode.p25.message.ldu.lc.ExtendedFunctionCommand)ldu;

						mCallEventModel.add( 
								new P25CallEvent.Builder( CallEventType.COMMAND )
									.aliasList( mAliasList )
									.to( efc.getTargetAddress() )
									.details( "FUNCTION:" + efc.getExtendedFunction().getLabel() + 
											  " ARG:" + efc.getArgument() )
								  .build() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case GROUP_AFFILIATION_QUERY:
					if( ldu instanceof decode.p25.message.ldu.lc.GroupAffiliationQuery )
					{
						decode.p25.message.ldu.lc.GroupAffiliationQuery gaq = 
								(decode.p25.message.ldu.lc.GroupAffiliationQuery)ldu;
						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.QUERY )
								.aliasList( mAliasList )
								.details( "GROUP AFFILIATION QUERY" )
								.from( gaq.getSourceAddress() )
								.to( gaq.getTargetAddress() )
								.build() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case GROUP_VOICE_CHANNEL_UPDATE:
					if( ldu instanceof decode.p25.message.ldu.lc.GroupVoiceChannelUpdate )
					{
						decode.p25.message.ldu.lc.GroupVoiceChannelUpdate gvcu = 
							(decode.p25.message.ldu.lc.GroupVoiceChannelUpdate)ldu;
						
						if( isActiveCall( gvcu.getChannelA(), 
								  gvcu.getGroupAddressA() ) )
						{
							updateCall( State.CALL, 
									    gvcu.getChannelA(), 
									    gvcu.getGroupAddressA(), 
									    gvcu.getGroupAddressB() );
						}
						else
						{
							P25CallEvent gvcuEvent = 
									new P25CallEvent.Builder( CallEventType.CALL_DETECT )
										.aliasList( mAliasList )
										.channel( gvcu.getChannelA() )
										.details( ( gvcu.isEncrypted() ? "ENCRYPTED" : "" ) )
										.frequency( gvcu.getDownlinkFrequencyA() )
									    .from( gvcu.getGroupAddressA() )
										.to( gvcu.getGroupAddressB() )
										.build();
							
							addCall( gvcuEvent );
						}
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					break;
				case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
					if( ldu instanceof decode.p25.message.ldu.lc.GroupVoiceChannelUpdateExplicit )
					{
						decode.p25.message.ldu.lc.GroupVoiceChannelUpdateExplicit gvcue = 
							(decode.p25.message.ldu.lc.GroupVoiceChannelUpdateExplicit)ldu;
						
						if( isActiveCall( gvcue.getTransmitChannel(), 
						  		  gvcue.getGroupAddress() ) )
						{
							updateCall( State.CALL, 
									    gvcue.getTransmitChannel(), 
									    null, 
									    gvcue.getGroupAddress() );
						}
						else
						{
							P25CallEvent gvcueEvent = 
								new P25CallEvent.Builder( CallEventType.CALL_DETECT )
									.aliasList( mAliasList )
									.channel( gvcue.getTransmitChannel() )
									.details( ( gvcue.isEncrypted() ? "ENCRYPTED" : "" ) )
									.frequency( gvcue.getDownlinkFrequency() )
									.to( gvcue.getGroupAddress() )
									.build();
							
							addCall( gvcueEvent );
						}
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					break;
				case GROUP_VOICE_CHANNEL_USER:
					if( ldu instanceof decode.p25.message.ldu.lc.GroupVoiceChannelUser )
					{
						decode.p25.message.ldu.lc.GroupVoiceChannelUser gvcuser = 
							(decode.p25.message.ldu.lc.GroupVoiceChannelUser)ldu;
						
						if( isActiveCall( mCurrentChannel, gvcuser.getGroupAddress() ) )
						{
							updateCall( State.CALL, 
										mCurrentChannel, 
										gvcuser.getSourceAddress(), 
										gvcuser.getGroupAddress() );
						}
						else
						{
							P25CallEvent gvcuserEvent = 
									new P25CallEvent.Builder( CallEventType.CALL )
										.aliasList( mAliasList )
										.channel( mCurrentChannel )
										.details( ( gvcuser.isEncrypted() ? "ENCRYPTED" : "" ) + 
												  ( gvcuser.isEmergency() ? " EMERGENCY" : "") )
										.frequency( mCurrentChannelFrequency )
									    .from( gvcuser.getSourceAddress() )
										.to( gvcuser.getGroupAddress() )
										.build();
							
							addCall( gvcuserEvent );
						}
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					break;
				case MESSAGE_UPDATE:
					if( ldu instanceof decode.p25.message.ldu.lc.MessageUpdate )
					{
						decode.p25.message.ldu.lc.MessageUpdate mu = 
								(decode.p25.message.ldu.lc.MessageUpdate)ldu;

						mCallEventModel.add( 
								new P25CallEvent.Builder( CallEventType.SDM )
									.aliasList( mAliasList )
									.from( mu.getSourceAddress() )
									.to( mu.getTargetAddress() )
									.details( "MSG: " + mu.getShortDataMessage() )
									.build() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case NETWORK_STATUS_BROADCAST:
					if( ldu instanceof decode.p25.message.ldu.lc.NetworkStatusBroadcast )
					{
						updateSystem( ((decode.p25.message.ldu.lc.NetworkStatusBroadcast)ldu).getSystem() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case NETWORK_STATUS_BROADCAST_EXPLICIT:
					if( ldu instanceof decode.p25.message.ldu.lc.NetworkStatusBroadcastExplicit )
					{
						updateSystem( ((decode.p25.message.ldu.lc.NetworkStatusBroadcastExplicit)ldu).getSystem() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case PROTECTION_PARAMETER_BROADCAST:
					if( ldu instanceof decode.p25.message.ldu.lc.ProtectionParameterBroadcast )
					{
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
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case RFSS_STATUS_BROADCAST:
					if( ldu instanceof decode.p25.message.ldu.lc.RFSSStatusBroadcast )
					{
						decode.p25.message.ldu.lc.RFSSStatusBroadcast rfsssb =
							(decode.p25.message.ldu.lc.RFSSStatusBroadcast)ldu;
						
						updateSystem( rfsssb.getSystem() );

						String site = rfsssb.getRFSubsystemID() + "-" + 
								rfsssb.getSiteID();

						updateSite( site );
						
//						if( mCurrentChannel == null || 
//							!mCurrentChannel.contentEquals( rfsssb.getChannel() ) )
//						{
//							mCurrentChannel = rfsssb.getChannel();
//							mCurrentChannelFrequency = rfsssb.getDownlinkFrequency();
//						}
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case RFSS_STATUS_BROADCAST_EXPLICIT:
					if( ldu instanceof decode.p25.message.ldu.lc.RFSSStatusBroadcastExplicit )
					{
						decode.p25.message.ldu.lc.RFSSStatusBroadcastExplicit rfsssbe =
							(decode.p25.message.ldu.lc.RFSSStatusBroadcastExplicit)ldu;
						
						String site = rfsssbe.getRFSubsystemID() + "-" + 
								rfsssbe.getSiteID();

						updateSite( site );
						
//						if( mCurrentChannel == null || 
//							!mCurrentChannel.contentEquals( rfsssbe.getTransmitChannel() ) )
//						{
//							mCurrentChannel = rfsssbe.getTransmitChannel();
//							mCurrentChannelFrequency = rfsssbe.getDownlinkFrequency();
//						}
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case SECONDARY_CONTROL_CHANNEL_BROADCAST:
					if( ldu instanceof decode.p25.message.ldu.lc.SecondaryControlChannelBroadcast )
					{
						decode.p25.message.ldu.lc.SecondaryControlChannelBroadcast sccb = 
						(decode.p25.message.ldu.lc.SecondaryControlChannelBroadcast)ldu;
						
						String site = sccb.getRFSubsystemID() + "-" + 
								sccb.getSiteID();

						updateSite( site );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
					if( ldu instanceof decode.p25.message.ldu.lc.SecondaryControlChannelBroadcastExplicit )
					{
						decode.p25.message.ldu.lc.SecondaryControlChannelBroadcastExplicit sccb = 
						(decode.p25.message.ldu.lc.SecondaryControlChannelBroadcastExplicit)ldu;
						
						String site = sccb.getRFSubsystemID() + "-" + 
								sccb.getSiteID();

						updateSite( site );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case STATUS_QUERY:
					if( ldu instanceof decode.p25.message.ldu.lc.StatusQuery )
					{
						decode.p25.message.ldu.lc.StatusQuery sq = 
								(decode.p25.message.ldu.lc.StatusQuery)ldu;

						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.QUERY )
								.aliasList( mAliasList )
								.details( "STATUS QUERY" )
								.from( sq.getSourceAddress() )
								.to( sq.getTargetAddress() )
								.build() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case STATUS_UPDATE:
					if( ldu instanceof decode.p25.message.ldu.lc.StatusUpdate )
					{
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
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );

					break;
				case SYSTEM_SERVICE_BROADCAST:
					/* This message doesn't provide anything we need for channel state */
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
					if( ldu instanceof decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest )
					{
						decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest tiar = 
								(decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest)ldu;

						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.PAGE )
								.aliasList( mAliasList )
								.from( tiar.getTelephoneNumber() )
								.to( tiar.getTargetAddress() )
								.details( "TELEPHONE CALL ALERT" )
								.build() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
					if( ldu instanceof TelephoneInterconnectVoiceChannelUser )
					{
						TelephoneInterconnectVoiceChannelUser tivcu =
								(TelephoneInterconnectVoiceChannelUser)ldu;
						
						if( isActiveCall( mCurrentChannel, tivcu.getAddress() ) )
						{
							updateCall( State.CALL, 
										mCurrentChannel, 
										null, 
										tivcu.getAddress() );
						}
						else
						{
							P25CallEvent tivcuEvent = 
								new P25CallEvent.Builder( CallEventType.CALL )
									.aliasList( mAliasList )
									.channel( mCurrentChannel )
									.details( "TELEPHONE INTERCONNECT " + 
											  ( tivcu.isEncrypted() ? "ENCRYPTED" : "" ) + 
											  ( tivcu.isEmergency() ? " EMERGENCY" : "") )
									.frequency( mCurrentChannelFrequency )
								    .from( null )
									.to( tivcu.getAddress() )
									.build();
							
							addCall( tivcuEvent );
						}
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					break;
				case UNIT_AUTHENTICATION_COMMAND:
					if( ldu instanceof decode.p25.message.ldu.lc.UnitAuthenticationCommand )
					{
						decode.p25.message.ldu.lc.UnitAuthenticationCommand uac = 
								(decode.p25.message.ldu.lc.UnitAuthenticationCommand)ldu;

						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.COMMAND )
								.aliasList( mAliasList )
								.to( uac.getCompleteTargetAddress() )
								.details( "AUTHENTICATE" )
								.build() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case UNIT_REGISTRATION_COMMAND:
					if( ldu instanceof decode.p25.message.ldu.lc.UnitRegistrationCommand )
					{
						decode.p25.message.ldu.lc.UnitRegistrationCommand urc = 
								(decode.p25.message.ldu.lc.UnitRegistrationCommand)ldu;

						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.COMMAND )
								.aliasList( mAliasList )
								.to( urc.getCompleteTargetAddress() )
								.details( "REGISTER" )
								.build() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case UNIT_TO_UNIT_ANSWER_REQUEST:
					if( ldu instanceof decode.p25.message.ldu.lc.UnitToUnitAnswerRequest )
					{
						decode.p25.message.ldu.lc.UnitToUnitAnswerRequest uuar = 
								(decode.p25.message.ldu.lc.UnitToUnitAnswerRequest)ldu;

						mCallEventModel.add( 
							new P25CallEvent.Builder( CallEventType.PAGE )
								.aliasList( mAliasList )
								.from( uuar.getSourceAddress() )
								.to( uuar.getTargetAddress() )
								.details( "UNIT TO UNIT CALL ALERT" )
								.build() );
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					
					updateCall( State.CALL, mCurrentChannel, null, null );
					break;
				case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
					if( ldu instanceof UnitToUnitVoiceChannelUser )
					{
						UnitToUnitVoiceChannelUser uuvcu = 
								(UnitToUnitVoiceChannelUser)ldu;
						
						if( isActiveCall( mCurrentChannel, uuvcu.getTargetAddress() ) )
						{
							updateCall( State.CALL, 
										mCurrentChannel, 
										uuvcu.getSourceAddress(), 
										uuvcu.getTargetAddress() );
						}
						else
						{
							P25CallEvent tivcuEvent = 
								new P25CallEvent.Builder( CallEventType.CALL )
									.aliasList( mAliasList )
									.channel( mCurrentChannel )
									.details( ( uuvcu.isEncrypted() ? "ENCRYPTED" : "" ) + 
											  ( uuvcu.isEmergency() ? " EMERGENCY" : "" ) )
									.frequency( mCurrentChannelFrequency )
								    .from( uuvcu.getSourceAddress() )
									.to( uuvcu.getTargetAddress() )
									.build();
							
							addCall( tivcuEvent );
						}
					}
					else
					{
						logAlternateVendorMessage( ldu );
					}
					break;
				default:
					break;
			}
		}
		else
		{
			//LDU2
			updateCall( State.CALL, mCurrentChannel, null, null );
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
	private void processPDU( PDUMessage pdu )
	{
		if( pdu instanceof PDUConfirmedMessage )
		{
			PDUConfirmedMessage pduc = (PDUConfirmedMessage)pdu;

			switch( pduc.getPDUType() )
			{
				case SN_ACTIVATE_TDS_CONTEXT_ACCEPT:
					SNDCPActivateTDSContextAccept satca = 
									(SNDCPActivateTDSContextAccept)pduc;
					
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.channel( mCurrentChannel )
							.details( "ACTIVATE SNDCP USE IP:" + satca.getIPAddress() )
							.frequency( mCurrentChannelFrequency )
							.to( satca.getLogicalLinkID() )
							.build() );
					break;
				case SN_ACTIVATE_TDS_CONTEXT_REJECT:
					SNDCPActivateTDSContextReject satcr = 
									(SNDCPActivateTDSContextReject)pduc;
	
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.COMMAND )
							.aliasList( mAliasList )
							.channel( mCurrentChannel )
							.details( "REJECT: SNDCP CONTEXT ACTIVATION "
								+ "REASON:" + satcr.getReason().getLabel() )
							.frequency( mCurrentChannelFrequency )
							.to( satcr.getLogicalLinkID() )
							.build() );
					break;
				case SN_ACTIVATE_TDS_CONTEXT_REQUEST:
					SNDCPActivateTDSContextRequest satcreq = 
					(SNDCPActivateTDSContextRequest)pduc;
	
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.channel( mCurrentChannel )
							.details( "REQUEST SNDCP USE IP:" + satcreq.getIPAddress() )
							.frequency( mCurrentChannelFrequency )
							.from( satcreq.getLogicalLinkID() )
							.build() );
					break;
				case SN_DEACTIVATE_TDS_CONTEXT_ACCEPT:
					SNDCPDeactivateTDSContext sdtca = 
								(SNDCPDeactivateTDSContext)pduc;

					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.channel( mCurrentChannel )
							.details( "ACCEPT DEACTIVATE SNDCP CONTEXT" )
							.frequency( mCurrentChannelFrequency )
							.from( sdtca.getLogicalLinkID() )
							.build() );
					break;
				case SN_DEACTIVATE_TDS_CONTEXT_REQUEST:
					SNDCPDeactivateTDSContext sdtcreq = 
							(SNDCPDeactivateTDSContext)pduc;
	
					mCallEventModel.add( 
						new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.channel( mCurrentChannel )
							.details( "REQUEST DEACTIVATE SNDCP CONTEXT" )
							.frequency( mCurrentChannelFrequency )
							.from( sdtcreq.getLogicalLinkID() )
							.build() );
					break;
				case SN_RF_CONFIRMED_DATA:
					SNDCPUserData sud = (SNDCPUserData)pduc;
					
					StringBuilder sbFrom = new StringBuilder();
					StringBuilder sbTo = new StringBuilder();
					
					sbFrom.append( sud.getSourceIPAddress() );
					sbTo.append( sud.getDestinationIPAddress() );
					
					if( sud.getIPProtocol() == IPProtocol.UDP )
					{
						sbFrom.append( ":" );
						sbFrom.append( sud.getUDPSourcePort() );
						sbTo.append( ":" );
						sbTo.append( sud.getUDPDestinationPort() );
					}

					P25CallEvent dataEvent = new P25CallEvent.Builder( CallEventType.DATA_CALL )
								.aliasList( mAliasList )
								.channel( mCurrentChannel )
								.details( "DATA: " + sud.getPayload() + 
									" RADIO IP:" + sbTo.toString() )
								.frequency( mCurrentChannelFrequency )
								.from( sbFrom.toString() )
								.to( pduc.getLogicalLinkID() )
								.build();
					
					if( isActiveCall( mCurrentChannel, pduc.getLogicalLinkID() ) )
					{
						updateCall( State.DATA, mCurrentChannel, sbFrom.toString(), pduc.getLogicalLinkID() );
					}
					else
					{
						addCall( dataEvent );
					}
					break;
				default:
					mLog.debug( "PDUC - Unrecognized Message: " + pduc.toString() );
					break;
			}
		}
		else
		{
			switch( pdu.getOpcode() )
			{
				case CALL_ALERT:
					if( pdu instanceof CallAlertExtended )
					{
						CallAlertExtended ca = (CallAlertExtended)pdu;
						
						mCallEventModel.add(
								new P25CallEvent.Builder( CallEventType.PAGE )
									.aliasList( mAliasList )
									.from( ca.getWACN() + "-" + ca.getSystemID() + "-" + 
											ca.getSourceID() )
									.to( ca.getTargetAddress() )
									.build() );	
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case GROUP_AFFILIATION_QUERY:
					if( pdu instanceof GroupAffiliationQueryExtended )
					{
						GroupAffiliationQueryExtended gaqe = 
								(GroupAffiliationQueryExtended)pdu;
					
						if( mLastQueryEventID == null || !gaqe.getTargetAddress()
								.contentEquals( mLastQueryEventID ) )
						{
							mCallEventModel.add( new P25CallEvent.Builder( CallEventType.QUERY )
								.aliasList( mAliasList )
								.details( "GROUP AFFILIATION" )
								.from( gaqe.getWACN() + "-" + gaqe.getSystemID() + 
										"-" + gaqe.getSourceID() )
								.to( gaqe.getTargetAddress() )
								.build() );	

							mLastQueryEventID = gaqe.getToID();
						}
					}
					break;
				case GROUP_AFFILIATION_RESPONSE:
					if( pdu instanceof GroupAffiliationResponseExtended )
					{
						GroupAffiliationResponseExtended gar = 
								(GroupAffiliationResponseExtended)pdu;
						
						if( mLastResponseEventID == null || !gar.getTargetAddress()
								.contentEquals( mLastResponseEventID ) )
						{
							mCallEventModel.add( new P25CallEvent.Builder( CallEventType.RESPONSE )
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
								.build() );	
							
							mLastResponseEventID = gar.getTargetAddress();
						}
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case GROUP_DATA_CHANNEL_GRANT:
					if( pdu instanceof GroupDataChannelGrantExtended )
					{
						GroupDataChannelGrantExtended gdcge = 
								(GroupDataChannelGrantExtended)pdu;

						if( isActiveCall( gdcge.getTransmitChannel(), gdcge.getGroupAddress() ) )
						{
							updateCall( State.DATA, 
										gdcge.getTransmitChannel(), 
										gdcge.getSourceAddress(), 
										gdcge.getGroupAddress() );
						}
						else
						{
							P25CallEvent event =  
									new P25CallEvent.Builder( CallEventType.DATA_CALL )
										.aliasList( mAliasList )
										.channel( gdcge.getTransmitChannel() )
										.details( ( gdcge.isEncrypted() ? "ENCRYPTED" : "" ) + 
												  ( gdcge.isEmergency() ? " EMERGENCY" : "") )
									    .frequency( gdcge.getDownlinkFrequency() )
										.from( gdcge.getSourceAddress() )
										.to( gdcge.getGroupAddress() )
										.build();
							
							addCall( event );
						}
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case GROUP_VOICE_CHANNEL_GRANT:
					if( pdu instanceof GroupVoiceChannelGrantExplicit )
					{
						GroupVoiceChannelGrantExplicit gvcge = 
								(GroupVoiceChannelGrantExplicit)pdu;

						if( isActiveCall( gvcge.getTransmitChannel(), gvcge.getGroupAddress() ) )
						{
							updateCall( State.CALL,
										gvcge.getTransmitChannel(), 
										gvcge.getSourceAddress(), 
										gvcge.getGroupAddress() );
						}
						else
						{
							P25CallEvent event =  
									new P25CallEvent.Builder( CallEventType.CALL )
										.aliasList( mAliasList )
										.channel( gvcge.getTransmitChannel() )
										.details( ( gvcge.isEncrypted() ? "ENCRYPTED" : "" ) + 
												  ( gvcge.isEmergency() ? " EMERGENCY" : "") )
									    .frequency( gvcge.getDownlinkFrequency() )
										.from( gvcge.getSourceAddress() )
										.to( gvcge.getGroupAddress() )
										.build();
							
							addCall( event );
						}
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case INDIVIDUAL_DATA_CHANNEL_GRANT:
					if( pdu instanceof IndividualDataChannelGrantExtended )
					{
						IndividualDataChannelGrantExtended idcge = 
								(IndividualDataChannelGrantExtended)pdu;

						if( isActiveCall( idcge.getTransmitChannel(), 
										  idcge.getTargetAddress() ) )
						{
							updateCall( State.DATA,
										idcge.getTransmitChannel(),
									    idcge.getSourceAddress(), 
									    idcge.getTargetAddress() );
						}
						else
						{
							P25CallEvent event =  
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
							
							addCall( event );
						}
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case MESSAGE_UPDATE:
					if( pdu instanceof MessageUpdateExtended )
					{
						MessageUpdateExtended mu = (MessageUpdateExtended)pdu;
						
						P25CallEvent event = 
								new P25CallEvent.Builder( CallEventType.SDM )
									.aliasList( mAliasList )
									.details( "MESSAGE: " + mu.getMessage() )
									.from( mu.getSourceWACN() + "-" + mu.getSourceSystemID() + 
											"-" + mu.getSourceID() )
									.to( mu.getTargetAddress() )
									.build();	

						mCallEventModel.add( event );
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case RFSS_STATUS_BROADCAST:
					if( pdu instanceof RFSSStatusBroadcastExtended )
					{
						RFSSStatusBroadcastExtended rsbe = (RFSSStatusBroadcastExtended)pdu;
						
						updateNAC( rsbe.getNAC() );
						updateSystem( rsbe.getSystemID() );
						updateSite( rsbe.getRFSubsystemID() + "-" + rsbe.getSiteID() );
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case ROAMING_ADDRESS_UPDATE:
					if( pdu instanceof RoamingAddressUpdateExtended )
					{
						RoamingAddressUpdateExtended raue = 
								(RoamingAddressUpdateExtended)pdu;

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
						
						mCallEventModel.add( new P25CallEvent.Builder( CallEventType.RESPONSE )
									.aliasList( mAliasList )
									.details( sb.toString() )
									.from( raue.getSourceID() )
									.to( raue.getTargetAddress() )
									.build() );	
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case STATUS_QUERY:
					if( pdu instanceof StatusQueryExtended )
					{
						StatusQueryExtended sq = (StatusQueryExtended)pdu;
						
						if( mLastQueryEventID == null || !sq.getTargetAddress()
								.contentEquals( mLastQueryEventID ) )
						{
							mCallEventModel.add( new P25CallEvent.Builder( CallEventType.QUERY )
							.aliasList( mAliasList )
							.details( "STATUS QUERY" )
							.from( sq.getSourceWACN() + "-" + 
								   sq.getSourceSystemID() + "-" + 
								   sq.getSourceID() )
							.to( sq.getTargetAddress() )
							.build() );	

							mLastQueryEventID = sq.getToID();
						}
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case STATUS_UPDATE:
					if( pdu instanceof StatusUpdateExtended )
					{
						StatusUpdateExtended su = (StatusUpdateExtended)pdu;

						if( mLastResponseEventID == null || !mLastResponseEventID
								.contentEquals( su.getTargetAddress() ) )
						{
							mCallEventModel.add( new P25CallEvent.Builder( CallEventType.RESPONSE )
								.aliasList( mAliasList )
								.details( "STATUS USER: " + su.getUserStatus() + 
										  " UNIT: " + su.getUnitStatus() )
								.from( su.getSourceWACN() + "-" + 
									   su.getSourceSystemID() + "-" + 
									   su.getSourceID() )
								.to( su.getTargetAddress() )
								.build() );	
				
							mLastResponseEventID = su.getTargetAddress();
						}
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
					TelephoneInterconnectChannelGrantExplicit ticge =
								(TelephoneInterconnectChannelGrantExplicit)pdu;
					
					if( isActiveCall( ticge.getTransmitChannel(), ticge.getAddress() ))
					{
						updateCall( State.CALL,
									ticge.getTransmitChannel(), 
									ticge.getLogicalLinkID(), 
								    ticge.getAddress() );
					}
					else
					{
						P25CallEvent event = 
								new P25CallEvent.Builder( CallEventType.CALL )
									.aliasList( mAliasList )
									.channel( ticge.getTransmitChannel() )
									.details( "TELEPHONE CALL " + 
											  ( ticge.isEncrypted() ? "ENCRYPTED" : "" ) + 
											  ( ticge.isEmergency() ? " EMERGENCY" : "") +
											  " CALL TIMER:" + ticge.getCallTimer() )
								    .frequency( ticge.getDownlinkFrequency() )
								    .from( ticge.getAddress() )
									.build();
						
						addCall( event );
					}
					break;
				case UNIT_REGISTRATION_RESPONSE:
					if( pdu instanceof UnitRegistrationResponseExtended )
					{
						UnitRegistrationResponseExtended urr = 
								(UnitRegistrationResponseExtended)pdu;

						if( urr.getResponse() == Response.ACCEPT )
						{
							mRegistrations.put( urr.getAssignedSourceAddress(), 
												System.currentTimeMillis() );
						}
			
						if( mLastRegistrationEventID == null || !mLastRegistrationEventID
								.contentEquals( urr.getAssignedSourceAddress() ) )
						{
							mCallEventModel.add( new P25CallEvent.Builder( CallEventType.REGISTER )
										.aliasList( mAliasList )
										.details( "REGISTRATION:" + urr.getResponse().name() +
												  " FOR EXTERNAL SYSTEM ADDRESS: " +
												  urr.getWACN() + "-" + 
												  urr.getSystemID() + "-" +
												  urr.getSourceAddress() +
												  " SOURCE ID: " + urr.getSourceID() )
										.from( urr.getAssignedSourceAddress() )
										.build() );
							
							mLastRegistrationEventID = urr.getAssignedSourceAddress();
						}
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case UNIT_TO_UNIT_ANSWER_REQUEST:
					if( pdu instanceof UnitToUnitAnswerRequestExplicit )
					{
						UnitToUnitAnswerRequestExplicit utuare = 
								(UnitToUnitAnswerRequestExplicit)pdu;

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
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
					if( pdu instanceof UnitToUnitVoiceChannelGrantExtended )
					{
						UnitToUnitVoiceChannelGrantExtended uuvcge = 
								(UnitToUnitVoiceChannelGrantExtended)pdu;
					
						if( isActiveCall( uuvcge.getTransmitChannel(), uuvcge.getTargetAddress() ) )
						{
							updateCall( State.CALL,
										uuvcge.getTransmitChannel(), 
										uuvcge.getSourceAddress(), 
									    uuvcge.getTargetAddress() );
						}
						else
						{
							P25CallEvent uuvcgeEvent = 
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
							
							addCall( uuvcgeEvent );
						}
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
					if( pdu instanceof UnitToUnitVoiceChannelGrantUpdateExtended )
					{
						UnitToUnitVoiceChannelGrantUpdateExtended uuvcgue = 
								(UnitToUnitVoiceChannelGrantUpdateExtended)pdu;
		
						if( isActiveCall( uuvcgue.getTransmitChannel(), 
										  uuvcgue.getTargetAddress() ) )
						{
							updateCall( State.CALL,
										uuvcgue.getTransmitChannel(), 
										uuvcgue.getSourceAddress(),
										uuvcgue.getTargetAddress() );
						}
						else
						{
							P25CallEvent event = 
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
							
							addCall( event );
						}
					}
					else
					{
						logAlternateVendorMessage( pdu );
					}
					break;
				default:
					mLog.debug( "PDU - unrecognized OPCODE message: " + pdu.toString() );
					break;
			}
		}
	}
	
	private void processTSBKCommand( TSBKMessage message )
	{
		P25CallEvent event = null;
		
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
	
	private void processTSBKRFSSStatus( RFSSStatusBroadcast message )
	{
		updateNAC( message.getNAC() );
		
		updateSystem( message.getSystemID() );
		
		updateSite( message.getRFSubsystemID() + "-" + message.getSiteID() );
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
					updateCall( State.DATA,
								gdca.getChannel1(), 
								gdca.getGroupAddress2(), 
							    gdca.getGroupAddress1() );
				}
				else
				{
					P25CallEvent event =  
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
						updateCall( State.DATA,
									gdca.getChannel2(), 
									null, 
									gdca.getGroupAddress2() );
					}
					else
					{
						P25CallEvent event =  
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
					updateCall( State.DATA,
								gdcae.getTransmitChannel(), 
								null, 
							    gdcae.getGroupAddress() );
				}
				else
				{
					P25CallEvent event =  
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
					updateCall( State.DATA,
								gdcg.getChannel(), null, 
								gdcg.getGroupAddress() );
				}
				else
				{
					P25CallEvent event =  
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
					updateCall( State.CALL,
								gvcg.getChannel(), 
								gvcg.getSourceAddress(), 
								gvcg.getGroupAddress() );
				}
				else
				{
					P25CallEvent event =  
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
					updateCall( State.CALL,
								gvcgu.getChannel1(), 
								gvcgu.getGroupAddress2(), 
								gvcgu.getGroupAddress1() );
				}
				else
				{
					P25CallEvent event =  
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
						updateCall( State.CALL,
									gvcgu.getChannel2(), null, 
									gvcgu.getGroupAddress2() );
					}
					else
					{
						P25CallEvent event = 
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
					updateCall( State.DATA,
								idcg.getChannel(), 
								idcg.getSourceAddress(), 
								idcg.getTargetAddress() );
				}
				else
				{
					P25CallEvent event =  
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
					updateCall( State.DATA,
								sdcg.getTransmitChannel(), 
								null, 
								sdcg.getTargetAddress() );
				}
				else
				{
					P25CallEvent event = 
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
					updateCall( State.CALL,
								tivcg.getChannel(), 
								tivcg.getSourceAddress(), 
								tivcg.getAddress() );
				}
				else
				{
					P25CallEvent tivcgEvent = 
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
					updateCall( State.CALL,
								tivcgu.getChannel(), 
								tivcgu.getSourceAddress(), 
								tivcgu.getAddress() );
				}
				else
				{
					P25CallEvent tivcguEvent = 
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
					updateCall( State.CALL,
								uuvcg.getChannel(), 
								uuvcg.getSourceAddress(), 
								uuvcg.getTargetAddress() );
				}
				else
				{
					P25CallEvent uuvcgEvent = 
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
					updateCall( State.CALL,
								uuvcgu.getChannel(), 
								uuvcgu.getSourceAddress(), 
								uuvcgu.getTargetAddress() );
				}
				else
				{
					P25CallEvent uuvcguEvent = 
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
	
	/**
	 * Indicates if either the current call, or any current call detects contain
	 * the talkgroup id.  For the current call, only the TO talkgroup is tested
	 * 
	 * @param channel
	 * @param talkgroup
	 * @return
	 */
	private boolean isActiveCall( String channel, String talkgroup )
	{
		if( channel != null )
		{
			if( mCurrentChannel.contentEquals( channel ) )
			{
				return mCurrentActiveCall != null && 
				   ( talkgroup == null || 
				     mCurrentActiveCall.getCallEvent().getToID().contentEquals( talkgroup ) );
			}
			else
			{
				ActiveCall call = mCallDetects.get( channel );
				
				if( call != null )
				{
					return talkgroup != null && call.getTalkgroups().contains( talkgroup );
				}
			}
		}
		
		return false;
	}

	/**
	 * Starts a new call event.  For P25 Phase 1, only 1 call event should ever
	 * have a call event type of CALL.  All other detected call events on other
	 * channels should use call event type CALL DETECT. 
	 */
	private void addCall( P25CallEvent event )
	{
		mCallEventModel.add( event );

		String channel = event.getChannel();

		ActiveCall call = new ActiveCall( getProcessingChain()
				.getResourceManager().getThreadPoolManager(), event );
		
		if( channel.contentEquals( mCurrentChannel ) )
		{
			if( mCurrentActiveCall != null )
			{
				mCurrentActiveCall.end();
			}

			mCurrentActiveCall = call;

			switch( event.getCallEventType() )
			{
				case CALL:
					setState( State.CALL );
					updateFrom( event.getFromID() );
					updateTo( event.getToID() );
					break;
				case DATA_CALL:
					setState( State.DATA );
					updateFrom( event.getFromID() );
					updateTo( event.getToID() );
					break;
				case CALL_DETECT:
					/* don't update call state */
				default:
					break;
			}
		}
		else
		{
			/* End the current channel call event, if there is one */
			if( mCallDetects.containsKey( channel ) )
			{
				mCallDetects.get( channel ).end();
			}
			
			mCallDetects.put( channel, call );
		}

	}
	
	/**
	 * Ends all calls in the mActiveCalls queue
	 */
	private void endAllCalls()
	{
		if( mCurrentActiveCall != null )
		{
			mCurrentActiveCall.end();
		}
		
		for( ActiveCall call: mCallDetects.values() )
		{
			call.end();
		}
	}

	/**
	 * Updates state, channel, from and to for a currently active call.  Use the
	 * isActiveCall() method to verify that a call is currently active, otherwise
	 * this invocation is ignored
	 */
	private void updateCall( State state, 
							 String channel, 
							 String from, 
							 String to )
	{
		if( mCurrentChannel.contentEquals( channel ) )
		{
			if( mCurrentActiveCall != null )
			{
				mCurrentActiveCall.update();

				P25CallEvent event = mCurrentActiveCall.getCallEvent();
				
				switch( event.getCallEventType() )
				{
					case CALL:
						setState( State.CALL );
	
						/* Broacast call state update */
						updateFrom( from );
						updateTo( to );
	
						if( from != null &&	
							!from.contentEquals( "0000" ) && 
							!from.contentEquals( "000000" ) &&
							( !event.hasFromID() || !event.getFromID().contentEquals( from ) ) )
						{
							event.setFromID( from );
						}
						break;
					case DATA_CALL:
						setState( State.DATA );
						
						updateFrom( from );
						updateTo( to );
	
						if( from != null &&	
								!from.contentEquals( "0000" ) && 
								!from.contentEquals( "000000" ) &&
								( !event.hasFromID() || !event.getFromID().contentEquals( from ) ) )
							{
								event.setFromID( from );
							}
						break;
					case CALL_DETECT:
						/* don't update call state */
					default:
						break;
				}
			}
			else
			{
				mLog.debug( "Request to update non-existent current call with "
					+ "state:" + state + " channel:" + channel + " from:" + 
						from + " to:" + to );
			}
		}
		else
		{
			ActiveCall call = mCallDetects.get( channel );
			
			if( call != null )
			{
				/* Update the timestamp to now */
				call.update();
			}
			else
			{
				mLog.debug( "Request to update non-existent call detect with "
						+ "state:" + state + " channel:" + channel + " from:" + 
							from + " to:" + to );
			}
		}
	}
	
	/**
	 * Broadcasts an update for the TO group or unit address 
	 */
	private void updateTo( String to )
	{
		if( to != null &&	
			!to.contentEquals( "0000" ) && 
			!to.contentEquals( "000000" ) &&
			( mToTalkgroup == null || !mToTalkgroup.contentEquals( to ) ) )
		{
			mToTalkgroup = to;
			broadcastChange( ChangedAttribute.TO_TALKGROUP );
			
			if( mAliasList != null )
			{
				mToAlias = mAliasList.getTalkgroupAlias( to );
			}
			else
			{
				mToAlias = null;
			}
			
			broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
		}
	}

	/**
	 * Broadcasts an update for the FROM group or unit address 
	 */
	private void updateFrom( String from )
	{
		if( from != null &&	
			!from.contentEquals( "0000" ) && 
			!from.contentEquals( "000000" ) &&
			( mFromTalkgroup == null || !mFromTalkgroup.contentEquals( from ) ) )
		{
			mFromTalkgroup = from;
			broadcastChange( ChangedAttribute.FROM_TALKGROUP );
			
			if( mAliasList != null )
			{
				mFromAlias = mAliasList.getTalkgroupAlias( from );
			}
			else
			{
				mFromAlias = null;
			}
			
			broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );
		}
	}
	
	/**
	 * Broadcasts an update to the NAC
	 */
	private void updateNAC( String nac )
	{
		if( mNAC == null && nac != null )
		{
			mNAC = nac;
			
			broadcastChange( ChangedAttribute.NAC );
		}
	}
	
	/**
	 * Updates the site information
	 */
	private void updateSite( String site )
	{
		if( mSite == null || !mSite.contentEquals( site ) )
		{
			mSite = site;
			
			broadcastChange( ChangedAttribute.SITE );
			
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

			broadcastChange( ChangedAttribute.SITE_ALIAS );
		}
	}
	
	private void updateSystem( String system )
	{
		if( mSystem == null || !mSystem.contentEquals( system ) )
		{
			mSystem = system;
			
			broadcastChange( ChangedAttribute.SYSTEM );
		}
	}
	

	/**
	 * Process a unit paging event message
	 */
	private void processTSBKPage( TSBKMessage message )
	{
		P25CallEvent event = null;
		
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
	
	public Alias getFromAlias()
	{
		return mFromAlias;
	}
	
	public String getToTalkgroup()
	{
		return mToTalkgroup;
	}
	
	public Alias getToAlias()
	{
		return mToAlias;
	}
	
	public class ActiveCall
	{
		public static final long CALL_TIMEOUT = 2000; //milliseconds
		public static final long CALL_DETECT_TIMEOUT = 5000; //milliseconds
		
		private P25CallEvent mEvent;
		private long mLastUpdate;
		private ThreadPoolManager mThreadPoolManager;
		private AtomicBoolean mEnded = new AtomicBoolean( false );
		private long mTimeout;
		private List<String> mTalkgroups = new ArrayList<String>();
		
		public ActiveCall( ThreadPoolManager threadManager, P25CallEvent event  )
		{
			mThreadPoolManager = threadManager;
			mEvent = event;
			
			if( event.getFromID() != null )
			{
				mTalkgroups.add( event.getFromID() );
			}
			
			if( event.getToID() != null )
			{
				mTalkgroups.add( event.getToID() );
			}
			
			mLastUpdate = System.currentTimeMillis();

			mTimeout = event.getCallEventType() == 
				CallEventType.CALL_DETECT ? CALL_DETECT_TIMEOUT : CALL_TIMEOUT;
			
			mThreadPoolManager.scheduleOnce( new CallExpirationCheck( this ), 
					mTimeout, TimeUnit.MILLISECONDS );
		}
		
		public P25CallEvent getCallEvent()
		{
			return mEvent;
		}
		
		public List<String> getTalkgroups()
		{
			return mTalkgroups;
		}
		
		public boolean matches( String channel, String id )
		{
			return mEvent.getChannel() != null &&
				   channel != null &&
				   mEvent.getChannel().contentEquals( channel ) &&
				   id != null &&
				   mEvent.getToID().contentEquals( id );
		}
		
		/**
		 * Sets the last update timestamp to now
		 */
		public void update()
		{
			mLastUpdate = System.currentTimeMillis();
		}
		
		public void checkExpiration()
		{
			long now = System.currentTimeMillis();

			if( !mEnded.get() )
			{
				if( now > mLastUpdate + mTimeout )
				{
					cleanup();
				}
				else
				{
					mThreadPoolManager.scheduleOnce( new CallExpirationCheck( this ), 
							mTimeout, TimeUnit.MILLISECONDS );
				}
			}
		}
		
		public void end()
		{
			/* The expire check timer could fire while we're doing this, so we
			 * need to safely switch the flag and then run cleanup */
			if( mEnded.compareAndSet( false, true ) )
			{
				cleanup();
			}
		}
		
		private void cleanup()
		{
			setState( State.FADE );
			mCallEventModel.setEnd( mEvent );
			
			if( mEvent.getChannel().contentEquals( mCurrentChannel ) )
			{
				if( mCurrentActiveCall == this )
				{
					mCurrentActiveCall = null;
				}
			}
			else
			{
				mCallDetects.remove( this );
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
