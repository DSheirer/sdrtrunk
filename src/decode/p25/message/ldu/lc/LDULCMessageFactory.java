package decode.p25.message.ldu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.message.ldu.LDU1Message;
import decode.p25.reference.Vendor;


public class LDULCMessageFactory
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( LDULCMessageFactory.class );

	/**
	 * Constructs a new sub-class of the TDULC message if the opcode is recognized
	 * or returns the original message.
	 */
	public static LDU1Message getMessage( LDU1Message message )
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
				case GROUP_VOICE_CHANNEL_USER:
					return new GroupVoiceChannelUser( message );
			    default:
			    	break;
			}
		}
		else
		{
			switch( message.getVendor() )
			{
				default:
					break;
			}
		}
		
		return message;
	}
}
