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

// Reference: OP25 tk_smartnet.py lines 806-1764 (boatbod/op25)
package io.github.dsheirer.module.decode.moto.message;

import io.github.dsheirer.module.decode.moto.Bandplan;
import io.github.dsheirer.module.decode.moto.message.OswQueue.OswEntry;

import java.util.ArrayList;
import java.util.List;

public class MotorolaTypeIIMessageFactory
{
    private static final int CMD_IDLE = 0x2F8;
    private static final int CMD_GROUP_BUSY = 0x300;
    private static final int CMD_PRIVATE_BUSY = 0x302;
    private static final int CMD_EMERGENCY_BUSY = 0x303;
    private static final int CMD_ANALOG_GRANT = 0x308;
    private static final int CMD_DIGITAL_GRANT = 0x321;
    private static final int CMD_SYSTEM_INFO = 0x320;
    private static final int CMD_INTERCONNECT_REJECT = 0x324;
    private static final int CMD_SEND_AFFILIATION = 0x32A;
    private static final int CMD_SYSTEM_ID = 0x32B;
    private static final int CMD_ROAMING = 0x32C;
    private static final int CMD_BSI = 0x3A0;
    private static final int CMD_NETWORK_STATUS = 0x3BF;
    private static final int CMD_SYSTEM_STATUS = 0x3C0;

    private static final int CMD2_DYNAMIC_REGROUP = 0x30A;
    private static final int CMD2_SYSTEM_ID_CC = 0x30B;
    private static final int CMD2_STATUS_ACK = 0x30D;
    private static final int CMD2_AFFILIATION = 0x310;
    private static final int CMD2_MESSAGE = 0x311;
    private static final int CMD2_PRIVATE_CALL_RING_ENC = 0x315;
    private static final int CMD2_PRIVATE_CALL_RING_CLR = 0x317;
    private static final int CMD2_PRIVATE_CALL_RING_ACK = 0x318;
    private static final int CMD2_CALL_ALERT = 0x319;
    private static final int CMD2_CALL_ALERT_ACK = 0x31A;
    private static final int CMD2_OMNILINK_TRESPASS = 0x31B;
    private static final int CMD2_DATE_TIME = 0x322;
    private static final int CMD2_EMERGENCY_PTT = 0x32E;
    private static final int CMD2_PATCH = 0x340;

    private static final int ADDR_CC_BROADCAST_MASK = 0xFF00;
    private static final int ADDR_CC_BROADCAST_VALUE = 0x1F00;
    private static final int ADDR_ADJACENT_MASK = 0xFC00;
    private static final int ADDR_ADJACENT_VALUE = 0x6000;
    private static final int ADDR_ALT_CC_MASK = 0xFC00;
    private static final int ADDR_ALT_CC_VALUE = 0x2800;

    private static final int ADDR_RADIO_CHECK = 0x261B;
    private static final int ADDR_DEAFFILIATION = 0x261C;
    private static final int ADDR_STATUS_ACK_MIN = 0x26E0;
    private static final int ADDR_STATUS_ACK_MAX = 0x26E7;
    private static final int ADDR_EMERGENCY_ACK = 0x26E8;
    private static final int ADDR_PATCH_CANCEL = 0x2021;

