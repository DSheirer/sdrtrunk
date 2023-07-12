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
import java.util.function.Function;

/**
 * Message filter for Digital Coded Squelch (DCS)
 */
public class DCSMessageFilter extends Filter<IMessage,String>
{
    private static final String DCS_KEY = "DCS";
    private final KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public DCSMessageFilter()
    {
        super("DCS Messages");
        add(new FilterElement(DCS_KEY));
    }

    /**
     * Indicates that this filter can process DCS messages.
     * @param message to test
     * @return true if the message can be processed
     */
    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof DCSMessage && super.canProcess(message);
    }

    /**
     * Key extractor that always returns the same key constant.
     * @return extractor function.
     */
    @Override
    public Function<IMessage, String> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,String>
    {
        @Override
        public String apply(IMessage message)
        {
            return DCS_KEY;
        }
    }
}
