/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.preference.swing;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.EventQueue;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitors a JTable column model and persists column width, order, and sort changes to the user preferences.
 * Restores previous column layout on application restart.
 */
public class JTableColumnWidthMonitor
{
    private static final Logger mLog = LoggerFactory.getLogger(JTableColumnWidthMonitor.class);
    private UserPreferences mUserPreferences;
    private JTable mTable;
    private String mKey;
    private ColumnModelListener mColumnModelListener = new ColumnModelListener();
    private AtomicBoolean mSaveInProgress = new AtomicBoolean();

    /**
     * Constructs a column width monitor.
     *
     * @param userPreferences to store column widths
     * @param table to monitor for column width changes
     * @param key that uniquely identifies the table to monitor
     */
    public JTableColumnWidthMonitor(UserPreferences userPreferences, JTable table, String key)
    {
        mUserPreferences = userPreferences;
        mTable = table;
        mKey = key;

        mTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        // Wait until the UI is realized to restore column layout
        EventQueue.invokeLater(() -> {
            restoreColumnOrder();
            restoreColumnWidths();
            restoreSortOrder();
        });

        // Listen for column resize, reorder, and sort changes
        mTable.getColumnModel().addColumnModelListener(mColumnModelListener);

        // Listen for sort order changes if a row sorter is present
        if(mTable.getRowSorter() != null)
        {
            mTable.getRowSorter().addRowSorterListener(e -> {
                if(mSaveInProgress.compareAndSet(false, true))
                {
                    ThreadPool.SCHEDULED.schedule(new ColumnLayoutSaveTask(), 2, TimeUnit.SECONDS);
                }
            });
        }
    }

    /**
     * Prepares this monitor for disposal by unregistering as a listener to the table column model.
     */
    public void dispose()
    {
        if(mTable != null && mColumnModelListener != null)
        {
            mTable.getColumnModel().removeColumnModelListener(mColumnModelListener);
        }

        mTable = null;
        mUserPreferences = null;
    }

