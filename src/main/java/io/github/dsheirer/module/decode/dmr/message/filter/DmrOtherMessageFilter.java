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
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import java.util.function.Function;

/**
 * Filter for unknown messages, meaning messages not handled by any other filter.
 */
public class DmrOtherMessageFilter extends Filter<IMessage,String>
{
    private static final String OTHER_KEY = "Other/Unknown Message";
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public DmrOtherMessageFilter()
    {
        super("Other DMR messages");
        add(new FilterElement<>(OTHER_KEY));
    }

    @Override
    public Function<IMessage, String> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof DMRMessage && super.canProcess(message);
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,String>
    {
        @Override
        public String apply(IMessage message)
        {
            if(message instanceof DMRMessage)
            {
                return OTHER_KEY;
            }

            return null;
        }
    }
}
