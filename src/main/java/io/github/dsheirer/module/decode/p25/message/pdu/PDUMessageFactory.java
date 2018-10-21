/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.pdu;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_1_2_P25;
import io.github.dsheirer.module.decode.p25.P25Interleave;
import io.github.dsheirer.module.decode.p25.message.pdu.header.PDUHeader;
import io.github.dsheirer.module.decode.p25.message.pdu.header.PDUHeaderFactory;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;

public class PDUMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(PDUMessageFactory.class);

    public static final int PDU0_BEGIN = 0;
    public static final int PDU0_END = 196;

    private static final ViterbiDecoder_1_2_P25 VITERBI_HALF_RATE_DECODER = new ViterbiDecoder_1_2_P25();

    public static PacketSequence createPacketSequence(int nac, long timestamp, CorrectedBinaryMessage correctedBinaryMessage)
    {
        //Get deinterleaved header chunk
        BitSet interleaved = correctedBinaryMessage.get(PDU0_BEGIN, PDU0_END);
        CorrectedBinaryMessage deinterleaved = P25Interleave.deinterleaveChunk(P25Interleave.DATA_DEINTERLEAVE, interleaved);

        //Decode 1/2 rate trellis encoded PDU header
        CorrectedBinaryMessage viterbiDecoded = VITERBI_HALF_RATE_DECODER.decode(deinterleaved);

        if(viterbiDecoded != null)
        {
            PDUHeader header = PDUHeaderFactory.getPDUHeader(viterbiDecoded);
            return new PacketSequence(header, timestamp, nac);
        }

        return null;
    }

    /**
     * Creates a packet sequence message from the packet sequence.
     *
     * @param packetSequence
     * @return
     */
    public static PacketSequenceMessage create(PacketSequence packetSequence, int nac, long timestamp)
    {
        return new PacketSequenceMessage(packetSequence, nac, timestamp);
    }

    public static DataBlock createConfirmedDataBlock(CorrectedBinaryMessage interleaved)
    {
        CorrectedBinaryMessage deinterleaved = P25Interleave.deinterleaveChunk(P25Interleave.DATA_DEINTERLEAVE, interleaved);
        return new ConfirmedDataBlock(deinterleaved);
    }

    public static DataBlock createUnconfirmedDataBlock(CorrectedBinaryMessage interleaved)
    {
        CorrectedBinaryMessage deinterleaved = P25Interleave.deinterleaveChunk(P25Interleave.DATA_DEINTERLEAVE, interleaved);
        return new UnconfirmedDataBlock(deinterleaved);
    }

    public static PDUMessage getMessage(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        PDUFormat format = PDUFormat.fromValue(message.getInt(PDUMessage.FORMAT));

//        switch(format)
//        {
//            case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
//                Vendor vendor = Vendor.fromValue(
//                    message.getInt(PDUMessage.VENDOR_ID));
//
//                switch(vendor)
//                {
//                    case STANDARD:
//                        Opcode opcode = Opcode.fromValue(message.getInt(PDUMessage.OPCODE));
//
//                        switch(opcode)
//                        {
//                            case OSP_ADJACENT_STATUS_BROADCAST:
//                                return new AdjacentStatusBroadcastExtended(message, duid, aliasList);
//                            case OSP_CALL_ALERT:
//                                return new CallAlertExtended(message, duid, aliasList);
//                            case OSP_GROUP_AFFILIATION_QUERY:
//                                return new GroupAffiliationQueryExtended(message, duid, aliasList);
//                            case OSP_GROUP_AFFILIATION_RESPONSE:
//                                return new GroupAffiliationResponseExtended(message, duid, aliasList);
//                            case OSP_GROUP_DATA_CHANNEL_GRANT:
//                                return new GroupDataChannelGrantExtended(message, duid, aliasList);
//                            case OSP_GROUP_VOICE_CHANNEL_GRANT:
//                                return new GroupVoiceChannelGrantExplicit(message, duid, aliasList);
//                            case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
//                                return new IndividualDataChannelGrantExtended(message, duid, aliasList);
//                            case OSP_MESSAGE_UPDATE:
//                                return new MessageUpdateExtended(message, duid, aliasList);
//                            case OSP_NETWORK_STATUS_BROADCAST:
//                                return new NetworkStatusBroadcastExtended(message, duid, aliasList);
//                            case OSP_PROTECTION_PARAMETER_BROADCAST:
//                                return new ProtectionParameterBroadcast(message, duid, aliasList);
//                            case OSP_RFSS_STATUS_BROADCAST:
//                                return new RFSSStatusBroadcastExtended(message,
//                                    duid, aliasList);
//                            case OSP_ROAMING_ADDRESS_UPDATE:
//                                return new RoamingAddressUpdateExtended(message, duid, aliasList);
//                            case OSP_STATUS_QUERY:
//                                return new StatusQueryExtended(message, duid, aliasList);
//                            case OSP_STATUS_UPDATE:
//                                return new StatusUpdateExtended(message, duid,
//                                    aliasList);
//                            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
//                                return new TelephoneInterconnectChannelGrantExplicit(message, duid, aliasList);
//                            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
//                                return new TelephoneInterconnectChannelGrantUpdateExplicit(message, duid, aliasList);
//                            case OSP_UNIT_REGISTRATION_RESPONSE:
//                                return new UnitRegistrationResponseExtended(message, duid, aliasList);
//                            case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
//                                return new UnitToUnitAnswerRequestExplicit(message, duid, aliasList);
//                            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
//                                return new UnitToUnitVoiceChannelGrantExtended(message, duid, aliasList);
//                            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
//                                return new UnitToUnitVoiceChannelGrantUpdateExtended(message, duid, aliasList);
//                            default:
//                                break;
//                        }
//                    case MOTOROLA:
//                        break;
//                    default:
//                        break;
//                }
//
//                return new PDUMessage(message, duid, aliasList);
//
//            case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
//                return new PDUMessage(message, duid, aliasList);
//            default:
//                return new PDUMessage(message, duid, aliasList);
//        }
        return null;
    }
}
