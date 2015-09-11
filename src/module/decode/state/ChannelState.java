/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package module.decode.state;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import module.Module;
import module.decode.config.DecodeConfiguration;
import module.decode.event.CallEvent;
import module.decode.event.ICallEventProvider;
import module.decode.state.DecoderStateEvent.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import audio.metadata.IMetadataProvider;
import audio.metadata.Metadata;
import audio.metadata.MetadataReset;
import audio.metadata.MetadataType;
import audio.squelch.ISquelchStateProvider;
import audio.squelch.SquelchState;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;
import controller.channel.Channel.ChannelType;

/**
 * Channel state tracks the overall state of all processing modules and decoders
 * configured for the channel and provides squelch control events.
 * 
 * Primary channel state provides a state machine for tracking decode events
 * during a voice or data call through it's life cycle of CALL/DATA, FADE, END, 
 * and then back to an IDLE state.  This class also provides squelch control 
 * to support any audio processing.
 * 
 * State Descriptions:
 *  IDLE		No voice or data call activity
 * 	CALL/DATA	A voice or data call has started, or is continuing
 *  CONTROL     Control channel
 *  FADE		The phase after a voice or data call when either an explicit
 *  			call end has been received, or when no new signalling updates
 *  			have been received, and the fade timer has expired.  This phase
 *  			allows for gui updates to signal to the user that the call is
 *  			ended, while continuing to display the call details for the user
 *  END			The phase after the voice or data call FADE, when the call is
 *  			now over, and the gui should be reset to IDLE.
 * 
 * When a call is started, invoke mTimerService.startFadeTimer() to start the 
 * call fade monitoring timer process that will automatically signal a call end
 * once no more call signalling has been detected after the fade timeout has
 * occurred, by invoking the fade() method.
 * 
 * When a call ends, invoke the mTimerService.stopFadeTimer() and then invoke
 * the mTimerService.startResetTimer().  Once the reset timeout occurs, 
 * the reset() method will be called, allowing the channel state to perform
 * any reset actions, like clearing status and id labels on any view panels.
 */
