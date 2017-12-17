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

public abstract class Node
{
    private int mInputValue;
    private int mTransmittedOutputValue;

    /**
     * Trellis Coded Modulation (TCM) node.  A node represents a time instant on the trellis and contains an input
     * value and the corresponding transmitted output value for the time instant.  A node does not maintain the
     * state at the time instant -- the state is determined by the input value of a preceeding node.
     *
     * @param inputValue for this node
     * @param transmittedOutputValue for this node
     */
    public Node(int inputValue, int transmittedOutputValue)
    {
        mInputValue = inputValue;
        mTransmittedOutputValue = transmittedOutputValue;
    }

    /**
     * Input value for this node.
     *
     * @return input value
     */
    public int getInputValue()
    {
        return mInputValue;
    }

    /**
     * Transmitted output value for this node
     * @return transmitted output value
     */
    public int getTransmittedOutputValue()
    {
        return mTransmittedOutputValue;
    }

    /**
     * Calculates the error value for this node (time instant) by determining the expected output value that would have
     * been transmitted using the state from the preceding node argument and this node's input value and comparing that
     * expected value to the actual transmitted output value.  This error is normally represented as the Hamming
     * distance between the two values.
     *
     * @param precedingNode that precedes this node
     * @return error value for this node relative to the preceeding node
     */
    public abstract int getError(Node precedingNode);

    /**
     * Expected output value when this node is preceeded by the node specified in the argument.
     *
     * @param precedingNode that precedes this node
     * @return expected output value
     */
    public abstract int getOutputValue(Node precedingNode);
}
