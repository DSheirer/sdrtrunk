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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.identifier.P25Location;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlOpcode;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisGPS;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import io.github.dsheirer.protocol.Protocol;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * L3Harris Talker GPS Complete.  This message is assembled from FLC L3Harris Talker GPS Block 1 and Block 2.
 */
public class LCHarrisTalkerGPSComplete extends LinkControlWord implements IMessage
{
    private static final DecimalFormat GPS_FORMAT = new DecimalFormat("0.000000");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");
    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    private P25Location mLocation;
    private GeoPosition mGeoPosition;
    private List<Identifier> mIdentifiers;
    private long mTimestamp;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     * @param message constructed from block 1 and block 2 fragments
     */
    private LCHarrisTalkerGPSComplete(CorrectedBinaryMessage message, long timestamp)
    {
        super(message);
        mTimestamp = timestamp;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    @Override
    public int getTimeslot()
    {
        return P25P1Message.TIMESLOT_0;
    }

    /**
     * Constructs an instance of this message from block 1 and block 2 FLC GPS message fragments.
     * @param block1 FLC Harris Talker GPS
     * @param block2 FLC Harris Talker GPS
     * @param timestamp for the message
     * @return instance of this message.
     */
    public static LCHarrisTalkerGPSComplete create(LCHarrisTalkerGPSBlock1 block1, LCHarrisTalkerGPSBlock2 block2,
                                                   long timestamp)
    {
        CorrectedBinaryMessage cbm = new CorrectedBinaryMessage(112);
        cbm.load(0, block1.getMessage().getSubMessage(16, 72));
        cbm.load(56, block2.getMessage().getSubMessage(16, 72));
        return new LCHarrisTalkerGPSComplete(cbm, timestamp);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("L3HARRIS TALKER GPS ");
        GeoPosition geo = getGeoPosition();
        sb.append(GPS_FORMAT.format(geo.getLatitude())).append(" ").append(GPS_FORMAT.format(geo.getLongitude()));
        sb.append(" HEADING:").append(getHeading());
        sb.append(" TIME:").append(SDF.format(getTimestampMs()));
        sb.append(" UTC MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    @Override
    public Vendor getVendor()
    {
        return Vendor.HARRIS;
    }

    @Override
    public LinkControlOpcode getOpcode()
    {
        return LinkControlOpcode.L3HARRIS_TALKER_GPS_COMPLETE;
    }

    @Override
    public int getOpcodeNumber()
    {
        return getOpcode().getCode();
    }

    /**
     * Heading
     * @return heading, 0-359 degrees.
     */
    public int getHeading()
    {
        return L3HarrisGPS.parseHeading(getMessage(), 0);
    }

    /**
     * GPS Position time in milliseconds.
     * @return time in ms UTC
     */
    public long getTimestampMs()
    {
        return L3HarrisGPS.parseTimestamp(getMessage(), 0);
    }

    /**
     * GPS Location
     * @return location in decimal degrees
     */
    public P25Location getLocation()
    {
        if(mLocation == null)
        {
            GeoPosition geoPosition = getGeoPosition();
            mLocation = P25Location.createFrom(geoPosition.getLatitude(), geoPosition.getLongitude());
        }

        return mLocation;
    }

    /**
     * Geo position
     * @return position
     */
    public GeoPosition getGeoPosition()
    {
        if(mGeoPosition == null)
        {
            mGeoPosition = new GeoPosition(L3HarrisGPS.parseLatitude(getMessage(), 0),
                L3HarrisGPS.parseLongitude(getMessage(), 0));
        }

        return mGeoPosition;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getLocation());
        }

        return mIdentifiers;
    }
}
