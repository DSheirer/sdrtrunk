package io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.module.decode.p25.message.tsbk2.ISPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.Collections;
import java.util.List;

/**
 * Unknown/Unrecognized opcode message.
 */
public class UnknownISPMessage extends ISPMessage
{
    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public UnknownISPMessage(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" **UNRECOGNIZED OPCODE**");
        return sb.toString();
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
