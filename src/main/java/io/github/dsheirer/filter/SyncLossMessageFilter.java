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

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import java.util.function.Function;

/**
 * Filter for sync-loss messages
 */
public class SyncLossMessageFilter extends Filter<IMessage,String>
{
    private static final String SYNC_LOSS_KEY = "Sync-Loss";
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public SyncLossMessageFilter()
    {
        super("Sync Loss Messages");
        add(new FilterElement<>(SYNC_LOSS_KEY));
    }

    @Override
    public Function<IMessage, String> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof SyncLossMessage;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,String>
    {
        @Override
        public String apply(IMessage message)
        {
            if(message instanceof SyncLossMessage)
            {
                return SYNC_LOSS_KEY;
            }

            return null;
        }
    }
}
