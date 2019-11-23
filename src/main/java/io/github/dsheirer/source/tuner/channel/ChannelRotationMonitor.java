/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.source.tuner.channel;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventListener;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors channel state to detect when a channel is not in an identified active state and issues a request to rotate
 * the next channel frequency in the list.  This class depends on the ChannelState to provide a continuous
 * stream of channel active/inactive messaging in the form of DecoderStateEvents.
 */
public class ChannelRotationMonitor extends Module implements ISourceEventProvider, IDecoderStateEventListener,
        Listener<DecoderStateEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelRotationMonitor.class);

    private UserPreferences mUserPreferences;
    private Collection<State> mActiveStates;
    private ScheduledFuture<?> mScheduledFuture;
    private Listener<SourceEvent> mSourceEventListener;
    private long mRotationDelay;
    private boolean mActive;
    private long mInactiveTimestamp = System.currentTimeMillis();

    /**
     * Constructs a channel rotation monitor.
     *
     * @param activeStates to monitor
     * @param userPreferences to receive updates for rotation delay value from user preferences
     */
    public ChannelRotationMonitor(Collection<State> activeStates, UserPreferences userPreferences)
    {
        mActiveStates = activeStates;
        mUserPreferences = userPreferences;
        mRotationDelay = mUserPreferences.getChannelMultiFrequencyPreference().getRotationDelay();

        //Register to receive user preference updates
        MyEventBus.getEventBus().register(this);
    }

    /**
     * Registers the external listener to receive frequency rotation requests from this module
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventListener = listener;
    }

    /**
     * Unregisters the external listener from receiving frequency rotation requests.
     */
    @Override
    public void removeSourceEventListener()
    {
        mSourceEventListener = null;
    }

    @Override
    public Listener<DecoderStateEvent> getDecoderStateListener()
    {
        return this;
    }

    @Override
    public void receive(DecoderStateEvent decoderStateEvent)
    {
        if(decoderStateEvent.getEvent() == DecoderStateEvent.Event.NOTIFICATION_CHANNEL_ACTIVE_STATE)
        {
            mActive = true;
        }
        else if(decoderStateEvent.getEvent() == DecoderStateEvent.Event.NOTIFICATION_CHANNEL_INACTIVE_STATE)
        {
            if(mActive)
            {
                //If we're toggling from active to inactive, update the timestamp,
                mInactiveTimestamp = System.currentTimeMillis();
            }

            mActive = false;
        }
    }

    /**
     * Checks the current active state and when inactive for longer than the specified delay, issues a
     * channel frequency rotation request
     */
    private void checkState()
    {
        if(mSourceEventListener != null && !mActive &&
            ((mInactiveTimestamp + mRotationDelay) < System.currentTimeMillis()))
        {
            mSourceEventListener.receive(SourceEvent.frequencyRotationRequest());
            mInactiveTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Guava event bus method to receive notifications when user preferences are updated.  This allows us to
     * dynamically adjust rotation delay if/when the user changes the rotation delay value.
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.MULTI_FREQUENCY)
        {
            mRotationDelay = mUserPreferences.getChannelMultiFrequencyPreference().getRotationDelay();
        }
    }

    @Override
    public void reset()
    {

    }

    @Override
    public void start()
    {
        if(mScheduledFuture == null)
        {
            mScheduledFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(() -> checkState(), 1, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop()
    {
        if(mScheduledFuture != null)
        {
            mScheduledFuture.cancel(true);
        }
    }

    @Override
    public void dispose()
    {
    }
}

