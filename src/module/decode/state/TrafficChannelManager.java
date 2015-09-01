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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import source.config.SourceConfigTuner;
import source.tuner.TunerChannel;
import source.tuner.TunerChannel.Type;
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
	private ResourceManager mResourceManager;
	private long mTrafficChannelTimeout;

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
	 * @param trafficChannelTimeout - millisecond call timer limit used when an
	 * end of call signalling event is not decoded.
	 *  
	 * @param trafficChannelPoolSize - maximum number of allocated traffic channels
	 * in the pool
	 */
	public TrafficChannelManager( ResourceManager resourceManager,
								  DecodeConfiguration decodeConfiguration,
								  long trafficChannelTimeout,
								  int trafficChannelPoolSize ) 
	{
		mResourceManager = resourceManager;
		mDecodeConfiguration = decodeConfiguration;
		mTrafficChannelTimeout = trafficChannelTimeout;
		mTrafficChannelPoolMaximumSize = trafficChannelPoolSize;
	}
	
	@Override
	public void dispose()
	{
		mCallEventListener = null;
		mResourceManager = null;
		mDecodeConfiguration = null;
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
				
				channel.setResourceManager( mResourceManager );
				
				channel.setTrafficChannelTimeout( mTrafficChannelTimeout );
				
				mTrafficChannelPool.add( channel );
			}

			/* If we have a configured channel, start it and track it */
			if( channel != null )
			{
				channel.setSourceConfiguration( new SourceConfigTuner( tunerChannel ) );
				
				channel.setSystem( new System( "Traffic" ), false );
				channel.setSite( new Site( "Channel" ), false );
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
					
					channel.setSystem( null, false );
					channel.setSite( null, false );
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
			
			if( frequency > 0 )
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
	public void init()
	{
		
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
