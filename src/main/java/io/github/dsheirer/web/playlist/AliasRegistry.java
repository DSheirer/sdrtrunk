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

package io.github.dsheirer.web.playlist;

import io.github.dsheirer.alias.Alias;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Ids for aliases, which have no id of their own.  An alias keeps its id while the application runs, like a channel
 * id.  Ids are assigned on first use and never reused.
 */
public class AliasRegistry
{
    private final Map<Alias,Long> mIds = new IdentityHashMap<>();
    private final Map<Long,Alias> mAliases = new HashMap<>();
    private long mNextId = 1;

    /**
     * Id of the alias, assigned on first use
     */
    public synchronized long getId(Alias alias)
    {
        Long id = mIds.get(alias);

        if(id == null)
        {
            id = mNextId++;
            mIds.put(alias, id);
            mAliases.put(id, alias);
        }

        return id;
    }

    /**
     * Id of the alias if it has one, or null
     */
    public synchronized Long findId(Alias alias)
    {
        return mIds.get(alias);
    }

    /**
     * Alias with the id, or null
     */
    public synchronized Alias getAlias(long id)
    {
        return mAliases.get(id);
    }

    /**
     * Forgets a removed alias.
     */
    public synchronized void remove(Alias alias)
    {
        Long id = mIds.remove(alias);

        if(id != null)
        {
            mAliases.remove(id);
        }
    }
}
