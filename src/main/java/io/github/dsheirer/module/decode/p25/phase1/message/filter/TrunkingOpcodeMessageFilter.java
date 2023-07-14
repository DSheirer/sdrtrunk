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

package io.github.dsheirer.module.decode.p25.phase1.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.umbtc.UMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import java.util.Collection;
import java.util.function.Function;

/**
 * Message filter for P25 TSBK, AMBTC and UMBTC trunking control messages.
 */
public class TrunkingOpcodeMessageFilter extends Filter<IMessage, Opcode>
{
    private KeyExtractor mKeyExtractor = new KeyExtractor();
    private Collection<Opcode> mOpcodes;

    /**
     * Constructs an instance
     *
     * @param name of this filter
     */
    public TrunkingOpcodeMessageFilter(String name, Collection<Opcode> opcodes)
    {
        super(name);

        mOpcodes = opcodes;

        for(Opcode opcode: opcodes)
        {
            add(new FilterElement<>(opcode));
        }
    }

    /**
     * Key extractor
     * @return opcode extractor
     */
    @Override
    public Function<IMessage, Opcode> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor for Opcode values from TSBK, AMBTC and UMBTC messages.
     */
    private class KeyExtractor implements Function<IMessage,Opcode>
    {
        @Override
        public Opcode apply(IMessage message)
        {
            if(message instanceof TSBKMessage tsbk)
            {
                return tsbk.getOpcode();
            }
            else if(message instanceof AMBTCMessage ambtc)
            {
                return ambtc.getHeader().getOpcode();
            }
            else if(message instanceof UMBTCMessage umbtc)
            {
                return umbtc.getOpcode();
            }

            return null;
        }
    }
}
