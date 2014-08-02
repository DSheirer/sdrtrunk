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
package controller.channel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import log.Log;
import message.Message;
import record.config.RecordConfiguration;
import sample.Listener;
import source.SourceType;
import source.config.SourceConfigFactory;
import source.config.SourceConfigRecording;
import source.config.SourceConfigTuner;
import source.config.SourceConfiguration;
import source.tuner.TunerChannel;
import source.tuner.TunerChannel.Type;
import controller.ResourceManager;
import controller.activity.ActivitySummaryFrame;
import controller.config.Configuration;
import controller.site.Site;
import controller.state.ChannelState;
import controller.state.ChannelState.State;
import controller.system.System;
import decode.config.AuxDecodeConfiguration;
import decode.config.DecodeConfigFactory;
import decode.config.DecodeConfiguration;
import eventlog.config.EventLogConfiguration;

@XmlSeeAlso( { Configuration.class } )
@XmlRootElement( name = "channel" )
public class Channel extends Configuration
{
	public enum ChannelType 
	{ 
		STANDARD,
		TRAFFIC 
	};
	
	private static final boolean ENABLED = true;
	private static final boolean DISABLED = false;
	private static final boolean BROADCAST_CHANGE = true;
	
	private CopyOnWriteArrayList<ChannelListener> mChannelListeners =
				new CopyOnWriteArrayList<ChannelListener>();

	private CopyOnWriteArrayList<Listener<Message>> mMessageListeners =
			new CopyOnWriteArrayList<Listener<Message>>();
	
	private HashMap<Integer,Channel> mTrafficChannels = 
						new HashMap<Integer,Channel>();
	
	private DecodeConfiguration mDecodeConfiguration = 
				DecodeConfigFactory.getDefaultDecodeConfiguration();
	private AuxDecodeConfiguration mAuxDecodeConfiguration =
				new AuxDecodeConfiguration();
	private SourceConfiguration mSourceConfiguration = 
				SourceConfigFactory.getDefaultSourceConfiguration();
	private EventLogConfiguration mEventLogConfiguration =
			    new EventLogConfiguration();
	private RecordConfiguration mRecordConfiguration = new RecordConfiguration();

	private ResourceManager mResourceManager;
	private String mAliasListName;
	private System mSystem;
    private Site mSite;
    private String mName;
	private boolean mEnabled;
	private boolean mSelected;
	private ChannelType mChannelType;
	private ProcessingChain mProcessingChain;

	/**
	 * Constructs a new standard channel with a default name of "New Channel"
	 */
	public Channel()
	{
	    this( "New Channel", ChannelType.STANDARD );
	}
	
	/**
	 * Constructs a new standard channel with the specified channel name
	 */
	public Channel( String channelName )
	{
		this( channelName, ChannelType.STANDARD );
	}

	/**
	 * Constructs a new channel with the specified channel name and type.
	 * 
	 * @param channelName
	 * @param channelType
	 */
	public Channel( String channelName, ChannelType channelType )
	{
		mName = channelName;
		mChannelType = channelType;
	}
	
	public ChannelType getChannelType()
	{
		return mChannelType;
	}

	/**
	 * Sets the resource manager responsible for this channel and registers
	 * the channel manager as a listener to this channel.  Fires channel add
	 * channel event to the newly added channel manager channel listener.
	 */
	public void setResourceManager( ResourceManager resourceManager )
	{
		mResourceManager = resourceManager;

		/* Add system-wide channel listeners onto this channel */
		addListeners( mResourceManager.getChannelManager().getChannelListeners() );

		/* If we're enabled, fire enable changed event to get processing started */
		if( mEnabled )
		{
			enableChanged();
		}
	}
	
	@XmlTransient
	public ResourceManager getResourceManager()
	{
		return mResourceManager;
	}

	/**
	 * Returns the processing chain for this channel.  If the processing chain
	 * is not currently constructed and running, this method will return null.
	 */
	@XmlTransient
	public ProcessingChain getProcessingChain()
	{
		return mProcessingChain;
	}
	
	/**
	 * Indicates if this channel has a processing chain constructed and it
	 * is processing.
	 */
	public boolean isProcessing()
	{
		return mProcessingChain != null && mProcessingChain.isProcessing();
	}

	/**
	 * Indicates if this channel has been selected for audio output
	 */
	@XmlTransient
	public boolean getSelected()
	{
		return mSelected;
	}
	
