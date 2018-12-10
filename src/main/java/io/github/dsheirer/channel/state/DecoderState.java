package io.github.dsheirer.channel.state;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.IdentifierUpdateProvider;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.configuration.ChannelDescriptorConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.DecoderTypeConfigurationIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.ActivitySummaryProvider;
import io.github.dsheirer.module.decode.event.CallEvent;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;

/**
 * Channel state monitors the stream of decoded messages produced by the
 * decoder and broadcasts call events as they occur within the decoded message activity.
 *
 * Provides access to a textual activity summary of events observed.
 */
public abstract class DecoderState extends Module implements ActivitySummaryProvider, Listener<IMessage>,
    IDecodeEventProvider, IDecoderStateEventListener, IDecoderStateEventProvider, IMessageListener,
    IdentifierUpdateProvider, IdentifierUpdateListener
{
//    private final static Logger mLog = LoggerFactory.getLogger(DecoderState.class);

    protected String DIVIDER1 = "======================================================\n";
    protected String DIVIDER2 = "------------------------------------------------------\n";

    /* This has to be a broadcaster in order for references to persist */
    private Broadcaster<IDecodeEvent> mDecodeEventBroadcaster = new Broadcaster<>();
    private Listener<DecoderStateEvent> mDecoderStateListener;
    private DecoderStateEventListener mDecoderStateEventListener = new DecoderStateEventListener();
    private MutableIdentifierCollection mIdentifierCollection = new MutableIdentifierCollection();
    private ConfigurationIdentifierListener mConfigurationIdentifierListener = new ConfigurationIdentifierListener();

    private IChannelDescriptor mCurrentChannel;
    protected CallEvent mCurrentCallEvent;

    public DecoderState()
    {
        mIdentifierCollection.update(new DecoderTypeConfigurationIdentifier(getDecoderType()));
    }

    @Override
    public void start()
    {
        //Broadcast the existing identifiers (as add events) so that they can be received by external listeners
        mIdentifierCollection.broadcastIdentifiers();
    }

    public abstract DecoderType getDecoderType();

    /**
     * Current collection of identifiers managed by the decoder state.
     */
    public MutableIdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Registers the listener to receive identifier update notifications
     */
    @Override
    public void setIdentifierUpdateListener(Listener<IdentifierUpdateNotification> listener)
    {
        getIdentifierCollection().setIdentifierUpdateListener(listener);
    }

    /**
     * Removes the listener from receiving identifier update notifications
     */
    @Override
    public void removeIdentifierUpdateListener()
    {
        getIdentifierCollection().removeIdentifierUpdateListener();
    }

    /**
     * Provides subclass reference to the call event broadcaster
     */
    protected Broadcaster<IDecodeEvent> getDecodeEventBroadcaster()
    {
        return mDecodeEventBroadcaster;
    }


    @Override
    public Listener<IMessage> getMessageListener()
    {
        return this;
    }

    /**
     * Resets the current temporal state variables to end the current event and prepare for the next.
     */
    protected void resetState()
    {
        mIdentifierCollection.remove(IdentifierClass.USER);
        mCurrentChannel = null;
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
        mDecodeEventBroadcaster.dispose();
        mDecodeEventBroadcaster = null;
        mDecoderStateListener = null;
    }

    /**
     * Activity Summary - textual summary of activity observed by the channel state.
     */
    public abstract String getActivitySummary();

    /**
     * Broadcasts a decode event to any registered listeners
     */
    protected void broadcast(IDecodeEvent event)
    {
        mDecodeEventBroadcaster.broadcast(event);
    }

    /**
     * Adds a call event listener
     */
    @Override
    public void addDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the call event listener
     */
    @Override
    public void removeDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventBroadcaster.removeListener(listener);
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
     * Listens for configuration identifiers and adds them to this decoder state's identifier collection.
     * @return
     */
    @Override
    public Listener<IdentifierUpdateNotification> getIdentifierUpdateListener()
    {
        return mConfigurationIdentifierListener;
    }

    /**
     * Configuration identifier listener.
     */
    public ConfigurationIdentifierListener getConfigurationIdentifierListener()
    {
        return mConfigurationIdentifierListener;
    }

    /**
     * Optional current channel descriptor
     */
    protected IChannelDescriptor getCurrentChannel()
    {
        return mCurrentChannel;
    }

    /**
     * Sets the current channel descriptor
     */
    protected void setCurrentChannel(IChannelDescriptor channel)
    {
        mCurrentChannel = channel;
    }

    /**
     * Listener for configuration type identifier updates sent from the channel state.  Adds configuration
     * identifiers to this decoder state so that decode events will contain configuration details in the
     * event's identifier collection.
     */
    public class ConfigurationIdentifierListener implements Listener<IdentifierUpdateNotification>
    {
        @Override
        public void receive(IdentifierUpdateNotification identifierUpdateNotification)
        {
            if(identifierUpdateNotification.getIdentifier().getIdentifierClass() == IdentifierClass.CONFIGURATION &&
               identifierUpdateNotification.getIdentifier().getForm() != Form.DECODER_TYPE &&
               identifierUpdateNotification.getIdentifier().getForm() != Form.CHANNEL_DESCRIPTOR)
            {
                getIdentifierCollection().update(identifierUpdateNotification.getIdentifier());
            }

            if(identifierUpdateNotification.getOperation() == IdentifierUpdateNotification.Operation.ADD)
            {
                Identifier identifier = identifierUpdateNotification.getIdentifier();

                if(identifier instanceof ChannelDescriptorConfigurationIdentifier)
                {
                    mCurrentChannel = ((ChannelDescriptorConfigurationIdentifier)identifier).getValue();
                }
            }
        }
    }
}
