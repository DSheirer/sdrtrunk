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

package io.github.dsheirer.edac.galois;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.galois.bch.linux.BCHDecoder;
import io.github.dsheirer.edac.galois.bch.linux.P25BCHDecoder_63_16;
import java.util.Arrays;

public class P25BCHDecoderTest
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

    /**
     * Creates a valid P25 NID 64-bit message containing the NAC and DUID values and calculated parity bits.
     * @param nac
     * @param duid
     * @return
     */
    public static CorrectedBinaryMessage create(int nac, int duid)
    {
        CorrectedBinaryMessage cbm = new CorrectedBinaryMessage(64);
        cbm.setInt(nac, P25BCHDecoder.NAC_FIELD);
        cbm.setInt(duid, P25BCHDecoder.DUID_FIELD);

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

    public void testOneBitErrors()
    {
        P25BCHDecoder decoder = new P25BCHDecoder();

        int nac = 1;
        int duid = 1;
        CorrectedBinaryMessage baseMessage = create(nac, duid);

        boolean passes = testSequence(decoder, baseMessage);

        System.out.println("Passes: " + passes);
    }

    private boolean test(P25BCHDecoder decoder, CorrectedBinaryMessage message, int maxErrorCount, int currentError)
    {
        boolean passes = true;

        if(currentError < maxErrorCount)
        {
            for(int index = currentError; index < 63 - maxErrorCount + 2; index++)
            {
                CorrectedBinaryMessage testMessage = new CorrectedBinaryMessage(message);
                testMessage.flip(index);
                passes ^= testOneBit(decoder, testMessage, index + 1);

                if(index < (63 - maxErrorCount - currentError))
                {
                    //Recursive call
                    passes ^= test(decoder, testMessage, maxErrorCount, index + 1);
                }
            }
        }

        return passes;
    }

    private boolean testOneBit(P25BCHDecoder decoder, CorrectedBinaryMessage message, int startIndex)
    {
        boolean passes = true;

        for(int i = startIndex; i < 63; i++)
        {
            CorrectedBinaryMessage testMessage = new CorrectedBinaryMessage(message);
            testMessage.flip(i);
            decoder.decode(testMessage, -1);

            if(testMessage.getCorrectedBitCount() < 0)
            {
                passes = false;
            }
        }

        return passes;
    }

    private boolean testSequence(P25BCHDecoder decoder, CorrectedBinaryMessage message)
    {
        boolean passes = true;

        CorrectedBinaryMessage testMessage = new CorrectedBinaryMessage(message);

        int errorIndex = 0;

        while(errorIndex < 11 && passes)
        {
            for(int error = 0; error <= errorIndex; error++)
            {
                testMessage.flip(error);
            }
            decoder.decode(testMessage, -1);
            passes = testMessage.getCorrectedBitCount() >= 0;
            errorIndex++;
        }

        return passes;
    }

    public void testExample31_21()
    {
        CorrectedBinaryMessage messageOriginal = new CorrectedBinaryMessage(31);
        CorrectedBinaryMessage messageWithErrors = new CorrectedBinaryMessage(31);

//        int[] r = {0, 3, 5, 6, 8, 10, 14, 16, 17, 20, 21, 23, 24, 25};
        int[] r = {0, 3, 4, 5, 6, 8, 10, 14, 16, 17, 18, 20, 21, 23, 24, 25};

        for(int bit: r)
        {
            messageOriginal.set(bit);
        }

        for(int bit: r)
        {
            messageWithErrors.set(bit);
        }

        int[] inducedErrors = {4,18};

        for(int error: inducedErrors)
        {
            messageWithErrors.flip(error);
        }

        BCHDecoder31_21 decoder = new BCHDecoder31_21();
        System.out.println("Processing Original No-Error Message");
        System.out.println(messageOriginal);
        decoder.decode(messageOriginal);

        System.out.println("\nInducing Errors At the Following Indices: " + Arrays.toString(inducedErrors));

        System.out.println("\nProcessing Error Message");
        System.out.println(messageWithErrors);
        decoder.decode(messageWithErrors);

    }

    public void testExample15_7()
    {
        CorrectedBinaryMessage messageC = new CorrectedBinaryMessage(15);
        CorrectedBinaryMessage messageR = new CorrectedBinaryMessage(15);

//        int[] c = {0, 1, 3, 4, 5, 6, 7, 10, 11};
//        int[] r = {0, 1, 3, 4, 5, 6, 8, 10, 11}; //Errors: 7, 8

        int[] c = {0, 2, 8};
        int[] r = {0, 8};

        for(int bit: c)
        {
            messageC.set(bit);
        }
        for(int bit: r)
        {
            messageR.set(bit);
        }

        BCHDecoder15_7 decoder = new BCHDecoder15_7();
        System.out.println("Processing Original No-Error Message");
        System.out.println(messageC);
        decoder.decode(messageC);
        System.out.println("\nProcessing Error Message");
        System.out.println(messageR);
        decoder.decode(messageR);
    }

    public void testNewDecoder()
    {
        BCHDecoder decoder = new P25BCHDecoder_63_16();

        int nac = 534;
        int duid = 2;
        CorrectedBinaryMessage message = create(nac, duid);

        CorrectedBinaryMessage flipped = new CorrectedBinaryMessage(63);
        for(int x = 0; x < 63; x++)
        {
            if(message.get(x))
            {
                flipped.set(62 - x);
            }
        }

        System.out.println("Message: " + message);
        System.out.println("Flipped: " + flipped);

        flipped.flip(2);
        flipped.flip(7);

        decoder.decode(flipped);
        System.out.println("Test Complete!");
    }

    public static void main(String[] args)
    {
        P25BCHDecoderTest test = new P25BCHDecoderTest();
        test.testNewDecoder();



//        test.testExample15_7();
//        test.testExample31_21();
//        test.testOneBitErrors();

//        CorrectedBinaryMessage message = P25BCHDecoderTest.create(1, 1);
//        message.flip(0);
//
//        P25BCHDecoder decoder = new P25BCHDecoder();
//        boolean passes = test.testOneBit(decoder, message, 0);
//
//        System.out.println("PASS: " + passes);

    }
}
