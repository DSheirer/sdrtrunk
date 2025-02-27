/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.edac.bch;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the BCH decoder for APCO25 NID fragments protected by a BCH(63,16,23) code
 */
public class BCH_63_16_23_P25_Test
{
    /**
     * NAC: 1, DUID:0 (HDU), line 12 in the FDMA Table 19 NID Generator matrix.
     *
     * Octal: 00 0020 1430 2667 7236 1044
     *   Bin: 0 000 000 000 010 000 001 100 011 000 010 110 110 111 111 010 011 110 001 000 100 100
     *   Bin: 0000 0000 0001 0000 0011 0001 1000 0101 1011 0111 1110 1001 1110 0010 0010 0100
     *   Hex:    0    0    1    0    3    1    8    5    B    7    E    9    E    2    2    4
     *   Hex:    00103185B7E9E224
     */

    private static final long[] P25_NID_BCH_63_16_GENERATOR_MATRIX = new long[16];

    static
    {
        P25_NID_BCH_63_16_GENERATOR_MATRIX[0] = Long.parseLong("6331141367235452", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[1] = Long.parseLong("5265521614723276", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[2] = Long.parseLong("4603711461164164", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[3] = Long.parseLong("2301744630472072", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[4] = Long.parseLong("7271623073000466", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[5] = Long.parseLong("5605650752635660", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[6] = Long.parseLong("2702724365316730", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[7] = Long.parseLong("1341352172547354", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[8] = Long.parseLong("0560565075263566", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[9] = Long.parseLong("6141333751704220", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[10] = Long.parseLong("3060555764742110", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[11] = Long.parseLong("1430266772361044", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[12] = Long.parseLong("0614133375170422", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[13] = Long.parseLong("6037114611641642", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[14] = Long.parseLong("5326507063515373", 8);
        P25_NID_BCH_63_16_GENERATOR_MATRIX[15] = Long.parseLong("4662302756473127", 8);
    }

    private static final IntField NAC_FIELD = IntField.length12(0);
    private static final IntField DUID_FIELD = IntField.length4(12);

    /**
     * Creates a valid P25 NID 64-bit message containing the NAC and DUID values and calculated parity bits.
     * @param nac
     * @param duid
     * @return
     */
    public static CorrectedBinaryMessage create(int nac, int duid)
    {
        CorrectedBinaryMessage cbm = new CorrectedBinaryMessage(64);
        cbm.setInt(nac, NAC_FIELD);
        cbm.setInt(duid, DUID_FIELD);

        long parity = 0;
        for(int x = 0; x < 16; x++)
        {
            if(cbm.get(x))
            {
                parity ^= P25_NID_BCH_63_16_GENERATOR_MATRIX[x];
            }
        }

        cbm.load(16, 48, parity);
        return cbm;
    }

    /**
     * Verify that the decoder can correct all 1-bit errors across the message bits 0-62.  Note: this test ignores the
     * unused parity bit at index 63.
     * @param decoder that can correct one-bit errors.
     * @param original that is fully formed across indices 0-62 with BCH parity bits
     * @return true if the test is successful
     */
    public static List<Integer> testOneBitErrors(BCH decoder, CorrectedBinaryMessage original, List<Integer> errors)
    {
        //Induce an additional 1 bit error and test across each bit position.
        for(int error = 0; error < 63; error++)
        {
            if(!errors.contains(error))
            {
                CorrectedBinaryMessage localCopy = original.getSubMessage(0, 62);

                for(int additionalError: errors)
                {
                    localCopy.flip(additionalError);
                }
                localCopy.flip(error);

                decoder.decode(localCopy);
                localCopy.xor(original);

                if(localCopy.cardinality() > 0)
                {
                    errors.add(error);
                    return errors;
                }
            }
        }

        return Collections.emptyList();
    }

    public static List<Integer> testMultiBitErrors(BCH decoder, CorrectedBinaryMessage original, List<Integer> errors)
    {
        System.out.print("\rTesting: " + errors);
        if(errors.size() + 2 >= decoder.getMaxErrorCorrection())
        {
            return Collections.emptyList();
        }

        for(int error = 0; error < 63; error++)
        {
            if(!errors.contains(error))
            {
                List<Integer> errorsCopy = new ArrayList<>(errors);
                errorsCopy.add(error);
                List<Integer> result = testOneBitErrors(decoder, original, errorsCopy);

                if(!result.isEmpty())
                {
                    return result;
                }

                errorsCopy = new ArrayList<>(errors);
                errorsCopy.add(error);
                result = testMultiBitErrors(decoder, original, errorsCopy);

                if(!result.isEmpty())
                {
                    return result;
                }
            }
        }

        return Collections.emptyList();
    }

    public static void main(String[] args)
    {
        System.out.println("Starting ....");
        BCH decoder = new BCH_63_16_23_P25();
        CorrectedBinaryMessage message = create(534, 2);

        //Test correcting 1-11 bit errors
        message.clear(63); //Clear the parity bit so that it doesn't interfere with the results validation
        List<Integer> errors = new ArrayList<>();
        List<Integer> results = testMultiBitErrors(decoder, message, errors);
        System.out.println("Test Results: " + (results.isEmpty() ? "PASS" : "FAIL") + " Resulse: " + results);

        //Test 11 bit errors.
        CorrectedBinaryMessage errorMessage = message.getSubMessage(0, 63);
        int[] errorArray = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for(int error: errorArray)
        {
            errorMessage.flip(error);
        }

        decoder.decode(errorMessage);

        System.out.println(" Original: " + message);
        System.out.println("Corrected: " + errorMessage + " Corrected Bit Count: " + errorMessage.getCorrectedBitCount());
    }
}
