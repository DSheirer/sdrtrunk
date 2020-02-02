package io.github.dsheirer.module.decode.dmr.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

public class CACH {
    private static final int CACH_MESSAGE_LENGTH = 24;
    private static final int[] INTERLEAVE_MATRIX =
            new int[]{0, 4, 8, 12, 14, 18, 22, 1, 2, 3, 5, 6, 7, 9, 10, 11, 13, 15, 16, 17, 19, 20, 21, 23};
    private static final int INBOUND_CHANNEL_ACCESS_TYPE = 0;
    private static final int OUTBOUND_BURST_TIMESLOT = 1;
    private static final int[] LINK_CONTROL_START_STOP = new int[]{2, 3};
    private static final int[] CACH_CRC = new int[]{4, 5, 6};
    private static final int[] CHECKSUMS = new int[]{5, 7, 6, 3};
    private static final int PAYLOAD_START = 7;
    private static final int PAYLOAD_END = 23;

    public enum AccessType {IDLE, BUSY}

    ;

    private CorrectedBinaryMessage mMessage;
    private BinaryMessage mDecodedMessage;

    /**
     * Common Announcement Channel is a 24-bit sequence that preceeds an outbound DMR frame transmitted by a repeater.
     *
     * @param message containing an initial 24 bits representing a CACH sequence.
     */
    public CACH(CorrectedBinaryMessage message) {
        mMessage = message;
    }

    /**
     * Indicates the state of the next inbound channel timeslot, IDLE or BUSY
     */
    public AccessType getInboundChannelAccessType() {
        if (getDecodedMessage().get(INBOUND_CHANNEL_ACCESS_TYPE)) {
            return AccessType.BUSY;
        } else {
            return AccessType.IDLE;
        }
    }

    /**
     * Link Control or CSBK Start/Stop fragment indicator
     */
    public LCSS getLCSS() {
        return LCSS.fromValue(getDecodedMessage().getInt(LINK_CONTROL_START_STOP));
    }
    /**
     * Indicates the outbound timeslot for the next frame that follows this CACH, 0 or 1
     */
    public int getTimeslot() {
        return getDecodedMessage().get(OUTBOUND_BURST_TIMESLOT) ? 1 : 0;
    }

    /**
     * Binary Payload (17-bit) message fragment
     */
    public BinaryMessage getPayload() {
        return getDecodedMessage().getSubMessage(PAYLOAD_START, PAYLOAD_END);
    }

    /**
     * Deinterleaved and error corrected CACH message
     */
    public BinaryMessage getDecodedMessage() {
        if (mDecodedMessage == null) {
            mDecodedMessage = new BinaryMessage(CACH_MESSAGE_LENGTH);

            //Deinterleave the transmitted message to create the decoded message
            for (int x = 0; x < CACH_MESSAGE_LENGTH; x++) {
                if (mMessage.get(INTERLEAVE_MATRIX[x])) {
                    mDecodedMessage.set(x);
                }
            }

            //Perform error detection
            int checksum = mDecodedMessage.getInt(CACH_CRC);

            for (int x = 0; x < 4; x++) {
                if (mDecodedMessage.get(x)) {
                    checksum ^= CHECKSUMS[x];
                }
            }

            //Perform error correction in primary message
            if (checksum != 0) {
                for (int x = 0; x < 4; x++) {
                    if (checksum == CHECKSUMS[x]) {
                        if (mDecodedMessage.get(x)) {
                            mDecodedMessage.clear(x);
                        } else {
                            mDecodedMessage.set(x);
                        }

                        checksum ^= CHECKSUMS[x];

                        //mMessage.addCorrectedBitCount(1);
                    }
                }
            }

            //Perform error correction on CRC bits for single-bit errors
            if (checksum != 0) {
                if (checksum == 4) {
                    mDecodedMessage.clear(4);
                    //mMessage.addCorrectedBitCount(1);
                } else if (checksum == 2) {
                    mDecodedMessage.clear(5);
                    //mMessage.addCorrectedBitCount(1);
                } else if (checksum == 1) {
                    mDecodedMessage.clear(6);
                    //mMessage.addCorrectedBitCount(1);
                }
            }
        }

        return mDecodedMessage;
    }
}