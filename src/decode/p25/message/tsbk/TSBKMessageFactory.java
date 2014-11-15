package decode.p25.message.tsbk;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.osp.control.AcknowledgeResponse;
import decode.p25.message.tsbk.osp.control.AdjacentStatusBroadcast;
import decode.p25.message.tsbk.osp.control.AuthenticationCommand;
import decode.p25.message.tsbk.osp.control.CallAlert;
import decode.p25.message.tsbk.osp.control.DenyResponse;
import decode.p25.message.tsbk.osp.control.ExtendedFunctionCommand;
import decode.p25.message.tsbk.osp.control.GroupAffiliationQuery;
import decode.p25.message.tsbk.osp.control.GroupAffiliationResponse;
import decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import decode.p25.message.tsbk.osp.control.MessageUpdate;
import decode.p25.message.tsbk.osp.control.NetworkStatusBroadcast;
import decode.p25.message.tsbk.osp.control.ProtectionParameterUpdate;
import decode.p25.message.tsbk.osp.data.GroupDataChannelAnnouncement;
import decode.p25.message.tsbk.osp.data.GroupDataChannelAnnouncementExplicit;
import decode.p25.message.tsbk.osp.data.GroupDataChannelGrant;
import decode.p25.message.tsbk.osp.data.IndividualDataChannelGrant;
import decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrant;
import decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdate;
import decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdateExplicit;
import decode.p25.message.tsbk.osp.voice.TelephoneInterconnectAnswerRequest;
import decode.p25.message.tsbk.osp.voice.TelephoneInterconnectVoiceChannelGrant;
import decode.p25.message.tsbk.osp.voice.UnitToUnitAnswerRequest;
import decode.p25.message.tsbk.osp.voice.UnitToUnitVoiceChannelGrant;
import decode.p25.message.tsbk.osp.voice.UnitToUnitVoiceChannelGrantUpdate;
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
                    case INDIVIDUAL_DATA_CHANNEL_GRANT:
                        return new IndividualDataChannelGrant( message, duid, 
                                aliasList );
                    case GROUP_DATA_CHANNEL_GRANT:
                        return new GroupDataChannelGrant( message, duid, 
                                aliasList );
                    case GROUP_DATA_CHANNEL_ANNOUNCEMENT:
                        return new GroupDataChannelAnnouncement( message, duid, 
                                aliasList );
                    case GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                        return new GroupDataChannelAnnouncementExplicit( 
                                message, duid, aliasList );
                    case ACKNOWLEDGE_RESPONSE_FNE:
                        return new AcknowledgeResponse( message, duid, 
                                aliasList );
                    case ADJACENT_STATUS_BROADCAST:
                        return new AdjacentStatusBroadcast( message, duid, 
                                aliasList );
                    case AUTHENTICATION_COMMAND:
                        return new AuthenticationCommand( message, duid, 
                                aliasList );
                    case CALL_ALERT:
                        return new CallAlert( message, duid, aliasList );
                    case DENY_RESPONSE:
                        return new DenyResponse( message, duid, aliasList );
                    case EXTENDED_FUNCTION_COMMAND:
                        return new ExtendedFunctionCommand( message, duid, 
                                aliasList );
                    case GROUP_AFFILIATION_QUERY:
                        return new GroupAffiliationQuery( message, duid, 
                                aliasList );
                    case GROUP_AFFILIATION_RESPONSE:
                        return new GroupAffiliationResponse( message, duid, 
                                aliasList );
                    case IDENTIFIER_UPDATE:
                        return new IdentifierUpdate( message, duid, aliasList );
                    case MESSAGE_UPDATE:
                        return new MessageUpdate( message, duid, aliasList );
                    case NETWORK_STATUS_BROADCAST:
                        return new NetworkStatusBroadcast( message, duid, 
                                aliasList );
                    case PROTECTION_PARAMETER_UPDATE:
                        return new ProtectionParameterUpdate( message, duid, 
                                aliasList );
				    default:
		                return new TSBKMessage( message, duid, aliasList );
				}
				
			case MOTOROLA:
			default:
				return new TSBKMessage( message, duid, aliasList );
		}
	}
}
