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

package io.github.dsheirer.module.decode.nxdn.layer3.coding;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * NXDN convolutional decoder path for 1/2 rate K=5 convolutional code.  Tracks the creation of a candidate message and
 * calculates the encoded G1/G2 bit pair for each added message bit.  Compares the generated encoded bit pair to the
 * original encoded message and tracks the bit error metric as a score.  Uses the puncture provider to distinguish
 * puncture bit errors from real bit errors and tracks/places the correct/real bit error count in the decoded message.
 *
 * Provides an auto-decode method that attempts to decode the message up to the point that it encounters a bit error
 * between the auto-generated message sequence and the encoded message.  This method ignores errors in punctured bit
 * positions, returning true on success or false on failure where it leaves the message pointer at the first detected
 * bit error position so that we can pick up with trellis decoding there.
 */
public class Path implements Comparable<Path>
{
    //Pre-calculated G1 and G2 values across each of the 32 state values
    private static final boolean[] G1 = {false, true, false, true, false, true, false, true, true, false, true, false,
            true, false, true, false, true, false, true, false, true, false, true, false, false, true, false, true,
            false, true, false, true};
    private static final boolean[] G2 = {false, true, true, false, true, false, false, true, false, true, true, false,
            true, false, false, true, true, false, false, true, false, true, true, false, true, false, false, true, false,
            true, true, false};
    private static final int STATE_MASK = 0x1F; //K=5

    //Permanent, non-cloned fields
    private final CorrectedBinaryMessage mEncodedMessage;
    private final PunctureProvider mPunctureProvider;

    //Cloneable fields
    private int mState;
    private CorrectedBinaryMessage mMessage;
    private int mMessagePointer = 0;
    private int mEncodedPointer;
    private int mBitErrors;

    /**
     * Constructs an instance
     * @param encodedMessage convolutionally encoded message to decode
     * @param punctureProvider to identify punctured bit errors versus true bit errors
     */
    public Path(CorrectedBinaryMessage encodedMessage, PunctureProvider punctureProvider)
    {
        mEncodedMessage = encodedMessage;
        mPunctureProvider = punctureProvider;
        mMessage = new CorrectedBinaryMessage(encodedMessage.size() / 2);
    }

    /**
     * Decoded message for this path
     * @return message with corrected bit count
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * For autoDecode() this value points to the next bit that must be decoded.  If the autoDecode() returns true,
     * then this points to the end of the message.
     */
    public int getMessagePointer()
    {
        return mMessagePointer;
    }

    /**
     * Current score for this path.  Score is the total number of bit errors for this path relative to the encoded
     * message.
     */
    public int getBitErrorCount()
    {
        return mBitErrors;
    }

    /**
     * Creates a deep clone of this path
     * @return cloned path
     */
    public Path clone()
    {
        Path clone = new Path(mEncodedMessage, mPunctureProvider);
        clone.mState = mState;
        clone.mBitErrors = mBitErrors;
        clone.mMessagePointer = mMessagePointer;
        clone.mEncodedPointer = mEncodedPointer;
        clone.mMessage = new CorrectedBinaryMessage(mMessage); //Deep clone
        clone.mMessage.setCorrectedBitCount(mMessage.getCorrectedBitCount());
        return clone;
    }

    /**
     * Clones this path from the argument path.
     * @param identity to apply to this path.
     */
    public void cloneFrom(Path identity)
    {
        mState = identity.mState;
        mBitErrors = identity.mBitErrors;
        mMessagePointer = identity.mMessagePointer;
        mEncodedPointer = identity.mEncodedPointer;
        mMessage.clear();
        mMessage.xor(identity.mMessage);
        mMessage.setCorrectedBitCount(identity.mMessage.getCorrectedBitCount());
    }

    /**
     * Attempts to automatically decode the encoded message, stopping if/when it encounters a non-puncture bit error.
     * @return true if success or false if it encounters a bit error, leaving message pointer at the error position and
     * state at the correct state prior to the bit error.
     */
    public boolean autoDecode()
    {
        int state = 0, encodedPointer = 0;
        boolean g1, g2, t1, t2, error;
        //The final 4 message bits should be zero flushing bits, so don't test those
        for(int x = 0; x < mMessage.size() - 4; x++)
        {
            state = (state << 1) & STATE_MASK;

            //Encoded/transmitted bit 1/2
            t1 = mEncodedMessage.get(encodedPointer);
            t2 = mEncodedMessage.get(encodedPointer + 1);

            //Test encoding G1/G2 for a 0 bit
            g1 = G1[state];
            g2 = G2[state];

            error = (g1 ^ t1) || ((g2 ^ t2) & mPunctureProvider.isPreserved(encodedPointer + 1));

            if(error)
            {
                //Test encoding G1/G2 for a 1 bit
                state++;
                g1 = G1[state];
                g2 = G2[state];
                error = (g1 ^ t1) || ((g2 ^ t2) & mPunctureProvider.isPreserved(encodedPointer + 1));

                if(!error)
                {
                    mMessage.set(x, true); //Move ahead with the 1 bit
                }
            }
            else
            {
                mMessage.set(x, false); //Move ahead with the 0 bit
            }

            if(error)
            {
                mMessage.clear();
                return false;
            }

            encodedPointer += 2;
        }

        return true;
    }

    /**
     * Add the unencoded bit to the message, update the convolution state and lookup the G1/G2 encoded bit pair from
     * the current register state.  Compare G1/G2 bits to the encoded message to update the bit error metrics.
     * @param bit to add to this path's decoded message
     */
    public void add(boolean bit)
    {
        mMessage.set(mMessagePointer++, bit);
        mState = ((mState << 1) & STATE_MASK) + (bit ? 1 : 0);
        boolean g1 = G1[mState];
        boolean g2 = G2[mState];

        //Compare the g1 bit against the encoded message to account for errors and update the bit error count
        if(g1 ^ mEncodedMessage.get(mEncodedPointer++))
        {
            mBitErrors++;
            mMessage.incrementCorrectedBitCount(1);
        }

        //Compare the g2 bit against the encoded message to account for errors and update the bit error count
        if(g2 ^ mEncodedMessage.get(mEncodedPointer))
        {
            mBitErrors++;

            //NXDN only does puncturing on the G2 bit sequence.  Only update (true) bit corrections if it's not a punctured bit
            if(mPunctureProvider.isPreserved(mEncodedPointer))
            {
                mMessage.incrementCorrectedBitCount(1);
            }
        }

        mEncodedPointer++;
    }

    @Override
    public String toString()
    {
        return "BIT ERRORS:" + getMessage().getCorrectedBitCount() + " SCORE: " + getBitErrorCount() + " MSG:" + getMessage();
    }

    /**
     * Comparator implementation that sorts by non-punctured bit errors first and bit errors second.
     * @param other path to be compared.
     * @return comparison value
     */
    @Override
    public int compareTo(Path other)
    {
        if(mMessage.getCorrectedBitCount() < other.mMessage.getCorrectedBitCount())
        {
            return -1;
        }

        if(mMessage.getCorrectedBitCount() > other.mMessage.getCorrectedBitCount())
        {
            return 1;
        }

        return Integer.compare(mBitErrors, other.mBitErrors);
    }
}
