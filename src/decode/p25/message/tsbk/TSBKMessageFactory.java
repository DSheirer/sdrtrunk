package decode.p25.message.tsbk;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;
import decode.p25.reference.Vendor;

public class TSBKMessageFactory
{
	public static TSBKMessage getMessage( BitSetBuffer message, 
	                                      DataUnitID duid,
	                                      AliasList aliasList )
	{
		int opcode = message.getInt( TSBKMessage.OPCODE );

		Vendor vendor = Vendor.fromValue( 
				message.getInt( TSBKMessage.VENDOR_ID ) );
		
		switch( vendor )
		{
			case STANDARD:
				switch( Opcode.fromValue( opcode ) )
				{
				    case GROUP_VOICE_CHANNEL_GRANT:
				        return new GroupVoiceChannelGrant( message, duid, 
				                aliasList );
				    case GROUP_VOICE_CHANNEL_GRANT_UPDATE:
				        return new GroupVoiceChannelGrantUpdate( message, duid, 
				                aliasList );
				    case GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
				        return new GroupVoiceChannelGrantUpdateExplicit( message, 
				                duid, aliasList );
				    case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
				        return new TelephoneInterconnectVoiceChannelGrant( 
				                message, duid, aliasList );
				    case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
				        return new TelephoneInterconnectAnswerRequest( 
				                message, duid, aliasList );
                    case UNIT_TO_UNIT_ANSWER_REQUEST:
                        return new UnitToUnitAnswerRequest( message, duid, 
                                aliasList );
                    case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                        return new UnitToUnitVoiceChannelGrant( message, 
                                duid, aliasList );
                    case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                        return new UnitToUnitVoiceChannelGrantUpdate( message, 
                                duid, aliasList );
				    default:
		                return new TSBKMessage( message, duid, aliasList );
				}
			case MOTOROLA:
			default:
				return new TSBKMessage( message, duid, aliasList );
		}
	}
}
