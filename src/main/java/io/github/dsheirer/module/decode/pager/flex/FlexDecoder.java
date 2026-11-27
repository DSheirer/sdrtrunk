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

package io.github.dsheirer.module.decode.pager.flex;

import io.github.dsheirer.module.decode.pager.BCH3121;
import io.github.dsheirer.module.decode.pager.FskSymbolDemodulator;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.pager.PagerIdleMessage;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import java.util.Arrays;

/**
 * FLEX frame decoder.
 *
 * Each 1.875 second frame is:
 * <ul>
 *     <li>Sync 1 at 1600 symbols per second, 2-level: bit sync, then a 64-bit word with a 16-bit mode code, a fixed
 *     32-bit marker and the inverted mode code.</li>
 *     <li>Frame information word (FIW) at 1600/2: 16 dotting bits, then a 32-bit word with the cycle and frame
 *     numbers.</li>
 *     <li>Sync 2: 25 milliseconds at the data symbol rate.</li>
 *     <li>Data: 1.76 seconds at the data symbol rate and levels, carrying 11 blocks of 8 interleaved 32-bit words in
 *     each of 1, 2 or 4 phases (A, B, C, D).</li>
 * </ul>
 *
 * Both signal polarities are supported: the polarity is detected from the sync 1 word.  This decoder controls the
 * symbol rate and levels of the symbol demodulator, which switches to the frame's data rate after the FIW and back to
 * 1600/2 for the next frame.
 */
public class FlexDecoder implements FskSymbolDemodulator.ISymbolListener
{
    private static final int SYNC_MARKER = 0xA6C6AAAA;
    private static final int SYNC_MAX_BIT_ERRORS = 3;
    private static final int SYNC_SYMBOL_RATE = 1600;
    private static final int FIW_DOTTING_BITS = 16;
    private static final int FIW_BITS = 32;
    private static final int SYNC2_MILLISECONDS = 25;
    private static final int DATA_MILLISECONDS = 1760;
    private static final int PHASE_COUNT = 4;
    private static final char[] PHASE_LABELS = {'A', 'B', 'C', 'D'};
    private static final float SLICE_THRESHOLD = 0.667f;
    private static final int MAX_INVALID_WORDS_PER_PHASE = FlexMessageParser.WORDS_PER_PHASE / 4;

    private final FskSymbolDemodulator mDemodulator;
    private final Listener<IMessage> mMessageListener;
    private State mState = State.SYNC1;
    private long mSyncRegister;
    private boolean mInverted;
    private FlexMode mMode;
    private int mCount;
    private int mFrameInformationWord;
    private int mCycle;
    private int mFrame;
    private long mFrameTimestamp;
    private int mDataSymbolCount;
    private int mDataBitCounter;
    private boolean mPhaseToggle;
    private final int[][] mPhases = new int[PHASE_COUNT][FlexMessageParser.WORDS_PER_PHASE];

    /**
     * Constructs an instance
     * @param demodulator providing symbols to this decoder.  This decoder changes the symbol rate and levels of the
     * demodulator to follow the frame structure.
     * @param messageListener to receive decoded pages and sync (idle) messages
     */
    public FlexDecoder(FskSymbolDemodulator demodulator, Listener<IMessage> messageListener)
    {
        mDemodulator = demodulator;
        mMessageListener = messageListener;
    }

    /**
     * Symbol rate for sync 1 detection
     */
    public static int getSyncSymbolRate()
    {
        return SYNC_SYMBOL_RATE;
    }

    /**
     * Indicates if the decoder is currently synchronized to a frame
     */
    public boolean isSynchronized()
    {
        return mState != State.SYNC1;
    }

    /**
     * Resets the decoder.  A partially received frame is decoded so that the pages in the received blocks are
     * recovered.
     */
    public void reset()
    {
        if(mState == State.DATA)
        {
            decodeData();
        }

        returnToSync();
    }

    @Override
    public void receive(float symbol)
    {
        switch(mState)
        {
            case SYNC1 -> processSync(symbol);
            case FIW -> processFrameInformation(symbol);
            case SYNC2 ->
            {
                if(++mCount >= mMode.getSymbolRate() * SYNC2_MILLISECONDS / 1000)
                {
                    startData();
                }
            }
            case DATA -> processData(symbol);
        }
    }

