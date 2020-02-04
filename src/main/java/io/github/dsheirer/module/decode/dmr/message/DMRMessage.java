package io.github.dsheirer.module.decode.dmr.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.dmr.message.data.DataType;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.protocol.Protocol;

public abstract class DMRMessage extends Message
{
    public static final int PAYLOAD_1_START = 24;
    public static final int SYNC_START = 132;
    public static final int PAYLOAD_2_START = 180;

    private DMRSyncPattern mSyncPattern;
    protected CorrectedBinaryMessage mMessage;
    private CACH mCACH;

    /**
     * DMR message frame.  This message is comprised of a 24-bit prefix and a 264-bit message frame.  Outbound base
     * station frames transmit a Common Announcement Channel (CACH) in the 24-bit prefix, whereas Mobile inbound frames
     * do not use the 24-bit prefix.
     *
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public DMRMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message)
    {
        mSyncPattern = syncPattern;
        mMessage = message;
    }

    /**
     * Common Announcement Channel (CACH) message frame.  Note: check hasCACH() before accessing this method.
     * @return CACH frame or null if this message does not contain a CACH.
     */
    public CACH getCACH()
    {
        if(hasCACH() && mCACH == null)
        {
            mCACH = new CACH(mMessage);
        }

        return mCACH;
    }
    @Override
    public Protocol getProtocol() {
        return Protocol.DMR;
    }
    /**
     * Indicates if this frame contains Common Announcement Channel (CACH) frame data.
     */
    public boolean hasCACH()
    {
        return getSyncPattern().hasCACH();
    }

    /**
     * DMR Sync pattern used by this message
     */
    public DMRSyncPattern getSyncPattern()
    {
        return mSyncPattern;
    }

    public static DMRSyncPattern getSyncType(CorrectedBinaryMessage message)
    {
        long syncExpected = message.getLong(SYNC_START, PAYLOAD_2_START - 1);
        return DMRSyncPattern.fromValue(syncExpected);
    }
    /**
     * The original message as captured over the wire with initial error detection and correction applied.
     */
    public CorrectedBinaryMessage getTransmittedMessage()
    {
        return mMessage;
    }
}