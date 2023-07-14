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
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDUMessage;
import java.util.function.Function;

/**
 * Filter for PDU packet data unit messages
 */
public class PDUMessageFilter extends Filter<IMessage,String>
{
    private static final String PDU_KEY = "Packet Data Unit (PDU)";
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public PDUMessageFilter()
    {
        super("Packet Data Unit messages");
        add(new FilterElement<>(PDU_KEY));
    }

    @Override
    public Function<IMessage, String> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof TDUMessage && super.canProcess(message);
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,String>
    {
        @Override
        public String apply(IMessage message)
        {
            if(message instanceof PDUMessage)
            {
                return PDU_KEY;
            }

            return null;
        }
    }
}