    public static List<MotorolaTypeIIMessage> process(OswQueue queue, Bandplan bandplan)
    {
        List<MotorolaTypeIIMessage> messages = new ArrayList<>();

        if(!queue.isFull() || queue.hasReset())
        {
            if(queue.hasReset())
            {
                queue.clearToReset();
            }
            return messages;
        }

        OswEntry osw0 = queue.getOldest();   // index 0 - first OSW of multi-OSW message
        OswEntry osw1 = queue.getSecond();   // index 1 - second OSW
        OswEntry osw2 = queue.getThird();    // index 2 - third OSW (for 3-OSW messages)

        if(isIdle(osw0))
        {
            queue.removeOldest();
            if(isIdle(osw1))
            {
                queue.removeOldest();
            }
            return messages;
        }

        // Check for 3-OSW system info first (osw0=0x308, osw1=0x320, osw2=0x30B)
        if(osw0.command == CMD_ANALOG_GRANT && osw1.command == CMD_SYSTEM_INFO &&
           osw2.command == CMD2_SYSTEM_ID_CC)
        {
            return processThreeOswSystem(queue, osw0, osw1, osw2);
        }

        // 3-OSW CONTROL CHANNEL: osw0=0x308, osw1=0x30B (0x28xx grp), osw2=channel (0x1Fxx addr)
        // OP25 lines 1127-1131
        if(osw0.command == CMD_ANALOG_GRANT &&
           osw1.command == CMD2_SYSTEM_ID_CC && osw1.isGroup &&
           osw2.isChannel && !osw2.isGroup &&
           (osw2.address & ADDR_CC_BROADCAST_MASK) == ADDR_CC_BROADCAST_VALUE &&
           (osw1.address & ADDR_ALT_CC_MASK) == ADDR_ALT_CC_VALUE &&
           (osw1.address & 0x3FF) == osw2.command)
        {
            return processThreeOswControlChannel(queue, osw0, osw1, osw2);
        }

        // Check for 2-OSW analog grant or sub-command (cmd 0x308)
        // OP25: when osw0.cmd == 0x308, ALWAYS dispatch to processAnalogGrant
        // which checks for channel (grant) OR non-channel sub-commands (PATCH, AFFILIATION, etc.)
        if(osw0.command == CMD_ANALOG_GRANT && osw0.address != 0)
        {
            return processAnalogGrant(queue, osw0, osw1);
        }
        
        if(osw1.command == CMD_ANALOG_GRANT && osw1.address != 0)
        {
            return processAnalogGrant(queue, osw1, osw2);
        }

        // Check for 2-OSW digital grant or sub-command (cmd 0x321)
        if(osw0.command == CMD_DIGITAL_GRANT && osw0.address != 0)
        {
            return processDigitalGrant(queue, osw0, osw1);
        }
        
        if(osw1.command == CMD_DIGITAL_GRANT && osw1.address != 0)
        {
            return processDigitalGrant(queue, osw1, osw2);
        }

        // Handle IDLE interleaving: if osw1 is IDLE between osw0 and osw2
        if(isIdle(osw1) && (osw0.command == CMD_ANALOG_GRANT || osw0.command == CMD_DIGITAL_GRANT))
        {
            // Remove osw0 and osw1 (IDLE), put osw0 back at front
            queue.removeOldest();  // remove osw0
            queue.removeOldest();  // remove osw1 (IDLE)
            queue.pushFront(osw0); // put osw0 back
            return messages;
        }

        // No multi-OSW pattern matched - process osw0 as one-OSW message
        return processOneOsw(queue, osw0);
    }

    private static List<MotorolaTypeIIMessage> processThreeOswSystem(OswQueue queue, OswEntry osw0,
                                                                       OswEntry osw1, OswEntry osw2)
    {
        List<MotorolaTypeIIMessage> messages = new ArrayList<>();

        if((osw2.address & ADDR_ADJACENT_MASK) == ADDR_ADJACENT_VALUE)
        {
            messages.add(createMessage(MotorolaTypeIIMessageType.ADJACENT_SITE, osw0.address,
                osw2.isGroup, osw0.command, osw2.address));
        }
        else
        {
            messages.add(createMessage(MotorolaTypeIIMessageType.SYSTEM_INFO, osw0.address,
                true, osw0.command, osw2.address));
        }

        queue.removeOldest();
        queue.removeOldest();
        queue.removeOldest();
        return messages;
    }

    private static List<MotorolaTypeIIMessage> processThreeOswControlChannel(OswQueue queue, OswEntry osw0,
                                                                                OswEntry osw1, OswEntry osw2)
    {
        List<MotorolaTypeIIMessage> messages = new ArrayList<>();

        // osw0.address = system ID, osw2.command = channel number (CC frequency)
        messages.add(createMessage(MotorolaTypeIIMessageType.CONTROL_CHANNEL, osw0.address,
            false, osw0.command, osw2.command));

        queue.removeOldest();
        queue.removeOldest();
        queue.removeOldest();
        return messages;
    }

