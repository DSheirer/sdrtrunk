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

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlOpcode;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDULCMessage;
import java.util.Collection;
import java.util.function.Function;

/**
 * Filter for TDU terminator and TDULC terminator with link control messages
 */
public class TerminatorMessageFilter extends Filter<IMessage, LinkControlOpcode>
{
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructs an instance
     *
     * @param name of this filter
     */
    public TerminatorMessageFilter(String name, Collection<LinkControlOpcode> opcodes)
    {
        super(name);

        for(LinkControlOpcode opcode: opcodes)
        {
            add(new FilterElement<>(opcode));
        }
    }

    @Override
    public Function<IMessage, LinkControlOpcode> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,LinkControlOpcode>
    {
        @Override
        public LinkControlOpcode apply(IMessage message)
        {
            if(message instanceof TDULCMessage tdulc)
            {
                return tdulc.getLinkControlWord().getOpcode();
            }

            return null;
        }
    }
}
