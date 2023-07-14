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
package io.github.dsheirer.module.decode.lj1200;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import java.util.function.Function;

/**
 * Filter for LJ1200 messages
 */
public class LJ1200MessageFilter extends Filter<IMessage,String>
{
    private static final String LJ1200_KEY = "LJ-1200";
    private final LJ1200KeyExtractor mKeyExtractor = new LJ1200KeyExtractor();

    /**
     * Constructor
     */
    public LJ1200MessageFilter()
    {
        super("LJ-1200 Messages");
        add(new FilterElement<>(LJ1200_KEY));
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return (message instanceof LJ1200Message || message instanceof LJ1200TransponderMessage);
    }

    @Override
    public Function<IMessage, String> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    public class LJ1200KeyExtractor implements Function<IMessage,String>
    {
        @Override
        public String apply(IMessage message)
        {
            return LJ1200_KEY;
        }
    }
}
