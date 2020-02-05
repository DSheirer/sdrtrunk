package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;

import static io.github.dsheirer.edac.BPTC_196_96.*;

public class DataMessage extends DMRMessage
{
    private SlotType mSlotType;
    protected CorrectedBinaryMessage dataMessage;
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
    protected CorrectedBinaryMessage getMessageBody(CorrectedBinaryMessage _message)
    {
        CorrectedBinaryMessage bm1 = new CorrectedBinaryMessage(196);
        try {
            for(int i = 24; i < 122; i++) {
                bm1.add(_message.get(i));
            }
            for(int i = 190; i < 190 + 98; i++) {
                bm1.add(_message.get(i));
            }
        } catch (BitSetFullException ex) {

        }
        CorrectedBinaryMessage message = bptc_deinterleave(bm1);
        if(bptc_196_96_check_and_repair(message)) {
            message = bptc_196_96_extractdata(message);
        } else {
            return null;
        }
        return message;
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
    public List<Identifier> getIdentifiers() {
        return new ArrayList<Identifier>();
    }
}
