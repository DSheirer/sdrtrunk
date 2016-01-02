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
package controller.site;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.tree.DefaultMutableTreeNode;

import controller.ConfigurableNode;
import controller.channel.Channel;
import controller.channel.ChannelNode;
import controller.system.System;
import controller.system.SystemNode;

public class SiteNode extends ConfigurableNode
{
    private static final long serialVersionUID = 1L;

	public SiteNode( Site site )
	{
	    super( site );
	}
	
	public void init()
	{
		for( Channel channel: getSite().getChannel() )
		{
			ChannelNode node = new ChannelNode( channel );
			
			getModel().insertNodeInto( node, SiteNode.this, getChildCount() );
			
			node.init();
		}
		
		sort();
	}
	
    public String getIconPath()
    {
    	return "images/site.png";
    }
	
	@Override
	public Color getBackgroundColor()
	{
    	@SuppressWarnings( "unchecked" )
        Enumeration<DefaultMutableTreeNode> nodes = children();

    	while( nodes.hasMoreElements() )
    	{
    		ChannelNode child = (ChannelNode)nodes.nextElement();

    		if( child.getBackgroundColor() != null )
    		{
    			return Color.CYAN;
    		}
    	}

    	return null;
	}
	
	@Override
    public JPanel getEditor()
    {
        return new SiteEditor( this );
    }
    
	public Site getSite()
	{
		return (Site)getUserObject();
	}
	
	public String toString()
	{
		return getSite().getName();
	}
	
	public void delete()
	{
		for( int x = 0; x < getChildCount(); x++ )
		{
			((ChannelNode)getChildAt( x )).delete();
		}
		
		((SystemNode)getParent()).getSystem().removeSite( getSite() );

		save();
		
		getModel().removeNodeFromParent( SiteNode.this );
	}
	
	public JPopupMenu getContextMenu()
	{
		JPopupMenu retVal = new JPopupMenu();
		
		JMenuItem addChannelItem = new JMenuItem( "Add Channel" );
		addChannelItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
			    System system = ((SystemNode)getParent()).getSystem();

			    Channel channel = new Channel();
			    channel.setSystem( system.getName() );
			    channel.setSite( getSite().getName() );
			    
			    getSite().addChannel( channel );
			    
			    ChannelNode node = new ChannelNode( channel );

				getModel().addNode( node, 
									SiteNode.this, 
									SiteNode.this.getChildCount() );
				
				sort();
				save();
				node.show();

				/* Give channel reference to the resource manager and the 
				 * channel will register the listeners and send a channel 
				 * add event */
				channel.setResourceManager( getModel().getResourceManager() );
            }
		} );
		
		retVal.add( addChannelItem );
		
		retVal.add( new JSeparator() );

		JMenuItem deleteItem = new JMenuItem( "Delete" );
		deleteItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				delete();
            }
		} );
		
		retVal.add( deleteItem );

		return retVal;
	}
}
