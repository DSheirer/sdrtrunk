package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.acknowledge;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRUnitStatus;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Mobile Subscriber Status Acknowledged
 *
 * Indicates the controller accepts the status message and will forward to the status service.
 */
public class AcknowledgeStatus extends Acknowledge
{
    private DMRUnitStatus mUnitStatus;

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
    public AcknowledgeStatus(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * Status value sent by the mobile subscriber
     */
    public DMRUnitStatus getUnitStatus()
    {
        if(mUnitStatus == null)
        {
            mUnitStatus = DMRUnitStatus.create(getMessage().getInt(RESPONSE_INFO));
        }

        return mUnitStatus;
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
        sb.append(" ACKNOWLEDGE STATUS:").append(getUnitStatus());
        sb.append(" TO:").append(getTargetAddress());

        if(hasSourceRadio())
        {
            sb.append(" FM:").append(getSourceRadio());
        }

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());

            if(hasSourceRadio())
            {
                mIdentifiers.add(getSourceRadio());
            }

            mIdentifiers.add(getUnitStatus());
        }

        return mIdentifiers;
    }
}
