package channel.state;

import channel.metadata.Attribute;
import message.IMessageListener;
import message.Message;
import module.Module;
import module.decode.DecoderType;
import module.decode.event.ActivitySummaryProvider;
import module.decode.event.CallEvent;
import module.decode.event.ICallEventProvider;
import sample.Broadcaster;
import sample.Listener;
import alias.AliasList;
import audio.metadata.IMetadataProvider;
import audio.metadata.Metadata;

/**
 * Channel state monitors the stream of decoded messages produced by the 
 * decoder and broadcasts call events as they occur within the decoded message activity.
 * 
 * Provides access to a textual activity summary of events observed.
 */
public abstract class DecoderState extends Module 
			implements ActivitySummaryProvider,
					   Listener<Message>,
					   ICallEventProvider,
					   IChangedAttributeProvider, 
					   IDecoderStateEventListener,
					   IDecoderStateEventProvider,
					   IMessageListener,
					   IMetadataProvider
{
	/* This has to be a broadcaster in order for references to persist */
	private Broadcaster<CallEvent> mCallEventBroadcaster = new Broadcaster<>();
	private Listener<Attribute> mChangedAttributeListener;
	private Listener<DecoderStateEvent> mDecoderStateListener;
	private Listener<Metadata> mMetadataListener;
	
	private DecoderStateEventListener mDecoderStateEventListener = 
										new DecoderStateEventListener();
	
	protected CallEvent mCurrentCallEvent;
	
	private AliasList mAliasList;
	
	public DecoderState( AliasList aliasList )
	{
		mAliasList = aliasList;
	}
	
	public abstract DecoderType getDecoderType();
	
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
		mCallEventBroadcaster.dispose();
		mCallEventBroadcaster = null;
		mChangedAttributeListener = null;
		mDecoderStateListener = null;
		mMetadataListener = null;

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
		mCallEventBroadcaster.broadcast( event );
	}

	/**
	 * Adds a call event listener
	 */
	@Override
	public void addCallEventListener( Listener<CallEvent> listener )
	{
		mCallEventBroadcaster.addListener( listener );
	}

	/**
	 * Removes the call event listener
	 */
	@Override
	public void removeCallEventListener( Listener<CallEvent> listener )
	{
		mCallEventBroadcaster.removeListener( listener );
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

	/**
	 * Broadcasts metadata to a registered listener
	 */
	protected void broadcast( Metadata metadata )
	{
		if( mMetadataListener != null )
		{
			mMetadataListener.receive( metadata );
		}
	}

	@Override
	public void setMetadataListener( Listener<Metadata> listener )
	{
		mMetadataListener = listener;
	}

	@Override
	public void removeMetadataListener()
	{
		mMetadataListener = null;
	}

	@Override
	public Listener<Message> getMessageListener()
	{
		return this;
	}
}
