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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

import java.util.List;

public class ViterbiDecoder_3_4_P25 extends ViterbiDecoder
{
    /**
     * Viterbi decoder for P25 3/4 rate Trellis Coded Modulation (TCM) encoded messages.
     */
    public ViterbiDecoder_3_4_P25()
    {
        super(3, 4);
    }

    /**
     * Decodes a 3/4 rate trellis coded modulation (TCM) encoded P25 binary message containing 196 bits that have
     * already been deinterleaved.
     *
     * @param encodedMessage to decode that has already been deinterleaved.
     * @return decoded message
     */
    public CorrectedBinaryMessage decode(BinaryMessage encodedMessage)
    {
        int[] symbols = getSymbols(encodedMessage);

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
     * Extracts (49) four-bit symbols (196 bits / 4) from the deinterleaved P25 binary message
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
     * Creates a P25 node
     *
     * @param inputValue for the specific trellis node
     * @param transmittedOutputValue the actual transmitted output value
     * @return created node
     */
    @Override
    protected Node createNode(int inputValue, int transmittedOutputValue)
    {
        return new P25_3_4_Node(inputValue, transmittedOutputValue);
    }

    /**
     * Creates a P25 flushing node.  P25 uses an input value of zero to flush the final output value out of the
     * encoder.
     *
     * @param transmittedOutputValue the actual transmitted output value
     * @return
     */
    @Override
    protected Node createFlushingNode(int transmittedOutputValue)
    {
        return new P25_3_4_Node(0, transmittedOutputValue);
    }

    /**
     * Creates a P25 starting node.  P25 uses a starting input value of zero for the initial node.
     */
    @Override
    protected Node createStartingNode()
    {
        return new P25_3_4_Node(0, 0);
    }
}
