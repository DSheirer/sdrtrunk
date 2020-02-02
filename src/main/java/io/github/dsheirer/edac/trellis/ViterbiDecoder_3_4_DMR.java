/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.edac.trellis;

import com.google.common.base.Joiner;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Viterbi decoder for 3/4 rate trellis coded modulation (TCM) data blocks
 */
public class ViterbiDecoder_3_4_DMR extends ViterbiDecoder
{
    private static final int[] DEINTERLEAVE_DIBITS = new int[]{0, 1, 8, 9, 16, 17, 24, 25, 32, 33, 40, 41, 48, 49, 56, 57, 64, 65, 72, 73, 80, 81,
        88, 89, 96, 97, 2, 3, 10, 11, 18, 19, 26, 27, 34, 35, 42, 43, 50, 51, 58, 59, 66, 67, 74, 75, 82, 83, 90, 91, 4, 5, 12, 13, 20, 21, 28, 29, 36, 37, 44, 45,
        52, 53, 60, 61, 68, 69, 76, 77, 84, 85, 92, 93, 6, 7, 14, 15, 22, 23, 30, 31, 38, 39, 46, 47, 54, 55, 62, 63, 70, 71, 78, 79, 86, 87, 94, 95};

    private static final int[] DEINTERLEAVE_INDEXES = new int[]{0, 1, 2, 3, 16, 17, 18, 19, 32, 33, 34, 35, 48, 49, 50,
        51, 64, 65, 66, 67, 80, 81, 82, 83, 96, 97, 98, 99, 112, 113, 114, 115, 128, 129, 130, 131, 144, 145, 146, 147,
        160, 161, 162, 163, 176, 177, 178, 179, 192, 193, 194, 195, 4, 5, 6, 7, 20, 21, 22, 23, 36, 37, 38, 39, 52, 53,
        54, 55, 68, 69, 70, 71, 84, 85, 86, 87, 100, 101, 102, 103, 116, 117, 118, 119, 132, 133, 134, 135, 148, 149,
        150, 151, 164, 165, 166, 167, 180, 181, 182, 183, 8, 9, 10, 11, 24, 25, 26, 27, 40, 41, 42, 43, 56, 57, 58, 59,
        72, 73, 74, 75, 88, 89, 90, 91, 104, 105, 106, 107, 120, 121, 122, 123, 136, 137, 138, 139, 152, 153, 154, 155,
        168, 169, 170, 171, 184, 185, 186, 187, 12, 13, 14, 15, 28, 29, 30, 31, 44, 45, 46, 47, 60, 61, 62, 63, 76, 77,
        78, 79, 92, 93, 94, 95, 108, 109, 110, 111, 124, 125, 126, 127, 140, 141, 142, 143, 156, 157, 158, 159, 172,
        173, 174, 175, 188, 189, 190, 191};

    /**
     * Viterbi decoder for Digital Mobile Radio (DMR) 3/4 rate Trellis Coded Modulation (TCM) encoded messages.
     */
    public ViterbiDecoder_3_4_DMR()
    {
        super(3, 4);
    }

    public static void main(String[] args)
    {
        List<Integer> indexes = new ArrayList<>();
        for(int i : DEINTERLEAVE_DIBITS)
        {
            indexes.add(i * 2);
            indexes.add(i * 2 + 1);
        }

        System.out.println(Joiner.on(",").join(indexes));

        Collections.sort(indexes);
        System.out.println(Joiner.on(",").join(indexes));
    }

    /**
     * Performs deinterleave of transmitted 3/4 TCM encoded payload
     * @param interleaved
     * @return
     */
    private static CorrectedBinaryMessage deinterleave(CorrectedBinaryMessage interleaved)
    {
        CorrectedBinaryMessage deinterleaved = new CorrectedBinaryMessage(196);

        for(int x = 0; x < 196; x++)
        {
            if(interleaved.get(x))
            {
                deinterleaved.set(DEINTERLEAVE_INDEXES[x]);
            }
        }

        return deinterleaved;
    }


    /**
     * Decodes a 3/4 rate trellis coded modulation (TCM) encoded DMR binary message containing 196 bits that have
     * already been deinterleaved.
     *
     * @param encodedMessage to decode that has already been deinterleaved.
     * @return decoded message
     */
    public CorrectedBinaryMessage decode(CorrectedBinaryMessage encodedMessage)
    {
        CorrectedBinaryMessage deinterleaved = deinterleave(encodedMessage);
        int[] symbols = getSymbols(deinterleaved);
        Path mostLikelyPath = decode(symbols);
        return getMessage(mostLikelyPath);
    }

    /**
     * Extracts the decoded/corrected message from the surviving path argument.
     *
     * @param path to extract a message from
     * @return corrected binary message.
     */
    private static CorrectedBinaryMessage getMessage(Path path)
    {
        List<Node> mNodes = path.getNodes();

        //Each node contains an input value excluding the starting(0) and final flushing(0) nodes.
        CorrectedBinaryMessage message = new CorrectedBinaryMessage((mNodes.size() - 2) * 3);

        if(mNodes.size() > 2)
        {
            for(int x = 1; x < mNodes.size() - 1; x++)
            {
                int inputValue = mNodes.get(x).getInputValue();

                int messageOffset = 3 * (x - 1);

                if((inputValue & 4) == 4)
                {
                    message.set(messageOffset);
                }

                if((inputValue & 2) == 2)
                {
                    message.set(messageOffset + 1);
                }

                if((inputValue & 1) == 1)
                {
                    message.set(messageOffset + 2);
                }
            }
        }

        //Transfer the corrected error count to the message
        message.setCorrectedBitCount(path.getError());

        return message;
    }

    /**
     * Extracts (49) four-bit symbols (196 bits / 4) from the deinterleaved DMR binary message
     *
     * @param encodedMessage that deinterleaved and contains 196-bits of 3/4 TCM encoding
     * @return symbols
     */
    public int[] getSymbols(BinaryMessage encodedMessage)
    {
        //Ensure we have an integral number of transmitted symbols (nibbles) in the message
        if(encodedMessage.size() % getOutputBitLength() != 0)
        {
            throw new IllegalArgumentException("Encoded message must contain an integral number of 4-bit symbols -" +
                " message size: " + encodedMessage.size());
        }

        int[] symbols = new int[encodedMessage.size() / getOutputBitLength()];

        int index = 0;
        int pointer = 0;

        while(index < encodedMessage.size())
        {
            int symbol = encodedMessage.getInt(index, index + (getOutputBitLength() - 1));
            symbols[pointer++] = symbol;
            index += getOutputBitLength();
        }

        return symbols;
    }

    /**
     * Creates a DMR node
     *
     * @param inputValue for the specific trellis node
     * @param transmittedOutputValue the actual transmitted output value
     * @return created node
     */
    @Override
    protected Node createNode(int inputValue, int transmittedOutputValue)
    {
        return new DMR_3_4_Node(inputValue, transmittedOutputValue);
    }

    /**
     * Creates a DMR flushing node.  DMR uses an input value of zero to flush the final output value out of the
     * encoder.
     *
     * @param transmittedOutputValue the actual transmitted output value
     * @return
     */
    @Override
    protected Node createFlushingNode(int transmittedOutputValue)
    {
        return new DMR_3_4_Node(0, transmittedOutputValue);
    }

    /**
     * Creates a DMR starting node.  DMR uses a starting input value of zero for the initial node.
     */
    @Override
    protected Node createStartingNode()
    {
        return new DMR_3_4_Node(0, 0);
    }
}
