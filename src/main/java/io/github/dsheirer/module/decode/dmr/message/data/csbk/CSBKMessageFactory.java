/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.csbk;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.Hytera08Acknowledge;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.Hytera68Acknowledge;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraAdjacentSiteInformation;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraAloha;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraAnnouncement;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraCsbko44;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraCsbko47;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraSmsAvailableNotification;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraXPTPreamble;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraXPTSiteState;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityMaxAloha;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityPlusCSBKO_60;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityPlusDataRevertWindowAnnouncement;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityPlusDataRevertWindowGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityPlusNeighbors;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityPlusPreamble;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityPlusSystemStatus;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusCSBKO_16;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusDataChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusDataRevertWindowAnnouncement;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusDataRevertWindowGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusNeighborReport;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusOTAAnnouncement;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusRegistrationRequest;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusRegistrationResponse;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusTalkgroupAffiliation;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusTerminateChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Aloha;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Clear;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.MoveTSCC;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Preamble;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Protect;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.acknowledge.Acknowledge;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.acknowledge.AcknowledgeStatus;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.acknowledge.RegistrationAccepted;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy.Ahoy;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy.AuthenticateRegisterRadioCheck;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy.CancelCall;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy.ServiceRadioCheck;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy.StunReviveKill;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy.UnknownAhoy;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.AdjacentSiteInformation;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.AnnounceChannelFrequency;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.AnnounceWithdrawTSCC;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.Announcement;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.CallTimerParameters;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.LocalTime;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.MassRegistration;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.VoteNowAdvice;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.BroadcastTalkgroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.DuplexPrivateDataChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.DuplexPrivateVoiceChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.PrivateDataChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.PrivateVoiceChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.TalkgroupDataChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.TalkgroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.header.MBCHeader;
import io.github.dsheirer.module.decode.dmr.message.data.mbc.MBCContinuationBlock;
import io.github.dsheirer.module.decode.dmr.message.data.mbc.UnknownMultiCSBK;
import io.github.dsheirer.module.decode.dmr.message.type.AnnouncementType;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceKind;
import java.util.List;

/**
 * Factory for creating DMR CSBK and Multi-Block CSBK messages
 */
