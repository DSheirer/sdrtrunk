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

package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.data.header.HeaderMessage;
import io.github.dsheirer.module.decode.dmr.message.data.sequence.UnknownDataBlockMessage;
import io.github.dsheirer.module.decode.dmr.message.data.terminator.TerminatorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.dsheirer.edac.BPTC_196_96.bptc_196_96_check_and_repair;
import static io.github.dsheirer.edac.BPTC_196_96.bptc_196_96_extractdata;
import static io.github.dsheirer.edac.BPTC_196_96.bptc_deinterleave;

/**
 * Factory for creating data messages that contain a 196-bit BPTC protected message.
 */
public class DataMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(DataMessageFactory.class);

    /**
     * Creates a data message class
     * @param pattern for the DMR burst
     * @param message DMR burst as transmitted
     * @param cach from the DMR burst
     * @param timestamp for the message
     * @param timeslot for the message
     * @return data message instance
     */
    public static DataMessage create(DMRSyncPattern pattern, BinaryMessage message, CACH cach, long timestamp, int timeslot)
    {
        CorrectedBinaryMessage payload = getPayload(message);
        SlotType slotType = SlotType.getSlotType(message);

        if(slotType.isValid())
        {
            switch(slotType.getDataType())
            {
                case SLOT_IDLE:
                    return new IDLEMessage(pattern, payload, cach, slotType, timestamp, timeslot);
                case CSBK:
                    return CSBKMessageFactory.create(pattern, payload, cach, slotType, timestamp, timeslot);
                case CHANNEL_CONTROL_ENC_HEADER:
                case CSBK_ENC_HEADER:
                case DATA_ENC_HEADER:
                case DATA_HEADER:
                case MBC_ENC_HEADER:
                case MBC_HEADER:
                case PI_HEADER:
                case VOICE_HEADER:
                    return new HeaderMessage(pattern, payload, cach, slotType, timestamp, timeslot);
                case TLC:
                    return new TerminatorMessage(pattern, payload, cach, slotType, timestamp, timeslot);
                case MBC:
                case RATE_1_OF_2_DATA:
                case RATE_3_OF_4_DATA:
                case RATE_1_DATA:
                    return new UnknownDataBlockMessage(pattern, payload, cach, slotType, timestamp, timeslot);
                case RESERVED_15:
                case UNKNOWN:
                    return new UnknownDataMessage(pattern, payload, cach, slotType, timestamp, timeslot);
            }
        }

        return new UnknownDataMessage(pattern, payload, cach, slotType, timestamp, timeslot);
    }

    /**
     * De-scramble, decode and error check a BPTC protected raw message and return a 96-bit error corrected
     * payload or null.
     * @param _message
     * @return
     */
    private static CorrectedBinaryMessage getPayload(BinaryMessage _message)
    {
        CorrectedBinaryMessage bm1 = new CorrectedBinaryMessage(196);

        try
        {
            for(int i = 24; i < 122; i++)
            {
                bm1.add(_message.get(i));
            }
            for(int i = 190; i < 190 + 98; i++)
            {
                bm1.add(_message.get(i));
            }
        }
        catch(BitSetFullException ex)
        {
            mLog.error("Error decoding DMR BPTC 196 structure");
        }

        CorrectedBinaryMessage message = bptc_deinterleave(bm1);

        //TODO: detect the quantity of bits that were repaired and insert that value into the corrected binary message result
        if(bptc_196_96_check_and_repair(message))
        {
            message = bptc_196_96_extractdata(message);
        }
        else
        {
            //TODO: the above should always return the payload, even if it fails error correction which should cause
            //TODO: the wrapping message class to setValid(false).  That way you're never having to deal with null values.
            message = null;
        }

        return message;
    }
}
