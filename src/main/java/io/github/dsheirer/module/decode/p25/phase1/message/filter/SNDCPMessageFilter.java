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
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp.SNDCPMessage;
import io.github.dsheirer.module.decode.p25.reference.PDUType;
import java.util.function.Function;

/**
 * Filter for SNDCP messages
 */
public class SNDCPMessageFilter extends Filter<IMessage, PDUType>
{
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public SNDCPMessageFilter()
    {
        super("Sub-Network Dependent Convergence Protocol (SNDCP) Messages");

        for(PDUType pduType: PDUType.values())
        {
            add(new FilterElement<>(pduType));
        }
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof SNDCPMessage && super.canProcess(message);
    }

    @Override
    public Function<IMessage, PDUType> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,PDUType>
    {
        @Override
        public PDUType apply(IMessage message)
        {
            if(message instanceof SNDCPMessage sndcp)
            {
                return sndcp.getPDUType();
            }

            return null;
        }
    }
}
