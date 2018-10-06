/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.message;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.map.Plottable;

import java.util.Collections;
import java.util.List;

/**
 * Legacy base message class.
 *
 * Note: this class will eventually be removed as the architecture is updated to use IMessage interface.
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
     * Map plottable
     */
    public Plottable getPlottable()
    {
//TODO: move this to an IPlottable interface that only gets implemented as needed
        return null;
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
     * Status of the CRC check of the message
     */
    public abstract String getErrorStatus();

    /**
     * Parsed Message
     *
     * @return
     */
    public abstract String getMessage();

    /**
     * Raw ( 0 & 1 ) message bits
     */
    public abstract String getBinaryMessage();


    /**
     * Decoded protocol
     */
    public abstract String getProtocol();

    /**
     * Event - call, data, idle, etc.
     */
    public abstract String getEventType();

    /**
     * Formatted from identifier
     */
    public abstract String getFromID();

    /**
     * From identifier alias (from AliasManager)
     */
    public abstract Alias getFromIDAlias();

    /**
     * Formatted to identifier
     */
    public abstract String getToID();

    /**
     * To identifier alias (from AliasManager)
     *
     * @return
     */
    public abstract Alias getToIDAlias();


    /**
     * Provides a listing of aliases contained in the message.
     */
    public List<Alias> getAliases()
    {
        return Collections.EMPTY_LIST;
    }
}