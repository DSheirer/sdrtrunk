/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.header.motorola;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.header.ProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.type.ApplicationType;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceAccessPoint;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;

/**
 * Motorola MOTOTRBO Network Interface Service (MNIS) Data Header for SAP:1
 *
 * See: https://cwh050.blogspot.com/2019/08/what-does-mnis-do.html
 */
public class MNISProprietaryDataHeader extends ProprietaryDataHeader
{
    private static final int[] SERVICE_ACCESS_POINT = new int[]{0, 1, 2, 3};
    private static final int[] VENDOR = new int[]{8, 9, 10, 11, 12, 13, 14, 15};
    //NOTE: the following field definitions are speculative ...
    private static final int[] UNKNOWN_02 = new int[]{16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] MESSAGE_TYPE = new int[]{24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] APPLICATION_TYPE = new int[]{32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] PACKET_NUMBER = new int[]{40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int PACKET_PREFIX_START = 56;
    private static final int PACKET_PREFIX_END = PACKET_PREFIX_START + 24;


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
    public MNISProprietaryDataHeader(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CC:").append(getSlotType().getColorCode());
        sb.append(" MOTOROLA MNIS HEADER");
        if(getApplicationType() == ApplicationType.UNKNOWN)
        {
            sb.append(" APPLICATION TYPE:0x").append(Integer.toHexString(getApplicationTypeNumber()).toUpperCase());
        }
        else
        {
            sb.append(" APPLICATION:").append(getApplicationType());
        }
        sb.append(" PACKET:").append(getPacketNumber());
        sb.append(" PACKET PREFIX:").append(getPacketPrefix().toHexString());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Utility method to lookup the vendor from a CSBK message
     *
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

    /**
     * MNIS Application Type lookup utility
     * @param message
     * @return
     */
    public static ApplicationType getApplicationType(CorrectedBinaryMessage message)
    {
        return ApplicationType.fromValue(message.getInt(APPLICATION_TYPE));
    }

    /**
     * MNIS Application Type
     */
    public ApplicationType getApplicationType()
    {
        return getApplicationType(getMessage());
    }

    /**
     * Numeric value of the application type field
     * @param message containing the application type field
     * @return value
     */
    public static int getApplicationTypeValue(CorrectedBinaryMessage message)
    {
        return message.getInt(APPLICATION_TYPE);
    }

    /**
     * Numeric value for the application type field
     */
    public int getApplicationTypeNumber()
    {
        return getMessage().getInt(APPLICATION_TYPE);
    }

    public int getPacketNumber()
    {
        return getMessage().getInt(PACKET_NUMBER);
    }

    /**
     * Access the first 3 octets/bytes of the packet sequence contained in this header
     */
    public CorrectedBinaryMessage getPacketPrefix()
    {
        return getMessage().getSubMessage(PACKET_PREFIX_START, PACKET_PREFIX_END);
    }
}
