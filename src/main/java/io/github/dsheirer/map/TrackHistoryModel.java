package io.github.dsheirer.map;

import javax.swing.table.AbstractTableModel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Table model for track history.
 */
public class TrackHistoryModel extends AbstractTableModel
{
    private static final String[] COLUMNS = new String[]{"Time", "Latitude", "Longitude"};
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("HH:mm:ss");
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
