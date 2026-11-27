/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.config.AccessCodeFilter;
import io.github.dsheirer.module.decode.config.AccessCodeGate;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Nac;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;

/**
 * Applies a Network Access Code (NAC) filter to a P25 Phase 1 message stream.  Every P25 Phase 1 message carries the
 * NAC from its BCH error-corrected Network Identifier (NID).
 *
 * The NAC is trusted only when it was decoded from the message's own NID.  A NID that failed hard decoding can be
 * recovered with the help of the tracked (most often seen) NAC, either by a second BCH attempt with the tracked NAC
 * written into the NAC field or by soft decoding against the tracked NAC's codewords.  Either way the result carries
 * the tracked NAC even when a weak co-channel transmitter sent a different one, so those messages (and placeholder
 * messages, whose NAC carries over from the previous NID) follow the most recent trusted decision.
 */
public class P25P1NacGate extends AccessCodeGate
{
    /**
     * Constructs an instance
     * @param filter to apply, or null to pass all messages
     */
    public P25P1NacGate(AccessCodeFilter filter)
    {
        super(filter);
    }

    @Override
    protected Integer getTrustedAccessCode(IMessage message)
    {
        if(message instanceof P25P1Message p25 && !p25.isNACAssisted() && p25.getNAC() instanceof APCO25Nac nac)
        {
            return nac.getValue();
        }

        return null;
    }
}
