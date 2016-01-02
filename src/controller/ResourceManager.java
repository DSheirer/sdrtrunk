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
import module.log.EventLogManager;
import playlist.PlaylistManager;
import record.RecorderManager;
import settings.SettingsManager;
import source.SourceManager;
import source.recording.RecordingSourceManager;
import source.tuner.TunerManager;
import audio.AudioManager;
import controller.channel.ChannelModel;

/**
 * Manager for system wide resources
 */
public class ResourceManager
{
	private AudioManager mAudioManager;
    private EventLogManager mEventLogManager;
    private PlaylistManager mPlaylistManager;
    private RecorderManager mRecorderManager;
    private RecordingSourceManager mRecordingSourceManager;
	private SettingsManager mSettingsManager;
    private SourceManager mSourceManager;
    private TunerManager mTunerManager;
    private ConfigurationControllerModel mController;
    private MapService mMapService;
    private ThreadPoolManager mThreadPoolManager;

    public ResourceManager( PlaylistManager playlistManager,
    						ThreadPoolManager threadPoolManager,
    						ChannelModel channelModel )
	{
    	this( threadPoolManager,
    		  new SettingsManager(),
    		  playlistManager,
    		  new EventLogManager(),
    		  channelModel );
	}
    
    public ResourceManager( ThreadPoolManager threadPoolManager,
    						SettingsManager settingsManager,
    						PlaylistManager playlistManager,
    						EventLogManager eventLogManager,
    						ChannelModel channelModel )
    {
    	mThreadPoolManager = threadPoolManager;
    	mSettingsManager = settingsManager;
    	mPlaylistManager = playlistManager;
    	mEventLogManager = eventLogManager;
 
    	/** 
    	 * ChannelManager requires a reference to the ResourceManager so that
    	 * channel processing chains can access system wide resources
    	 */
    	mAudioManager = new AudioManager( mThreadPoolManager );
       	mRecorderManager = new RecorderManager( mThreadPoolManager );
    	mSourceManager = new SourceManager( this );
    	mRecordingSourceManager = new RecordingSourceManager( this );
    	mTunerManager = new TunerManager( this );
    	mController = new ConfigurationControllerModel( channelModel, this );
    	mMapService = new MapService( this );    
    }
    
    public AudioManager getAudioManager()
    {
    	return mAudioManager;
    }
    
    public void setAudioManager( AudioManager audioManager )
    {
    	mAudioManager = audioManager;
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
    
    public RecordingSourceManager getRecordingSourceManager()
    {
    	return mRecordingSourceManager;
    }
    
    public void setRecordingSourceManager( RecordingSourceManager manager )
    {
    	mRecordingSourceManager = manager;
    }
}
