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

package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.protocol.Protocol;
import org.jdesktop.swingx.mapviewer.GeoPosition;

public class PlottableDecodeEvent extends DecodeEvent
{
    private GeoPosition mGeoPosition;
    private double mHeading;
    private double mSpeed;

    public PlottableDecodeEvent(long start)
    {
        super(start);
    }

    /**
     * Creates a new decode event builder with the specified start timestamp.
     * @param timeStart for the event
     * @return builder
     */
    public static PlottableDecodeEventBuilder plottableBuilder(long timeStart)
    {
        return new PlottableDecodeEventBuilder(timeStart);
    }

    /**
     * Sets the location for the event
     */
    public void setLocation(GeoPosition geoPosition)
    {
        mGeoPosition = geoPosition;
    }

    /**
     * Location of the event
     */
    public GeoPosition getLocation()
    {
        return mGeoPosition;
    }

    /**
     * Sets the heading for the mobile plottable event
     * @param heading
     */
    public void setHeading(double heading)
    {
        mHeading = heading;
    }

    /**
     * Heading of the mobile plottable event
     */
    public double getHeading()
    {
        return mHeading;
    }

    /**
     * Sets the speed of the mobile plottable event in kph
     */
    public void setSpeed(double speed)
    {
        mSpeed = speed;
    }

    /**
     * Speed of the mobile plottable event in kph
     */
    public double getSpeed()
    {
        return mSpeed;
    }

    /**
     * Builder pattern for constructing decode events.
     */
    public static class PlottableDecodeEventBuilder
    {
        private long mTimeStart;
        private long mDuration;
        private String mEventDescription;
        private DecodeEventType mDecodeEventType;
        private IdentifierCollection mIdentifierCollection;
        private IChannelDescriptor mChannelDescriptor;
        private String mDetails;
        private Protocol mProtocol = Protocol.UNKNOWN;
        private GeoPosition mGeoPosition;
        private double mHeading;
        private double mSpeed;

        /**
         * Constructs a builder instance with the specified start time in milliseconds
         */
        public PlottableDecodeEventBuilder(long timeStart)
        {
            mTimeStart = timeStart;
        }

        /**
         * Sets the duration value
         * @param duration in milliseconds
         */
        public PlottableDecodeEventBuilder duration(long duration)
        {
            mDuration = duration;
            return this;
        }

        /**
         * Sets the duration value using the end - start timestamps
         * @param timestamp for end of event
         */
        public PlottableDecodeEventBuilder end(long timestamp)
        {
            mDuration = timestamp - mTimeStart;
            return this;
        }

        /**
         * Sets the channel descriptor for this event
         * @param channelDescriptor
         */
        public PlottableDecodeEventBuilder channel(IChannelDescriptor channelDescriptor)
        {
            mChannelDescriptor = channelDescriptor;
            return this;
        }

        /**
         * Sets the event description text
         * @param description of the event
         */
        public PlottableDecodeEventBuilder eventDescription(String description)
        {
            mEventDescription = description;
            return this;
        }

        /**
         * Sets the identifier collection.
         * @param identifierCollection containing optional identifiers like TO, FROM, frequency and
         * alias list configuration name.
         */
        public PlottableDecodeEventBuilder identifiers(IdentifierCollection identifierCollection)
        {
            mIdentifierCollection = identifierCollection;
            return this;
        }

        /**
         * Sets the details for the event
         * @param details
         */
        public PlottableDecodeEventBuilder details(String details)
        {
            mDetails = details;
            return this;
        }

        /**
         * Sets the protocol for this event
         * @param protocol
         */
        public PlottableDecodeEventBuilder protocol(Protocol protocol)
        {
            mProtocol = protocol;
            return this;
        }

        /**
         * Sets the Decode Event type for this event.
         * @param eventType
         */
        public PlottableDecodeEventBuilder eventType(DecodeEventType eventType) {
            mDecodeEventType = eventType;
            return this;
        }

        /**
         * Sets the location for the plottable event
         */
        public PlottableDecodeEventBuilder location(GeoPosition geoPosition)
        {
            mGeoPosition = geoPosition;
            return this;
        }

        /**
         * Sets the speed for the plottable event in kph
         */
        public PlottableDecodeEventBuilder speed(double speed)
        {
            mSpeed = speed;
            return this;
        }

        /**
         * Sets the heading for the plottable event in degrees relative to true North
         */
        public PlottableDecodeEventBuilder heading(double heading)
        {
            mHeading = heading;
            return this;
        }

        /**
         * Builds the decode event
         */
        public PlottableDecodeEvent build()
        {
            PlottableDecodeEvent decodeEvent = new PlottableDecodeEvent(mTimeStart);
            decodeEvent.setChannelDescriptor(mChannelDescriptor);
            decodeEvent.setDetails(mDetails);
            decodeEvent.setDuration(mDuration);
            decodeEvent.setEventType(mDecodeEventType);
            decodeEvent.setEventDescription(mEventDescription);
            decodeEvent.setIdentifierCollection(mIdentifierCollection);
            decodeEvent.setProtocol(mProtocol);
            decodeEvent.setLocation(mGeoPosition);
            decodeEvent.setHeading(mHeading);
            decodeEvent.setSpeed(mSpeed);
            return decodeEvent;
        }
    }

}
