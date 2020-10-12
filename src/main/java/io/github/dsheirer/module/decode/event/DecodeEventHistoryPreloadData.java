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

import io.github.dsheirer.controller.channel.event.PreloadDataContent;

import java.util.List;

/**
 * Decode event history for pre-loading into a processing chain.
 */
public class DecodeEventHistoryPreloadData extends PreloadDataContent<List<IDecodeEvent>>
{
    /**
     * Constructs an instance
     *
     * @param data to preload
     */
    public DecodeEventHistoryPreloadData(List<IDecodeEvent> data)
    {
        super(data);
    }
}
