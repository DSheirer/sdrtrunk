/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import com.google.common.base.Joiner;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.protocol.Protocol;

/**
 * Decode event implementation.  A decode event is a discrete event such as a call or text message
 * or radio registration.
 */
public class DecodeEvent implements IDecodeEvent
{
    private long mTimeStart;
    private long mTimeEnd;
    private String mEventDescription;
    private DecodeEventType mDecodeEventType;
    private IdentifierCollection mIdentifierCollection;
    private IChannelDescriptor mChannelDescriptor;
    private String mDetails;
    private Protocol mProtocol;
    private Integer mTimeslot;

    public DecodeEvent(long start)
    {
        mTimeStart = start;
    }

    /**
     * Creates a new decode event builder with the specified start timestamp.
     * @param timeStart for the event
     * @return builder
     */
    public static DecodeEventBuilder builder(long timeStart)
    {
        return new DecodeEventBuilder(timeStart);
    }

    /**
     * Updates the duration value using the end - start timestamps.  Note: this method can be
     * invoked multiple times and the duration will be set to the value of the final invocation.
     * @param timestamp for end of the event.
     */
    public void end(long timestamp)
    {
        if(timestamp > mTimeStart)
        {
            mTimeEnd = timestamp;
        }
    }

    /**
     * Updates the current in-progress call with the latest timestamp
     */
    public void update(long timestamp)
    {
        end(timestamp);
    }

    /**
     * Event start in milliseconds
     */
    @Override
    public long getTimeStart()
    {
        return mTimeStart;
    }

    /**
     * Event end in milliseconds
     */
    public long getTimeEnd()
    {
        return mTimeEnd;
    }

    /**
     * Event duration in milliseconds
     */
    @Override
    public long getDuration()
    {
        if(mTimeEnd < mTimeStart)
        {
            return 0;
        }

        return mTimeEnd - mTimeStart;
    }

    /**
     * Sets the event duration in milliseconds
     */
    public void setDuration(long duration)
    {
        mTimeEnd = mTimeStart + duration;
    }

    /**
     * Event description
     */
    @Override
    public String getEventDescription()
    {
        return mEventDescription;
    }

    /**
     * Sets the event description text
     */
    public void setEventDescription(String description)
    {
        mEventDescription = description;
    }

    /**
     * {@link DecodeEventType}
     */
    @Override
    public DecodeEventType getEventType() {
        return mDecodeEventType;
    }

    /**
     * Sets the {@link DecodeEventType}
     */
    public void setEventType(DecodeEventType eventType) {
        this.mDecodeEventType = eventType;
    }

    /**
     * Identifier collection for this event.
     */
    @Override
    public IdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Sets the identifier collection for this event.  Note: identifier collection should contain
     * a FrequencyConfigurationIdentifier and optionally an AliasListConfigurationIdentifier so that
     * display elements can be properly configured to display the event.
     */
    public void setIdentifierCollection(IdentifierCollection identifierCollection)
    {
        mIdentifierCollection = identifierCollection;
    }

    /**
     * Channel descriptor for the channel
     */
    @Override
    public IChannelDescriptor getChannelDescriptor()
    {
        return mChannelDescriptor;
    }

    /**
     * Sets the channel descriptor for the event
     */
    public void setChannelDescriptor(IChannelDescriptor channelDescriptor)
    {
        mChannelDescriptor = channelDescriptor;
    }

    /**
     * Event details text
     */
    @Override
    public String getDetails()
    {
        return mDetails;
    }

    /**
     * Sets the event details text
     */
    public void setDetails(String details)
    {
        mDetails = details;
    }

    /**
     * Protocol (ie decoder type) for the event
     */
    @Override
    public Protocol getProtocol()
    {
        return mProtocol;
    }

    /**
     * Sets the protocol for the event
     */
    public void setProtocol(Protocol protocol)
    {
        mProtocol = protocol;
    }

    /**
     * Timeslot for this event
     * @return timeslot or default value of 0
     */
    @Override
    public Integer getTimeslot()
    {
        return mTimeslot;
    }

