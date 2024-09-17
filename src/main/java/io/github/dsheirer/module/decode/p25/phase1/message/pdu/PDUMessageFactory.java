/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.module.decode.p25.phase1.message.pdu;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_1_2_P25;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.P25P1Interleave;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCAuthenticationQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCAuthenticationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCCallAlertRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCGroupAffiliationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCIndividualDataServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCLocationRegistrationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCMessageUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCRoamingAddressRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCStatusQueryRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCStatusQueryResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCStatusUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCUnitAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCUnitToUnitAnswerResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCUnitToUnitVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCAdjacentStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCCallAlert;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCFrequencyBandUpdateTDMA;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCGroupAffiliationQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCGroupAffiliationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCGroupDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCGroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCIndividualDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCMessageUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCMotorolaGroupRegroupChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCProtectionParameterBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCRFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCRoamingAddressResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCRoamingAddressUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCStatusQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCStatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCTelephoneInterconnectChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCTelephoneInterconnectChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitToUnitAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitToUnitVoiceServiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitToUnitVoiceServiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.ConfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.DataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.UnconfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.PacketHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.PacketMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp.SNDCPPacketMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.response.ResponseMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.umbtc.isp.UMBTCTelephoneInterconnectRequestExplicitDialing;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import java.util.BitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDUMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(PDUMessageFactory.class);

    private static final int[] BLOCK_0_UMBTC_OPCODE = {2, 3, 4, 5, 6, 7};
    private static final int PDU0_BEGIN = 0;
    private static final int PDU0_END = 196;

    private static final ViterbiDecoder_1_2_P25 VITERBI_HALF_RATE_DECODER = new ViterbiDecoder_1_2_P25();

    public static PDUSequence createPacketSequence(int nac, long timestamp, CorrectedBinaryMessage correctedBinaryMessage)
    {
        //Get deinterleaved header chunk
        BitSet interleaved = correctedBinaryMessage.get(PDU0_BEGIN, PDU0_END);
        CorrectedBinaryMessage deinterleaved = P25P1Interleave.deinterleaveChunk(P25P1Interleave.DATA_DEINTERLEAVE, interleaved);

        //Decode 1/2 rate trellis encoded PDU header
        CorrectedBinaryMessage viterbiDecoded = VITERBI_HALF_RATE_DECODER.decode(deinterleaved);

        if(viterbiDecoded != null)
        {
            PDUHeader header = PDUHeaderFactory.getPDUHeader(viterbiDecoded);
            return new PDUSequence(header, timestamp, nac);
        }

        return null;
    }

    /**
     * Creates a packet sequence message from the packet sequence.
     *
     * @param pduSequence
     * @return
     */
    public static P25P1Message create(PDUSequence pduSequence, int nac, long timestamp)
    {
        switch(pduSequence.getHeader().getFormat())
        {
            case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
                return createAMBTC(pduSequence, nac, timestamp);
            case PACKET_DATA:
                return createPacketData(pduSequence, nac, timestamp);
            case RESPONSE_PACKET_HEADER_FORMAT:
                return new ResponseMessage(pduSequence, nac, timestamp);
            case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
                return createUMBTC(pduSequence, nac, timestamp);
            default:
                return new PDUSequenceMessage(pduSequence, nac, timestamp);
        }
    }

    /**
     * Creates a confirmed data block for a packet sequence
     */
    public static DataBlock createConfirmedDataBlock(CorrectedBinaryMessage interleaved)
    {
        CorrectedBinaryMessage deinterleaved = P25P1Interleave.deinterleaveChunk(P25P1Interleave.DATA_DEINTERLEAVE, interleaved);
        return new ConfirmedDataBlock(deinterleaved);
    }

    /**
     * Creates an unconfirmed data block for a packet sequence
     */
    public static DataBlock createUnconfirmedDataBlock(CorrectedBinaryMessage interleaved)
    {
        CorrectedBinaryMessage deinterleaved = P25P1Interleave.deinterleaveChunk(P25P1Interleave.DATA_DEINTERLEAVE, interleaved);
        return new UnconfirmedDataBlock(deinterleaved);
    }

    /**
     * Creates a packet data message
     *
     * @param pduSequence containing a packet data header and zero or more data blocks
     * @param nac network access code
     * @param timestamp of the packet sequence
     * @return packet data message parser
     */
    public static P25P1Message createPacketData(PDUSequence pduSequence, int nac, long timestamp)
    {
        PacketHeader packetHeader = (PacketHeader)pduSequence.getHeader();

        switch(packetHeader.getServiceAccessPoint())
        {
            case UNENCRYPTED_USER_DATA:
                return new PacketMessage(pduSequence, nac, timestamp);
            case SNDCP_PACKET_DATA_CONTROL:
                return new SNDCPPacketMessage(pduSequence, nac, timestamp);
            case PACKET_DATA:
                return new PacketMessage(pduSequence, nac, timestamp);
            case ADDRESS_RESOLUTION_PROTOCOL:
            case CHANNEL_REASSIGNMENT:
            case CIRCUIT_DATA:
            case CIRCUIT_DATA_CONTROL:
            case ENCRYPTED_KEY_MANAGEMENT_MESSAGE:
            case ENCRYPTED_TRUNKING_CONTROL:
            case ENCRYPTED_USER_DATA:
            case EXTENDED_ADDRESS:
            case MOBILE_RADIO_CONFIGURATION:
            case MOBILE_RADIO_LOOPBACK:
            case MOBILE_RADIO_PAGING:
            case MOBILE_RADIO_STATISTICS:
            case MOBILE_RADIO_OUT_OF_SERVICE:
            case REGISTRATION_AND_AUTHORIZATION:
            case SYSTEM_CONFIGURATION:
            case UNENCRYPTED_KEY_MANAGEMENT_MESSAGE:
            case UNENCRYPTED_TRUNKING_CONTROL:
            case UNKNOWN:
            default:
        }

        return new PDUSequenceMessage(pduSequence, nac, timestamp);
    }


        /**
         * Creates an alternate multi-block trunking control message
         *
         * @param pduSequence containing an AMBTC (PDU) header
         * @param nac network access code
         * @param timestamp of the packet sequence
         * @return AMBTC message parser for the specific opcode
         */
    public static P25P1Message createAMBTC(PDUSequence pduSequence, int nac, long timestamp)
    {
        AMBTCHeader ambtcHeader = (AMBTCHeader)pduSequence.getHeader();

        switch(ambtcHeader.getOpcode())
        {
            case ISP_AUTHENTICATION_QUERY_OBSOLETE:
                return new AMBTCAuthenticationQuery(pduSequence, nac, timestamp);
            case ISP_AUTHENTICATION_RESPONSE:
                return new AMBTCAuthenticationResponse(pduSequence, nac, timestamp);
            case ISP_CALL_ALERT_REQUEST:
                return new AMBTCCallAlertRequest(pduSequence, nac, timestamp);
            case ISP_GROUP_AFFILIATION_REQUEST:
                return new AMBTCGroupAffiliationRequest(pduSequence, nac, timestamp);
            case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                return new AMBTCIndividualDataServiceRequest(pduSequence, nac, timestamp);
            case ISP_LOCATION_REGISTRATION_REQUEST:
                return new AMBTCLocationRegistrationRequest(pduSequence, nac, timestamp);
            case ISP_MESSAGE_UPDATE_REQUEST:
                return new AMBTCMessageUpdateRequest(pduSequence, nac, timestamp);
            case ISP_ROAMING_ADDRESS_REQUEST:
                return new AMBTCRoamingAddressRequest(pduSequence, nac, timestamp);
            case ISP_STATUS_QUERY_REQUEST:
                return new AMBTCStatusQueryRequest(pduSequence, nac, timestamp);
            case ISP_STATUS_QUERY_RESPONSE:
                return new AMBTCStatusQueryResponse(pduSequence, nac, timestamp);
            case ISP_STATUS_UPDATE_REQUEST:
                return new AMBTCStatusUpdateRequest(pduSequence, nac, timestamp);
            case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                return new AMBTCUnitAcknowledgeResponse(pduSequence, nac, timestamp);
            case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                return new AMBTCUnitToUnitVoiceServiceRequest(pduSequence, nac, timestamp);
            case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                return new AMBTCUnitToUnitAnswerResponse(pduSequence, nac, timestamp);

            case OSP_ADJACENT_STATUS_BROADCAST:
                return new AMBTCAdjacentStatusBroadcast(pduSequence, nac, timestamp);
            case OSP_CALL_ALERT:
                return new AMBTCCallAlert(pduSequence, nac, timestamp);
            case OSP_GROUP_DATA_CHANNEL_GRANT:
                return new AMBTCGroupDataChannelGrant(pduSequence, nac, timestamp);
            case OSP_GROUP_AFFILIATION_QUERY:
                return new AMBTCGroupAffiliationQuery(pduSequence, nac, timestamp);
            case OSP_GROUP_AFFILIATION_RESPONSE:
                return new AMBTCGroupAffiliationResponse(pduSequence, nac, timestamp);
            case OSP_GROUP_VOICE_CHANNEL_GRANT:
                return new AMBTCGroupVoiceChannelGrant(pduSequence, nac, timestamp);
            case OSP_IDENTIFIER_UPDATE_TDMA:
                return new AMBTCFrequencyBandUpdateTDMA(pduSequence, nac, timestamp);
            case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
                return new AMBTCIndividualDataChannelGrant(pduSequence, nac, timestamp);
            case OSP_MESSAGE_UPDATE:
                return new AMBTCMessageUpdate(pduSequence, nac, timestamp);
            case OSP_NETWORK_STATUS_BROADCAST:
                return new AMBTCNetworkStatusBroadcast(pduSequence, nac, timestamp);
            case OSP_ADJACENT_STATUS_BROADCAST_UNCOORDINATED_BAND_PLAN:
                return new AMBTCProtectionParameterBroadcast(pduSequence, nac, timestamp);
            case OSP_RFSS_STATUS_BROADCAST:
                return new AMBTCRFSSStatusBroadcast(pduSequence, nac, timestamp);
            case OSP_ROAMING_ADDRESS_UPDATE:
                return new AMBTCRoamingAddressUpdate(pduSequence, nac, timestamp);
            case OSP_ROAMING_ADDRESS_COMMAND:
                return new AMBTCRoamingAddressResponse(pduSequence, nac, timestamp);
            case OSP_STATUS_QUERY:
                return new AMBTCStatusQuery(pduSequence, nac, timestamp);
            case OSP_STATUS_UPDATE:
                return new AMBTCStatusUpdate(pduSequence, nac, timestamp);
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                return new AMBTCTelephoneInterconnectChannelGrant(pduSequence, nac, timestamp);
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                return new AMBTCTelephoneInterconnectChannelGrantUpdate(pduSequence, nac, timestamp);
            case OSP_UNIT_REGISTRATION_RESPONSE:
                return new AMBTCUnitRegistrationResponse(pduSequence, nac, timestamp);
            case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
                return new AMBTCUnitToUnitAnswerRequest(pduSequence, nac, timestamp);
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                return new AMBTCUnitToUnitVoiceServiceChannelGrant(pduSequence, nac, timestamp);
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                return new AMBTCUnitToUnitVoiceServiceChannelGrantUpdate(pduSequence, nac, timestamp);
            case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_GRANT:
                return new AMBTCMotorolaGroupRegroupChannelGrant(pduSequence, nac, timestamp);
            default:
                return new PDUSequenceMessage(pduSequence, nac, timestamp);
        }
    }

    /**
     * Creates an unconfirmed multi-block trunking control message
     *
     * @param pduSequence containing a UMBTC (PDU) header
     * @param nac network access code
     * @param timestamp of the packet sequence
     * @return UMBTC message parser for the specific opcode
     */
    public static P25P1Message createUMBTC(PDUSequence pduSequence, int nac, long timestamp)
    {
        Opcode opcode = Opcode.OSP_UNKNOWN;

        if(pduSequence.hasDataBlock(0))
        {
            opcode = Opcode.fromValue(pduSequence.getDataBlock(0).getMessage().getInt(BLOCK_0_UMBTC_OPCODE),
                pduSequence.getHeader().getDirection(), pduSequence.getHeader().getVendor());
        }

        switch(opcode)
        {
            case ISP_TELEPHONE_INTERCONNECT_EXPLICIT_DIAL_REQUEST:
                return new UMBTCTelephoneInterconnectRequestExplicitDialing(pduSequence, nac, timestamp);
            default:
                return new PDUSequenceMessage(pduSequence, nac, timestamp);
        }
    }

    @Deprecated
    public static PDUMessage getMessage(BinaryMessage message, P25P1DataUnitID duid, AliasList aliasList)
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
