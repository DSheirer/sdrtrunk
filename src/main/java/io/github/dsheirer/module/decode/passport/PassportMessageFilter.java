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

package io.github.dsheirer.module.decode.passport;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageType;
import java.util.function.Function;

/**
 * Filter for Passport messages
 */
public class PassportMessageFilter extends Filter<IMessage,MessageType>
{
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public PassportMessageFilter()
    {
        super("Passport Messages");
        add(new FilterElement<>(MessageType.CA_STRT));
        add(new FilterElement<>(MessageType.SY_IDLE));
        add(new FilterElement<>(MessageType.ID_TGAS));
        add(new FilterElement<>(MessageType.ID_ESNH));
        add(new FilterElement<>(MessageType.CA_PAGE));
        add(new FilterElement<>(MessageType.ID_RDIO));
        add(new FilterElement<>(MessageType.DA_STRT));
        add(new FilterElement<>(MessageType.RA_REGI));
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof PassportMessage && super.canProcess(message);
    }

    @Override
    public Function<IMessage, MessageType> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,MessageType>
    {
        @Override
        public MessageType apply(IMessage message)
        {
            if(message instanceof PassportMessage passport)
            {
                return passport.getMessageType();
            }

            return null;
        }
    }
}
