package io.github.dsheirer.module.decode.p25.message.tdu.lc;

import io.github.dsheirer.module.decode.p25.reference.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TDULCMessageFactory
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( TDULCMessageFactory.class );

	/**
	 * Constructs a new sub-class of the TDULC message if the opcode is recognized
	 * or returns the original message.
	 */
	public static TDULinkControlMessage getMessage( TDULinkControlMessage message )
	{
		if( message.isImplicitFormat() || message.getVendor() == Vendor.STANDARD )
		{
			switch( message.getOpcode() )
			{
				case ADJACENT_SITE_STATUS_BROADCAST:
					return new AdjacentSiteStatusBroadcast( message );
				case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
					return new AdjacentSiteStatusBroadcastExplicit( message );
				case CALL_ALERT:
					return new CallAlert( message );
				case CALL_TERMINATION_OR_CANCELLATION:
					return new CallTermination( message );
				case CHANNEL_IDENTIFIER_UPDATE:
					return new ChannelIdentifierUpdate( message );
				case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
					return new ChannelIdentifierUpdateExplicit( message );
				case EXTENDED_FUNCTION_COMMAND:
					return new ExtendedFunctionCommand( message );
				case GROUP_AFFILIATION_QUERY:
					return new GroupAffiliationQuery( message );
				case GROUP_VOICE_CHANNEL_UPDATE:
					return new GroupVoiceChannelUpdate( message );
				case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
					return new GroupVoiceChannelUpdateExplicit( message );
				case GROUP_VOICE_CHANNEL_USER:
					return new GroupVoiceChannelUser( message );
				case MESSAGE_UPDATE:
					return new MessageUpdate( message );
				case NETWORK_STATUS_BROADCAST:
					return new NetworkStatusBroadcast( message );
				case NETWORK_STATUS_BROADCAST_EXPLICIT:
					return new NetworkStatusBroadcastExplicit( message );
				case PROTECTION_PARAMETER_BROADCAST:
					return new ProtectionParameterBroadcast( message );
				case RFSS_STATUS_BROADCAST:
					return new RFSSStatusBroadcast( message );
				case RFSS_STATUS_BROADCAST_EXPLICIT:
					return new RFSSStatusBroadcastExplicit( message );
				case SECONDARY_CONTROL_CHANNEL_BROADCAST:
					return new SecondaryControlChannelBroadcast( message );
				case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
					return new SecondaryControlChannelBroadcastExplicit( message );
				case STATUS_QUERY:
					return new StatusQuery( message );
				case STATUS_UPDATE:
					return new StatusUpdate( message );
				case SYSTEM_SERVICE_BROADCAST:
					return new SystemServiceBroadcast( message );
				case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
					return new TelephoneInterconnectAnswerRequest( message );
				case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
					return new TelephoneInterconnectVoiceChannelUser( message );
				case UNIT_AUTHENTICATION_COMMAND:
					return new UnitAuthenticationCommand( message );
				case UNIT_REGISTRATION_COMMAND:
					return new UnitRegistrationCommand( message );
				case UNIT_TO_UNIT_ANSWER_REQUEST:
					return new UnitToUnitAnswerRequest( message );
				case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
					return new UnitToUnitVoiceChannelUser( message );
			    default:
			    	break;
			}
		}
		else
		{
			switch( message.getVendor() )
			{
				case MOTOROLA:
				default:
					break;
			}
		}
		
		return message;
	}
}
