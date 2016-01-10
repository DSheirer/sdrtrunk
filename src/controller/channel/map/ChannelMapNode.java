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
package controller.channel.map;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import playlist.PlaylistManager;
import controller.ConfigurableNode;

public class ChannelMapNode extends ConfigurableNode
{
    private static final long serialVersionUID = 1L;

	public ChannelMapNode( PlaylistManager playlistManager, ChannelMap ChannelMap )
	{
		super( playlistManager, ChannelMap );
	}
	
	public void init()
	{
		//For consistency, but not used
	}
	
	@Override
    public String getIconPath()
    {
		return "images/ChannelMap.png";
    }
	
	@Override
	public JPanel getEditor()
	{
	    return new ChannelMapEditor( this );
	}
	
	public ChannelMap getChannelMap()
	{
		return (ChannelMap)getUserObject();
	}
	
	public String toString()
	{
		return getChannelMap().getName();
	}
	
	public void delete()
	{
		ChannelMapListNode parent = (ChannelMapListNode)getParent();
		
		parent.getChannelMapList().removeChannelMap( getChannelMap() );

		save();

		getModel().removeNodeFromParent( ChannelMapNode.this );

		/* Show the first child, if one exists so that we don't have a 
		 * collapsed list */
		if( parent.getChildCount() > 0 )
		{
			((ConfigurableNode)parent.getFirstChild()).show();
		}
	}
	
	public JPopupMenu getContextMenu()
	{
		JPopupMenu retVal = new JPopupMenu();
		
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
