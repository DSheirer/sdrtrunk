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

package io.github.dsheirer.module.decode.pager.pocsag;

import io.github.dsheirer.module.decode.pager.BCH3121;
import io.github.dsheirer.module.decode.pager.FskSymbolDemodulator;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.pager.PagerIdleMessage;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayList;
import java.util.List;

/**
 * POCSAG (CCIR Radiopaging Code No. 1) bit stream decoder for one bit rate (512, 1200 or 2400 baud).
 *
 * A transmission is a preamble of alternating bits, followed by one or more batches.  Each batch is a 32-bit sync
 * codeword followed by 8 frames of 2 codewords each.  A codeword is 21 data bits, 10 BCH(31,21) check bits and an even
 * parity bit, transmitted MSB first.  An address codeword (bit 31 = 0) starts a page to the capcode formed from its 18
 * address bits and the frame number.  Following message codewords (bit 31 = 1) carry the page content until the next
 * address codeword or an idle codeword.  A page can span batches.
 *
 * Both signal polarities are supported: the polarity is detected from the sync codeword.  Since half of all random
 * 31-bit words are within two bit errors of a BCH codeword, a batch is only processed when most of its codewords are
 * valid, which rejects batches that follow a false sync detection in noise.
 */
public class PocsagDecoder implements FskSymbolDemodulator.ISymbolListener
{
    public static final int SYNC_CODEWORD = 0x7CD215D8;
    public static final int IDLE_CODEWORD = 0x7A89C197;
    /**
     * Returned by {@link #correct(int)} for a codeword that can't be corrected.  This is outside of the 32-bit range
     * since every 32-bit value, including 0xFFFFFFFF, can be a valid codeword.
     */
    public static final long UNCORRECTABLE = -1L;
    private static final int CODEWORDS_PER_BATCH = 16;
    private static final int SYNC_DETECT_MAX_BIT_ERRORS = 2;
    private static final int SYNC_TRACK_MAX_BIT_ERRORS = 6;
    private static final int BATCH_MIN_VALID_CODEWORDS = 12;
    private static final int MAX_MESSAGE_CODEWORDS = 256;

    private final int mBitRate;
    private final boolean mAfsk;
    private final Listener<IMessage> mMessageListener;
    private long mSyncTimestamp;
    private boolean mSynchronized;
    private boolean mInverted;
    private int mShiftRegister;
    private int mCodeword;
    private int mCodewordBitCount;
    private int mCodewordIndex;
    private final int[] mBatch = new int[CODEWORDS_PER_BATCH];

    //Page in progress
    private int mCapcode = -1;
    private int mFunction;
    private long mPageTimestamp;
    private final List<Integer> mMessageWords = new ArrayList<>();

    /**
     * Constructs an instance
     * @param bitRate of the decoder
     * @param afsk true if the symbols are from AFSK audio tones instead of direct FSK
     * @param messageListener to receive decoded pages and sync (idle) messages
     */
    public PocsagDecoder(int bitRate, boolean afsk, Listener<IMessage> messageListener)
    {
        mBitRate = bitRate;
        mAfsk = afsk;
        mMessageListener = messageListener;
    }

    /**
     * Bit rate for this decoder
     */
    public int getBitRate()
    {
        return mBitRate;
    }

    /**
     * Indicates if the decoder is currently synchronized to a POCSAG batch stream.
     */
    public boolean isSynchronized()
    {
        return mSynchronized;
    }

    /**
     * Resets the decoder, sends any page in progress and returns to sync search.
     */
    public void reset()
    {
        loseSync();
    }

    @Override
    public void receive(float symbol)
    {
        //POCSAG specifies a binary 1 as the lower frequency.  Polarity is resolved from the sync codeword.
        int bit = symbol < 0.0f ? 1 : 0;

        if(!mSynchronized)
        {
            mShiftRegister = (mShiftRegister << 1) | bit;

            if(Integer.bitCount(mShiftRegister ^ SYNC_CODEWORD) <= SYNC_DETECT_MAX_BIT_ERRORS)
            {
                startBatches(false);
            }
            else if(Integer.bitCount(~mShiftRegister ^ SYNC_CODEWORD) <= SYNC_DETECT_MAX_BIT_ERRORS)
            {
                startBatches(true);
            }
        }
        else
        {
            if(mInverted)
            {
                bit ^= 1;
            }

            mCodeword = (mCodeword << 1) | bit;

            if(++mCodewordBitCount == 32)
            {
                processCodeword(mCodeword);
                mCodeword = 0;
                mCodewordBitCount = 0;
            }
        }
    }

    /**
     * Starts collecting batch codewords after a sync codeword is detected.
     */
    private void startBatches(boolean inverted)
    {
        mSynchronized = true;
        mInverted = inverted;
        mCodeword = 0;
        mCodewordBitCount = 0;
        mCodewordIndex = 0;
        mSyncTimestamp = System.currentTimeMillis();
    }

