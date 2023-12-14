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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages alternate CRC masks that may be employed in a system.
 */
public class DmrCrcMaskManager
{
    private Logger LOGGER = LoggerFactory.getLogger(DmrCrcMaskManager.class);
    private Map<Integer, ResidualCrcMaskTracker> mCsbkResidualTrackerMap = new TreeMap<>();
    private int mDominantMask = 0;

    /**
     * Constructs an instance
     */
    public DmrCrcMaskManager()
    {
    }

    /**
     * Creates a deep copy (ie clone) of all tracked values
     * @return list of tracker clones.
     */
    public List<ResidualCrcMaskTracker> cloneTrackers()
    {
        List<ResidualCrcMaskTracker>trackers = new ArrayList<>();
        for(ResidualCrcMaskTracker tracker: mCsbkResidualTrackerMap.values())
        {
            trackers.add(tracker.clone());
        }

        return trackers;
    }

    /**
     * Logs the current tracked CRC values and counts.
     */
    public void log()
    {
        List<ResidualCrcMaskTracker> trackers = new ArrayList<>(mCsbkResidualTrackerMap.values());
        Collections.sort(trackers);

        StringBuilder sb = new StringBuilder();
        sb.append("DMR CRC Mask Manager - Current Tracked Values\n");
        for(ResidualCrcMaskTracker tracker: trackers)
        {
            sb.append("\tTracked Value: ").append(String.format("0x%04X", tracker.getTrackedValue()));
            sb.append(" Count:").append(tracker.getCount());
            sb.append(" Last Observed: " + new Date(tracker.getLastUpdated())).append("\n");
        }

        LOGGER.info(sb.toString());
    }

    /**
     * Checks the CSBK message that has a failed CRC checksum to detect if an alternate CRC mask value is employed and
     * if so, attempts to correct the message by using the alternate mask value once the alternate mask value is
     * detected to be used consistently enough.
     * @param csbk to re-check.
     */
    public void check(CSBKMessage csbk)
    {
        if(!csbk.isValid())
        {
            int residual = CRCDMR.calculateResidual(csbk.getMessage(), 0, 80);

            if(mCsbkResidualTrackerMap.containsKey(residual))
            {
                mCsbkResidualTrackerMap.get(residual).increment();
            }
            else
            {
                mCsbkResidualTrackerMap.put(residual, new ResidualCrcMaskTracker(residual));
            }

            List<ResidualCrcMaskTracker> trackers = new ArrayList<>(mCsbkResidualTrackerMap.values());
            Collections.sort(trackers);

            if(trackers.size() > 5)
            {
                mCsbkResidualTrackerMap.remove(trackers.get(0).getTrackedValue());
            }

            if(trackers.size() > 0)
            {
                ResidualCrcMaskTracker dominant = trackers.get(trackers.size() - 1);

                if(dominant.isValid())
                {
                    mDominantMask = dominant.getTrackedValue();
                }
            }

            if(mDominantMask != 0)
            {
                csbk.checkCRC(mDominantMask);
//
//                if(csbk.isValid())
//                {
//                    LOGGER.info("CSBK fixed using alternate mask: " + Integer.toHexString(mDominantMask).toUpperCase());
//                }
            }
        }
    }

    /**
     * Residual CRC mask value tracker.  Tracks the number of observances of a residual CRC calculated value to detect
     * when an alternate CRC mask pattern is employed and automatically correct CRC values for messages.
     */
    public class ResidualCrcMaskTracker implements Comparable<ResidualCrcMaskTracker>
    {
        private int mTrackedValue;
        private int mCount;
        private long mLastUpdated;

        /**
         * Constructs an instance for the specified residual, sets the count to one and sets the last update to now.
         * @param residual value to track.
         */
        public ResidualCrcMaskTracker(int residual)
        {
            mTrackedValue = residual;
            mCount = 1;
            mLastUpdated = System.currentTimeMillis();
        }

        /**
         * Create a deep copy of this instance.
         * @return cloned instance.
         */
        public ResidualCrcMaskTracker clone()
        {
            ResidualCrcMaskTracker clone = new ResidualCrcMaskTracker(getTrackedValue());
            clone.mCount = mCount;
            clone.mLastUpdated = mLastUpdated;
            return clone;
        }

        /**
         * Count of number of times this residual has been observed.
         * @return count
         */
        public int getCount()
        {
            return mCount;
        }

        /**
         * Indicates that the residual value has been observed at least 3x times indicating that it is possibly valid.
         * @return
         */
        public boolean isValid()
        {
            return mCount >= 3;
        }

        /**
         * Timestamp when this value was last observed.
         */
        public long getLastUpdated()
        {
            return mLastUpdated;
        }

        /**
         * Residual tracked value.
         * @return residual
         */
        public int getTrackedValue()
        {
            return mTrackedValue;
        }

        public void increment()
        {
            mCount++;
            //Prevent rollover by keeping value at max integer value.
            if(mCount < 0)
            {
                mCount = Integer.MAX_VALUE;
            }
            mLastUpdated = System.currentTimeMillis();
        }

        /**
         * Custom sort order by count and then by last updated timestamp.
         * @param o the object to be compared.
         * @return
         */
        @Override
        public int compareTo(ResidualCrcMaskTracker o)
        {
            int comparison = Integer.compare(getCount(), o.getCount());

            if(comparison == 0)
            {
                comparison = Long.compare(getLastUpdated(), o.getLastUpdated());
            }

            return comparison;
        }
    }
}
