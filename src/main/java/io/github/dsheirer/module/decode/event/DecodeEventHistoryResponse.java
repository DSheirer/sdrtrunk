/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.module.ModuleEventBusMessage;

/**
 * Response to decode event history request from a processing chain for the specified channel.
 */
public class DecodeEventHistoryResponse extends ModuleEventBusMessage
{
    private DecodeEventHistory mDecodeEventHistory;

    /**
     * Constructs an instance
     * @param history that has a decode event history
     */
    public DecodeEventHistoryResponse(DecodeEventHistory history)
    {
        mDecodeEventHistory = history;
    }

    /**
     * Decode event history module
     */
    public DecodeEventHistory getDecodeEventHistory()
    {
        return mDecodeEventHistory;
    }
}
