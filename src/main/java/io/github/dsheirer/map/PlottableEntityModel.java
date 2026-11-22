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
package io.github.dsheirer.map;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Plottable entity histories, one for each FROM identifier and alias list that reported a location.  Thread safe:
 * listeners are called on the thread that made the change, outside of the model's lock.
 */
public class PlottableEntityModel implements Listener<PlottableDecodeEvent>
{
    private static final String KEY_NO_ALIAS_LIST = "(no alias list)";
    private final Map<String,PlottableEntityHistory> mEntityHistoryMap = new HashMap<>();
    private final List<PlottableEntityHistory> mEntityHistories = new ArrayList<>();
    private final List<IPlottableUpdateListener> mPlottableUpdateListeners = new CopyOnWriteArrayList<>();
    private final AliasModel mAliasModel;

    /**
     * Constructs an instance
     * @param aliasModel to lookup aliases
     */
    public PlottableEntityModel(AliasModel aliasModel)
    {
        mAliasModel = aliasModel;
    }

    /**
     * Deletes all tracks and histories.
     */
    public void deleteAllTracks()
    {
        List<PlottableEntityHistory> removed;

        synchronized(this)
        {
            removed = new ArrayList<>(mEntityHistories);
            mEntityHistoryMap.clear();
            mEntityHistories.clear();
        }

        if(!removed.isEmpty())
        {
            mPlottableUpdateListeners.forEach(listener -> listener.entitiesRemoved(removed));
        }
    }

    /**
     * Deletes the tracks from the entity histories.
     * @param tracksToDelete to delete
     */
    public void delete(List<PlottableEntityHistory> tracksToDelete)
    {
        List<PlottableEntityHistory> removed = new ArrayList<>();

        synchronized(this)
        {
            for(PlottableEntityHistory track : tracksToDelete)
            {
                if(mEntityHistories.remove(track))
                {
                    mEntityHistoryMap.values().removeIf(value -> value == track);
                    removed.add(track);
                }
            }
        }

        if(!removed.isEmpty())
        {
            mPlottableUpdateListeners.forEach(listener -> listener.entitiesRemoved(removed));
        }
    }

    @Override
    public void receive(PlottableDecodeEvent plottableDecodeEvent)
    {
        if(!plottableDecodeEvent.isValidLocation())
        {
            return;
        }

        Identifier from = plottableDecodeEvent.getIdentifierCollection().getFromIdentifier();

        if(from == null || from.getForm() == Form.LOCATION)
        {
            return;
        }

        AliasListConfigurationIdentifier aliasList = plottableDecodeEvent.getIdentifierCollection()
            .getAliasListConfiguration();
        String key = (aliasList != null ? aliasList.toString() : KEY_NO_ALIAS_LIST) + from;
        PlottableEntityHistory entityHistory;
        boolean added = false;

        synchronized(this)
        {
            entityHistory = mEntityHistoryMap.get(key);

            if(entityHistory == null)
            {
                entityHistory = new PlottableEntityHistory(from, plottableDecodeEvent);
                mEntityHistories.add(entityHistory);
                mEntityHistoryMap.put(key, entityHistory);
                added = true;
            }
            else
            {
                entityHistory.add(plottableDecodeEvent);
            }
        }

        for(IPlottableUpdateListener listener : mPlottableUpdateListeners)
        {
            if(added)
            {
                listener.entityAdded(entityHistory);
            }
            else
            {
                listener.entityUpdated(entityHistory);
            }
        }
    }

    /**
     * Snapshot of all plottable entity histories, in the order they were added.
     */
    public synchronized List<PlottableEntityHistory> getAll()
    {
        return new ArrayList<>(mEntityHistories);
    }

    /**
     * Aliases for the entity's identifier from the entity's alias list
     * @param history of the entity
     * @return aliases, or an empty list
     */
    public List<Alias> getAliases(PlottableEntityHistory history)
    {
        AliasList aliasList = mAliasModel.getAliasList(history.getIdentifierCollection());

        if(aliasList != null && history.getIdentifier() != null)
        {
            return aliasList.getAliases(history.getIdentifier());
        }

        return Collections.emptyList();
    }

    /**
     * Name of the entity's first alias, or null
     */
    public String getAliasName(PlottableEntityHistory history)
    {
        List<Alias> aliases = getAliases(history);
        return aliases.isEmpty() ? null : aliases.getFirst().getName();
    }

    /**
     * Name of the entity's alias list
     */
    public static String getAliasListName(PlottableEntityHistory history)
    {
        Identifier aliasList = history.getIdentifierCollection().getAliasListConfiguration();
        return aliasList != null ? aliasList.toString() : KEY_NO_ALIAS_LIST;
    }

    /**
     * Adds a listener.  New listeners don't receive the existing entities: see {@link #getAll()}.
     */
    public void addListener(IPlottableUpdateListener listener)
    {
        mPlottableUpdateListeners.add(listener);
    }

    public void removeListener(IPlottableUpdateListener listener)
    {
        mPlottableUpdateListeners.remove(listener);
    }
}
