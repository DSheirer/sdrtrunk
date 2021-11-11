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
package io.github.dsheirer.module.decode.dmr.event;

import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.event.DecodeEventType;

/**
 * DMR Channel Grant Event.
 *
 * This event class is send traffic channel grant details to the traffic channel manager to allocate a traffic channel.
 */
public class DMRChannelGrantEvent extends DMRDecodeEvent
{
    private boolean mEncrypted;

    /**
     * Constructs an instance
     * @param timestamp for the event
     */
    public DMRChannelGrantEvent(long timestamp)
    {
        super(timestamp);
    }

    /**
     * Indicates if the channel grant was for an encrypted call
     */
    public boolean isEncrypted()
    {
        return mEncrypted;
    }

    /**
     * Sets the encrypte state for the call
     * @param encrypted
     */
    public void setEncrypted(boolean encrypted)
    {
        mEncrypted = encrypted;
    }

    /**
     * Creates a new decode event builder with the specified start timestamp.
     * @param timeStart for the event
     * @return builder
     */
    public static DMRChannelGrantDecodeEventBuilder channelGrantBuilder(long timeStart)
    {
        return new DMRChannelGrantDecodeEventBuilder(timeStart);
    }

    /**
     * Builder pattern for constructing decode events.
     */
    public static class DMRChannelGrantDecodeEventBuilder
    {
        protected long mTimeStart;
        protected long mDuration;
        protected DecodeEventType mDecodeEventType;
        protected String mEventDescription;
        protected IdentifierCollection mIdentifierCollection;
        protected DMRChannel mChannel;
        protected String mDetails;
        protected boolean mEncrypted;

        /**
         * Constructs a builder instance with the specified start time in milliseconds
         *
         * @param timeStart
         */
        public DMRChannelGrantDecodeEventBuilder(long timeStart)
        {
            mTimeStart = timeStart;
        }

        /**
         * Sets the encrypted state for the channel/timeslot
         */
        public DMRChannelGrantDecodeEventBuilder encrypted(boolean encrypted)
        {
            mEncrypted = encrypted;
            return this;
        }

        /**
         * Sets the duration value
         * @param duration in milliseconds
         */
        public DMRChannelGrantDecodeEventBuilder duration(long duration)
        {
            mDuration = duration;
            return this;
        }

        /**
         * Sets the duration value using the end - start timestamps
         * @param timestamp for end of event
         */
        public DMRChannelGrantDecodeEventBuilder end(long timestamp)
        {
            mDuration = timestamp - mTimeStart;
            return this;
        }

        /**
         * Sets the channel descriptor for this event
         * @param channel
         */
        public DMRChannelGrantDecodeEventBuilder channel(DMRChannel channel)
        {
            mChannel = channel;
            return this;
        }

        /**
         * Sets the Decode Event type for this event.
         * @param eventType
         */
        public DMRChannelGrantDecodeEventBuilder eventType(DecodeEventType eventType) {
            mDecodeEventType = eventType;
            return this;
        }

        /**
         * Sets the event description text
         * @param description of the event
         */
        public DMRChannelGrantDecodeEventBuilder eventDescription(String description)
        {
            mEventDescription = description;
            return this;
        }

        /**
         * Sets the identifier collection.
         * @param identifierCollection containing optional identifiers like TO, FROM, frequency and
         * alias list configuration name.
         */
        public DMRChannelGrantDecodeEventBuilder identifiers(IdentifierCollection identifierCollection)
        {
            mIdentifierCollection = identifierCollection;
            return this;
        }

        /**
         * Sets the details for the event
         * @param details
         */
        public DMRChannelGrantDecodeEventBuilder details(String details)
        {
            mDetails = details;
            return this;
        }

        /**
         * Builds the decode event
         */
        public DMRChannelGrantEvent build()
        {
            DMRChannelGrantEvent decodeEvent = new DMRChannelGrantEvent(mTimeStart);
            decodeEvent.setChannelDescriptor(mChannel);
            decodeEvent.setTimeslot(mChannel.getTimeslot());
            decodeEvent.setDetails(mDetails);
            decodeEvent.setDuration(mDuration);
            decodeEvent.setEventType(mDecodeEventType);
            decodeEvent.setEventDescription(mEventDescription);
            decodeEvent.setIdentifierCollection(mIdentifierCollection);
            return decodeEvent;
        }
    }
}
