/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides duplicate decode event detection support
 */
public class DecodeEventDuplicateDetector
{
    private static final long EVENT_MAX_AGE_MILLISECONDS = Duration.ofMinutes(1).toMillis();
    private Map<DecodeEventType,DecodeEventTracker> mTrackerMap = new HashMap<>();

    /**
     * Indicates if the event is a duplicate event.
     * @param event to check
     * @param timestamp of a current message to trigger time-based event age off.
     * @return true if the event is a duplicate.
     */
    public synchronized boolean isDuplicate(IDecodeEvent event, long timestamp)
    {
        //Null event types and voice call event types are not tracked by this detector
        if(event.getEventType() == null || event.getEventType().isVoiceCallEvent())
        {
            return false;
        }

        if(!mTrackerMap.containsKey(event.getEventType()))
        {
            mTrackerMap.put(event.getEventType(), new DecodeEventTracker());
        }

        boolean duplicate = mTrackerMap.get(event.getEventType()).isDuplicate(event);

        for(DecodeEventTracker tracker: mTrackerMap.values())
        {
            tracker.ageOff(timestamp);
        }

        return duplicate;
    }

    /**
     * Tracks all decode events for a given decode event type
     */
    private class DecodeEventTracker
    {
        private Map<String,IDecodeEvent> mDecodeEventMap = new HashMap<>();

        /**
         * Checks the event for duplicate
         * @param event to check
         * @return true if duplicate
         */
        public boolean isDuplicate(IDecodeEvent event)
        {
            if(event.getIdentifierCollection().getToIdentifier() == null || event.getDetails() == null)
            {
                return false;
            }

            String key = getKey(event);

            if(key == null || key.isEmpty())
            {
                return false;
            }

            if(mDecodeEventMap.containsKey(key))
            {
                return true;
            }
            else
            {
                mDecodeEventMap.put(key, event);
            }

            return false;
        }

        /**
         * Generates a unique event key that is a combinatio of the TO identifier value and the details of the event.
         * @param event for the key
         * @return generated key.
         */
        private static String getKey(IDecodeEvent event)
        {
            return event.getIdentifierCollection().getToIdentifier().toString() + event.getDetails();
        }

        /**
         * Removes all decode events from the duplicate detection map that are too old.
         * @param timestamp for the current messaging.
         */
        public void ageOff(long timestamp)
        {
            long threshold = timestamp - EVENT_MAX_AGE_MILLISECONDS;
            List<String> toRemove = new ArrayList<>();

            for(Map.Entry<String,IDecodeEvent> entry: mDecodeEventMap.entrySet())
            {
                if(entry.getValue() == null || entry.getValue().getTimeStart() < threshold)
                {
                    toRemove.add(entry.getKey());
                }
            }

            for(String key: toRemove)
            {
                mDecodeEventMap.remove(key);
            }
        }
    }
}
