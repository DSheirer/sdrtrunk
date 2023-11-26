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

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Captures the state of table columns.
 */
public class TableState
{
    private List<ColumnState> mColumns = new ArrayList<>();
    private List<SortedColumn> mSort = new ArrayList<>();

    /**
     * Constructs an instance
     */
    public TableState()
    {
    }

    /**
     * Utility method to create a table state from a list of table columns
     * @param columns to capture state
     * @return table state instance
     */
    public static TableState create(TableView tableView)
    {
        TableState tableState = new TableState();

        List<TableColumn> columns = tableView.getColumns();
        for(TableColumn column: columns)
        {
            if(column.getId() == null || column.getId().isEmpty())
            {
                throw new IllegalArgumentException("TableColumns must have a non-null, non-empty ID value.");
            }

            tableState.addColumn(new ColumnState(column.getId(), column.isVisible(), (int)column.getWidth()));
        }

        List<TableColumn> sortedColumns = tableView.getSortOrder();
        for(TableColumn sortedColumn: sortedColumns)
        {
            if(sortedColumn.getId() == null || sortedColumn.getId().isEmpty())
            {
                throw new IllegalArgumentException("TableColumns must have a non-null, non-empty ID value.");
            }

            tableState.addSort(new SortedColumn(sortedColumn.getId(), sortedColumn.getSortType()));
        }

        return tableState;
    }

    /**
     * Column states.
     * @return column states
     */
    public List<ColumnState> getColumns()
    {
        return mColumns;
    }

    /**
     * Adds a column state
     * @param columnState to add
     */
    public void addColumn(ColumnState columnState)
    {
        mColumns.add(columnState);
    }

    /**
     * List of sorted columns
     * @return sorted columns
     */
    public List<SortedColumn> getSort()
    {
        return mSort;
    }

    /**
     * Add the sorted column
     * @param sortedColumn to add
     */
    public void addSort(SortedColumn sortedColumn)
    {
        mSort.add(sortedColumn);
    }

    /**
     * Column state.  Note: arg/variable names are abbreviated for brevity in json representation.
     * @param id for the column
     * @param v visible status
     * @param w width of the column
     */
    public record ColumnState(String id, boolean v, int w) {};

    /**
     * Sorted column
     * @param id of the column
     * @param sortType ascending or descending
     */
    public record SortedColumn(String id, TableColumn.SortType sortType) {};
}
