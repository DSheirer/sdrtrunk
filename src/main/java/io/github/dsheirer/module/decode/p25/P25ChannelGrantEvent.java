/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import io.github.dsheirer.protocol.Protocol;

public class P25ChannelGrantEvent extends P25DecodeEvent
{
    private ServiceOptions mServiceOptions;

    public P25ChannelGrantEvent(long timestamp)
    {
        super(timestamp);
    }

    /**
     * Creates a new decode event builder with the specified start timestamp.
     * @param timeStart for the event
     * @return builder
     */
    public static P25ChannelGrantDecodeEventBuilder builder(long timeStart, ServiceOptions serviceOptions)
    {
        return new P25ChannelGrantDecodeEventBuilder(timeStart, serviceOptions);
    }

    /**
     * Service options for the channel grant
     */
    public ServiceOptions getServiceOptions()
    {
        return mServiceOptions;
    }


    /**
     * Service options for the channel grant
     */
    public void setServiceOptions(ServiceOptions serviceOptions)
    {
        mServiceOptions = serviceOptions;
    }

    /**
     * Indicates if this channel grant event has non-null service options
     */
    public boolean hasServiceOptions()
    {
        return mServiceOptions != null;
    }

    /**
     * Builder pattern for constructing decode events.
     */
    public static class P25ChannelGrantDecodeEventBuilder
    {
        protected long mTimeStart;
        protected long mDuration;
        protected String mEventDescription;
        protected IdentifierCollection mIdentifierCollection;
        protected IChannelDescriptor mChannelDescriptor;
        protected String mDetails;
        private ServiceOptions mServiceOptions;

        /**
         * Constructs a builder instance with the specified start time in milliseconds
         *
         * @param timeStart
         */
        public P25ChannelGrantDecodeEventBuilder(long timeStart, ServiceOptions serviceOptions)
        {
            mTimeStart = timeStart;
            mServiceOptions = serviceOptions;
        }

        /**
         * Sets the duration value
         * @param duration in milliseconds
         */
        public P25ChannelGrantDecodeEventBuilder duration(long duration)
        {
            mDuration = duration;
            return this;
        }

        /**
         * Sets the duration value using the end - start timestamps
         * @param timestamp for end of event
         */
        public P25ChannelGrantDecodeEventBuilder end(long timestamp)
        {
            mDuration = timestamp - mTimeStart;
            return this;
        }

        /**
         * Sets the channel descriptor for this event
         * @param channelDescriptor
         */
        public P25ChannelGrantDecodeEventBuilder channel(IChannelDescriptor channelDescriptor)
        {
            mChannelDescriptor = channelDescriptor;
            return this;
        }

        /**
         * Sets the event description text
         * @param description of the event
         */
        public P25ChannelGrantDecodeEventBuilder eventDescription(String description)
        {
            mEventDescription = description;
            return this;
        }

        /**
         * Sets the identifier collection.
         * @param identifierCollection containing optional identifiers like TO, FROM, frequency and
         * alias list configuration name.
         */
        public P25ChannelGrantDecodeEventBuilder identifiers(IdentifierCollection identifierCollection)
        {
            mIdentifierCollection = identifierCollection;
            return this;
        }

        /**
         * Sets the details for the event
         * @param details
         */
        public P25ChannelGrantDecodeEventBuilder details(String details)
        {
            mDetails = details;
            return this;
        }

        /**
         * Builds the decode event
         */
        public P25ChannelGrantEvent build()
        {
            P25ChannelGrantEvent decodeEvent = new P25ChannelGrantEvent(mTimeStart);
            decodeEvent.setProtocol(Protocol.APCO25);
            decodeEvent.setChannelDescriptor(mChannelDescriptor);
            decodeEvent.setDetails(mDetails);
            decodeEvent.setDuration(mDuration);
            decodeEvent.setEventDescription(mEventDescription);
            decodeEvent.setIdentifierCollection(mIdentifierCollection);
            decodeEvent.setServiceOptions(mServiceOptions);
            return decodeEvent;
        }
    }
}
