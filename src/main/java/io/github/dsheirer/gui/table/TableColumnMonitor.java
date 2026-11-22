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
package io.github.dsheirer.gui.table;

import io.github.dsheirer.preference.swing.SwingPreference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Stores and restores the order, visibility and width of the columns of a JavaFX table across application sessions,
 * and restores the default column order and visibility on request.
 *
 * Each column must have a unique, stable ID (TableColumn.setId()), which is the key for its stored settings.  The
 * order and visibility of the columns when this monitor is constructed are the defaults.  A column that is added to
 * the table in a later version of the application is shown at its default position.
 *
 * Use this class only on the JavaFX application thread.
 */
public class TableColumnMonitor<S>
{
    /**
     * Column property that holds the name shown for the column in the column configuration editor, when the
     * column header text doesn't identify the column by itself (e.g. two columns titled 'Alias').
     */
    public static final String CONFIGURATION_NAME = "column.configuration.name";
    /**
     * Column property that holds the column number of the column in the Swing version of the table, for the stored
     * Swing column widths.
     */
    public static final String LEGACY_INDEX = "column.legacy.index";
    private static final String ORDER = ".column.order";
    private static final String HIDDEN = ".column.hidden";
    private static final String KNOWN = ".column.known";
    private static final String WIDTH = ".column.width.";
    private static final String SEPARATOR = ",";

    private final TableView<S> mTableView;
    private final String mKey;
    private final Preferences mPreferences;
    private final List<TableColumn<S,?>> mDefaultOrder;
    private final Map<TableColumn<S,?>,Boolean> mDefaultVisibility = new HashMap<>();
    private boolean mRestoring;

    /**
     * Constructs an instance and applies the stored column settings to the table.
     * @param tableView to monitor
     * @param key that uniquely identifies the table, used as the prefix for its stored settings
     */
    public TableColumnMonitor(TableView<S> tableView, String key)
    {
        this(tableView, key, Preferences.userNodeForPackage(TableColumnMonitor.class), null, null);
    }

    /**
     * Constructs an instance and applies the stored column settings to the table.  Columns that don't have a stored
     * width yet use the width stored by the Swing version of the table, so that users keep their column widths.
     * @param tableView to monitor
     * @param key that uniquely identifies the table, used as the prefix for its stored settings
     * @param swingPreference that holds the column widths of the Swing table
     * @param swingKey of the Swing table.  The Swing column number is the default index of the column.
     */
    public TableColumnMonitor(TableView<S> tableView, String key, SwingPreference swingPreference, String swingKey)
    {
        this(tableView, key, Preferences.userNodeForPackage(TableColumnMonitor.class), swingPreference, swingKey);
    }

    /**
     * Constructs an instance and applies the stored column settings to the table.
     * @param tableView to monitor
     * @param key that uniquely identifies the table, used as the prefix for its stored settings
     * @param preferences node for storing the settings
     * @param swingPreference that holds legacy Swing column widths, or null
     * @param swingKey for the legacy Swing column widths, or null
     */
    TableColumnMonitor(TableView<S> tableView, String key, Preferences preferences, SwingPreference swingPreference,
                       String swingKey)
    {
        mTableView = tableView;
        mKey = key;
        mPreferences = preferences;
        mDefaultOrder = List.copyOf(tableView.getColumns());

        Set<String> ids = new HashSet<>();

        for(TableColumn<S,?> column: mDefaultOrder)
        {
            if(column.getId() == null || column.getId().isEmpty() || column.getId().contains(SEPARATOR) ||
                !ids.add(column.getId()))
            {
                throw new IllegalArgumentException("Each table column needs a unique ID without commas - column [" +
                    column.getText() + "] has ID [" + column.getId() + "]");
            }

            mDefaultVisibility.put(column, column.isVisible());
        }

        restore(swingPreference, swingKey);

        mTableView.getColumns().addListener((ListChangeListener<TableColumn<S,?>>)c -> {
            if(!mRestoring)
            {
                storeOrder();
            }
        });

        for(TableColumn<S,?> column: mDefaultOrder)
        {
            column.visibleProperty().addListener((observable, wasVisible, visible) -> {
                if(!mRestoring)
                {
                    //Don't let the user hide every column, since that leaves no column header for getting them back
                    if(!visible && getVisibleColumnCount() == 0)
                    {
                        column.setVisible(true);
                        return;
                    }

                    storeVisibility();
                }
            });

            column.widthProperty().addListener((observable, oldWidth, width) -> {
                if(!mRestoring && column.isVisible() && width.doubleValue() > 0)
                {
                    mPreferences.putDouble(getWidthKey(column), width.doubleValue());
                }
            });
        }
    }

