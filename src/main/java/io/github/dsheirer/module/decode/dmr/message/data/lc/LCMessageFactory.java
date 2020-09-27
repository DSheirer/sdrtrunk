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
import io.github.dsheirer.edac.ReedSolomon_12_9_4_DMR;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.FullLCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.GPSInformation;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.GroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.TerminatorData;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.UnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.UnknownFullLCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.hytera.HyteraGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.hytera.HyteraTerminator;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.hytera.HyteraUnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola.CapacityPlusGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola.CapacityPlusWideAreaVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ActivityUpdateMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.CapacityPlusRestChannel;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ConnectPlusControlChannel;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ConnectPlusTrafficChannel;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ControlChannelSystemParameters;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.HyteraXPTChannel;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.NullMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ShortLCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.UnknownShortLCMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Link control factory class for creating both short and full link control messages
 */
public class LCMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(LCMessageFactory.class);
    private static final ReedSolomon_12_9_4_DMR REED_SOLOMON_12_9_4_DMR = new ReedSolomon_12_9_4_DMR();
    private static int TERMINATOR_LINK_CONTROL_CRC_MASK = 0x99;
    private static int VOICE_LINK_CONTROL_CRC_MASK = 0x96;

    /**
     * Creates a full link control message
     * @param message bits
     * @param timestamp of the original message
     * @param timeslot of the original message
     * @param isTerminator set to true if this is LC for a terminator data burst.
     * @return message class
     */
    public static FullLCMessage createFull(CorrectedBinaryMessage message, long timestamp, int timeslot, boolean isTerminator)
    {
        if(message == null)
        {
            throw new IllegalArgumentException("Message cannot be null");
        }

        //RS(12,9,4) can correct up to floor(4/2) = 1 bit error.
        boolean corrected = REED_SOLOMON_12_9_4_DMR.correctFullLinkControl(message,
            isTerminator ? TERMINATOR_LINK_CONTROL_CRC_MASK : VOICE_LINK_CONTROL_CRC_MASK);

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
            case FULL_STANDARD_TERMINATOR_DATA:
                flc = new TerminatorData(message, timestamp, timeslot);
                break;
            case FULL_CAPACITY_PLUS_GROUP_VOICE_CHANNEL_USER:
                flc = new CapacityPlusGroupVoiceChannelUser(message, timestamp, timeslot);
                break;
            case FULL_CAPACITY_PLUS_WIDE_AREA_VOICE_CHANNEL_USER:
                flc = new CapacityPlusWideAreaVoiceChannelUser(message, timestamp, timeslot);
                break;

            case FULL_HYTERA_GROUP_VOICE_CHANNEL_USER:
                flc = new HyteraGroupVoiceChannelUser(message, timestamp, timeslot);
                break;
            case FULL_HYTERA_UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                flc = new HyteraUnitToUnitVoiceChannelUser(message, timestamp, timeslot);
                break;
            case FULL_HYTERA_TERMINATOR:
                flc = new HyteraTerminator(message, timestamp, timeslot);
                break;
            case FULL_STANDARD_TALKER_ALIAS_HEADER:
            case FULL_STANDARD_TALKER_ALIAS_BLOCK_1:
            case FULL_STANDARD_TALKER_ALIAS_BLOCK_2:
            case FULL_STANDARD_TALKER_ALIAS_BLOCK_3:
            default:
                flc = new UnknownFullLCMessage(message, timestamp, timeslot);
                break;
        }

        flc.setValid(corrected);
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
            case SHORT_CAPACITY_PLUS_REST_CHANNEL_NOTIFICATION:
                slc = new CapacityPlusRestChannel(message, timestamp, timeslot);
                break;
            case SHORT_CONNECT_PLUS_CONTROL_CHANNEL:
                slc = new ConnectPlusControlChannel(message, timestamp, timeslot);
                break;
            case SHORT_CONNECT_PLUS_TRAFFIC_CHANNEL:
                slc = new ConnectPlusTrafficChannel(message, timestamp, timeslot);
                break;
            case SHORT_STANDARD_CONTROL_CHANNEL_SYSTEM_PARAMETERS:
                slc = new ControlChannelSystemParameters(message, timestamp, timeslot);
                break;
            case SHORT_HYTERA_XPT_CHANNEL:
            case SHORT_STANDARD_XPT_CHANNEL:
                slc = new HyteraXPTChannel(message, timestamp, timeslot);
                break;
            case SHORT_STANDARD_TRAFFIC_CHANNEL_SYSTEM_PARAMETERS:
            case SHORT_STANDARD_UNKNOWN:
            default:
                slc = new UnknownShortLCMessage(message, timestamp, timeslot);
                break;
        }

        boolean valid = CRCDMR.crc8(message, 36) == 0;

//        System.out.println("SLC MSG:" + message.toHexString() + " CRC-8 RESIDUAL: " + CRCDMR.crc8(message, 36));
//
        slc.setValid(valid);

        return slc;
    }
}
