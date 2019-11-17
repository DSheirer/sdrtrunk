/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.p25.phase1.message.pdu.umbtc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.P25Utils;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUHeader;
import io.github.dsheirer.module.decode.p25.reference.ServiceAccessPoint;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

/**
 * Unconfirmed Multi-Block Trunking Control Header
 */
public class UMBTCHeader extends PDUHeader
{
    public static final int[] SAP_ID = {10, 11, 12, 13, 14, 15};
    public static final int[] PAD_OCTET_COUNT = {59, 60, 61, 62, 63};
    public static final int[] RESERVED = {64, 65, 66, 67, 68, 69, 70, 71};
    public static final int[] DATA_HEADER_OFFSET = {74, 75, 76, 77, 78, 79};

    public UMBTCHeader(CorrectedBinaryMessage message, boolean passesCRC)
    {
        super(message, passesCRC);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("*CRC-FAIL*");
        }

        sb.append("UMBTC");

        Vendor vendor = getVendor();

        P25Utils.pad(sb, 22);

        if(vendor != Vendor.STANDARD)
        {
            sb.append(" VENDOR:").append(getVendor());
        }

        sb.append(" HDR:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Service Access Point (SAP) - determines the network service that will process this packet
     */
    public ServiceAccessPoint getServiceAccessPoint()
    {
        return ServiceAccessPoint.fromValue(getMessage().getInt(SAP_ID));
    }

    /**
     * Number of octets (bytes) that are appended to the end of the packet to make a full final block
     */
    public int getPadOctetCount()
    {
        return getMessage().getInt(PAD_OCTET_COUNT);
    }

    /**
     * Number of bits to where the header
     * @return
     */
    public int getDataHeaderOffset()
    {
        return getMessage().getInt(DATA_HEADER_OFFSET);
    }
}