public class CSBKMessageFactory
{
    /**
     * Creates a CSBK Message
     * @param pattern that was transmitted
     * @param message contents
     * @param cach instance
     * @param slotType instance
     * @param timestamp of the message
     * @param timeslot the message was transmitted on
     * @return constructed CSBK mesage
     */
    public static CSBKMessage create(DMRSyncPattern pattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                                     long timestamp, int timeslot)
    {
        CSBKMessage csbk = null;

        if(message != null)
        {
            Opcode opcode = CSBKMessage.getOpcode(message);

            switch(opcode)
            {
                case STANDARD_ACKNOWLEDGE_RESPONSE_INBOUND_TSCC:
                case STANDARD_ACKNOWLEDGE_RESPONSE_OUTBOUND_TSCC:
                case STANDARD_ACKNOWLEDGE_RESPONSE_INBOUND_PAYLOAD:
                case STANDARD_ACKNOWLEDGE_RESPONSE_OUTBOUND_PAYLOAD:
                    switch(Acknowledge.getReason(message))
                    {
                        case TS_REGISTRATION_ACCEPTED:
                            csbk = new RegistrationAccepted(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        case TS_ACCEPTED_FOR_STATUS_POLLING_SERVICE:
                            csbk = new AcknowledgeStatus(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        default:
                            csbk = new Acknowledge(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                    }
                    break;
                case STANDARD_AHOY:
                    ServiceKind serviceKind = Ahoy.getServiceKind(message);
                    switch(serviceKind)
                    {
                        case AUTHENTICATE_REGISTER_RADIO_CHECK_SERVICE:
                            csbk = new AuthenticateRegisterRadioCheck(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        case CANCEL_CALL_SERVICE:
                            csbk = new CancelCall(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        case SUPPLEMENTARY_SERVICE:
                            csbk = new StunReviveKill(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        case FULL_DUPLEX_MS_TO_MS_PACKET_CALL_SERVICE:
                        case FULL_DUPLEX_MS_TO_MS_VOICE_CALL_SERVICE:
                        case INDIVIDUAL_VOICE_CALL_SERVICE:
                        case INDIVIDUAL_PACKET_CALL_SERVICE:
                        case INDIVIDUAL_UDT_SHORT_DATA_CALL_SERVICE:
                        case TALKGROUP_PACKET_CALL_SERVICE:
                        case TALKGROUP_UDT_SHORT_DATA_CALL_SERVICE:
                        case TALKGROUP_VOICE_CALL_SERVICE:
                            csbk = new ServiceRadioCheck(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        default:
                            csbk = new UnknownAhoy(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                    }
                    break;
                case STANDARD_ALOHA:
                    csbk = new Aloha(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case STANDARD_ANNOUNCEMENT:
                    switch(Announcement.getAnnouncementType(message))
                    {
                        case ADJACENT_SITE_INFORMATION:
                            csbk = new AdjacentSiteInformation(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        case ANNOUNCE_OR_WITHDRAW_TSCC:
                            csbk = new AnnounceWithdrawTSCC(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        case CALL_TIMER_PARAMETERS:
                            csbk = new CallTimerParameters(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        case LOCAL_TIME:
                            csbk = new LocalTime(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        case MASS_REGISTRATION:
                            csbk = new MassRegistration(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        case VOTE_NOW_ADVICE:
                            csbk = new VoteNowAdvice(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        default:
                            csbk = new Announcement(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                    }
                    break;
                case STANDARD_BROADCAST_TALKGROUP_VOICE_CHANNEL_GRANT:
                    csbk = new BroadcastTalkgroupVoiceChannelGrant(pattern, message, cach, slotType, timestamp,
                            timeslot);
                    break;
                case STANDARD_CLEAR:
                    csbk = new Clear(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case STANDARD_DUPLEX_PRIVATE_DATA_CHANNEL_GRANT:
                    csbk = new DuplexPrivateDataChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case STANDARD_DUPLEX_PRIVATE_VOICE_CHANNEL_GRANT:
                    csbk = new DuplexPrivateVoiceChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case STANDARD_PRIVATE_DATA_CHANNEL_GRANT_SINGLE_ITEM:
                    csbk = new PrivateDataChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case STANDARD_PRIVATE_VOICE_CHANNEL_GRANT:
                    csbk = new PrivateVoiceChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case STANDARD_PROTECT:
                    csbk = new Protect(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_SINGLE_ITEM:
                    csbk = new TalkgroupDataChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT:
                    csbk = new TalkgroupVoiceChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case STANDARD_MOVE_TSCC:
                    csbk = new MoveTSCC(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case STANDARD_PREAMBLE:
                    csbk = new Preamble(pattern, message, cach, slotType, timestamp, timeslot);
                    break;

                case HYTERA_68_ALOHA:
                    csbk = new HyteraAloha(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case HYTERA_08_ANNOUNCEMENT:
                case HYTERA_68_ANNOUNCEMENT:
                    AnnouncementType announcementType = HyteraAnnouncement.getAnnouncementType(message);
                    switch(announcementType)
                    {
                        case ADJACENT_SITE_INFORMATION:
                            csbk = new HyteraAdjacentSiteInformation(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                        default:
                            csbk = new HyteraAnnouncement(pattern, message, cach, slotType, timestamp, timeslot);
                            break;
                    }
                    break;
                case HYTERA_68_XPT_PREAMBLE:
                    csbk = new HyteraXPTPreamble(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case HYTERA_68_XPT_SITE_STATE:
                    csbk = new HyteraXPTSiteState(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case HYTERA_08_ACKNOWLEDGE:
                    csbk = new Hytera08Acknowledge(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case HYTERA_08_CSBKO_44:
                    csbk = new HyteraCsbko44(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case HYTERA_08_CSBKO_47:
                    csbk = new HyteraCsbko47(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case HYTERA_68_ACKNOWLEDGE:
                    csbk = new Hytera68Acknowledge(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case HYTERA_68_CSBKO_62:
                    csbk = new HyteraSmsAvailableNotification(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CAPMAX_ALOHA:
                    csbk = new CapacityMaxAloha(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CAPPLUS_NEIGHBOR_REPORT:
                    csbk = new CapacityPlusNeighbors(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CAPPLUS_PREAMBLE:
                    csbk = new CapacityPlusPreamble(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CAPPLUS_SYSTEM_STATUS:
                    csbk = new CapacityPlusSystemStatus(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CAPPLUS_DATA_WINDOW_ANNOUNCEMENT:
                    csbk = new CapacityPlusDataRevertWindowAnnouncement(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CAPPLUS_DATA_WINDOW_GRANT:
                    csbk = new CapacityPlusDataRevertWindowGrant(pattern, message, cach, slotType, timestamp, timeslot);
                    break;

                case MOTOROLA_CONPLUS_CSBKO_10:
                    csbk = new ConnectPlusOTAAnnouncement(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CONPLUS_CSBKO_16:
                    csbk = new ConnectPlusCSBKO_16(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CONPLUS_REGISTRATION_REQUEST:
                    csbk = new ConnectPlusRegistrationRequest(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CONPLUS_REGISTRATION_RESPONSE:
                    csbk = new ConnectPlusRegistrationResponse(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CONPLUS_TALKGROUP_AFFILIATION:
                    csbk = new ConnectPlusTalkgroupAffiliation(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CAPPLUS_CSBKO_60:
                    csbk = new CapacityPlusCSBKO_60(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CONPLUS_DATA_CHANNEL_GRANT:
                    csbk = new ConnectPlusDataChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CONPLUS_NEIGHBOR_REPORT:
                    csbk = new ConnectPlusNeighborReport(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CONPLUS_TERMINATE_CHANNEL_GRANT:
                    csbk = new ConnectPlusTerminateChannelGrant(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CONPLUS_VOICE_CHANNEL_USER:
                    csbk = new ConnectPlusVoiceChannelUser(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CONPLUS_DATA_WINDOW_ANNOUNCEMENT:
                    csbk = new ConnectPlusDataRevertWindowAnnouncement(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
                case MOTOROLA_CONPLUS_DATA_WINDOW_GRANT:
                    csbk = new ConnectPlusDataRevertWindowGrant(pattern, message, cach, slotType, timestamp, timeslot);
                    break;
            }

            if(csbk == null)
            {
                csbk = new UnknownCSBKMessage(pattern, message, cach, slotType, timestamp, timeslot);
            }
        }

        //Check CRC and set valid flag
        if(csbk != null)
        {
            csbk.checkCRC();
        }

        return csbk;
    }

    /**
     * Creates a multi-block CSBK message
     * @param header for the Multi-Block CSBK (MBC)
     * @param continuationBlocks that form the rest of the message
     * @return message
     */
    public static CSBKMessage create(MBCHeader header, List<MBCContinuationBlock> continuationBlocks)
    {
        CSBKMessage csbk;

        Opcode opcode = header.getOpcode();

        switch(opcode)
        {
            case STANDARD_ANNOUNCEMENT:
                AnnouncementType announcementType = Announcement.getAnnouncementType(header.getMessage());
                switch(announcementType)
                {
                    case ADJACENT_SITE_INFORMATION:
                        csbk = new AdjacentSiteInformation(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                            header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                        break;
                    case ANNOUNCE_OR_WITHDRAW_TSCC:
                        csbk = new AnnounceWithdrawTSCC(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                            header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                        break;
                    case CHANNEL_FREQUENCY_ANNOUNCEMENT:
                        csbk = new AnnounceChannelFrequency(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                            header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                        break;
                    case VOTE_NOW_ADVICE:
                        csbk = new VoteNowAdvice(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                            header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                        break;
                    default:
                        csbk = new UnknownMultiCSBK(header, continuationBlocks);
                        break;
                }
                break;
            case STANDARD_BROADCAST_TALKGROUP_VOICE_CHANNEL_GRANT:
                csbk = new BroadcastTalkgroupVoiceChannelGrant(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                    header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                break;
            case STANDARD_CLEAR:
                csbk = new Clear(header.getSyncPattern(), header.getMessage(), header.getCACH(), header.getSlotType(),
                    header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                break;
            case STANDARD_DUPLEX_PRIVATE_DATA_CHANNEL_GRANT:
                csbk = new DuplexPrivateDataChannelGrant(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                    header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                break;
            case STANDARD_DUPLEX_PRIVATE_VOICE_CHANNEL_GRANT:
                csbk = new DuplexPrivateVoiceChannelGrant(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                    header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                break;
            case STANDARD_MOVE_TSCC:
                csbk = new MoveTSCC(header.getSyncPattern(), header.getMessage(), header.getCACH(), header.getSlotType(),
                    header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                break;
            case STANDARD_PRIVATE_DATA_CHANNEL_GRANT_SINGLE_ITEM:
                csbk = new PrivateDataChannelGrant(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                    header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                break;
            case STANDARD_PRIVATE_VOICE_CHANNEL_GRANT:
                csbk = new PrivateVoiceChannelGrant(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                    header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                break;
            case STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_SINGLE_ITEM:
                csbk = new TalkgroupDataChannelGrant(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                    header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                break;
            case STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT:
                csbk = new TalkgroupVoiceChannelGrant(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                    header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                break;

            case HYTERA_08_ANNOUNCEMENT:
            case HYTERA_68_ANNOUNCEMENT:
                AnnouncementType hyteraAnnouncementType = HyteraAnnouncement.getAnnouncementType(header.getMessage());
                switch(hyteraAnnouncementType)
                {
                    case ADJACENT_SITE_INFORMATION:
                        csbk = new HyteraAdjacentSiteInformation(header.getSyncPattern(), header.getMessage(), header.getCACH(),
                            header.getSlotType(), header.getTimestamp(), header.getTimeslot(), getBlock1(continuationBlocks));
                        break;
                    default:
                        csbk = new UnknownMultiCSBK(header, continuationBlocks);
                        break;
                }
                break;
            default:
                csbk = new UnknownMultiCSBK(header, continuationBlocks);
                break;
        }

        //Check CRC and set valid flag
        if(csbk != null)
        {
            csbk.checkCRC();
        }

        return csbk;
    }

    /**
     * Extracts the first block from the list
     * @param blocks containing zero or more blocks
     * @return first block or null
     */
    private static MBCContinuationBlock getBlock1(List<MBCContinuationBlock> blocks)
    {
        if(blocks != null && blocks.size() >= 1)
        {
            return blocks.get(0);
        }

        return null;
    }
}
