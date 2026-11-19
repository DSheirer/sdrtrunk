/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.edacs;

import java.util.function.Consumer;

/**
 * Finds EDACS outbound control channel frames in a 9600 baud bit stream and produces messages.
 *
 * Frame (288 bits, every 30 ms): 48-bit sync 0x555557125555, then two messages of 120 bits.  Each message is sent
 * three times as 40-bit BCH codewords, the second copy inverted.  The three copies are majority voted and the result
 * must pass the BCH check.  MBTA transmits the frame inverted, so both polarities are searched.
 */
public class EDACSMessageFramer
{
    public static final long SYNC = 0x555557125555L;
    private static final long MASK_48 = 0xFFFFFFFFFFFFL;
    private static final int MAX_SYNC_ERRORS = 3;
    private static final int PAYLOAD_BITS = 240;

    private final Consumer<EDACSMessage> mListener;
    private long mShift;
    private boolean mCollecting;
    private boolean mInverted;
    private final boolean[] mPayload = new boolean[PAYLOAD_BITS];
    private int mPayloadIndex;
    private long mBitCount;
    private int mSystemKeyMask;

    //Statistics
    private long mSyncCount, mValidCount, mInvalidCount;

    public EDACSMessageFramer(Consumer<EDACSMessage> listener)
    {
        mListener = listener;
    }

    /**
     * Sets the EDACS System Key (ESK).  Systems with an ESK XOR the top 8 bits of each 28-bit message with it; the key
     * is removed from each message after the BCH check, which covers the transmitted (keyed) bits.
     * @param systemKey 0 for none, or the key (0xA0 is the value in use)
     */
    public void setSystemKey(int systemKey)
    {
        mSystemKeyMask = (systemKey & 0xFF) << 20;
    }

    /**
     * Processes one demodulated bit.
     * @param bit value
     * @param timestamp of the bit
     */
    public void process(boolean bit, long timestamp)
    {
        mBitCount++;

        if(mCollecting)
        {
            mPayload[mPayloadIndex++] = bit ^ mInverted;

            if(mPayloadIndex == PAYLOAD_BITS)
            {
                mCollecting = false;
                decodeFrame(timestamp);
                mShift = 0;
            }
            return;
        }

        mShift = ((mShift << 1) | (bit ? 1 : 0)) & MASK_48;

        if(mBitCount < 48)
        {
            return;
        }

        int errors = Long.bitCount(mShift ^ SYNC);

        if(errors <= MAX_SYNC_ERRORS)
        {
            startPayload(false);
        }
        else if(48 - errors <= MAX_SYNC_ERRORS)
        {
            startPayload(true);
        }
    }

    private void startPayload(boolean inverted)
    {
        mSyncCount++;
        mInverted = inverted;
        mCollecting = true;
        mPayloadIndex = 0;
    }

    /**
     * Majority votes the three copies of each message.  A frame is passed on when its first message passes the BCH
     * check; the second message is included when it passes too (-1 otherwise).
     */
    private void decodeFrame(long timestamp)
    {
        boolean[] corrected = new boolean[1];
        long first = vote(0, corrected);
        long second = vote(120, corrected);

        if(EDACSBCH.isValid(first))
        {
            mValidCount++;
            int message2 = -1;

            if(EDACSBCH.isValid(second))
            {
                mValidCount++;
                message2 = (int)(second >> EDACSBCH.CHECK_BITS) ^ mSystemKeyMask;
            }
            else
            {
                mInvalidCount++;
            }

            mListener.accept(new EDACSMessage((int)(first >> EDACSBCH.CHECK_BITS) ^ mSystemKeyMask, message2, timestamp,
                corrected[0]));
        }
        else
        {
            mInvalidCount++;

            if(EDACSBCH.isValid(second))
            {
                mValidCount++; //counted, but the frame is not usable without its first message
            }
            else
            {
                mInvalidCount++;
            }
        }
    }

    private long vote(int offset, boolean[] corrected)
    {
        long codeword = 0;

        for(int x = 0; x < 40; x++)
        {
            boolean a = mPayload[offset + x];
            boolean b = !mPayload[offset + 40 + x];
            boolean c = mPayload[offset + 80 + x];
            boolean vote = (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) >= 2;

            if(a != b || b != c)
            {
                corrected[0] = true;
            }

            codeword = (codeword << 1) | (vote ? 1 : 0);
        }

        return codeword;
    }

    public long getSyncCount()
    {
        return mSyncCount;
    }

    public long getValidCount()
    {
        return mValidCount;
    }

    public long getInvalidCount()
    {
        return mInvalidCount;
    }

    /**
     * Polarity of the most recent frame.
     */
    public boolean isInverted()
    {
        return mInverted;
    }
}
