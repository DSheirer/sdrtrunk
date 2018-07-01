/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.source.heartbeat;

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;

public class HeartbeatManager
{
    private static final Heartbeat HEARTBEAT = new Heartbeat();
    private Broadcaster<Heartbeat> mHeartbeatBroadcaster = new Broadcaster<>();

    /**
     * Broadcasts heartbeat as commanded and handles listener registration details.
     */
    public HeartbeatManager()
    {
    }

    /**
     * Sends a heartbeat to all registered listeners
     */
    public void broadcast()
    {
        mHeartbeatBroadcaster.broadcast(HEARTBEAT);
    }

    /**
     * Adds the listener to receive heartbeats
     */
    public void addHeartbeatListener(Listener<Heartbeat> listener)
    {
        mHeartbeatBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving heartbeats
     * @param listener
     */
    public void removeHeartbeatListener(Listener<Heartbeat> listener)
    {
        mHeartbeatBroadcaster.removeListener(listener);
    }
}
