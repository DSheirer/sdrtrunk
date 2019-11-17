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

import io.github.dsheirer.audio.squelch.ISquelchStateProvider;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.sample.Listener;

public class StateMonitoringSquelchController implements IStateMachineListener, ISquelchStateProvider
{
    private boolean mSquelchLocked;
    private SquelchState mSquelchState = SquelchState.SQUELCH;
    private Listener<SquelchStateEvent> mSquelchStateListener;
    private int mTimeslot;

    public StateMonitoringSquelchController(int timeslot)
    {
        mTimeslot = timeslot;
    }

    public void setSquelchStateListener(Listener<SquelchStateEvent> listener)
    {
        mSquelchStateListener = listener;
    }

    public void removeSquelchStateListener()
    {
        mSquelchStateListener = null;
    }

    public void setSquelchLock(boolean locked)
    {
        mSquelchLocked = locked;
        setSquelchState(mSquelchLocked ? SquelchState.UNSQUELCH : SquelchState.SQUELCH);
    }

    private void setSquelchState(SquelchState squelchState)
    {
        if(mSquelchState != squelchState)
        {
            mSquelchState = squelchState;

            if(mSquelchStateListener != null)
            {
                mSquelchStateListener.receive(new SquelchStateEvent(squelchState, mTimeslot));
            }
        }
    }

    @Override
    public void stateChanged(State state, int timeslot)
    {
        if(!mSquelchLocked)
        {
            if(state == State.CALL)
            {
                setSquelchState(SquelchState.UNSQUELCH);
            }
            else
            {
                setSquelchState(SquelchState.SQUELCH);
            }
        }
    }
}
