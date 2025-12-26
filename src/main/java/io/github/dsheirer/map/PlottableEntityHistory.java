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

package io.github.dsheirer.map;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;

/**
 * Plottable entity history with location history.
 */
public class PlottableEntityHistory
{
    private final TrackHistoryModel mTrackHistoryModel = new TrackHistoryModel();
    private PlottableDecodeEvent mCurrentEvent;
    private final Identifier mIdentifier;

    /**
     * Constructs a plottable entity history
     */
    public PlottableEntityHistory(Identifier identifier, PlottableDecodeEvent event)
    {
        mIdentifier = identifier;
        mCurrentEvent = event;
        add(event);
    }

    /**
     * Location history for this entity
     */
    public TrackHistoryModel getTrackHistoryModel()
    {
        return mTrackHistoryModel;
    }

    /**
     * Latest position for this history
     */
    public TimestampedGeoPosition getLatestPosition()
    {
        return mTrackHistoryModel.get(0);
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
            mTrackHistoryModel.add(new TimestampedGeoPosition(event.getLocation(), event.getTimeStart()));
        }

        mCurrentEvent = event;
    }
}
