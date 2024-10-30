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

import io.github.dsheirer.audio.squelch.ISquelchStateProvider;
import io.github.dsheirer.channel.metadata.ChannelMetadata;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.IdentifierUpdateProvider;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.sample.IOverflowListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.heartbeat.Heartbeat;
import io.github.dsheirer.source.heartbeat.IHeartbeatListener;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractChannelState extends Module implements IChannelEventProvider, IDecodeEventProvider,
    IDecoderStateEventProvider, ISourceEventProvider, IHeartbeatListener, ISquelchStateProvider,
    IdentifierUpdateProvider, IOverflowListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractChannelState.class);

    protected Listener<ChannelEvent> mChannelEventListener;
    protected Listener<IDecodeEvent> mDecodeEventListener;
    protected Listener<DecoderStateEvent> mDecoderStateListener;
    protected Listener<SourceEvent> mExternalSourceEventListener;
    private Channel mChannel;
    protected boolean mSourceOverflow = false;
    private HeartbeatReceiver mHeartbeatReceiver = new HeartbeatReceiver();
    protected boolean mTeardownSequenceStarted = false;
    protected boolean mTeardownSequenceCompleted = false;

    //TODO: remove the IOverflowListener code from this class

    /**
     * Constructs an instance
     * @param channel configuration
     */
    public AbstractChannelState(Channel channel)
    {
        mChannel = channel;
    }

    /**
     * Indicates if the teardown sequence was started.
     */
    public boolean isTeardownSequenceCompleted()
    {
        return mTeardownSequenceCompleted;
    }

    /**
     * Indicates if the teardown sequence was completed, meaning that the request disable channel event was dispatched.
     */
    public boolean isTeardownSequenceStarted()
    {
        return mTeardownSequenceStarted;
    }

    /**
     * Updates/replaces the current channel configuration with the argument.
     */
    protected void updateChannelConfiguration(Channel channel)
    {
        mChannel = channel;
    }

    /**
     * Channel configuration for this channel state
     */
    protected Channel getChannel()
    {
        return mChannel;
    }

    /**
     * Invoked each time that a heartbeat is received so that sub-class implementations can check current timers and
     * adjust channel state as necessary.  The heartbeat arrives on a periodic basis independent of any decoded
     * messages so that channel state is not entirely dependent on a continuous decoded message stream.
     */
    protected abstract void checkState();

    /**
     * Indicates if any timeslot is currently in a TEARDOWN state.
     */
    public abstract boolean isTeardownState();

    public abstract List<ChannelMetadata> getChannelMetadata();

    public abstract void updateChannelStateIdentifiers(IdentifierUpdateNotification notification);

    /**
     * Receiver inner class that implements the IHeartbeatListener interface to receive heartbeat messages.
     */
    @Override
    public Listener<Heartbeat> getHeartbeatListener()
    {
        return mHeartbeatReceiver;
    }

    /**
     * This method is invoked if the source buffer provider goes into overflow state.  Since this is an external state,
     * we use the mSourceOverflow variable to override the internal state reported to external listeners.
     *
     * @param overflow true to indicate an overflow state
     */
    @Override
    public void sourceOverflow(boolean overflow)
    {
        mSourceOverflow = overflow;
    }

    /**
     * Indicates if this channel's sample buffer is in overflow state, meaning that the inbound sample
     * stream is not being processed fast enough and samples are being thrown away until the processing can
     * catch up.
     *
     * @return true if the channel is in overflow state.
     */
    public boolean isOverflow()
    {
        return mSourceOverflow;
    }

    @Override
    public void setChannelEventListener(Listener<ChannelEvent> listener)
    {
        mChannelEventListener = listener;
    }

    @Override
    public void removeChannelEventListener()
    {
        mChannelEventListener = null;
    }

    @Override
    public void addDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventListener = listener;
    }

    @Override
    public void removeDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventListener = null;
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
     * Registers the listener to receive source events from the channel state
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mExternalSourceEventListener = listener;
    }

    /**
     * De-Registers a listener from receiving source events from the channel state
     */
    @Override
    public void removeSourceEventListener()
    {
        mExternalSourceEventListener = null;
    }

    /**
     * Processes periodic heartbeats received from the processing chain to perform state monitoring and cleanup
     * functions.
     *
     * Monitors decoder state events to automatically transition the channel state to IDLE (standard channel) or to
     * TEARDOWN (traffic channel) when decoding stops or the monitored channel returns to a no signal state.
     *
     * Provides a FADE transition state to allow for momentary decoding dropouts and to allow the user access to call
     * details for a fade period upon call end.
     */
    public class HeartbeatReceiver implements Listener<Heartbeat>
    {
        @Override
        public void receive(Heartbeat heartbeat)
        {
            checkState();
        }
    }
}
