package ua.in.smartjava.module.decode.p25.message.pdu.confirmed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDUConfirmedMessageFactory
{
	public final static Logger mLog = 
			LoggerFactory.getLogger( PDUConfirmedMessage.class );

	public static PDUConfirmedMessage getMessage( PDUConfirmedMessage message )
	{
		switch( message.getServiceAccessPoint() )
		{
			case ADDRESS_RESOLUTION_PROTOCOL:
				break;
			case CHANNEL_REASSIGNMENT:
				break;
			case CIRCUIT_DATA:
				break;
			case CIRCUIT_DATA_CONTROL:
				break;
			case ENCRYPTED_KEY_MANAGEMENT_MESSAGE:
				break;
			case ENCRYPTED_TRUNKING_CONTROL:
				break;
			case ENCRYPTED_USER_DATA:
				break;
			case EXTENDED_ADDRESS:
				break;
			case MR_CONFIGURATION:
				break;
			case MR_LOOPBACK:
				break;
			case MR_OUT_OF_SERVICE:
				break;
			case MR_PAGING:
				break;
			case MR_STATISTICS:
				break;
			case PACKET_DATA:
				break;
			case REGISTRATION_AND_AUTHORIZATION:
				break;
				/* Deliberate fall-through */
			case SNDCP_PACKET_DATA_CONTROL:
			case UNENCRYPTED_USER_DATA:
				switch( message.getPDUType() )
				{
					case SNDCP_ACTIVATE_TDS_CONTEXT_ACCEPT:
						return new SNDCPActivateTDSContextAccept( message );
					case SNDCP_ACTIVATE_TDS_CONTEXT_REJECT:
						return new SNDCPActivateTDSContextReject( message );
					case SNDCP_ACTIVATE_TDS_CONTEXT_REQUEST:
						return new SNDCPActivateTDSContextRequest( message );
					case SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT:
					case SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST:
						return new SNDCPDeactivateTDSContext( message );
					case SNDCP_RF_CONFIRMED_DATA:
						return new SNDCPUserData( message );
					case SNDCP_RF_UNCONFIRMED_DATA:
						break;
					default:
						break;
				}
				break;
			case SYSTEM_CONFIGURATION:
				break;
			case UNENCRYPTED_KEY_MANAGEMENT_MESSAGE:
				break;
			case UNENCRYPTED_TRUNKING_CONTROL:
				break;
			default:
		
		}
		
		return message;
	}
}
