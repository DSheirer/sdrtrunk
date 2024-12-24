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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages alternate CRC masks that may be employed in a system.  Some vendors employ non-standard CRC checksum masks
 * or initial fill values.  This utility tracks these non-standard values by observation count and time and accepts
 * these alternate mask values to automatically correct or validate messages when those messages employ these
 * alternate mask values.  Alternate mask values must be observed at least 3x times in a 2-minute window against the
 * same message content, in order for the alternate mask value to be used.  Tracked values that do not get another
 * observation count within a 2-minute window will be ejected from the cache and excluded as a valid CRC mask candidate.
 *
 * Notes: different alternate CRC mask values can be used independently in each timeslot.  Further, I've seen multiple
 * CRC mask values being employed within the same timeslot.  This indicates that systems can use multiple CRC mask
 * values against CSBK messages and some other function is involved in determining which CRC mask value to use when
 * checking the CRC of the transmitted CSBK ... fun and games!
 */
public class DMRCrcMaskManager
{
    private Logger LOGGER = LoggerFactory.getLogger(DMRCrcMaskManager.class);
    private Map<Integer, MaskTracker> mCsbkTrackerMapTS1 = new TreeMap<>();
    private Map<Integer, MaskTracker> mCsbkTrackerMapTS2 = new TreeMap<>();
    private Set<MaskTracker> mSortedTrackersTS1 = new TreeSet<>();
    private Set<MaskTracker> mSortedTrackersTS2 = new TreeSet<>();

    /**
     * Constructs an instance
     */
    public DMRCrcMaskManager()
    {
    }

    /**
     * Logs the current tracked CRC values and counts.
     */
    public void log()
    {

        StringBuilder sb = new StringBuilder();

        sb.append("DMR CRC Mask Manager - TS1 Current Tracked Values\n");
        for(MaskTracker tracker: mSortedTrackersTS1)
        {
            sb.append("\tTracked Value: ").append(String.format("0x%04X", tracker.getTrackedValue()));
            sb.append(" Count:").append(tracker.getObservationCount());
            sb.append(" Last Observed: " + new Date(tracker.getLastUpdated())).append("\n");
        }

        sb.append("DMR CRC Mask Manager - TS2 Current Tracked Values\n");
        for(MaskTracker tracker: mSortedTrackersTS2)
        {
            sb.append("\tTracked Value: ").append(String.format("0x%04X", tracker.getTrackedValue()));
            sb.append(" Count:").append(tracker.getObservationCount());
            sb.append(" Last Observed: " + new Date(tracker.getLastUpdated())).append("\n");
        }

        LOGGER.info(sb.toString());
    }

    /**
     * Checks the CSBK message that has a failed CRC checksum to detect if an alternate CRC mask value is employed and
     * if so, attempts to correct the message.
     * @param csbk to re-check using the currently tracked mask value.
     */
    public void check(CSBKMessage csbk)
    {
        if(!csbk.isValid())
        {
            int alternateMask = CRCDMR.calculateResidual(csbk.getMessage(), 0, 80);

            Map<Integer,MaskTracker> map;
            Set<MaskTracker> sortedSet;

            if(csbk.getTimeslot() == 1)
            {
                map = mCsbkTrackerMapTS1;
                sortedSet = mSortedTrackersTS1;
            }
            else
            {
                map = mCsbkTrackerMapTS2;
                sortedSet = mSortedTrackersTS2;
            }

            if(map.containsKey(alternateMask))
            {
                MaskTracker matchingTracker = map.get(alternateMask);
                matchingTracker.increment(csbk.getTimestamp());

                if(matchingTracker.isValid())
                {
                    csbk.checkCRC(matchingTracker.getTrackedValue());
                }
            }
            else
            {
                //Attempt to use other tracked mask values in order of observed count and also remove stale trackers
                boolean found = false;
                MaskTracker next;
                Iterator<MaskTracker> it = sortedSet.iterator();
                while(it.hasNext())
                {
                    next = it.next();

                    //If the tracker is stale, remove it
                    if(next.isStale(csbk.getTimestamp()))
                    {
                        it.remove();
                        map.remove(next.getTrackedValue());
                    }
                    else
                    {
                        //If we haven't found a match yet, attempt to correct the message using this tracked mask value
                        if(!found)
                        {
                            csbk.checkCRC(next.getTrackedValue());

                            if(csbk.isValid())
                            {
                                found = true;
                                next.increment(csbk.getTimestamp());
                            }
                        }
                    }
                }

                //If we didn't find a perfect match or even a close match (with 1 bit error), create a new tracked value.
                if(!found)
                {
                    MaskTracker tracker = new MaskTracker(alternateMask, csbk.getTimestamp());
                    map.put(alternateMask, tracker);
                    sortedSet.add(tracker);
                }
            }
        }
    }

