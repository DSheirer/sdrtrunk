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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import message.Message;
import module.Module;
import module.ProcessingChain;
import module.decode.DecoderFactory;
import module.decode.config.AuxDecodeConfiguration;
import module.decode.config.DecodeConfigFactory;
import module.decode.config.DecodeConfiguration;
import module.decode.event.CallEventModel;
import module.decode.event.MessageActivityModel;
import module.decode.state.ChannelState;
import module.log.MessageEventLogger;
import module.log.config.EventLogConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import record.RecorderManager;
import record.RecorderType;
import record.config.RecordConfiguration;
import sample.Listener;
import source.Source;
import source.SourceException;
import source.SourceType;
import source.config.SourceConfigFactory;
import source.config.SourceConfigRecording;
import source.config.SourceConfigTuner;
import source.config.SourceConfiguration;
import source.tuner.TunerChannel;
import source.tuner.TunerChannel.Type;
import alias.AliasList;
import audio.metadata.Metadata;
import audio.metadata.MetadataType;
import controller.ResourceManager;
import controller.channel.ChannelEvent.Event;
import controller.config.Configuration;
import controller.site.Site;
import controller.system.System;
import filter.FilterSet;

@XmlSeeAlso( { Configuration.class } )
@XmlRootElement( name = "channel" )
public class Channel extends Configuration
{
	private final static Logger mLog = LoggerFactory.getLogger( Channel.class );

	private static final boolean ENABLED = true;
	private static final boolean DISABLED = false;
	private static final boolean BROADCAST_CHANGE = true;

	public enum ChannelType 
	{ 
		STANDARD,
		TRAFFIC 
	};
	
	
	private CopyOnWriteArrayList<ChannelEventListener> mChannelListeners =
				new CopyOnWriteArrayList<ChannelEventListener>();

	private CopyOnWriteArrayList<Listener<Message>> mMessageListeners =
			new CopyOnWriteArrayList<Listener<Message>>();
	
	private Map<String,Channel> mTrafficChannels = new HashMap<>();
	
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

	private CallEventModel mCallEventModel;
	private MessageActivityModel mMessageActivityModel;
	private MessageEventLogger mEventLogger;
	private ProcessingChain mProcessingChain;
	
	private ChannelState mChannelState;
	private long mTrafficChannelTimeout = 
			DecodeConfiguration.DEFAULT_CALL_TIMEOUT_SECONDS;

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
	
	/**
	 * Indicates if the channel has Automatic or Direct Frequency Control
	 */
	public boolean hasFrequencyControl()
	{
		return mProcessingChain != null && 
			   mProcessingChain.hasFrequencyCorrectionControl();
	}
	
	public long getFrequencyCorrection()
	{
		if( hasFrequencyControl() )
		{
			return mProcessingChain.getFrequencyCorrectionControl().getErrorCorrection();
		}
		
		return 0;
	}

	/**
	 * Sets the channel state traffic channel call end timer to the specified 
	 * value.
	 */
	public void setTrafficChannelTimeout( long milliseconds )
	{
		mTrafficChannelTimeout = milliseconds;
		
		if( mChannelState != null )
		{
			mChannelState.setTrafficChannelTimeout( milliseconds );
		}
	}

	/**
	 * Sets the resource manager responsible for this channel and registers
	 * the channel manager as a listener to this channel.  Fires channel add
	 * channel event to the newly added channel manager channel listener.
	 * 
	 * Note: resource manager instance is applied after object construction
	 * since we construct the channel objects from the xml playlist
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
		return mProcessingChain != null && mProcessingChain.processing();
	}

	/**
	 * Indicates if this channel has been selected for audio output
	 */
	@XmlTransient
	public boolean isSelected()
	{
		return mSelected;
	}
	
	public void setSelected( boolean selected )
	{
		mSelected = selected;
		
		fireChannelEvent( Event.CHANGE_SELECTED );
	}
	
	@XmlTransient
	public CallEventModel getCallEventModel()
	{
		return mCallEventModel;
	}
	
	/**
	 * Returns the MessageEventLogger for this processing chain
	 */
	@XmlTransient
	public MessageEventLogger getEventLogger()
	{
		return mEventLogger;
	}

	/**
	 * Sets the MessageEventLogger for this processing chain
	 */
	public void setEventLogger( MessageEventLogger eventLogger )
	{
		mEventLogger = eventLogger;
	}
	
	@XmlTransient
	public MessageActivityModel getMessageActivityModel()
	{
		return mMessageActivityModel;
	}
	
	@XmlTransient
	public ChannelState getChannelState()
	{
		return mChannelState;
	}
	
	@XmlTransient
	public ChannelType getChannelType()
	{
		return mChannelType;
	}

	
	/**
	 * Orderly shutdown method when this channel is going to be deleted.
	 */
	public void dispose()
	{
		setEnabled( DISABLED, BROADCAST_CHANGE );

		/* Broadcast channel deleted event */
		fireChannelEvent( Event.CHANNEL_DELETED );
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

	public boolean hasSystem()
	{
		return mSystem != null;
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
	        fireChannelEvent( Event.CHANGE_SYSTEM );
	    }
	}

