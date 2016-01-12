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

package controller.channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import message.Message;
import module.Module;
import module.ProcessingChain;
import module.decode.DecoderFactory;
import module.decode.event.MessageActivityModel;
import module.log.EventLogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import playlist.PlaylistManager;
import record.RecorderManager;
import record.RecorderType;
import sample.Listener;
import source.Source;
import source.SourceException;
import source.SourceManager;
import audio.AudioPacket;
import audio.metadata.Metadata;
import audio.metadata.MetadataType;
import controller.ThreadPoolManager;
import controller.channel.Channel.ChannelType;
import controller.channel.ChannelEvent.Event;
import filter.FilterSet;

public class ChannelProcessingManager implements ChannelEventListener
{
	private final static Logger mLog = LoggerFactory.getLogger( ChannelProcessingManager.class );

	private Map<Integer,ProcessingChain> mProcessingChains = new HashMap<>();
	
	private List<Listener<AudioPacket>> mAudioPacketListeners = new CopyOnWriteArrayList<>();
	private List<Listener<Message>> mMessageListeners = new CopyOnWriteArrayList<>();

	private ChannelModel mChannelModel;
	private EventLogManager mEventLogManager;
	private PlaylistManager mPlaylistManager;
	private RecorderManager mRecorderManager;
	private SourceManager mSourceManager;
	private ThreadPoolManager mThreadPoolManager;
	
	public ChannelProcessingManager( ChannelModel channelModel,
									 EventLogManager eventLogManager,
									 PlaylistManager playlistManager,
									 RecorderManager recorderManager,
									 SourceManager sourceManager,
									 ThreadPoolManager threadPoolManager )
	{
		mChannelModel = channelModel;
		mEventLogManager = eventLogManager;
		mPlaylistManager = playlistManager;
		mRecorderManager = recorderManager;
		mSourceManager = sourceManager;
		mThreadPoolManager = threadPoolManager;
		
		mChannelModel.addListener( this );
	}

	/**
	 * Indicates if a processing chain is constructed for the channel and that 
	 * the processing chain is currently processing.
	 */
	public boolean isProcessing( Channel channel )
	{
		if( mProcessingChains.containsKey( channel.getChannelID() ) )
		{
			return mProcessingChains.get( channel.getChannelID() ).isProcessing();
		}
		
		return false;
	}
	
	/**
	 * Returns the current processing chain associated with the channel, or 
	 * null if a processing chain is not currently setup for the channel
	 */
	public ProcessingChain getProcessingChain( Channel channel )
	{
		return mProcessingChains.get( channel.getChannelID() );
	}

	@Override
	public void channelChanged( ChannelEvent event )
	{
		Channel channel = event.getChannel();
		
		switch( event.getEvent() )
		{
			case REQUEST_ENABLE:
				if( !mProcessingChains.containsKey( channel.getChannelID() ) ||
					!mProcessingChains.get( channel.getChannelID() ).isProcessing() )
				{
					startProcessing( channel );
				}
				break;
			case REQUEST_DISABLE:
				if( channel.getEnabled() )
				{
					switch( channel.getChannelType() )
					{
						case STANDARD:
							stopProcessing( channel, true );
							break;
						case TRAFFIC:
							//Don't remove traffic channel processing chains 
							//until explicitly deleted, so that we can reuse them
							stopProcessing( channel, false );
						default:
							break;
					}
				}
			case NOTIFICATION_DELETE:
				if( channel.getEnabled() )
				{
					stopProcessing( channel, true );
				}
				break;
			default:
				break;
		}
	}
	
