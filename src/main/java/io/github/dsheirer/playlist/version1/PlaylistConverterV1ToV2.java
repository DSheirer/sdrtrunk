package io.github.dsheirer.playlist.version1;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.playlist.PlaylistV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.dsheirer.alias.Alias;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PlaylistConverterV1ToV2
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( PlaylistConverterV1ToV2.class );

	private PlaylistV2 mConvertedPlaylist;
	private List<String> mErrors = new ArrayList<>();
	
    @SuppressWarnings( "deprecation" )
	public PlaylistConverterV1ToV2( Path path )
	{
		PlaylistV1 playlistVersion1 = null;
        
        if( path != null && Files.exists( path ) )
		{
			mLog.info( "attempting playlist v1 to v2 conversion [" +
							path.toString() + "]" );
			
			JAXBContext context = null;
			
			InputStream in = null;
			
			try
	        {
		        in = new FileInputStream( path.toString() );

				try
		        {
			        context = JAXBContext.newInstance( PlaylistV1.class );

			        Unmarshaller m = context.createUnmarshaller();

			        playlistVersion1 = (PlaylistV1)m.unmarshal( in );
		        }
		        catch ( JAXBException e )
		        {
		        	mErrors.add( "Couldn't unmarshall version 1 playlist - " +
		        				e.getLocalizedMessage() );
		        }
	        }
	        catch ( Exception e )
	        {
	        	mErrors.add( "Couldn't open version 1 playlist - [" +
	        			path.toString() + "] - " + e.getLocalizedMessage() );
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
			mErrors.add( "version 1 playlist is null or not found [" +
					path.toString() + "]" );
		}

		if( playlistVersion1 != null )
		{
			mConvertedPlaylist = new PlaylistV2();
			
			if( playlistVersion1.hasSystemList() )
			{
				SystemList systemList = playlistVersion1.getSystemList();
				
				for( System system: systemList.getSystem() )
				{
					for( Site site: system.getSite() )
					{
						for( Channel channel: site.getChannel() )
						{
							channel.setSystem( system.getName() );
							channel.setSite( site.getName() );
							
							mConvertedPlaylist.getChannels().add( channel );
						}
					}
				}
			}

			if( playlistVersion1.hasAliasDirectory() )
			{
				AliasDirectory aliasDirectory = playlistVersion1.getAliasDirectory();
				
				for( AliasListOld list: aliasDirectory.getAliasList() )
				{
					for( Group group: list.getGroup() )
					{
						for( Alias alias: group.getAlias() )
						{
							alias.setList( list.getName() );
							alias.setGroup( group.getName() );
							
							mConvertedPlaylist.getAliases().add( alias );
						}
					}
				}
			}
			
			if( playlistVersion1.hasChannelMapList() )
			{
				//Move the channel maps out of the channel map list
				ChannelMapList channelMapList = playlistVersion1.getChannelMapList();
				
				List<ChannelMap> channelMaps = channelMapList.getChannelMap();
				
				if( channelMaps != null && !channelMaps.isEmpty() )
				{
					mConvertedPlaylist.getChannelMaps().addAll( channelMaps );
				}
			}
			
			mLog.info( "Converted [" + mConvertedPlaylist.getChannels().size() + 
					"] channels to new playlist format" );
			mLog.info( "Converted [" + mConvertedPlaylist.getAliases().size() + 
					"] aliases to new playlist format" );
			mLog.info( "Converted [" + mConvertedPlaylist.getChannelMaps().size() + 
					"] channel maps to new playlist format" );
		}
	}
	
	public PlaylistV2 getConvertedPlaylist()
	{
		return mConvertedPlaylist;
	}
	
	public List<String> getErrorMessages()
	{
		return mErrors;
	}
	
	public boolean hasErrorMessages()
	{
		return !mErrors.isEmpty();
	}
}
