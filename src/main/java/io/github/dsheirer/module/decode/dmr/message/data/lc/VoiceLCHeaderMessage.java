package io.github.dsheirer.module.decode.dmr.message.data.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.edac.ReedSolomon_12_9;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;

import java.util.List;

import static io.github.dsheirer.edac.BPTC_196_96.*;

public class VoiceLCHeaderMessage extends FullLCMessage {
    /*
    Protect Flag (PF) 1
Reserved 1
Full Link Control Opcode
(FLCO)
6
Feature set ID (FID) 8 The FID shall be either SFID or MFID, see clause 9.3.13
Full LC Data 56 (see note 1)
Full LC CRC (see note 2) Either a Reed-Solomon (12,9) FEC for header and terminator
burst, as described in clause B.3.6, or a 5 bit checksum for
embedded signalling, as described in clause B.3.11, shall be used
     */
    public VoiceLCHeaderMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage _message)
    {
        super(syncPattern, _message);
    }
    @Override
    public String toString() {

        return "[VoiceLC] " + super.toString();
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
