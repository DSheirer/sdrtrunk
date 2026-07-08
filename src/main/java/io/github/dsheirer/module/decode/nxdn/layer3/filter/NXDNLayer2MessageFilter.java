/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.SACCHFragment;
import java.util.function.Function;

/**
 * NXDN SACCH layer 2 message filter
 */
public class NXDNLayer2MessageFilter extends Filter<IMessage, String>
{
	private static final String KEY = "SACCH Message";
	private final Extractor mKeyExtractor = new Extractor();

	/**
	 * Constructor
	 */
    public NXDNLayer2MessageFilter()
    {
        super("Layer 2 Message Filter");
		add(new FilterElement<>(KEY));
    }

	@Override
	public Function<IMessage,String> getKeyExtractor()
	{
		return mKeyExtractor;
	}

	/**
	 * Key extractor for audio messages
	 */
	public static class Extractor implements Function<IMessage,String>
	{
		@Override
		public String apply(IMessage message)
		{
			if(message instanceof SACCHFragment)
			{
				return KEY;
			}

			return null;
		}
	}
}