    /**
     * Sets the preferred column widths on the table from persisted settings
     */
    private void restoreColumnWidths()
    {
        TableColumnModel model = mTable.getColumnModel();
        boolean hasWidths = false;

        for(int x = 0; x < model.getColumnCount(); x++)
        {
            int width = mUserPreferences.getSwingPreference().getInt(getColumnKey(x), Integer.MAX_VALUE);

            if(width != Integer.MAX_VALUE)
            {
                model.getColumn(x).setPreferredWidth(width);
                model.getColumn(x).setWidth(width);
                hasWidths = true;
            }
        }

        // Temporarily disable auto-resize to let saved widths take effect, then re-enable
        if(hasWidths)
        {
            mTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            EventQueue.invokeLater(() -> mTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS));
        }
    }

    /**
     * Stores the current column widths to the user preferences
     */
    private void storeColumnWidths()
    {
        TableColumnModel model = mTable.getColumnModel();

        for(int x = 0; x < model.getColumnCount(); x++)
        {
            mUserPreferences.getSwingPreference().setInt(getColumnKey(x), model.getColumn(x).getWidth());
        }
    }

    /**
     * Stores the current column order to user preferences.
     * Saves the model index of each view column position.
     */
    private void storeColumnOrder()
    {
        TableColumnModel model = mTable.getColumnModel();

        for(int viewIndex = 0; viewIndex < model.getColumnCount(); viewIndex++)
        {
            mUserPreferences.getSwingPreference().setInt(
                mKey + ".column.order." + viewIndex, model.getColumn(viewIndex).getModelIndex());
        }

        mUserPreferences.getSwingPreference().setInt(mKey + ".column.order.count", model.getColumnCount());
    }

    /**
     * Restores column order from user preferences.
     */
    private void restoreColumnOrder()
    {
        TableColumnModel model = mTable.getColumnModel();
        int savedCount = mUserPreferences.getSwingPreference().getInt(mKey + ".column.order.count", 0);

        if(savedCount != model.getColumnCount())
        {
            return; // Column count changed or no saved order
        }

        try
        {
            int[] modelIndices = new int[savedCount];
            for(int i = 0; i < savedCount; i++)
            {
                modelIndices[i] = mUserPreferences.getSwingPreference().getInt(mKey + ".column.order." + i, i);
            }

            // Move columns to match saved order
            for(int targetView = 0; targetView < modelIndices.length; targetView++)
            {
                int desiredModel = modelIndices[targetView];

                for(int currentView = targetView; currentView < model.getColumnCount(); currentView++)
                {
                    if(model.getColumn(currentView).getModelIndex() == desiredModel)
                    {
                        if(currentView != targetView)
                        {
                            model.moveColumn(currentView, targetView);
                        }
                        break;
                    }
                }
            }
        }
        catch(Exception e)
        {
            mLog.error("Error restoring column order", e);
        }
    }

    /**
     * Stores the current sort order to user preferences.
     * Saves sort column index and sort direction (0=unsorted, 1=ascending, 2=descending).
     */
    private void storeSortOrder()
    {
        if(mTable.getRowSorter() == null)
        {
            return;
        }

        List<? extends RowSorter.SortKey> sortKeys = mTable.getRowSorter().getSortKeys();

        mUserPreferences.getSwingPreference().setInt(mKey + ".sort.count", sortKeys.size());

        for(int i = 0; i < sortKeys.size(); i++)
        {
            mUserPreferences.getSwingPreference().setInt(mKey + ".sort." + i + ".column", sortKeys.get(i).getColumn());
            mUserPreferences.getSwingPreference().setInt(mKey + ".sort." + i + ".direction", sortKeys.get(i).getSortOrder().ordinal());
        }
    }

    /**
     * Restores sort order from user preferences.
     */
    private void restoreSortOrder()
    {
        if(mTable.getRowSorter() == null)
        {
            return;
        }

        int sortCount = mUserPreferences.getSwingPreference().getInt(mKey + ".sort.count", 0);

        if(sortCount <= 0)
        {
            return;
        }

        try
        {
            List<RowSorter.SortKey> sortKeys = new ArrayList<>();

            for(int i = 0; i < sortCount; i++)
            {
                int column = mUserPreferences.getSwingPreference().getInt(mKey + ".sort." + i + ".column", -1);
                int direction = mUserPreferences.getSwingPreference().getInt(mKey + ".sort." + i + ".direction", 0);

                if(column >= 0 && column < mTable.getColumnCount())
                {
                    SortOrder sortOrder = SortOrder.values()[Math.min(direction, SortOrder.values().length - 1)];
                    sortKeys.add(new RowSorter.SortKey(column, sortOrder));
                }
            }

            if(!sortKeys.isEmpty())
            {
                mTable.getRowSorter().setSortKeys(sortKeys);
            }
        }
        catch(Exception e)
        {
            mLog.error("Error restoring sort order", e);
        }
    }

    /**
     * Constructs a preference key for the column number
     */
    private String getColumnKey(int column)
    {
        return mKey + ".column." + column;
    }

    /**
     * Table column model listener that saves width, order, and sort changes.
     */
    class ColumnModelListener implements TableColumnModelListener
    {
        @Override
        public void columnMarginChanged(ChangeEvent e)
        {
            scheduleSave();
        }

        @Override
        public void columnMoved(TableColumnModelEvent e)
        {
            if(e.getFromIndex() != e.getToIndex())
            {
                scheduleSave();
            }
        }

        @Override
        public void columnAdded(TableColumnModelEvent e){}
        @Override
        public void columnRemoved(TableColumnModelEvent e){}
        @Override
        public void columnSelectionChanged(ListSelectionEvent e){}

        private void scheduleSave()
        {
            if(mSaveInProgress.compareAndSet(false, true))
            {
                ThreadPool.SCHEDULED.schedule(new ColumnLayoutSaveTask(), 2, TimeUnit.SECONDS);
            }
        }
    }

    public class ColumnLayoutSaveTask implements Runnable
    {
        @Override
        public void run()
        {
            storeColumnWidths();
            storeColumnOrder();
            storeSortOrder();
            mSaveInProgress.set(false);
        }
    }
}
