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

package io.github.dsheirer.module.decode.dcs;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Message filter for Digital Coded Squelch (DCS)
 */
public class DCSMessageFilter extends Filter<IMessage>
{
    private Map<String,FilterElement<String>> mElements = new HashMap<>();
    private static final String DCS_MESSAGE_FILTER_TAG = "DCS";

    public DCSMessageFilter()
    {
        super("DCS Message Filter");
        mElements.put(DCS_MESSAGE_FILTER_TAG, new FilterElement<>("DCS Code", true));
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && canProcess(message))
        {
            return mElements.get(DCS_MESSAGE_FILTER_TAG).isEnabled();
        }

        return false;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof DCSMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mElements.values());
    }
}
