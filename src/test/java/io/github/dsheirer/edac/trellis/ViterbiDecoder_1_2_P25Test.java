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

package io.github.dsheirer.edac.trellis;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the P25 1/2 rate trellis soft decision Viterbi decoder.
 */
public class ViterbiDecoder_1_2_P25Test
{
    private static final int INPUT_DIBITS = 48;
    //Faded symbol spacing for the reliability weighting test (ie one third of the symbols are faded)
    private static final int SPACING = 3;
    private static final Dibit[] DIBITS = {Dibit.D00_PLUS_1, Dibit.D01_PLUS_3, Dibit.D10_MINUS_1, Dibit.D11_MINUS_3};

    /**
     * Encodes the input dibit values (plus a flushing zero) into 98 ideal soft symbols.
     */
    private static float[] encode(int[] inputs)
    {
        float[] symbols = new float[(inputs.length + 1) * 2];
        int state = 0;

        for(int step = 0; step <= inputs.length; step++)
        {
            int input = step < inputs.length ? inputs[step] : 0;
            int output = P25_1_2_Node.TRANSITION_MATRIX[state][input];
            symbols[2 * step] = DIBITS[output >> 2].getIdealPhase();
            symbols[2 * step + 1] = DIBITS[output & 3].getIdealPhase();
            state = input;
        }

        return symbols;
    }

    /**
     * Counts the decoded message bits that differ from the input dibit values.
     */
    private static int countBitErrors(int[] inputs, CorrectedBinaryMessage decoded)
    {
        int errors = 0;

        for(int step = 0; step < inputs.length; step++)
        {
            if(decoded.get(2 * step) != ((inputs[step] & 2) == 2))
            {
                errors++;
            }

            if(decoded.get(2 * step + 1) != ((inputs[step] & 1) == 1))
            {
                errors++;
            }
        }

        return errors;
    }

    private static int[] randomInputs(Random random)
    {
        int[] inputs = new int[INPUT_DIBITS];

        for(int x = 0; x < inputs.length; x++)
        {
            inputs[x] = random.nextInt(4);
        }

        return inputs;
    }

    /**
     * Test: ideal soft symbols decode without error in both the linear (C4FM) and phase wrapped (LSM) modes.
     */
    @Test
    void testIdealSymbols()
    {
        ViterbiDecoder_1_2_P25 decoder = new ViterbiDecoder_1_2_P25();
        Random random = new Random(1);

        for(int trial = 0; trial < 50; trial++)
        {
            int[] inputs = randomInputs(random);
            float[] symbols = encode(inputs);
            assertEquals(0, countBitErrors(inputs, decoder.decodeSoft(symbols)), "Linear");
            assertEquals(0, countBitErrors(inputs, decoder.decodeSoft(symbols, true)), "Phase wrapped");
        }
    }

    /**
     * Test: phase wrapped decoding measures the soft symbol error around the circle, so outer symbols that noise
     * pushed across the +/- PI phase wrap are decoded correctly.
     */
    @Test
    void testOuterSymbolsAcrossPhaseWrap()
    {
        ViterbiDecoder_1_2_P25 decoder = new ViterbiDecoder_1_2_P25();
        Random random = new Random(2);
        int linearErrors = 0;
        int wrappedErrors = 0;

        for(int trial = 0; trial < 200; trial++)
        {
            int[] inputs = randomInputs(random);
            float[] symbols = encode(inputs);
            int outerCount = 0;

            for(int x = 0; x < symbols.length; x++)
            {
                //Push every third outer (+/- 3PI/4) symbol 0.85 radians toward and across the phase wrap
                if(Math.abs(symbols[x]) > Math.PI / 2 && (outerCount++ % 3) == 0)
                {
                    double pushed = symbols[x] + Math.signum(symbols[x]) * 0.85;
                    symbols[x] = (float)(pushed - 2.0 * Math.PI * Math.round(pushed / (2.0 * Math.PI)));
                }
            }

            linearErrors += countBitErrors(inputs, decoder.decodeSoft(symbols));
            wrappedErrors += countBitErrors(inputs, decoder.decodeSoft(symbols, true));
        }

        assertEquals(0, wrappedErrors, "Phase wrapped bit errors");
        assertTrue(wrappedErrors <= linearErrors, "Phase wrapped decoding no worse than linear decoding");
    }

