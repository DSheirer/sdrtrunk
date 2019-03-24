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

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ChannelNumber;
import io.github.dsheirer.protocol.Protocol;

/**
 * APCO25 Phase 2 Base Message
 */
public abstract class P25P2Message implements IMessage
{
    private long mTimestamp;
    private boolean mValid = true;

    /**
     * Constructs the message
     * @param timestamp of the final bit of the message
     */
    protected P25P2Message(long timestamp)
    {
        mTimestamp = timestamp;
    }

    /**
     * Timestamp when the final bit of this message was transmitted
     * @return timestamp as milliseconds since epoch
     */
    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Indicates if this message is valid
     */
    @Override
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Sets the valid flag for this message
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    /**
     * Protocol for this message
     */
    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25_PHASE2;
    }

    /**
     * Channel number or timeslot
     * @return timeslot 0 or 1
     */
    public abstract ChannelNumber getChannelNumber();
}
