/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package audio.broadcast;

public enum BroadcastState
{
    /**
     * Error while broadcasting stream data.  Temporary error state to allow connection to be reset.
     */
    BROADCAST_ERROR("Broadcast Error", false),

    /**
     * Connected to the broadcast server and capable of streaming
     */
    CONNECTED("Connected", false),

    /**
     * Connection interrupted, attempting to reconnect.
     */
    CONNECTING("Connecting", false),

    /**
     * Disconnected from the broadcast server
     */
    DISCONNECTED("Disconnected", true),

    /**
     * Invalid password
     */
    INVALID_PASSWORD("Invalid Password", true),

    /**
     * Server is not known or reachable
     */
    NO_SERVER("No Server", false),

    /**
     * Connected but paused.  No audio is streaming.
     */
    PAUSED("Paused", false),

    /**
     * Initial state with no connection attempted.
     */
    READY("Ready", false),

    /**
     * Server host name or port is invalid
     */
    UNKNOWN_HOST("Unknown Host or Invalid Port", true),

    /**
     * Unspecified error
     */
    ERROR("Error", true);

    private String mLabel;
    private boolean mErrorState;

    private BroadcastState(String label, boolean connected)
    {
        mLabel = label;
        mErrorState = connected;
    }

    public String toString()
    {
        return mLabel;
    }

    public boolean isErrorState()
    {
        return mErrorState;
    }
}