	/**
	 * Returns the owning site for this channel.
	 */
	public Site getSite()
	{
	    return mSite;
	}

	public boolean hasSite()
	{
		return mSite != null;
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
            fireChannelEvent( Event.CHANGE_SITE );
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
			if( enabled )
			{
				fireChannelEvent( Event.CHANNEL_ENABLED );
			}
			else
			{
				fireChannelEvent( Event.CHANNEL_DISABLED );
			}
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
			fireChannelEvent( Event.CHANGE_ALIAS_LIST );
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
			fireChannelEvent( Event.CHANGE_DECODER );
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
			fireChannelEvent( Event.CHANGE_SOURCE );
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
			fireChannelEvent( Event.CHANGE_EVENT_LOGGER );
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
			fireChannelEvent( Event.CHANGE_RECORDER );
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
			fireChannelEvent( Event.CHANGE_NAME );
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
		if( mEnabled && mResourceManager != null )
		{
			setupChannelState();
			
			try
			{
				setup();
				
				mProcessingChain.start();
				
				fireChannelEvent( Event.CHANNEL_PROCESSING_STARTED );
			}
			catch( SourceException se )
			{
				mLog.error( "Error obtaining source for channel" );
			}
		}
	}
	
	private void setupChannelState()
	{
		if( mChannelState == null )
		{
			mChannelState = new ChannelState( mResourceManager
					.getThreadPoolManager(), mChannelType );
			
			mChannelState.setTrafficChannelTimeout( mTrafficChannelTimeout );
		}
	}

	/**
	 * Sets up the channel and prepares to start processing.  Sets up the 
	 * processing chain and requests a source.
	 * 
	 * @throws SourceException - if no source is available or if there is an 
	 * error obtaining a source
	 */
	public void setup() throws SourceException
	{
		setupProcessingChain();
		
		setupSource();
	}

	/**
	 * Setup recording options.  Adds RecorderManager as a audio packet listener
	 * to receive and record decoded audio.
	 */
	private void setupRecording()
	{
		List<RecorderType> recorders = mRecordConfiguration.getRecorders();

		if( !recorders.isEmpty() )
		{
			if( recorders.contains( RecorderType.AUDIO ) )
			{
				mProcessingChain.addAudioPacketListener( 
						mResourceManager.getRecorderManager() );
			}

			/* Add baseband recorder */
			if( ( recorders.contains( RecorderType.BASEBAND ) &&
				  mChannelType == ChannelType.STANDARD ) )
			{
				mProcessingChain.addModule( RecorderManager.getBasebandRecorder( 
					mResourceManager.getThreadPoolManager(), getChannelName() ) );
			}
			
			/* Add traffic channel baseband recorder */
			if( recorders.contains( RecorderType.TRAFFIC_BASEBAND ) &&
				mChannelType == ChannelType.TRAFFIC )
			{
				mProcessingChain.addModule( RecorderManager.getBasebandRecorder( 
					mResourceManager.getThreadPoolManager(), getChannelName() ) );
			}
		}
	}
	
	private String getChannelName()
	{
		StringBuilder sb = new StringBuilder();
		sb.append( mSystem );
		sb.append( "_" );
		sb.append( mSite );
		sb.append( "_" );
		sb.append( mName );
		
		return sb.toString();
	}
	
	private void setupLogging()
	{
		mLog.debug( "Setting up logging ..." );
		if( mProcessingChain != null )
		{
			String channelName = getChannelName();
			
			List<Module> loggers = mResourceManager.getEventLogManager()
					.getLoggers( mEventLogConfiguration, channelName );
			
			mLog.debug( "Created [" + loggers.size() + "] loggers for [" + 
					channelName + "]" );

			if( !loggers.isEmpty() )
			{
				mProcessingChain.addModules( loggers );
			}
		}
	}
	
	private void setupProcessingChain()
	{
		if( mProcessingChain == null )
		{
			mProcessingChain = new ProcessingChain( 
					mResourceManager.getThreadPoolManager() );
			
			/* Get the optional alias list for the decode modules to use */
			AliasList aliasList = mResourceManager.getPlaylistManager()
					.getPlayist().getAliasDirectory().getAliasList( mAliasListName );
			
			/* Add the channel state as a module */
			mProcessingChain.addModule( mChannelState );
			
			/* Processing Modules */
			List<Module> modules = DecoderFactory.getModules( mChannelType, 
				mResourceManager, mDecodeConfiguration, mRecordConfiguration,
				mAuxDecodeConfiguration, aliasList, mSystem, 
				mSite );
			
			mProcessingChain.addModules( modules );

			/* Get message filters for the set of processing modules */
			FilterSet<Message> messageFilter = DecoderFactory.getMessageFilters( modules ); 
			
			/* Setup the message activity model and add message listeners */
			mMessageActivityModel = new MessageActivityModel( messageFilter );
			mProcessingChain.addMessageListener( mMessageActivityModel );
			
			/* Add audio manager as listener for audio packets */
			mProcessingChain.addAudioPacketListener( 
					mResourceManager.getAudioManager() );

			/* Add system-wide message listeners */
			mProcessingChain.addMessageListeners( 
					mResourceManager.getChannelManager().getMessageListeners() );
			
			/* Add individual message listeners */
			mProcessingChain.addMessageListeners( mMessageListeners );

			/* Create the call event model to receive call events */
			mCallEventModel = new CallEventModel();
			mProcessingChain.addCallEventListener( mCallEventModel );
			
			setupLogging();
			
			setupRecording();
			
			/* Inject channel metadata that will be inserted into audio packets
			 * for the recorder manager and streaming */
			mProcessingChain.broadcast( 
					new Metadata( MetadataType.SYSTEM, mSystem.getName() ) );
			mProcessingChain.broadcast( 
					new Metadata( MetadataType.SITE_ID, mSite.getName() ) );
			mProcessingChain.broadcast( 
					new Metadata( MetadataType.CHANNEL_NAME, mName ) );
		}
	}

