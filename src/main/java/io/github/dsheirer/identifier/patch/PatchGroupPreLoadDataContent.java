/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.identifier.patch;

import io.github.dsheirer.controller.channel.event.PreloadDataContent;
import io.github.dsheirer.identifier.IdentifierCollection;

/**
 * Preload patch group data content carrying the current list of identifiers in an identifier collection.
 */
public class PatchGroupPreLoadDataContent extends PreloadDataContent<IdentifierCollection>
{
    private long mTimestamp;

    /**
     * Constructs an instance
     *
     * @param data to preload
     */
    public PatchGroupPreLoadDataContent(IdentifierCollection data, long timestamp)
    {
        super(data);
        mTimestamp = timestamp;
    }

    /**
     * Freshness timestamp for the preload data.
     * @return timestamp milliseconds.
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }
}
