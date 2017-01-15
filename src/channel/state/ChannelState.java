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
package channel.state;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import channel.metadata.Attribute;
import channel.metadata.AttributeChangeRequest;
import channel.metadata.IAttributeChangeRequestListener;
import channel.metadata.MutableMetadata;
import channel.traffic.TrafficChannelManager;
import module.Module;
import module.decode.config.DecodeConfiguration;
import module.decode.event.CallEvent;
import module.decode.event.ICallEventProvider;
import channel.state.DecoderStateEvent.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import audio.metadata.IMetadataProvider;
import audio.metadata.Metadata;
import audio.metadata.MetadataReset;
import audio.metadata.MetadataType;
import audio.squelch.ISquelchStateProvider;
import audio.squelch.SquelchState;
import controller.channel.Channel.ChannelType;

public class ChannelState extends Module implements ICallEventProvider,	IChangedAttributeProvider, 
	IDecoderStateEventListener, IDecoderStateEventProvider, IMetadataProvider, ISquelchStateProvider,
    IAttributeChangeRequestListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ChannelState.class );

	public static final long FADE_TIMEOUT_DELAY = 1200;
	public static final long RESET_TIMEOUT_DELAY = 2000;

	private MutableMetadata mMutableMetadata = new MutableMetadata();
	private State mState = State.IDLE;

	private Listener<CallEvent> mCallEventListener;
	private Listener<Attribute> mChangedAttributeListener;
	private Listener<DecoderStateEvent> mDecoderStateListener;
	private Listener<Metadata> mMetadataListener;
	private Listener<SquelchState> mSquelchStateListener;
	private DecoderStateEventReceiver mDecoderStateEventReceiver = 
					new DecoderStateEventReceiver();

	private boolean mSquelchLocked = false;
	private boolean mSelected = false;
	
	private long mStandardChannelFadeTimeout = FADE_TIMEOUT_DELAY;
	private long mTrafficChannelFadeTimeout = DecodeConfiguration.DEFAULT_CALL_TIMEOUT_SECONDS * 1000;
	private long mFadeTimeout;
	private long mEndTimeout;
	
	private StateMonitor mStateMonitor = new StateMonitor();
	private ScheduledFuture<?> mStateMonitorFuture;

	private ChannelType mChannelType;
	
	private TrafficChannelManager mTrafficChannelEndListener;
	private CallEvent mTrafficChannelCallEvent;
	
	/**
	 * Channel state tracks the overall state of all processing modules and decoders
	 * configured for the channel and provides squelch control and decoder state
	 * reset events.
	 * 
	 * Uses a state enumeration that defines allowable channel state transitions in
	 * order to track a call or data decode event from start to finish.  Uses a 
	 * timer to monitor for inactivity and to provide a FADE period that indicates
	 * to the user that the activity has stopped while continuing to provide details
	 * about the call, before the state is reset to IDLE.
	 * 
	 * Since this class is multi-threaded between decoder events and the internal
	 * timer that monitors for inactivity, this class uses the mState object for
	 * thread synchronization.
	 * 
	 * State Descriptions:
	 *  IDLE		Normal state. No voice or data call activity
	 *  CALL/DATA/ENCRYPTED/CONTROL
	 *  			Decoding states.
	 *  FADE		The phase after a voice or data call when either an explicit
	 *  			call end has been received, or when no new signalling updates
	 *  			have been received, and the fade timer has expired.  This phase
	 *  			allows for gui updates to signal to the user that the call is
	 *  			ended, while continuing to display the call details for the user
	 *  TEARDOWN	Indicates a traffic channel that will be torn down for reuse.
	 */
	public ChannelState( ChannelType channelType )
	{
		mChannelType = channelType;
	}


    /**
     * Metadata for this channel containing channel details, primary and secondary entities for call events and any
     * decoded messages.  This metadata is primarily intended to be used by graphical components to convey the
     * details of the current channel state and to provide metadata backing any decoded audio packets produced by
     * any audio streams for this channel.
     */
	public MutableMetadata getMutableMetadata()
    {
        return mMutableMetadata;
    }

	/**
	 * Resets this channel state and prepares it for reuse.
	 */
	@Override
	public void reset()
	{
		broadcast( new DecoderStateEvent( this, Event.RESET, State.IDLE ) );

		mMutableMetadata.resetTemporalAttributes();

		mState = State.IDLE;
		broadcast( Attribute.CHANNEL_STATE );
	}

	@Override
	public void start( ScheduledExecutorService executor )
	{
		if( mStateMonitorFuture == null && mStateMonitor != null )
		{
		    mMutableMetadata.resetAllAttributes();

			if( mTrafficChannelEndListener != null )
			{
				setState( State.CALL );
			}
			
			try
			{
				mStateMonitorFuture = executor.scheduleAtFixedRate(  mStateMonitor, 0, 20, TimeUnit.MILLISECONDS );
			}
			catch( RejectedExecutionException ree )
			{
				mLog.error( "state monitor scheduled task rejected", ree );
			}
		}
	}

	@Override
	public void stop()
	{
		if( mStateMonitorFuture != null )
		{
			boolean success = mStateMonitorFuture.cancel( true );
			
			if( !success )
			{
				mLog.error("Couldn't stop monitoring scheduled future" );
			}
		}

		mTrafficChannelEndListener = null;
		
		if( mTrafficChannelCallEvent != null )
		{
			mTrafficChannelCallEvent.end();
			broadcast( mTrafficChannelCallEvent );
		}
		
		mTrafficChannelCallEvent = null;
		
		mStateMonitorFuture = null;
		
		mSquelchLocked = false;
	}

	public void dispose()
	{
		mCallEventListener = null;
		mChangedAttributeListener = null;
		mDecoderStateListener = null;
		mSquelchStateListener = null;
		mStateMonitor = null;
	}
	
	private boolean isStandardChannel()
	{
		return mChannelType == ChannelType.STANDARD;
	}
	
	private boolean isTrafficChannel()
	{
		return mChannelType == ChannelType.TRAFFIC;
	}
	
	public void setStandardChannelTimeout( long milliseconds )
	{
		mStandardChannelFadeTimeout = milliseconds;
		
		if( mChannelType == ChannelType.STANDARD )
		{
			mFadeTimeout = mStandardChannelFadeTimeout;
		}
	}
	
	public void setTrafficChannelTimeout( long milliseconds )
	{
		mTrafficChannelFadeTimeout = milliseconds;
		
		if( mChannelType == ChannelType.TRAFFIC )
		{
			mFadeTimeout = System.currentTimeMillis() + mTrafficChannelFadeTimeout;
		}
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
	
	/**
	 * Updates the fade timeout threshold to the current time plus delay
	 */
	private void updateFadeTimeout()
	{
		if( isTrafficChannel() )
		{
			mFadeTimeout = System.currentTimeMillis() + mTrafficChannelFadeTimeout;
		}
		else
		{
			mFadeTimeout = System.currentTimeMillis() + mStandardChannelFadeTimeout;
		}
		
	}

	/**
	 * Updates the reset timeout threshold to the current time plus delay
	 */
	private void updateResetTimeout()
	{
		if( isTrafficChannel() )
		{
			mEndTimeout = System.currentTimeMillis();
		}
		else
		{
			mEndTimeout = System.currentTimeMillis() + RESET_TIMEOUT_DELAY;
		}
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
		synchronized( mState )
		{
			if( state == mState )
			{
				if( State.CALL_STATES.contains( state ) )
				{
					updateFadeTimeout();
				}
			}
			else if( state != mState && mState.canChangeTo( state ) )
			{
				switch( state )
				{
					case CONTROL:
						//Don't allow traffic channels to be control channels, 
						//otherwise they can't transition to call tear down
						if( isStandardChannel() )
						{
							broadcast( SquelchState.SQUELCH );
							updateFadeTimeout();
							mState = state;
							broadcast( Attribute.CHANNEL_STATE );
						}
						break;
					case DATA:
					case ENCRYPTED:
						broadcast( SquelchState.SQUELCH );
						updateFadeTimeout();
						mState = state;
						broadcast( Attribute.CHANNEL_STATE );
						break;
					case CALL:
						broadcast( SquelchState.UNSQUELCH );
						updateFadeTimeout();
						mState = state;
						broadcast( Attribute.CHANNEL_STATE );
						break;
					case FADE:
						processFadeState();
						break;
					case IDLE:
						processIdleState();
						break;
					case TEARDOWN:
						processTeardownState();
						break;
					case RESET:
						mState = State.IDLE;
						break;
					default:
						break;
				}
			}
			else
			{
//				mLog.debug( "Can't change from [" + mState + "] to [" + state + "]" );
			}
		}
	}
	
	/**
	 * Sets the state and processes related actions
	 */
	private void processFadeState()
	{
		updateResetTimeout();
		mState = State.FADE;
		
		broadcast( SquelchState.SQUELCH );
		broadcast( Attribute.CHANNEL_STATE );
	}
	
	private void processIdleState()
	{
		broadcast( SquelchState.SQUELCH );
		
		if( mState == State.FADE )
		{
			broadcast( new DecoderStateEvent( this, Event.RESET, State.IDLE ) );
			broadcast( new MetadataReset() );
		}
		
		mState = State.IDLE;
		
		broadcast( Attribute.CHANNEL_STATE );
	}
	
	private void processTeardownState()
	{
		broadcast( SquelchState.SQUELCH );

		broadcast( new MetadataReset() );

		mState = State.TEARDOWN;
		
		broadcast( Attribute.CHANNEL_STATE );

		if( mTrafficChannelEndListener != null )
		{
			mTrafficChannelEndListener.callEnd( mTrafficChannelCallEvent.getChannel() );
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
	public void addCallEventListener( Listener<CallEvent> listener )
	{
		mCallEventListener = listener;
	}

	@Override
	public void removeCallEventListener( Listener<CallEvent> listener )
	{
		mCallEventListener = null;
	}



	//NEW:
    @Override
    public Listener<AttributeChangeRequest> getAttributeChangeRequestListener()
    {
        return mMutableMetadata;
    }

    /**
	 * Broadcasts the channel state attribute change event to all registered
	 * listeners
	 */
	protected void broadcast( Attribute attribute )
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
	public void setChangedAttributeListener( Listener<Attribute> listener )
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
	public void configureAsTrafficChannel( TrafficChannelManager manager, CallEvent callEvent )
	{
		mTrafficChannelEndListener = manager;
		
		mTrafficChannelCallEvent = callEvent;
		
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
			broadcast( new Metadata( MetadataType.FREQUENCY, String.valueOf( frequency ), true ) );
		}
		
		/* Rebroadcast the allocation event so that the internal decoder states
		 * can self-configure with the call event details */
		broadcast( mTrafficChannelCallEvent );
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
					case CHANGE_CALL_TIMEOUT:
						if( event instanceof ChangeChannelTimeoutEvent )
						{
							ChangeChannelTimeoutEvent timeout = (ChangeChannelTimeoutEvent)event;
							
							if( timeout.getChannelType() == ChannelType.STANDARD )
							{
								setStandardChannelTimeout( timeout.getCallTimeout() );
							}
							else
							{
								setTrafficChannelTimeout( timeout.getCallTimeout() );
							}
						}
					case CONTINUATION:
					case DECODE:
					case START:
						if( State.CALL_STATES.contains( event.getState() ) )
						{
							setState( event.getState() );
						}
						break;
					case END:
						if( isTrafficChannel() )
						{
							setState( State.TEARDOWN );
						}
						else
						{
							setState( State.FADE );
						}
						break;
					case RESET:
						/* Channel State does not respond to reset events */
						break;
					default:
						break;
				}
			}
		}
	}
	
	/**
	 * Monitors decoder state events to automatically transition the channel
	 * state to IDLE (standard channel) or to TEARDOWN (traffic channel) when
	 * decoding stops or the monitored channel returns to a no signal state.
	 * 
	 * Provides a FADE transition state to allow for momentary decoding dropouts 
	 * and to allow the user access to call details for a fade period upon 
	 * call end.
	 */
	public class StateMonitor implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				synchronized( mState )
				{
					if( State.CALL_STATES.contains( mState ) &&
						mFadeTimeout <= System.currentTimeMillis() )
					{
						processFadeState();
					}
					else if( mState == State.FADE && 
							 mEndTimeout <= System.currentTimeMillis() )
					{
						if( isTrafficChannel() )
						{
							processTeardownState();
						}
						else
						{
							processIdleState();
						}
					}
				}
			}
			catch( Exception e )
			{
				mLog.error( "Exception thrown while state monitor is running " +
						"- state [" + getState() + 
						"] current [" + System.currentTimeMillis() +
						"] mResetTimeout [" + mEndTimeout +
						"] mFadeTimeout [" + mFadeTimeout + 
						"]", e );
			}
		}
	}
}
