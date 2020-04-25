/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageType;

import java.util.*;

public class MPT1327MessageFilter extends Filter<IMessage>
{
    private Map<MPT1327Message.MPTMessageType, FilterElement<MPT1327Message.MPTMessageType>> mFilterElements = new EnumMap<>(MPT1327Message.MPTMessageType.class);

    public MPT1327MessageFilter()
    {
        super("MPT1327 Message Type Filter");

        for(MPT1327Message.MPTMessageType type : MPT1327Message.MPTMessageType.values())
        {
            if(type != MPT1327Message.MPTMessageType.UNKN)
            {
                mFilterElements.put(type, new FilterElement<MPT1327Message.MPTMessageType>(type));
            }
        }
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && canProcess(message))
        {
            MPT1327Message mpt = (MPT1327Message) message;

            FilterElement<MPT1327Message.MPTMessageType> element =
                    mFilterElements.get(mpt.getMessageType());

            if(element != null)
            {
                return element.isEnabled();
            }
        }

        return false;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof MPT1327Message;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mFilterElements.values());
    }
}

