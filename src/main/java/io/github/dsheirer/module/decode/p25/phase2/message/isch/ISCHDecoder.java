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

package io.github.dsheirer.module.decode.p25.phase2.message.isch;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Informational Inter-slot Signalling Chaannel (I-ISCH) decoder
 */
public class ISCHDecoder
{
    private final static Logger mLog = LoggerFactory.getLogger(ISCHDecoder.class);
    private static final IntField CHANNEL_NUMBER = IntField.range(2, 3);
    private static final IntField ISCH_LOCATION = IntField.range(4, 5);

    private static final Map<Long,BinaryMessage> CODEWORD_MAP_TS1 = new TreeMap<>();
    private static final Map<Long,BinaryMessage> CODEWORD_MAP_TS2 = new TreeMap<>();
    private static final long[] GENERATOR = {0x8816CE36D7l, 0x201DFD4F64l, 0x100F4B1758l, 0x0C00DED18El, 0x020807F7FFl,
        0x09048D9B72l, 0x009DA3A171l, 0x0058CBAA4El, 0x00343D8597l};
    private static final long CODE_WORD_OFFSET = 0x184229d461l;

    /**
     * Constructs the ISCH-I parsing class
     */
    public ISCHDecoder()
    {
        for(int x = 0; x < 512; x++)
        {
            BinaryMessage message = new BinaryMessage(9);
            message.load(0, 9, x);

            int channelNumber = message.getInt(CHANNEL_NUMBER) + 1;
            int ischLocation = message.getInt(ISCH_LOCATION);

            long codeword = getCodeWord(message);
            codeword ^= CODE_WORD_OFFSET;

            if(ischLocation != 3)
            {
                if(channelNumber == 1)
                {
                    CODEWORD_MAP_TS1.put(codeword, message);
                }
                else if(channelNumber == 2)
                {
                    CODEWORD_MAP_TS2.put(codeword, message);
                }
            }
        }
    }

    /**
     * Creates a codeword from the binary message where each set bit in the binary message corresponds to one of the
     * words in the generator and we XOR the words together.
     * @param message for generating a codeword
     * @return codeword
     */
    public static long getCodeWord(BinaryMessage message)
    {
        long codeword = 0;

        for(int x = 0; x < message.length(); x++)
        {
            if(message.get(x))
            {
                codeword ^= GENERATOR[x];
            }
        }

        return codeword;
    }

    /**
     * Decodes the 40-bit message codeword into an error-corrected 9-bit message.  The expected timeslot allows us to
     * only use the codewords that are correct for the expected timeslot.
     * @param message containing 40 bit codeword
     * @param expectedTimeslot either 1 or 2
     */
    public CorrectedBinaryMessage decode(BinaryMessage message, int expectedTimeslot)
    {
        long codeword = message.getLong(0, 39);

        if(expectedTimeslot == 1)
        {
            if(CODEWORD_MAP_TS1.containsKey(codeword))
            {
                return new CorrectedBinaryMessage(CODEWORD_MAP_TS1.get(codeword));
            }
            else
            {
                int smallestErrorCount = 8;
                long closestCodeword = 0;

                for(long validCodeword: CODEWORD_MAP_TS1.keySet())
                {
                    long mask = codeword & validCodeword;
                    int errorCount = Long.bitCount(mask);

                    if(errorCount < smallestErrorCount)
                    {
                        smallestErrorCount = errorCount;
                        closestCodeword = validCodeword;
                    }
                }

                if(closestCodeword != 0 && smallestErrorCount <= 7) //Max correctable bit errors of 7
                {
                    CorrectedBinaryMessage decoded = new CorrectedBinaryMessage(CODEWORD_MAP_TS1.get(closestCodeword));
                    decoded.setCorrectedBitCount(smallestErrorCount);
                    return  decoded;
                }
            }
        }
        else if(expectedTimeslot == 2)
        {
            if(CODEWORD_MAP_TS2.containsKey(codeword))
            {
                return new CorrectedBinaryMessage(CODEWORD_MAP_TS2.get(codeword));
            }
            else
            {
                int smallestErrorCount = 16;
                long closestCodeword = 0;

                for(long validCodeword: CODEWORD_MAP_TS2.keySet())
                {
                    long mask = codeword & validCodeword;
                    int errorCount = Long.bitCount(mask);

                    if(errorCount < smallestErrorCount)
                    {
                        smallestErrorCount = errorCount;
                        closestCodeword = validCodeword;
                    }
                }

                if(closestCodeword != 0 && smallestErrorCount <= 7) //Max correctable bit errors of 7
                {
                    CorrectedBinaryMessage decoded = new CorrectedBinaryMessage(CODEWORD_MAP_TS2.get(closestCodeword));
                    decoded.setCorrectedBitCount(smallestErrorCount);
                    return  decoded;
                }
            }
        }
        else
        {
            mLog.warn("Unexpected timeslot value: " + expectedTimeslot);
        }

        //This shouldn't happen, but we'll set bit error count to 9 to indicate a bad decode
        CorrectedBinaryMessage decoded = new CorrectedBinaryMessage(9);
        decoded.setCorrectedBitCount(9);
        return decoded;
    }
}