    /**
     * Searches for the sync 1 word.
     */
    private void processSync(float symbol)
    {
        //The sync 1 word bits are 1 for the lower (negative) frequency.
        mSyncRegister = (mSyncRegister << 1) | (symbol < 0.0f ? 1 : 0);

        FlexMode mode = getMode(mSyncRegister);
        boolean inverted = false;

        if(mode == null)
        {
            mode = getMode(~mSyncRegister);
            inverted = true;
        }

        if(mode != null)
        {
            mMode = mode;
            mInverted = inverted;
            mState = State.FIW;
            mCount = 0;
            mFrameInformationWord = 0;
            mFrameTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Checks the 64-bit sync register for a sync 1 word.
     * @return mode identified by the sync word, or null.
     */
    static FlexMode getMode(long register)
    {
        int marker = (int)(register >>> 16);

        if(Integer.bitCount(marker ^ SYNC_MARKER) > SYNC_MAX_BIT_ERRORS)
        {
            return null;
        }

        int codeHigh = (int)(register >>> 48) & 0xFFFF;
        int codeLow = (int)(~register) & 0xFFFF;

        if(Integer.bitCount(codeHigh ^ codeLow) > SYNC_MAX_BIT_ERRORS)
        {
            return null;
        }

        return FlexMode.fromSyncCode(codeHigh);
    }

    /**
     * Collects and decodes the frame information word.
     */
    private void processFrameInformation(float symbol)
    {
        mCount++;

        if(mCount > FIW_DOTTING_BITS)
        {
            if(getSymbol(symbol) > 1)
            {
                mFrameInformationWord |= 1 << (mCount - FIW_DOTTING_BITS - 1);
            }

            if(mCount == FIW_DOTTING_BITS + FIW_BITS)
            {
                int fiw = correct(mFrameInformationWord);

                if(fiw >= 0 && isValidChecksum(fiw))
                {
                    mCycle = (fiw >>> 4) & 0xF;
                    mFrame = (fiw >>> 8) & 0x7F;
                    mState = State.SYNC2;
                    mCount = 0;
                    mDemodulator.setSymbolRate(mMode.getSymbolRate());
                    mDemodulator.setLevels(mMode.getLevels());

                    //Report the frame sync now that the frame information word has been validated
                    if(mMessageListener != null)
                    {
                        mMessageListener.receive(new PagerIdleMessage(mFrameTimestamp, Protocol.FLEX,
                            mMode.getBitRate(), false, "MODE:" + mMode + " CYCLE:" + mCycle + " FRAME:" + mFrame));
                    }
                }
                else
                {
                    returnToSync();
                }
            }
        }
    }

    /**
     * Verifies the 4-bit FIW checksum: the sum of the five lower nibbles and bit 20 is 0xF.
     */
    static boolean isValidChecksum(int fiw)
    {
        int checksum = (fiw & 0xF) + ((fiw >>> 4) & 0xF) + ((fiw >>> 8) & 0xF) + ((fiw >>> 12) & 0xF) +
            ((fiw >>> 16) & 0xF) + ((fiw >>> 20) & 0x1);
        return (checksum & 0xF) == 0xF;
    }

    /**
     * Starts collecting the frame data.
     */
    private void startData()
    {
        mState = State.DATA;
        mDataSymbolCount = 0;
        mDataBitCounter = 0;
        mPhaseToggle = false;

        for(int[] phase: mPhases)
        {
            Arrays.fill(phase, 0);
        }
    }

    /**
     * De-interleaves a data symbol into the phase words.  Each block of 256 bits per phase is 8 words of 32 bits
     * sent column by column: bit n of the block is bit (n / 8) of word (n % 8).  At 3200 symbols per second, symbols
     * alternate between phases A/B and phases C/D.  For 4-level modes, each symbol carries one bit in A (or C) and
     * one bit in B (or D).
     */
    private void processData(float value)
    {
        int symbol = getSymbol(value);
        int bitA = symbol > 1 ? 1 : 0;
        int bitB = (mMode.getLevels() == 4 && (symbol == 1 || symbol == 2)) ? 1 : 0;

        if(mMode.getSymbolRate() == SYNC_SYMBOL_RATE)
        {
            mPhaseToggle = false;
        }

        int index = ((mDataBitCounter >>> 5) & 0xFFF8) | (mDataBitCounter & 0x7);
        int bit = (mDataBitCounter >>> 3) & 0x1F;

        if(index < FlexMessageParser.WORDS_PER_PHASE)
        {
            if(!mPhaseToggle)
            {
                mPhases[0][index] |= bitA << bit;
                mPhases[1][index] |= bitB << bit;
            }
            else
            {
                mPhases[2][index] |= bitA << bit;
                mPhases[3][index] |= bitB << bit;
            }
        }

        mPhaseToggle = !mPhaseToggle;

        if(mMode.getSymbolRate() == SYNC_SYMBOL_RATE || !mPhaseToggle)
        {
            mDataBitCounter++;
        }

        if(++mDataSymbolCount >= mMode.getSymbolRate() * DATA_MILLISECONDS / 1000)
        {
            decodeData();
            returnToSync();
        }
    }

    /**
     * Error corrects and parses the active phases of the frame.
     */
    private void decodeData()
    {
        boolean[] activePhases = getActivePhases(mMode);

        for(int phase = 0; phase < PHASE_COUNT; phase++)
        {
            if(activePhases[phase])
            {
                int[] words = new int[FlexMessageParser.WORDS_PER_PHASE];
                boolean[] valid = new boolean[FlexMessageParser.WORDS_PER_PHASE];
                int invalidCount = 0;

                for(int x = 0; x < FlexMessageParser.WORDS_PER_PHASE; x++)
                {
                    int corrected = correct(mPhases[phase][x]);
                    valid[x] = corrected >= 0;
                    words[x] = valid[x] ? corrected : 0;

                    if(!valid[x])
                    {
                        invalidCount++;
                    }
                }

                //A phase with many uncorrectable words is too corrupted to trust the words that do pass error
                //correction, since a large fraction of random words are within two bit errors of a codeword.
                if(invalidCount > MAX_INVALID_WORDS_PER_PHASE)
                {
                    continue;
                }

                for(FlexMessage message: FlexMessageParser.parse(words, valid, PHASE_LABELS[phase], mMode, mCycle,
                    mFrame, mFrameTimestamp))
                {
                    if(mMessageListener != null)
                    {
                        mMessageListener.receive(message);
                    }
                }
            }
        }
    }

    /**
     * Phases that carry data for the mode: A for 1600/2, A and B for 3200/4, A and C for 3200/2, and all four for
     * 6400/4.
     */
    static boolean[] getActivePhases(FlexMode mode)
    {
        boolean fourLevel = mode.getLevels() == 4;
        boolean fast = mode.getSymbolRate() > SYNC_SYMBOL_RATE;
        return new boolean[]{true, fourLevel, fast, fast && fourLevel};
    }

    /**
     * Returns to sync 1 detection at 1600/2.
     */
    private void returnToSync()
    {
        mState = State.SYNC1;
        mSyncRegister = 0;
        mCount = 0;
        mDemodulator.setSymbolRate(SYNC_SYMBOL_RATE);
        mDemodulator.setLevels(2);
    }

    /**
     * Slices the normalized symbol value into a symbol (0-3) where 0 is the most negative and 3 the most positive
     * level after correcting for the detected signal polarity.  For 2-level signals, the symbol is 0 or 3.
     */
    private int getSymbol(float value)
    {
        int symbol;

        if(mState == State.DATA && mMode.getLevels() == 4)
        {
            if(value > 0.0f)
            {
                symbol = value > SLICE_THRESHOLD ? 3 : 2;
            }
            else
            {
                symbol = value < -SLICE_THRESHOLD ? 0 : 1;
            }
        }
        else
        {
            symbol = value > 0.0f ? 3 : 0;
        }

        return mInverted ? 3 - symbol : symbol;
    }

    /**
     * Error corrects a received FLEX word, where bit 0 was received first, using the BCH(31,21) code in bits 0-30 and
     * the even parity bit in bit 31.
     * @param word received
     * @return 21 data bits, or -1 if the word can't be corrected.
     */
    static int correct(int word)
    {
        int codeword = BCH3121.reverse31(word);
        int pattern = BCH3121.getErrorPattern(codeword);

        if(pattern == BCH3121.UNCORRECTABLE)
        {
            return -1;
        }

        int corrected = BCH3121.reverse31(codeword ^ pattern);

        //Parity mismatch after correcting two bits indicates three or more errors.
        if(Integer.bitCount(pattern) >= 2 && ((Integer.bitCount(corrected) & 1) != (word >>> 31)))
        {
            return -1;
        }

        return corrected & 0x1FFFFF;
    }

    private enum State
    {
        SYNC1, FIW, SYNC2, DATA
    }
}
