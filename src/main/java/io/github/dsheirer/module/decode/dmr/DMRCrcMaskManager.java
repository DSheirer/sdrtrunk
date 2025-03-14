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

package io.github.dsheirer.module.decode.dmr;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks alternate CRC masks that may be employed in a system.  Some vendors employ non-standard error detection and
 * correction checksum masks or initial fill values.  This utility tracks these non-standard values by observation count
 * and time and accepts these alternate mask values to automatically correct or validate messages when those messages
 * employ these alternate mask values.  Alternate mask values must be observed at least 2x times in a staleness
 * threshold window against the same message type and opcode, in order for the alternate mask value to be accepted as
 * valid.  Tracked values that do not get another observation count within the staleness time threshold will be
 * ejected from the cache and excluded as a valid CRC mask candidate.
 */
public class DMRCrcMaskManager
{
    /**
     * Maximum time age of a tracked (alternate) mask value before it's ejected from the cache.
     */
    private static final long CACHE_EJECTION_POLICY_TIME = TimeUnit.MINUTES.toMillis(2);

    /**
     * Maximum staleness count before a mask is ejected from the cache.  Staleness count is only incremented when the
     * specific type and opcode is encountered.
     */
    private static final int CACHE_EJECTION_POLICY_COUNT = 20;

    private final Map<Integer, OpcodeMaskTracker> mCsbkTrackerMap = new TreeMap<>();
    private final Map<Integer, OpcodeMaskTracker> mCrc5TrackerMap = new TreeMap<>();
    private final Map<Integer, OpcodeMaskTracker> mRs12_9TrackerMap = new TreeMap<>();
    private final boolean mIgnoreCRCChecksums;

    /**
     * Constructs an instance
     * @param ignoreCRCChecksums value used to flag all error detection as valid.
     */
    public DMRCrcMaskManager(boolean ignoreCRCChecksums)
    {
        mIgnoreCRCChecksums = ignoreCRCChecksums;
    }

    /**
     * Indicates if the decoder is configured to ignore invalid error detection and correction checksums.
     */
    public boolean isIgnoreCRCChecksums()
    {
        return mIgnoreCRCChecksums;
    }

    /**
     * Indicates if the residual value is commonly seen for this opcode.
     * @param opcode of the link control message
     * @param residual from the RS(12,9) check
     * @param timestamp of the message
     * @return true if this value is repeatedly seen for this opcode.
     */
    public boolean isValidRS12_9(int opcode, int residual, long timestamp)
    {
        return mIgnoreCRCChecksums || isValid(opcode, residual, timestamp, mRs12_9TrackerMap);
    }

    /**
     * Indicates if the residual value is commonly seen for this opcode.
     * @param opcode of the link control message
     * @param residual from the CRC-5 check
     * @param timestamp of the message
     * @return true if this value is repeatedly seen for this opcode.
     */
    public boolean isValidCRC5(int opcode, int residual, long timestamp)
    {
        return mIgnoreCRCChecksums || isValid(opcode, residual, timestamp, mCrc5TrackerMap);
    }

    /**
     * Indicates if the residual value is commonly seen for this opcode.
     * @param opcode of the link control message
     * @param residual from the CRC-5 check
     * @param timestamp of the message
     * @return true if this value is repeatedly seen for this opcode.
     */
    public boolean isValidCSBK(int opcode, int residual, long timestamp)
    {
        return mIgnoreCRCChecksums || isValid(opcode, residual, timestamp, mCsbkTrackerMap);
    }

    /**
     * Checks if the opcode and residual mask value have been seen recently, multiple times.
     * @param opcode to check
     * @param residual from the CRC or RS checksum.
     * @param timestamp of the message
     * @param trackerMap to check
     * @return true if the opcode and residual have been seen recently, multiple times.
     */
    private boolean isValid(int opcode, int residual, long timestamp, Map<Integer, OpcodeMaskTracker> trackerMap)
    {
        if(opcode >= 0)
        {
            OpcodeMaskTracker tracker = trackerMap.get(opcode);

            if(tracker != null)
            {
                return tracker.isValid(residual, timestamp);
            }
            else
            {
                trackerMap.put(opcode, new OpcodeMaskTracker(residual, timestamp));
            }
        }

        return false;
    }

    /**
     * Tracks a set of mask values for an opcode value.
     */
    public static class OpcodeMaskTracker
    {
        private final Map<Integer, MaskTracker> mTrackerMap = new TreeMap<>();

        /**
         * Constructs an instance
         * @param mask to track for the opcode
         * @param timestamp when the mask was detected, for tracking staleness
         */
        public OpcodeMaskTracker(int mask, long timestamp)
        {
            mTrackerMap.put(mask, new MaskTracker(timestamp));
        }

        /**
         * Indicates if the mask is valid by checking for multiple recent usage.
         * @param mask to check
         * @param timestamp when the mask was used
         * @return true if the mask has been seen multiple times recently
         */
        public boolean isValid(int mask, long timestamp)
        {
            boolean valid = false;

            if(mTrackerMap.containsKey(mask))
            {
                MaskTracker matchingTracker = mTrackerMap.get(mask);
                matchingTracker.increment(timestamp);
                valid = matchingTracker.isValid();
            }
            else
            {
                mTrackerMap.put(mask, new MaskTracker(timestamp));
            }

            Iterator<Map.Entry<Integer,MaskTracker>> it = mTrackerMap.entrySet().iterator();
            {
                while(it.hasNext())
                {
                    if(it.next().getValue().isStale(timestamp))
                    {
                        it.remove();
                    }
                }
            }

            return valid;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            for(Map.Entry<Integer,MaskTracker> entry: mTrackerMap.entrySet())
            {
                sb.append("\t").append("Mask [").append(entry.getKey()).append("] ").append(entry.getValue()).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Alternate CRC mask value tracker.  Tracks the number of observances of the mask value to detect when an alternate
     * CRC mask pattern is employed and can then be used to automatically correct CRC values for messages.
     */
    public static class MaskTracker implements Comparable<MaskTracker>
    {
        private int mObservationCount;
        private int mStalenessCount;
        private long mLastUpdated;

        /**
         * Constructs an instance for the specified mask, sets the observation count to one and sets the last update to
         * the supplied timestamp.
         * @param timestamp for the observation
         */
        public MaskTracker(long timestamp)
        {
            mObservationCount = 1;
            mLastUpdated = timestamp;
        }

        @Override
        public String toString()
        {
            return "Observed:" + mObservationCount + " Staleness:" + mStalenessCount;
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
         * Indicates that the residual value has been observed at least 3x times within the preceding 2-minute window,
         * indicating that it is (likely) valid.
         * @return true if the tracked mask value is valid.
         */
        public boolean isValid()
        {
            return mObservationCount >= 2;
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
            return mStalenessCount > CACHE_EJECTION_POLICY_COUNT ||
                    Math.abs(timestamp - mLastUpdated) > CACHE_EJECTION_POLICY_TIME;
        }

        /**
         * Timestamp when this value was last observed.
         */
        public long getLastUpdated()
        {
            return mLastUpdated;
        }

        /**
         * Increment the observation count and use the supplied timestamp as the last updated timestamp.
         * @param timestamp for the observed message.
         */
        public void increment(long timestamp)
        {
            mStalenessCount = 0;
            mObservationCount++;
            //Detect integer rollover and adjust to max integer value.
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
