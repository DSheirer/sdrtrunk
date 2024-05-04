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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.location.LocationIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRLocation;
import io.github.dsheirer.module.decode.dmr.message.type.PositionError;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * GPS Information
 *
 * ETSI TS 102 361-2 7.1.1.3
 */
public class GPSInformation extends FullLCMessage
{
    private static final double LATITUDE_UNITS = 180.0 / FastMath.pow(2.0, 24.0);
    private static final double LONGITUDE_UNITS = 360.0 / FastMath.pow(2.0, 25.0);
    private static final int[] POSITION_ERROR = new int[]{20, 21, 22};
    private static final int LONGITUDE_START = 23;
    private static final int LONGITUDE_END = 47;
    private static final int LATITUDE_START = 48;
    private static final int LATITUDE_END = 71;

    private DMRLocation mGPSLocation;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     */
    public GPSInformation(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("FLC GPS LOCATION ");
        sb.append(getGPSLocation().toString());
        sb.append(" POSITION ERROR:").append(getPositionError().toString());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Position error report
     */
    public PositionError getPositionError()
    {
        return PositionError.fromValue(getMessage().getInt(POSITION_ERROR));
    }

    /**
     * Latitude in decimal degrees
     */
    public double getLatitude()
    {
        return getMessage().getTwosComplement(LATITUDE_START, LATITUDE_END) * LATITUDE_UNITS;
    }

    /**
     * Longitude in decimal degrees
     */
    public double getLongitude()
    {
        return getMessage().getTwosComplement(LONGITUDE_START, LONGITUDE_END) * LONGITUDE_UNITS;
    }

    /**
     * Geo-position for the lat/lon
     */
    public GeoPosition getPosition()
    {
        return new GeoPosition(getLatitude(), getLongitude());
    }

    /**
     * GPS location as an identifier
     */
    public LocationIdentifier getGPSLocation()
    {
        if(mGPSLocation == null)
        {
            mGPSLocation = DMRLocation.createFrom(getLatitude(), getLongitude());
        }

        return mGPSLocation;
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getGPSLocation());
        }

        return mIdentifiers;
    }

    public static void main(String[] args)
    {
        CorrectedBinaryMessage cbm = new CorrectedBinaryMessage(BinaryMessage.loadHex("08000F8A177E3903C230"));
        GPSInformation gps = new GPSInformation(cbm, 0, 1);
        System.out.println(gps);
    }
}
