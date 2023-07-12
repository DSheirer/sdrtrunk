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
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCOpcode;
import java.util.Collection;
import java.util.function.Function;

/**
 * Message filter for link control opcodes.
 *
 * Note: this does not include link control messages carried by data messages with embedded link control.
 */
public class LinkControlMessageFilter extends Filter<IMessage, LCOpcode>
{
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructs an instance
     *
     * @param name of this filter
     */
    public LinkControlMessageFilter(String name, Collection<LCOpcode> opcodes)
    {
        super(name);

        for(LCOpcode opcode: opcodes)
        {
            add(new FilterElement<>(opcode));
        }
    }

    @Override
    public Function<IMessage, LCOpcode> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,LCOpcode>
    {
        @Override
        public LCOpcode apply(IMessage message)
        {
            if(message instanceof LCMessage lcMessage)
            {
                return lcMessage.getOpcode();
            }

            return null;
        }
    }
}
