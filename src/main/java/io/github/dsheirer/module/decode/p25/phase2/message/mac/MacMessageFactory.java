/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ChannelNumber;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.EndPushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NullInformationMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnknownStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Message factory for creating Encoded MAC Information (EMI) message parser instances.
 */
public class MacMessageFactory
{
    public static final int DEFAULT_MAC_STRUCTURE_INDEX = 8;

    public static List<MacMessage> create(ChannelNumber channelNumber, DataUnitID dataUnitID,
                                          CorrectedBinaryMessage message, long timestamp)
    {
        List<MacMessage> messages = new ArrayList<>();

        MacPduType macPduType = MacMessage.getMacPduTypeFromMessage(message);

        switch(macPduType)
        {
            case MAC_0_RESERVED:
                break;
            case MAC_1_PTT:
                messages.add(new MacMessage(channelNumber, dataUnitID, message, timestamp, new PushToTalk(message)));
                break;
            case MAC_2_END_PTT:
                messages.add(new MacMessage(channelNumber, dataUnitID, message, timestamp, new EndPushToTalk(message)));
                break;
            case MAC_3_IDLE:
            case MAC_4_ACTIVE:
            case MAC_6_HANGTIME:
                List<Integer> indices = getMacStructureIndices(message);

                for(Integer index: indices)
                {
                    MacStructure macStructure = createMacStructure(message, index);
                    messages.add(new MacMessage(channelNumber, dataUnitID, message, timestamp, macStructure));
                }
                break;
            case MAC_5_RESERVED:
                break;
            case MAC_7_RESERVED:
                break;
            case MAC_UNKNOWN:
                break;
            default:
                messages.add(new UnknownMacMessage(channelNumber, dataUnitID, message, timestamp));
                break;
        }

        return messages;
    }

    /**
     * Identifies the MAC structure start indices for the message
     * @param message containing one or more MAC structures
     * @return structure start indices
     */
    private static List<Integer> getMacStructureIndices(CorrectedBinaryMessage message)
    {
        List<Integer> indices = new ArrayList<>();

        //There should always be a MAC structure at index 8
        indices.add(DEFAULT_MAC_STRUCTURE_INDEX);

        MacOpcode opcode = MacStructure.getOpcode(message, DEFAULT_MAC_STRUCTURE_INDEX);

        if(opcode.getLength() > 0 && opcode != MacOpcode.TDMA_0_NULL_INFORMATION_MESSAGE)
        {
            int secondStructureIndex = DEFAULT_MAC_STRUCTURE_INDEX + (opcode.getLength() * 8);

            if(secondStructureIndex < message.size())
            {
                MacOpcode secondOpcode = MacStructure.getOpcode(message, secondStructureIndex);

                if(secondOpcode != MacOpcode.TDMA_0_NULL_INFORMATION_MESSAGE)
                {
                    indices.add(secondStructureIndex);

                    if(secondOpcode.getLength() > 0)
                    {
                        int thirdStructureIndex = secondStructureIndex + (secondOpcode.getLength() * 8);

                        if(thirdStructureIndex < message.size())
                        {
                            MacOpcode thirdOpcode = MacStructure.getOpcode(message, thirdStructureIndex);

                            if(thirdOpcode != MacOpcode.TDMA_0_NULL_INFORMATION_MESSAGE)
                            {
                                indices.add(thirdStructureIndex);
                            }
                        }
                    }
                    else
                    {
                        //handle variable length structure for 3rd structure
                    }
                }

            }
        }
        else
        {
            //handle variable length opcode for 2nd structure
        }

        return indices;
    }

    /**
     * Creates a MAC structure parser for the message with the specified structure start offset.
     * @param message containing a MAC structure
     * @param offset to the start of the structure
     * @return MAC structure parser
     */
    public static MacStructure createMacStructure(CorrectedBinaryMessage message, int offset)
    {
        MacOpcode opcode = MacStructure.getOpcode(message, offset);

        switch(opcode)
        {
            case TDMA_0_NULL_INFORMATION_MESSAGE:
                return new NullInformationMessage(message, offset);
            case TDMA_1_GROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                return new GroupVoiceChannelUserAbbreviated(message, offset);
            case PHASE1_122_RFSS_STATUS_BROADCAST_ABBREVIATED:
                return new RfssStatusBroadcastAbbreviated(message, offset);
            case PHASE1_123_NETWORK_STATUS_BROADCAST_ABBREVIATED:
                return new NetworkStatusBroadcastAbbreviated(message, offset);
            case PHASE1_66_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                return new GroupVoiceChannelGrantUpdate(message, offset);
        }

        return new UnknownStructure(message, offset);
    }
}
