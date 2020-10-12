/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.header;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceAccessPoint;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;

/**
 * Proprietary Data Header
 */
public class ProprietaryDataHeader extends DataHeader
{
    private static final int[] SERVICE_ACCESS_POINT = new int[]{0, 1, 2, 3};
    private static final int[] VENDOR = new int[]{8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Constructs an instance.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message containing extracted 196-bit payload.
     * @param cach for the DMR burst
     * @param slotType for this data message
     * @param timestamp message was received
     * @param timeslot for the DMR burst
     */
    public ProprietaryDataHeader(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CC:").append(getSlotType().getColorCode());
        if(!isValid())
        {
            sb.append(" [CRC ERROR]");
        }
        sb.append(" PROPRIETARY DATA HEADER");
        sb.append(" VENDOR:").append(getVendor());
        sb.append(" ").append(getServiceAccessPoint());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Optional packet prefix.
     * @return packet prefix fragment or null.
     */
    public CorrectedBinaryMessage getPacketPrefix()
    {
        return null;
    }

    /**
     * Utility method to lookup the vendor from a CSBK message
     * @param message containing CSBK bits
     * @return vendor
     */
    public static Vendor getVendor(BinaryMessage message)
    {
        return Vendor.fromValue(message.getInt(VENDOR));
    }

    /**
     * Vendor for this message
     */
    public Vendor getVendor()
    {
        return getVendor(getMessage());
    }

    /**
     * Service access point for the specified message
     */
    public static ServiceAccessPoint getServiceAccessPoint(CorrectedBinaryMessage message)
    {
        return ServiceAccessPoint.fromValue(message.getInt(SERVICE_ACCESS_POINT));
    }

    /**
     * Service access point for this message
     */
    public ServiceAccessPoint getServiceAccessPoint()
    {
        return getServiceAccessPoint(getMessage());
    }
}
