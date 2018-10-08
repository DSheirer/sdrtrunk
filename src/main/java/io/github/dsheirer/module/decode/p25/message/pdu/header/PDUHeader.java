package io.github.dsheirer.module.decode.p25.message.pdu.header;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IBitErrorProvider;
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
    public static final int[] FORMAT = {3, 4, 5, 6, 7};
    public static final int[] VENDOR_ID = {16, 17, 18, 19, 20, 21, 22, 23};
    public static final int[] TO_LOGICAL_LINK_ID = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    public static final int[] BLOCKS_TO_FOLLOW = {49, 50, 51, 52, 53, 54, 55};
    public static final int[] PDU_CRC = {80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};

    protected boolean mValid;
    protected CorrectedBinaryMessage mMessage;

    /**
     * Constructs a PDU header.
     * @param message
     * @param passesCRC
     */
    public PDUHeader(CorrectedBinaryMessage message, boolean passesCRC)
    {
        mMessage = message;
        mValid = passesCRC;
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
        return getFormat(mMessage);
    }

    /**
     * Determines the PDU format for the message
     */
    public static PDUFormat getFormat(BinaryMessage binaryMessage)
    {
        return PDUFormat.fromValue(binaryMessage.getInt(FORMAT));
    }

    /**
     * Number of bits processed to produce this header.
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
        return mMessage.getCorrectedBitCount();
    }

    /**
     * Vendor or manufacturer ID
     */
    public Vendor getVendor()
    {
        return Vendor.fromValue(mMessage.getInt(VENDOR_ID));
    }

    /**
     * Logical Link Identifier (ie TO radio identifier)
     */
    public String getToLogicalLinkID()
    {
        return mMessage.getHex(TO_LOGICAL_LINK_ID, 6);
    }

    /**
     * Number of data blocks that follow this header
     */
    public int getBlocksToFollowCount()
    {
        int blocksToFollow = mMessage.getInt(BLOCKS_TO_FOLLOW);

        if(blocksToFollow <= 3)
        {
            return blocksToFollow;
        }
        else
        {
            mLog.debug("***Excessive blocks to follow count detected: " + blocksToFollow);
            return 0;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append(" *CRC-FAIL*");
        }

        sb.append("PDU HEADER FORMAT:");
        sb.append(getFormat().getLabel());
        sb.append(isConfirmationRequired() ? " CONFIRMED" : " UNCONFIRMED");
        sb.append(" VENDOR:" + getVendor().getLabel());

        return sb.toString();
    }
}
