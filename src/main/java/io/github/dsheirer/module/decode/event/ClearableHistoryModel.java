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

package io.github.dsheirer.module.decode.event;

import java.awt.EventQueue;
import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * AbstractTableModel implementation supporting clearable method options.
 */
public abstract class ClearableHistoryModel<T> extends AbstractTableModel
{
    public static final int DEFAULT_HISTORY_SIZE = 200;
    private LinkedList<T> mItems = new LinkedList<>();
    private int mHistorySize = DEFAULT_HISTORY_SIZE;

    /**
     * Access an item/row by the model index value.
     * @param index to retrieve
     * @return item or null
     */
    public T getItem(int index)
    {
        if(index < mItems.size())
        {
            return mItems.get(index);
        }

        return null;
    }

    /**
     * Adds the item to the top of the item list and removes any tail items while the item list size exceeds the
     * maximum history size for this model.
     * @param item to add
     */
    public void add(T item)
    {
        if(mItems.contains(item))
        {
            int itemRow = mItems.indexOf(item);
            fireTableRowsUpdated(itemRow, itemRow);
        }
        else
        {
            mItems.addFirst(item);
            fireTableRowsInserted(0, 0);

            while(mItems.size() > mHistorySize)
            {
                mItems.removeLast();
                fireTableRowsDeleted(mItems.size() - 1, mItems.size() - 1);
            }
        }
    }

    /**
     * Clears all messages from history
     */
    public void clear()
    {
        EventQueue.invokeLater(() -> {
            mItems.clear();
            fireTableDataChanged();
        });
    }

    /**
     * Clears the current messages and loads the messages argument
     */
    public void clearAndSet(List<T> items)
    {
        EventQueue.invokeLater(() -> {
            mItems.clear();
            fireTableDataChanged();
            for(T item: items)
            {
                add(item);
            }
        });
    }

    /**
     * Current history size
     * @return history size
     */
    public int getHistorySize()
    {
        return mHistorySize;
    }

    /**
     * Sets the history size
     * @param historySize
     */
    public void setHistorySize(int historySize)
    {
        mHistorySize = historySize;
    }

    @Override
    public int getRowCount()
    {
        return mItems.size();
    }
}
