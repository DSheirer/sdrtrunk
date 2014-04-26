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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import log.Log;
import properties.SystemProperties;

public class PlaylistManager
{
	private Playlist mPlaylist = new Playlist();
	
	public PlaylistManager()
	{
		init();
	}

	public Playlist getPlayist()
	{
		return mPlaylist;
	}

	/**
	 * Loads playlist from the current playlist file, or the default playlist file,
	 * as specified in the current SDRTRunk system settings
	 */
	private void init()
	{
		SystemProperties props = SystemProperties.getInstance();
		
		Path playlistFolder = props.getApplicationFolder( "playlist" );
		
		String defaultPlaylistFile = 
				props.get( "playlist.defaultfilename", "playlist.xml" );
		
		String playlistFile = 
				props.get( "playlist.currentfilename", defaultPlaylistFile );
		
		load( playlistFolder.resolve( playlistFile ) );
	}
	

	public void save()
	{
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
			Log.error( "PlaylistManager - couldn't create file to save playlist [" + filePath.toString() + "]" );
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
		        Log.error( "PlaylistManager - jaxb exception while saving " +
		        		"playlist: " + e.getLocalizedMessage() );
		        e.printStackTrace();
	        }
        }
        catch ( Exception e )
        {
        	Log.error( "PlaylistManager - coulcn't open outputstream to " +
        			"save playlist [" + filePath.toString() + "]" );
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
	 * Erases current playlist and loads playlist from the playlistPath filename,
	 * if it exists.
	 */
	public void load( Path playlistPath )
	{
		if( Files.exists( playlistPath ) )
		{
			Log.info( "PlaylistManager - loading playlist file [" + 
							playlistPath.toString() + "]" );
			
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
			        Log.error( "PlaylistManager - jaxb exception while loading " +
			        		"playlist: " + e.getLocalizedMessage() );
			        		
			        e.printStackTrace();
		        }
	        }
	        catch ( Exception e )
	        {
	        	e.printStackTrace();
	        	Log.error( "PlaylistManager - coulcn't open inputstream to " +
	        			"load playlist [" + playlistPath.toString() + "]" );
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
	                	Log.error( "PlaylistManager - exception while closing " +
	                			"the playlist file inputstream reader - " + 
	                			e.getLocalizedMessage() );
	                }
				}
			}
		}
		else
		{
			Log.info( "PlaylistManager - playlist does not exist [" + 
							playlistPath.toString() + "]" );
		}
		
		if( mPlaylist == null )
		{
			mPlaylist = new Playlist();
		}
	}
}
