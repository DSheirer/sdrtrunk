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

package io.github.dsheirer.filter;

import java.util.function.Function;

/**
 * Implements an all-pass filter that automatically passes/allows any presented object.
 *
 * A key type of String is used, but that makes no difference in how this filter functions.
 */
public class AllPassFilter<T> extends Filter<T, String>
{
	private static final String KEY = "Other/Unlisted";
	private final AllPassKeyExtractor mKeyExtractor = new AllPassKeyExtractor();

	/**
	 * Constructor
	 * @param name to display for this filter
	 */
    public AllPassFilter(String name)
    {
        super(name);
		add(new FilterElement<>(KEY));
    }

	@Override
	public Function getKeyExtractor()
	{
		return mKeyExtractor;
	}

	/**
	 * Key extractor that always returns the same (String)key.
	 */
	public class AllPassKeyExtractor implements Function<T,String>
	{
		@Override
		public String apply(T t)
		{
			return KEY;
		}
	}
}
