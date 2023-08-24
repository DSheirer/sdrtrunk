package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.acknowledge;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;

/**
 * Registration Accepted
 */
public class RegistrationAccepted extends Acknowledge
{
    /**
     * Constructs an instance
     *
     * @param syncPattern for the CSBK
     * @param message     bits
     * @param cach        for the DMR burst
     * @param slotType    for this message
     * @param timestamp
     * @param timeslot
     */
    public RegistrationAccepted(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * Power Save offset that the mobile subscriber should monitor when in power save mode.
     * @return frame, 1-8
     */
    public int getPowerSaveOffset()
    {
        return getMessage().getInt(RESPONSE_INFO);
    }

    /**
     * Indicates if a power save offset value is specified by the TSCC
     */
    public boolean hasPowerSaveOffset()
    {
        return getPowerSaveOffset() > 0;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("CC:").append(getSlotType().getColorCode());
        sb.append(" REGISTRATION ACCEPTED TO:").append(getTargetAddress());
        sb.append(" FM:").append(getSourceRadio());

        if(hasPowerSaveOffset())
        {
            sb.append(" POWER SAVE OFFSET:").append(getPowerSaveOffset());
        }

        return sb.toString();
    }
}
