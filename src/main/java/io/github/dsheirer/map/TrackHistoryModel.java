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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model for track history.
 */
public class TrackHistoryModel extends AbstractTableModel
{
    private static final String[] COLUMNS = new String[]{"Time", "Latitude", "Longitude"};
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private DecimalFormat mDegreeFormat = new DecimalFormat("0.00000");
    private List<TimestampedGeoPosition> mTimestampedGeoPositions = new ArrayList<>();
    private PlottableEntityHistory mPlottableEntityHistory;

    /**
     * Constructs an instance
     */
    public TrackHistoryModel()
    {
    }

    /**
     * Get the geo position at the specified index
     * @param index to retrieve
     * @return geo or null.
     */
    public TimestampedGeoPosition get(int index)
    {
        if(index < mTimestampedGeoPositions.size())
        {
            return mTimestampedGeoPositions.get(index);
        }

        return null;
    }

    /**
     * Loads the plottable entity history into this model
     * @param plottableEntityHistory to load
     */
    public void load(PlottableEntityHistory plottableEntityHistory)
    {
        mPlottableEntityHistory = plottableEntityHistory;

        if(mTimestampedGeoPositions.size() > 0)
        {
            int lastRow = mTimestampedGeoPositions.size() - 1;
            mTimestampedGeoPositions.clear();
            fireTableRowsDeleted(0, lastRow);
        }

        if(mPlottableEntityHistory != null)
        {
            mTimestampedGeoPositions.addAll(mPlottableEntityHistory.getLocationHistory());

            if(mTimestampedGeoPositions.size() > 0)
            {
                fireTableRowsInserted(0, mTimestampedGeoPositions.size() - 1);
            }
        }
    }

    /**
     * Updates or refreshes the track history from the current plottable entity.
     */
    public void update()
    {
        if(mPlottableEntityHistory != null)
        {
            List<TimestampedGeoPosition> geos = mPlottableEntityHistory.getLocationHistory();
            Collections.reverse(geos);

            for(TimestampedGeoPosition geo: geos)
            {
                if(!mTimestampedGeoPositions.contains(geo))
                {
                    mTimestampedGeoPositions.add(0, geo);
                    fireTableRowsInserted(0, 0);
                }
            }
        }
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
