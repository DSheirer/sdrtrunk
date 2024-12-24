/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.module.decode.dmr.bptc.BPTC_68_36;
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
    private BPTC_68_36 mBPTC = new BPTC_68_36();

    /**
     * Constructs an instance
     */
    public SLCAssembler()
    {
    }

    /**
     * Resets this assembler when a sync loss is detected to prevent malformed SLC messages.
     */
    public void reset()
    {
        mAssemblingMessage = null;
        mFragmentCount = 0;
        mTimestamp = System.currentTimeMillis();
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

    /**
     * Performs error detection and correction and extracts the message from the BPTC protected interleaved message.
     * @param interleaved message
     * @param timestamp for the decoded message.
     * @return decoded message.
     */
    private ShortLCMessage decode(BinaryMessage interleaved, long timestamp)
    {
        if(interleaved != null)
        {
            CorrectedBinaryMessage corrected = mBPTC.extract(interleaved);
            //Note: slco is timeslot-agnostic, so we use timeslot 0 every time
            ShortLCMessage slco = LCMessageFactory.createShort(corrected, timestamp, 0);
            slco.setValid(corrected.getCorrectedBitCount() >= 0);
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
}
