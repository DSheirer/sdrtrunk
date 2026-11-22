/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.module.decode.event.ChannelEventTracker;

/**
 * Wrapper to track the state of a traffic channel event to manage updates from the control channel and the traffic
 * channel and to assist in determining when the communicants of a traffic channel have changed, indicating the need
 * for a new event.
 */
public class P25ChannelEventTracker extends ChannelEventTracker<P25ChannelGrantEvent>
{
    private boolean mEncrypted;

    /**
     * Constructs an instance
     *
     * @param event to track for the traffic channel.
     */
    public P25ChannelEventTracker(P25ChannelGrantEvent event)
    {
        super(event);
    }

    /**
     * Indicates if this call is known to be encrypted.  Once a call is flagged as encrypted, the flag persists for the
     * lifetime of this tracker because many channel grant update messages don't carry service options and those
     * updates are processed with empty (ie unencrypted) service options.
     * @return true if the call is known to be encrypted.
     */
    public boolean isEncrypted()
    {
        return mEncrypted;
    }

    /**
     * Flags this call as encrypted.
     * @param encrypted true if encrypted.
     */
    public void setEncrypted(boolean encrypted)
    {
        mEncrypted = encrypted;
    }
}
