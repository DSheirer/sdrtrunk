/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.EmptyTimeslotPlaceholderMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.DMRCrcMaskManager;
import io.github.dsheirer.module.decode.dmr.message.data.DMRDataMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceAMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceEMBMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import io.github.dsheirer.protocol.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DMR Message Factory class for creating messages from DMR bursts.
 */
public class DMRMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRMessageFactory.class);
    private DMRDataMessageFactory mDataMessageFactory;

    /**
     * Constructs an instance
     * @param crcMaskManager for error detection and correction.
     */
    public DMRMessageFactory(DMRCrcMaskManager crcMaskManager)
    {
        mDataMessageFactory = new DMRDataMessageFactory(crcMaskManager);
    }

    /**
     * Creates a DMR message wrapper for the binary message.  DMR message type is specified by the sync pattern.
     * @param syncPattern for the message
     * @param binaryMessage containing the raw DMR message burst
     * @param timestamp for the burst
     * @param timeslot for the burst
     * @return DMRMessage instance or null for UNKNOWN sync pattern
     */
    public IMessage create(DMRSyncPattern syncPattern, CorrectedBinaryMessage binaryMessage, CACH cach, long timestamp,
                           int timeslot)
    {
        switch(syncPattern)
        {
            case BASE_STATION_VOICE, MOBILE_STATION_VOICE:
            case BS_VOICE_FRAME_B, BS_VOICE_FRAME_C, BS_VOICE_FRAME_D, BS_VOICE_FRAME_E, BS_VOICE_FRAME_F:
            case MS_VOICE_FRAME_B, MS_VOICE_FRAME_C, MS_VOICE_FRAME_D, MS_VOICE_FRAME_E, MS_VOICE_FRAME_F:
            case DIRECT_VOICE_FRAME_B, DIRECT_VOICE_FRAME_C, DIRECT_VOICE_FRAME_D, DIRECT_VOICE_FRAME_E,
                 DIRECT_VOICE_FRAME_F:
                return createVoiceMessage(syncPattern, binaryMessage, cach, timestamp, timeslot);
            case DIRECT_VOICE_TIMESLOT_1:
                return createVoiceMessage(syncPattern, binaryMessage, cach, timestamp, 1);
            case DIRECT_VOICE_TIMESLOT_2:
                return createVoiceMessage(syncPattern, binaryMessage, cach, timestamp, 2);
            case BASE_STATION_DATA:
            case MOBILE_STATION_DATA:
                return mDataMessageFactory.create(syncPattern, binaryMessage, cach, timestamp, timeslot);
            case DIRECT_DATA_TIMESLOT_1:
                return mDataMessageFactory.create(syncPattern, binaryMessage, cach, timestamp, 1);
            case DIRECT_DATA_TIMESLOT_2:
                return mDataMessageFactory.create(syncPattern, binaryMessage, cach, timestamp, 2);
            case DIRECT_EMPTY_TIMESLOT:
                return new EmptyTimeslotPlaceholderMessage(timestamp, 288, Protocol.DMR, timeslot);
            case UNKNOWN:
                return null;
            case REVERSE_CHANNEL:
            case RESERVED:
            default:
                return new UnknownDMRMessage(syncPattern, binaryMessage, cach, timestamp, timeslot);
        }
    }

    /**
     * Creates a voice message from the raw transmitted DMR burst frame
     * @param syncPattern for the frame
     * @param message containing the DMR burst
     * @param cach extracted from the first 24 bits of the message
     * @param timestamp the message was received
     * @param timeslot for the DMR burst
     * @return constructed voice message
     */
    private static VoiceMessage createVoiceMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach,
                                                  long timestamp, int timeslot)
    {
        switch(syncPattern)
        {
            case BASE_STATION_VOICE:
            case MOBILE_STATION_VOICE:
            case DIRECT_VOICE_TIMESLOT_1:
            case DIRECT_VOICE_TIMESLOT_2:
                return new VoiceAMessage(syncPattern, message, cach, timestamp, timeslot);
            case BS_VOICE_FRAME_B, BS_VOICE_FRAME_C, BS_VOICE_FRAME_D, BS_VOICE_FRAME_E, BS_VOICE_FRAME_F:
            case MS_VOICE_FRAME_B, MS_VOICE_FRAME_C, MS_VOICE_FRAME_D, MS_VOICE_FRAME_E, MS_VOICE_FRAME_F:
            case DIRECT_VOICE_FRAME_B, DIRECT_VOICE_FRAME_C, DIRECT_VOICE_FRAME_D, DIRECT_VOICE_FRAME_E,
                 DIRECT_VOICE_FRAME_F:
            default:
                return new VoiceEMBMessage(syncPattern, message, cach, timestamp, timeslot);
        }
    }
}

