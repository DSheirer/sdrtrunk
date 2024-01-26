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

package io.github.dsheirer.gui.javafx.table;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to monitor a JavaFX TableView's column ordering, sizes and visibiltiy and restore those settings on startup.
 */
public class TableViewColumnController implements ListChangeListener
{
    private static final Logger LOG = LoggerFactory.getLogger(TableViewColumnController.class);
    private final Preferences mPreferences = Preferences.userNodeForPackage(TableViewColumnController.class);
    private static final String PREFERENCE_KEY_TABLE_STATE = ".table.state";
    private String mPreferencePrefix;
    private TableView mTableView;
    private TableState mDefaultTableState;
    private ColumnVisibilityMonitor mVisibilityMonitor = new ColumnVisibilityMonitor();
    private ColumnWidthMonitor mWidthMonitor = new ColumnWidthMonitor();

    /**
     * Constructs an instance
     * @param tableView to be monitor and control the column settings
     * @param preferencePrefix to use for storing the table state in preferences, must be non-null/non-empty/unique.
     */
    public TableViewColumnController(TableView tableView, String preferencePrefix)
    {
        if(preferencePrefix == null || preferencePrefix.isEmpty())
        {
            throw new IllegalArgumentException("Preference prefix [" + preferencePrefix + "] must be non-null & non-empty");
        }

        mTableView = tableView;
        mPreferencePrefix = preferencePrefix;
        mDefaultTableState = TableState.create(mTableView);

        TableState storedTableState = getStoredTableState();

        if(storedTableState != null)
        {
            apply(storedTableState);
        }

        //Register listeners on the table and the individual columns
        mTableView.getColumns().addListener(this);
        mTableView.getSortOrder().addListener(this);

        ObservableList<TableColumn> columns = mTableView.getColumns();
        for(TableColumn tableColumn: columns)
        {
            tableColumn.visibleProperty().addListener(mVisibilityMonitor);
            tableColumn.widthProperty().addListener(mWidthMonitor);
        }
    }

    /**
     * Resets the table columns to the default state
     */
    public void reset()
    {
        apply(mDefaultTableState);

        //Clear the preference since we're at default state
        mPreferences.remove(mPreferencePrefix + PREFERENCE_KEY_TABLE_STATE);
    }

    /**
     * Applies the table state to the currently monitored table view.
     * @param tableState to apply.
     */
    private void apply(TableState tableState)
    {
        if(tableState != null)
        {
            if(tableState.getColumns().size() != mTableView.getColumns().size())
            {
                LOG.error("Unable to restore tableview column state - column size mismatch");
                return;
            }

            //Remove the columns from the table and re-add them according to the stored ordering.
            List<TableColumn> columns = new ArrayList<>(mTableView.getColumns());
            mTableView.getColumns().clear();

            for(TableState.ColumnState columnState: tableState.getColumns())
            {
                TableColumn match = null;
                for(TableColumn tableColumn: columns)
                {
                    if(tableColumn.getId() != null && tableColumn.getId().equals(columnState.id()))
                    {
                        match = tableColumn;
                        break;
                    }
                }

                if(match != null)
                {
                    match.setVisible(columnState.v());
                    match.setPrefWidth(columnState.w());
                    mTableView.getColumns().add(match);
                }
                else
                {
                    LOG.warn("Unable to find matching table column for id [" + columnState.id() + "]");
                }
            }

            //If the table names got screwed up in the table state, just add the columns all back in.
            if(mTableView.getColumns().isEmpty())
            {
                mTableView.getColumns().addAll(columns);
            }

            //Clear and restore the sort ordering
            mTableView.getSortOrder().clear();
            for(TableState.SortedColumn sortedColumn: tableState.getSort())
            {
                List<TableColumn> tableColumns = mTableView.getColumns();

                for(TableColumn column: tableColumns)
                {
                    if(sortedColumn.id().equals(column.getId()))
                    {
                        column.setSortType(sortedColumn.sortType());
                        mTableView.getSortOrder().add(column);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Stores current table state
     */
    private void storeTableState()
    {
        TableState tableState = TableState.create(mTableView);
        ObjectMapper objectMapper = new ObjectMapper();

        try
        {
            String json = objectMapper.writeValueAsString(tableState);
            mPreferences.put(mPreferencePrefix + PREFERENCE_KEY_TABLE_STATE, json);
        }
        catch(Exception e)
        {
            LOG.error("Error creating table state json for table [" + mPreferencePrefix + "]", e);
        }
    }

    /**
     * Get the stored table state
     * @return stored table state or null if this is the first usage or the state has been reset to default.
     */
    private TableState getStoredTableState()
    {
        String json = mPreferences.get(mPreferencePrefix + PREFERENCE_KEY_TABLE_STATE, null);

        if(json != null)
        {
            ObjectMapper objectMapper = new ObjectMapper();
            try
            {
                return objectMapper.readValue(json.getBytes(), TableState.class);
            }
            catch(Exception e)
            {
                LOG.error("Error deserializing table state from json [" + json + "] " + e.getLocalizedMessage());
            }
        }

        return null;
    }

    /**
     * Implements the ListChangeListener interface to monitor the table columns for ordering changes.
     * @param c an object representing the change that was done
     */
    @Override
    public void onChanged(Change c)
    {
        boolean changed = false;

        while(c.next())
        {
            changed = true;
        }

        if(changed)
        {
            storeTableState();
        }
    }

    /**
     * Column visibility monitor
     */
    private class ColumnVisibilityMonitor implements ChangeListener<Boolean>
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            storeTableState();
        }
    }

    /**
     * Column width monitor
     */
    private class ColumnWidthMonitor implements ChangeListener<Number>
    {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
        {
            storeTableState();
        }
    }
}
