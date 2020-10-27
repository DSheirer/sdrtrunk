/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.channel.rotation;

import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.module.ModuleEventBusMessage;

/**
 * Request to add a channel state to the list of active channel states monitored by the channel rotation monitor.
 */
public class AddChannelRotationActiveStateRequest extends ModuleEventBusMessage
{
    private State mState;

    /**
     * Constructs an instance
     * @param state to add
     */
    public AddChannelRotationActiveStateRequest(State state)
    {
        mState = state;
    }

    /**
     * State to add to the list of active states
     * @return state
     */
    public State getState()
    {
        return mState;
    }
}
