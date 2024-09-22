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

package io.github.dsheirer.map;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import java.util.ArrayList;
import java.util.List;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * Plottable entity history with location history.
 */
public class PlottableEntityHistory
{
    public static final int MAX_LOCATION_HISTORY = 10;
    private List<TimestampedGeoPosition> mLocationHistory = new ArrayList<>();
    private PlottableDecodeEvent mCurrentEvent;
    private Identifier mIdentifier;

    /**
     * Constructs a plottable entity history
     */
    public PlottableEntityHistory(Identifier identifier, PlottableDecodeEvent event)
    {
        mIdentifier = identifier;
        add(event);
    }

    /**
     * Location history for this entity
     */
    public List<TimestampedGeoPosition> getLocationHistory()
    {
        return new ArrayList<>(mLocationHistory);
    }

    /**
     * Latest position for this entity.
     */
    public TimestampedGeoPosition getLatestPosition()
    {
        if(mLocationHistory.size() > 0)
        {
            return mLocationHistory.get(0);
        }

        return null;
    }

    /**
     * Identifier for this plottable
     */
    public Identifier getIdentifier()
    {
        return mIdentifier;
    }

    /**
     * Identifier collection from the latest event for this plottable
     */
    public IdentifierCollection getIdentifierCollection()
    {
        return mCurrentEvent.getIdentifierCollection();
    }

    /**
     * Updates the entity history with a location from the latest decode event
     */
    public void add(PlottableDecodeEvent event)
    {
        if(event.isValidLocation())
        {
            TimestampedGeoPosition mostRecentPosition = getLatestPosition();
            TimestampedGeoPosition latest = new TimestampedGeoPosition(event.getLocation(), event.getTimeStart());

            if(isUnique(latest, mostRecentPosition))
            {
                mCurrentEvent = event;
                mLocationHistory.add(0, latest);

                while(mLocationHistory.size() > MAX_LOCATION_HISTORY)
                {
                    mLocationHistory.remove(mLocationHistory.size() - 1);
                }
            }
        }
    }

    /**
     * Indicates if the latest time and position is at least 30 seconds newer than the previous position and either the
     * latitude or longitude differs by at least 0.00001 degrees.
     * @param latest location
     * @param previous location
     * @return indication of uniqueness.
     */
    private boolean isUnique(TimestampedGeoPosition latest, TimestampedGeoPosition previous)
    {
        if(latest != null && previous == null)
        {
            return true;
        }

        if(latest != null && previous != null)
        {
            return latest.getTimestamp() > (previous.getTimestamp() + 30_000) ||
                   Math.abs(latest.getLatitude() - previous.getLatitude()) > 0.00001 ||
                   Math.abs(latest.getLongitude() - previous.getLongitude()) > 0.00001;
        }

        return false;
    }
}
