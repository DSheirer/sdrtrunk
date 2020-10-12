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

public class DMR_3_4_Node extends Node
{
    //Hamming error values indicating the number of bits set in each indexed value, 0-15
    public static final int[] HAMMING_ERROR_COUNT = new int[]{0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4};

    /**
     * Transmitted quad-bit value, derived from ETSI TS 102-361-1, Table B.7 Trellis encoder state transition table
     *
     * Note: the B.7 table contains constellation point ID numbers that have to be converted into actual bit values
     * where the bits represent the dibit pair for the constellation point.
     */
    public static final int[][] DMR_TRANSITION_MATRIX = new int[][]
    {
        {2,13,14,1,7,8,11,4},
        {14,1,7,8,11,4,2,13},
        {10,5,6,9,15,0,3,12},
        {6,9,15,0,3,12,10,5},
        {15,0,3,12,10,5,6,9},
        {3,12,10,5,6,9,15,0},
        {7,8,11,4,2,13,14,1},
        {11,4,2,13,14,1,7,8}
    };

    /**
     * Trellis Coded Modulation (TCM) 3/4-rate trellis node for decoding DMR
     *
     * @param inputValue for the trellis time instant
     * @param transmittedOutputValue the actual four-bit output value that was transmitted.
     */
    public DMR_3_4_Node(int inputValue, int transmittedOutputValue)
    {
        super(inputValue, transmittedOutputValue);
    }

    /**
     * Calculates the error between the preceeding node and this node as the Hamming distance between the expected
     * four-bit output value versus the actual transmitted output value.
     *
     * @param precedingNode that precedes this node
     * @return error value for this node relative to the preceeding node argument
     */
    @Override
    public int getError(Node precedingNode)
    {
        if(precedingNode != null)
        {
            int expectedOutputValue = getOutputValue(precedingNode);
            int errorMask = expectedOutputValue ^ getTransmittedOutputValue();
            return HAMMING_ERROR_COUNT[errorMask];
        }

        return 0;
    }

    /**
     * Expected output four-bit value when this node is preceeded by the argument node.
     *
     * @param precedingNode that precedes this node
     * @return expected output value
     */
    @Override
    public int getOutputValue(Node precedingNode)
    {
        return DMR_TRANSITION_MATRIX[precedingNode.getInputValue()][getInputValue()];
    }

    @Override
    public String toString()
    {
        return "IN:" + getInputValue() + " OUT:" + getTransmittedOutputValue();
    }
}
