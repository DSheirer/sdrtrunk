/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;

/**
 * Table model for plottable entity history elements.
 */
public class PlottableEntityModel extends AbstractTableModel implements Listener<PlottableDecodeEvent>
{
    private final static Logger LOGGER = LoggerFactory.getLogger(PlottableEntityModel.class);
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_ALIAS = 1;
    private static final int COLUMN_ALIAS_LIST = 2;
    private static final String[] COLUMN_NAMES = {"ID", "Alias", "List"};
    private static final String KEY_NO_ALIAS_LIST = "(no alias list)";
    private Map<String,PlottableEntityHistory> mEntityHistoryMap = new HashMap();
    private List<PlottableEntityHistory> mEntityHistories = new ArrayList<>();
    private List<IPlottableUpdateListener> mPlottableUpdateListeners = new ArrayList<>();
    private AliasModel mAliasModel;

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
        EventQueue.invokeLater(() -> {
            if(mEntityHistories.size() > 0)
            {
                int firstRow = 0;
                int lastRow = mEntityHistories.size() - 1;
                mEntityHistoryMap.clear();
                mEntityHistories.clear();
                fireTableRowsDeleted(firstRow, lastRow);
            }
        });
    }

    /**
     * Deletes the tracks from both the map and entity histories and fires a table model changed event.
     * @param tracksToDelete to delete
     */
    public void delete(List<PlottableEntityHistory> tracksToDelete)
    {
        EventQueue.invokeLater(() ->
        {
            for(PlottableEntityHistory track : tracksToDelete)
            {
                int index = mEntityHistories.indexOf(track);
                mEntityHistories.remove(track);
                mEntityHistoryMap.entrySet().removeIf(entry -> entry.getValue() == track);
                fireTableRowsDeleted(index, index);
            }
        });
    }

    @Override
    public void receive(PlottableDecodeEvent plottableDecodeEvent)
    {
        if(plottableDecodeEvent.isValidLocation())
        {
            //Add or update the event on the swing event thread
            EventQueue.invokeLater(() -> {
                Identifier from = plottableDecodeEvent.getIdentifierCollection().getFromIdentifier();

                if(from != null && from.getForm() != Form.LOCATION)
                {
                    AliasListConfigurationIdentifier aliasList = plottableDecodeEvent.getIdentifierCollection().getAliasListConfiguration();
                    String key = (aliasList != null ? aliasList.toString() : KEY_NO_ALIAS_LIST) + from;

                    PlottableEntityHistory entityHistory = mEntityHistoryMap.get(key);

                    if(entityHistory == null)
                    {
                        entityHistory = new PlottableEntityHistory(from, plottableDecodeEvent);
                        mEntityHistories.add(entityHistory);
                        mEntityHistoryMap.put(key, entityHistory);
                        int index = mEntityHistories.indexOf(entityHistory);
                        fireTableRowsInserted(index, index);
                    }
                    else
                    {
                        entityHistory.add(plottableDecodeEvent);
                        int index = mEntityHistories.indexOf(entityHistory);
                        fireTableRowsUpdated(index, index);
                    }

                    for(IPlottableUpdateListener listener : mPlottableUpdateListeners)
                    {
                        listener.addPlottableEntity(entityHistory);
                    }
                }
                else
                {
//                    LOGGER.warn("Received plottable decode event that does not contain a FROM identifier - cannot plot");
                }
            });
        }
    }

    @Override
    public String getColumnName(int column)
    {
        if(0 <= column && column < COLUMN_NAMES.length)
        {
            return COLUMN_NAMES[column];
        }

        return "error!";
    }

    @Override
    public int getRowCount()
    {
        return mEntityHistories.size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMN_NAMES.length;
    }

    /**
     * Get the plottable entity history for the specified model index
     * @param index in the model
     * @return entity history or null.
     */
    public PlottableEntityHistory get(int index)
    {
        if(index >= 0 && index < mEntityHistories.size())
        {
            return mEntityHistories.get(index);
        }

        return null;
    }

    /**
     * Get all plottable entity histories.
     */
    public List<PlottableEntityHistory> getAll()
    {
        return new ArrayList<>(mEntityHistories);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        PlottableEntityHistory history = mEntityHistories.get(rowIndex);

        if(history != null)
        {
            switch(columnIndex)
            {
                case COLUMN_ID:
                    Identifier identifier = history.getIdentifier();

                    if(identifier != null)
                    {
                        return identifier.toString();
                    }
                    else
                    {
                        return "(no ID)";
                    }
                case COLUMN_ALIAS:
                    Identifier aliasListConfig = history.getIdentifierCollection().getAliasListConfiguration();
                    if(aliasListConfig != null)
                    {
                        AliasList al = mAliasModel.getAliasList(aliasListConfig.toString());

                        if(al != null)
                        {
                            List<Alias> aliases = al.getAliases(history.getIdentifier());

                            if(!aliases.isEmpty())
                            {
                                return aliases.get(0).getName();
                            }
                        }
                    }
                    break;
                case COLUMN_ALIAS_LIST:
                    Identifier aliasList = history.getIdentifierCollection().getAliasListConfiguration();
                    if(aliasList != null)
                    {
                        return aliasList.toString();
                    }
                    else
                    {
                        return "(no alias list)";
                    }
                default:
                    throw new IllegalArgumentException("Unexpected column index");
            }
        }

        return null;
    }

    public void addListener(IPlottableUpdateListener listener)
    {
        mPlottableUpdateListeners.add(listener);
    }

    public void removeListener(IPlottableUpdateListener listener)
    {
        mPlottableUpdateListeners.remove(listener);
    }
}