    /**
     * Table that is monitored.
     */
    public TableView<S> getTableView()
    {
        return mTableView;
    }

    /**
     * Columns in their default order.
     */
    public List<TableColumn<S,?>> getDefaultOrder()
    {
        return mDefaultOrder;
    }

    /**
     * Name of the column for display in the column configuration editor.
     * @param column to name
     * @return configuration name for the column, or the column header text.
     */
    public static String getName(TableColumn<?,?> column)
    {
        Object name = column.getProperties().get(CONFIGURATION_NAME);
        return name != null ? name.toString() : column.getText();
    }

    /**
     * Sets the name of the column for display in the column configuration editor
     * @param column to name
     * @param name to show in the configuration editor
     */
    public static void setName(TableColumn<?,?> column, String name)
    {
        column.getProperties().put(CONFIGURATION_NAME, name);
    }

    /**
     * Sets the column number of the column in the Swing version of the table, for migrating the Swing column width.
     * When any column of a table has a legacy index, only those columns use Swing column widths.  Otherwise, the
     * default column index is the Swing column number.
     * @param column to set
     * @param index of the column in the Swing table
     */
    public static void setLegacyIndex(TableColumn<?,?> column, int index)
    {
        column.getProperties().put(LEGACY_INDEX, index);
    }

    /**
     * Restores the default column order and visibility and removes the stored order and visibility settings.  The
     * column widths aren't changed.
     */
    public void reset()
    {
        mRestoring = true;

        try
        {
            if(!mTableView.getColumns().equals(mDefaultOrder))
            {
                mTableView.getColumns().setAll(mDefaultOrder);
            }

            for(TableColumn<S,?> column: mDefaultOrder)
            {
                column.setVisible(mDefaultVisibility.get(column));
            }
        }
        finally
        {
            mRestoring = false;
        }

        mPreferences.remove(mKey + ORDER);
        mPreferences.remove(mKey + HIDDEN);
        mPreferences.remove(mKey + KNOWN);
    }

