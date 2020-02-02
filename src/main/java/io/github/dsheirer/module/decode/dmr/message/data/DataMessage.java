package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.protocol.Protocol;

import java.util.List;

public class DataMessage extends DMRMessage
{
    private SlotType mSlotType;

    /**
     * DMR Data Message.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public DataMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message)
    {
        super(syncPattern, message);
    }

    /**
     * Slot Type identifies the color code and data type for this data message
     */
    public SlotType getSlotType()
    {
        if(mSlotType == null)
        {
            mSlotType = new SlotType(getTransmittedMessage());
        }

        return mSlotType;
    }

    @Override
    public String toString()
    {
        return null;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public Protocol getProtocol() {
        return null;
    }

    @Override
    public List<Identifier> getIdentifiers() {
        return null;
    }
}
