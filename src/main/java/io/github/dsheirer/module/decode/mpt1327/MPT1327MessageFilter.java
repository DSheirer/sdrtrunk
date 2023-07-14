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

package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import java.util.function.Function;

/**
 * Filter for MPT1327 messages
 */
public class MPT1327MessageFilter extends Filter<IMessage, MPT1327Message.MPTMessageType>
{
    private final KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public MPT1327MessageFilter()
    {
        super("MPT1327 Messages");

        for(MPT1327Message.MPTMessageType type : MPT1327Message.MPTMessageType.values())
        {
            add(new FilterElement<>(type));
        }
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof MPT1327Message && super.canProcess(message);
    }

    @Override
    public Function<IMessage, MPT1327Message.MPTMessageType> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage, MPT1327Message.MPTMessageType>
    {
        @Override
        public MPT1327Message.MPTMessageType apply(IMessage message)
        {
            if(message instanceof MPT1327Message mpt)
            {
                return mpt.getMessageType();
            }

            return null;
        }
    }
}

