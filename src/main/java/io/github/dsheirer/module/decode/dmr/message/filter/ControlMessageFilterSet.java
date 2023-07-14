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

package io.github.dsheirer.module.decode.dmr.message.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.Opcode;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter set for DMR control (CSBK) messages using Opcodes for filter elements.
 */
public class ControlMessageFilterSet extends FilterSet<IMessage>
{
    /**
     * Constructor
     */
    public ControlMessageFilterSet()
    {
        super("Control (CSBK) Messages");
        addFilter(new ControlMessageFilter("Data Channel Grants", Opcode.DATA_CHANNEL_GRANTS));
        addFilter(new ControlMessageFilter("Data-Related", Opcode.DATA_OPCODES));
        addFilter(new ControlMessageFilter("Mobile Request/Response", Opcode.MOBILE_REQUEST_RESPONSE));
        addFilter(new ControlMessageFilter("Network Request/Response", Opcode.NETWORK_REQUEST_RESPONSE));
        addFilter(new ControlMessageFilter("Voice Channel Grants", Opcode.VOICE_CHANNEL_GRANTS));
        addFilter(new ControlMessageFilter("Hytera", Opcode.HYTERA));
        addFilter(new ControlMessageFilter("Motorola Capacity Max", Opcode.MOTOROLA_CAPACITY_MAX));
        addFilter(new ControlMessageFilter("Motorola Capacity Plus", Opcode.MOTOROLA_CAPACITY_PLUS));
        addFilter(new ControlMessageFilter("Motorola Connect Plus", Opcode.MOTOROLA_CONNECT_PLUS));

        //Add all remaining opcodes that are not grouped into one of the above enumsets.
        List<Opcode> otherOpcodes = new ArrayList<>();
        for(Opcode opcode: Opcode.values())
        {
            if(!opcode.isGrouped())
            {
                otherOpcodes.add(opcode);
            }
        }

        addFilter(new ControlMessageFilter("Other/Unknown", otherOpcodes));
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof CSBKMessage && super.canProcess(message);
    }
}
