/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import org.apache.commons.math3.util.FastMath;

public abstract class ViterbiDecoder
{
    private int mInputBitLength;
    private int mInputValueCount;
    private int mOutputBitLength;
    private int mOutputValueCount;

    /**
     * Viterbi decoder for trellis coded modulation (TCM) encoded binary sequences.
     *
     * @param inputBitLength for coding (e.g. 3/4 rate input bit length is 3)
     * @param outputBitLength for coding (e.g. 3/4 rate output bit length is 4)
     */
    public ViterbiDecoder(int inputBitLength, int outputBitLength)
    {
        mInputBitLength = inputBitLength;
        mOutputBitLength = outputBitLength;

        mInputValueCount = (int) FastMath.pow(2.0, mInputBitLength);
        mOutputValueCount = (int)FastMath.pow(2.0, mOutputBitLength);
    }

    /**
     * Creates a node that represents an input value and the actual/transmitted output value.
     *
     * Note: this method must be implemented by a sub-class since the node requires a specific state transition
     * table/matrix implementation.
     *
     * @param inputValue for the specific trellis node
     * @param transmittedOutputValue the actual transmitted output value
     * @return node for decoding
     */
    protected abstract Node createNode(int inputValue, int transmittedOutputValue);

    /**
     * Creates a flushing or final node that represents an actual/transmitted output value and a flushing input value.
     *
     * The final flushing input value is implementation-specific, therefore this will be implemented in any subclass
     * of this decoder.  A normal flushing input value is zero.
     *
     * @param transmittedOutputValue the actual transmitted output value
     * @return flushing node
     */
    protected abstract Node createFlushingNode(int transmittedOutputValue);

    /**
     * Creates a starting node that represents a starting state and notional transmitted output value.
     *
     * The starting input value is implementation-specific, therefore this will be implemented in any subclass
     * of this decoder.  A normal starting input value is zero.
     *
     * @return starting node
     */
    protected abstract Node createStartingNode();

    /**
     * Size in bits of the input and state values (e.g. 3/4 rate input size is 3)
     *
     * @return input bit size
     */
    public int getInputBitLength()
    {
        return mInputBitLength;
    }

    /**
     * Count of input or state values where the set of values is in the range of 0 <> count - 1
     *
     * @return input or state values count
     */
    public int getInputValueCount()
    {
        return mInputValueCount;
    }

    /**
     * Size in bits of the output constellation or transmitted symbol value (e.g. 3/4 rate output size is 4)
     *
     * @return output size in bits
     */
    public int getOutputBitLength()
    {
        return mOutputBitLength;
    }

    /**
     * Count of input or state values where the set of values is in the range of 0 <> count - 1
     *
     * @return input or state values count
     */
    public int getOutputValueCount()
    {
        return mOutputValueCount;
    }

    /**
     * Decodes the TCM encoded transmitted output values and returns a path the represents the most likely transmitted
     * sequence of nodes.
     *
     * @param transmittedOutputValues from the encoded message
     * @return most likely path representing the transmitted values
     */
    public Path decode(int[] transmittedOutputValues)
    {
        Path[] survivingPaths = new Path[getInputValueCount()];
        survivingPaths[0] = new Path(createStartingNode());

        //Add all but the last transmitted value to the starting path, creating a set of survivors at each iteration.
        for(int x = 0; x < transmittedOutputValues.length - 1; x++)
        {
            survivingPaths = add(survivingPaths, transmittedOutputValues[x]);
        }

        //Flush the survivors with the final transmitted value and return the lone surviving path
        return flush(survivingPaths, transmittedOutputValues[transmittedOutputValues.length - 1]);
    }

    /**
     * Creates a set of new nodes representing all possible input values and the transmitted output value argument
     * and iteratively adds the set of nodes to a copy of each of the surviving paths.  A set of surviving paths is
     * returned that represents each path that terminates at a distinct input/state value that has the least error
     * value.
     *
     * @param previousPaths from the previous time instant to evaluate for the next time instant in the trellis
     * using the transmitted output value.
     *
     * @param transmittedOutputValue that will be decoded for this time instant.
     *
     * @return paths that have survived (ie have the lowest error value) after adding nodes for the transmitted
     * output value
     *
     */
    public Path[] add(Path[] previousPaths, int transmittedOutputValue)
    {
        Path[] survivorPaths = new Path[getInputValueCount()];

        for(int x = 0; x < getInputValueCount(); x++)
        {
            //Create a single/unique node for each possible input value combined with the transmitted output value
            Node node = createNode(x, transmittedOutputValue);

            //Evaluate each surviving path by creating a copy and adding the node to it and see if it survives
            for(Path path: previousPaths)
            {
                if(path != null)
                {
                    Path pathToEvaluate = path.copyOf();

                    pathToEvaluate.add(node);

                    if(survivorPaths[node.getInputValue()] != null)
                    {
                        Path survivingPath = survivorPaths[node.getInputValue()];

                        //Replace the current survivor path if this evaluate path has a lower error value
                        if(pathToEvaluate.getError() < survivingPath.getError())
                        {
                            survivorPaths[node.getInputValue()] = pathToEvaluate;
                        }
                    }
                    else
                    {
                        //No survivor yet -- set this evaluate path as the first survivor
                        survivorPaths[node.getInputValue()] = pathToEvaluate;
                    }
                }
            }
        }

        return survivorPaths;
    }

    /**
     * Flushes each of the surviving paths with a final/flushing node using the transmitted symbol.  A final flushing
     * input value or state is implementation-specific, but normally uses a value of zero.  Since all paths will be
     * evaluated against each other as terminating at the same state, only the path with the lowest error value will
     * be returned from this method.
     *
     * @param survivingPaths to flush with the final flushing node
     * @param transmittedOutputValue to use for the final flushing node
     *
     * @return the single survivor path that remains after adding the flushing node to each of the candidate argument
     * survivor paths.
     */
    public Path flush(Path[] survivingPaths, int transmittedOutputValue)
    {
        Node flushingNode = createFlushingNode(transmittedOutputValue);

        Path bestPath = null;

        for(Path survivingPath: survivingPaths)
        {
            if(survivingPath != null)
            {
                survivingPath.add(flushingNode);

                if(bestPath == null || (survivingPath.getError() < bestPath.getError()))
                {
                    bestPath = survivingPath;
                }
            }
        }

        return bestPath;
    }
}
