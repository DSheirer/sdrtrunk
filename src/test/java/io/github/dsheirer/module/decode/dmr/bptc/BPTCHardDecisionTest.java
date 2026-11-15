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

package io.github.dsheirer.module.decode.dmr.bptc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Hamming13;
import io.github.dsheirer.edac.Hamming15;
import io.github.dsheirer.edac.Hamming16;
import io.github.dsheirer.edac.Hamming17;
import io.github.dsheirer.edac.ProductCodeDecoder;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Error injection tests for the DMR BPTC hard decision decoders: every error pattern up to the guaranteed correction
 * capability (half the product code minimum distance) is corrected.  For the codes with an even minimum distance,
 * error patterns with one more bit error are corrected or detected (message unchanged), but never miscorrected.  Also
 * tests the fallback decoders for error patterns that exceed the minimum distance decoder maximum.
 */
public class BPTCHardDecisionTest
{
    private static final int[] H15 = ProductCodeDecoder.getParityChecks(15, m -> Hamming15.getSyndrome(m, 0));
    private static final int[] H13 = ProductCodeDecoder.getParityChecks(13,
            m -> Hamming13.getSyndrome(m, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}));
    private static final int[] H16 = ProductCodeDecoder.getParityChecks(16, m -> Hamming16.getSyndrome(m, 0));
    private static final int[] H17 = ProductCodeDecoder.getParityChecks(17, m -> Hamming17.getSyndrome(m, 0));

    /**
     * Valid codewords for the row code.
     */
    private static int[] getCodewords(int[] checks)
    {
        return java.util.stream.IntStream.range(0, 1 << checks.length)
                .filter(word -> ProductCodeDecoder.getSyndrome(word, checks) == 0).toArray();
    }

    /**
     * Creates a random product codeword as row words: random row codewords for the information rows and column parity
     * rows from the systematic column code.
     */
    private static int[] createRows(Random random, int[] rowChecks, int[] columnChecks, int parityRows)
    {
        int[] codewords = getCodewords(rowChecks);
        int[] rows = new int[columnChecks.length];
        int informationRows = columnChecks.length - parityRows;

        for(int row = 0; row < informationRows; row++)
        {
            rows[row] = codewords[random.nextInt(codewords.length)];
        }

        for(int column = 0; column < rowChecks.length; column++)
        {
            int columnSyndrome = 0;

            for(int row = 0; row < informationRows; row++)
            {
                if((rows[row] & (1 << column)) != 0)
                {
                    columnSyndrome ^= columnChecks[row];
                }
            }

            for(int row = informationRows; row < rows.length; row++)
            {
                if((columnSyndrome & columnChecks[row]) != 0)
                {
                    rows[row] |= 1 << column;
                }
            }
        }

        return rows;
    }

    /**
     * Converts row words to a message where row r column c is bit offset + (r * columns) + c.
     */
    private static CorrectedBinaryMessage toMessage(int[] rows, int columns, int offset)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(offset + rows.length * columns);

        for(int row = 0; row < rows.length; row++)
        {
            for(int column = 0; column < columns; column++)
            {
                if((rows[row] & (1 << column)) != 0)
                {
                    message.set(offset + row * columns + column);
                }
            }
        }

        return message;
    }

    private static int[] range(int from, int to)
    {
        return java.util.stream.IntStream.range(from, to).toArray();
    }

    private static CorrectedBinaryMessage withErrors(CorrectedBinaryMessage codeword, int[] errors)
    {
        CorrectedBinaryMessage received = new CorrectedBinaryMessage(codeword);

        for(int error: errors)
        {
            received.flip(error);
        }

        return received;
    }

    /**
     * Checks the decoder outcome for a received message: corrected to the codeword with the corrected bit count set to
     * the error count, or (when allowed) uncorrectable with the message unchanged and the corrected bit count set to
     * ERRORS_NOT_CORRECTED.
     */
    private static void check(Predicate<CorrectedBinaryMessage> decoder, CorrectedBinaryMessage codeword, int[] errors,
                              boolean mustCorrect)
    {
        CorrectedBinaryMessage received = withErrors(codeword, errors);
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(received);

        if(decoder.test(message))
        {
            if(!message.equals(codeword))
            {
                fail("Miscorrected errors at " + Arrays.toString(errors));
            }

            assertEquals(errors.length, message.getCorrectedBitCount(), "Corrected bit count for " +
                    Arrays.toString(errors));
        }
        else
        {
            if(mustCorrect)
            {
                fail("Failed to correct errors at " + Arrays.toString(errors));
            }

            assertEquals(received, message, "Uncorrectable message was changed");
            assertEquals(BPTCBase.ERRORS_NOT_CORRECTED, message.getCorrectedBitCount());
        }
    }

    /**
     * Checks every error pattern of the specified weight over the positions.
     */
    private static void checkAll(Predicate<CorrectedBinaryMessage> decoder, CorrectedBinaryMessage codeword,
                                 int[] positions, int weight, boolean mustCorrect)
    {
        int[] pattern = new int[weight];
        int[] errors = new int[weight];

        for(int i = 0; i < weight; i++)
        {
            pattern[i] = i;
        }

        while(true)
        {
            for(int i = 0; i < weight; i++)
            {
                errors[i] = positions[pattern[i]];
            }

            check(decoder, codeword, errors, mustCorrect);

            //Advance to the next combination
            int i = weight - 1;

            while(i >= 0 && pattern[i] == positions.length - weight + i)
            {
                i--;
            }

            if(i < 0)
            {
                return;
            }

            pattern[i]++;

            for(int j = i + 1; j < weight; j++)
            {
                pattern[j] = pattern[j - 1] + 1;
            }
        }
    }

    /**
     * Checks random error patterns of the specified weight over the positions.
     */
    private static void checkRandom(Random random, Predicate<CorrectedBinaryMessage> decoder,
                                    CorrectedBinaryMessage codeword, int[] positions, int weight, int trials,
                                    boolean mustCorrect)
    {
        for(int trial = 0; trial < trials; trial++)
        {
            int[] errors = random.ints(0, positions.length).distinct().limit(weight).map(i -> positions[i]).toArray();
            check(decoder, codeword, errors, mustCorrect);
        }
    }

    /**
     * Test: BPTC(196,96) has a minimum distance of 9 and corrects every 1-4 bit error pattern.  Exhaustive for 1-3 bit
     * errors, all 2x2 row/column squares and random patterns for 4 bit errors.  Bit 0 is a pad bit.
     */
    @Test
    void testBPTC_196_96()
    {
        Random random = new Random(1);
        CorrectedBinaryMessage codeword = toMessage(createRows(random, H15, H13, 4), 15, 1);
        Predicate<CorrectedBinaryMessage> decoder = BPTC_196_96::correct;
        int[] positions = range(1, 196);

        check(decoder, codeword, new int[0], true);

        for(int weight = 1; weight <= 3; weight++)
        {
            checkAll(decoder, codeword, positions, weight, true);
        }

        //Errors at the corners of every row/column square, which shadow each other for row and column decoding
        for(int row1 = 0; row1 < 13; row1++)
        {
            for(int row2 = row1 + 1; row2 < 13; row2++)
            {
                for(int column1 = 0; column1 < 15; column1++)
                {
                    for(int column2 = column1 + 1; column2 < 15; column2++)
                    {
                        check(decoder, codeword, new int[]{BPTC_196_96.getIndex(column1, row1),
                                BPTC_196_96.getIndex(column2, row1), BPTC_196_96.getIndex(column1, row2),
                                BPTC_196_96.getIndex(column2, row2)}, true);
                    }
                }
            }
        }

        checkRandom(random, decoder, codeword, positions, 4, 100000, true);

        //Beyond the guaranteed correction capability, most patterns are corrected.  Error injection testing: 99.7% of 8
        //bit error patterns are corrected.
        ProductCodeDecoder validator = new ProductCodeDecoder(H15, H13, 0);

        for(int weight = 5; weight <= 8; weight++)
        {
            int corrected = 0;
            int trials = 2000;

            for(int trial = 0; trial < trials; trial++)
            {
                CorrectedBinaryMessage other = toMessage(createRows(random, H15, H13, 4), 15, 1);
                int[] errors = random.ints(0, positions.length).distinct().limit(weight).map(i -> positions[i])
                        .toArray();
                CorrectedBinaryMessage received = withErrors(other, errors);
                CorrectedBinaryMessage message = new CorrectedBinaryMessage(received);

                if(BPTC_196_96.correct(message))
                {
                    assertTrue(validator.isValid(getRows196(message)), "Corrected message is not a valid codeword");

                    if(message.equals(other))
                    {
                        corrected++;
                    }
                }
                else
                {
                    assertEquals(received, message, "Uncorrectable message was changed");
                }
            }

            assertTrue(corrected >= trials * 0.99, "Corrected " + corrected + "/" + trials + " " + weight +
                    " bit error patterns");
        }
    }

    /**
     * Row words of the de-interleaved BPTC(196,96) message.
     */
    private static int[] getRows196(CorrectedBinaryMessage message)
    {
        int[] rows = new int[13];

        for(int row = 0; row < 13; row++)
        {
            for(int column = 0; column < 15; column++)
            {
                if(message.get(BPTC_196_96.getIndex(column, row)))
                {
                    rows[row] |= 1 << column;
                }
            }
        }

        return rows;
    }

    /**
     * Test: BPTC(196,96) turbo decoding fallback corrects spread error patterns that exceed the minimum distance decoder
     * maximum: one bit error in each row, in distinct columns (13 bit errors).
     */
    @Test
    void testBPTC_196_96SpreadErrors()
    {
        Random random = new Random(6);

        for(int trial = 0; trial < 200; trial++)
        {
            CorrectedBinaryMessage codeword = toMessage(createRows(random, H15, H13, 4), 15, 1);
            int[] columns = random.ints(0, 15).distinct().limit(13).toArray();
            int[] errors = new int[13];

            for(int row = 0; row < 13; row++)
            {
                errors[row] = BPTC_196_96.getIndex(columns[row], row);
            }

            check(BPTC_196_96::correct, codeword, errors, true);
        }
    }

    /**
     * Test: BPTC(196,96) random words are either uncorrectable (unchanged) or corrected to a valid codeword.
     */
    @Test
    void testBPTC_196_96RandomWords()
    {
        Random random = new Random(2);
        ProductCodeDecoder validator = new ProductCodeDecoder(H15, H13, 0);

        for(int trial = 0; trial < 2000; trial++)
        {
            CorrectedBinaryMessage message = new CorrectedBinaryMessage(196);

            for(int i = 0; i < 196; i++)
            {
                message.set(i, random.nextBoolean());
            }

            CorrectedBinaryMessage received = new CorrectedBinaryMessage(message);

            if(BPTC_196_96.correct(message))
            {
                assertTrue(validator.isValid(getRows196(message)), "Corrected message is not a valid codeword");
            }
            else
            {
                assertEquals(received, message);
            }
        }
    }

    /**
     * Test: BPTC(128,77) has a minimum distance of 8, corrects every 1-3 bit error pattern and never miscorrects a 4 bit
     * error pattern.
     */
    @Test
    void testBPTC_128_77()
    {
        Random random = new Random(3);
        int[] columnChecks = new int[8];
        Arrays.fill(columnChecks, 1);
        BPTC_128_77 bptc = new BPTC_128_77();
        Predicate<CorrectedBinaryMessage> decoder = bptc::correct;
        int[] positions = range(0, 128);

        for(int codewordCount = 0; codewordCount < 2; codewordCount++)
        {
            CorrectedBinaryMessage codeword = toMessage(createRows(random, H16, columnChecks, 1), 16, 0);
            check(decoder, codeword, new int[0], true);

            for(int weight = 1; weight <= 3; weight++)
            {
                checkAll(decoder, codeword, positions, weight, true);
            }

            checkRandom(random, decoder, codeword, positions, 4, 100000, false);
        }
    }

    /**
     * Test: BPTC(128,77) treats a received word that is equidistant from two codewords as uncorrectable.
     */
    @Test
    void testBPTC_128_77Equidistant()
    {
        //A minimum weight (8) product codeword: a weight 4 row codeword repeated in two rows.  Received with half of
        //its bits set, the word is 4 bits from both this codeword and the all-zero codeword.
        int rowCodeword = Arrays.stream(getCodewords(H16)).filter(word -> Integer.bitCount(word) == 4).findFirst()
                .orElseThrow();
        int[] rows = new int[8];
        rows[2] = rowCodeword & -rowCodeword;
        rows[2] |= Integer.lowestOneBit(rowCodeword ^ rows[2]);
        rows[5] = rowCodeword ^ rows[2];
        CorrectedBinaryMessage received = toMessage(rows, 16, 0);
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(received);
        assertFalse(new BPTC_128_77().correct(message));
        assertEquals(received, message);
    }

    /**
     * Test: BPTC(68,36) has a minimum distance of 6, corrects every 1-2 bit error pattern and never miscorrects a 3 bit
     * error pattern.
     */
    @Test
    void testBPTC_68_36()
    {
        Random random = new Random(4);
        int[] columnChecks = new int[]{1, 1, 1, 1};
        BPTC_68_36 bptc = new BPTC_68_36();
        Predicate<CorrectedBinaryMessage> decoder = bptc::correct;
        int[] positions = range(0, 68);

        //Multiple codewords, since the previous Hamming(17,12,3) decoder failed for some codewords and not others
        for(int codewordCount = 0; codewordCount < 8; codewordCount++)
        {
            CorrectedBinaryMessage codeword = toMessage(createRows(random, H17, columnChecks, 1), 17, 0);
            check(decoder, codeword, new int[0], true);

            for(int weight = 1; weight <= 2; weight++)
            {
                checkAll(decoder, codeword, positions, weight, true);
            }

            checkAll(decoder, codeword, positions, 3, false);
        }
    }

    /**
     * Test: BPTC(68,36) row erasure fallback corrects most 5-7 bit error patterns in a single row, which exceed the
     * minimum distance decoder maximum, and never miscorrects them.  The patterns where the row decodes to a codeword
     * that is equidistant with the transmitted codeword are uncorrectable.
     */
    @Test
    void testBPTC_68_36RowErrors()
    {
        Random random = new Random(7);
        int[] columnChecks = new int[]{1, 1, 1, 1};
        BPTC_68_36 bptc = new BPTC_68_36();
        int trials = 3000;
        int corrected = 0;

        for(int trial = 0; trial < trials; trial++)
        {
            CorrectedBinaryMessage codeword = toMessage(createRows(random, H17, columnChecks, 1), 17, 0);
            int row = random.nextInt(4);
            int[] errors = random.ints(0, 17).distinct().limit(5 + trial % 3).map(column -> row * 17 + column)
                    .toArray();
            check(bptc::correct, codeword, errors, false);

            CorrectedBinaryMessage message = withErrors(codeword, errors);

            if(bptc.correct(message))
            {
                corrected++;
            }
        }

        //Error injection testing: corrects 78%
        assertTrue(corrected >= trials * 0.7, "Corrected " + corrected + "/" + trials);
    }

    /**
     * Interleaves a 32-bit single burst deinterleaved message, using the BPTC_16_2 deinterleave mapping.
     */
    private static CorrectedBinaryMessage interleave16_2(CorrectedBinaryMessage deinterleaved)
    {
        CorrectedBinaryMessage interleaved = new CorrectedBinaryMessage(32);

        for(int x = 0; x < 32; x++)
        {
            CorrectedBinaryMessage unit = new CorrectedBinaryMessage(32);
            unit.set(x);
            interleaved.set(x, deinterleaved.get(BPTC_16_2.deinterleave(unit).nextSetBit(0)));
        }

        return interleaved;
    }

    /**
     * Test: BPTC(16,2) single burst (non-reverse and reverse channel) has a minimum distance of 8, corrects every 1-3
     * bit error pattern and never miscorrects a 4 bit error pattern.
     */
    @Test
    void testBPTC_16_2()
    {
        Random random = new Random(5);
        int[] codewords = getCodewords(H16);

        for(boolean reverseChannel: new boolean[]{false, true})
        {
            for(int codewordCount = 0; codewordCount < 4; codewordCount++)
            {
                int row = codewords[random.nextInt(codewords.length)];
                CorrectedBinaryMessage codeword = toMessage(new int[]{row, reverseChannel ? row ^ 0xFFFF : row}, 16, 0);
                Predicate<CorrectedBinaryMessage> decoder = message -> {
                    CorrectedBinaryMessage interleaved = interleave16_2(message);
                    CorrectedBinaryMessage decoded = reverseChannel ? BPTC_16_2.decodeReverseChannel(interleaved) :
                            BPTC_16_2.decodeShortBurst(interleaved);

                    if(decoded == null)
                    {
                        message.setCorrectedBitCount(BPTCBase.ERRORS_NOT_CORRECTED);
                        return false;
                    }

                    message.clear();
                    message.xor(decoded);
                    message.setCorrectedBitCount(decoded.getCorrectedBitCount());
                    return true;
                };

                check(decoder, codeword, new int[0], true);

                for(int weight = 1; weight <= 3; weight++)
                {
                    checkAll(decoder, codeword, range(0, 32), weight, true);
                }

                checkAll(decoder, codeword, range(0, 32), 4, false);
            }
        }

        //Non-reverse channel decoder rejects a reverse channel message and vice versa
        int row = codewords[17];
        assertNull(BPTC_16_2.decodeShortBurst(interleave16_2(toMessage(new int[]{row, row ^ 0xFFFF}, 16, 0))));
        assertNotNull(BPTC_16_2.decodeReverseChannel(interleave16_2(toMessage(new int[]{row, row ^ 0xFFFF}, 16, 0))));
        assertNull(BPTC_16_2.decodeReverseChannel(interleave16_2(toMessage(new int[]{row, row}, 16, 0))));
    }
}
