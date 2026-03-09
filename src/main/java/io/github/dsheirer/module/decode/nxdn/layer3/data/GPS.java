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
 * JVC Kenwood NEXEDGE GPS position report.
 */
public class GPS extends NXDNPacketMessage
{
    private static final IntField PACKET_CRC = IntField.length32(0);
    private static final IntField MESSAGE_TYPE = IntField.length16(32); //Constant: 0xFFFC
    private static final IntField UNKNOWN_1 = IntField.length8(48); //Satellites in view?
    private static final IntField UNKNOWN_2 = IntField.length8(56); //This may be 2x 4-bit fields
    private static final IntField UNKNOWN_3 = IntField.length8(64); //Always 0x00 or 0xFF - GPS Valid?
    private static final IntField UNKNOWN_4 = IntField.length32(72); //0x37FF3422 or 0x3400342F ??
    private static final IntField UNKNOWN_5 = IntField.length32(104);
    private static final IntField UNKNOWN_6 = IntField.length16(136);
    private static final IntField UNKNOWN_7 = IntField.length16(152);
    private static final IntField UNKNOWN_8 = IntField.length16(168);
    private static final IntField DATE_UTC_YEAR = IntField.length7(184); //Year: 2000 - 2127
    private static final IntField DATE_UTC_MONTH = IntField.length4(191); //Month: 1-12
    private static final IntField DATE_UTC_DAY = IntField.length5(195); //Day: 0-30 (add 1 to value to get 1-31)

    private static final IntField LONGITUDE = IntField.length32(200);
    private static final IntField LONGITUDE_DEGREE_TENS = IntField.length6(200);
    private static final IntField LONGITUDE_DEGREE_ONES = IntField.length4(206);
    //I haven't yet figured out minutes and fractional minutes
    private static final IntField LONGITUDE_MINUTES = IntField.length6(212);
    private static final int LONGITUDE_ALWAYS_ZERO = 216;
    private static final int LONGITUDE_HEMISPHERE_FLAG = 231;

    private static final IntField LATITUDE = IntField.length32(232);
    private static final IntField LATITUDE_DEGREE_TENS = IntField.length6(232);
    private static final IntField LATITUDE_DEGREE_ONES = IntField.length4(238);
    //I haven't yet figured out minutes and fractional minutes
    private static final IntField LATITUDE_MINUTES = IntField.length6(244);
    private static final int LATITUDE_ALWAYS_ZERO = 248;
    private static final int LATITUDE_HEMISPHERE_FLAG = 263;

    private static final IntField UNKNOWN_9 = IntField.length16(264); //Always 0x0000 ?
    private static final IntField UNKNOWN_10 = IntField.length15(280);
    private static final IntField TIME_UTC_HOURS = IntField.length5(295);
    private static final IntField TIME_UTC_MINUTES = IntField.length6(300);
    private static final IntField TIME_UTC_SECONDS = IntField.length6(306);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss z");
    private static final DecimalFormat DEGREES_FORMAT = new DecimalFormat("0.00000");
    private static final double SCALOR_JUNK_NOT_VALID = 1.0 / 6_555_000D;
    private Long mTimestamp;

    /**
     * Constructs an instance
     *
     * @param packetSequence reassembled from Data Call or Short Data
     */
    public GPS(PacketSequence packetSequence)
    {
        super(packetSequence);
    }

    public String getLatGuess()
    {
        int degrees = (getMessage().getInt(LATITUDE_DEGREE_TENS) * 10) + getMessage().getInt(LATITUDE_DEGREE_ONES);
        degrees *= getMessage().get(LATITUDE_HEMISPHERE_FLAG) ? -1 : 1;
        int minutes = getMessage().getInt(LATITUDE_MINUTES);
        return "LAT D:" + degrees + " M:" + minutes;
    }

    public String getLonGuess()
    {
        int degrees = (getMessage().getInt(LONGITUDE_DEGREE_TENS) * 10) + getMessage().getInt(LONGITUDE_DEGREE_ONES);
        degrees *= getMessage().get(LONGITUDE_HEMISPHERE_FLAG) ? -1 : 1;
        int minutes = getMessage().getInt(LONGITUDE_MINUTES);
        return "LON D:" + degrees + " M:" + minutes;
    }

    /**
     * Indicates if the packet sequence is a GPS message
     * @param packetSequence to evaluate
     * @return true if it has the correct message tgype
     */
    public static boolean isGPS(PacketSequence packetSequence)
    {
        //This is a guess ... not sure if 0xFFFC is the message type
        return packetSequence.getMessage().getInt(MESSAGE_TYPE) == 0xFFFC;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("GPS");
        sb.append(" " + getLatGuess());
        sb.append(" " + getLonGuess());
        sb.append(" HEX LAT:").append(Integer.toHexString(getMessage().getInt(LATITUDE)).toUpperCase());
        sb.append(" HEX LON:").append(Integer.toHexString(getMessage().getInt(LONGITUDE)).toUpperCase());
        sb.append(" POS:").append(DEGREES_FORMAT.format(getLatitude()));
        sb.append(" ").append(DEGREES_FORMAT.format(getLongitude()));
        sb.append(" HDG:").append(getHeading());
        sb.append(" SPEED:").append(getSpeed());
        sb.append(" TIME:").append(DATE_FORMAT.format(new Date(getTimestamp())));
        sb.append(" MSG:").append(getMessage().toHexString());
        sb.append(" HDR:").append(getPacketSequence().getHeader());
        return sb.toString();
    }

    /**
     * Heading
     * @return
     */
    public double getHeading()
    {
        return 0; //TBD
    }

    /**
     * Speed
     * @return
     */
    public double getSpeed()
    {
        return 0; //TBD
    }

    /**
     * Geo position
     * @return position
     */
    public GeoPosition getLocation()
    {
        return new GeoPosition(getLatitude(), getLongitude());
    }

    /**
     * Latitude
     * @return latitude in degrees decimal
     */
    public double getLatitude()
    {
        return getMessage().getInt(LATITUDE) * SCALOR_JUNK_NOT_VALID;
    }

    public long getRawLatitude()
    {
        return getMessage().getInt(LATITUDE);
    }

    public long getRawLongitude()
    {
        return getMessage().getInt(LONGITUDE);
    }

    /**
     * Longitude
     * @return longitude in degrees decimal
     */
    public double getLongitude()
    {
        //fix everything to the western hemisphere until we can figure out the hemisphere indicator bit
        return getMessage().getInt(LONGITUDE) * SCALOR_JUNK_NOT_VALID * -1;
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
        return List.of();
    }

    static void main()
    {
        long lon = 495264727;
        long lat = 281885032;

        System.out.println("Lat: " + lat);
        System.out.println("Lon: " + lon);

        double scaleLat1 = 43.00962d / lat;
        double scaleLon1 = 75.96055d / lon;

        System.out.println("Scale Lat 1: " + scaleLat1);
        System.out.println("Scale Lon 1: " + scaleLon1);

        System.out.println("Scale Lat 1: " + (1 / scaleLat1));
        System.out.println("Scale Lon 1: " + (1 / scaleLon1));


    }
}
