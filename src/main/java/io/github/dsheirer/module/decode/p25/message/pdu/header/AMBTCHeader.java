/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.pdu.header;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.vendor.VendorOpcode;
import io.github.dsheirer.module.decode.p25.reference.Opcode;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import io.github.dsheirer.module.decode.p25.reference.ServiceAccessPoint;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

/**
 * Alternage Multi-Block Trunking Control Header
 */
public class AMBTCHeader extends PDUHeader
{
    public static final int[] SAP_ID = {10, 11, 12, 13, 14, 15};
    public static final int FULL_MESSAGE_FLAG = 48;
    public static final int[] PAD_OCTET_COUNT = {59, 60, 61, 62, 63};
    public static final int[] OPCODE = {58, 59, 60, 61, 62, 63};
    public static final int[] DATA_HEADER_OFFSET = {74, 75, 76, 77, 78, 79};

    public AMBTCHeader(CorrectedBinaryMessage message, boolean passesCRC)
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

        sb.append("AMBTC");

        Vendor vendor = getVendor();

        if(vendor == Vendor.STANDARD)
        {
            sb.append(" ").append(getOpcode().getLabel());
        }
        else
        {
            sb.append(" ").append(getVendorOpcode().getLabel());
        }

        sb.append(" TA DAH!");
        return sb.toString();
    }

    /**
     * Service Access Point (SAP) - determines the network service that will process this packet
     */
    public ServiceAccessPoint getServiceAccessPoint()
    {
        return ServiceAccessPoint.fromValue(mMessage.getInt(SAP_ID));
    }

    /**
     * Indicates if this is a full or complete message
     */
    public boolean isFullMessage()
    {
        return mMessage.get(FULL_MESSAGE_FLAG);
    }

    /**
     * Number of octets (bytes) that are appended to the end of the packet to make a full final block
     */
    public int getPadOctetCount()
    {
        return mMessage.getInt(PAD_OCTET_COUNT);
    }

    public Opcode getOpcode()
    {
        if(getFormat() == PDUFormat.ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL)
        {
            return Opcode.fromValue(mMessage.getInt(OPCODE));
        }

        return Opcode.UNKNOWN;
    }

    public VendorOpcode getVendorOpcode()
    {
        if(getFormat() == PDUFormat.ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL)
        {
            return VendorOpcode.fromValue(mMessage.getInt(OPCODE));
        }

        return VendorOpcode.UNKNOWN;
    }

    public int getDataHeaderOffset()
    {
        return mMessage.getInt(DATA_HEADER_OFFSET);
    }
}
