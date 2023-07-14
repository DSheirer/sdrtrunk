/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.protocol.Protocol;

/**
 * Decode event interface
 */
public interface IDecodeEvent
{
    /**
     * Event start time
     *
     * @return timestamp in milliseconds
     */
    long getTimeStart();

    /**
     * Event end time
     * @return timestamp in milliseconds
     */
    long getTimeEnd();

    /**
     * Event duration.
     *
     * @return event duration in milliseconds or 0 if there is no duration
     */
    long getDuration();

    /**
     * Collection of identifiers associated with the event.  This collection should contain a
     * Role.FROM and a Role.TO identifier, a Decoder Type identifier, and (optionally) an Alias List
     * Configuration identifier.
     */
    IdentifierCollection getIdentifierCollection();

    /**
     * Channel descriptor for the channel where the event occurred
     *
     * @return descriptor or null
     */
    IChannelDescriptor getChannelDescriptor();

    /**
     * Optional Details about the event
     */
    String getDetails();

    /**
     * Protocol for the decoder that produced the event
     */
    Protocol getProtocol();

    /**
     * {@link DecodeEventType} for the event produced
     */
    DecodeEventType getEventType();

    /**
     * Timeslot for the event.
     * @return timeslot or default of 0
     */
    int getTimeslot();

    /**
     * Indicates if the event has a timeslot specified
     */
    boolean hasTimeslot();
}
