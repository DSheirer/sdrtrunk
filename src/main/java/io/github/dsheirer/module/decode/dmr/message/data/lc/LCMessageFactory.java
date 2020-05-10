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

package io.github.dsheirer.module.decode.dmr.message.data.lc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.edac.ReedSolomon_12_9;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.FullLCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.GPSInformation;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.GroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.UnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.UnknownFullLCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ActivityUpdateMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ConnectPlusControlChannel;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ConnectPlusTrafficChannel;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.NullMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ShortLCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.UnknownShortLCMessage;

/**
 * Link control factory class for creating both short and full link control messages
 */
public class LCMessageFactory
{
    /**
     * Creates a full link control message
     * @param message bits
     * @return message class
     */
    public static FullLCMessage createFull(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        int errorCount = ReedSolomon_12_9.checkReedSolomon(message, 0, 72, 0x96);
        message.setCorrectedBitCount(message.getCorrectedBitCount() + errorCount);
        LCOpcode opcode = FullLCMessage.getOpcode(message);

        FullLCMessage flc = null;

        switch(opcode)
        {
            case FULL_STANDARD_GROUP_VOICE_CHANNEL_USER:
                flc = new GroupVoiceChannelUser(message, timestamp, timeslot);
                break;
            case FULL_STANDARD_UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                flc = new UnitToUnitVoiceChannelUser(message, timestamp, timeslot);
                break;
            case FULL_STANDARD_GPS_INFO:
                flc = new GPSInformation(message, timestamp, timeslot);
                break;
            default:
                flc = new UnknownFullLCMessage(message, timestamp, timeslot);
                break;
        }

        flc.setValid(errorCount < 4);
        return flc;
    }

    /**
     * Creates a short link control message
     * @param message bits
     * @return message class
     */
    public static ShortLCMessage createShort(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        LCOpcode opcode = ShortLCMessage.getOpcode(message);

        ShortLCMessage slc = null;

        switch(opcode)
        {
            case SHORT_STANDARD_NULL_MESSAGE:
                slc = new NullMessage(message, timestamp, timeslot);
                break;
            case SHORT_STANDARD_ACTIVITY_UPDATE:;
                slc = new ActivityUpdateMessage(message, timestamp, timeslot);
                break;
            case SHORT_CONNECT_PLUS_CONTROL_CHANNEL:
                slc = new ConnectPlusControlChannel(message, timestamp, timeslot);
                break;
            case SHORT_CONNECT_PLUS_TRAFFIC_CHANNEL:
                slc = new ConnectPlusTrafficChannel(message, timestamp, timeslot);
                break;
            case SHORT_STANDARD_CONTROL_CHANNEL_SYSTEM_PARAMETERS:
            case SHORT_STANDARD_TRAFFIC_CHANNEL_SYSTEM_PARAMETERS:
            case SHORT_STANDARD_UNKNOWN:
            default:
                slc = new UnknownShortLCMessage(message, timestamp, timeslot);
                break;
        }

        boolean valid = CRCDMR.crc8(message, 36) == 0;

        if(!valid)
        {
            int a = 0;
        }
        slc.setValid(valid);

        return slc;
    }
}
