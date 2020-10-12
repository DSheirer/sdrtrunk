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

package io.github.dsheirer.module.decode.dmr.message.data.lc.shorty;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Hamming17;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.type.LCSS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reassembles fragments into Short Link Control Messages
 */
public class SLCAssembler
{
    private final static Logger mLog = LoggerFactory.getLogger(SLCAssembler.class);
    private CorrectedBinaryMessage mAssemblingMessage;
    private int mFragmentCount = 0;
    private long mTimestamp;

    /**
     * Constructs an instance
     */
    public SLCAssembler()
    {
    }

    /**
     * Processes the short link control fragment
     * @return a short link control message or null if the message is not yet fully assembled
     */
    public ShortLCMessage process(LCSS lcss, BinaryMessage fragment, long timestamp)
    {
        mTimestamp = timestamp;

        ShortLCMessage message = null;

        switch(lcss)
        {
            case FIRST_FRAGMENT:
                message = dispatch();
                mAssemblingMessage = new CorrectedBinaryMessage(68);
                add(fragment);
                break;
            case CONTINUATION_FRAGMENT:
                if(mAssemblingMessage == null)
                {
                    mAssemblingMessage = new CorrectedBinaryMessage(68);
                    mAssemblingMessage.setPointer(17);
                }
                add(fragment);
                break;
            case LAST_FRAGMENT:
                if(mAssemblingMessage == null)
                {
                    mAssemblingMessage = new CorrectedBinaryMessage(68);
                    mAssemblingMessage.setPointer(51);
                }
                add(fragment);
                message = dispatch();
                break;
            case SINGLE_FRAGMENT:
            default:
                message = createSingleFragment(fragment);
                break;
        }

        return message;
    }

    /**
     * Creates a single fragment short link control message
     * @param binaryMessage from cach payload
     * @return short link control message
     */
    private ShortLCMessage createSingleFragment(BinaryMessage binaryMessage)
    {
        CorrectedBinaryMessage corrected = new CorrectedBinaryMessage(17);
        for(int x = 0; x < 17; x++)
        {
            if(binaryMessage.get(x))
            {
                corrected.set(x);
            }
        }

        return LCMessageFactory.createShort(corrected, mTimestamp, 0);
    }

    private ShortLCMessage dispatch()
    {
        ShortLCMessage message = null;

        if(mFragmentCount == 4)
        {
            message = decode(mAssemblingMessage, mTimestamp);
        }

        mAssemblingMessage = null;
        mFragmentCount = 0;
        return message;
    }

    private static ShortLCMessage decode(BinaryMessage interleaved, long timestamp)
    {
        if(interleaved != null)
        {
            boolean valid = true;

            CorrectedBinaryMessage deinterleaved = new CorrectedBinaryMessage(68);
            int i, src;

            for(int index = 0; index < 67; index++)
            {
                if(interleaved.get(index))
                {
                    deinterleaved.set((index * 17 % 67));
                }
            }

            if(interleaved.get(67))
            {
                deinterleaved.set(67);
            }

            int errorCount = 0;

            for(int row = 0; row < 4; row++)
            {
                int rowErrorCount = Hamming17.checkAndCorrect(deinterleaved, row * 17);
                errorCount += rowErrorCount;

                if(rowErrorCount > 1)
                {
                    valid = false;
                }
            }

            //If valid, check the column parity values
            if(valid)
            {
                //Check the column parity values
                for(int column = 0; column < 17; column++)
                {
                    boolean parity = deinterleaved.get(column);

                    for(int y = 1; y < 4; y++)
                    {
                        parity ^= deinterleaved.get(column + (y * 17));
                    }

                    if(parity)
                    {
                        valid = false;
                    }
                }
            }

            //Extract the message
            CorrectedBinaryMessage extractedMessage = new CorrectedBinaryMessage(51);

            for(int row = 0; row < 3; row++)
            {
                for(int column = 0; column < 12; column++)
                {
                    extractedMessage.set(row * 12 + column, deinterleaved.get(row * 17 + column));
                }
            }

            extractedMessage.setCorrectedBitCount(errorCount);
            extractedMessage.setSize(36);
            ShortLCMessage slco = LCMessageFactory.createShort(extractedMessage, timestamp, 0);


            //Note: slco is timeslot-agnostic, so we use timeslot 0 every time
//            ShortLCMessage slco = LCMessageFactory.createShort(decoded, timestamp, 0);
            slco.setValid(valid);
            return slco;
        }

        return null;
    }

    /**
     * Adds the cach payload to the currently assembling message
     */
    private void add(BinaryMessage payload)
    {
        if(payload == null || payload.size() != 17)
        {
            return;
        }

        mFragmentCount++;

        try
        {
            if(mAssemblingMessage == null)
            {
                mAssemblingMessage = new CorrectedBinaryMessage(68);
            }

            for(int x = 0; x < 17; x++)
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

    public static void main(String[] args)
    {
        String raw = "A0000C0000A0000C0000";
        System.out.println(" RAW:" + raw);
        String bits = "10100000000000000110000000000000001010000000000000011000000000000000";

        BinaryMessage rawBin= BinaryMessage.load(bits);
        System.out.println("ORIG:" + rawBin.toHexString());

        ShortLCMessage slc = decode(rawBin, System.currentTimeMillis());

        System.out.println(slc.toString());
    }
}