    private static List<MotorolaTypeIIMessage> processAnalogGrant(OswQueue queue, OswEntry oldest, OswEntry newer)
    {
        List<MotorolaTypeIIMessage> messages = new ArrayList<>();

        // 2-OSW CONTROL CHANNEL: oldest=0x308, newer=channel with 0x1Fxx addr (CC broadcast pattern)
        // OP25 lines 985-993
        if(newer.isChannel && (newer.address & ADDR_CC_BROADCAST_MASK) == ADDR_CC_BROADCAST_VALUE)
        {
            messages.add(createMessage(MotorolaTypeIIMessageType.CONTROL_CHANNEL, oldest.address,
                false, oldest.command, newer.channelNumber));
            queue.removeOldest();
            queue.removeOldest();
            return messages;
        }

        // OP25: 0x308 OSW (oldest) has source RID in address; channel cmd OSW (newer) has talkgroup in address
        if(newer.isChannel && oldest.isGroup && oldest.address != 0 && newer.address != 0)
        {
            messages.add(createGrantMessage(MotorolaTypeIIMessageType.ANALOG_GROUP_GRANT,
                newer.address, true, newer.command, newer.channelNumber, oldest.address));
            queue.removeOldest();
            queue.removeOldest();
            return messages;
        }

        if(!newer.isGroup && newer.isChannel && oldest.address != 0 && newer.address != 0)
        {
            // Private call: 0x308 OSW has destination RID, channel cmd OSW has source RID
            messages.add(createGrantMessage(MotorolaTypeIIMessageType.ANALOG_PRIVATE_CALL,
                oldest.address, false, newer.command, newer.channelNumber, newer.address));
            queue.removeOldest();
            queue.removeOldest();
            return messages;
        }

        if(!newer.isGroup && (oldest.address & ADDR_CC_BROADCAST_MASK) == ADDR_CC_BROADCAST_VALUE)
        {
            messages.add(createMessage(MotorolaTypeIIMessageType.SYSTEM_ID_CC, newer.address,
                false, newer.command, newer.isChannel ? newer.channelNumber : 0));
            queue.removeOldest();
            queue.removeOldest();
            return messages;
        }

        if(isIdle(newer))
        {
            queue.removeOldest();
            queue.removeOldest();
            queue.add(oldest);
            return messages;
        }

        MotorolaTypeIIMessageType subType = mapAnalogSubCommand(oldest, newer);

        if(subType != null)
        {
            messages.add(createMessage(subType, oldest.address, oldest.isGroup, oldest.command, newer.address));
            queue.removeOldest();
            queue.removeOldest();
            return messages;
        }

        return messages;
    }

    private static List<MotorolaTypeIIMessage> processDigitalGrant(OswQueue queue, OswEntry oldest, OswEntry newer)
    {
        List<MotorolaTypeIIMessage> messages = new ArrayList<>();

        // OP25: 0x321 OSW (oldest) has source RID in address; channel cmd OSW (newer) has talkgroup in address
        if(newer.isChannel && oldest.isGroup && newer.isGroup && oldest.address != 0)
        {
            messages.add(createGrantMessage(MotorolaTypeIIMessageType.DIGITAL_GROUP_GRANT,
                newer.address, true, newer.command, newer.channelNumber, oldest.address));
            queue.removeOldest();
            queue.removeOldest();
            return messages;
        }

        if(!newer.isGroup && newer.isChannel && oldest.address != 0 && newer.address != 0)
        {
            // Private call: 0x321 OSW has destination RID, channel cmd OSW has source RID
            messages.add(createGrantMessage(MotorolaTypeIIMessageType.DIGITAL_PRIVATE_CALL,
                oldest.address, false, newer.command, newer.channelNumber, newer.address));
            queue.removeOldest();
            queue.removeOldest();
            return messages;
        }

        if(isIdle(newer))
        {
            queue.removeOldest();
            queue.removeOldest();
            queue.add(oldest);
            return messages;
        }

        if(!oldest.isGroup && !newer.isGroup)
        {
            MotorolaTypeIIMessageType ringType = mapDigitalSubCommand(newer.command);
            if(ringType != null)
            {
                messages.add(createMessage(ringType, oldest.address, false, oldest.command, newer.address));
                queue.removeOldest();
                queue.removeOldest();
                return messages;
            }
        }

        return messages;
    }

