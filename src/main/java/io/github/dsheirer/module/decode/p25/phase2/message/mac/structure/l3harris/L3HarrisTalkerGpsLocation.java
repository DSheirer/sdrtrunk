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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.identifier.P25Location;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * L3Harris Talker GPS Location.
 *
 * Bit field definitions are best-guess from observed samples.
 */
public class L3HarrisTalkerGpsLocation extends MacStructureVendor
{
    private static final DecimalFormat GPS_FORMAT = new DecimalFormat("0.000000");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");
    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    private static final int DATA_OFFSET = 24;

    private P25Location mLocation;
    private GeoPosition mGeoPosition;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public L3HarrisTalkerGpsLocation(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(getVendor() == Vendor.HARRIS)
        {
            sb.append("L3H TALKER GPS ");
        }
        else
        {
            sb.append("VENDOR:").append(getVendor()).append(" TALKER GPS ");
        }

        GeoPosition geo = getGeoPosition();
        sb.append(GPS_FORMAT.format(geo.getLatitude())).append(" ").append(GPS_FORMAT.format(geo.getLongitude()));
        sb.append(" HEADING:").append(getHeading());
        sb.append(" TIME:").append(SDF.format(getTimestampMs()));
        sb.append(" UTC MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Heading
     * @return heading, 0-359 degrees.
     */
    public int getHeading()
    {
        return L3HarrisGPS.parseHeading(getMessage(), getOffset() + DATA_OFFSET);
    }

    /**
     * GPS Position time in milliseconds.
     * @return time in ms UTC
     */
    public long getTimestampMs()
    {
        return L3HarrisGPS.parseTimestamp(getMessage(), getOffset() + DATA_OFFSET);
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
            mGeoPosition = new GeoPosition(L3HarrisGPS.parseLatitude(getMessage(), getOffset() + DATA_OFFSET),
                                           L3HarrisGPS.parseLongitude(getMessage(), getOffset() + DATA_OFFSET));
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

    public static void main(String[] args)
    {
        String[] examples = new String[]{
            "8080AAA41106CC031E150E8A53C0E69200061C05E00D9000",
            "8080AAA41106CC031E150E8A53C0E89300061C2A700D9000",
            "8080AAA41106CC031E150E8A53C0E99300061C78400D9000",
            "8080AAA41106D6031E150E8A53C0F29300061CDB200D9000",
            "8080AAA41106D6031E150E8A53C0F39300061C89100D9000",
            "8C80AAA41106CC031E150E8A53C11D9800061CE68088C000",
            "8C80AAA41106CC031E150E8A53C11E9800061C10D088C000",
            "8880AAA41106CC031E150E8A53C0D39100061C6AF0732000",
            "8080AAA4111F7C231D0852B152BBB99600001C75700D9000",
            "8080AAA4111F7C231D0852B152BBBA9700001CC3700D9000",
            "8480AAA4111F86231D0852B152BBE08D00001C8BD0F67000",
            "8480AAA4111F86231D0852B152BBE18D00001CD9E0F67000",
            "8480AAA4111F7C231D0852B152BBEA8C00001C6560F67000",
            "8480AAA4111F7C231D0852B152BBEB8D00001C7700F67000",
            "8480AAA4111F7C231D0852B152BBEE8D00001CE480F67000",
            "8480AAA4111F7C231D0852B152BBF88A00001CDDB0F67000",
            "8480AAA4111F7C231D0852B152BBF98A00001C8F80F67000",
            "8480AAA4111F7C231D0852B152BBF88D00001C9570F67000",
            "8880AAA4111748212525C6B84DBD9C0F080517E600732000", //19 04642010
            "8880AAA4111748212525C6B84DBDA69F0A08172480732000",
            "8880AAA4111748212525C6B84DBDA89F0A08174B40732000",
            "8880AAA4111748212525C6B84DBDAAB4010816B9E0732000",
            "8880AAA4111748212525C6B84DBDABB401081762A0732000",
            "8880AAA4111748212525C6B84DBDAC9C0008170650732000",
            "8C80AAA4111734212525DAB84DBE454200071748A088C000",
            "9080AAA4111734212525DAB84DBE4E42000717B4E0F0F000",
            "9080AAA4111734212525DAB84DBE4F42000717E6D0F0F000",
            "8480AAA4111748212525D0B84DBE868E180217A0A0F67000",
            "8480AAA4111748212525C6B84DBE87641303170E80F67000",
            "8480AAA411173E212525BCB84DBE89641303172190F67000",
            "8480AAA41116A821252468B84DC0C3CF3A0915ECF0F67000",
            "8480AAA41116A82125247CB84DC0C443130916FBA0F67000",
            "8480AAA41116A821252486B84DC0C7272005133230F67000",
            "9080AAA41116BC2325178EB24DBE2855050D15E920F0F000", //04642051
            "8C80AAA41116BC2325178EB24DBE2E71060D142C1088C000",
            "8C80AAA41116BC2325178EB24DBE2F73030D141C1088C000",
            "8480AAA411173E232517D4B24DBF03C1020D14B910F67000",
            "9080AAA411260C21251A90804EBF8A9CAD1A166980F0F000",//04642016  << only sample set that is moving, heading south then north
            "9080AAA411261621251B1C804EBF8B8C921A16FAA0F0F000",
            "8480AAA411263421250F6E814EBFD3982F13168E80F67000",
            "8480AAA411263421250EEC814EBFD4982F1316FC70F67000",
            "8480AAA411263421250E60814EBFD5982F131645A0F67000",
            "8880AAA411087A21251F0E804EC07CB39108143C40732000",
            "8880AAA411087A21251F54804EC07DB39108140EE0732000",
            "8480AAA4110820212520EE804EC0AA4ED00116E970F67000",
            "8480AAA411083E21252094804EC0AB26F702164140F67000",
            "8480AAA411086621252058804EC0AC26F702169920F67000",
            "8480AAA411088E2125201C804EC0AD26F702167A00F67000",
            "8480AAA41108B621251FD6804EC0AEEFE50116DDB0F67000",
            "8C80AAA4110AD21F25134C804EBFCDBD040A1801F088C000", //50  04642025
            "8C80AAA4110AD21F25134C804EBFCEBD090A1832C088C000",
            "8080AAA4110AFA1F25136A804EBFE9A70B0A18A4F00D9000",
            "9080AAA4111BE41E250DD4AE4DC09B5F010314D660F0F000", //53  04642031
            "9080AAA4111BE41E250DD4AE4DC09D5F0903149440F0F000",
            "9080AAA4111BE41E250DD4AE4DC09E5F0903146210F0F000",
            "8080AAA4111BBC1E250DF2AE4DC0A3022F0714FA900D9000",
            "8080AAA4111BBC1E250DE8AE4DC0A4022F071484F00D9000",
            "8080AAA4111BD01E250DE8AE4DC0A5022F07146DB00D9000",
            "8480AAA411265C33200DA098600C644B01061931C0F67", //RADIO:7376955 - Rockwall,TX 2024-04-24
            "8480AAA411265B33200DA198600C65E40105193290F67",
            "8480AAA411265B33200DA298600C667701061965F0F67",
            "8480AAA411265A33200DA298600C670C0107195330F67",
            "8C80AAA411265A33200DA298600C6C43010719087088C",
        };

        for(String example: examples)
        {
            CorrectedBinaryMessage cbm = new CorrectedBinaryMessage(CorrectedBinaryMessage.loadHex(example));
            L3HarrisTalkerGpsLocation gps = new L3HarrisTalkerGpsLocation(cbm, 16);
            System.out.println(gps);
        }
    }
}
