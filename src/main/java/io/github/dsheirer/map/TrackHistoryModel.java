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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Track history table model.
 */
public class TrackHistoryModel extends AbstractTableModel
{
    public static final int MAX_LOCATION_HISTORY = 10;
    private static final String[] COLUMNS = new String[]{"Time", "Latitude", "Longitude"};
    private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DecimalFormat mDegreeFormat = new DecimalFormat("0.00000");
    private final List<TimestampedGeoPosition> mTimestampedGeoPositions = new ArrayList<>();

    /**
     * Constructs an instance
     */
    public TrackHistoryModel()
    {
    }

    /**
     * Complete history for this entity.
     */
    public List<TimestampedGeoPosition> getTrackHistory()
    {
        return Collections.unmodifiableList(mTimestampedGeoPositions);
    }

    /**
     * Adds the position to the history if it is unique and trims the history to MAX_LOCATION_HISTORY size.
     * @param latest position to add.
     */
    public void add(TimestampedGeoPosition latest)
    {
        TimestampedGeoPosition mostRecentPosition = null;

        if(!mTimestampedGeoPositions.isEmpty())
        {
            mostRecentPosition = mTimestampedGeoPositions.getFirst();
        }

        if(isUnique(latest, mostRecentPosition))
        {
            mTimestampedGeoPositions.addFirst(latest);
            fireTableRowsInserted(0, 0);

            while(mTimestampedGeoPositions.size() > MAX_LOCATION_HISTORY)
            {
                int index = mTimestampedGeoPositions.size() - 1;
                mTimestampedGeoPositions.removeLast();
                fireTableRowsDeleted(index, index);
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
     * @param index to retrieve
     * @return geo or null.
     */
    public TimestampedGeoPosition get(int index)
    {
        if(index >= 0 && index < mTimestampedGeoPositions.size())
        {
            return mTimestampedGeoPositions.get(index);
        }

        return null;
    }

    @Override
    public int getRowCount()
    {
        return mTimestampedGeoPositions.size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column)
    {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        TimestampedGeoPosition geoPosition = mTimestampedGeoPositions.get(rowIndex);
        switch (columnIndex)
        {
            case 0:
                return mSimpleDateFormat.format(geoPosition.getTimestamp());
            case 1:
                return mDegreeFormat.format(geoPosition.getLatitude());
            case 2:
                return mDegreeFormat.format(geoPosition.getLongitude());
        }

        return null;
    }
}
