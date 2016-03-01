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

import properties.SystemProperties;
import sample.Listener;
import alias.Alias;
import alias.AliasDirectory;
import alias.AliasEvent;
import alias.AliasList;
import alias.AliasModel;
import alias.Group;
import controller.ThreadPoolManager;
import controller.channel.Channel;
import controller.channel.Channel.ChannelType;
import controller.channel.ChannelEvent;
import controller.channel.ChannelEventListener;
import controller.channel.ChannelModel;
import controller.site.Site;
import controller.system.SystemList;

public class PlaylistManager implements ChannelEventListener, Listener<AliasEvent>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( PlaylistManager.class );

	private Playlist mPlaylist = new Playlist();
	
	private ThreadPoolManager mThreadPoolManager;
	private AliasModel mAliasModel;
	private ChannelModel mChannelModel;
	
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
							ChannelModel channelModel )
	{
		mThreadPoolManager = threadPoolManager;
		mAliasModel = aliasModel;
		mChannelModel = channelModel;

		//Register for alias and channel events so that we can save the 
		//playlist with changes
		mAliasModel.addListener( this );
		mChannelModel.addListener( this );
	}

	public Playlist getPlayist()
	{
		return mPlaylist;
	}
	
	/**
	 * Transfers data from persisted playlist into system models
	 */
	private void transferPlaylistToModels()
	{
//TODO: delete the playlist after we've loaded it, since the models will have
//all of the data

		mPlaylistLoading = true;

		mAliasModel.addAliases( mPlaylist.getAliases() );
		mChannelModel.addChannels( mPlaylist.getChannels() );
		
		mPlaylistLoading = false;
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
				props.get( "playlist.defaultfilename", "playlist.xml" );
		
		String playlistFile = 
				props.get( "playlist.currentfilename", defaultPlaylistFile );
		
		load( playlistFolder.resolve( playlistFile ) );
	}

	/**
	 * Alias event listener method.  Monitors alias events for events that
	 * indicate that the playlist has changed and queues automatic playlist
	 * saving.
	 */
	@Override
	public void receive( AliasEvent event )
	{
		if( !mPlaylistLoading )
		{
			switch( event.getEvent() )
			{
				case ADD:
				case DELETE:
				case CHANGE:
					schedulePlaylistSave();
					break;
				default:
					//When a new event enum entry is added and received, throw an
					//exception here to ensure developer adds support for the 
					//use case
					throw new IllegalArgumentException( "Unrecognized Alias "
							+ "Event [" + event.getEvent().toString() + "]" );
			}
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
//TODO: recreate the playlist and load values from each of the models
		
		mPlaylist.setAliases( mAliasModel.getAliases() );

		mPlaylist.setChannels( mChannelModel.getChannels() );
		
		JAXBContext context = null;
		
		SystemProperties props = SystemProperties.getInstance();

		Path playlistPath = props.getApplicationFolder( "playlist" );
		
		String playlistDefault = props.get( "playlist.defaultfilename", 
										 "playlist.xml" );

		String playlistCurrent = props.get( "playlist.currentfilename", 
										 playlistDefault );
		
		Path filePath = playlistPath.resolve( playlistCurrent );
		
		File outputFile = new File( filePath.toString() );

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
					+ "playlist [" + filePath.toString() + "]" );
		}
		
		OutputStream out = null;
		
		try
        {
	        out = new FileOutputStream( outputFile );
	        
			try
	        {
		        context = JAXBContext.newInstance( Playlist.class );

		        Marshaller m = context.createMarshaller();

		        m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
	        
		        m.marshal( mPlaylist, out );
	        }
	        catch ( JAXBException e )
	        {
	        	mLog.error( "jaxb exception while saving playlist: ", e );
	        }
        }
        catch ( Exception e )
        {
        	mLog.error( "coulcn't open outputstream to save playlist [" + filePath.toString() + "]", e );
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
	
	private void createBackupPlaylist()
	{
		SystemProperties props = SystemProperties.getInstance();

		Path playlistPath = props.getApplicationFolder( "playlist" );
		
		String playlistDefault = props.get( "playlist.defaultfilename", 
										 "playlist.xml" );

		String playlistCurrent = props.get( "playlist.currentfilename", 
				 playlistDefault );

		Path current = playlistPath.resolve( playlistCurrent );

		if( Files.exists( current ) )
		{
			int revision = 1;

			String playlistBackup = playlistCurrent.replace( ".xml", "_backup_" + revision + ".xml" );

			Path filePath = playlistPath.resolve( playlistBackup );

			while( Files.exists( filePath ) )
			{
				revision++;

				playlistBackup = playlistCurrent.replace( ".xml", "_backup_" + revision + ".xml" );

				filePath = playlistPath.resolve( playlistBackup );
				
				if( revision > 10 )
				{
					mLog.error( "Couldn't create playlist backup - maximum revisions exceeded" );
					return;
				}
			}
			
			File outputFile = new File( filePath.toString() );

			try
			{
				if( !outputFile.exists() )
				{
					outputFile.createNewFile();
				}
			}
			catch( Exception e )
			{
				mLog.error( "PlaylistManager - couldn't create file to save "
						+ "playlist backup [" + filePath.toString() + "]" );
			}

			try( OutputStream out = new FileOutputStream( outputFile ) )
			{
				Files.copy( current, out );
				
				mLog.info( "Playlist backed up to:" + filePath );
			}
			catch( IOException ioe )
			{
				mLog.error( "Error copying playlist to backup file [" + filePath + "]" );
			}
		}
		else
		{
			mLog.error( "Couldn't create backup playlist - original playlist does not exist" );
		}
	}
	
	/**
	 * Erases current playlist and loads playlist from the playlistPath filename,
	 * if it exists.
	 */
	public void load( Path playlistPath )
	{
		if( Files.exists( playlistPath ) )
		{
			mLog.info( "loading playlist file [" + playlistPath.toString() + "]" );
			
			JAXBContext context = null;
			
			InputStream in = null;
			
			try
	        {
		        in = new FileInputStream( playlistPath.toString() );
		        
				try
		        {
			        context = JAXBContext.newInstance( Playlist.class );

			        Unmarshaller m = context.createUnmarshaller();

			        mPlaylist = (Playlist)m.unmarshal( in );
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
		
		boolean saveRequired = false;

		if( mPlaylist == null )
		{
			mPlaylist = new Playlist();
			saveRequired = true;
		}

		//Check for and convert from legacy play list format
		if( mPlaylist.hasSystemList() || mPlaylist.hasAliasDirectory() )
		{
			convertPlaylistFormat();
			saveRequired = true;
		}
		
		transferPlaylistToModels();

		if( saveRequired )
		{
			save();
		}
	}

	/**
	 * Converts playlist data over to new format
	 */
	private void convertPlaylistFormat()
	{
		mLog.info( "Legacy playlist format detected - converting" );

		createBackupPlaylist();
		
		//Playlist version 1 to version 2 format conversion.  In order to keep
		//backwards compatibility, transfer all of the System-Site-Channel 
		//objects over to the new channel list format
		if( mPlaylist.hasSystemList() )
		{
			SystemList systemList = mPlaylist.getSystemList();
			
			for( controller.system.System system: systemList.getSystem() )
			{
				for( Site site: system.getSite() )
				{
					for( Channel channel: site.getChannel() )
					{
						channel.setSystem( system.getName() );
						channel.setSite( site.getName() );
						
						mPlaylist.getChannels().add( channel );
					}
				}
			}

			mPlaylist.getSystemList().clearSystems();
			
			mLog.info( "Converted [" + mPlaylist.getChannels().size() + 
					"] channels to new playlist format" );
		}

		if( mPlaylist.hasAliasDirectory() )
		{
			AliasDirectory aliasDirectory = mPlaylist.getAliasDirectory();
			
			for( AliasList list: aliasDirectory.getAliasList() )
			{
				for( Group group: list.getGroup() )
				{
					for( Alias alias: group.getAlias() )
					{
						alias.setList( list.getName() );
						alias.setGroup( group.getName() );
						
						mPlaylist.getAliases().add( alias );
					}
				}
			}
			
			mPlaylist.getAliasDirectory().clearAliasLists();
			
			mLog.info( "Converted [" + mPlaylist.getAliases().size() + 
					"] aliases to new playlist format" );
		}
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
	 * Saves the playlist and resets the playlist save pending flag to false
	 */
	public class PlaylistSaveTask implements Runnable
	{
		@Override
		public void run()
		{
			save();

			mPlaylistSavePending.set( false );
		}
	}
}
