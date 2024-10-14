/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlOpcode;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDULCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDUMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter set for terminator/link control messages
 */
public class TerminatorMessageFilterSet extends FilterSet<IMessage>
{
    /**
     * Constructor
     */
    public TerminatorMessageFilterSet()
    {
        super("Terminator Messages");
        addFilter(new TDUMessageFilter());
        addFilter(new TerminatorMessageFilter("Command/Status", LinkControlOpcode.COMMAND_STATUS_OPCODES));
        addFilter(new TerminatorMessageFilter("Network/Channel", LinkControlOpcode.NETWORK_OPCODES));
        addFilter(new TerminatorMessageFilter("Voice", LinkControlOpcode.VOICE_OPCODES));
        addFilter(new TerminatorMessageFilter("L3Harris", LinkControlOpcode.L3HARRIS_OPCODES));
        addFilter(new TerminatorMessageFilter("Motorola", LinkControlOpcode.MOTOROLA_OPCODES));

        List<LinkControlOpcode> others = new ArrayList<>();
        for(LinkControlOpcode opcode: LinkControlOpcode.values())
        {
            if(!opcode.isGrouped())
            {
                others.add(opcode);
            }
        }
        addFilter(new TerminatorMessageFilter("Other/Reserved", others));
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return (message instanceof TDUMessage || message instanceof TDULCMessage) && super.canProcess(message);
    }
}
