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

package io.github.dsheirer.channel.state;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.decoder.ChannelStateIdentifier;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * State machine for tracking a channel state.
 */
public class StateMachine
{
    private final static Logger mLog = LoggerFactory.getLogger(StateMachine.class);

    protected State mState = State.IDLE;
    protected long mFadeTimeout;
    protected long mFadeTimeoutBuffer = 0;
    protected long mEndTimeout;
    protected long mEndTimeoutBuffer = 0;
    private int mTimeslot;
    private EnumSet<State> mActiveStates;
    private Channel.ChannelType mChannelType = Channel.ChannelType.STANDARD;
    private List<IStateMachineListener> mStateMachineListeners = new ArrayList<>();
    private Listener<IdentifierUpdateNotification> mIdentifierUpdateListener;

    /**
     * Constructs an instance
     *
     * @param timeslot for this state machine
     * @param activeStates set of states considered active for updating the fade timeout
     */
    public StateMachine(int timeslot, EnumSet<State> activeStates)
    {
        mTimeslot = timeslot;
        mActiveStates = activeStates;
    }

    public void addListener(IStateMachineListener listener)
    {
        mStateMachineListeners.add(listener);
    }

    public void removeListener(IStateMachineListener listener)
    {
        mStateMachineListeners.remove(listener);
    }

    public void setIdentifierUpdateListener(Listener<IdentifierUpdateNotification> listener)
    {
        mIdentifierUpdateListener = listener;
    }

    public void setChannelType(Channel.ChannelType channelType)
    {
        mChannelType = channelType;
    }

    public void checkState()
    {
        if(mActiveStates.contains(mState) && mFadeTimeout <= System.currentTimeMillis())
        {
            setState(State.FADE);
        }
        else if(mState == State.FADE && mEndTimeout <= System.currentTimeMillis())
        {
            setState(State.TEARDOWN);
        }
    }

    public State getState()
    {
        return mState;
    }

    public void setState(State state)
    {
        if(state == mState)
        {
            if(mActiveStates.contains(state))
            {
                updateFadeTimeout();
            }
        }
        else if(mState.canChangeTo(state))
        {
            if(mActiveStates.contains(state))
            {
                updateFadeTimeout();
            }

            switch(state)
            {
                case ACTIVE:
                    mState = state;
                    broadcast(ChannelStateIdentifier.ACTIVE);
                    break;
                case CONTROL:
                    //Don't allow traffic channels to be control channels, otherwise they can't transition to teardown
                    if(mChannelType == Channel.ChannelType.STANDARD)
                    {
                        mState = state;
                        broadcast(ChannelStateIdentifier.CONTROL);
                    }
                    break;
                case DATA:
                    mState = state;
                    broadcast(ChannelStateIdentifier.DATA);
                    break;
                case ENCRYPTED:
                    mState = state;
                    broadcast(ChannelStateIdentifier.ENCRYPTED);
                    break;
                case CALL:
                    mState = state;
                    broadcast(ChannelStateIdentifier.CALL);
                    break;
                case FADE:
                    mState = state;
                    broadcast(ChannelStateIdentifier.FADE);
                    break;
                case IDLE:
                    mState = state;
                    broadcast(ChannelStateIdentifier.IDLE);
                    break;
                case TEARDOWN:
                    mState = state;
                    broadcast(ChannelStateIdentifier.TEARDOWN);
                    break;
                case RESET:
                    mState = State.RESET;
                    broadcast(ChannelStateIdentifier.RESET);
                    break;
                default:
                    break;
            }

            //If the state successfully changed to the new state, announce it
            if(mState == state)
            {
                for(IStateMachineListener listener: mStateMachineListeners)
                {
                    listener.stateChanged(mState, mTimeslot);
                }
            }
        }
    }

    private void broadcast(ChannelStateIdentifier channelStateIdentifier)
    {
        if(mIdentifierUpdateListener != null)
        {
            mIdentifierUpdateListener.receive(new IdentifierUpdateNotification(channelStateIdentifier,
                IdentifierUpdateNotification.Operation.ADD, mTimeslot));
        }
    }

    private void updateFadeTimeout()
    {
        mFadeTimeout = System.currentTimeMillis() + mFadeTimeoutBuffer;
    }

    public void setFadeTimeout(long timeout)
    {
        mFadeTimeout = timeout;
    }

    public long getFadeTimeout()
    {
        return mFadeTimeout;
    }

    public void setFadeTimeoutBuffer(long buffer)
    {
        mFadeTimeoutBuffer = buffer;
        updateFadeTimeout();
    }

    private void updateEndTimeout()
    {
        mEndTimeout = System.currentTimeMillis() + mEndTimeoutBuffer;
    }

    public void setEndTimeoutBuffer(long buffer)
    {
        mEndTimeoutBuffer = buffer;
        updateEndTimeout();
    }

    public void setEndTimeout(long endTimeout)
    {
        mEndTimeout = endTimeout;
    }

    public long getEndTimeout()
    {
        return mEndTimeout;
    }
}
