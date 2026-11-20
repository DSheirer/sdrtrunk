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
        if(message instanceof P25P1Message p25 && p25.getNAC() instanceof APCO25Nac nac)
        {
            return nac.getValue();
        }

        return null;
    }
}
