package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.protocol.Protocol;

import java.util.List;

public class IDLEMessage extends DataMessage {

    public IDLEMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message)
    {
        super(syncPattern, message);
    }
    @Override
    public String toString() {

        return "[IDLE]";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.DMR;
    }

}
