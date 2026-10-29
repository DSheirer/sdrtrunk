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

package io.github.dsheirer.module.decode.p25.phase2.message.isch;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the I-ISCH (40,9,16) codeword decoder.
 */
public class ISCHDecoderTest
{
    private static final long CODE_WORD_OFFSET = 0x184229d461L;
    private final ISCHDecoder mDecoder = new ISCHDecoder();

    /**
     * Creates the transmitted 40-bit codeword for the 9-bit message value.
     */
    private static BinaryMessage encode(int value)
    {
        BinaryMessage message = new BinaryMessage(9);
        message.load(0, 9, value);
        BinaryMessage codeword = new BinaryMessage(40);
        codeword.load(0, 40, ISCHDecoder.getCodeWord(message) ^ CODE_WORD_OFFSET);
        return codeword;
    }

    /**
     * Channel number (1 or 2) for the message value, or 0 for an ISCH location (3) that isn't used for I-ISCH.
     */
    private static int getChannel(int value)
    {
        int ischLocation = (value >> 3) & 0x3;
        return ischLocation == 3 ? 0 : ((value >> 5) & 0x3) + 1;
    }

    @Test
    public void correctsUpToSevenBitErrors()
    {
        Random random = new Random(1);

        for(int value = 0; value < 512; value++)
        {
            int channel = getChannel(value);

            if(channel != 1 && channel != 2)
            {
                continue;
            }

            for(int errors = 0; errors <= 7; errors++)
            {
                for(int trial = 0; trial < 10; trial++)
                {
                    BinaryMessage codeword = encode(value);
                    int flipped = 0;

                    while(flipped < errors)
                    {
                        int bit = random.nextInt(40);

                        if(codeword.get(bit))
                        {
                            codeword.clear(bit);
                        }
                        else
                        {
                            codeword.set(bit);
                        }

                        flipped++;
                    }

                    CorrectedBinaryMessage decoded = mDecoder.decode(codeword, channel);
                    assertEquals(value, decoded.getInt(0, 8), "Value [" + value + "] errors [" + errors + "]");
                    assertTrue(decoded.getCorrectedBitCount() <= errors);
                }
            }
        }
    }

    @Test
    public void rejectsWrongTimeslotCodewords()
    {
        for(int value = 0; value < 512; value++)
        {
            int channel = getChannel(value);

            if(channel == 1 || channel == 2)
            {
                //A codeword for the other timeslot is at least 16 bits from every codeword for the expected timeslot
                CorrectedBinaryMessage decoded = mDecoder.decode(encode(value), channel == 1 ? 2 : 1);
                assertTrue(decoded.getCorrectedBitCount() >= 8, "Value [" + value + "]");
            }
        }
    }
}
