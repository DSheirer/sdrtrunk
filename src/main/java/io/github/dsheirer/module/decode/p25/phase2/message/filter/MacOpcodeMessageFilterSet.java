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

package io.github.dsheirer.module.decode.p25.phase2.message.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter set for P25 Phase 2 MAC messages
 */
public class MacOpcodeMessageFilterSet extends FilterSet<IMessage>
{
    /**
     * Constructor
     */
    public MacOpcodeMessageFilterSet()
    {
        super("Trunking Messages");

        addFilter(new MacOpcodeMessageFilter("Call Maintenance", MacOpcode.CALL_MAINTENANCE));
        addFilter(new MacOpcodeMessageFilter("Data Channel Grants", MacOpcode.DATA_GRANTS));
        addFilter(new MacOpcodeMessageFilter("Mobile Request & Response", MacOpcode.MOBILE_REQUEST_RESPONSE));
        addFilter(new MacOpcodeMessageFilter("Network Request & Response", MacOpcode.NETWORK_REQUEST_RESPONSE));
        addFilter(new MacOpcodeMessageFilter("Network Status & Announcements", MacOpcode.NETWORK_STATUS));
        addFilter(new MacOpcodeMessageFilter("Voice Channel Grants", MacOpcode.VOICE_GRANTS));

        List<MacOpcode> ungrouped = new ArrayList<>();
        for(MacOpcode opcode: ungrouped)
        {
            if(!opcode.isGrouped())
            {
                ungrouped.add(opcode);
            }
        }
        addFilter(new MacOpcodeMessageFilter("Other/Unknown", ungrouped));
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return (message instanceof MacMessage) && super.canProcess(message);
    }
}
