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
package io.github.dsheirer.message;

import io.github.dsheirer.protocol.Protocol;

/**
 * IMessage implementation.
 */
public abstract class Message implements IMessage
{
    private long mTimestamp;

    /**
     * Constructs a new message using current system time as the timestamp.
     */
    public Message()
    {
        mTimestamp = System.currentTimeMillis();
    }

    /**
     * Constructs a new message using the argument timestamp.
     *
     * @param timestamp for the message
     */
    public Message(long timestamp)
    {
        mTimestamp = timestamp;
    }

    /**
     * Timestamp when the message was received or processed.  This timestamp should be as close to
     * accurate as possible.
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Decoded textual representation of the message
     */
    public abstract String toString();

    /**
     * Indicates if the message is valid and has passed crc/integrity checks
     */
    public abstract boolean isValid();

    /**
     * Timeslot default of 0 unless override in subclass.
     */
    public int getTimeslot()
    {
        return 0;
    }

    /**
     * Decoded protocol
     */
    public abstract Protocol getProtocol();
}