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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRCrcMaskManager;
import io.github.dsheirer.module.decode.dmr.bptc.BPTC_128_77;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.type.LCSS;

/**
 * Reassembles embedded signalling fragments into Full Link Control Messages
 */
public class FLCAssembler
{
    private CorrectedBinaryMessage mAssemblingMessage;
    private int mFragmentCount = 0;
    private long mTimestamp;
    private final int mTimeslot;
    private final BPTC_128_77 mBPTC = new BPTC_128_77();
    private final LCMessageFactory mMessageFactory;

    /**
     * Constructs an instance
     */
    public FLCAssembler(int timeslot, DMRCrcMaskManager maskManager)
    {
        mTimeslot = timeslot;
        mMessageFactory = new LCMessageFactory(maskManager);
    }

    /**
     * Resets this assembler when a sync loss is detected.
     */
    public void reset()
    {
        mAssemblingMessage = null;
        mFragmentCount = 0;
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
     * Dispatches a fully-received message
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
    private FullLCMessage decode(BinaryMessage message, long timestamp, int timeslot)
    {
        if(message != null)
        {
            CorrectedBinaryMessage extractedMessage = mBPTC.extract(message);
            FullLCMessage flco = mMessageFactory.createFull(extractedMessage, timestamp, timeslot, false);

            if(extractedMessage.getCorrectedBitCount() < 0)
            {
                flco.setValid(false);
            }

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
