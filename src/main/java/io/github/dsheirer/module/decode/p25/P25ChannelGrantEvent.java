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
    public static P25ChannelGrantDecodeEventBuilder builder(long timeStart)
    {
        return new P25ChannelGrantDecodeEventBuilder(timeStart);
    }


    public void setServiceOptions(ServiceOptions serviceOptions)
    {
        mServiceOptions = serviceOptions;
    }

    /**
     * Service options for the channel grant
     */
    public ServiceOptions getServiceOptions()
    {
        return mServiceOptions;
    }

    /**
     * Builder pattern for constructing decode events.
     */
    public static class P25ChannelGrantDecodeEventBuilder extends DecodeEventBuilder
    {
        private ServiceOptions mServiceOptions;

        /**
         * Constructs a builder instance with the specified start time in milliseconds
         *
         * @param timeStart
         */
        public P25ChannelGrantDecodeEventBuilder(long timeStart)
        {
            super(timeStart);
        }


        /**
         * Builds the decode event
         */
        public P25ChannelGrantEvent build()
        {
            P25ChannelGrantEvent decodeEvent = new P25ChannelGrantEvent(mTimeStart);
            decodeEvent.setChannelDescriptor(mChannelDescriptor);
            decodeEvent.setDetails(mDetails);
            decodeEvent.setDuration(mDuration);
            decodeEvent.setEventDescription(mEventDescription);
            decodeEvent.setIdentifierCollection(mIdentifierCollection);
            decodeEvent.setProtocol(Protocol.APCO25);
            decodeEvent.setServiceOptions(mServiceOptions);
            return decodeEvent;
        }
    }
}
