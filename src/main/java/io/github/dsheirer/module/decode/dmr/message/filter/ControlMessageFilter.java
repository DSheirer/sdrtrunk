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

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.Opcode;
import java.util.Collection;
import java.util.function.Function;

/**
 * Filter for opcode based control (CSBK) messages.
 */
public class ControlMessageFilter extends Filter<IMessage, Opcode>
{
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructs an instance
     * @param name of this control message filter
     * @param opcodes to use with this filter
     */
    public ControlMessageFilter(String name, Collection<Opcode> opcodes)
    {
        super(name);

        for(Opcode opcode: opcodes)
        {
            add(new FilterElement<>(opcode));
        }
    }

    @Override
    public Function<IMessage, Opcode> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,Opcode>
    {
        @Override
        public Opcode apply(IMessage message)
        {
            if(message instanceof CSBKMessage csbk)
            {
                return csbk.getOpcode();
            }

            return null;
        }
    }
}
