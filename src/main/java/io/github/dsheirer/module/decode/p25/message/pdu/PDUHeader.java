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
package io.github.dsheirer.module.decode.p25.message.pdu;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.message.IBitErrorProvider;
import io.github.dsheirer.module.decode.p25.message.tsbk.vendor.VendorOpcode;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.Opcode;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import io.github.dsheirer.module.decode.p25.reference.ServiceAccessPoint;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDUHeader implements IBitErrorProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(PDUHeader.class);

    public static final int CONFIRMATION_REQUIRED_INDICATOR = 1;
    public static final int PACKET_DIRECTION_INDICATOR = 2;
    public static final int[] FORMAT = {3, 4, 5, 6, 7};
    public static final int[] SAP_ID = {10, 11, 12, 13, 14, 15};
    public static final int[] VENDOR_ID = {16, 17, 18, 19, 20, 21, 22, 23};
    public static final int[] LOGICAL_LINK_ID = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    public static final int FULL_MESSAGE_FLAG = 48;
    public static final int[] BLOCKS_TO_FOLLOW = {49, 50, 51, 52, 53, 54, 55};
    public static final int[] PAD_OCTET_COUNT = {59, 60, 61, 62, 63};
    public static final int[] OPCODE = {58, 59, 60, 61, 62, 63};
    public static final int[] DATA_HEADER_OFFSET = {74, 75, 76, 77, 78, 79};
    public static final int[] PDU_CRC = {80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};

    private boolean mValid = true;
    private CorrectedBinaryMessage mMessage;

    public PDUHeader(CorrectedBinaryMessage message)
    {
        mMessage = message;
        CRCP25.correctCCITT80(mMessage, 0, PDU_CRC[0]);
        mValid = mMessage.getCorrectedBitCount() <= 1;
        mLog.debug("PDU Header Bit Error Count: " + mMessage.getCorrectedBitCount());
    }

    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Indicates if this PDU requires confirmation of receipt
     */
    public boolean isConfirmationRequired()
    {
        return mMessage.get(CONFIRMATION_REQUIRED_INDICATOR);
    }

    /**
     * Direction of this message, inbound or outbound.
     */
    public Direction getDirection()
    {
        return Direction.fromValue(mMessage.get(PACKET_DIRECTION_INDICATOR));
    }

    /**
     * Packet Data Unit format
     */
    public PDUFormat getFormat()
    {
        return PDUFormat.fromValue(mMessage.getInt(FORMAT));
    }

    @Override
    public int getBitsProcessedCount()
    {
        return mMessage.size();
    }

    @Override
    public int getBitErrorsCount()
    {
        return mMessage.getCorrectedBitCount();
    }

//    protected String getMessageStub()
//    {
//        StringBuilder sb = new StringBuilder();
//
//        Vendor vendor = getVendor();
//
//        sb.append("NAC:");
//        sb.append(getNAC());
//        sb.append(" ");
//
//        switch(getFormat())
//        {
//            case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
//                sb.append("ATSBK");
//                break;
//            case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
//                sb.append("**** UNCONFIRMED MULTI-BLOCK TRUNKING CONTROL");
//                break;
//            default:
//                sb.append(getDUID().getLabel());
//                break;
//        }
//
//        if(vendor == Vendor.STANDARD)
//        {
//            sb.append(" ");
//            sb.append(getOpcode().getLabel());
//        }
//        else
//        {
//            sb.append(" ");
//            sb.append(vendor.getLabel());
//        }
//
//        return sb.toString();
//    }
//
//    @Override
//    public String getMessage()
//    {
//        StringBuilder sb = new StringBuilder();
//
//        sb.append(getMessageStub());
//
//        switch(getFormat())
//        {
//            case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
//                break;
//            case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
//                sb.append(" PAD OCTETS:" + getPadOctetCount());
//                sb.append(" DATA HDR OFFSET:" + getDataHeaderOffset());
//                break;
//            default:
//        }
//
//        sb.append(" ");
//        sb.append(getConfirmation());
//        sb.append(" ");
//        sb.append(getDirection());
//        sb.append(" FMT:");
//        sb.append(getFormat().getLabel());
//        sb.append(" SAP:");
//        sb.append(getServiceAccessPoint().name());
//        sb.append(" VEND:");
//        sb.append(getVendor().getLabel());
//        sb.append(" LLID:");
//        sb.append(getLogicalLinkID());
//        sb.append(" BLKS TO FOLLOW:");
//        sb.append(getBlocksToFollowCount());
//
//        return sb.toString();
//    }

    public ServiceAccessPoint getServiceAccessPoint()
    {
        return ServiceAccessPoint.fromValue(mMessage.getInt(SAP_ID));
    }

    public Vendor getVendor()
    {
        return Vendor.fromValue(mMessage.getInt(VENDOR_ID));
    }

    public String getLogicalLinkID()
    {
        return mMessage.getHex(LOGICAL_LINK_ID, 6);
    }

    public int getBlocksToFollowCount()
    {
        return mMessage.getInt(BLOCKS_TO_FOLLOW);
    }

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
