/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import playlist.version1.PlaylistConverterV1ToV2;
import properties.SystemProperties;
import sample.Listener;
import alias.AliasEvent;
import alias.AliasModel;
import controller.ThreadPoolManager;
import controller.channel.Channel.ChannelType;
import controller.channel.ChannelEvent;
import controller.channel.ChannelEventListener;
import controller.channel.ChannelModel;
import controller.channel.map.ChannelMapEvent;
import controller.channel.map.ChannelMapModel;

public class PlaylistManager implements ChannelEventListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( PlaylistManager.class );

	private ThreadPoolManager mThreadPoolManager;
	private AliasModel mAliasModel;
	private ChannelModel mChannelModel;
	private ChannelMapModel mChannelMapModel;
	private Path mCurrentPlaylistPath;
	private AtomicBoolean mPlaylistSavePending = new AtomicBoolean();
	private boolean mPlaylistLoading = false;
	
	/**
	 * Playlist manager - manages all channel configurations, channel maps, and
	 * alias lists and handles loading or persisting to a playlist.xml file
	 * 
	 * Monitors playlist changes to automatically save configuration changes 
	 * after they occur.
	 * 
	 * @param threadPoolManager
	 * @param channelModel
	 */
	public PlaylistManager( ThreadPoolManager threadPoolManager,
							AliasModel aliasModel,
							ChannelModel channelModel,
							ChannelMapModel channelMapModel )
	{
		mThreadPoolManager = threadPoolManager;
		mAliasModel = aliasModel;
		mChannelModel = channelModel;
		mChannelMapModel = channelMapModel;

		//Register for alias, channel and channel map events so that we can 
		//save the playlist when there are any changes
		mChannelModel.addListener( this );
		
		mAliasModel.addListener( new Listener<AliasEvent>()
		{
			@Override
			public void receive( AliasEvent t )
			{
				schedulePlaylistSave();
			}
		} );
		
		mChannelMapModel.addListener( new Listener<ChannelMapEvent>()
		{
			@Override
			public void receive( ChannelMapEvent t )
			{
				schedulePlaylistSave();
			}
		} );
	}

	/**
	 * Loads playlist from the current playlist file, or the default playlist file,
	 * as specified in the current SDRTRunk system settings
	 */
	public void init()
	{
		SystemProperties props = SystemProperties.getInstance();
		
		Path playlistFolder = props.getApplicationFolder( "playlist" );
		
		String defaultPlaylistFile = 
				props.get( "playlist.v2.defaultfilename", "playlist_v2.xml" );
		
		String playlistFile = 
				props.get( "playlist.v2.currentfilename", defaultPlaylistFile );

		mCurrentPlaylistPath = playlistFolder.resolve( playlistFile );

		PlaylistV2 playlist = load( mCurrentPlaylistPath );

		boolean saveRequired = false;
		
		if( playlist == null )
		{
			mLog.info( "Couldn't find version 2 playlist - looking for "
					+ "version 1 playlist to convert"  );
			
			Path playlistV1Path = playlistFolder.resolve( "playlist.xml" );
			
			PlaylistConverterV1ToV2 converter = 
					new PlaylistConverterV1ToV2( playlistV1Path );
			
			if( converter.hasErrorMessages() )
			{
				mLog.error( "Playlist version 1 conversion errors: " + 
							converter.getErrorMessages() );
			}
			
			playlist = converter.getConvertedPlaylist();
			
			saveRequired = true;
		}
		
		transferPlaylistToModels( playlist );
		
		if( saveRequired )
		{
			schedulePlaylistSave();
		}
	}

	/**
	 * Transfers data from persisted playlist into system models
	 */
	private void transferPlaylistToModels( PlaylistV2 playlist )
	{
		if( playlist != null )
		{
			mPlaylistLoading = true;

			mAliasModel.addAliases( playlist.getAliases() );
			mChannelModel.addChannels( playlist.getChannels() );
			mChannelMapModel.addChannelMaps( playlist.getChannelMaps() );
			
			mPlaylistLoading = false;
		}
	}

	
	/**
	 * Channel event listener method.  Monitors channel events for events that
	 * indicate that the playlist has changed and queues automatic playlist
	 * saving.
	 */
	@Override
	public void channelChanged( ChannelEvent event )
	{
		//Only save playlist for changes to standard channels (not traffic)
		if( event.getChannel().getChannelType() == ChannelType.STANDARD )
		{
			switch( event.getEvent() )
			{
				case NOTIFICATION_ADD:
				case NOTIFICATION_CONFIGURATION_CHANGE:
				case NOTIFICATION_DELETE:
				case NOTIFICATION_PROCESSING_START:
				case NOTIFICATION_PROCESSING_STOP:
					schedulePlaylistSave();
					break;
				case NOTIFICATION_ENABLE_REJECTED:
				case NOTIFICATION_SELECTION_CHANGE:
				case NOTIFICATION_STATE_RESET:
				case REQUEST_DELETE:
				case REQUEST_DESELECT:
				case REQUEST_DISABLE:
				case REQUEST_ENABLE:
				case REQUEST_SELECT:
					//Do nothing for these event types
					break;
				default:
					//When a new event enum entry is added and received, throw an
					//exception here to ensure developer adds support for the 
					//use case
					throw new IllegalArgumentException( "Unrecognized Channel "
							+ "Event [" + event.getEvent().name() + "]" );
			}
		}
	}

	public void save()
	{
		PlaylistV2 playlist = new PlaylistV2();
		
		playlist.setAliases( mAliasModel.getAliases() );
		playlist.setChannels( mChannelModel.getChannels() );
		playlist.setChannelMaps( mChannelMapModel.getChannelMaps() );
		
		JAXBContext context = null;

		if( mCurrentPlaylistPath == null )
		{
			SystemProperties props = SystemProperties.getInstance();

			Path playlistPath = props.getApplicationFolder( "playlist" );
			
			String playlistDefault = props.get( "playlist.defaultfilename", 
											 "playlist_v2.xml" );

			String playlistCurrent = props.get( "playlist.currentfilename", 
											 playlistDefault );
			
			mCurrentPlaylistPath = playlistPath.resolve( playlistCurrent );
		}
		
		File outputFile = new File( mCurrentPlaylistPath.toString() );

		try
		{
			if( !outputFile.exists() )
			{
				outputFile.createNewFile();
			}
		}
		catch( Exception e )
		{
			mLog.error( "couldn't create file to save "
					+ "playlist [" + mCurrentPlaylistPath.toString() + "]" );
		}
		
		OutputStream out = null;
		
		try
        {
	        out = new FileOutputStream( outputFile );
	        
			try
	        {
		        context = JAXBContext.newInstance( PlaylistV2.class );

		        Marshaller m = context.createMarshaller();

		        m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
	        
		        m.marshal( playlist, out );
	        }
	        catch ( JAXBException e )
	        {
	        	mLog.error( "jaxb exception while saving playlist: ", e );
	        }
        }
        catch ( Exception e )
        {
        	mLog.error( "coulcn't open outputstream to save playlist [" + 
        			mCurrentPlaylistPath.toString() + "]", e );
        }
		finally
		{
			if( out != null )
			{
				try
                {
	                out.close();
                }
                catch ( IOException e )
                {
	                e.printStackTrace();
                }
			}
		}
	}
	
	/**
	 * Loads a version 2 playlist
	 */
	public PlaylistV2 load( Path playlistPath )
	{
		mLog.info( "loading version 2 playlist file [" + playlistPath.toString() + "]" );

		PlaylistV2 playlist = null;

		if( Files.exists( playlistPath ) )
		{
			JAXBContext context = null;
			
			InputStream in = null;
			
			try
	        {
		        in = new FileInputStream( playlistPath.toString() );
		        
				try
		        {
			        context = JAXBContext.newInstance( PlaylistV2.class );

			        Unmarshaller m = context.createUnmarshaller();

			        playlist = (PlaylistV2)m.unmarshal( in );
		        }
		        catch ( JAXBException e )
		        {
		        	mLog.error( "jaxb exception while loading playlist: ", e );
		        }
	        }
	        catch ( Exception e )
	        {
	        	mLog.error( "coulcn't open inputstream to load playlist [" + playlistPath.toString() + "]", e );
	        }
			finally
			{
				if( in != null )
				{
					try
	                {
		                in.close();
	                }
	                catch ( IOException e )
	                {
	                	mLog.error( "exception while closing " +
	                			"the playlist file inputstream reader", e );
	                }
				}
			}
		}
		else
		{
			mLog.info( "PlaylistManager - playlist not found at [" + 
							playlistPath.toString() + "]" );
		}

		return playlist;
	}

	/**
	 * Schedules a playlist save task.  Subsequent calls to this method will be 
	 * ignored until the save event occurs, thus limiting repetitive playlist 
	 * saving to a minimum.
	 */
	private void schedulePlaylistSave()
	{
		if( !mPlaylistLoading )
		{
			if( mPlaylistSavePending.compareAndSet( false, true ) )
			{
				mThreadPoolManager.scheduleOnce( new PlaylistSaveTask(), 
						2, TimeUnit.SECONDS );
			}
		}
	}

	/**
	 * Resets the playlist save pending flag to false and proceeds to save the
	 * playlist.  
	 */
	public class PlaylistSaveTask implements Runnable
	{
		@Override
		public void run()
		{
			mPlaylistSavePending.set( false );
			
			save();
		}
	}
}
