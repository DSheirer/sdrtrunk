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
import io.github.dsheirer.module.decode.p25.reference.PacketResponse;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

public class ResponseHeader extends PDUHeader
{
    public static final int[] RESPONSE = {8,9,10,11,12,13,14,15};
    public static final int SOURCE_LLID_FLAG = 48;
    public static final int[] FROM_LOGICAL_LINK_ID = {56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79};

    public ResponseHeader(CorrectedBinaryMessage message, boolean passesCRC)
    {
        super(message, passesCRC);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("PDU   RESPONSE:").append(getResponse());

        if(!isValid())
        {
            sb.append(" *CRC-FAIL*");
        }

        if(hasSourceLLID())
        {
            sb.append(" FROM:").append(getFromLogicalLinkID());
        }

        sb.append(" TO:").append(getToLogicalLinkID());

        Vendor vendor = getVendor();

        if(vendor != Vendor.STANDARD)
        {
            sb.append(" VENDOR:").append(getVendor());
        }

        sb.append(" BLOCKS TO FOLLOW:").append(getBlocksToFollowCount());

        return sb.toString();
    }

    /**
     * Packet response message
     */
    public PacketResponse getResponse()
    {
        return PacketResponse.fromValue(mMessage.getInt(RESPONSE));
    }

    /**
     * Indicates if this header contains a source (from) LLID
     */
    public boolean hasSourceLLID()
    {
        return !mMessage.get(SOURCE_LLID_FLAG);
    }

    /**
     * Source Logical Link Identifier (ie FROM radio identifier)
     */
    public String getFromLogicalLinkID()
    {
        return mMessage.getHex(FROM_LOGICAL_LINK_ID, 6);
    }
}
