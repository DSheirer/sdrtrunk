/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ISCHSequence;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.SuperframeSequence;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * Inter-slot signalling channel informational (ISCH-I) parsing class
 */
public class InterSlotSignallingChannel
{
    private final static Logger mLog = LoggerFactory.getLogger(InterSlotSignallingChannel.class);

    private static Map<Long,BinaryMessage> sCodewordMap = new TreeMap<>();

    static
    {
        double[][] matrix = {{1,0,0,0,1,0,0,0,0,0,0,1,0,1,1,0,1,1,0,0,1,1,1,0,0,0,1,1,0,1,1,0,1,1,0,1,0,1,1,1},
            {0,0,1,0,0,0,0,0,0,0,0,1,1,1,0,1,1,1,1,1,1,1,0,1,0,1,0,0,1,1,1,1,0,1,1,0,0,1,0,0},
            {0,0,0,1,0,0,0,0,0,0,0,0,1,1,1,1,0,1,0,0,1,0,1,1,0,0,0,1,0,1,1,1,0,1,0,1,1,0,0,0},
            {0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,1,1,1,0,1,1,0,1,0,0,0,1,1,0,0,0,1,1,1,0},
            {0,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1},
            {0,0,0,0,1,0,0,1,0,0,0,0,0,1,0,0,1,0,0,0,1,1,0,1,1,0,0,1,1,0,1,1,0,1,1,1,0,0,1,0},
            {0,0,0,0,0,0,0,0,1,0,0,1,1,1,0,1,1,0,1,0,0,0,1,1,1,0,1,0,0,0,0,1,0,1,1,1,0,0,0,1},
            {0,0,0,0,0,0,0,0,0,1,0,1,1,0,0,0,1,1,0,0,1,0,1,1,1,0,1,0,1,0,1,0,0,1,0,0,1,1,1,0},
            {0,0,0,0,0,0,0,0,0,0,1,1,0,1,0,0,0,0,1,1,1,1,0,1,1,0,0,0,0,1,0,1,1,0,0,1,0,1,1,1}};

        RealMatrix generator = MatrixUtils.createRealMatrix(matrix);

        //We only generate a lookup map for 7/10 least significant bits since MSB and reserved bits are not used
        for(int x = 0; x < 128; x++)
        {
            RealMatrix word = toMatrix10(x);
            RealMatrix codewordMatrix = word.multiply(generator);
            long codeword = decodeMatrix(codewordMatrix);
            codeword ^= 0x184229d461l;
            BinaryMessage message = new BinaryMessage(9);
            message.load(0, 9, x);

            sCodewordMap.put(codeword, message);
        }
    }

    private static final int[] RESERVED = {0, 1};
    private static final int[] CHANNEL_NUMBER = {2, 3};
    private static final int[] ISCH_SEQUENCE = {4, 5};
    private static final int INBOUND_SACCH_FREE_INDICATOR = 6;
    private static final int[] SUPERFRAME_SEQUENCE = {7, 8};

    private CorrectedBinaryMessage mMessage;
    private boolean mValid;

    /**
     * Constructs the ISCH-I parsing class
     *
     * @param message containing bits
     * @param expectedTimeslot for this ISCH, either channel 0 or channel 1 to assist with validating the message
     */
    public InterSlotSignallingChannel(BinaryMessage message, int expectedTimeslot)
    {
        decode(message);
        mValid = (getMessage().getCorrectedBitCount() < 9) && (getTimeslot() == expectedTimeslot);
    }

    /**
     * Decodes the 40-bit message codeword into an error-corrected 9-bit message
     * @param message containing 40 bit codeword
     */
    private void decode(BinaryMessage message)
    {
        long codeword = message.getLong(0, 39);

        if(sCodewordMap.containsKey(codeword))
        {
            mMessage = new CorrectedBinaryMessage(sCodewordMap.get(codeword));
        }
        else
        {
            int smallestErrorCount = 16;
            long closestCodeword = 0;

            for(long validCodeword: sCodewordMap.keySet())
            {
                long mask = codeword & validCodeword;
                int errorCount = Long.bitCount(mask);

                if(errorCount < smallestErrorCount)
                {
                    smallestErrorCount = errorCount;
                    closestCodeword = validCodeword;
                }
            }

            if(closestCodeword != 0)
            {
                mMessage = new CorrectedBinaryMessage(sCodewordMap.get(closestCodeword));
                mMessage.setCorrectedBitCount(smallestErrorCount);
            }
            else
            {
                //This shouldn't happen, but we'll set bit error count to 9 to indicate a bad decode
                mMessage = new CorrectedBinaryMessage(9);
                mMessage.setCorrectedBitCount(9);
            }
        }
    }

    /**
     * Indicates if this message is valid
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Bit error count or the number of bits that were corrected while decoding the transmitted 40-bit codeword
     */
    public int getBitErrorCount()
    {
        return getMessage().getCorrectedBitCount();
    }

    /**
     * Decoded and corrected 9-bit message
     */
    private CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Timeslot for this ISCH
     *
     * @return timeslot 0 or 1
     */
    public int getTimeslot()
    {
        return getMessage().getInt(CHANNEL_NUMBER);
    }

    /**
     * Indicates this ISCH sequence's location within a super-frame
     *
     * @return location 1, 2, or 3(final)
     */
    public ISCHSequence getIschSequence()
    {
        return ISCHSequence.fromValue(getMessage().getInt(ISCH_SEQUENCE));
    }

    /**
     * Indicates if the next inbound SACCH timeslot is free for mobile access
     *
     * @return true if the inbound SACCH is free
     */
    public boolean isInboundSacchFree()
    {
        return getMessage().get(INBOUND_SACCH_FREE_INDICATOR);
    }

    /**
     * Superframe sequence/location within an ultraframe
     *
     * @return location, 1-4
     */
    public SuperframeSequence getSuperframeSequence()
    {
        return SuperframeSequence.fromValue(getMessage().getInt(SUPERFRAME_SEQUENCE));
    }

    /**
     * Decoded string representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(isValid())
        {
            sb.append("ISCHI ").append(getTimeslot());
            sb.append(" ").append(getIschSequence());
            sb.append(" ").append(getSuperframeSequence());
            sb.append(isInboundSacchFree() ? " SACCH:FREE" : " SACCH:BUSY");
        }
        else
        {
            sb.append("ISCHI      **INVALID**        ");
        }

        return sb.toString();
    }

    /**
     * Creates a 10-element matrix from the value.
     * @param value in range 0 - 127
     * @return matrix
     */
    private static RealMatrix toMatrix10(int value)
    {
        double[] values = new double[9];
        for(int x = 0; x < 7; x++)
        {
            int mask = (int)Math.pow(2, x);
            if((value & mask) == mask)
            {
                values[8 - x] = 1;
            }
        }

        return MatrixUtils.createRowRealMatrix(values);
    }

    /**
     * Decodes the matrix which is assumed to be a single row with 40 elements representing bits
     * @param matrix to decode
     * @return long value
     */
    private static long decodeMatrix(RealMatrix matrix)
    {
        long decoded = 0;
        double[] values = matrix.getRow(0);

        for(int x = 0; x < 40; x++)
        {
            int value = (int)values[39 - x];

            if((value & 1) == 1)
            {
                decoded += (long)Math.pow(2, x);
            }
        }

        return decoded;
    }
}
