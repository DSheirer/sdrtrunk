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
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCOpcode;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter set for short & voice-fragment link control messages
 *
 * Note: this filter set does NOT work for link control messages in Data Message with Link Control.
 */
public class LinkControlMessageFilterSet extends FilterSet<IMessage>
{
    /**
     * Constructor
     */
    public LinkControlMessageFilterSet()
    {
        super("Short/Voice-Fragment Link Control Messages");

        List<LCOpcode> fullLinkControl = new ArrayList<>();
        List<LCOpcode> shortLinkControl = new ArrayList<>();
        for(LCOpcode opcode: LCOpcode.values())
        {
            if(opcode.isFull())
            {
                fullLinkControl.add(opcode);
            }
            else
            {
                shortLinkControl.add(opcode);
            }
        }

        addFilter(new LinkControlMessageFilter("Full Link Control", fullLinkControl));
        addFilter(new LinkControlMessageFilter("Short Link Control", shortLinkControl));
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof LCMessage && super.canProcess(message);
    }
}