	/**
	 * Attempts to obtain a source and apply it to the processing chain.
	 * 
	 * @throws SourceException - if the processing chain is not setup or if
	 * there is an error produced while obtaining the source
	 */
	private void setupSource() throws SourceException
	{
		if( mProcessingChain != null )
		{
			Source source = mResourceManager.getSourceManager()
			.getSource( getSourceConfiguration(), getDecodeConfiguration()
					.getDecoderType().getChannelBandwidth() );
			
			if( source != null )
			{
				mProcessingChain.setSource( source );
			}
			else
			{
				throw new SourceException( "Couldn't obtain tuner channel source" );
			}
		}
		else
		{
			throw new SourceException( "Cannot obtain source for a null "
				+ "processing chain - setup the processing chain first" );
		}
	}
	
	/**
	 * Stops the channel processing chain
	 */
	private void stop()
	{
		if( !mEnabled && mProcessingChain != null )
		{
			mProcessingChain.stop();

			mSelected = false;
		}

		fireChannelEvent( Event.CHANNEL_PROCESSING_STOPPED );
	}
	
	/**
	 * Broadcasts a channel change event to all registered listeners
	 */
	public void fireChannelEvent( Event event )
	{
		ChannelEvent channelEvent = new ChannelEvent( this, event );
		
		for( ChannelEventListener listener: mChannelListeners )
		{
			listener.channelChanged( channelEvent );
		}
		
		/* Send event to processing chain so modules can respond to channel
		 * selection events */
		if( mProcessingChain != null )
		{
			mProcessingChain.getChannelEventListener().receive( channelEvent );
		}
	}
	
	/**
	 * Adds a channel listener to receive all channel events from this channel
	 * and automatically sends the listener a channel add event.
	 */
	public void addListener( ChannelEventListener listener )
	{
		if( !mChannelListeners.contains( listener ) )
		{
			mChannelListeners.add( listener );

			listener.channelChanged( 
					new ChannelEvent( this, Event.CHANNEL_ADDED ) );
		}
		else
		{
			mLog.error( "Channel - attempt to add already existing channel "
					+ "listener [" + listener.getClass() + "]" );
		}
	}

	/**
	 * Adds a list of channel listeners to receive all channel events from this
	 * channel and automatically sends each one a channel add event.
	 */
	public void addListeners( List<ChannelEventListener> listeners )
	{
		for( ChannelEventListener listener: listeners )
		{
			addListener( listener );
		}
	}
	
	/**
	 * Removes a channel listener from receiving channel events from this channel
	 */
	public void removeListener( ChannelEventListener listener )
	{
		mChannelListeners.remove( listener );
	}
	
	public void addListener( Listener<Message> listener )
	{
		mMessageListeners.add( listener );
		
		if( isProcessing() )
		{
			mProcessingChain.addMessageListener( listener );
		}
	}
	
	public void removeListener( Listener<Message> listener )
	{
		mMessageListeners.remove( listener );
		
		if( mProcessingChain != null )
		{
			mProcessingChain.removeMessageListener( listener );
		}
	}
	
	/**
	 * Indicates if any part of this channel is contained within the
	 * minimum and maximum frequency values.
	 */
	public boolean isWithin( long minimum, long maximum )
	{
		TunerChannel tunerChannel = getTunerChannel();
		
		return tunerChannel != null &&
			   tunerChannel.isWithin( minimum, maximum );
	}
	
	public void addTrafficChannel( String channelID, Channel channel )
	{
		mTrafficChannels.put( channelID, channel );
	}
	
	public boolean hasTrafficChannel( Channel channel )
	{
		return mTrafficChannels.containsValue( channel );
	}
	
	public void removeTrafficChannel( Channel channel )
	{
		String name = channel.getChannelDisplayName();
		
		if( mTrafficChannels.containsKey( name ) )
		{
			mTrafficChannels.remove( name );
		}
	}
	
	public Collection<Channel> getTrafficChannels()
	{
		return Collections.unmodifiableCollection( mTrafficChannels.values() );
	}
	
	public boolean hasTrafficChannel( String name )
	{
		return mTrafficChannels.containsKey( name );
	}
	
}
