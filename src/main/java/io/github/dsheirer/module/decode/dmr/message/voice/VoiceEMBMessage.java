package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;

/**
 * DMR Voice Frames B - F
 */
public class VoiceEMBMessage extends VoiceMessage
{
    private static final int[] EMB = new int[]{132, 133, 134, 135, 136, 137, 138, 139, 172, 173, 174,
        175, 176, 177, 178, 179};
    private static final int PAYLOAD_START = 140;
    private static final int PAYLOAD_END = 172;

    private EMB mEMB;

    /**
     * DMR message frame.  This message is comprised of a 24-bit prefix and a 264-bit message frame.  Outbound base
     * station frames transmit a Common Announcement Channel (CACH) in the 24-bit prefix, whereas Mobile inbound frames
     * do not use the 24-bit prefix.
     *
     * @param syncPattern
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public VoiceEMBMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(getEMB().isValid())
        {
            sb.append("CC:").append(getEMB().getColorCode());
        }
        else
        {
            sb.append("CC:?");
        }

        sb.append(" ").append(getSyncPattern().toString());

        if(getEMB().isValid())
        {
            if(getEMB().isEncrypted())
            {
                sb.append(" ENCRYPTED");
            }
        }
        else
        {
            sb.append(" [EMB CRC-ERROR]");
        }

        return sb.toString();
    }

    /**
     * EMB field
     */
    public EMB getEMB()
    {
        if(mEMB == null)
        {
            //Transfer the embedded signalling bits into a new binary message
            CorrectedBinaryMessage segment = new CorrectedBinaryMessage(16);

            for(int x = 0; x < EMB.length; x++)
            {
                segment.set(x, getMessage().get(EMB[x]));
            }

            mEMB = new EMB(segment);
        }

        return mEMB;
    }

    /**
     * Embedded signalling full link control fragment
     */
    public BinaryMessage getFLCFragment()
    {
        return getMessage().getSubMessage(PAYLOAD_START, PAYLOAD_END);
    }
}
