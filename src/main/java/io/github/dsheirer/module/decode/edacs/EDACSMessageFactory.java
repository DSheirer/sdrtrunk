/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.edacs.message.EDACSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EDACS control channel message factory. Parses the 28-bit data payload
 * (or two 28-bit payloads for two-word frames) into an {@link EDACSMessage}
 * with the message type, parsed LCN/talkgroup/radio/site/system fields,
 * and identifiers populated.
 *
 * <p>Field layouts re-implemented from the DSD-FME
 * {@code edacs-fme.c} dispatch table (US patent US7546135B2). Each
 * outbound control channel frame carries two 28-bit messages; the
 * first 5 bits of each are {@code MT1}, which selects the major
 * dispatch. {@code MT1 == 0x1F} indicates a system message and the
 * next 4 bits are {@code MT2}.</p>
 */
public class EDACSMessageFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(EDACSMessageFactory.class);

    public static EDACSMessage create(CorrectedBinaryMessage data, long timestamp)
    {
        return create(data, null, timestamp);
    }

    public static EDACSMessage create(CorrectedBinaryMessage data, CorrectedBinaryMessage data2, long timestamp)
    {
        return create(data, data2, timestamp, false);
    }

    /**
     * Creates a message.
     * @param esk true for an EDACS Security Key (ESK) control channel.  The ESK mask is removed from both 28-bit
     * messages before they are parsed, as DSD-FME edacs-fme.c does.  The mask is applied after error correction, so
     * the stored message bits stay as they were received.
     */
    public static EDACSMessage create(CorrectedBinaryMessage data, CorrectedBinaryMessage data2, long timestamp,
                                      boolean esk)
    {
        int msg_1 = getInt(data, 0, 28);
        int msg_2 = data2 != null ? getInt(data2, 0, 28) : 0;

        if(esk)
        {
            int mask = DecodeConfigEDACS.ESK_MASK << 20;
            msg_1 ^= mask;
            msg_2 ^= mask;
        }

        int mt1 = (msg_1 >> 23) & 0x1F;
        int mt2 = (msg_1 >> 19) & 0x0F;

        EDACSMessage message = new EDACSMessage(EDACSMessageType.UNKNOWN, data, data2, timestamp);
        StringBuilder details = new StringBuilder();

        if(mt1 == 0x1F)
        {
            dispatchSystemMessage(message, msg_1, msg_2, mt2, details);
        }
        else if(mt1 == 0x19)
        {
            dispatchLogin(message, msg_1, msg_2, details);
        }
        else
        {
            dispatchCallMessage(message, msg_1, msg_2, mt1, details);
        }

        if(details.length() > 0)
        {
            message.setDetails(details.toString().trim());
        }
        return message;
    }

    /**
     * Dispatch for MT1=0x1F (system messages). MT2 selects the subtype.
     */
    private static void dispatchSystemMessage(EDACSMessage message, int msg_1, int msg_2, int mt2, StringBuilder details)
    {
        switch(mt2)
        {
            case 0x0: // Initiate Test Call
            {
                // DSD-FME: CC LCN [cc_lcn], WC LCN [wc_lcn]
                // MSG_1 bits 17-12 = cc_lcn, bits 11-7 = wc_lcn (shifted)
                int ccLcn = (msg_1 & 0x3E000) >> 13;
                int wcLcn = (msg_1 & 0xF80) >> 7;
                message.setMessageType(EDACSMessageType.TEST_CALL);
                message.setCcLcn(ccLcn);
                message.setLCN(wcLcn);
                details.append("CC LCN:").append(ccLcn)
                        .append(" WC LCN:").append(wcLcn);
                break;
            }
            case 0x1: // Adjacent Site
            {
                int adjLcn = (msg_1 & 0x1F000) >> 12;
                int adjIndex = (msg_1 & 0xF00) >> 8;
                int adjSite = msg_1 & 0xFF;
                message.setMessageType(EDACSMessageType.ADJACENT_SITE);
                message.setAdjLcn(adjLcn);
                message.setAdjIndex(adjIndex);
                message.setAdjSiteId(adjSite);
                message.setLCN(adjLcn);
                details.append("Site:").append(adjSite)
                        .append(" Idx:").append(adjIndex)
                        .append(" CC LCN:").append(adjLcn);
                break;
            }
            case 0x4: // Status/Message
            {
                int status = msg_1 & 0xFF;
                int source = msg_2 & 0xFFFFF;
                message.setMessageType(EDACSMessageType.STATUS_MESSAGE);
                message.setSource(source);
                if(status == 248)
                {
                    details.append("Status Request Target:").append(source);
                }
                else
                {
                    details.append("Status:").append(status)
                            .append(" Source:").append(source);
                }
                break;
            }
            case 0x7: // Unit Enable/Disable
            {
                int qualifier = (msg_2 & 0xC00000) >> 22;
                int target = msg_2 & 0xFFFFF;
                message.setMessageType(EDACSMessageType.UNIT_ENABLE_DISABLE);
                message.setTarget(target);
                String q;
                switch(qualifier)
                {
                    case 0x0: q = "Temporary Disable"; break;
                    case 0x1: q = "Corrupt Personality"; break;
                    case 0x2: q = "Revoke Logical ID"; break;
                    default:   q = "Re-enable Unit"; break;
                }
                details.append(q).append(" Target:").append(target);
                break;
            }
            case 0x8: // System Information (CC LCN)
            {
                int ccLcn = msg_2 & 0x1F;
                int systemId = msg_1 & 0xFFFF;
                if(ccLcn == 0)
                {
                    // No CC LCN info, skip per DSD-FME convention
                    message.setMessageType(EDACSMessageType.UNKNOWN);
                    return;
                }
                message.setMessageType(EDACSMessageType.SYSTEM_INFO);
                message.setSystemId(systemId);
                message.setCcLcn(ccLcn);
                details.append("System ID:").append(String.format("%04X", systemId))
                        .append(" CC LCN:").append(ccLcn);
                break;
            }
            case 0xA: // Extended Addressing (Site ID + Area)
            {
                int siteId = ((msg_1 & 0x7000) >> 7) | (msg_1 & 0x1F);
                int area = (msg_1 & 0xFE0) >> 5;
                message.setMessageType(EDACSMessageType.EXTENDED_ADDRESSING);
                message.setSiteId(siteId);
                message.setArea(area);
                details.append("Site:").append(String.format("%02X", siteId))
                        .append("(").append(siteId).append(")")
                        .append(" Area:").append(String.format("%02X", area))
                        .append("(").append(area).append(")");
                break;
            }
            case 0xB: // Dynamic Regroup Plan Bitmap
            {
                int bank1 = (msg_1 & 0x10000) >> 16;
                int resident1 = (msg_1 & 0xFF00) >> 8;
                int active1 = msg_1 & 0xFF;
                int bank2 = (msg_2 & 0x10000) >> 16;
                int resident2 = (msg_2 & 0xFF00) >> 8;
                int active2 = msg_2 & 0xFF;
                message.setMessageType(EDACSMessageType.DYNAMIC_REGROUP_PLAN);
                message.setBank1(bank1);
                message.setResident1(resident1);
                message.setActive1(active1);
                message.setBank2(bank2);
                message.setResident2(resident2);
                message.setActive2(active2);
                details.append("Bank1:").append(bank1)
                        .append(" Res:").append(String.format("%02X", resident1))
                        .append(" Act:").append(String.format("%02X", active1))
                        .append(" Bank2:").append(bank2)
                        .append(" Res:").append(String.format("%02X", resident2))
                        .append(" Act:").append(String.format("%02X", active2));
                break;
            }
            case 0xC: // Dynamic Regroup Detail (SP-WGID, patches)
            {
                //Field layout per DSD-FME edacs-fme.c, which reverse engineered it from live systems using the L3Harris
                //P25 regroup messages as a guide.  MT1 and MT2 are the top 9 bits of msg_1 (bits 27-19), so the
                //option starts at bit 18.  Option is 7 for an active patch.  A message whose target is the supergroup
                //itself deletes the patch.  SSN differs per site for the same supergroup.
                int option = (msg_1 >> 16) & 0x7;
                int wgid = msg_1 & 0xFF;
                int ssn = (msg_2 >> 23) & 0x1F;
                int target = msg_2 & 0xFFFFF;
                message.setMessageType(EDACSMessageType.DYNAMIC_REGROUP_DETAIL);
                message.setGroup(wgid);
                message.setTarget(target);
                message.setRegroupOption(option);
                message.setRegroupSsn(ssn);
                details.append("SP-WGID:").append(wgid);

                if(message.isRegroupDelete())
                {
                    details.append(" Patch Delete");
                }
                else
                {
                    details.append(" Target:").append(target)
                            .append(" Opt:").append(option)
                            .append(" SSN:").append(String.format("%02X", ssn));
                }
                break;
            }
            default:
            {
                // Unknown MT2 - leave as UNKNOWN, no fields set
                details.append("MT1:1F MT2:").append(mt2);
                break;
            }
        }
    }

    /**
     * Dispatch for MT1=0x19 (Login).
     */
    private static void dispatchLogin(EDACSMessage message, int msg_1, int msg_2, StringBuilder details)
    {
        int group = msg_1 & 0xFFFF;
        int source = msg_2 & 0xFFFFF;
        // Validation: a real login has a non-zero group. Source can
        // legitimately be 0 for some logins, but group=0 is not
        // valid. Reject group=0 to filter out BCH-corrected noise.
        if(group == 0)
        {
            message.setMessageType(EDACSMessageType.UNKNOWN);
            return;
        }
        message.setMessageType(EDACSMessageType.LOGIN);
        message.setGroup(group);
        message.setSource(source);
        details.append("Group:").append(group)
                .append(" Source:").append(source);
    }

    /**
     * Dispatch for MT1 in 0x00-0x1E (call grants and data messages).
     */
    private static void dispatchCallMessage(EDACSMessage message, int msg_1, int msg_2, int mt1, StringBuilder details)
    {
        switch(mt1)
        {
            case 0x01: // TDMA Group Call
            {
                int lcn = (msg_1 & 0x3E0000) >> 17;
                int group = msg_1 & 0xFFFF;
                int source = msg_2 & 0xFFFFF;
                message.setMessageType(EDACSMessageType.TDMA_GROUP_CALL);
                message.setLCN(lcn);
                message.setGroup(group);
                message.setSource(source);
                details.append("TDMA Group Call TG:").append(group)
                        .append(" Source:").append(source)
                        .append(" LCN:").append(lcn);
                break;
            }
            case 0x02: // Data Group Call
            {
                int lcn = (msg_1 & 0x3E0000) >> 17;
                int group = msg_1 & 0xFFFF;
                int source = msg_2 & 0xFFFFF;
                message.setMessageType(EDACSMessageType.DATA_GROUP_CALL);
                message.setLCN(lcn);
                message.setGroup(group);
                message.setSource(source);
                details.append("Data Group Call TG:").append(group)
                        .append(" Source:").append(source)
                        .append(" LCN:").append(lcn);
                break;
            }
            case 0x03: // Digital Group Call (ProVoice)
            case 0x06: // Analog Group Call
            {
                int lcn = (msg_1 & 0x3E0000) >> 17;
                int isDigital = (mt1 == 0x03) ? 1 : 0;
                int isUpdate = (msg_1 & 0x10000) >> 16;
                int group = msg_1 & 0xFFFF;
                int isTxTrunking = (msg_2 & 0x200000) >> 21;
                int isEmergency = (msg_2 & 0x100000) >> 20;
                int source = msg_2 & 0xFFFFF;
                // Validation: a real call grant has a non-zero LCN (1-25)
                // and a non-zero talkgroup. BCH-corrected noise frequently
                // produces LCN=0 or group=0; reject those to keep them
                // from triggering traffic channel allocation.
                if(lcn == 0 || lcn > 25 || group == 0)
                {
                    message.setMessageType(EDACSMessageType.UNKNOWN);
                    return;
                }
                message.setMessageType(isDigital != 0 ? EDACSMessageType.DIGITAL_GROUP_CALL :
                        EDACSMessageType.ANALOG_GROUP_CALL);
                message.setLCN(lcn);
                message.setGroup(group);
                message.setSource(source);
                message.setDigital(isDigital != 0);
                message.setUpdate(isUpdate != 0);
                message.setTxTrunking(isTxTrunking != 0);
                message.setEmergency(isEmergency != 0);
                details.append(isDigital != 0 ? "Digital " : "Analog ")
                        .append(isUpdate != 0 ? "Update" : "Assignment")
                        .append(" TG:").append(group)
                        .append(" Source:").append(source)
                        .append(" LCN:").append(lcn);
                if(isTxTrunking != 0)
                {
                    details.append(" [TxTrunking]");
                }
                if(isEmergency != 0)
                {
                    details.append(" [EMERGENCY]");
                }
                break;
            }
            case 0x10: // I-Call (Individual Call)
            {
                int lcn = (msg_2 & 0x1F00000) >> 20;
                int isDigital = (msg_1 & 0x200000) >> 21;
                int isUpdate = (msg_1 & 0x100000) >> 20;
                int target = msg_1 & 0xFFFFF;
                int source = msg_2 & 0xFFFFF;
                message.setMessageType(EDACSMessageType.INDIVIDUAL_CALL);
                message.setLCN(lcn);
                message.setTarget(target);
                message.setSource(source);
                message.setDigital(isDigital != 0);
                message.setUpdate(isUpdate != 0);
                if(target == 0 && source == 0)
                {
                    // Test call variant (no actual call)
                    message.setMessageType(EDACSMessageType.TEST_CALL);
                    details.append("Test Call LCN:").append(lcn);
                }
                else
                {
                    details.append(isDigital != 0 ? "Digital " : "")
                            .append(isUpdate != 0 ? "Update" : "Assignment")
                            .append(" Target:").append(target)
                            .append(" Source:").append(source)
                            .append(" LCN:").append(lcn);
                }
                break;
            }
            case 0x12: // Channel Assignment (Unknown Data)
            {
                int lcn = (msg_2 & 0x1F00000) >> 20;
                int source = msg_2 & 0xFFFFF;
                message.setMessageType(EDACSMessageType.CHANNEL_ASSIGNMENT);
                message.setLCN(lcn);
                message.setSource(source);
                details.append("Source:").append(source)
                        .append(" LCN:").append(lcn);
                break;
            }
            case 0x16: // System All-Call
            {
                int lcn = (msg_1 & 0x3E0000) >> 17;
                int isDigital = (msg_1 & 0x10000) >> 16;
                int isUpdate = (msg_1 & 0x8000) >> 15;
                int source = msg_2 & 0xFFFFF;
                // Validation: a real all-call has a non-zero LCN.
                if(lcn == 0 || lcn > 25)
                {
                    message.setMessageType(EDACSMessageType.UNKNOWN);
                    return;
                }
                message.setMessageType(EDACSMessageType.ALL_CALL);
                message.setLCN(lcn);
                message.setGroup(0); // System all-call: TG=0
                message.setSource(source);
                message.setDigital(isDigital != 0);
                message.setUpdate(isUpdate != 0);
                details.append(isDigital != 0 ? "Digital " : "Analog ")
                        .append("All-Call ")
                        .append(isUpdate != 0 ? "Update" : "Assignment")
                        .append(" Source:").append(source)
                        .append(" LCN:").append(lcn);
                break;
            }
            default:
            {
                // Unknown MT1 - leave as UNKNOWN
                details.append("MT1:").append(String.format("%02X", mt1));
                break;
            }
        }
    }

    /**
     * Reads bits [offset, offset+length) from a {@link CorrectedBinaryMessage}
     * as an integer with bit {@code offset} as the MSB. Treats the bit
     * set as 1 and clear as 0.
     */
    private static int getInt(CorrectedBinaryMessage msg, int offset, int length)
    {
        int value = 0;
        for(int x = 0; x < length; x++)
        {
            if(msg.get(offset + x))
            {
                value |= (1 << (length - 1 - x));
            }
        }
        return value;
    }
}
