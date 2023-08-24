/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.Collections;
import java.util.List;

/**
 * DMR Voice Frame A with embedded sync pattern.
 */
public class VoiceAMessage extends VoiceMessage
{
    /**
     * Constructs an instance.
     *
     * @param syncPattern for the Voice A frame
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public VoiceAMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, long timestamp,
                         int timeslot)
    {
        super(syncPattern, message, cach, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!getSyncPattern().isMobileSyncPattern())
        {
            sb.append("CC:- ");
        }

        sb.append(getSyncPattern());

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
