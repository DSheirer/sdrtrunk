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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import map.IconManager;
import playlist.PlaylistNode;
import source.config.SourceConfigTuner;
import source.tuner.Tuner;
import source.tuner.TunerGroupNode;
import source.tuner.TunerNode;
import source.tuner.TunerSelectionListener;
import controller.channel.Channel;
import controller.channel.ChannelListener;
import controller.channel.ChannelNode;
import controller.site.Site;
import controller.system.System;
import controller.system.SystemListNode;
import controller.system.SystemNode;
import decode.DecoderType;
import decode.config.DecodeConfigFactory;

public class ConfigurationControllerModel extends DefaultTreeModel
{
    private static final long serialVersionUID = 1L;
    
    private JTree mTree;

    private ResourceManager mResourceManager;

    private ArrayList<ChannelListener> mChannelConfigChangeListeners =
			new ArrayList<ChannelListener>();

    private ArrayList<TunerSelectionListener> mTunerSelectionListeners =
			new ArrayList<TunerSelectionListener>();

    
    private PlaylistNode mPlaylistNode;
    
    private TunerGroupNode mTunerGroupNode;
    
    private IconManager mIconManagerFrame;

	/**
	 * Implements a merged system controller and (tree) model.  The user will 
	 * primarily interact with the system nodes via the tree view, so from a 
	 * design perspective, the model and the control are necessarily merged to
	 * work well with the JTree as the system view.
	 */
	public ConfigurationControllerModel( ResourceManager resourceManager )
    {
    	super( new BaseNode( null ) );
    	
    	mResourceManager = resourceManager;

    	/**
    	 * Give the root node a reference to this model, so that all nodes can
    	 * invoke add/delete operations on the tree via context menu
    	 */
    	((BaseNode)getRoot()).setModel( this );
    }
	
	public JTree getTree()
	{
		return mTree;
	}
	
	public void showIconManager()
	{
		if( mIconManagerFrame == null )
		{
			mIconManagerFrame = new IconManager( mResourceManager, mTree );
			
			mResourceManager.getSettingsManager().addListener( mIconManagerFrame );
		}
		
		if( !mIconManagerFrame.isVisible() )
		{
			mIconManagerFrame.setVisible( true );
		}
		
		mIconManagerFrame.toFront();
	}

	/**
	 * init() gets invoked manually/externally, after all of the references 
	 * are established in the constructor
	 */
	public void init()
	{
    	/**
    	 * Add the root tuner group node
    	 */
    	mTunerGroupNode = new TunerGroupNode();
    	insertNodeInto( mTunerGroupNode, (MutableTreeNode)root, 0 );
    	addTuners( mTunerGroupNode );
    	
		/**
		 * Add the playlist node
		 */
    	mPlaylistNode = new PlaylistNode();
    	insertNodeInto( mPlaylistNode, (MutableTreeNode)root, 1 );
    	mPlaylistNode.loadPlaylist();
	}
	
	public void showNode( DefaultMutableTreeNode node )
	{
    	if( mTree != null && node != null )
    	{
    		mTree.setSelectionPath( new TreePath( node.getPath() ) );
    	}
	}
	
	public ResourceManager getResourceManager()
	{
		return mResourceManager;
	}
	
	public void setTree( JTree tree )
	{
		mTree = tree;
	}
	
	private void addTuners( TunerGroupNode parent )
	{
		List<Tuner> tuners = mResourceManager.getTunerManager().getTuners();
		
		for( Tuner tuner: tuners )
		{
			TunerNode child = new TunerNode( tuner );
			
			insertNodeInto( child, parent, parent.getChildCount() );
			
			parent.sort();

			show( child );
		}
		
	}
	
    /**
     * Convenience method to add a new default system, site and channel to 
     * the tree
     */
    public void createChannel( long frequency, DecoderType decoder )
    {
    	System system = new System();

    	Site site = new Site();
    	system.addSite( site );
    	
    	Channel channel = new Channel();
    	channel.setDecodeConfiguration( 
    				DecodeConfigFactory.getDecodeConfiguration( decoder ) );
    	SourceConfigTuner sourceConfig = new SourceConfigTuner();
    	sourceConfig.setFrequency( frequency );
    	channel.setSourceConfiguration( sourceConfig );
    	channel.setEnabled( true, true );
    	site.addChannel( channel );

    	mPlaylistNode.getPlaylist().getSystemList().addSystem( system );
    	
    	SystemNode systemNode = new SystemNode( system );
    	SystemListNode systemListNode = mPlaylistNode.getSystemListNode();
    	addNode( systemNode, systemListNode, systemListNode.getChildCount() );
    	systemNode.init();

    	systemNode.save();
    }

    public void addNode( BaseNode child, BaseNode parent, int index )
    {
    	insertNodeInto( child, parent, index );
    	show( child );
    }
    
    public void deleteNode( BaseNode node )
    {
    	node.delete();
    }
    
    private void show( BaseNode node )
    {
    	if( mTree != null )
    	{
    		mTree.scrollPathToVisible( new TreePath( node.getPath() ) );
    	}
    }

    public void addListener( TunerSelectionListener listener )
    {
    	mTunerSelectionListeners.add( listener );
    }
    
    public void removeListener( TunerSelectionListener listener )
    {
    	mTunerSelectionListeners.remove( listener );
    }

    /**
     * Notifies registered listeners that the tuner has been selected
     */
    public void fireTunerSelectedEvent( Tuner tuner )
    {
    	Iterator<TunerSelectionListener> it = 
    			mTunerSelectionListeners.iterator();
    	
    	while( it.hasNext() )
    	{
    		it.next().tunerSelected( tuner );
    	}
    }
    
    /**
     * Fires a tuner selection event for the first tuner listed in the tree.
     * This is mainly used on application startup to show the spectral display
     * of the first tuner in the spectrum panel.
     */
    public void fireFirstTunerSelected()
    {
    	if( mTunerGroupNode.getChildCount() > 0 )
    	{
    		TunerNode node = (TunerNode)mTunerGroupNode.getChildAt( 0 );

    		fireTunerSelectedEvent( node.getTuner() );
    	}
    }
    
    public ArrayList<Channel> getCurrentChannelConfigs()
    {
    	return getChannelConfigs( mPlaylistNode );
    }
    
    private ArrayList<Channel> getChannelConfigs( DefaultMutableTreeNode parent )
    {
    	ArrayList<Channel> configs = new ArrayList<Channel>();

    	Enumeration<DefaultMutableTreeNode> nodes = parent.children();
    	
    	while( nodes.hasMoreElements() )
    	{
    		DefaultMutableTreeNode node = nodes.nextElement();
    		
    		if( node instanceof ChannelNode )
    		{
    			configs.add( ((ChannelNode)node).getChannel() );
    		}

    		//Recursive call
    		configs.addAll( getChannelConfigs( node ) );
    	}
    	
    	return configs;
    }
}
