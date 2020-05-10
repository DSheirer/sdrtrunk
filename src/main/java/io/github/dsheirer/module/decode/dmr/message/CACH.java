package io.github.dsheirer.module.decode.dmr.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.type.LCSS;

/**
 * Common Announcement Channel is a 24-bit sequence that precedes an outbound DMR frame transmitted by a repeater.
 */
public class CACH
{
    private static final int CACH_MESSAGE_LENGTH = 24;
    private static final int[] INTERLEAVE_MATRIX =
        new int[]{0, 4, 8, 12, 14, 18, 22, 1, 2, 3, 5, 6, 7, 9, 10, 11, 13, 15, 16, 17, 19, 20, 21, 23};
    private static final int INBOUND_CHANNEL_ACCESS_TYPE = 0;
    private static final int OUTBOUND_BURST_TIMESLOT = 1;
    private static final int[] LINK_CONTROL_START_STOP = new int[]{2, 3};
    private static final int[] CACH_CRC = new int[]{4, 5, 6};
    private static final int[] CHECKSUMS = new int[]{5, 7, 6, 3};
    private static final int PAYLOAD_START = 7;
    private static final int PAYLOAD_END = 24;

    public enum AccessType {IDLE, BUSY};

    private CorrectedBinaryMessage mMessage;

    /**
     * Constructs an instance.  Note: constructor is private.  Use the getCACH() method to extract a CACH from a
     * raw transmitted DMR burst frame.
     *
     * @param message containing the deinterleaved and error corrected CACH binary message
     */
    private CACH(CorrectedBinaryMessage message)
    {
        mMessage = message;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getLCSS());
        sb.append(" ").append(getPayload().toHexString());
        return sb.toString();
    }

     /**
     * Indicates the state of the next inbound channel timeslot, IDLE or BUSY
     */
    public AccessType getInboundChannelAccessType()
    {
        if(mMessage.get(INBOUND_CHANNEL_ACCESS_TYPE))
        {
            return AccessType.BUSY;
        }
        else
        {
            return AccessType.IDLE;
        }
    }

    /**
     * Link Control or CSBK Start/Stop fragment indicator
     */
    public LCSS getLCSS()
    {
        return LCSS.fromValue(mMessage.getInt(LINK_CONTROL_START_STOP));
    }

    /**
     * Indicates the outbound timeslot for the next frame that follows this CACH, 0 or 1
     */
    public int getTimeslot()
    {
        return mMessage.get(OUTBOUND_BURST_TIMESLOT) ? 1 : 0;
    }

    /**
     * Binary Payload (17-bit) message fragment
     */
    public BinaryMessage getPayload()
    {
        return mMessage.getSubMessage(PAYLOAD_START, PAYLOAD_END);
    }

    /**
     * Utility method to create a CACH from a raw transmitted DMR burst frame.  Performs deinterleaving and error
     * correction.
     *
     * @return constructed CACH message.
     */
    public static CACH getCACH(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage decodedMessage = new CorrectedBinaryMessage(CACH_MESSAGE_LENGTH);

        //Deinterleave the transmitted message to create the decoded message
        for(int x = 0; x < CACH_MESSAGE_LENGTH; x++)
        {
            if(message.get(INTERLEAVE_MATRIX[x]))
            {
                decodedMessage.set(x);
            }
        }

        //Perform error detection
        int checksum = decodedMessage.getInt(CACH_CRC);

        for(int x = 0; x < 4; x++)
        {
            if(decodedMessage.get(x))
            {
                checksum ^= CHECKSUMS[x];
            }
        }

        //Perform error correction in primary message
        if(checksum != 0)
        {
            for(int x = 0; x < 4; x++)
            {
                if(checksum == CHECKSUMS[x])
                {
                    if(decodedMessage.get(x))
                    {
                        decodedMessage.clear(x);
                    }
                    else
                    {
                        decodedMessage.set(x);
                    }

                    checksum ^= CHECKSUMS[x];

                    decodedMessage.incrementCorrectedBitCount(1);
                }
            }
        }

        //Perform error correction on CRC bits for single-bit errors
        if(checksum != 0)
        {
            if(checksum == 4)
            {
                decodedMessage.clear(4);
                decodedMessage.incrementCorrectedBitCount(1);
            }
            else if(checksum == 2)
            {
                decodedMessage.clear(5);
                decodedMessage.incrementCorrectedBitCount(1);
            }
            else if(checksum == 1)
            {
                decodedMessage.clear(6);
                decodedMessage.incrementCorrectedBitCount(1);
            }
        }

        return new CACH(decodedMessage);
    }
}