	public void setSelected( boolean selected )
	{
		mSelected = selected;
		
		if( mProcessingChain != null && mProcessingChain.isProcessing() )
		{
			mProcessingChain.getAudioOutput().setAudioPlaybackEnabled( selected );
		}
		
		fireChannelEvent( ChannelEvent.CHANGE_SELECTED );
	}
	
	public JMenu getContextMenu()
	{
		JMenu menu = new JMenu( mSite + "-" + mName );
		
		if( mEnabled )
		{
			JMenuItem disable = new JMenuItem( "Disable" );
			disable.addActionListener( new ActionListener() 
			{
				@Override
                public void actionPerformed( ActionEvent e )
                {
					setEnabled( DISABLED, BROADCAST_CHANGE );
					getResourceManager().getPlaylistManager().save();					
                }
			} );
			
			menu.add( disable );
			
			menu.add( new JSeparator() );

			JMenuItem actySummaryItem = 
					new JMenuItem( "Activity Summary" );

			actySummaryItem.addActionListener( new ActionListener() 
			{
				@Override
	            public void actionPerformed( ActionEvent e )
	            {
					if( mResourceManager != null )
					{
						ProcessingChain chain = getProcessingChain();
						
						if( chain != null )
						{
							ChannelState state = chain.getChannelState();
									
							if( state != null )
							{
								String summary = state.getActivitySummary();
								
								new ActivitySummaryFrame( summary,
									mResourceManager.getController().getTree() );
							}
						}
					}
	            }
			} );
				
			menu.add( actySummaryItem );
		}
		else
		{
			JMenuItem enable = new JMenuItem( "Enable" );
			enable.addActionListener( new ActionListener() 
			{
				@Override
                public void actionPerformed( ActionEvent e )
                {
					setEnabled( ENABLED, BROADCAST_CHANGE );
					getResourceManager().getPlaylistManager().save();                }
			} );
			
			menu.add( enable );
		}
		
		menu.add( new JSeparator() );
		
		JMenuItem deleteItem = new JMenuItem( "Delete" );
		deleteItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				dispose();
				getResourceManager().getPlaylistManager().save();            }
		} );
		
		menu.add( deleteItem );
		
		
		return menu;
	}
	
	/**
	 * Orderly shutdown method when this channel is going to be deleted.
	 */
	public void dispose()
	{
		setEnabled( DISABLED, BROADCAST_CHANGE );

		/* Broadcast channel deleted event */
		fireChannelEvent( ChannelEvent.CHANNEL_DELETED );
	}
	
	/**
	 * Returns a full channel name containing the system, site and channel name.
	 */
	public String getChannelDisplayName()
	{
		StringBuilder sb = new StringBuilder();
		
		if( mSystem != null )
		{
			sb.append( mSystem.getName() );
		}
		else
		{
			sb.append( "-" );
		}
		
		sb.append( "/" );
		
		if( mSite != null )
		{
			sb.append( mSite.getName() );
		}
		else
		{
			sb.append( "-" );
		}
		
		sb.append( "/" );

		sb.append( mName );
		
		return sb.toString();
	}
	
	/**
	 * Returns the tuner channel requirement for this channel when the source
	 * configuration specifies a tuner and frequency as the source for this
	 * channel.
	 * 
	 * If the source configuration is anything else, this method returns null.
	 */
    public TunerChannel getTunerChannel()
    {
        TunerChannel retVal = null;
        
        if( mSourceConfiguration.getSourceType() == SourceType.TUNER )
        {
            SourceConfigTuner config = (SourceConfigTuner)mSourceConfiguration;

            retVal = new TunerChannel( Type.LOCKED, config.getFrequency(), 
                    mDecodeConfiguration.getDecoderType().getChannelBandwidth() );
        }
        else if( mSourceConfiguration.getSourceType() == SourceType.RECORDING )
        {
            SourceConfigRecording config = 
            		(SourceConfigRecording)mSourceConfiguration;

            retVal = new TunerChannel( Type.LOCKED, config.getFrequency(), 
                mDecodeConfiguration.getDecoderType().getChannelBandwidth() );
        }
        
        return retVal;
    }

    /**
     * Returns the owning system for this channel.
     */
	public System getSystem()
	{
	    return mSystem;
	}

	/**
	 * Sets the owning system for this channel and optionally broadcasts a
	 * system change channel event.
	 */
	public void setSystem( System system, boolean fireChangeEvent )
	{
	    mSystem = system;
	    
	    if( fireChangeEvent )
	    {
	        fireChannelEvent( ChannelEvent.CHANGE_SYSTEM );
	    }
	}

	/**
	 * Returns the owning site for this channel.
	 */
	public Site getSite()
	{
	    return mSite;
	}

	/**
	 * Sets the owning site for this channel and optionally broadcasts a site
	 * change channel event.
	 */
    public void setSite( Site site, boolean fireChangeEvent )
    {
        mSite = site;
        
        if( fireChangeEvent )
        {
            fireChannelEvent( ChannelEvent.CHANGE_SITE );
        }
    }

    /**
     * Default display string for this channel -- the channel name.
     */
	public String toString()
	{
		return mName;
	}

	/**
	 * Indicates if this channel is enabled for processing.  
	 */
	@XmlAttribute
	public boolean getEnabled()
	{
		return mEnabled;
	}

	/**
	 * Enables/disables this channel for processing.  Automatically starts or
	 * stops channel processing in response to this change.
	 */
	public void setEnabled( boolean enabled )
	{
		mEnabled = enabled;
		
		enableChanged();
	}
	
	/**
	 * Enables/disables this channel for processing.  Automatically starts or
	 * stops channel processing in response to this change.  Optionally
	 * broadcasts a enabled change channel event.
	 */
	public void setEnabled( boolean enabled, boolean fireChannelEvent )
	{
		setEnabled( enabled );
		
		if( fireChannelEvent )
		{
			fireChannelEvent( ChannelEvent.CHANGE_ENABLED );
		}
	}

	/**
	 * Returns the alias list that is used by this channel for looking up alias
	 * values for the various identifiers produced by the decoder.
	 */
	@XmlElement( name = "alias_list_name" )
	public String getAliasListName()
	{
		return mAliasListName;
	}

	/**
	 * Sets the alias list to be used for looking up the alias values for the 
	 * various identifiers produced by the decoder
	 */
	public void setAliasListName( String name )
	{
		mAliasListName = name;
	}
	
	/**
	 * Sets the alias list to be used for looking up the alias values for the 
	 * various identifiers produced by the decoder.  This method is the same
	 * as setAliasName(name) and optionally broadcasts an alias list change
	 * channel event.
	 */
	public void setAliasListName( String name, boolean fireChangeEvent )
	{
		mAliasListName = name;
		
		if( fireChangeEvent )
		{
			fireChannelEvent( ChannelEvent.CHANGE_ALIAS_LIST );
		}
	}

	/**
	 * Gets the decoder configuration used by this channel
	 */
	@XmlElement( name = "decode_configuration" )
	public DecodeConfiguration getDecodeConfiguration()
	{
		return mDecodeConfiguration;
	}

	/**
	 * Sets the decoder configuration used by this channel
	 */
	public void setDecodeConfiguration( DecodeConfiguration config )
	{
		mDecodeConfiguration = config;

		if( mProcessingChain != null && mProcessingChain.isRunning() )
		{
			mProcessingChain.updateDecoder();
		}
	}

	/**
	 * Gets the aux decoder configuration used by this channel
	 */
	@XmlElement( name = "aux_decode_configuration" )
	public AuxDecodeConfiguration getAuxDecodeConfiguration()
	{
		return mAuxDecodeConfiguration;
	}

	/**
	 * Sets the decoder configuration used by this channel
	 */
	public void setAuxDecodeConfiguration( AuxDecodeConfiguration config )
	{
		mAuxDecodeConfiguration = config;

		if( mProcessingChain != null && mProcessingChain.isRunning() )
		{
			mProcessingChain.updateDecoder();
		}
	}

	/**
	 * Sets the decoder configuration used by this channel and optionally
	 * broadcasts a decoder change channel event.
	 */
	public void setDecodeConfiguration( DecodeConfiguration config, 
										boolean fireChangeEvent )
	{
		setDecodeConfiguration( config );
		
		if( fireChangeEvent )
		{
			fireChannelEvent( ChannelEvent.CHANGE_DECODER );
		}
	}

	/**
	 * Returns the source configuration for this channel.
	 */
	@XmlElement( name = "source_configuration" )
	public SourceConfiguration getSourceConfiguration()
	{
		return mSourceConfiguration;
	}

	/**
	 * Sets the source configuration for this channel.
	 */
	public void setSourceConfiguration( SourceConfiguration config )
	{
		mSourceConfiguration = config;
		
		if( mProcessingChain != null && mProcessingChain.isRunning() )
		{
			mProcessingChain.updateSource();
		}
	}

	/**
	 * Sets the source configuration for this channel and optionally broadcasts
	 * a source change channel event.
	 */
	public void setSourceConfiguration( SourceConfiguration config, 
										boolean fireChangeEvent )
	{
		setSourceConfiguration( config );
		
		if( fireChangeEvent )
		{
			fireChannelEvent( ChannelEvent.CHANGE_SOURCE );
		}
	}
	
	/**
	 * Returns the event logger configuration for this channel.
	 */
	@XmlElement( name = "event_log_configuration" )
	public EventLogConfiguration getEventLogConfiguration()
	{
		return mEventLogConfiguration;
	}

	/**
	 * Sets the event logger configuration for this channel.
	 */
	public void setEventLogConfiguration( EventLogConfiguration config )
	{
		mEventLogConfiguration = config;

		if( mProcessingChain != null && mProcessingChain.isRunning() )
		{
			mProcessingChain.updateEventLogging();
		}
	}

	/**
	 * Sets the event logger configuration for this channel and optionally
	 * broadcasts an event logger change channel event.
	 */
	public void setEventLogConfiguration( EventLogConfiguration config, 
										  boolean fireChangeEvent )
	{
		setEventLogConfiguration( config );
		
		if( fireChangeEvent )
		{
			fireChannelEvent( ChannelEvent.CHANGE_EVENT_LOGGER );
		}
	}

	/**
	 * Returns the recorder configuration for this channel.
	 */
	@XmlElement( name = "record_configuration" )
	public RecordConfiguration getRecordConfiguration()
	{
		return mRecordConfiguration;
	}

	/**
	 * Sets the recorder configuration for this channel.
	 */
	public void setRecordConfiguration( RecordConfiguration config )
	{
		mRecordConfiguration = config;

		if( mProcessingChain != null && mProcessingChain.isRunning() )
		{
			mProcessingChain.updateRecording();
		}
	}

	/**
	 * Sets the recorder configuration for this channel and optionally broadcasts
	 * a recorder configuration change channel event.
	 */
	public void setRecordConfiguration( RecordConfiguration config,
										boolean fireChangeEvent )
	{
		setRecordConfiguration( config );
		
		if( fireChangeEvent )
		{
			fireChannelEvent( ChannelEvent.CHANGE_RECORDER );
		}
	}

	/**
	 * Returns the name of this channel.
	 */
	@XmlAttribute
	public String getName()
	{
		return mName;
	}

	/**
	 * Sets the name of this channel.
	 */
	public void setName( String name )
	{
		mName = name;
	}

	/**
	 * Sets the name of this channel and optionally broadcasts a name change
	 * channel event.
	 */
	public void setName( String name, boolean fireChangeEvent )
	{
		mName = name;
		
		if( fireChangeEvent )
		{
			fireChannelEvent( ChannelEvent.CHANGE_NAME );
		}
	}

	/**
	 * Responds to changes in the enabled setting by starting the processing
	 * change if the channel is now enabled, or stopping the procesing chain
	 * if it was already started.
	 */
	private void enableChanged()
	{
		if( mEnabled )
		{
			start();
		}
		else
		{
			stop();
		}
	}
	
	/**
	 * Starts the channel processing chain.
	 */
	private void start()
	{
		if( mEnabled && mProcessingChain == null && mResourceManager != null )
		{
			mProcessingChain = new ProcessingChain( this, mResourceManager );

			/* Add system-wide message listeners */
			mProcessingChain.addListeners( 
					mResourceManager.getChannelManager().getMessageListeners() );

			/* Add individual message listeners */
			mProcessingChain.addListeners( mMessageListeners );

			mProcessingChain.start();
			
			fireChannelEvent( ChannelEvent.PROCESSING_STARTED );
		}

		/* If this is a traffic channel, override the call fade timeout before
		 * we set the state to call, so that it will auto-expire after the 
		 * fade timeout and be auto deleted */
		if( mChannelType == ChannelType.TRAFFIC )
		{
			if( mProcessingChain.isProcessing() )
			{
				mProcessingChain.getChannelState().setCallFadeTimeout( 40000 );
				mProcessingChain.getChannelState().setState( State.CALL );
			}
			else
			{
				mProcessingChain.getChannelState().setCallFadeTimeout( 5000 );
				/* Channel state should already be no_tuner */
			}
			
		}
	}
	
	/**
	 * Stops the channel processing chain
	 */
	private void stop()
	{
		/* Stop and remove any traffic channels */
		for( Channel traffic: mTrafficChannels.values() )
		{
			traffic.stop();
			traffic.dispose();
		}
		
		mTrafficChannels.clear();

		if( !mEnabled && mProcessingChain != null )
		{
			mProcessingChain.stop();

			mProcessingChain.dispose();

			mProcessingChain = null;
			
			mSelected = false;
		}

		fireChannelEvent( ChannelEvent.PROCESSING_STOPPED );
	}
	
	/**
	 * Broadcasts a channel change event to all registered listeners
	 */
	public void fireChannelEvent( ChannelEvent event )
	{
		for( ChannelListener listener: mChannelListeners )
		{
			listener.occurred( this, event );
		}
	}
	
	/**
	 * Adds a channel listener to receive all channel events from this channel
	 * and automatically sends the listener a channel add event.
	 */
	public void addListener( ChannelListener listener )
	{
		if( !mChannelListeners.contains( listener ) )
		{
			mChannelListeners.add( listener );
			
			listener.occurred( this, ChannelEvent.CHANNEL_ADDED );
		}
		else
		{
			Log.error( "Channel - attempt to add already existing channel listener [" + listener.getClass() + "]" );
		}
	}

	/**
	 * Adds a list of channel listeners to receive all channel events from this
	 * channel and automatically sends each one a channel add event.
	 */
	public void addListeners( List<ChannelListener> listeners )
	{
		for( ChannelListener listener: listeners )
		{
			addListener( listener );
		}
	}
	
	/**
	 * Removes a channel listener from receiving channel events from this channel
	 */
	public void removeListener( ChannelListener listener )
	{
		mChannelListeners.remove( listener );
	}
	
	public void addListener( Listener<Message> listener )
	{
		mMessageListeners.add( listener );
		
		if( isProcessing() )
		{
			mProcessingChain.addListener( listener );
		}
	}
	
	public void removeListener( Listener<Message> listener )
	{
		mMessageListeners.remove( listener );
		
		if( isProcessing() )
		{
			mProcessingChain.removeListener( listener );
		}
	}
	
	/**
	 * Indicates if any part of this channel is contained within the
	 * minimum and maximum frequency values.
	 */
	public boolean isWithin( long minimum, long maximum )
	{
		return getTunerChannel() != null &&
			   getTunerChannel().isWithin( minimum, maximum );
	}
	
	public void addTrafficChannel( int channelNumber, Channel channel )
	{
		mTrafficChannels.put( channelNumber, channel );
	}
	
	public boolean hasTrafficChannel( Channel channel )
	{
		return mTrafficChannels.containsValue( channel );
	}
	
	public void removeTrafficChannel( Channel channel )
	{
		int keyToRemove = -1;
		
		if( mTrafficChannels.values().contains( channel ) )
		{
			for( Integer channelNumber: mTrafficChannels.keySet() )
			{
				if( mTrafficChannels.get( channelNumber ).equals( channel ) )
				{
					keyToRemove = channelNumber;
				}
			}
		}

		if( keyToRemove != -1 )
		{
			mTrafficChannels.remove( keyToRemove );
		}
	}
	
	public Collection<Channel> getTrafficChannels()
	{
		return Collections.unmodifiableCollection( mTrafficChannels.values() );
	}
	
	public boolean hasTrafficChannel( int channelNumber )
	{
		return mTrafficChannels.containsKey( channelNumber );
	}
	
	/**
	 * Channel Events - used to specify channel events and changes to channel
	 * configurations and settings.
	 */
	public enum ChannelEvent
	{
		CHANGE_ALIAS_LIST,
		CHANGE_DECODER,
		CHANGE_ENABLED,
        CHANGE_EVENT_LOGGER,
		CHANGE_NAME,
		CHANGE_SITE,
		CHANGE_RECORDER,
		CHANGE_SELECTED,
		CHANGE_SOURCE,
		CHANGE_SYSTEM,

		CHANNEL_ADDED,
        CHANNEL_DELETED,

		PROCESSING_STARTED,
		PROCESSING_STOPPED,
	}
}
