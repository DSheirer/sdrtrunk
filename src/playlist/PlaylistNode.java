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
package playlist;

import javax.swing.JPanel;

import settings.SettingsManager;
import source.SourceManager;
import alias.AliasDirectory;
import alias.AliasDirectoryNode;
import controller.BaseNode;
import controller.EmptyEditor;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;
import controller.channel.map.ChannelMapList;
import controller.channel.map.ChannelMapListNode;
import controller.system.SystemList;
import controller.system.SystemListNode;

public class PlaylistNode extends BaseNode
{
    private static final long serialVersionUID = 1L;
    private AliasDirectoryNode mAliasDirectoryNode;
    private ChannelMapListNode mChannelMapListNode;
    private SystemListNode mSystemListNode;
    
	private ChannelModel mChannelModel;
	private ChannelProcessingManager mChannelProcessingManager;
	private PlaylistManager mPlaylistManager;
	private SettingsManager mSettingsManager;
	private SourceManager mSourceManager;

	public PlaylistNode( ChannelModel channelModel,
						 ChannelProcessingManager channelProcessingManager,
						 PlaylistManager playlistManager,
						 SettingsManager settingsManager,
						 SourceManager sourceManager )
	{
    	super( null );
		
		mChannelModel = channelModel;
		mChannelProcessingManager = channelProcessingManager;
		mPlaylistManager = playlistManager;
		mSettingsManager = settingsManager;
		mSourceManager = sourceManager;
	}

    private void init()
    {
    	/**
    	 * Delete any existing children
    	 */
    	delete();

    	/**
    	 * Load the AliasDirectory
    	 */
    	AliasDirectory directory = getPlaylist().getAliasDirectory();
    	
    	if( directory == null )
    	{
    		directory = new AliasDirectory();
    		getPlaylist().setAliasDirectory( directory );
    	}
    	
    	mAliasDirectoryNode = new AliasDirectoryNode( mPlaylistManager, 
    			mSettingsManager, directory );

    	getModel().insertNodeInto( mAliasDirectoryNode, this, 0 );

    	mAliasDirectoryNode.init();
    	
    	/**
    	 * Load the Channel Map List
    	 */
    	ChannelMapList channelMapList = getPlaylist().getChannelMapList();
    	
    	if( channelMapList == null )
    	{
    		channelMapList = new ChannelMapList();
    	}
    	
    	mChannelMapListNode = new ChannelMapListNode( mPlaylistManager, channelMapList );
    	
    	getModel().insertNodeInto( mChannelMapListNode, this, 1 );
    	
    	mChannelMapListNode.init();

    	/**
    	 * Load the SystemList
    	 */
    	SystemList list = getPlaylist().getSystemList();
    	
    	if( list == null )
    	{
    		list = new SystemList();
    		getPlaylist().setSystemList( list );
    	}
    	
    	mSystemListNode = new SystemListNode( list, mChannelModel, 
			mChannelProcessingManager, mPlaylistManager, mSourceManager );
    	
    	getModel().insertNodeInto( mSystemListNode, this, 2 );
    	
    	mSystemListNode.init();
    	
    	mSystemListNode.show();
    }
    
    public SystemListNode getSystemListNode()
    {
    	return mSystemListNode;
    }
    
    public AliasDirectoryNode getAliasDirectoryNode()
    {
    	return mAliasDirectoryNode;
    }
    
	public void loadPlaylist()
	{
		Playlist playlist = mPlaylistManager.getPlayist();

		setUserObject( playlist );
		
		init();
	}
	

	public Playlist getPlaylist()
    {
		Playlist retVal = null;
		
		Object userObject = getUserObject();
		
		if( userObject != null )
		{
	    	retVal = (Playlist)userObject;
		}
		
		return retVal;
    }
    
    public String toString()
    {
        return "Playlist";
    }
    
    public JPanel getEditor()
    {
        return new EmptyEditor();
    }
}