	private void startProcessing( Channel channel )
	{
		ProcessingChain processingChain = mProcessingChains.get( channel.getChannelID() );

		//If we're already processing, ignore the request
		if( processingChain != null && processingChain.isProcessing() )
		{
			return;
		}
		
		//Ensure that we can get a source before we construct a new processing chain
		Source source = null;

		try
		{
			source = mSourceManager.getSource( channel.getSourceConfiguration(), 
				channel.getDecodeConfiguration().getDecoderType().getChannelBandwidth() );
		}
		catch( SourceException se )
		{
			mLog.debug( "Error obtaining source for channel [" + channel.getName() + "]", se );
		}

		if( source == null )
		{
			channel.setEnabled( false );
			
			mChannelModel.broadcast( 
				new ChannelEvent( channel, Event.NOTIFICATION_ENABLE_REJECTED ) );
			
			return;
		}

		if( processingChain == null )
		{
			processingChain = new ProcessingChain( mThreadPoolManager, 
					channel.getChannelType() );
			
			/* Register global listeners */
			for( Listener<AudioPacket> listener: mAudioPacketListeners )
			{
				processingChain.addAudioPacketListener( listener );
			}
			
			for( Listener<Message> listener: mMessageListeners )
			{
				processingChain.addMessageListener( listener );
			}

			/* Processing Modules */
			List<Module> modules = DecoderFactory.getModules( mChannelModel, 
								this, mPlaylistManager, channel );
			processingChain.addModules( modules );

			/* Setup message activity model with filtering */
			FilterSet<Message> messageFilter = DecoderFactory.getMessageFilters( modules ); 
			MessageActivityModel messageModel = new MessageActivityModel( messageFilter );
			processingChain.setMessageActivityModel( messageModel );
			
			/* Setup event logging */
			List<Module> loggers = mEventLogManager.getLoggers( 
					channel.getEventLogConfiguration(), channel.getName() );
			
			if( !loggers.isEmpty() )
			{
				processingChain.addModules( loggers );
			}

			/* Setup recorders */
			List<RecorderType> recorders = channel.getRecordConfiguration().getRecorders();

			if( !recorders.isEmpty() )
			{
				if( recorders.contains( RecorderType.AUDIO ) )
				{
					processingChain.addAudioPacketListener( mRecorderManager );
				}

				/* Add baseband recorder */
				if( ( recorders.contains( RecorderType.BASEBAND ) &&
					  channel.getChannelType() == ChannelType.STANDARD ) )
				{
					processingChain.addModule( mRecorderManager.getBasebandRecorder( 
							channel.toString() ) );
				}
				
				/* Add traffic channel baseband recorder */
				if( recorders.contains( RecorderType.TRAFFIC_BASEBAND ) &&
					channel.getChannelType() == ChannelType.TRAFFIC )
				{
					processingChain.addModule( mRecorderManager
						.getBasebandRecorder( channel.toString() ) );
				}
			}
			
			/* Inject channel metadata that will be inserted into audio packets
			 * for the recorder manager and streaming */
			processingChain.broadcast( 
					new Metadata( MetadataType.SYSTEM, channel.getSystem() ) );
			processingChain.broadcast( 
					new Metadata( MetadataType.SITE_ID, channel.getSite() ) );
			processingChain.broadcast( 
					new Metadata( MetadataType.CHANNEL_NAME, channel.getName() ) );
		}
		
		processingChain.setSource( source );
		processingChain.start();

		channel.setEnabled( true );
		
		mProcessingChains.put( channel.getChannelID(), processingChain );
		
		mChannelModel.broadcast( 
			new ChannelEvent( channel, Event.NOTIFICATION_PROCESSING_START ) );
	}
	
	private void stopProcessing( Channel channel, boolean remove )
	{
		if( mProcessingChains.containsKey( channel.getChannelID() ) )
		{
			ProcessingChain chain = mProcessingChains.get( channel.getChannelID() );
			
			chain.stop();

			mChannelModel.broadcast( 
					new ChannelEvent( channel, Event.NOTIFICATION_PROCESSING_STOP ) );
			
			if( remove )
			{
				mProcessingChains.remove( channel.getChannelID() );
				
				chain.dispose();
			}
		}
		
		channel.setEnabled( false );
	}
	
	/**
	 * Adds a message listener that will be added to all channels to receive
	 * any messages.
	 */
	public void addAudioPacketListener( Listener<AudioPacket> listener )
	{
	    mAudioPacketListeners.add( listener );
	}

	/**
	 * Removes a message listener.
	 */
	public void removeAudioPacketListener( Listener<AudioPacket> listener )
	{
	    mAudioPacketListeners.remove( listener );
	}
	
	/**
	 * Adds a message listener that will be added to all channels to receive
	 * any messages.
	 */
	public void addMessageListener( Listener<Message> listener )
	{
	    mMessageListeners.add( listener );
	}

	/**
	 * Removes a message listener.
	 */
	public void removeMessageListener( Listener<Message> listener )
	{
	    mMessageListeners.remove( listener );
	}
}
