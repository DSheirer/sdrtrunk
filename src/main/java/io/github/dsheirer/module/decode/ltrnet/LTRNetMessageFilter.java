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

package io.github.dsheirer.module.decode.ltrnet;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.ltrnet.message.LtrNetMessage;
import java.util.function.Function;

/**
 * Filter for LTR-Net messages
 */
public class LTRNetMessageFilter extends Filter<IMessage,LtrNetMessageType>
{
    private final KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public LTRNetMessageFilter()
    {
        super("LTR-Net Messages");

        for(LtrNetMessageType type: LtrNetMessageType.values())
        {
            add(new FilterElement<>(type));
        }
    }

    /**
     * Tests for instance of LTR-Net message
     * @param message to test
     * @return true if correct type.
     */
    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof LtrNetMessage && super.canProcess(message);
    }

    /**
     * Key extractor for LTR-Net messages
     * @return extractor
     */
    @Override
    public Function<IMessage, LtrNetMessageType> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,LtrNetMessageType>
    {
        @Override
        public LtrNetMessageType apply(IMessage message)
        {
            if(message instanceof LtrNetMessage ltr)
            {
                return ltr.getLtrNetMessageType();
            }

            return null;
        }
    }
}
