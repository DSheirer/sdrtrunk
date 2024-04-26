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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IBitErrorProvider;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P25 Packet Data Unit header
 */
public class PDUHeader implements IBitErrorProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(PDUHeader.class);

    public static final int CONFIRMATION_REQUIRED_INDICATOR = 1;
    public static final int PACKET_DIRECTION_INDICATOR = 2;
    public static final int[] PDU_FORMAT = {3, 4, 5, 6, 7};
    public static final int[] VENDOR_ID = {16, 17, 18, 19, 20, 21, 22, 23};
    public static final int[] LOGICAL_LINK_ID = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    public static final int[] BLOCKS_TO_FOLLOW = {49, 50, 51, 52, 53, 54, 55};
    public static final int[] PDU_CRC = {80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};

    protected boolean mValid;
    private CorrectedBinaryMessage mMessage;
    private Identifier mLLID;

    /**
     * Constructs a PDU header.
     *
     * @param message
     * @param passesCRC
     */
    public PDUHeader(CorrectedBinaryMessage message, boolean passesCRC)
    {
        mMessage = message;
        mValid = passesCRC;
    }

    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Indicates if this header was correctly decoded and passed CCITT-16 CRC error check.
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Indicates if this PDU requires confirmation of receipt
     */
    public boolean isConfirmationRequired()
    {
        return getMessage().get(CONFIRMATION_REQUIRED_INDICATOR);
    }

    /**
     * Direction of this message, inbound or outbound.
     */
    public Direction getDirection()
    {
        return Direction.fromValue(getMessage().get(PACKET_DIRECTION_INDICATOR));
    }

    /**
     * Indicates if this is an outbound (true) FNE -> SU, or an inbound (false) SU -> FNE packet.
     */
    public boolean isOutbound()
    {
        return getDirection() == Direction.OUTBOUND;
    }

    /**
     * Packet Data Unit format
     */
    public PDUFormat getFormat()
    {
        return getFormat(getMessage());
    }

    /**
     * Determines the PDU format for the message
     */
    public static PDUFormat getFormat(BinaryMessage binaryMessage)
    {
        return PDUFormat.fromValue(binaryMessage.getInt(PDU_FORMAT));
    }

    /**
     * Number of bits processed to produce this header.
     *
     * @return 196 bits
     */
    @Override
    public int getBitsProcessedCount()
    {
        return 196;
    }

    /**
     * Number of bit errors detected and/or corrected
     */
    @Override
    public int getBitErrorsCount()
    {
        return getMessage().getCorrectedBitCount();
    }

    /**
     * Vendor or manufacturer ID
     */
    public Vendor getVendor()
    {
        return Vendor.fromValue(getMessage().getInt(VENDOR_ID));
    }

    /**
     * Logical Link Identifier (ie TO radio identifier)
     */
    public Identifier getTargetLLID()
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

    /**
     * Number of data blocks that follow this header
     */
    public int getBlocksToFollowCount()
    {
        return getMessage().getInt(BLOCKS_TO_FOLLOW);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("***CRC-FAIL*** ");
        }

        sb.append("PDU HEADER FORMAT:");
        sb.append(getFormat().getLabel());
        sb.append(isConfirmationRequired() ? " CONFIRMED" : " UNCONFIRMED");
        sb.append(" VENDOR:").append(getVendor().getLabel());
        sb.append(isOutbound() ? "TO" : "FROM").append(" LLID").append(getTargetLLID());

        return sb.toString();
    }
}