    /**
     * Indicates if this event specifies a timeslot
     */
    public boolean hasTimeslot()
    {
        return mTimeslot != null;
    }

    /**
     * Sets the timeslot for this event
     * @param timeslot of the event
     */
    public void setTimeslot(Integer timeslot)
    {
        mTimeslot = timeslot;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mProtocol);
        sb.append(" DECODE EVENT: ").append(getEventDescription());
        sb.append(" DETAILS:").append(getDetails());
        if(mIdentifierCollection != null)
        {
            sb.append(" IDS:").append(Joiner.on(",").join(mIdentifierCollection.getIdentifiers()));
        }
        sb.append(" DURATION:").append(getDuration());
        sb.append(" CHANNEL:").append(mChannelDescriptor);
        return sb.toString();
    }

    /**
     * Builder pattern for constructing decode events.
     */
    public static class DecodeEventBuilder
    {
        protected long mTimeStart;
        protected long mDuration;
        protected String mEventDescription;
        protected DecodeEventType mDecodeEventType;
        protected IdentifierCollection mIdentifierCollection;
        protected IChannelDescriptor mChannelDescriptor;
        protected String mDetails;
        protected Protocol mProtocol = Protocol.UNKNOWN;
        protected Integer mTimeslot;

        /**
         * Constructs a builder instance with the specified start time in milliseconds
         */
        public DecodeEventBuilder(long timeStart)
        {
            mTimeStart = timeStart;
        }

        /**
         * Sets the duration value
         * @param duration in milliseconds
         */
        public DecodeEventBuilder duration(long duration)
        {
            mDuration = duration;
            return this;
        }

        /**
         * Sets the duration value using the end - start timestamps
         * @param timestamp for end of event
         */
        public DecodeEventBuilder end(long timestamp)
        {
            mDuration = timestamp - mTimeStart;
            return this;
        }

        /**
         * Sets the channel descriptor for this event
         * @param channelDescriptor
         */
        public DecodeEventBuilder channel(IChannelDescriptor channelDescriptor)
        {
            mChannelDescriptor = channelDescriptor;
            return this;
        }

        /**
         * Sets the Decode Event type for this event.
         * @param eventType
         */
        public DecodeEventBuilder eventType(DecodeEventType eventType) {
            mDecodeEventType = eventType;
            return this;
        }

        /**
         * Sets the event description text
         * @param description of the event
         */
        public DecodeEventBuilder eventDescription(String description)
        {
            mEventDescription = description;
            return this;
        }

        /**
         * Sets the identifier collection.
         * @param identifierCollection containing optional identifiers like TO, FROM, frequency and
         * alias list configuration name.
         */
        public DecodeEventBuilder identifiers(IdentifierCollection identifierCollection)
        {
            mIdentifierCollection = identifierCollection;
            return this;
        }

        /**
         * Sets the details for the event
         * @param details
         */
        public DecodeEventBuilder details(String details)
        {
            mDetails = details;
            return this;
        }

        /**
         * Sets the protocol for this event
         * @param protocol
         */
        public DecodeEventBuilder protocol(Protocol protocol)
        {
            mProtocol = protocol;
            return this;
        }

        public DecodeEventBuilder timeslot(Integer timeslot)
        {
            mTimeslot = timeslot;
            return this;
        }

        /**
         * Builds the decode event
         */
        public DecodeEvent build()
        {
            DecodeEvent decodeEvent = new DecodeEvent(mTimeStart);
            decodeEvent.setChannelDescriptor(mChannelDescriptor);
            decodeEvent.setDetails(mDetails);
            decodeEvent.setDuration(mDuration);
            decodeEvent.setEventType(mDecodeEventType);
            decodeEvent.setEventDescription(mEventDescription);
            decodeEvent.setIdentifierCollection(mIdentifierCollection);
            decodeEvent.setProtocol(mProtocol);
            decodeEvent.setTimeslot(mTimeslot);
            return decodeEvent;
        }
    }
}
