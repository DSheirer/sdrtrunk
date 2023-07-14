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
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.type.DataType;
import java.util.function.Function;

/**
 * Filter for data messages, excluding CSBK messages.
 */
public class DataMessageFilter extends Filter<IMessage, DataType>
{
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructs an instance
     *
     * @param name of this filter
     */
    public DataMessageFilter()
    {
        super("Data Messages");

        for(DataType dataType: DataType.values())
        {
            add(new FilterElement<>(dataType));
        }
    }

    @Override
    public Function<IMessage, DataType> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof DataMessage && !(message instanceof CSBKMessage) && super.canProcess(message);
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,DataType>
    {
        @Override
        public DataType apply(IMessage message)
        {
            if(message instanceof DataMessage dataMessage)
            {
                return dataMessage.getSlotType().getDataType();
            }

            return null;
        }
    }
}
