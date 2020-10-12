/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.module.decode.dmr.message.CACH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks proper alignment of timeslots into the two bursts of a burst framer.
 */
public class TimeslotAlignmentTracker
{
    private final static Logger mLog = LoggerFactory.getLogger(TimeslotAlignmentTracker.class);

    private int mBurst1Ts1 = 0;
    private int mBurst1Ts2 = 0;
    private int mBurst2Ts1 = 0;
    private int mBurst2Ts2 = 0;

    /**
     * Indicates if the track has accumulated enough cach data to accurately declare timeslot alignment.  The test
     * checks that both timeslots each have different counts for timeslot 0 and 1 and that both timeslots each indicate
     * a different dominant timeslot than the other.
     */
    public boolean hasSufficientData()
    {
        return ((mBurst1Ts1 > mBurst1Ts2 && mBurst2Ts2 > mBurst2Ts1) ||
                (mBurst1Ts1 < mBurst1Ts2 && mBurst2Ts2 < mBurst2Ts1) ||
                (mBurst1Ts1 == 0 && mBurst1Ts2 == 0 && mBurst2Ts1 != mBurst2Ts2) ||
                (mBurst1Ts1 != mBurst1Ts2 && mBurst2Ts1 == 0 && mBurst1Ts2 == 0));
    }

    /**
     * Indicates if the timeslots are aligned by inspection of the CACH value for frame 0 and frame 1
     */
    public boolean isAligned()
    {
        return (mBurst1Ts1 > mBurst1Ts2 && mBurst2Ts2 > mBurst2Ts1) ||
               (mBurst1Ts1 == 0 && mBurst1Ts2 == 0 && mBurst2Ts2 > mBurst2Ts1) ||
               (mBurst1Ts1 > mBurst1Ts2 && mBurst2Ts1 == 0 && mBurst2Ts2 == 0);
    }

    /**
     * Updates the tracker with the cach fields or sync patterns from the current TDMA burst frames
     * @param cach1 optional cach that is currently aligned as timeslot 1
     * @param cach2 optional cach that is currently aligned as timeslot 2
     * @param pattern1 sync pattern for the burst currently aligned as timeslot 1
     * @param pattern2 sync pattern for the burst currently aligned as timeslot 2
     */
    public void update(CACH cach1, CACH cach2, DMRSyncPattern pattern1, DMRSyncPattern pattern2)
    {
        if(cach1 != null && cach1.isValid())
        {
            if(cach1.isTimeslot0())
            {
                mBurst1Ts1++;
            }
            else
            {
                mBurst1Ts2++;
            }
        }

        if(cach2 != null && cach2.isValid())
        {
            if(cach2.isTimeslot0())
            {
                mBurst2Ts1++;
            }
            else
            {
                mBurst2Ts2++;
            }
        }

        if(pattern1.isDirectMode())
        {
            switch((pattern1))
            {
                case DIRECT_MODE_DATA_TIMESLOT_1:
                case DIRECT_MODE_VOICE_TIMESLOT_1:
                    mBurst1Ts1++;
                    break;
                case DIRECT_MODE_DATA_TIMESLOT_2:
                case DIRECT_MODE_VOICE_TIMESLOT_2:
                    mBurst1Ts2++;
                    break;
            }
        }

        if(pattern2.isDirectMode())
        {
            switch((pattern2))
            {
                case DIRECT_MODE_DATA_TIMESLOT_1:
                case DIRECT_MODE_VOICE_TIMESLOT_1:
                    mBurst2Ts1++;
                    break;
                case DIRECT_MODE_DATA_TIMESLOT_2:
                case DIRECT_MODE_VOICE_TIMESLOT_2:
                    mBurst2Ts2++;
                    break;
            }
        }
    }

    /**
     * Resets the tracker values.
     */
    public void reset()
    {
        mBurst1Ts1 = 0;
        mBurst1Ts2 = 0;
        mBurst2Ts1 = 0;
        mBurst2Ts2 = 0;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("BURST 1: ").append(mBurst1Ts1).append("/").append(mBurst1Ts2);
        sb.append(" BURST 2:").append(mBurst2Ts1).append("/").append(mBurst2Ts2);
        if(hasSufficientData())
        {
            if(isAligned())
            {
                sb.append(" **ALIGNED**");
            }
            else
            {
                sb.append(" >>MIS-ALIGNED<<");
            }
        }
        return sb.toString();
    }
}
