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
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.AMBTCHeader;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCAuthenticationQuery;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCAuthenticationResponse;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCCallAlertRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCGroupAffiliationRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCIndividualDataServiceRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCLocationRegistrationRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCMessageUpdateRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCRoamingAddressRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCStatusQueryRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCStatusQueryResponse;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCStatusUpdateRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCUnitAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCUnitToUnitVoiceServiceAnswerResponse;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCUnitToUnitVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCAdjacentStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCGroupDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCGroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCIndividualDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCTelephoneInterconnectChannelGrant;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCTelephoneInterconnectChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCUnitToUnitAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCUnitToUnitVoiceServiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCUnitToUnitVoiceServiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.pdu.header.PDUHeader;
import io.github.dsheirer.module.decode.p25.message.pdu.header.PDUHeaderFactory;
import io.github.dsheirer.module.decode.p25.message.pdu.umbtc.isp.UMBTCTelephoneInterconnectRequestExplicitDialing;
import io.github.dsheirer.module.decode.p25.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;

public class PDUMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(PDUMessageFactory.class);

    private static final int[] BLOCK_0_UMBTC_OPCODE = {2, 3, 4, 5, 6, 7};
    private static final int PDU0_BEGIN = 0;
    private static final int PDU0_END = 196;

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
    public static P25Message create(PacketSequence packetSequence, int nac, long timestamp)
    {
        switch(packetSequence.getHeader().getFormat())
        {
            case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
                return createAMBTC(packetSequence, nac, timestamp);
            case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
                return createUMBTC(packetSequence, nac, timestamp);
            default:
                return new PacketSequenceMessage(packetSequence, nac, timestamp);
        }
    }

    /**
     * Creates a confirmed data block for a packet sequence
     */
    public static DataBlock createConfirmedDataBlock(CorrectedBinaryMessage interleaved)
    {
        CorrectedBinaryMessage deinterleaved = P25Interleave.deinterleaveChunk(P25Interleave.DATA_DEINTERLEAVE, interleaved);
        return new ConfirmedDataBlock(deinterleaved);
    }

    /**
     * Creates an unconfirmed data block for a packet sequence
     */
    public static DataBlock createUnconfirmedDataBlock(CorrectedBinaryMessage interleaved)
    {
        CorrectedBinaryMessage deinterleaved = P25Interleave.deinterleaveChunk(P25Interleave.DATA_DEINTERLEAVE, interleaved);
        return new UnconfirmedDataBlock(deinterleaved);
    }

    /**
     * Creates an alternate multi-block trunking control message
     *
     * @param packetSequence containing an AMBTC (PDU) header
     * @param nac network access code
     * @param timestamp of the packet sequence
     * @return AMBTC message parser for the specific opcode
     */
    public static P25Message createAMBTC(PacketSequence packetSequence, int nac, long timestamp)
    {
        AMBTCHeader ambtcHeader = (AMBTCHeader)packetSequence.getHeader();

        switch(ambtcHeader.getOpcode())
        {
            case ISP_AUTHENTICATION_QUERY_OBSOLETE:
                return new AMBTCAuthenticationQuery(packetSequence, nac, timestamp);
            case ISP_AUTHENTICATION_RESPONSE:
                return new AMBTCAuthenticationResponse(packetSequence, nac, timestamp);
            case ISP_CALL_ALERT_REQUEST:
                return new AMBTCCallAlertRequest(packetSequence, nac, timestamp);
            case ISP_GROUP_AFFILIATION_REQUEST:
                return new AMBTCGroupAffiliationRequest(packetSequence, nac, timestamp);
            case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                return new AMBTCIndividualDataServiceRequest(packetSequence, nac, timestamp);
            case ISP_LOCATION_REGISTRATION_REQUEST:
                return new AMBTCLocationRegistrationRequest(packetSequence, nac, timestamp);
            case ISP_MESSAGE_UPDATE_REQUEST:
                return new AMBTCMessageUpdateRequest(packetSequence, nac, timestamp);
            case ISP_ROAMING_ADDRESS_REQUEST:
                return new AMBTCRoamingAddressRequest(packetSequence, nac, timestamp);
            case ISP_STATUS_QUERY_REQUEST:
                return new AMBTCStatusQueryRequest(packetSequence, nac, timestamp);
            case ISP_STATUS_QUERY_RESPONSE:
                return new AMBTCStatusQueryResponse(packetSequence, nac, timestamp);
            case ISP_STATUS_UPDATE_REQUEST:
                return new AMBTCStatusUpdateRequest(packetSequence, nac, timestamp);
            case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                return new AMBTCUnitAcknowledgeResponse(packetSequence, nac, timestamp);
            case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                return new AMBTCUnitToUnitVoiceServiceRequest(packetSequence, nac, timestamp);
            case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                return new AMBTCUnitToUnitVoiceServiceAnswerResponse(packetSequence, nac, timestamp);

            case OSP_ADJACENT_STATUS_BROADCAST:
                return new AMBTCAdjacentStatusBroadcast(packetSequence, nac, timestamp);
            case OSP_GROUP_DATA_CHANNEL_GRANT:
                return new AMBTCGroupDataChannelGrant(packetSequence, nac, timestamp);
            case OSP_GROUP_VOICE_CHANNEL_GRANT:
                return new AMBTCGroupVoiceChannelGrant(packetSequence, nac, timestamp);
            case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
                return new AMBTCIndividualDataChannelGrant(packetSequence, nac, timestamp);
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                return new AMBTCTelephoneInterconnectChannelGrant(packetSequence, nac, timestamp);
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                return new AMBTCTelephoneInterconnectChannelGrantUpdate(packetSequence, nac, timestamp);
            case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
                return new AMBTCUnitToUnitAnswerRequest(packetSequence, nac, timestamp);
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                return new AMBTCUnitToUnitVoiceServiceChannelGrant(packetSequence, nac, timestamp);
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                return new AMBTCUnitToUnitVoiceServiceChannelGrantUpdate(packetSequence, nac, timestamp);
            default:
                return new PacketSequenceMessage(packetSequence, nac, timestamp);
        }
    }

    /**
     * Creates an unconfirmed multi-block trunking control message
     *
     * @param packetSequence containing a UMBTC (PDU) header
     * @param nac network access code
     * @param timestamp of the packet sequence
     * @return UMBTC message parser for the specific opcode
     */
    public static P25Message createUMBTC(PacketSequence packetSequence, int nac, long timestamp)
    {
        Opcode opcode = Opcode.OSP_UNKNOWN;

        if(packetSequence.hasDataBlock(0))
        {
            opcode = Opcode.fromValue(packetSequence.getDataBlock(0).getMessage().getInt(BLOCK_0_UMBTC_OPCODE),
                packetSequence.getHeader().getDirection(), packetSequence.getHeader().getVendor());
        }

        switch(opcode)
        {
            case ISP_TELEPHONE_INTERCONNECT_EXPLICIT_DIAL_REQUEST:
                return new UMBTCTelephoneInterconnectRequestExplicitDialing(packetSequence, nac, timestamp);
            default:
                return new PacketSequenceMessage(packetSequence, nac, timestamp);
        }
    }

    @Deprecated
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
