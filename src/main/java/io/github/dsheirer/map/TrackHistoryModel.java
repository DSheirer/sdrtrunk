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

package io.github.dsheirer.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Location history of a plottable entity, newest first.  Thread safe: positions are added on the decode event thread
 * and read on the JavaFX thread.
 */
public class TrackHistoryModel
{
    public static final int MAX_LOCATION_HISTORY = 10;
    private final List<TimestampedGeoPosition> mTimestampedGeoPositions = new ArrayList<>();

    /**
     * Constructs an instance
     */
    public TrackHistoryModel()
    {
    }

    /**
     * Snapshot of the history for this entity, newest first.
     */
    public synchronized List<TimestampedGeoPosition> getTrackHistory()
    {
        return List.copyOf(mTimestampedGeoPositions);
    }

    /**
     * Adds the position to the history if it is unique and trims the history to MAX_LOCATION_HISTORY size.
     * @param latest position to add.
     * @return true if the position was added
     */
    public synchronized boolean add(TimestampedGeoPosition latest)
    {
        TimestampedGeoPosition mostRecentPosition = mTimestampedGeoPositions.isEmpty() ? null :
            mTimestampedGeoPositions.getFirst();

        if(isUnique(latest, mostRecentPosition))
        {
            mTimestampedGeoPositions.addFirst(latest);

            while(mTimestampedGeoPositions.size() > MAX_LOCATION_HISTORY)
            {
                mTimestampedGeoPositions.removeLast();
            }

            return true;
        }

        return false;
    }

    /**
     * Indicates if the latest time is more than 2 seconds newer than the previous position or either the latitude or
     * longitude differs by more than 0.00001 degrees.
     * @param latest location
     * @param previous location
     * @return indication of uniqueness.
     */
    private static boolean isUnique(TimestampedGeoPosition latest, TimestampedGeoPosition previous)
    {
        if(latest != null && previous == null)
        {
            return true;
        }

        if(latest != null)
        {
            return latest.getTimestamp() > (previous.getTimestamp() + 2_000) ||
                    Math.abs(latest.getLatitude() - previous.getLatitude()) > 0.00001 ||
                    Math.abs(latest.getLongitude() - previous.getLongitude()) > 0.00001;
        }

        return false;
    }

    /**
     * Get the geo position at the specified index
     * @param index to retrieve, 0 for the newest
     * @return geo or null.
     */
    public synchronized TimestampedGeoPosition get(int index)
    {
        if(index >= 0 && index < mTimestampedGeoPositions.size())
        {
            return mTimestampedGeoPositions.get(index);
        }

        return null;
    }

    /**
     * Number of positions in the history
     */
    public synchronized int size()
    {
        return mTimestampedGeoPositions.size();
    }
}
