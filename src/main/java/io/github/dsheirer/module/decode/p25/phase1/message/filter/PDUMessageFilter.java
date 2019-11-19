/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PDUMessageFilter extends Filter<IMessage>
{
    private Map<Opcode, FilterElement<Opcode>> mOpcodeFilterElements = new HashMap<Opcode, FilterElement<Opcode>>();

    public PDUMessageFilter()
    {
        super("PDU - Packet Data Unit");

        for(Opcode opcode : Opcode.values())
        {
            mOpcodeFilterElements.put(opcode, new FilterElement<Opcode>(opcode));
        }
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && message instanceof PDUMessage)
        {
            PDUMessage pdu = (PDUMessage) message;

            Opcode opcode = pdu.getOpcode();

            return mOpcodeFilterElements.get(opcode).isEnabled();
        }

        return false;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof PDUMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mOpcodeFilterElements.values());
    }
}
