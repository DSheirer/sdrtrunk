package io.github.dsheirer.map;

import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * Timestamped geo position
 */
public class TimestampedGeoPosition extends GeoPosition
{
    private long mTimestamp;

    /**
     * Constructs an instance
     * @param latitude for the position
     * @param longitude for the position
     * @param timestamp in milliseconds
     */
    public TimestampedGeoPosition(GeoPosition position, long timestamp)
    {
        super(position.getLatitude(), position.getLongitude());
        mTimestamp = timestamp;
    }

    /**
     * Timestamp for the geo position
     * @return timestamp in milliseconds.
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }
}
