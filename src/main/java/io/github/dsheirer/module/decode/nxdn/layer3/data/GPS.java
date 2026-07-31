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

package io.github.dsheirer.module.decode.nxdn.layer3.data;

import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * JVC/Kenwood NEXEDGE GPS position report.
 */
public class GPS extends NXDNPacketMessage
{
    private static final IntField MESSAGE_TYPE = IntField.length16(0); //Constant: 0xFFFC
    private static final IntField SATELLITES_IN_VIEW = IntField.length8(16);
    private static final IntField UNKNOWN_1 = IntField.length16(24);
    private static final IntField UNKNOWN_2 = IntField.length32(40); //0x37FF3422 or 0x3400342F ??
    private static final IntField ELEVATION_METERS = IntField.length16(72);
    private static final IntField SPEED_KPH_TENTHS = IntField.length14(90);
    private static final IntField UNKNOWN_3 = IntField.length4(104);
    private static final IntField HEADING_TENTHS = IntField.length12(108);
    private static final IntField UNKNOWN_4 = IntField.length32(120);
    private static final IntField DATE_UTC_YEAR = IntField.length7(152); //Year: 2000 - 2127
    private static final IntField DATE_UTC_MONTH = IntField.length4(159); //Month: 1-12
    private static final IntField DATE_UTC_DAY = IntField.length5(163); //Day: 0-30 (add 1 to value to get 1-31)
    private static final IntField LONGITUDE_DEGREES_MINUTES = IntField.length16(168);
    private static final IntField LONGITUDE_MINUTES_FRACTIONAL = IntField.length15(184);
    private static final int LONGITUDE_HEMISPHERE_FLAG = 199;
    private static final IntField LATITUDE_DEGREES_MINUTES = IntField.length16(200);
    private static final IntField LATITUDE_MINUTES_FRACTIONAL = IntField.length15(216);
    private static final int LATITUDE_HEMISPHERE_FLAG = 231;
    private static final IntField UNKNOWN_5 = IntField.length16(232); //Always 0x0000 ?
    private static final IntField UNKNOWN_6 = IntField.length15(248);
    private static final IntField TIME_UTC_HOURS = IntField.length5(263);
    private static final IntField TIME_UTC_MINUTES = IntField.length6(268);
    private static final IntField TIME_UTC_SECONDS = IntField.length6(274);
    private static final IntField EMPTY = IntField.length24(280); //Always 0x000000
    private static final IntField CRC_32 = IntField.length32(304);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss z");
    private static final DecimalFormat DEGREES_FORMAT = new DecimalFormat("0.000000");
    private Long mTimestamp;

    /**
     * Constructs an instance
     *
     * @param packetSequence reassembled from Data Call or Short Data
     */
    public GPS(PacketSequence packetSequence)
    {
        super(packetSequence);

        //Packet sequence assembler sets the valid flag based on the CRC-32 check
        setValid(packetSequence.isValid());
    }

    /**
     * Source radio that is sending the GPS location
     */
    public NXDNRadioIdentifier getSource()
    {
        return getPacketSequence().getHeader().getSource();
    }

    /**
     * Indicates if the packet sequence is a GPS message
     * @param packetSequence to evaluate
     * @return true if it has the correct message type
     */
    public static boolean isGPS(PacketSequence packetSequence)
    {
        //Guess ... not sure if 0xFFFC is the message type
        return packetSequence.getMessage().getInt(MESSAGE_TYPE) == 0xFFFC;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("GPS");
        sb.append(" RADIO:").append(getSource());
        sb.append(" ").append(getLocationFormatted());
        sb.append(" HDG:").append(getHeading());
        sb.append(" SPEED:").append(getSpeed());
        sb.append(" KPH ELEV:").append(getElevation());
        sb.append(" MTR TIME:").append(DATE_FORMAT.format(new Date(getTimestamp())));
        sb.append(" SATS VIEW:").append(getSatellitesInView());
//        sb.append(" MSG:").append(getMessage().toHexString());
//        sb.append(" HDR:").append(getPacketSequence().getHeader());
        return sb.toString();
    }

    /**
     * Elevation in meters
     * @return elevation in meters
     */
    public int getElevation()
    {
        return getMessage().getInt(ELEVATION_METERS);
    }

    /**
     * Number of satellites in view of the receiver
     * @return satellites count
     */
    public int getSatellitesInView()
    {
        return getMessage().getInt(SATELLITES_IN_VIEW);
    }

    /**
     * Heading degrees
     * @return heading in degrees relative to true North.
     */
    public double getHeading()
    {
        int value = getMessage().getInt(HEADING_TENTHS);

        //0xFFF seems to be 'no value'
        if(value != 0xFFF)
        {
            return value / 10.0;
        }

        return 0;
    }

    /**
     * Speed KPH
     * @return speed in KPH
     */
    public double getSpeed()
    {
        return getMessage().getInt(SPEED_KPH_TENTHS) / 10.0;
    }

    /**
     * Geo position.  The message format has a precision of 1/10,000th of a minute with an
     * accuracy or resolution of 0.5 feet.
     * @return position
     */
    public GeoPosition getLocation()
    {
        return new GeoPosition(getLatitude(), getLongitude());
    }

    /**
     * Location formatted to 6 decimal places
     */
    public String getLocationFormatted()
    {
        return DEGREES_FORMAT.format(getLatitude()) + " " + DEGREES_FORMAT.format(getLongitude());
    }

    /**
     * Latitude
     * @return latitude in degrees decimal
     */
    public double getLatitude()
    {
        return parse(LATITUDE_DEGREES_MINUTES, LATITUDE_MINUTES_FRACTIONAL, LATITUDE_HEMISPHERE_FLAG);
    }

    /**
     * Longitude
     * @return longitude in degrees decimal
     */
    public double getLongitude()
    {
        return parse(LONGITUDE_DEGREES_MINUTES, LONGITUDE_MINUTES_FRACTIONAL, LONGITUDE_HEMISPHERE_FLAG);
    }

    /**
     * Parses the latitude or longitude field using the specified field constants and offset
     * @param degreesMinutes field containing integral degrees and minutes
     * @param fractionalMinutes field containing integral units of 1/10,000 of a minute
     * @param hemisphere flag offset
     * @return parsed value.
     */
    private double parse(IntField degreesMinutes, IntField fractionalMinutes, int hemisphere)
    {
        int degMin = getMessage().getInt(degreesMinutes);
        int degrees = (int)Math.floor(degMin / 100.0);
        double minutes = degMin % 100;
        minutes += (getMessage().getInt(fractionalMinutes) / 10000.0);
        return (degrees + (minutes / 60.0)) * (getMessage().get(hemisphere) ? -1.0 : 1.0);
    }

    public long getTimestamp()
    {
        if(mTimestamp == null)
        {
            Calendar calendar = new GregorianCalendar();
            calendar.clear();
            calendar.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
            calendar.set(Calendar.YEAR, getMessage().getInt(DATE_UTC_YEAR) + 2000); //Starts at year 2000
            calendar.set(Calendar.MONTH, getMessage().getInt(DATE_UTC_MONTH) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, getMessage().getInt(DATE_UTC_DAY) + 1);
            calendar.set(Calendar.HOUR_OF_DAY, getMessage().getInt(TIME_UTC_HOURS));
            calendar.set(Calendar.MINUTE, getMessage().getInt(TIME_UTC_MINUTES));
            calendar.set(Calendar.SECOND, getMessage().getInt(TIME_UTC_SECONDS));
            mTimestamp = calendar.getTimeInMillis();
        }

        return mTimestamp;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return getPacketSequence().getIdentifiers();
    }
}
