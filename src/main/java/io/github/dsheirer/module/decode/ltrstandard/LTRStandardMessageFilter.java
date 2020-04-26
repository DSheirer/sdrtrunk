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

package io.github.dsheirer.module.decode.ltrstandard;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.ltrstandard.message.LTRMessage;

import java.util.*;

public class LTRStandardMessageFilter extends Filter<IMessage>
{
    private Map<LtrStandardMessageType,FilterElement<LtrStandardMessageType>> mElements = new EnumMap<>(LtrStandardMessageType.class);

    public LTRStandardMessageFilter()
    {
        super("LTR Message Filter");

        mElements.put(LtrStandardMessageType.CALL, new FilterElement<>(LtrStandardMessageType.CALL));
        mElements.put(LtrStandardMessageType.CALL_END, new FilterElement<>(LtrStandardMessageType.CALL_END));
        mElements.put(LtrStandardMessageType.IDLE, new FilterElement<>(LtrStandardMessageType.IDLE));
        mElements.put(LtrStandardMessageType.UNKNOWN, new FilterElement<>(LtrStandardMessageType.UNKNOWN));
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && canProcess(message))
        {
            LTRMessage ltr = (LTRMessage)message;

            if(mElements.containsKey(ltr.getMessageType()))
            {
                return mElements.get(ltr.getMessageType()).isEnabled();
            }
        }

        return false;
    }

    public boolean canProcess(IMessage message)
    {
        return message instanceof LTRMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mElements.values());
    }
}
