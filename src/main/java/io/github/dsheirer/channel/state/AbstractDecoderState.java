/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.channel.state;

import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateProvider;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.ActivitySummaryProvider;
import io.github.dsheirer.module.decode.event.DecodeEventDuplicateDetector;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDecoderState extends Module implements ActivitySummaryProvider, Listener<IMessage>,
    IDecodeEventProvider, IDecoderStateEventListener, IDecoderStateEventProvider, IMessageListener,
    IdentifierUpdateProvider, IdentifierUpdateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractDecoderState.class);
    protected String DIVIDER1 = "======================================================\n";
    protected String DIVIDER2 = "------------------------------------------------------\n";
    /* This has to be a broadcaster in order for references to persist */
    protected Broadcaster<IDecodeEvent> mDecodeEventBroadcaster = new Broadcaster<>();
    protected Listener<DecoderStateEvent> mDecoderStateListener;
    private DecoderStateEventListener mDecoderStateEventListener = new DecoderStateEventListener();
    private static final DecodeEventDuplicateDetector mDuplicateEventDetector = new DecodeEventDuplicateDetector();
    private boolean mRunning;

    public abstract DecoderType getDecoderType();

    /**
     * Implements module start and sets the mRunning flag to true so that messages can be processed.
     */
    @Override
    public void start()
    {
        mRunning = true;
    }

    /**
     * Implements the module stop and sets the mRunning flag to false to stop message processing
     */
    @Override
    public void stop()
    {
        mRunning = false;
    }

    /**
     * Indicates if this module is running and can process/pass messages down to sub-class implementations.
     * @return true if running
     */
    public boolean isRunning()
    {
        return mRunning;
    }

    /**
     * Provides subclass reference to the decode event broadcaster
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
     * Implements the IDecoderStateEventListener interface to receive state
     * reset events.
     */
    public abstract void receiveDecoderStateEvent(DecoderStateEvent event);

    /**
     * Activity Summary - textual summary of activity observed by the channel state.
     */
    public abstract String getActivitySummary();

    /**
     * Broadcasts a decode event to any registered listeners
     */
    protected void broadcast(IDecodeEvent event)
    {
        if(!mDuplicateEventDetector.isDuplicate(event, System.currentTimeMillis()))
        {
            mDecodeEventBroadcaster.broadcast(event);
        }
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
     * Wrapper that implements the IDecoderStateEventListener interface
     */
    private class DecoderStateEventListener implements Listener<DecoderStateEvent>
    {
        @Override
        public void receive(DecoderStateEvent event)
        {
            receiveDecoderStateEvent(event);
        }
    }
}
