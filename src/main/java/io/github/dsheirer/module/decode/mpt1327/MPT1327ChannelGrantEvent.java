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
package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.protocol.Protocol;

/**
 * MPT1327 Channel Grant Event
 */
public class MPT1327ChannelGrantEvent extends MPT1327DecodeEvent
{
    public MPT1327ChannelGrantEvent(DecodeEventType decodeEventType, long timestamp)
    {
        super(decodeEventType, timestamp);
    }

    /**
     * Creates a new decode event builder with the specified start timestamp.
     * @param timeStart for the event
     * @return builder
     */
    public static MPT1327ChannelGrantDecodeEventBuilder mpt1327Builder(DecodeEventType decodeEventType, long timeStart)
    {
        return new MPT1327ChannelGrantDecodeEventBuilder(decodeEventType, timeStart);
    }

    /**
     * Builder pattern for constructing decode events.
     */
    public static class MPT1327ChannelGrantDecodeEventBuilder
    {
        protected long mTimeStart;
        protected long mDuration;
        protected DecodeEventType mDecodeEventType;
        protected IdentifierCollection mIdentifierCollection;
        protected IChannelDescriptor mChannelDescriptor;
        protected String mDetails;

        /**
         * Constructs a builder instance with the specified start time in milliseconds
         *
         * @param timeStart
         */
        public MPT1327ChannelGrantDecodeEventBuilder(DecodeEventType decodeEventType, long timeStart)
        {
            mDecodeEventType = decodeEventType;
            mTimeStart = timeStart;
        }

        /**
         * Sets the duration value
         * @param duration in milliseconds
         */
        public MPT1327ChannelGrantDecodeEventBuilder duration(long duration)
        {
            mDuration = duration;
            return this;
        }

        /**
         * Sets the duration value using the end - start timestamps
         * @param timestamp for end of event
         */
        public MPT1327ChannelGrantDecodeEventBuilder end(long timestamp)
        {
            mDuration = timestamp - mTimeStart;
            return this;
        }

        /**
         * Sets the channel descriptor for this event
         * @param channelDescriptor
         */
        public MPT1327ChannelGrantDecodeEventBuilder channel(IChannelDescriptor channelDescriptor)
        {
            mChannelDescriptor = channelDescriptor;
            return this;
        }

        /**
         * Sets the identifier collection.
         * @param identifierCollection containing optional identifiers like TO, FROM, frequency and
         * alias list configuration name.
         */
        public MPT1327ChannelGrantDecodeEventBuilder identifiers(IdentifierCollection identifierCollection)
        {
            mIdentifierCollection = identifierCollection;
            return this;
        }

        /**
         * Sets the details for the event
         * @param details
         */
        public MPT1327ChannelGrantDecodeEventBuilder details(String details)
        {
            mDetails = details;
            return this;
        }

        /**
         * Builds the decode event
         */
        public MPT1327ChannelGrantEvent build()
        {
            MPT1327ChannelGrantEvent decodeEvent = new MPT1327ChannelGrantEvent(mDecodeEventType, mTimeStart);
            decodeEvent.setProtocol(Protocol.MPT1327);
            decodeEvent.setChannelDescriptor(mChannelDescriptor);
            decodeEvent.setDetails(mDetails);
            decodeEvent.setDuration(mDuration);
            decodeEvent.setIdentifierCollection(mIdentifierCollection);
            return decodeEvent;
        }
    }
}
