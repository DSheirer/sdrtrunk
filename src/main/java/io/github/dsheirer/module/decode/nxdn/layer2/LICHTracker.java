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

package io.github.dsheirer.module.decode.nxdn.layer2;

import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the observed channel type and direction occurrence counts.
 */
public class LICHTracker
{
    private static final Logger LOG = LoggerFactory.getLogger(LICHTracker.class);
    private static final int BUFFER_SIZE = 5;
    private final RFChannel[] mRFChannelBuffer = new RFChannel[BUFFER_SIZE];
    private final TransmissionMode mTransmissionMode;
    private int mBufferPointer = 0;
    private RFChannel mTrackedChannel = RFChannel.RCCH;
    private Direction mTrackedDirection = Direction.OUTBOUND;
    private List<Tracker> mTrackers = new ArrayList<>();
    private Map<RFChannel,Tracker> mTrackerMap = new HashMap<>();
    private int mCountOutbound;
    private int mCountInbound;

    /**
     * Constructs an instance
     */
    public LICHTracker(TransmissionMode transmissionMode)
    {
        mTransmissionMode = transmissionMode;

        for(RFChannel channel : RFChannel.values())
        {
            Tracker tracker = new Tracker(channel);
            mTrackers.add(tracker);
            mTrackerMap.put(channel, tracker);
        }

        reset();
    }

    /**
     * Indicates if the transmission mode is Type D
     */
    public boolean isTypeD()
    {
        return mTransmissionMode.isTypeD();
    }

    /**
     * Resets the channel tracked values.
     */
    public void reset()
    {
        Arrays.fill(mRFChannelBuffer, RFChannel.UNKNOWN);
        mTrackedChannel = RFChannel.UNKNOWN;
        mTrackedDirection = Direction.OUTBOUND;
        mTrackers.forEach(Tracker::reset);
    }

    /**
     * RF channel with the highest observed counts.
     */
    public RFChannel getChannel()
    {
        return mTrackedChannel;
    }

    /**
     * Channel direction with the highest observed counts
     */
    public Direction getDirection()
    {
        return mTrackedDirection;
    }

    /**
     * Tracks the decoded LICH
     * @param lich to track
     */
    public void track(LICH lich)
    {
        if(lich == null)
        {
            return;
        }

        try
        {
            //Update RF channel count and tracked RF channel as necessary.
            mTrackerMap.get(mRFChannelBuffer[mBufferPointer]).decrement();

            mRFChannelBuffer[mBufferPointer++]  = lich.getRFChannel();
            mBufferPointer %= BUFFER_SIZE;
            mTrackerMap.get(lich.getRFChannel()).increment();

            if(mTrackedChannel != lich.getRFChannel())
            {
                Collections.sort(mTrackers);
                mTrackedChannel = mTrackers.getLast().getRFChannel();
            }

            if(lich.getDirection() == Direction.OUTBOUND)
            {
                mCountOutbound = Math.min(++mCountOutbound, BUFFER_SIZE);
                mCountInbound = Math.max(--mCountInbound, 0);
            }
            else
            {
                mCountInbound = Math.min(++mCountInbound, BUFFER_SIZE);
                mCountOutbound = Math.max(--mCountOutbound, 0);
            }

            if(lich.getDirection() != mTrackedDirection)
            {
                mTrackedDirection = (mCountOutbound > mCountInbound) ? Direction.OUTBOUND : Direction.INBOUND;
            }
        }
        catch(Exception e)
        {
            LOG.error("Error while updating LICH tracker for LICH: {}", lich);
        }
    }

    /**
     * Tracks the occurrence count of an RF channel
     */
    static class Tracker implements Comparable<Tracker>
    {
        private final RFChannel mChannel;
        private int mCount;

        /**
         * Constructs an instance
         *
         * @param channel to track occurrence counts
         */
        public Tracker(RFChannel channel)
        {
            mChannel = channel;
        }

        /**
         * Tracked channel
         */
        public RFChannel getRFChannel()
        {
            return mChannel;
        }

        /**
         * Count of occurrences
         */
        public int getCount()
        {
            return mCount;
        }

        /**
         * Increments the counter
         */
        public void increment()
        {
            mCount = Math.min(++mCount, BUFFER_SIZE);
        }

        /**
         * Decrements the counter
         */
        public void decrement()
        {
            mCount = Math.max(--mCount, 0);
        }

        @Override
        public int compareTo(Tracker o)
        {
            return Integer.compare(mCount, o.mCount);
        }

        /**
         * Resets the counter to zero
         */
        public void reset()
        {
            mCount = 0;
        }

        @Override
        public String toString()
        {
            return "COUNT: " + mCount + " " + mChannel.toString();
        }
    }
}
