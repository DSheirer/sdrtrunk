package io.github.dsheirer.channel.state;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.channel.metadata.AttributeChangeRequest;
import io.github.dsheirer.channel.metadata.IAttributeChangeRequestProvider;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.ActivitySummaryProvider;
import io.github.dsheirer.module.decode.event.CallEvent;
import io.github.dsheirer.module.decode.event.ICallEventProvider;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;

/**
 * Channel state monitors the stream of decoded messages produced by the
 * decoder and broadcasts call events as they occur within the decoded message activity.
 *
 * Provides access to a textual activity summary of events observed.
 */
public abstract class DecoderState extends Module implements ActivitySummaryProvider, Listener<IMessage>,
        IAttributeChangeRequestProvider, ICallEventProvider, IDecoderStateEventListener,
        IDecoderStateEventProvider, IMessageListener
{
    protected String DIVIDER1 = "======================================================\n";
    protected String DIVIDER2 = "------------------------------------------------------\n";

    /* This has to be a broadcaster in order for references to persist */
    private Broadcaster<CallEvent> mCallEventBroadcaster = new Broadcaster<>();
    private Broadcaster<AttributeChangeRequest> mAttributeChangeRequestBroadcaster = new Broadcaster<>();
    private Listener<DecoderStateEvent> mDecoderStateListener;

    private DecoderStateEventListener mDecoderStateEventListener = new DecoderStateEventListener();

    protected CallEvent mCurrentCallEvent;

    private AliasList mAliasList;

    public DecoderState(AliasList aliasList)
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
     * Provides subclass reference to the call event broadcaster
     */
    protected Broadcaster<CallEvent> getCallEventBroadcaster()
    {
        return mCallEventBroadcaster;
    }


    @Override
    public Listener<IMessage> getMessageListener()
    {
        return this;
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
    public abstract void receiveDecoderStateEvent(DecoderStateEvent event);

    /**
     * Disposes any resources or pointers held by this instance to prepare for
     * garbage collection
     */
    public void dispose()
    {
        mCallEventBroadcaster.dispose();
        mCallEventBroadcaster = null;
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
    protected void broadcast(CallEvent event)
    {
        mCallEventBroadcaster.broadcast(event);
    }

    /**
     * Adds a call event listener
     */
    @Override
    public void addCallEventListener(Listener<CallEvent> listener)
    {
        mCallEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the call event listener
     */
    @Override
    public void removeCallEventListener(Listener<CallEvent> listener)
    {
        mCallEventBroadcaster.removeListener(listener);
    }

    @Override
    public Listener<DecoderStateEvent> getDecoderStateListener()
    {
        return mDecoderStateEventListener;
    }

    private class DecoderStateEventListener implements Listener<DecoderStateEvent>
    {
        @Override
        public void receive(DecoderStateEvent event)
        {
            receiveDecoderStateEvent(event);
        }
    }

    /**
     * Broadcasts a channel state event to any registered listeners
     */
    protected void broadcast(DecoderStateEvent event)
    {
        if(mDecoderStateListener != null)
        {
            mDecoderStateListener.receive(event);
        }
    }

    /**
     * Adds a decoder state event listener
     */
    @Override
    public void setDecoderStateListener(Listener<DecoderStateEvent> listener)
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
     * Broadcasts the attribute change request to the registered listener
     */
    protected void broadcast(AttributeChangeRequest<?> request)
    {
        mAttributeChangeRequestBroadcaster.broadcast(request);
    }

    /**
     * Sets the listener to receive attribute change requests from this decoder state
     */
    @Override
    public void setAttributeChangeRequestListener(Listener<AttributeChangeRequest> listener)
    {
        mAttributeChangeRequestBroadcaster.addListener(listener);
    }

    /**
     * Removes any listener from receiving attribute change requests
     */
    @Override
    public void removeAttributeChangeRequestListener(Listener<AttributeChangeRequest> listener)
    {
        mAttributeChangeRequestBroadcaster.removeListener(listener);
    }

    /**
     * Broadaster so that Attribute monitors can broadcast attribute change requests
     */
    protected Listener<AttributeChangeRequest> getAttributeChangeRequestListener()
    {
        return mAttributeChangeRequestBroadcaster;
    }
}