    /**
     * Ends synchronization, sends any page in progress and returns to sync search.
     */
    private void loseSync()
    {
        sendPage();
        mSynchronized = false;
        mShiftRegister = 0;
        mCodeword = 0;
        mCodewordBitCount = 0;
        mCodewordIndex = 0;
    }

    /**
     * Processes a received codeword.  After the 16 codewords of a batch, the next codeword should be a sync
     * codeword that starts the next batch.
     */
    private void processCodeword(int codeword)
    {
        if(mCodewordIndex < CODEWORDS_PER_BATCH)
        {
            mBatch[mCodewordIndex++] = codeword;

            if(mCodewordIndex == CODEWORDS_PER_BATCH)
            {
                processBatch();
            }
        }
        else if(Integer.bitCount(codeword ^ SYNC_CODEWORD) <= SYNC_TRACK_MAX_BIT_ERRORS)
        {
            mCodewordIndex = 0;
            mSyncTimestamp = System.currentTimeMillis();
        }
        else
        {
            //End of transmission or loss of signal
            loseSync();
        }
    }

    /**
     * Error corrects and processes the codewords of a complete batch.
     */
    private void processBatch()
    {
        int[] corrected = new int[CODEWORDS_PER_BATCH];
        boolean[] valid = new boolean[CODEWORDS_PER_BATCH];
        int validCount = 0;

        for(int x = 0; x < CODEWORDS_PER_BATCH; x++)
        {
            long result = correct(mBatch[x]);
            valid[x] = result != UNCORRECTABLE;
            corrected[x] = (int)result;

            if(valid[x])
            {
                validCount++;
            }
        }

        if(validCount < BATCH_MIN_VALID_CODEWORDS)
        {
            loseSync();
            return;
        }

        //Report the batch sync now that the batch has been validated
        broadcast(new PagerIdleMessage(mSyncTimestamp, Protocol.POCSAG, mBitRate, mAfsk, null));

        for(int x = 0; x < CODEWORDS_PER_BATCH; x++)
        {
            processBatchCodeword(corrected[x], valid[x], x / 2);
        }
    }

    /**
     * Processes an error corrected batch codeword.
     * @param codeword corrected
     * @param valid true if the codeword was correctable
     * @param frame number (0-7) within the batch
     */
    private void processBatchCodeword(int codeword, boolean valid, int frame)
    {
        if(!valid)
        {
            if(mCapcode >= 0)
            {
                mMessageWords.add(PocsagMessageParser.ERROR_WORD);
            }
        }
        else if(codeword == IDLE_CODEWORD)
        {
            sendPage();
        }
        else if((codeword & 0x80000000) == 0)
        {
            //Address codeword
            sendPage();
            mCapcode = (((codeword >>> 13) & 0x3FFFF) << 3) | frame;
            mFunction = (codeword >>> 11) & 0x3;
            mPageTimestamp = System.currentTimeMillis();
        }
        else if(mCapcode >= 0)
        {
            //Message codeword
            mMessageWords.add((codeword >>> 11) & 0xFFFFF);

            if(mMessageWords.size() >= MAX_MESSAGE_CODEWORDS)
            {
                sendPage();
            }
        }
    }

    /**
     * Sends the page in progress to the listener.
     */
    private void sendPage()
    {
        if(mCapcode >= 0)
        {
            PocsagMessage message = PocsagMessageParser.parse(mPageTimestamp, mCapcode, mFunction,
                new ArrayList<>(mMessageWords), mBitRate, mAfsk);

            broadcast(message);
        }

        mCapcode = -1;
        mMessageWords.clear();
    }

    /**
     * Sends the message to the registered listener.
     */
    private void broadcast(IMessage message)
    {
        if(mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }

    /**
     * Error corrects a 32-bit POCSAG codeword using the BCH(31,21) code and the even parity bit.
     * @param codeword to correct
     * @return corrected codeword (in the lower 32 bits) or {@link #UNCORRECTABLE}
     */
    public static long correct(int codeword)
    {
        int bch = codeword >>> 1;
        int pattern = BCH3121.getErrorPattern(bch);

        if(pattern == BCH3121.UNCORRECTABLE)
        {
            return UNCORRECTABLE;
        }

        bch ^= pattern;
        int parity = (Integer.bitCount(bch) & 1);

        //Parity mismatch after correcting two bits indicates three or more errors.  Otherwise, the parity bit itself
        //was in error and is regenerated.
        if(parity != (codeword & 1) && Integer.bitCount(pattern) >= 2)
        {
            return UNCORRECTABLE;
        }

        return ((bch << 1) | parity) & 0xFFFFFFFFL;
    }
}
