/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Message filter for NXDN Layer 3 messages
 */
public class NXDNLayer3MessageFilter extends Filter<IMessage, NXDNMessageType>
{
    private final KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructs an instance
     *
     * @param name of this filter
     */
    public NXDNLayer3MessageFilter(String name, Collection<NXDNMessageType> types)
    {
        super(name);

        List<NXDNMessageType> sorted = new ArrayList<>(types);
        sorted.sort(Comparator.comparing(NXDNMessageType::toString));
        for(NXDNMessageType type: sorted)
        {
            add(new FilterElement<>(type));
        }
    }

    /**
     * Key extractor
     * @return opcode extractor
     */
    @Override
    public Function<IMessage, NXDNMessageType> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor for Opcode values from TSBK, AMBTC and UMBTC messages.
     */
    private static class KeyExtractor implements Function<IMessage,NXDNMessageType>
    {
        @Override
        public NXDNMessageType apply(IMessage message)
        {
            if(message instanceof NXDNLayer3Message layer3)
            {
                return layer3.getMessageType();
            }

            return null;
        }
    }
}
