package module.decode.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import module.Module;
import module.decode.config.DecodeConfiguration;
import module.decode.event.CallEvent;
import module.decode.event.CallEvent.CallEventType;
import module.decode.event.ICallEventProvider;
import module.decode.mpt1327.MPT1327CallEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import record.config.RecordConfiguration;
import sample.Listener;
import source.config.SourceConfigTuner;
import source.tuner.TunerChannel;
import source.tuner.TunerChannel.Type;
import alias.Alias;
import alias.priority.Priority;
import controller.ResourceManager;
import controller.channel.Channel;
import controller.channel.Channel.ChannelType;
import controller.site.Site;
import controller.system.System;

public class TrafficChannelManager extends Module 
			implements ICallEventProvider, IDecoderStateEventListener
{
	private final static Logger mLog = LoggerFactory.getLogger( TrafficChannelManager.class );
	
	private int mTrafficChannelPoolMaximumSize = 
			DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT;
	private List<Channel> mTrafficChannelPool = new ArrayList<Channel>();
	private Map<String,Channel> mTrafficChannelsInUse = new ConcurrentHashMap<String,Channel>();

	private DecoderStateEventListener mEventListener = new DecoderStateEventListener();
	private Listener<CallEvent> mCallEventListener;

	private DecodeConfiguration mDecodeConfiguration;
	private RecordConfiguration mRecordConfiguration;
	private ResourceManager mResourceManager;
	private System mSystem;
	private Site mSite;
	private String mAliasListName;
	private long mTrafficChannelTimeout;
	private CallEvent mPreviousDoNotMonitorCallEvent;

	/**
	 * Monitors call events and allocates traffic decoder channels in response
	 * to traffic channel allocation call events.  Manages a pool of reusable
	 * traffic channel allocations.
	 *  
	 * @param resourceManager - resource manager
	 * 
	 * @param decodeConfiguration - decoder configuration to use for each 
	 * traffic channel allocation.
	 * 
	 * @param recordConfiguration - recording options for each traffic channel
	 * 
	 * @param trafficChannelTimeout - millisecond call timer limit used when an
	 * end of call signalling event is not decoded.
	 *  
	 * @param trafficChannelPoolSize - maximum number of allocated traffic channels
	 * in the pool
	 */
	public TrafficChannelManager( ResourceManager resourceManager,
								  DecodeConfiguration decodeConfiguration,
								  RecordConfiguration recordConfiguration,
								  System system,
								  Site site,
								  String aliasListName,
								  long trafficChannelTimeout,
								  int trafficChannelPoolSize ) 
	{
		mResourceManager = resourceManager;
		mDecodeConfiguration = decodeConfiguration;
		mRecordConfiguration = recordConfiguration;
		mSystem = system;
		mSite = site;
		mAliasListName = aliasListName;
		mTrafficChannelTimeout = trafficChannelTimeout;
		mTrafficChannelPoolMaximumSize = trafficChannelPoolSize;
	}
	
	@Override
	public void dispose()
	{
		mCallEventListener = null;
		mResourceManager = null;
		mDecodeConfiguration = null;
		mPreviousDoNotMonitorCallEvent = null;
	}

	private Channel getChannel( String channelNumber, TunerChannel tunerChannel )
	{
		Channel channel = null;

		if( !mTrafficChannelsInUse.containsKey( channelNumber ) )
		{
			for( Channel configuredChannel: mTrafficChannelPool )
			{
				if( !configuredChannel.isProcessing() )
				{
					channel = configuredChannel;
					break;
				}
			}
			
			if( channel == null && mTrafficChannelPool.size() < mTrafficChannelPoolMaximumSize )
			{
				channel = new Channel( "Traffic", ChannelType.TRAFFIC );
				
				channel.setDecodeConfiguration( mDecodeConfiguration );
				
				channel.setRecordConfiguration( mRecordConfiguration );
				
				channel.setResourceManager( mResourceManager );
				
				channel.setTrafficChannelTimeout( mTrafficChannelTimeout );
				
				channel.setAliasListName( mAliasListName );
				
				mTrafficChannelPool.add( channel );
			}

			/* If we have a configured channel, start it and track it */
			if( channel != null )
			{
				channel.setSourceConfiguration( new SourceConfigTuner( tunerChannel ) );
				
				channel.setSystem( mSystem, false );
				channel.setSite( mSite, false );
				channel.setName( channelNumber );

				channel.setEnabled( true );

				/* Check the channel obtained a source and is currently processing */
				if( channel.isProcessing() )
				{
					mTrafficChannelsInUse.put( channelNumber, channel );
				}
				else
				{
					channel.setEnabled( false );
					
					channel.setSystem( mSystem, false );
					channel.setSite( mSite, false );
					channel.setName( "Traffic" );
				}
			}
		}
		
		return channel;
	}
	
	private void process( TrafficChannelAllocationEvent event )
	{
		if( mCallEventListener != null )
		{
			CallEvent callEvent = event.getCallEvent();

			/* Check for duplicate events and suppress */
			if( mTrafficChannelsInUse.containsKey( callEvent.getChannel() ) )
			{
				return;
			}

			long frequency = callEvent.getFrequency();
			
			/* Check the from/to aliases for do not monitor priority */
			if( isDoNotMonitor( callEvent ) )
			{
				if( isSameCallEvent( mPreviousDoNotMonitorCallEvent, callEvent ) )
				{
					return;
				}
				else
				{
					mPreviousDoNotMonitorCallEvent = callEvent;
					callEvent.setCallEventType( CallEventType.CALL_DO_NOT_MONITOR );
				}
			}
			else if( frequency > 0 )
			{
				Channel channel = getChannel( callEvent.getChannel(), 
					new TunerChannel( Type.TRAFFIC, frequency, 
							mDecodeConfiguration.getDecoderType()
									.getChannelBandwidth() ) );

				if( channel != null )
				{
					if( channel.isProcessing() )
					{
						ChannelState state = channel.getChannelState();
						
						/* Register as a listener to be notified when the call
						 * is completed */
						state.configureAsTrafficChannel( 
							new TrafficChannelStatusListener( this, 
								callEvent.getChannel() ), event );
						
						/* Set state to call so that audio squelch state is correct */
						state.setState( State.CALL );
					}
					else
					{
						callEvent.setCallEventType( CallEventType.CALL_DETECT );
					}
				}
				else
				{
					callEvent.setCallEventType( CallEventType.CALL_DETECT );
				}
			}
			else
			{
				callEvent.setCallEventType( CallEventType.CALL_DETECT );
			}
			
			mCallEventListener.receive( callEvent );
		}
	}

	/**
	 * Compares the call type, channel and to fields for equivalence and the 
	 * from field for either both null, or equivalence.
	 * 
	 * @param e1
	 * @param e2
	 * @return
	 */
	public static boolean isSameCallEvent( CallEvent e1, CallEvent e2 )
	{
		if( e1 == null || 
			e2 == null || 
			e1.getCallEventType() != e2.getCallEventType() )
		{
			return false;
		}

		if( e1.getChannel() == null || 
			e2.getChannel() == null || 
			!e1.getChannel().contentEquals( e2.getChannel() ) )
		{
			return false;
		}
		
		if( e1.getToID() == null ||
			e2.getToID() == null ||
			!e1.getToID().contentEquals( e2.getToID() ) )
		{
			return false;
		}

		if( e1.getFromID() == null || e2.getFromID() == null )
		{
			if( e1.getFromID() == null && e2.getFromID() == null )
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else if( e1.getFromID().contentEquals( e2.getFromID() ) )
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks for aliases that contain a do not follow/process priority.  
	 * 
	 * @param event
	 * @return - true if there is an alias that has a do not process alias id
	 */
	private boolean isDoNotMonitor( CallEvent event )
	{
		Alias to = event.getToIDAlias();

		if( to != null && 
			to.hasPriority() && 
			to.getCallPriority() == Priority.DO_NOT_MONITOR )
		{
			return true;
		}
		
		Alias from = event.getFromIDAlias();

		if( from != null && 
			from.hasPriority() && 
			from.getCallPriority() == Priority.DO_NOT_MONITOR )
		{
			return true;
		}
		
		return false;
	}

	@Override
	public Listener<DecoderStateEvent> getDecoderStateListener()
	{
		return mEventListener;
	}

	@Override
	public void setCallEventListener( Listener<CallEvent> listener )
	{
		mCallEventListener = listener;
	}

	@Override
	public void removeCallEventListener()
	{
		mCallEventListener = null;
	}

	@Override
	public void reset()
	{
	}
	
	@Override
	public void start()
	{
	}

	@Override
	public void stop()
	{
		if( !mTrafficChannelsInUse.isEmpty() )
		{
			List<String> channels = new ArrayList<>();

			/* Copy the keyset so we don't get concurrent modification of the map */
			channels.addAll( mTrafficChannelsInUse.keySet() );
			
			for( String channel: channels )
			{
				callEnd( channel );
			}
		}
	}

	/**
	 * Callback used by the TrafficChannelStatusListener class to signal the 
	 * end of a an allocated traffic channel call event
	 * 
	 * @param channelNumber - channel number from the call event that signaled
	 * the start of a traffic channel allocation
	 */
	public void callEnd( String channelNumber )
	{
		if( channelNumber != null && mTrafficChannelsInUse.containsKey( channelNumber ) )
		{
			Channel channel = mTrafficChannelsInUse.get( channelNumber );
			
			mTrafficChannelsInUse.remove( channelNumber );

			/* Disable the channel and broadcast a notification */
			channel.setEnabled( false, true );
		}
	}
	
	/**
	 * Wrapper class for the decoder state event listener interface to catch
	 * traffic channel allocation requests
	 */
	public class DecoderStateEventListener implements Listener<DecoderStateEvent>
	{
		@Override
		public void receive( DecoderStateEvent event )
		{
			switch( event.getEvent() )
			{
				case TRAFFIC_CHANNEL_ALLOCATION:
					process( (TrafficChannelAllocationEvent)event );
					break;
				default:
					break;
			}
		}
	}
}
