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

package io.github.dsheirer.module.decode.ltrstandard;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.ltrstandard.message.LTRMessage;
import java.util.function.Function;

/**
 * Filter for LTR standard messages.
 */
public class LTRStandardMessageFilter extends Filter<IMessage,LtrStandardMessageType>
{
    private final KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public LTRStandardMessageFilter()
    {
        super("LTR Messages");

        for(LtrStandardMessageType type: LtrStandardMessageType.values())
        {
            add(new FilterElement<>(type));
        }
    }

    public boolean canProcess(IMessage message)
    {
        return message instanceof LTRMessage && super.canProcess(message);
    }

    @Override
    public Function<IMessage, LtrStandardMessageType> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,LtrStandardMessageType>
    {
        @Override
        public LtrStandardMessageType apply(IMessage message)
        {
            if(message instanceof LTRMessage ltr)
            {
                return ltr.getMessageType();
            }

            return null;
        }
    }
}
