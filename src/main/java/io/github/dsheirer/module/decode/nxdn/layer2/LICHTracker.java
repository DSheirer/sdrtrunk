/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import java.util.Arrays;

/**
 * Tracks the observed channel type and direction using a best two of three recently observed LICH configurations
 *
 * These tracked values are used to identify the closest matching (unknown) channel type/direction LICH entry when the
 * decodec frame LICH has errors and doesn't exactly match any valid LICH configuration.
 */
public class LICHTracker
{
    private final RFChannel[] mRFChannelBuffer = new RFChannel[3];
    private final Direction[] mDirectionBuffer = new Direction[3];
    private int mBufferPointer = 0;
    private RFChannel mTrackedChannel = RFChannel.UNKNOWN;
    private Direction mTrackedDirection = Direction.OUTBOUND;

    /**
     * Constructs an instance
     */
    public LICHTracker()
    {
        reset();
    }

    /**
     * Resets the channel tracked values.
     */
    public void reset()
    {
        Arrays.fill(mRFChannelBuffer, RFChannel.UNKNOWN);
        Arrays.fill(mDirectionBuffer, Direction.OUTBOUND);
        mTrackedChannel = RFChannel.UNKNOWN;
        mTrackedDirection = Direction.OUTBOUND;
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
        mRFChannelBuffer[mBufferPointer]  = lich.getRFChannel();
        mDirectionBuffer[mBufferPointer++] = lich.getDirection();
        mBufferPointer %= 3;

        //Best 2 out of 3 - if either of the two previous channel types match this LICH, set that as the tracked channel.
        if(mTrackedChannel != lich.getRFChannel() &&
                (mRFChannelBuffer[mBufferPointer] == lich.getRFChannel() ||
                 mRFChannelBuffer[(mBufferPointer + 1) % 3] == lich.getRFChannel()))
        {
            mTrackedChannel = lich.getRFChannel();
        }

        if(mTrackedDirection != lich.getDirection() &&
                (mDirectionBuffer[mBufferPointer] == lich.getDirection() ||
                 mDirectionBuffer[(mBufferPointer + 1) % 3] == lich.getDirection()))
        {
            mTrackedDirection = lich.getDirection();
        }
    }
}
