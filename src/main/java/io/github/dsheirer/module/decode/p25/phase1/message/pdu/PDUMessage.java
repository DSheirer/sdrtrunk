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
package io.github.dsheirer.module.decode.p25.phase1.message.pdu;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import io.github.dsheirer.module.decode.p25.reference.ServiceAccessPoint;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.Collections;
import java.util.List;

public class PDUMessage extends P25P1Message
{
    public static final int CONFIRMATION_REQUIRED_INDICATOR = 65;
    public static final int PACKET_DIRECTION_INDICATOR = 66;
    public static final int[] FORMAT = {67, 68, 69, 70, 71};
    public static final int[] SAP_ID = {74, 75, 76, 77, 78, 79};
    public static final int[] VENDOR_ID = {80, 81, 82, 83, 84, 85, 86, 87};
    public static final int[] LOGICAL_LINK_ID = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97,
            98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] BLOCKS_TO_FOLLOW = {113, 114, 115, 116, 117, 118, 119};
    public static final int[] PAD_OCTET_COUNT = {123, 124, 125, 126, 127};
    public static final int[] OPCODE = {122, 123, 124, 125, 126, 127};
    public static final int[] DATA_HEADER_OFFSET = {138, 139, 140, 141, 142, 143};
    public static final int[] PDU_CRC = {144, 145, 146, 147, 148, 149, 150, 151, 152,
            153, 154, 155, 156, 157, 158, 159};

    private Identifier mLLID;

    public PDUMessage(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);

        /* Setup a CRC array to hold the header CRC and the multi-block CRC */
//        mCRC = new CRC[ 2 ];
//        mCRC[ 0 ] = CRC.PASSED;
    }

    @Override
    public P25P1DataUnitID getDUID()
    {
        return P25P1DataUnitID.PACKET_DATA_UNIT;
    }

    protected String getMessageStub()
    {
        StringBuilder sb = new StringBuilder();

        Vendor vendor = getVendor();

        sb.append("NAC:");
        sb.append(getNAC());
        sb.append(" ");

        switch(getFormat())
        {
            case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
                sb.append("ATSBK");
                break;
            case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
                sb.append("**** UNCONFIRMED MULTI-BLOCK TRUNKING CONTROL");
                break;
            default:
                sb.append(getDUID().getLabel());
                break;
        }

        if(vendor == Vendor.STANDARD)
        {
            sb.append(" ");
            sb.append(getOpcode().getLabel());
        }
        else
        {
            sb.append(" ");
            sb.append(vendor.getLabel());
        }

        return sb.toString();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        switch(getFormat())
        {
            case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
                break;
            case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
                sb.append(" PAD OCTETS:").append(getPadOctetCount());
                sb.append(" DATA HDR OFFSET:").append(getDataHeaderOffset());
                break;
            default:
        }

        sb.append(" ");
        sb.append(getConfirmation());
        sb.append(" ");
        sb.append(getDirection());
        sb.append(" FMT:");
        sb.append(getFormat().getLabel());
        sb.append(" SAP:");
        sb.append(getServiceAccessPoint().name());
        sb.append(" VEND:");
        sb.append(getVendor().getLabel());
        sb.append(" LLID:");
        sb.append(getLogicalLinkID());
        sb.append(" BLKS TO FOLLOW:");
        sb.append(getBlocksToFollowCount());

        return sb.toString();
    }

    public String getConfirmation()
    {
        return getMessage().get(CONFIRMATION_REQUIRED_INDICATOR) ? "CONFIRMED" : "UNCONFIRMED";
    }

    public String getDirection()
    {
        return getMessage().get(PACKET_DIRECTION_INDICATOR) ? "OSP" : "ISP";
    }

    public boolean isOutbound()
    {
        return getMessage().get(PACKET_DIRECTION_INDICATOR);
    }

    public PDUFormat getFormat()
    {
        return PDUFormat.fromValue(getMessage().getInt(FORMAT));
    }

    public ServiceAccessPoint getServiceAccessPoint()
    {
        return ServiceAccessPoint.fromValue(getMessage().getInt(SAP_ID));
    }

    public Vendor getVendor()
    {
        return Vendor.fromValue(getMessage().getInt(VENDOR_ID));
    }

    public Identifier getLogicalLinkID()
    {
        if(mLLID == null)
        {
            if(isOutbound())
            {
                mLLID = APCO25RadioIdentifier.createTo(getMessage().getInt(LOGICAL_LINK_ID));
            }
            else
            {
                mLLID = APCO25RadioIdentifier.createFrom(getMessage().getInt(LOGICAL_LINK_ID));
            }
        }

        return mLLID;
    }

    public int getBlocksToFollowCount()
    {
        return getMessage().getInt(BLOCKS_TO_FOLLOW);
    }

    public int getPadOctetCount()
    {
        return getMessage().getInt(PAD_OCTET_COUNT);
    }

    public Opcode getOpcode()
    {
        if(getFormat() == PDUFormat.ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL)
        {
            return Opcode.fromValue(getMessage().getInt(OPCODE), Direction.OUTBOUND, getVendor());
        }

        return Opcode.OSP_UNKNOWN;
    }

    public int getDataHeaderOffset()
    {
        return getMessage().getInt(DATA_HEADER_OFFSET);
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
