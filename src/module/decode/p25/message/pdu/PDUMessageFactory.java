package module.decode.p25.message.pdu;

import module.decode.p25.message.pdu.osp.control.AdjacentStatusBroadcastExtended;
import module.decode.p25.message.pdu.osp.control.CallAlertExtended;
import module.decode.p25.message.pdu.osp.control.GroupAffiliationQueryExtended;
import module.decode.p25.message.pdu.osp.control.GroupAffiliationResponseExtended;
import module.decode.p25.message.pdu.osp.control.MessageUpdateExtended;
import module.decode.p25.message.pdu.osp.control.NetworkStatusBroadcastExtended;
import module.decode.p25.message.pdu.osp.control.ProtectionParameterBroadcast;
import module.decode.p25.message.pdu.osp.control.RFSSStatusBroadcastExtended;
import module.decode.p25.message.pdu.osp.control.RoamingAddressUpdateExtended;
import module.decode.p25.message.pdu.osp.control.StatusQueryExtended;
import module.decode.p25.message.pdu.osp.control.StatusUpdateExtended;
import module.decode.p25.message.pdu.osp.control.UnitRegistrationResponseExtended;
import module.decode.p25.message.pdu.osp.data.GroupDataChannelGrantExtended;
import module.decode.p25.message.pdu.osp.data.IndividualDataChannelGrantExtended;
import module.decode.p25.message.pdu.osp.voice.GroupVoiceChannelGrantExplicit;
import module.decode.p25.message.pdu.osp.voice.TelephoneInterconnectChannelGrantExplicit;
import module.decode.p25.message.pdu.osp.voice.TelephoneInterconnectChannelGrantUpdateExplicit;
import module.decode.p25.message.pdu.osp.voice.UnitToUnitAnswerRequestExplicit;
import module.decode.p25.message.pdu.osp.voice.UnitToUnitVoiceChannelGrantExtended;
import module.decode.p25.message.pdu.osp.voice.UnitToUnitVoiceChannelGrantUpdateExtended;
import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Opcode;
import module.decode.p25.reference.PDUFormat;
import module.decode.p25.reference.Vendor;
import alias.AliasList;
import bits.BinaryMessage;

public class PDUMessageFactory
{
	public static PDUMessage getMessage( BinaryMessage message, 
										 DataUnitID duid,
										 AliasList aliasList )
	{
		PDUFormat format = PDUFormat.fromValue( 
					message.getInt( PDUMessage.FORMAT ) );

		switch( format )
		{
			case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
				Vendor vendor = Vendor.fromValue( 
						message.getInt( PDUMessage.VENDOR_ID ) );
				
				switch( vendor )
				{
					case STANDARD:
						Opcode opcode = Opcode.fromValue( 
								message.getInt( PDUMessage.OPCODE ) );
						
						switch( opcode )
						{
							case ADJACENT_STATUS_BROADCAST:
								return new AdjacentStatusBroadcastExtended( 
										message, duid, aliasList );
							case CALL_ALERT:
								return new CallAlertExtended( message, duid, 
										aliasList );
							case GROUP_AFFILIATION_QUERY:
								return new GroupAffiliationQueryExtended( 
										message, duid, aliasList );
							case GROUP_AFFILIATION_RESPONSE:
								return new GroupAffiliationResponseExtended( 
										message, duid, aliasList );
							case GROUP_DATA_CHANNEL_GRANT:
								return new GroupDataChannelGrantExtended( 
										message, duid, aliasList );
							case GROUP_VOICE_CHANNEL_GRANT:
								
								return new GroupVoiceChannelGrantExplicit( 
										message, duid, aliasList );
							case INDIVIDUAL_DATA_CHANNEL_GRANT:
								return new IndividualDataChannelGrantExtended( 
										message, duid, aliasList );
							case MESSAGE_UPDATE:
								return new MessageUpdateExtended( message, duid, 
										aliasList );
							case NETWORK_STATUS_BROADCAST:
								return new NetworkStatusBroadcastExtended( 
										message, duid, aliasList );
							case PROTECTION_PARAMETER_BROADCAST:
								return new ProtectionParameterBroadcast(
										message, duid, aliasList );
							case RFSS_STATUS_BROADCAST:
								return new RFSSStatusBroadcastExtended( message, 
										duid, aliasList );
							case ROAMING_ADDRESS_UPDATE:
								return new RoamingAddressUpdateExtended( 
										message, duid, aliasList );
							case STATUS_QUERY:
								return new StatusQueryExtended( message, duid, 
										aliasList );
							case STATUS_UPDATE:
								return new StatusUpdateExtended( message, duid, 
										aliasList );
							case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
								return new TelephoneInterconnectChannelGrantExplicit( 
										message, duid, aliasList );
							case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
								return new TelephoneInterconnectChannelGrantUpdateExplicit( 
										message, duid, aliasList );
							case UNIT_REGISTRATION_RESPONSE:
								return new UnitRegistrationResponseExtended( 
										message, duid, aliasList );
							case UNIT_TO_UNIT_ANSWER_REQUEST:
								return new UnitToUnitAnswerRequestExplicit( 
										message, duid, aliasList );
							case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
								return new UnitToUnitVoiceChannelGrantExtended( 
										message, duid, aliasList );
							case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
								return new UnitToUnitVoiceChannelGrantUpdateExtended( 
										message, duid, aliasList );
							default:
								break;
						}
					case MOTOROLA:
						break;
					default:
						break;
				}
				
				return new PDUMessage( message, duid, aliasList );
				
			case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
				return new PDUMessage( message, duid, aliasList );
			default:
				return new PDUMessage( message, duid, aliasList );
		}
	}
}