    private static MotorolaTypeIIMessageType mapAnalogSubCommand(OswEntry oldest, OswEntry newer)
    {
        switch(newer.command)
        {
            case CMD2_DYNAMIC_REGROUP:
                if(!oldest.isGroup && !newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.DYNAMIC_REGROUP;
                }
                return null;
            case CMD2_SYSTEM_ID_CC:
                return mapSystemIdCC(oldest, newer);
            case CMD2_STATUS_ACK:
                if(!oldest.isGroup && !newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.STATUS_ACK;
                }
                return null;
            case CMD2_AFFILIATION:
                if(!oldest.isGroup && !newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.AFFILIATION;
                }
                return null;
            case CMD2_MESSAGE:
                if(!oldest.isGroup && !newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.MESSAGE;
                }
                return null;
            case CMD2_PRIVATE_CALL_RING_ENC:
            case CMD2_PRIVATE_CALL_RING_CLR:
            case CMD2_PRIVATE_CALL_RING_ACK:
                if(!oldest.isGroup && !newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.PRIVATE_CALL_RING;
                }
                return null;
            case CMD2_CALL_ALERT:
                if(!oldest.isGroup && !newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.CALL_ALERT;
                }
                return null;
            case CMD2_CALL_ALERT_ACK:
                if(!oldest.isGroup && !newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.CALL_ALERT_ACK;
                }
                return null;
            case CMD2_OMNILINK_TRESPASS:
                if(!oldest.isGroup && !newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.OMNILINK_TRESPASS;
                }
                return null;
            case CMD2_DATE_TIME:
                if(oldest.isGroup && newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.DATE_TIME;
                }
                return null;
            case CMD2_EMERGENCY_PTT:
                if(oldest.isGroup && newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.EMERGENCY_PTT;
                }
                return null;
            case CMD2_PATCH:
                if(oldest.isGroup && newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.PATCH;
                }
                return null;
            case CMD_GROUP_BUSY:
                if(newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.GROUP_BUSY_QUEUED;
                }
                return null;
            case CMD_PRIVATE_BUSY:
                if(!newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.PRIVATE_CALL_BUSY;
                }
                return null;
            case CMD_EMERGENCY_BUSY:
                if(newer.isGroup)
                {
                    return MotorolaTypeIIMessageType.EMERGENCY_BUSY_QUEUED;
                }
                return null;
            default:
                return null;
        }
    }

    private static MotorolaTypeIIMessageType mapSystemIdCC(OswEntry oldest, OswEntry newer)
    {
        if((newer.address & ADDR_ALT_CC_MASK) == ADDR_ALT_CC_VALUE && newer.isGroup)
        {
            return MotorolaTypeIIMessageType.SYSTEM_ID_CC;
        }

        if((newer.address & ADDR_ADJACENT_MASK) == ADDR_ADJACENT_VALUE)
        {
            return MotorolaTypeIIMessageType.ADJACENT_SITE;
        }

        if(!oldest.isGroup && !newer.isGroup)
        {
            if(newer.address == ADDR_RADIO_CHECK)
            {
                return MotorolaTypeIIMessageType.RADIO_CHECK;
            }
            if(newer.address == ADDR_DEAFFILIATION)
            {
                return MotorolaTypeIIMessageType.DEAFFILIATION;
            }
            if(newer.address >= ADDR_STATUS_ACK_MIN && newer.address <= ADDR_STATUS_ACK_MAX)
            {
                return MotorolaTypeIIMessageType.STATUS_ACK;
            }
        }

        if(oldest.isGroup && newer.isGroup)
        {
            if(newer.address == ADDR_PATCH_CANCEL)
            {
                return MotorolaTypeIIMessageType.PATCH;
            }
        }

        return MotorolaTypeIIMessageType.SYSTEM_ID_CC;
    }

    private static MotorolaTypeIIMessageType mapDigitalSubCommand(int command)
    {
        switch(command)
        {
            case CMD2_PRIVATE_CALL_RING_ENC:
            case CMD2_PRIVATE_CALL_RING_CLR:
            case CMD2_PRIVATE_CALL_RING_ACK:
                return MotorolaTypeIIMessageType.PRIVATE_CALL_RING;
            default:
                return null;
        }
    }

