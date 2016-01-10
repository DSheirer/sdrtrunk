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
package controller.system;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.tree.DefaultMutableTreeNode;

import playlist.PlaylistManager;
import source.SourceManager;
import controller.ConfigurableNode;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;
import controller.site.Site;
import controller.site.SiteNode;

public class SystemNode extends ConfigurableNode
{
    private static final long serialVersionUID = 1L;

	private ChannelModel mChannelModel;
	private ChannelProcessingManager mChannelProcessingManager;
	private SourceManager mSourceManager;

	public SystemNode( System system,
					   ChannelModel channelModel,
					   ChannelProcessingManager channelProcessingManager,
					   PlaylistManager playlistManager,
					   SourceManager sourceManager )
	{
		super( playlistManager, system );
		
		mChannelModel = channelModel;
		mChannelProcessingManager = channelProcessingManager;
		mSourceManager = sourceManager;
	}
	
	public void init()
	{
		for( Site site: getSystem().getSite() )
		{
			SiteNode node = new SiteNode( site, mChannelModel, 
				mChannelProcessingManager, getPlaylistManager(), mSourceManager );
			
			getModel().insertNodeInto( node, SystemNode.this, getChildCount() );
			
			node.init();
		}
		
		sort();
	}
	
	@Override
    public String getIconPath()
    {
		return "images/system.png";
    }
	
	@Override
	public Color getBackgroundColor()
	{
    	@SuppressWarnings( "unchecked" )
        Enumeration<DefaultMutableTreeNode> nodes = children();

    	while( nodes.hasMoreElements() )
    	{
    		SiteNode child = (SiteNode)nodes.nextElement();

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
	    return new SystemEditor( this );
	}
	
	public System getSystem()
	{
		return (System)getUserObject();
	}
	
	public String toString()
	{
		return getSystem().getName();
	}
	
	public void delete()
	{
		for( int x = 0; x < getChildCount(); x++ )
		{
			((SiteNode)getChildAt( x )).delete();
		}
		
		SystemListNode parent = (SystemListNode)getParent();
		
		parent.getSystemList().removeSystem( getSystem() );

		save();

		getModel().removeNodeFromParent( SystemNode.this );

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
		
		JMenuItem addSiteItem = new JMenuItem( "Add Site" );
		addSiteItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
			    Site site = new Site();
			    getSystem().addSite( site );
			    
			    SiteNode node = new SiteNode( site, mChannelModel, 
		    		mChannelProcessingManager, getPlaylistManager(), mSourceManager );
			    
				getModel().addNode( node, 
									SystemNode.this, 
									SystemNode.this.getChildCount() );
				
				sort();
				
				node.show();
            }
		} );
		
		retVal.add( addSiteItem );
		
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
