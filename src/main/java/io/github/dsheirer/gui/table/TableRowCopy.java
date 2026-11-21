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

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Copying table rows to the clipboard as tab separated text (pastes into a spreadsheet), and keeping a multiple row
 * selection while the rows of a live table are replaced.
 */
public final class TableRowCopy
{
    /**
     * Column property with a Function&lt;T,String&gt; that provides the copied text for a column whose cell value
     * isn't text (e.g. an icon column).
     */
    public static final String COPY_TEXT = "table.row.copy.text";
    public static final KeyCombination COPY_KEY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

    private TableRowCopy()
    {
    }

    /**
     * Sets the copied text function for a column whose cell value isn't the text to copy.
     */
    public static <T> void setCopyText(TableColumn<T,?> column, Function<T,String> text)
    {
        column.getProperties().put(COPY_TEXT, text);
    }

    /**
     * Indicates if the key event is the copy shortcut (Ctrl+C, or Cmd+C on macOS)
     */
    public static boolean isCopy(KeyEvent event)
    {
        return COPY_KEY.match(event);
    }

    /**
     * Copies the selected rows of the table (in displayed order, with the visible columns) to the clipboard.
     * @return number of rows copied
     */
    public static <T> int copySelected(TableView<T> table)
    {
        List<Integer> indices = new ArrayList<>(table.getSelectionModel().getSelectedIndices());
        indices.sort(null);
        List<T> rows = new ArrayList<>();

        for(Integer index: indices)
        {
            if(index != null && index >= 0 && index < table.getItems().size())
            {
                rows.add(table.getItems().get(index));
            }
        }

        if(!rows.isEmpty())
        {
            ClipboardContent content = new ClipboardContent();
            content.putString(toText(table.getVisibleLeafColumns(), rows));
            Clipboard.getSystemClipboard().setContent(content);
        }

        return rows.size();
    }

    /**
     * Tab separated text for the rows: a header line with the column names and a line per row.  Tabs and line breaks
     * in values are replaced with spaces so that each row stays on one line.
     */
    @SuppressWarnings("unchecked")
    public static <T> String toText(List<TableColumn<T,?>> columns, List<T> rows)
    {
        StringBuilder sb = new StringBuilder();
        List<String> header = new ArrayList<>();

        for(TableColumn<T,?> column: columns)
        {
            header.add(clean(TableColumnMonitor.getName(column)));
        }

        sb.append(String.join("\t", header)).append(System.lineSeparator());

        for(T row: rows)
        {
            List<String> cells = new ArrayList<>();

            for(TableColumn<T,?> column: columns)
            {
                Object copyText = column.getProperties().get(COPY_TEXT);
                String text;

                if(copyText instanceof Function<?,?> function)
                {
                    text = ((Function<T,String>)function).apply(row);
                }
                else
                {
                    ObservableValue<?> value = column.getCellObservableValue(row);
                    text = value != null && value.getValue() != null ? value.getValue().toString() : "";
                }

                cells.add(clean(text));
            }

            sb.append(String.join("\t", cells)).append(System.lineSeparator());
        }

        return sb.toString();
    }

    private static String clean(String value)
    {
        return value == null ? "" : value.replace("\r\n", " ").replace('\t', ' ').replace('\n', ' ')
            .replace('\r', ' ');
    }

    /**
     * Applies a change to the table rows and keeps the selected rows selected.  Replacing a row (e.g. a live update)
     * would otherwise drop it from the selection.
     * @param table with the rows
     * @param keys identity keys of a row (e.g. live key and database id) - rows that share a key are the same row
     * @param change to apply to the rows
     */
    public static <T> void preserveSelection(TableView<T> table, Function<T,Collection<?>> keys, Runnable change)
    {
        List<T> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());

        if(selected.isEmpty())
        {
            change.run();
            return;
        }

        Set<Object> selectedKeys = new HashSet<>();

        for(T row: selected)
        {
            if(row != null)
            {
                selectedKeys.addAll(keys.apply(row));
            }
        }

        change.run();

        List<Integer> indices = new ArrayList<>();

        for(int x = 0; x < table.getItems().size(); x++)
        {
            for(Object key: keys.apply(table.getItems().get(x)))
            {
                if(selectedKeys.contains(key))
                {
                    indices.add(x);
                    break;
                }
            }
        }

        table.getSelectionModel().clearSelection();

        if(!indices.isEmpty())
        {
            int[] rest = indices.subList(1, indices.size()).stream().mapToInt(Integer::intValue).toArray();
            table.getSelectionModel().selectIndices(indices.getFirst(), rest);
        }
    }
}
