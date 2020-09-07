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

package io.github.dsheirer.channel.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache for frequently reused decoder state notification events and timeslots
 */
public class DecoderStateNotificationEventCache
{
    private Map<Integer,Map<State,DecoderStateEvent>> mTimeslotMap = new HashMap<>();

    public DecoderStateNotificationEventCache()
    {
    }

    /**
     * Creates a decoder state notification event for the state and timeslot, or reuses a previously created event.
     * @param state for the decoder
     * @param timeslot of the decoder
     * @return state notification event
     */
    public DecoderStateEvent getStateNotificationEvent(State state, int timeslot)
    {
        Map<State,DecoderStateEvent> timeslotStateMap = mTimeslotMap.get(timeslot);

        if(timeslotStateMap == null)
        {
            timeslotStateMap = new HashMap<>();
            mTimeslotMap.put(timeslot, timeslotStateMap);
        }

        DecoderStateEvent event = timeslotStateMap.get(state);

        if(event == null)
        {
            event = DecoderStateEvent.stateNotification(state, timeslot);
            timeslotStateMap.put(state, event);
        }

        return event;
    }
}
