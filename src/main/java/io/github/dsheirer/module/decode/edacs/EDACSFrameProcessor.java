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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.edacs.message.EDACSMessage;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frame processor for EDACS control channel. Accepts a stream of demodulated
 * bits (0 or 1), detects frame boundaries via the 48-bit EDACS sync word,
 * assembles 6x 40-bit message words, applies 3-copy majority voting plus BCH(40,28)
 * error correction, and produces EDACSMessage instances via
 * {@link EDACSMessageFactory}.
 *
 * <p>Extracted from the original {@code EDACSSyncDetector} so that the
 * FM-demodulator stage can be bypassed when validating frames from a
 * pre-recorded symbol stream (e.g. DSD-FME's {@code .bin} capture).</p>
 *
 * <p>Wire format (EDACS-FM / DSD-FME reference, US patent US7546135B2):</p>
 * <ul>
 *   <li>Each outbound control channel frame = 48-bit sync + 6 * 40-bit
 *       message words = 288 bits total</li>
 *   <li>48-bit sync word: first 24 bits are alternating (dotting), last
 *       24 bits are the identifier. Read MSB-first on the wire the value
 *       is {@code 0x555557125555} (positive polarity) or its bitwise
 *       inverse {@code 0xAAAAEA8AAAAA} (negative polarity)</li>
 *   <li>The 6 message words are 3 copies of message-1 followed by 3
 *       copies of message-2; copies 2 and 5 (the middle copies) are
 *       bitwise-inverted on the wire for majority-vote resilience</li>
 *   <li>Each 40-bit word is a shortened BCH(40,28) codeword: 28 data
 *       bits + 12 BCH parity bits, capable of correcting up to 2 errors</li>
 * </ul>
 *
 * <p>Reference: DSD-FME {@code dsd_frame_sync.c} confirms the complete 48-bit
 * EDACS sync before dispatching a frame to {@code edacs-fme.c}. Dotting alone
 * is not a safe frame boundary because BCH false-passes can turn misaligned
 * frames into plausible but incorrect traffic grants.</p>
 */
public class EDACSFrameProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(EDACSFrameProcessor.class);

    private static final int SYNC_BITS = 48;
    private static final int WORD_BITS = 40;
    private static final int DATA_BITS = 6 * WORD_BITS;
    private static final int FRAME_BITS = SYNC_BITS + DATA_BITS;
    private static final long SYNC_MASK = 0xFFFFFFFFFFFFL;

    /** 48-bit EDACS sync word (positive polarity), MSB-first. */
    private static final long EDACS_SYNC = 0x555557125555L;

    /** 48-bit EDACS sync word (negative / inverted polarity), MSB-first. */
    private static final long EDACS_SYNC_INV = 0xAAAAEA8AAAAAL;

    private final boolean[] mDataBits = new boolean[DATA_BITS];
    private long mSyncRegister = 0;
    private int mFramePending = 0;
    private int mDataBitCount = 0;
    private boolean mInvertPayload = false;

    private final EDACSVoter mVoter = new EDACSVoter();
    private final boolean mEsk;

    /**
     * Constructs an instance for a control channel that does not use EDACS Security Key (ESK).
     */
    public EDACSFrameProcessor()
    {
        this(false);
    }

    /**
     * Constructs an instance
     * @param esk true if the control channel uses EDACS Security Key (ESK)
     */
    public EDACSFrameProcessor(boolean esk)
    {
        mEsk = esk;
    }

    /**
     * Feeds a single demodulated bit to the frame processor. Bit value 1
     * represents a positive FM deviation and 0 a negative. Idle / no-carrier
     * periods should be skipped by the caller.
     */
    public void processBit(int bit, Listener<IMessage> messageListener)
    {
        boolean currentBit = (bit != 0);
        mSyncRegister = ((mSyncRegister << 1) | (currentBit ? 1 : 0)) & SYNC_MASK;

        if(mFramePending > 0)
        {
            mDataBits[mDataBitCount++] = currentBit ^ mInvertPayload;
            mFramePending--;
            if(mFramePending == 0)
            {
                decodeFrame(messageListener);
                mDataBitCount = 0;
            }

            return;
        }

        if(mSyncRegister == EDACS_SYNC || mSyncRegister == EDACS_SYNC_INV)
        {
            mInvertPayload = mSyncRegister == EDACS_SYNC_INV;
            mFramePending = DATA_BITS;
            mDataBitCount = 0;
        }

    }

    /**
     * Tests whether the 48-bit register matches the EDACS sync word in
     * either polarity. Currently unused by the live bit path; retained
     * for future exact-sync enhancement.
     */
    private boolean isSyncMatch(long register)
    {
        return register == EDACS_SYNC || register == EDACS_SYNC_INV;
    }

    private void decodeFrame(Listener<IMessage> messageListener)
    {

        CorrectedBinaryMessage[] words = new CorrectedBinaryMessage[6];
        for(int w = 0; w < 6; w++)
        {
            CorrectedBinaryMessage word = new CorrectedBinaryMessage(WORD_BITS);
            for(int b = 0; b < WORD_BITS; b++)
            {
                if(mDataBits[w * WORD_BITS + b])
                {
                    word.set(b);
                }
            }
            words[w] = word;
        }

        CorrectedBinaryMessage data1 = mVoter.vote(words[0], words[1], words[2]);
        CorrectedBinaryMessage data2 = mVoter.vote(words[3], words[4], words[5]);

        if(data1 == null || data2 == null)
        {
            return;
        }

        EDACSMessage message = EDACSMessageFactory.create(data1, data2, System.currentTimeMillis(), mEsk);

        if(message == null)
        {
            return;
        }

        if(messageListener != null)
        {
            messageListener.receive(message);
        }
    }
}
