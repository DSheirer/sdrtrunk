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

    // ETSI 100 361-2:7.1.3.2; 361-1: 7.1.1
    private static final int[] SLCO = new int[]{2, 3, 4, 5, 6, 7};
    public VoiceLCHeaderMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage _message, long timestamp, int timeslot)
    {
        super(syncPattern, _message, timestamp, timeslot);
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
