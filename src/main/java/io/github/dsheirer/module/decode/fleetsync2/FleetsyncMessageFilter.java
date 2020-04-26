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

package io.github.dsheirer.module.decode.fleetsync2;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.fleetsync2.message.Fleetsync2Message;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;

import java.util.*;

public class FleetsyncMessageFilter extends Filter<IMessage>
{
    private Map<FleetsyncMessageType,FilterElement<FleetsyncMessageType>> mElements = new EnumMap<>(FleetsyncMessageType.class);

    public FleetsyncMessageFilter()
    {
        super("Fleetsync Message Filter");

        for(FleetsyncMessageType type : FleetsyncMessageType.values())
        {
            if(type != FleetsyncMessageType.UNKNOWN)
            {
                mElements.put(type, new FilterElement<FleetsyncMessageType>(type));
            }
        }
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && canProcess(message))
        {
            Fleetsync2Message fleet = (Fleetsync2Message)message;

            if(mElements.containsKey(fleet.getMessageType()))
            {
                return mElements.get(fleet.getMessageType()).isEnabled();
            }
        }

        return false;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof Fleetsync2Message;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mElements.values());
    }
}
