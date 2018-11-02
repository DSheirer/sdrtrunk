package io.github.dsheirer.module.decode.p25.message.pdu;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.Collections;
import java.util.List;

public class PDUSequenceMessage extends P25Message
{
    private PDUSequence mPDUSequence;

    public PDUSequenceMessage(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(null, nac, timestamp);
        mPDUSequence = PDUSequence;
    }

    public PDUSequence getPDUSequence()
    {
        return mPDUSequence;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getPDUSequence().toString());
        return sb.toString();
    }

    @Override
    public DataUnitID getDUID()
    {
        return DataUnitID.PACKET_DATA_UNIT;
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
