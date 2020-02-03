package io.github.dsheirer.module.decode.dmr.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.data.*;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.TerminatorWithLCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.VoiceLCHeaderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DMRMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(io.github.dsheirer.module.decode.dmr.message.DMRMessageFactory.class);

    /**
     * Creates a P25 message for known message types.
     *
     * NOTE: TSBK and PDU messages are not processed by this factory class.
     *
     * @param dataUnitID that identifies the message type
     * @param timestamp of the message
     * @param message containing message bits and bit error count results from nid error detection and
     * correction
     * @return constructed message parser
     */
    public static DataMessage createDataMessage(DataType dataUnitID, DMRSyncPattern pattern, long timestamp, CorrectedBinaryMessage message)
    {
        switch(dataUnitID)
        {
            case SLOT_IDLE:
                return new IDLEMessage(pattern, message);
            case CSBK:
                return new CSBKMessage(pattern, message);
            case VOICE_HEADER:
                return new VoiceLCHeaderMessage(pattern, message);
            case TLC:
                return new TerminatorWithLCMessage(pattern, message);
            default:
                return new IDLEMessage(pattern, message); //new UnknownP25Message(message, nac, timestamp, dataUnitID);
        }
    }
}

