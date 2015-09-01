package module.decode.state;

import message.Message;
import module.decode.event.ActivitySummaryProvider;
import module.decode.event.CallEvent;
import module.decode.event.ICallEventProvider;
import sample.Listener;
import alias.AliasList;

/**
 * Channel state monitors the stream of decoded messages produced by the 
 * decoder and broadcasts call events as they occur within the decoded message activity.
 * 
 * Provides access to a textual activity summary of events observed.
 */
public abstract class DecoderState implements ActivitySummaryProvider,
								   			  Listener<Message>,
								   			  ICallEventProvider,
								   			  IChangedAttributeProvider, 
								   			  IDecoderStateEventListener,
								   			  IDecoderStateEventProvider
{
	private Listener<CallEvent> mCallEventListener;
	private Listener<ChangedAttribute> mChangedAttributeListener;
	private Listener<DecoderStateEvent> mDecoderStateListener;
	
	private DecoderStateEventListener mDecoderStateEventListener = 
										new DecoderStateEventListener();
	
	protected CallEvent mCurrentCallEvent;
	
	private AliasList mAliasList;
	
	public DecoderState( AliasList aliasList )
	{
		mAliasList = aliasList;
	}
	
	public AliasList getAliasList()
	{
		return mAliasList;
	}
	
	public boolean hasAliasList()
	{
		return mAliasList != null;
	}
	
	/**
	 * Reset the decoder state to prepare for processing a different sample
	 * source
	 */
	public abstract void reset();

	/**
	 * Allow the decoder to perform any setup actions
	 */
	public abstract void init();

	/**
	 * Implements the IDecoderStateEventListener interface to receive state
	 * reset events.
	 */
	public abstract void receiveDecoderStateEvent( DecoderStateEvent event );
	
	/**
	 * Disposes any resources or pointers held by this instance to prepare for
	 * garbage collection
	 */
	public void dispose()
	{
		mCallEventListener = null;
		mChangedAttributeListener = null;
		mDecoderStateListener = null;

		mAliasList = null;
	}
	
	/**
	 * Activity Summary - textual summary of activity observed by the channel state.
	 */
	public abstract String getActivitySummary();

	/**
	 * Broadcasts a call event to any registered listeners
	 */
	protected void broadcast( CallEvent event )
	{
		if( mCallEventListener != null )
		{
			mCallEventListener.receive( event );
		}
	}

	/**
	 * Adds a call event listener
	 */
	@Override
	public void setCallEventListener( Listener<CallEvent> listener )
	{
		mCallEventListener = listener;
	}

	/**
	 * Removes the call event listener
	 */
	@Override
	public void removeCallEventListener()
	{
		mCallEventListener = null;
	}

	@Override
	public Listener<DecoderStateEvent> getDecoderStateListener()
	{
		return mDecoderStateEventListener;
	}
	
	private class DecoderStateEventListener implements Listener<DecoderStateEvent>
	{
		@Override
		public void receive( DecoderStateEvent event )
		{
			receiveDecoderStateEvent( event );
		}
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
}
