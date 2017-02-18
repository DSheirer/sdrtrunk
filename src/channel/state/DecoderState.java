package channel.state;

import alias.AliasList;
import channel.metadata.AttributeChangeRequest;
import channel.metadata.IAttributeChangeRequestProvider;
import message.IMessageListener;
import message.Message;
import module.Module;
import module.decode.DecoderType;
import module.decode.event.ActivitySummaryProvider;
import module.decode.event.CallEvent;
import module.decode.event.ICallEventProvider;
import sample.Broadcaster;
import sample.Listener;

/**
 * Channel state monitors the stream of decoded messages produced by the
 * decoder and broadcasts call events as they occur within the decoded message activity.
 *
 * Provides access to a textual activity summary of events observed.
 */
public abstract class DecoderState extends Module implements ActivitySummaryProvider, Listener<Message>,
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

    @Override
    public Listener<Message> getMessageListener()
    {
        return this;
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
