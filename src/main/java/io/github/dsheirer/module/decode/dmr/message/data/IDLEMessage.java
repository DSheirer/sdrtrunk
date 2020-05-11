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
package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.protocol.Protocol;

import java.util.List;

public class IDLEMessage extends DataMessage {

    public IDLEMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(syncPattern, message, timestamp, timeslot);
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
