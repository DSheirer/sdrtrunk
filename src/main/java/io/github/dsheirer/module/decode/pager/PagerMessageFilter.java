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

package io.github.dsheirer.module.decode.pager;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import java.util.function.Function;

/**
 * Message filter for paging messages, by message content type, and for sync detection (IDLE) messages.
 */
public class PagerMessageFilter extends Filter<IMessage,PagerMessageType>
{
    private final KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructs an instance
     */
    public PagerMessageFilter()
    {
        super("Pager Messages");

        for(PagerMessageType type: PagerMessageType.values())
        {
            add(new FilterElement<>(type));
        }
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return (message instanceof PagerMessage || message instanceof PagerIdleMessage) && super.canProcess(message);
    }

    @Override
    public Function<IMessage,PagerMessageType> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor for pager messages
     */
    private static class KeyExtractor implements Function<IMessage,PagerMessageType>
    {
        @Override
        public PagerMessageType apply(IMessage message)
        {
            if(message instanceof PagerMessage pagerMessage)
            {
                return pagerMessage.getMessageType();
            }

            if(message instanceof PagerIdleMessage)
            {
                return PagerMessageType.IDLE;
            }

            return null;
        }
    }
}