public class ChannelState extends Module implements ICallEventProvider,
	IChangedAttributeProvider, IDecoderStateEventListener, 
	IDecoderStateEventProvider, IMetadataProvider, ISquelchStateProvider
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ChannelState.class );

	public static final long FADE_TIMEOUT_DELAY = 1000;
	public static final long RESET_TIMEOUT_DELAY = 2000;
	
	private State mState = State.IDLE;
	private long mTrafficChannelTimeout = 
			DecodeConfiguration.DEFAULT_CALL_TIMEOUT_SECONDS;

	private Listener<CallEvent> mCallEventListener;
	private Listener<ChangedAttribute> mChangedAttributeListener;
	private Listener<DecoderStateEvent> mDecoderStateListener;
	private Listener<Metadata> mMetadataListener;
	private Listener<SquelchState> mSquelchStateListener;
	private DecoderStateEventReceiver mDecoderStateEventReceiver = 
					new DecoderStateEventReceiver();

	private boolean mSquelchLocked = false;
	private boolean mSelected = false;
	
	private long mFadeTimeout;
	private long mResetTimeout;
	
	private StateMonitor mStateMonitor = new StateMonitor();
	private AtomicBoolean mMonitoring = new AtomicBoolean();
	private ScheduledFuture<?> mRunningMonitor;

	private ThreadPoolManager mThreadPoolManager;
	private ChannelType mChannelType;
	
	private TrafficChannelStatusListener mTrafficChannelStatusListener;
	private CallEvent mTrafficChannelCallEvent;
	
	public ChannelState( ThreadPoolManager manager, ChannelType channelType )
	{
		mThreadPoolManager = manager;
		mChannelType = channelType;
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
	}

	public void dispose()
	{
		stopMonitor();
		
		mCallEventListener = null;
		mChangedAttributeListener = null;
		mDecoderStateListener = null;
		mSquelchStateListener = null;
		mThreadPoolManager = null;
		mStateMonitor = null;
	}
	
	public void setTrafficChannelTimeout( long milliseconds )
	{
		mTrafficChannelTimeout = milliseconds;
	}
	
	public void setSelected( boolean selected )
	{
		mSelected = selected;
	}

	public boolean isSelected()
	{
		return mSelected;
	}
	
	public State getState()
	{
		return mState;
	}
	
	private boolean isEndingState()
	{
		return mState == State.IDLE ||
			   mState == State.FADE ||
			   mState == State.END;
	}
	

	/**
	 * Starts the state monitor running when we're not in IDLE state.  Calls to
	 * start an already started monitor are ignored.
	 */
	private void startMonitor()
	{
		if( mMonitoring.compareAndSet( false, true ) )
		{
			if( mRunningMonitor == null )
			{
				mRunningMonitor = mThreadPoolManager.scheduleFixedRate( 
						ThreadType.DECODER, mStateMonitor, 20, TimeUnit.MILLISECONDS );
			}
			else
			{
				throw new RuntimeException( "Channel state monitor's scheduled "
						+ "future pointer was not null" );
			}
		}
	}

	/**
	 * Stops the state monitor when we're in an IDLE state.  Calls to stop an
	 * already stopped monitor are ignored.
	 */
	private void stopMonitor()
	{
		if( mMonitoring.compareAndSet( true, false ) )
		{
			if( mRunningMonitor != null )
			{
				mThreadPoolManager.cancel( mRunningMonitor );
			}
			else
			{
				throw new RuntimeException( "Channel state monitor's scheduled "
						+ "future pointer was null when trying to stop the monitor" );
			}
			
			mRunningMonitor = null;
		}
	}
	
	/**
	 * Updates the fade timeout threshold to the current time plus delay
	 */
	private void updateFadeTimeout()
	{
		/* Standard channel uses the FADE_TIMEOUT_DELAY and traffic channels
		 * use the TRAFFIC_FADE_TIMEOUT_DELAY.  Some traffic channels (e.g. MPT)
		 * don't have signaling on the traffic channel that can be used to
		 * update the fade timeout, so we set the timeout value larger to ensure
		 * the channel doesn't fade before the end of the call. */ 
		long timeout = System.currentTimeMillis() + 
				( mChannelType == ChannelType.STANDARD ? FADE_TIMEOUT_DELAY : 
					mTrafficChannelTimeout );
		
		if( timeout > mFadeTimeout )
		{
			mFadeTimeout = timeout;
		}
	}

	/**
	 * Updates the reset timeout threshold to the current time plus delay
	 */
	private void updateResetTimeout()
	{
		mResetTimeout = System.currentTimeMillis() + RESET_TIMEOUT_DELAY;
	}
	
	/**
	 * Broadcasts the squelch state to the registered listener
	 */
	protected void broadcast( SquelchState state )
	{
		if( mSquelchStateListener != null && !mSquelchLocked )
		{
			mSquelchStateListener.receive( state );
		}
	}

	/**
	 * Sets the squelch state listener 
	 */
	@Override
	public void setSquelchStateListener( Listener<SquelchState> listener )
	{
		mSquelchStateListener = listener;
	}

	/**
	 * Removes the squelch state listener
	 */
	@Override
	public void removeSquelchStateListener()
	{
		mSquelchStateListener = null;
	}

	/**
	 * Sets the channel state to the specified state, or updates the timeout
	 * values so that the state monitor will not change state.  Broadcasts a 
	 * squelch event when the state changes and the audio squelch state should
	 * change.  Also broadcasts changed attribute and decoder state events so
	 * that external processes can maintain sync with this channel state. 
	 */
	protected void setState( State state )
	{
		if( mState == state )
		{
			switch( state )
			{
				case CALL:
				case CONTROL:
				case DATA:
				case ENCRYPTED:
					updateFadeTimeout();
					break;
				default:
					break;
			}
		}
		else if( mState.canChangeTo( state ) )
		{
			switch( state )
			{
				case CONTROL:
				case DATA:
				case ENCRYPTED:
					broadcast( SquelchState.SQUELCH );
					updateFadeTimeout();
					startMonitor();
					break;
				case CALL:
					broadcast( SquelchState.UNSQUELCH );
					updateFadeTimeout();
					startMonitor();
					break;
				case FADE:
					broadcast( SquelchState.SQUELCH );
					updateResetTimeout();
					startMonitor();
					break;
				case END:
					broadcast( SquelchState.SQUELCH );
					broadcast( new DecoderStateEvent( this, Event.RESET, State.IDLE ) );
					broadcast( new MetadataReset() );
					break;
				case IDLE:
					broadcast( SquelchState.SQUELCH );
					stopMonitor();
					
					if( mChannelType == ChannelType.TRAFFIC )
					{
						if( mTrafficChannelStatusListener != null )
						{
							mTrafficChannelStatusListener.callEnd();
							mTrafficChannelStatusListener = null;
						}
						
						if( mTrafficChannelCallEvent != null )
						{
							mTrafficChannelCallEvent.end();
							
							broadcast( mTrafficChannelCallEvent );
							mTrafficChannelCallEvent = null;
						}
					}
					break;
				default:
					break;
			}

			mState = state;
			
			broadcast( ChangedAttribute.CHANNEL_STATE );
		}
	}

	public class StateMonitor implements Runnable
	{
		@Override
		public void run()
		{
			if( getState() == State.FADE )
			{
				if( mResetTimeout <= System.currentTimeMillis() )
				{
					setState( State.END );
				}
			}
			else if( getState() == State.END )
			{
				setState( State.IDLE );
			}
			else
			{
				if( mFadeTimeout <= System.currentTimeMillis() )
				{
					setState( State.FADE );
				}
			}
		}
	}

	/**
	 * Broadcasts the call event to the registered listener
	 */
	protected void broadcast( CallEvent event )	
	{
		if( mCallEventListener != null )
		{
			mCallEventListener.receive( event );
		}
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

	/**
	 * Broadcasts the channel state attribute change event to all registered
	 * listeners
	 */
	protected void broadcast( ChangedAttribute attribute )
	{
		if( mChangedAttributeListener != null )
		{
			mChangedAttributeListener.receive( attribute );
		}
	}

	/**
	 * Adds the listener to receive channel state attribute change events
	 */
	@Override
	public void setChangedAttributeListener( Listener<ChangedAttribute> listener )
	{
		mChangedAttributeListener = listener;
	}
	
	/**
	 * Removes the listener to receive channel state attribute change events
	 */
	@Override
	public void removeChangedAttributeListener()
	{
		mChangedAttributeListener = null;
	}

	/**
	 * Broadcasts a channel state event to any registered listeners
	 */
	protected void broadcast( DecoderStateEvent event )
	{
		if( mDecoderStateListener != null )
		{
			mDecoderStateListener.receive( event );
		}
	}

	/**
	 * Adds a decoder state event listener
	 */
	@Override
	public void setDecoderStateListener( Listener<DecoderStateEvent> listener )
	{
		mDecoderStateListener = listener;
	}

	/**
	 * Removes the decoder state event listener
	 */
	@Override
	public void removeDecoderStateListener()
	{
		mDecoderStateListener = null;
	}

	@Override
	public Listener<DecoderStateEvent> getDecoderStateListener()
	{
		return mDecoderStateEventReceiver;
	}

	/**
	 * Broadcasts metadata to the registered listener
	 */
	protected void broadcast( Metadata metadata )
	{
		if( mMetadataListener != null )
		{
			mMetadataListener.receive( metadata );
		}
	}

	/**
	 * Adds a metadata listener
	 */
	@Override
	public void setMetadataListener( Listener<Metadata> listener )
	{
		mMetadataListener = listener;
	}

	/**
	 * Removes the decoder state event listener
	 */
	@Override
	public void removeMetadataListener()
	{
		mMetadataListener = null;
	}

	/**
	 * Registers a listener to be notified when a traffic channel call event is
	 * completed, so that the listener can perform call tear-down 
	 */
	public void configureAsTrafficChannel( TrafficChannelStatusListener listener, 
			TrafficChannelAllocationEvent allocationEvent )
	{
		mTrafficChannelStatusListener = listener;
		mTrafficChannelCallEvent = allocationEvent.getCallEvent();
		
		/* Broadcast the call event details as metadata for the audio manager */
		String channel = mTrafficChannelCallEvent.getChannel();
		
		if( channel != null )
		{
			broadcast( new Metadata( MetadataType.CHANNEL_NUMBER, channel, true ) );
		}

		broadcast( new Metadata( MetadataType.PROTOCOL,  
			mTrafficChannelCallEvent.getDecoderType().getDisplayString(), true ) );

		String details = mTrafficChannelCallEvent.getDetails();
		
		if( details != null )
		{
			broadcast( new Metadata( MetadataType.DETAILS, details, true ) );
		}
		
		String from = mTrafficChannelCallEvent.getFromID();
		
		if( from != null )
		{
			broadcast( new Metadata( mTrafficChannelCallEvent.getFromIDType(), 
					from, mTrafficChannelCallEvent.getFromIDAlias(), true ) );
		}
		
		String to = mTrafficChannelCallEvent.getToID();
		
		if( to != null )
		{
			broadcast( new Metadata( mTrafficChannelCallEvent.getToIDType(), to, 
					mTrafficChannelCallEvent.getToIDAlias(), true ) );
		}
		
		long frequency = mTrafficChannelCallEvent.getFrequency();
		
		if( frequency > 0 )
		{
			broadcast( new Metadata( MetadataType.FREQUENCY, 
					String.valueOf( frequency ), true ) );
		}
		
		/* Rebroadcast the allocation event so that the internal decoder states
		 * can self-configure with the call event details */
		broadcast( allocationEvent );
	}
	
	/**
	 * DecoderStateEvent receiver wrapper
	 */
	public class DecoderStateEventReceiver implements Listener<DecoderStateEvent>
	{
		@Override
		public void receive( DecoderStateEvent event )
		{
			if( event.getSource() != this )
			{
				switch( event.getEvent() )
				{
					case ALWAYS_UNSQUELCH:
						broadcast( SquelchState.UNSQUELCH );
						mSquelchLocked = true;
						break;
					case CONTINUATION:
					case DECODE:
						if( isEndingState() )
						{
							setState( event.getState() );
						}
						else
						{
							updateFadeTimeout();
						}
						break;
					case END:
						setState( State.FADE );
						break;
					case RESET:
						/* Channel State does not respond to reset events */
						break;
					case START:
						setState( event.getState() );
						break;
					default:
						break;
				}
			}
		}
	}
}
