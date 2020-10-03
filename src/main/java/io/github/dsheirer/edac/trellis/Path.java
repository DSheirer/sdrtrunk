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

import java.util.ArrayList;
import java.util.List;

public class Path
{
    private List<Node> mNodes = new ArrayList<>();
    private int mError = 0;

    /**
     * Trellis Coded Modulation (TCM) path.  Represents a sequence of nodes that traverse various time instants and
     * states across a trellis.
     *
     * Note: each node can be shared across multiple paths.  This class does not assume that any nodes are unique.
     * @param startingNode to start the path.  This node normally contains an input value of zero, but is implementation
     * specific.
     */
    public Path(Node startingNode)
    {
        add(startingNode);
    }

    /**
     * Trellis Coded Modulation (TCM) path.  Represents a sequence of nodes that traverse various time instants and
     * states across a trellis.
     *
     * Note: each node can be shared across multiple paths.  This class does not assume that any nodes are unique.
     *
     * @param original path to create a copy from.  Each of the nodes in the original will be added to this path and
     * after construction both this path and the original will contain the exact same set of nodes.
     */
    private Path(Path original)
    {
        for(Node node: original.getNodes())
        {
            add(node);
        }
    }

    /**
     * Cumulative error across all nodes contained within this path.  Error value is updated as each new node is added
     * to this path.
     *
     * @return cumulative error value
     */
    public int getError()
    {
        return mError;
    }

    /**
     * Length or number of nodes contained within this path.  Note: this value includes the starting node and may
     * also include a flushing node.  The values contained in the starting and flushing nodes are normally not used
     * when decoding a path.
     *
     * @return number of nodes in this path
     */
    public int getLength()
    {
        return mNodes.size() - 1;
    }

    /**
     * The set of nodes contained in this path.
     * @return nodes
     */
    public List<Node> getNodes()
    {
        return mNodes;
    }

    /**
     * Adds the node to this path and updates the cumulative error value for this path by adding the node's error
     * value to the running error total.
     *
     * @param node to add to this path
     */
    public void add(Node node)
    {
        if(!mNodes.isEmpty())
        {
            mError += node.getError(getLastNode());
        }

        mNodes.add(node);
    }

    /**
     * Last node in this path.
     *
     * @return last node
     */
    public Node getLastNode()
    {
        if(!mNodes.isEmpty())
        {
            return mNodes.get(mNodes.size() - 1);
        }

        return null;
    }

    /**
     * Utility method to create an exact copy of this path.
     *
     * @return a new copy of this path.
     */
    public Path copyOf()
    {
        return new Path(this);
    }

    @Override
    public String toString()
    {
        return "ERR:" + getError() + " " + Joiner.on(",").join(mNodes);
    }
}
