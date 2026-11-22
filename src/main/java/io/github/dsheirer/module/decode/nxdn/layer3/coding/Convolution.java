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

        //Auto-decode leaves the message pointer at the first bit position where it encountered the error.  Explore
        //from that point to find the maximum likelihood message.
        for(int x = primaryPath.getMessagePointer(); x < messageLength; x++)
        {
            //Continue adding path clones to the collection until we have 16 total paths to cover up to
            //8 states x 2 candidates = 16 total
            if(paths.size() < 16)
            {
                List<Path> clones = new ArrayList<>();
                for(Path prototype : paths)
                {
                    Path clone = prototype.clone();
                    clones.add(clone);
                    prototype.add(false);
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
        for(Path path : paths)
        {
            path.add(false);
            path.add(false);
            path.add(false);
            path.add(false);
        }

        //Do a final score-order sort and select the lowest scoring path as the first path in the sorted collection.
        Collections.sort(paths);
        return paths.getFirst().getMessage();
    }

    /**
     * Maximum per-bit soft decision cost.  FM discriminator noise is impulsive (clicks) and an unbounded cost lets a
     * single corrupted symbol dominate the path metric.  A value of 4 is the sign bit confidence of an ideal inner
     * (+/-1) symbol.  Noise injection testing against NXDN48 and NXDN96 recordings: 3 - 4 performs best, versus
     * 2, 6, 8, 12, 16 and unclipped.
     */
    private static final float SOFT_BIT_COST_CLIP = 4.0f;

    /**
     * Soft decision Viterbi decoder (16 states) for the depunctured, deinterleaved, descrambled soft bit values.
     *
     * Soft bit values are signed confidences where a positive value favors a 1 bit, a negative value favors a 0 bit
     * and zero is an erasure (ie punctured bit).  The branch metric is the sum of the clipped soft bit magnitudes for
     * the encoded bits that disagree with the received soft bit sign.  The final 4 message bits are flushing (0) bits.
     *
     * The corrected bit count on the returned message is the count of non-punctured encoded bits where the hard
     * decision of the received soft bit disagrees with the re-encoded decoded message, consistent with hard decision
     * decoding.
     *
     * @param softBits depunctured soft bit values, two per message bit (G1, G2)
     * @param provider to identify punctured bits
     * @return decoded message, including the final 4 flushing bits, with the bit error count set.
     */
    public static CorrectedBinaryMessage decodeSoft(float[] softBits, PunctureProvider provider)
    {
        int steps = softBits.length / 2;
        int states = 16;
        float[] metrics = new float[states];
        float[] nextMetrics = new float[states];
        byte[][] survivors = new byte[steps][states]; //Previous state for each state at each step
        java.util.Arrays.fill(metrics, Float.MAX_VALUE);
        metrics[0] = 0.0f;

        for(int step = 0; step < steps; step++)
        {
            float soft1 = softBits[2 * step];
            float soft2 = softBits[2 * step + 1];
            float cost1 = Math.min(Math.abs(soft1), SOFT_BIT_COST_CLIP);
            float cost2 = Math.min(Math.abs(soft2), SOFT_BIT_COST_CLIP);
            boolean hard1 = soft1 > 0;
            boolean hard2 = soft2 > 0;
            int inputs = (step >= steps - 4) ? 1 : 2; //Final 4 bits are flushing zero bits
            java.util.Arrays.fill(nextMetrics, Float.MAX_VALUE);

            for(int state = 0; state < states; state++)
            {
                if(metrics[state] == Float.MAX_VALUE)
                {
                    continue;
                }

                for(int input = 0; input < inputs; input++)
                {
                    int register = (state << 1) | input;
                    float metric = metrics[state];

                    if(G1[register] != hard1)
                    {
                        metric += cost1;
                    }

                    if(G2[register] != hard2)
                    {
                        metric += cost2;
                    }

                    int nextState = register & 0xF;

                    if(metric < nextMetrics[nextState])
                    {
                        nextMetrics[nextState] = metric;
                        survivors[step][nextState] = (byte)state;
                    }
                }
            }

            float[] swap = metrics;
            metrics = nextMetrics;
            nextMetrics = swap;
        }

        //Trace back from the flushed (zero) state.  The input bit for each step is the LSB of the state after the step.
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(steps);
        int state = 0;

        for(int step = steps - 1; step >= 0; step--)
        {
            if((state & 1) == 1)
            {
                message.set(step);
            }

            state = survivors[step][state];
        }

        //Count the non-punctured bit errors between the received hard decisions and the re-encoded message
        int bitErrors = 0;
        int register = 0;

        for(int step = 0; step < steps; step++)
        {
            register = ((register << 1) & 0x1F) | (message.get(step) ? 1 : 0);

            if(G1[register] != (softBits[2 * step] > 0) && provider.isPreserved(2 * step))
            {
                bitErrors++;
            }

            if(G2[register] != (softBits[2 * step + 1] > 0) && provider.isPreserved(2 * step + 1))
            {
                bitErrors++;
            }
        }

        message.setCorrectedBitCount(bitErrors);
        return message;
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
}
