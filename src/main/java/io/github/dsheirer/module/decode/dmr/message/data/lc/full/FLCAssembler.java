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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Hamming16;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.type.LCSS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reassembles embedded signalling fragments into Full Link Control Messages
 */
public class FLCAssembler
{
    private final static Logger mLog = LoggerFactory.getLogger(FLCAssembler.class);
    private CorrectedBinaryMessage mAssemblingMessage;
    private int mFragmentCount = 0;
    private long mTimestamp;
    private int mTimeslot;

    /**
     * Constructs an instance
     */
    public FLCAssembler(int timeslot)
    {
        mTimeslot = timeslot;
    }

    /**
     * Processes the full link control fragment
     * @return a full link control message or null if the message is not yet fully assembled
     */
    public FullLCMessage process(LCSS lcss, BinaryMessage fragment, long timestamp)
    {
        mTimestamp = timestamp;

        FullLCMessage message = null;

        switch(lcss)
        {
            case FIRST_FRAGMENT:
                message = dispatch();
                mAssemblingMessage = new CorrectedBinaryMessage(128);
                add(fragment);
                break;
            case CONTINUATION_FRAGMENT:
                if(mAssemblingMessage == null)
                {
                    mAssemblingMessage = new CorrectedBinaryMessage(128);
                    mAssemblingMessage.setPointer(32);
                }
                add(fragment);
                break;
            case LAST_FRAGMENT:
                if(mAssemblingMessage == null)
                {
                    mAssemblingMessage = new CorrectedBinaryMessage(128);
                    mAssemblingMessage.setPointer(96);
                }
                add(fragment);
                message = dispatch();
                break;
            case SINGLE_FRAGMENT:
                //TODO: this is the reverse channel signalling embedded in voice frame F
                dispatch();
            default:
                //Ignore
                break;
        }

        return message;
    }

    /**
     * Dispatches a fully-received messsage
     */
    private FullLCMessage dispatch()
    {
        FullLCMessage message = null;

        if(mFragmentCount == 4)
        {
            message = decode(mAssemblingMessage, mTimestamp, mTimeslot);
        }

        mAssemblingMessage = null;
        mFragmentCount = 0;
        return message;
    }

    /**
     * Decodes an assembled FLC message, performs error detection and correction and returns a constructed FLC message
     */
    private static FullLCMessage decode(BinaryMessage message, long timestamp, int timeslot)
    {
        if(message != null)
        {
            boolean valid = true;

            CorrectedBinaryMessage descrambled = new CorrectedBinaryMessage(128);
            int src;
            descrambled.set(127, message.get(127));
            for(int i = 0; i < 127; i++)
            {
                src = (i * 8) % 127;
                boolean x = message.get(src);
                descrambled.set(i, message.get(src));
            }

            int errorCount = 0;

            for(int row = 0; row < 8; row++)
            {
                int rowErrorCount = Hamming16.checkAndCorrect(descrambled, row * 16);
                errorCount += rowErrorCount;

                if(errorCount > 1)
                {
                    valid = false;
                }
            }

            //If valid, check the column parity values
            if(valid)
            {
                //Check the column parity values
                for(int column = 0; column < 16; column++)
                {
                    boolean parity = descrambled.get(column);

                    for(int y = 1; y < 8; y++)
                    {
                        parity ^= descrambled.get(column + (y * 16));
                    }

                    if(parity)
                    {
                        valid = false;
                    }
                }
            }

            //Extract the message.  Row 1 and 2 are 11 bits.  Rows 3-7 are 10 bits.
            CorrectedBinaryMessage extractedMessage = new CorrectedBinaryMessage(77);
            int pointer = 0;

            for(int row = 0; row < 2; row++)
            {
                for(int column = 0; column < 11; column++)
                {
                    extractedMessage.set(pointer++, descrambled.get(row * 16 + column));
                }
            }

            for(int row = 2; row < 7; row++)
            {
                for(int column = 0; column < 10; column++)
                {
                    extractedMessage.set(pointer++, descrambled.get(row * 16 + column));
                }
            }

            extractedMessage.setCorrectedBitCount(errorCount);
            FullLCMessage flco = LCMessageFactory.createFull(extractedMessage, timestamp, timeslot, false);
            flco.setValid(valid);
            return flco;
        }

        return null;
    }

    /**
     * Adds the cach payload to the currently assembling message
     */
    private void add(BinaryMessage payload)
    {
        if(payload == null || payload.size() != 32)
        {
            return;
        }

        mFragmentCount++;

        try
        {
            if(mAssemblingMessage == null)
            {
                mAssemblingMessage = new CorrectedBinaryMessage(96);
            }

            for(int x = 0; x < 32; x++)
            {
                mAssemblingMessage.add(payload.get(x));
            }
        }
        catch(BitSetFullException bsfe)
        {
            //Something went wrong and we overfilled the bitset
            dispatch();
        }
    }
}