    /**
     * Test: reliability weighting decodes corrupted symbols that have low reliability that unweighted soft decision
     * decoding fails to decode.  The corrupted symbols are spaced across the block, the way the deinterleaver spreads
     * the symbols of a transmitted envelope fade.
     */
    @Test
    void testReliabilityWeightedFade()
    {
        ViterbiDecoder_1_2_P25 decoder = new ViterbiDecoder_1_2_P25();
        Random random = new Random(4);
        int unweightedErrors = 0;
        int weightedErrors = 0;

        for(int trial = 0; trial < 50; trial++)
        {
            int[] inputs = randomInputs(random);
            float[] symbols = encode(inputs);
            float[] reliabilities = new float[symbols.length];
            java.util.Arrays.fill(reliabilities, 1.0f);
            //Replace every SPACING symbol with a random phase (ie a deep fade) and mark it with low reliability
            for(int x = random.nextInt(SPACING); x < symbols.length; x += SPACING)
            {
                symbols[x] = (float)((random.nextDouble() * 2.0 - 1.0) * Math.PI);
                reliabilities[x] = 0.01f;
            }

            unweightedErrors += countBitErrors(inputs, decoder.decodeSoft(symbols, true));
            weightedErrors += countBitErrors(inputs, decoder.decodeSoft(symbols, reliabilities, true));
        }

        assertEquals(0, weightedErrors, "Reliability weighted bit errors");
        assertTrue(unweightedErrors > 0, "Unweighted decoding fails to decode the faded symbols");
    }

    /**
     * Test: unavailable or unusable reliabilities (null, NaN or all zero) decode the same as unweighted decoding.
     */
    @Test
    void testUnusableReliabilities()
    {
        ViterbiDecoder_1_2_P25 decoder = new ViterbiDecoder_1_2_P25();
        Random random = new Random(5);

        for(int trial = 0; trial < 20; trial++)
        {
            int[] inputs = randomInputs(random);
            float[] symbols = encode(inputs);

            for(int x = 0; x < symbols.length; x++)
            {
                symbols[x] += (float)(random.nextGaussian() * 0.5);
            }

            CorrectedBinaryMessage unweighted = decoder.decodeSoft(symbols, true);
            float[] nan = new float[symbols.length];
            java.util.Arrays.fill(nan, 1.0f);
            nan[7] = Float.NaN;
            assertEquals(unweighted, decoder.decodeSoft(symbols, null, true), "Null reliabilities");
            assertEquals(unweighted, decoder.decodeSoft(symbols, nan, true), "NaN reliability");
            assertEquals(unweighted, decoder.decodeSoft(symbols, new float[symbols.length], true), "Zero reliabilities");
        }
    }

    /**
     * Test: soft symbols that are not phase wrapped (C4FM) with outer symbols beyond +/- PI decode correctly with
     * linear error measurement.
     */
    @Test
    void testUnwrappedOuterSymbolsBeyondPi()
    {
        ViterbiDecoder_1_2_P25 decoder = new ViterbiDecoder_1_2_P25();
        Random random = new Random(3);

        for(int trial = 0; trial < 50; trial++)
        {
            int[] inputs = randomInputs(random);
            float[] symbols = encode(inputs);

            for(int x = 0; x < symbols.length; x++)
            {
                if(Math.abs(symbols[x]) > Math.PI / 2)
                {
                    symbols[x] += (float)(Math.signum(symbols[x]) * 0.85);
                }
            }

            assertEquals(0, countBitErrors(inputs, decoder.decodeSoft(symbols)), "Linear");
        }
    }
}
