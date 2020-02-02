package io.github.dsheirer.module.decode.dmr.message.data.csbk;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.protocol.Protocol;

import java.util.List;

public class CSBKMessage extends DataMessage {

    public CSBKMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message)
    {
        super(syncPattern, message);
        System.out.print("LB: " + (message.get(12) ? 1 : 0) + "\n");
    }
    @Override
    public String toString() {
        return null;
    }

    @Override
    public boolean isValid() {
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
