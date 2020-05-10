/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusNeighborReport;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusUnknownOpcode28;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusVoiceChannelUser;

/**
 * Factory for creating DMR CSBK messages
 */
public class CSBKMessageFactory
{
    public static CSBKMessage create(DMRSyncPattern pattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                                     long timestamp, int timeslot)
    {
        if(message != null)
        {
            int corrected = CRCDMR.correctCCITT80(message, 0, 80, 0xA5A5);
            message.setCorrectedBitCount(corrected);

            Opcode opcode = CSBKMessage.getOpcode(message);

            switch(opcode)
            {
                case MOTOROLA_CONPLUS_NEIGHBOR_REPORT:
                    return new ConnectPlusNeighborReport(pattern, message, cach, slotType, timestamp, timeslot);
                case MOTOROLA_CONPLUS_VOICE_CHANNEL_USER:
                    return new ConnectPlusVoiceChannelUser(pattern, message, cach, slotType, timestamp, timeslot);
                case MOTOROLA_CONPLUS_UNKNOWN_28:
                    return new ConnectPlusUnknownOpcode28(pattern, message, cach, slotType, timestamp, timeslot);
                default:
                    return new UnknownCSBKMessage(pattern, message, cach, slotType, timestamp, timeslot);
            }
        }

        return null;
    }
}
