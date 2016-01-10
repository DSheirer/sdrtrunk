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
import javax.swing.JPopupMenu;

import playlist.PlaylistManager;
import controller.ConfigurableNode;

public class ChannelMapListNode extends ConfigurableNode
{
    private static final long serialVersionUID = 1L;

    public ChannelMapListNode( PlaylistManager playlistManager, ChannelMapList list )
	{
        super( playlistManager, list );
	}

    public ChannelMapList getChannelMapList()
    {
        return (ChannelMapList)getUserObject();
    }

    public void init()
    {
    	for( ChannelMap channelMap: getChannelMapList().getChannelMap() )
    	{
    		ChannelMapNode node = new ChannelMapNode( getPlaylistManager(), channelMap );
    		
    		getModel().addNode( node, ChannelMapListNode.this, getChildCount() );

    		node.init();
    	}
    	
    	sort();
    }

    public String toString()
    {
    	return "Channel Maps [" + this.getChildCount() + "]";
    }

	public JPopupMenu getContextMenu()
	{
		JPopupMenu retVal = new JPopupMenu();
		
		JMenuItem addSystemItem = new JMenuItem( "Add Channel Map" );
		addSystemItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				ChannelMap channelMap = new ChannelMap();

			    getChannelMapList().addChannelMap( channelMap );
			    
				getModel().addNode( new ChannelMapNode( getPlaylistManager(), channelMap ), 
									ChannelMapListNode.this, 
									ChannelMapListNode.this.getChildCount() );
				
				save();
            }
		} );
		
		retVal.add( addSystemItem );

		return retVal;
	}
}