    /**
     * Alternate CRC mask value tracker.  Tracks the number of observances of the mask value to detect when an alternate
     * CRC mask pattern is employed and can then be used to automatically correct CRC values for messages.
     */
    public class MaskTracker implements Comparable<MaskTracker>
    {
        private static final long STALENESS_TIME_THRESHOLD = TimeUnit.MINUTES.toMillis(2);
        private static final int STALENESS_COUNT_THRESHOLD = 100;
        private int mTrackedValue;
        private int mObservationCount;
        private int mStalenessCount;
        private long mLastUpdated;

        /**
         * Constructs an instance for the specified mask, sets the observation count to one and sets the last update to
         * the supplied timestamp.
         * @param residual value to track.
         * @param timestamp for the observation
         */
        public MaskTracker(int residual, long timestamp)
        {
            mTrackedValue = residual;
            mObservationCount = 1;
            mLastUpdated = timestamp;
        }

        /**
         * Count of observations for this mask value.
         * @return count
         */
        public int getObservationCount()
        {
            return mObservationCount;
        }

        /**
         * Indicates that the residual value has been observed at least 3x times within the preceding 2 minute window,
         * indicating that it is (likely) valid.
         * @return true if the tracked mask value is valid.
         */
        public boolean isValid()
        {
            return mObservationCount >= 3;
        }

        /**
         * Indicates if this tracker is stale, relative to the supplied timestamp.  Stale indicates that this tracked
         * value hasn't been observed in the preceding 2 minutes, or has been checked for staleness more than 100 times
         * without an updated observation.
         * @param timestamp to compare for staleness check.
         * @return true if this tracker is stale.
         */
        public boolean isStale(long timestamp)
        {
            mStalenessCount++;
            return mStalenessCount > STALENESS_COUNT_THRESHOLD ||
                    Math.abs(timestamp - mLastUpdated) > STALENESS_TIME_THRESHOLD;
        }

        /**
         * Timestamp when this value was last observed.
         */
        public long getLastUpdated()
        {
            return mLastUpdated;
        }

        /**
         * Tracked alternate mask value.
         * @return tracked mask value
         */
        public int getTrackedValue()
        {
            return mTrackedValue;
        }

        /**
         * Increment the observation count and use the supplied timestamp as the last updated timestamp.
         * @param timestamp for the observed message.
         */
        public void increment(long timestamp)
        {
            mStalenessCount = 0;
            mObservationCount++;
            //Detect integer rollover and apply max integer value.
            if(mObservationCount < 0)
            {
                mObservationCount = Integer.MAX_VALUE;
            }
            mLastUpdated = timestamp;
        }

        /**
         * Custom (reversed) sort order by largest count and then by latest updated timestamp.
         * @param o the object to be compared.
         * @return comparison value.
         */
        @Override
        public int compareTo(MaskTracker o)
        {
            //Multiply by -1 to apply a reverse ordering
            int comparison = Integer.compare(getObservationCount(), o.getObservationCount()) * -1;

            if(comparison == 0)
            {
                comparison = Long.compare(getLastUpdated(), o.getLastUpdated()) * -1;
            }

            return comparison;
        }
    }
}
