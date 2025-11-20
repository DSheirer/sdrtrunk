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
import io.github.dsheirer.module.decode.nxdn.layer1.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * NXDN Convolutional encoder and Viterbi decoder for Rate 1/2, K=5
 */
public class Convolution
{
    private static final boolean[] G1 = {false, true, false, true, false, true, false, true, true, false, true, false,
            true, false, true, false, true, false, true, false, true, false, true, false, false, true, false, true,
            false, true, false, true};
    private static final boolean[] G2 = {false, true, true, false, true, false, false, true, false, true, true, false,
        true, false, false, true, true, false, false, true, false, true, true, false, true, false, false, true, false,
            true, true, false};

    private static final int M = 4;
    private static final int[] COST_TABLE_0 = {0, 0, 0, 0, 2, 2, 2, 2};
    private static final int[] COST_TABLE_1 = {0, 2, 2, 0, 0, 2, 2, 0};
    private static final int STATES_COUNT = 8;

    /**
     * Decodes the convolutionally encoded message and counts the true bit errors, ignoring any punctured bit errors.
     * @param encoded message to be decoded
     * @param provider to identify punctured bits
     * @return decoded message with the bit error count set.
     */
    public static CorrectedBinaryMessage decode(CorrectedBinaryMessage encoded, PunctureProvider provider)
    {
        //Create an initial path with a root state of zero and attempt to auto-decode the message
        Path primaryPath = new Path(encoded, provider);

        //Attempt to auto-decode the message.  If the message is error free, or if it only contains error in the
        //puncture bits, then there are no impactful errors and the message was decoded correctly.
        if(primaryPath.autoDecode())
        {
            return primaryPath.getMessage();
        }

        List<Path> paths = new ArrayList<>();
        paths.add(primaryPath);
        int messageLength = encoded.size() / 2 - 4; //Exclude the final 4 flushing (0) bits which we'll add separately.

        //Auto-decode leaves the message pointer at the first bit position where it encountered the error.  Explorer
        //from that point to find the maximum likelihood message.
        for(int x = primaryPath.getMessagePointer(); x < messageLength; x++)
        {
            //Continue adding path clones to the collection until we have 16 total paths to cover up to
            //8 states x 2 candidates = 16 total
            if(paths.size() < 16)
            {
                List<Path> clones = new ArrayList<>();
                for(Path experiment : paths)
                {
                    Path clone = experiment.clone();
                    clones.add(clone);
                    experiment.add(false);
                    clone.add(true);
                }
                paths.addAll(clones);
            }
            else
            {
                //Sort the paths in score order and clone the 8 best scoring paths onto the 8 worst scoring paths so
                //that we can feed the next candidate 0/1 explorations into each path pair.
                Collections.sort(paths);

                int a = 0;

                for(int y = 0; y < 8; y++)
                {
                    Path goodPath  = paths.get(y);
                    Path badPath = paths.get(y + 8);
                    badPath.cloneFrom(goodPath);

                    //Send the next candidate bits (0 or 1) to each path
                    goodPath.add(false);
                    badPath.add(true);
                }
            }
        }

        //Send the final four flushing zero bits onto each path
        for(int x = 0; x < 4; x++)
        {
            for(Path path : paths)
            {
                path.add(false);
            }
        }

        //Do a final score-order sort and select the lowest scoring path as the first path in the sorted collection.
        Collections.sort(paths);
        return paths.getFirst().getMessage();
    }

    /**
     * Encodes the message using NXDN convolutional encoder
     * @param message to be encoded
     * @return encoded message
     */
    public static CorrectedBinaryMessage encode(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage encoded = new CorrectedBinaryMessage(message.size() * 2);

        int state = 0;
        int mask = 0x1F;
        int encodedPointer = 0;

        for(int x = 0; x < message.size(); x++)
        {
            state = (mask & (state << 1)) + (message.get(x) ? 1 : 0);
            encoded.set(encodedPointer++, G1[state]);
            encoded.set(encodedPointer++, G2[state]);
        }

        return encoded;
    }

    /**
     * Utility method to generate the G1 and G2 precomputed value arrays across all 32 states.
     */
    public static void generatePolynomialValues()
    {
        int g1Mask = 0x19;   //11001 G1(D) = D4 + D3 + 1
        int g2Mask = 0x17;    //10111 = G2(D) = D4 + D2 + D1 + 1
        boolean[] g1 = new boolean[32];
        boolean[] g2 = new boolean[32];

        System.out.println("STATE: 11..1"); //G1 taps
        System.out.println("STATE: 43210");
        for(int state = 0; state < 32; state++)
        {
            g1[state] = Integer.bitCount(state & g1Mask) % 2 == 1;
            g2[state] = Integer.bitCount(state & g2Mask) % 2 == 1;
            System.out.println("STATE: " + StringUtils.leftPad(Integer.toBinaryString(state), 5, "0") + " G1: " + (g1[state] ? "1" : "0"));
        }

        System.out.println("private static final boolean[] G1 = {" + Arrays.toString(g1) + "};");
        System.out.println("private static final boolean[] G2 = {" + Arrays.toString(g2) + "};");
    }

    static void main()
    {
//        generatePolynomialValues();
        CorrectedBinaryMessage truth =     CorrectedBinaryMessage.load("001101010101001101000101101100000000001101011000010101100110001000110111");
        CorrectedBinaryMessage punctured = CorrectedBinaryMessage.load("001100010100001100000100101100000000001100011000010100100110001000110110");
        CorrectedBinaryMessage original = CorrectedBinaryMessage.load("01001100010000000001000100100011");
        boolean passes = NXDNCRC.checkSACCH(original);

        System.out.println("Passes: " +  passes);
        System.out.println("    MESSAGE: " + original);
        System.out.println("    ENCODED: " + truth);
        System.out.println("  PUNCTURED: " + punctured); //

        for(int x = 0; x < punctured.size(); x++)
        {
            punctured.flip(x);

            for(int y = x; y < punctured.size(); y++)
            {
                punctured.flip(y);

                CorrectedBinaryMessage decoded = decode(punctured, Frame.PUNCTURE_PROVIDER_SACCH);
                boolean correct = NXDNCRC.checkSACCH(decoded);

                if(!correct)
                {
                    System.out.println("          Incorrect: " + decoded + " AT X:" + x + " Y:" + y);
                }

                decoded.xor(original);

                if(decoded.cardinality() > 0)
                {
                    System.out.println("           Original: " + original);
                    System.out.println("Bit Error Positions: " + decoded + "\n");
                }

                //Undo the error that was introduced.
                punctured.flip(y);
            }

            //Undo the error that was introduced.
            punctured.flip(x);
        }

        System.out.println("Finished");

    }
}
