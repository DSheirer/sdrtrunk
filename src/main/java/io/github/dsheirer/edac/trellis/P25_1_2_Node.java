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

public class P25_1_2_Node extends Node
{
    /**
     * Hamming error values indicating the number of bits set in each indexed value, 0-15.  XOR the
     * transmitted 4-bit value against the reference 4-bit value to obtain the lookup index to this array.
     */
    public static final int[] HAMMING_ERROR_COUNT = new int[]{0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4};

    /**
     * P25 Finite State Machine 1/2 Rate transition matrix.
     *
     * Note: these transmitted bit values are translated from TIA-102 BAAA Table 7-2 encoder state
     * table constellation values.
     */
    public static final int[][] TRANSITION_MATRIX = new int[][]
    {
        {2,12,1,15},//0,15,12,3
        {14,0,13,3},//4,11,8,7
        {9,7,10,4}, //13,2,1,14
        {5,11,6,8}  //9,6,5,10
    };

    /**
     * P25 1/2-rate Trellis Coded Modulation (TCM) node
     *
     * @param inputValue for the trellis time instant
     * @param transmittedOutputValue the actual four-bit output value that was transmitted.
     */
    public P25_1_2_Node(int inputValue, int transmittedOutputValue)
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
        return TRANSITION_MATRIX[precedingNode.getInputValue()][getInputValue()];
    }
}
