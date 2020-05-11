/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2020 Zhenyu Mao
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.dmr.message.data.lc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.ReedSolomon_12_9;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;

import java.util.List;

public class TerminatorWithLCMessage extends FullLCMessage {
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
    public TerminatorWithLCMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage _message, long timestamp, int timeslot)
    {
        super(syncPattern, _message, timestamp, timeslot);
    }
    @Override
    public String toString() {
        return "[TermW/LC] " + super.toString();
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