    /**
     * Indicates if the column order and visibility are the defaults.
     */
    public boolean isDefault()
    {
        if(!mTableView.getColumns().equals(mDefaultOrder))
        {
            return false;
        }

        for(TableColumn<S,?> column: mDefaultOrder)
        {
            if(column.isVisible() != mDefaultVisibility.get(column))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Moves the column one position toward the start (-1) or the end (+1) of the table.
     * @param column to move
     * @param direction -1 or +1
     * @return true if the column moved
     */
    public boolean move(TableColumn<S,?> column, int direction)
    {
        ObservableList<TableColumn<S,?>> columns = mTableView.getColumns();
        int index = columns.indexOf(column);
        int target = index + Integer.signum(direction);

        if(index < 0 || direction == 0 || target < 0 || target >= columns.size())
        {
            return false;
        }

        //Rearrange a copy and apply it in one change, since a table view can't hold the same column twice
        List<TableColumn<S,?>> reordered = new ArrayList<>(columns);
        Collections.swap(reordered, index, target);
        columns.setAll(reordered);
        return true;
    }

    /**
     * Applies the stored column settings to the table.
     */
    private void restore(SwingPreference swingPreference, String swingKey)
    {
        mRestoring = true;

        try
        {
            Map<String,TableColumn<S,?>> columnMap = new LinkedHashMap<>();

            for(TableColumn<S,?> column: mDefaultOrder)
            {
                columnMap.put(column.getId(), column);
            }

            String order = mPreferences.get(mKey + ORDER, null);

            if(order != null)
            {
                List<TableColumn<S,?>> ordered = new ArrayList<>();

                for(String id: order.split(SEPARATOR))
                {
                    TableColumn<S,?> column = columnMap.get(id);

                    if(column != null && !ordered.contains(column))
                    {
                        ordered.add(column);
                    }
                }

                //Columns that aren't in the stored order are new, so show them at their default position
                for(int x = 0; x < mDefaultOrder.size(); x++)
                {
                    TableColumn<S,?> column = mDefaultOrder.get(x);

                    if(!ordered.contains(column))
                    {
                        ordered.add(Math.min(x, ordered.size()), column);
                    }
                }

                mTableView.getColumns().setAll(ordered);
            }

            String hidden = mPreferences.get(mKey + HIDDEN, null);

            if(hidden != null)
            {
                Set<String> hiddenIds = Set.of(hidden.isEmpty() ? new String[0] : hidden.split(SEPARATOR));
                Set<String> knownIds = getKnownIds();

                for(TableColumn<S,?> column: mDefaultOrder)
                {
                    //Columns that weren't in the table when the visibility was stored are new, so use the default
                    column.setVisible(knownIds.contains(column.getId()) ? !hiddenIds.contains(column.getId()) :
                        mDefaultVisibility.get(column));
                }

                if(getVisibleColumnCount() == 0)
                {
                    for(TableColumn<S,?> column: mDefaultOrder)
                    {
                        column.setVisible(mDefaultVisibility.get(column));
                    }
                }
            }

            boolean hasLegacyIndexes = mDefaultOrder.stream()
                .anyMatch(column -> column.getProperties().containsKey(LEGACY_INDEX));

            for(int x = 0; x < mDefaultOrder.size(); x++)
            {
                TableColumn<S,?> column = mDefaultOrder.get(x);
                double width = mPreferences.getDouble(getWidthKey(column), Double.NaN);
                Integer legacyIndex = column.getProperties().get(LEGACY_INDEX) instanceof Integer index ? index :
                    (hasLegacyIndexes ? null : x);

                if(Double.isNaN(width) && swingPreference != null && swingKey != null && legacyIndex != null)
                {
                    int swingWidth = swingPreference.getInt(swingKey + ".column." + legacyIndex, Integer.MAX_VALUE);

                    if(swingWidth != Integer.MAX_VALUE && swingWidth > 0)
                    {
                        width = swingWidth;
                    }
                }

                if(!Double.isNaN(width) && width > 0)
                {
                    column.setPrefWidth(width);
                }
            }
        }
        finally
        {
            mRestoring = false;
        }
    }

    /**
     * IDs of the columns that were in the table when the visibility was stored.  Settings stored before the known
     * columns were stored only had columns that are shown by default.
     */
    private Set<String> getKnownIds()
    {
        String known = mPreferences.get(mKey + KNOWN, null);

        if(known != null)
        {
            return Set.of(known.isEmpty() ? new String[0] : known.split(SEPARATOR));
        }

        Set<String> ids = new HashSet<>();

        for(TableColumn<S,?> column: mDefaultOrder)
        {
            if(mDefaultVisibility.get(column))
            {
                ids.add(column.getId());
            }
        }

        return ids;
    }

    private int getVisibleColumnCount()
    {
        int count = 0;

        for(TableColumn<S,?> column: mDefaultOrder)
        {
            if(column.isVisible())
            {
                count++;
            }
        }

        return count;
    }

    private void storeOrder()
    {
        List<String> ids = new ArrayList<>();

        for(TableColumn<S,?> column: mTableView.getColumns())
        {
            ids.add(column.getId());
        }

        mPreferences.put(mKey + ORDER, String.join(SEPARATOR, ids));
    }

    private void storeVisibility()
    {
        List<String> ids = new ArrayList<>();
        List<String> known = new ArrayList<>();

        for(TableColumn<S,?> column: mDefaultOrder)
        {
            known.add(column.getId());

            if(!column.isVisible())
            {
                ids.add(column.getId());
            }
        }

        mPreferences.put(mKey + HIDDEN, String.join(SEPARATOR, ids));
        mPreferences.put(mKey + KNOWN, String.join(SEPARATOR, known));
    }

    private String getWidthKey(TableColumn<S,?> column)
    {
        return mKey + WIDTH + column.getId();
    }
}
