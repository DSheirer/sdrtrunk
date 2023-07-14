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

package io.github.dsheirer.module.decode.fleetsync2;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.fleetsync2.message.Fleetsync2Message;
import java.util.function.Function;

/**
 * Filter for Fleetsync messages.
 */
public class FleetsyncMessageFilter extends Filter<IMessage,FleetsyncMessageType>
{
    private final KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public FleetsyncMessageFilter()
    {
        super("Fleetsync Messages");

        for(FleetsyncMessageType type : FleetsyncMessageType.values())
        {
            add(new FilterElement<>(type));
        }
    }

    /**
     * Limits processing to only fleetsync messages.
     * @param message to test
     * @return true if message type is correct.
     */
    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof Fleetsync2Message && super.canProcess(message);
    }

    /**
     * Key extractor for fleetsync messages
     * @return key extractor
     */
    @Override
    public Function<IMessage, FleetsyncMessageType> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor for Fleetsync 2 Messages.
     */
    private class KeyExtractor implements Function<IMessage, FleetsyncMessageType>
    {
        @Override
        public FleetsyncMessageType apply(IMessage message)
        {
            if(message instanceof Fleetsync2Message f2m)
            {
                return f2m.getMessageType();
            }

            return null;
        }
    }
}