    private static List<MotorolaTypeIIMessage> processOneOsw(OswQueue queue, OswEntry osw)
    {
        List<MotorolaTypeIIMessage> messages = new ArrayList<>();
        MotorolaTypeIIMessageType type = null;

        // One-OSW voice update (OP25: osw2_ch_rx and osw2_grp)
        // Talkgroup is in address, channel is in command field
        if(osw.isChannel && osw.isGroup)
        {
            type = MotorolaTypeIIMessageType.GROUP_UPDATE;
        }
        // One-OSW control channel broadcast (OP25: osw2_ch_rx and not osw2_grp and addr mask 0x1F00)
        else if(osw.isChannel && !osw.isGroup && (osw.address & ADDR_CC_BROADCAST_MASK) == ADDR_CC_BROADCAST_VALUE)
        {
            type = MotorolaTypeIIMessageType.CC_BROADCAST;
        }
        else if(osw.command == CMD_SYSTEM_ID && !osw.isGroup)
        {
            type = MotorolaTypeIIMessageType.SYSTEM_ID;
        }
        // AMSS - OP25 does NOT check group flag
        else if(osw.command >= 0x360 && osw.command <= 0x39F)
        {
            type = MotorolaTypeIIMessageType.AMSS;
        }
        // NETWORK_STATUS and SYSTEM_STATUS - OP25 does NOT check group flag (line 1692)
        else if(osw.command == CMD_NETWORK_STATUS || osw.command == CMD_SYSTEM_STATUS)
        {
            type = (osw.command == CMD_SYSTEM_STATUS) ? MotorolaTypeIIMessageType.SYSTEM_STATUS : MotorolaTypeIIMessageType.NETWORK_STATUS;
        }
        else if(osw.command == CMD_GROUP_BUSY && osw.isGroup)
        {
            type = MotorolaTypeIIMessageType.GROUP_BUSY;
        }
        else if(osw.command == CMD_EMERGENCY_BUSY && osw.isGroup)
        {
            type = MotorolaTypeIIMessageType.EMERGENCY_BUSY;
        }
        else if(osw.command == CMD_SEND_AFFILIATION)
        {
            type = MotorolaTypeIIMessageType.SEND_AFFILIATION;
        }
        else if(osw.command == CMD_ROAMING && !osw.isGroup)
        {
            type = MotorolaTypeIIMessageType.ROAMING;
        }
        else if(osw.command == CMD_INTERCONNECT_REJECT && !osw.isGroup)
        {
            type = MotorolaTypeIIMessageType.INTERCONNECT_REJECT;
        }
        else if(osw.command == CMD_BSI && osw.isGroup)
        {
            type = MotorolaTypeIIMessageType.BSI_DIAGNOSTIC;
        }

        if(type != null)
        {
            // For GROUP_UPDATE, talkgroup is in address, channel is in command field (osw.channelNumber)
            // For CC_BROADCAST, channel is in command field
            int channelNumber = osw.isChannel ? osw.channelNumber : 0;
            messages.add(createMessage(type, osw.address, osw.isGroup, osw.command, channelNumber));
            queue.removeOldest();
        }
        else
        {
            // No type matched - advance queue to prevent getting stuck
            queue.removeOldest();
        }

        return messages;
    }

    private static boolean isIdle(OswEntry entry)
    {
        return entry.command == CMD_IDLE && !entry.isGroup;
    }

    private static MotorolaTypeIIMessage createMessage(MotorolaTypeIIMessageType type, int address,
                                                        boolean isGroup, int command, int channelNumber)
    {
        return new MotorolaTypeIIMessage(address, isGroup, command, channelNumber, null, true, type);
    }

    private static MotorolaTypeIIMessage createGrantMessage(MotorolaTypeIIMessageType type, int talkgroupAddress,
                                                              boolean isGroup, int command, int channelNumber,
                                                              int sourceAddress)
    {
        return new MotorolaTypeIIMessage(talkgroupAddress, isGroup, command, channelNumber, sourceAddress, null, true, type);
    }
}
