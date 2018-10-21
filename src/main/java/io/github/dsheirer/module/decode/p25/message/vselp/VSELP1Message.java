package io.github.dsheirer.module.decode.p25.message.vselp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public class VSELP1Message extends P25Message
{
    /**
     * Motorola VSELP audio message 1 - not implemented.
     */
    public VSELP1Message(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    @Override
    public DataUnitID getDUID()
    {
        return DataUnitID.VSELP1;
    }
}
