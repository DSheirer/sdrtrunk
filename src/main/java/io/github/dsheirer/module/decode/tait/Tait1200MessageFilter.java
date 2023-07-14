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
package io.github.dsheirer.module.decode.tait;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.message.IMessage;
import java.util.function.Function;

/**
 * Filter for Tait1200 messages
 */
public class Tait1200MessageFilter extends Filter<IMessage,String>
{
    private static final String TAIT1200_KEY = "Tait-1200";
    private final KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public Tait1200MessageFilter()
    {
        super("Tait-1200 Messages");
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof Tait1200GPSMessage;
    }

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
            return TAIT1200_KEY;
        }
    }
}
