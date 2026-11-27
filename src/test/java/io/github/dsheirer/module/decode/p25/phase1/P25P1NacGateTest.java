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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.config.AccessCodeFilter;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class P25P1NacGateTest
{
    private static P25P1Message message(int nac, boolean assisted)
    {
        P25P1Message message = new P25P1Message(nac, 0)
        {
            @Override
            public P25P1DataUnitID getDUID()
            {
                return P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1;
            }

            @Override
            public boolean isValid()
            {
                return true;
            }

            @Override
            public List<Identifier> getIdentifiers()
            {
                return List.of();
            }

            @Override
            public String toString()
            {
                return "TEST NAC:" + nac;
            }
        };
        message.setNACAssisted(assisted);
        return message;
    }

    private static AccessCodeFilter filter(AccessCodeFilter.Mode mode, int nac)
    {
        AccessCodeFilter filter = new AccessCodeFilter(AccessCodeFilter.Format.HEX);
        filter.setEnabled(true);
        filter.setMode(mode);
        filter.setValueSet(Set.of(nac));
        return filter;
    }

    @Test
    public void includeIgnoresAssistedNac()
    {
        P25P1NacGate gate = new P25P1NacGate(filter(AccessCodeFilter.Mode.INCLUDE, 0x293));

        //A foreign transmitter decoded unassisted is rejected
        assertFalse(gate.passes(message(0x5A1, false)));

        //A weak foreign NID recovered with the tracked NAC carries the local NAC but doesn't change the decision
        assertFalse(gate.passes(message(0x293, true)));

        //The local NAC decoded unassisted passes, and later assisted messages follow that decision
        assertTrue(gate.passes(message(0x293, false)));
        assertTrue(gate.passes(message(0x293, true)));
    }

    @Test
    public void excludeIgnoresAssistedNac()
    {
        P25P1NacGate gate = new P25P1NacGate(filter(AccessCodeFilter.Mode.EXCLUDE, 0x5A1));

        assertTrue(gate.passes(message(0x293, false)));

        //An assisted NAC matching the excluded value doesn't drop wanted traffic
        assertTrue(gate.passes(message(0x5A1, true)));

        assertFalse(gate.passes(message(0x5A1, false)));
    }
}
