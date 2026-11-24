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
package io.github.dsheirer.gui.nowplaying;

import io.github.dsheirer.gui.table.TableColumnMonitor;
import io.github.dsheirer.settings.swing.SwingSettings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Table of a history of items, newest first, with filter, clear, history size and column configuration controls.
 *
 * Use this class on the JavaFX application thread.
 *
 * @param <T> type of the history items
 * @param <F> type of the filtered objects
 */
public abstract class HistoryTableView<T,F> extends VBox
{
    private static final KeyCombination COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
    //Row revision changes are in-place list updates so that the filter and sort re-check the updated item without
    //replacing the row in the table
    private final ObservableList<Row<T>> mRows =
        FXCollections.observableArrayList(row -> new Observable[]{row.revisionProperty()});
    private final FilteredList<Row<T>> mFilteredRows = new FilteredList<>(mRows);
    private final Map<T,Row<T>> mRowMap = new HashMap<>();
    private final TableView<Row<T>> mTableView = new TableView<>();
    private final TableColumnMonitor<Row<T>> mTableColumnMonitor;
    private final HistoryControls<F> mHistoryControls;
    private int mHistorySize = HistoryControls.DEFAULT_HISTORY_SIZE;

    /**
     * Constructs an instance
     * @param columns for the table
     * @param tableKey for the stored column settings
     * @param swingSettings with the legacy Swing column widths
     * @param swingTableKey of the legacy Swing table
     * @param filterEditorTitle for the filter editor window
     * @param columnsTitle for the column configuration editor window
     */
    protected HistoryTableView(List<TableColumn<Row<T>,?>> columns, String tableKey, SwingSettings swingSettings,
                               String swingTableKey, String filterEditorTitle, String columnsTitle)
    {
        mTableView.getColumns().addAll(columns);
        SortedList<Row<T>> sortedRows = new SortedList<>(mFilteredRows);
        sortedRows.comparatorProperty().bind(mTableView.comparatorProperty());
        mTableView.setItems(sortedRows);
        mTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        mTableView.setPlaceholder(new Label(""));
        mTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        mTableView.setOnKeyPressed(event -> {
            if(COPY.match(event))
            {
                ClipboardContent content = new ClipboardContent();
                content.putString(getSelectedText());
                Clipboard.getSystemClipboard().setContent(content);
                event.consume();
            }
        });
        mTableColumnMonitor = new TableColumnMonitor<>(mTableView, tableKey, swingSettings, swingTableKey);
        mHistoryControls = new HistoryControls<>(filterEditorTitle, this::clear, this::setHistorySize,
            mTableColumnMonitor, columnsTitle);

        VBox.setVgrow(mTableView, Priority.ALWAYS);
        getChildren().addAll(mHistoryControls, mTableView);
        refilter();
    }

    /**
     * Indicates if the item passes the current filters
     */
    protected abstract boolean include(T item);

    /**
     * History controls toolbar
     */
    protected HistoryControls<F> getHistoryControls()
    {
        return mHistoryControls;
    }

    /**
     * Re-applies the filters to the items, after a filter change.
     */
    protected void refilter()
    {
        mFilteredRows.setPredicate(row -> include(row.getItem()));
    }

    /**
     * Adds the item to the top of the history, or updates the row of the item when it is already in the history.
     * Removes the oldest items when the history is larger than the history size.
     */
    protected void add(T item)
    {
        Row<T> row = mRowMap.get(item);

        if(row != null)
        {
            row.refresh();
        }
        else
        {
            row = new Row<>(item);
            mRowMap.put(item, row);
            mRows.addFirst(row);
            trim();
        }
    }

    /**
     * Replaces the history with the items, oldest first.
     */
    protected void setItems(List<T> items)
    {
        clear();

        for(T item: items)
        {
            add(item);
        }
    }

    /**
     * Removes all items from the history
     */
    protected void clear()
    {
        mRows.clear();
        mRowMap.clear();
    }

    /**
     * Updates the cells of all rows, after a change to the display formats.
     */
    protected void refreshAll()
    {
        mRows.forEach(Row::refresh);
    }

    /**
     * Text of the selected rows, one line per row with the visible column values separated by tabs.
     */
    private String getSelectedText()
    {
        StringJoiner text = new StringJoiner("\n");

        for(int row = 0; row < mTableView.getItems().size(); row++)
        {
            if(mTableView.getSelectionModel().isSelected(row))
            {
                StringJoiner line = new StringJoiner("\t");

                for(TableColumn<Row<T>,?> column: mTableView.getVisibleLeafColumns())
                {
                    Object value = column.getCellData(row);
                    line.add(value == null ? "" : value.toString());
                }

                text.add(line.toString());
            }
        }

        return text.toString();
    }

    private void setHistorySize(int historySize)
    {
        mHistorySize = historySize;
        trim();
    }

    private void trim()
    {
        while(mRows.size() > mHistorySize)
        {
            mRowMap.remove(mRows.removeLast().getItem());
        }
    }

    /**
     * Table row item for a history item.  The revision increments each time the item changes so that the row cells
     * update.
     */
    public static class Row<T>
    {
        private final T mItem;
        private final IntegerProperty mRevision = new SimpleIntegerProperty();

        Row(T item)
        {
            mItem = item;
        }

        public T getItem()
        {
            return mItem;
        }

        public IntegerProperty revisionProperty()
        {
            return mRevision;
        }

        void refresh()
        {
            mRevision.set(mRevision.get() + 1);
        }
    }
}
