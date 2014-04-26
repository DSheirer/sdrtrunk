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

import map.MapService;
import playlist.PlaylistManager;
import record.RecorderManager;
import settings.SettingsManager;
import source.SourceManager;
import source.tuner.TunerManager;
import controller.channel.ChannelManager;
import eventlog.EventLogManager;

/**
 * Manager for system wide resources
 */
public class ResourceManager
{
    private ChannelManager mChannelManager;
    private EventLogManager mEventLogManager;
    private PlaylistManager mPlaylistManager;
    private RecorderManager mRecorderManager;
	private SettingsManager mSettingsManager;
    private SourceManager mSourceManager;
    private TunerManager mTunerManager;
    private ConfigurationControllerModel mController;
    private MapService mMapService;
    private ThreadPoolManager mThreadPoolManager;

    public ResourceManager()
	{
    	this( new SettingsManager(),
    		  new PlaylistManager(),
    		  new EventLogManager(),
    		  new RecorderManager() );
	}
    
    public ResourceManager( SettingsManager settingsManager,
    						PlaylistManager playlistManager,
    						EventLogManager eventLogManager,
    						RecorderManager recorderManager )
    {
    	mSettingsManager = settingsManager;
    	mPlaylistManager = playlistManager;
    	mEventLogManager = eventLogManager;
    	mRecorderManager = recorderManager;

    	/** 
    	 * ChannelManager requires a reference to the ResourceManager so that
    	 * channel processing chains can access system wide resources
    	 */
    	mChannelManager = new ChannelManager( this );
    	mSourceManager = new SourceManager( this );
    	mTunerManager = new TunerManager( this );
    	mController = new ConfigurationControllerModel( this );
    	mMapService = new MapService( this );    
    	mThreadPoolManager = new ThreadPoolManager();
    }

    public ConfigurationControllerModel getController()
    {
    	return mController;
    }
    
    public ThreadPoolManager getThreadPoolManager()
    {
    	return mThreadPoolManager;
    }
    
    public void setController( ConfigurationControllerModel controller )
    {
    	mController = controller;
    }
    
    public TunerManager getTunerManager()
    {
    	return mTunerManager;
    }
    
    public MapService getMapService()
    {
    	return mMapService;
    }
    
    public SettingsManager getSettingsManager()
    {
    	return mSettingsManager;
    }
    
    public void setSettingsManager( SettingsManager manager )
    {
    	mSettingsManager = manager;
    }
    
    public PlaylistManager getPlaylistManager()
    {
    	return mPlaylistManager;
    }
    
    public void setPlaylistManager( PlaylistManager aliasManager )
    {
    	mPlaylistManager = aliasManager;
    }
    
    public EventLogManager getEventLogManager()
    {
    	return mEventLogManager;
    }
    
    public void setEventLogManager( EventLogManager eventLogManager )
    {
    	mEventLogManager = eventLogManager;
    }

    public RecorderManager getRecorderManager()
    {
    	return mRecorderManager;
    }
    
    public void setRecorderManager( RecorderManager recorderManager )
    {
    	mRecorderManager = recorderManager;
    }
    
    public SourceManager getSourceManager()
    {
    	return mSourceManager;
    }
    
    public void setSourceManager( SourceManager sourceManager )
    {
    	mSourceManager = sourceManager;
    }

    public ChannelManager getChannelManager()
    {
    	return mChannelManager;
    }
    
    public void setChannelManager( ChannelManager channelManager )
    {
    	mChannelManager = channelManager;
    }
}
