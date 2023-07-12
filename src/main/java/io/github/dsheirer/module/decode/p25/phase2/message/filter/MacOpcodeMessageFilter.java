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

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import java.util.Collection;
import java.util.function.Function;

/**
 * Message filter for P25 Phase 2 MAC opcode messages.
 */
public class MacOpcodeMessageFilter extends Filter<IMessage, MacOpcode>
{
    private KeyExtractor mKeyExtractor = new KeyExtractor();
    private Collection<MacOpcode> mOpcodes;

    /**
     * Constructs an instance
     *
     * @param name of this filter
     */
    public MacOpcodeMessageFilter(String name, Collection<MacOpcode> opcodes)
    {
        super(name);

        mOpcodes = opcodes;

        for(MacOpcode opcode: opcodes)
        {
            add(new FilterElement<>(opcode));
        }
    }

    /**
     * Key extractor
     * @return opcode extractor
     */
    @Override
    public Function<IMessage, MacOpcode> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor for MacOpcode values from MacMessage.
     */
    private class KeyExtractor implements Function<IMessage,MacOpcode>
    {
        @Override
        public MacOpcode apply(IMessage message)
        {
            if(message instanceof MacMessage mac)
            {
                return mac.getMacStructure().getOpcode();
            }

            return null;
        }
    }
}
