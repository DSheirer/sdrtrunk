/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class BaseNode extends DefaultMutableTreeNode
                              implements Comparable<BaseNode>
{
    private static final long serialVersionUID = 1L;

    /**
     * Model backing the JTree and nodes.  Since we're primarily interacting
     * with the model via JTree tree nodes via context menus, injecting a 
     * reference to the model facilitates nodal access to the model's methods
     */
    protected ConfigurationControllerModel mModel = null;

    /**
     * Implements a base node that has a reference to the underlying
     * tree data model to facilitate tree node operations from nodal context 
     * menus.
     */
    public BaseNode( Object object )
    {
    	super( object );
    }

    /**
     * Override this method to return a custom icon for the node
     * @return
     */
    public String getIconPath()
    {
    	return null;
    }
    
    public String toString()
    {
        if( getUserObject() != null )
        {
            return (String)getUserObject();
        }
        else
        {
            return "SDRTrunk";
        }
    }
    
    public JPanel getEditor()
    {
        return new EmptyEditor();
    }

    /**
     * Comparator to support nodal sorting/ordering
     */
    @Override
    public int compareTo( BaseNode node )
    {
        return toString().compareTo( node.toString() );
    }
    
    /**
     * This method only needs to be called once on the root node and all nodes
     * in the tree will then have a reference to the model via getModel()
     */
    public void setModel( ConfigurationControllerModel model )
    {
    	mModel = model;
    }
    
    /**
     * Returns reference to the SystemTreeModel.  Only ConfigurableNode types
     * or children are allowed in this tree, so this method assumes that the 
     * node or parent is of ConfigurableNode class or subclass.
     * 
     * Reference to the model is required to enable node context operations
     * like adding/deleting/stopping/starting.
     */
    public ConfigurationControllerModel getModel()
    {
    	if( mModel == null )
    	{
    		TreeNode parent = getParent();
    		
    		if( parent != null )
    		{
        		return ((BaseNode)parent).getModel();
    		}
    		else
    		{
    			return null;
    		}
    	}
    	else
    	{
        	return mModel;
    	}
    }

    /**
     * Supports a context menu for any children of the BaseNode.  Override the
     * method in the child node to provide a custom context menu for that node.
     */
    public JPopupMenu getContextMenu()
    {
    	return null;
    }

    /**
     * Invokes a recursive delete() on all children to perform any cleanup 
     * operations prior to deleting.  Override this method to perform cleanup
     * operations specific to the node.  Remember to also either invoke 
     * super.delete(), or specifically call .delete() on any children of the 
     * node with the custom delete method.
     */
    public void delete()
    {
    	for( int x = 0; x < getChildCount(); x++ )
    	{
    		BaseNode node = (BaseNode)getChildAt( x );

    		//Tell the node to delete its children
    		node.delete();

    		//Delete the child node
    		getModel().deleteNode( node );
    	}
    }
    
    public void show()
    {
        getModel().showNode( this );
    }
    
    public void refresh()
    {
        getModel().nodeChanged( this );
    }

    public void sort()
    {
        List<BaseNode> nodesToSort = new ArrayList<BaseNode>();
        
        while( children().hasMoreElements() )
        {
            BaseNode child = (BaseNode)children().nextElement();
            
            nodesToSort.add( child );
            
            getModel().removeNodeFromParent( child );
        }
        
        if( !nodesToSort.isEmpty() )
        {
            Collections.sort( nodesToSort );
            
            for( BaseNode node: nodesToSort )
            {
                getModel().insertNodeInto( node, this, getChildCount() );
            }
        }
    }
}
