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
package io.github.dsheirer.database.event;

import io.github.dsheirer.database.IdentifierRecord;
import io.github.dsheirer.module.decode.event.DecodeEventType;

import java.util.Objects;

/**
 * A decode event as stored in the events table.
 *
 * Instances are mutable while they are being built, but a record that has been handed to the database writer or to
 * the user interface is a snapshot that is never changed afterwards.  Use {@link #copy()} to create a new snapshot.
 */
public class EventRecord extends IdentifierRecord
{
    private long mEventKey;
    private long mDuration;
    private String mEventType;
    private String mChannelDescriptor;
    private String mTalkerAlias;
    private String mDetails;

    /**
     * Creates a copy (snapshot) of this record.
     */
    public EventRecord copy()
    {
        EventRecord copy = new EventRecord();
        copyTo(copy);
        copy.mEventKey = mEventKey;
        copy.mDuration = mDuration;
        copy.mEventType = mEventType;
        copy.mChannelDescriptor = mChannelDescriptor;
        copy.mTalkerAlias = mTalkerAlias;
        copy.mDetails = mDetails;
        return copy;
    }

    /**
     * In-memory key that identifies a live event across snapshots, or 0 for records loaded from the database.
     */
    public long getEventKey()
    {
        return mEventKey;
    }

    public void setEventKey(long eventKey)
    {
        mEventKey = eventKey;
    }

    /**
     * Event duration in milliseconds, or 0 when the event has no duration.
     */
    public long getDuration()
    {
        return mDuration;
    }

    public void setDuration(long duration)
    {
        mDuration = Math.max(0, duration);
    }

    /**
     * Decode event type enumeration entry name (e.g. CALL_GROUP)
     */
    public String getEventType()
    {
        return mEventType;
    }

    public void setEventType(String eventType)
    {
        mEventType = eventType;
    }

    /**
     * Decode event type, or UNKNOWN when the stored value isn't recognized.
     */
    public DecodeEventType getEventTypeValue()
    {
        if(mEventType != null)
        {
            try
            {
                return DecodeEventType.valueOf(mEventType);
            }
            catch(IllegalArgumentException iae)
            {
                //Fall through
            }
        }

        return DecodeEventType.UNKNOWN;
    }

    /**
     * Event category derived from the event type
     */
    public EventCategory getCategory()
    {
        return EventCategory.from(getEventTypeValue());
    }

    /**
     * Channel descriptor for the event (e.g. the traffic channel number of a call), or null.
     */
    public String getChannelDescriptor()
    {
        return mChannelDescriptor;
    }

    public void setChannelDescriptor(String channelDescriptor)
    {
        mChannelDescriptor = channelDescriptor;
    }

    public String getTalkerAlias()
    {
        return mTalkerAlias;
    }

    public void setTalkerAlias(String talkerAlias)
    {
        mTalkerAlias = talkerAlias;
    }

    /**
     * Decoder supplied details for the event, or null.
     */
    public String getDetails()
    {
        return mDetails;
    }

    public void setDetails(String details)
    {
        mDetails = details;
    }

    /**
     * Key used to detect an event that already exists in a target database during an append transfer.
     */
    public String getTransferKey()
    {
        return "E:" + getStartTime() + "|" + mEventType + "|" + getChannel() + "|" + getTimeslot() + "|" + getToId() +
            "|" + getFromId() + "|" + Objects.hashCode(mDetails);
    }

    @Override
    public String toString()
    {
        return "Event " + getId() + " " + mEventType + " " + getProtocol() + " TO:" + getToId() + " FROM:" +
            getFromId() + (mDetails != null ? " " + mDetails : "");
    }
}
