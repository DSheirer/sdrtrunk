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
import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.icon.IconModel;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

import javax.swing.UIManager;

/**
 * Table column and cell helpers for the Now Playing tables.
 */
public final class NowPlayingCells
{
    private static final String ALIGNMENT = "now.playing.cell.alignment";

    private NowPlayingCells()
    {
    }

    /**
     * Creates a table column.
     * @param id that identifies the column for storing the column settings.  Don't change it, or users lose the
     * stored order, visibility and width of the column.
     * @param text for the column header
     * @param configurationName for the column in the column configuration editor, or null to use the header text
     * @param alignment of the cell contents
     * @param value of the column for the row item.  The value is recalculated each time the row revision changes.
     * @param revision of the row item, or null when the row value doesn't change
     */
    public static <S,T> TableColumn<S,T> column(String id, String text, String configurationName, Pos alignment,
                                                Function<S,T> value, Function<S,ObservableValue<?>> revision)
    {
        TableColumn<S,T> column = new TableColumn<>(text);
        column.setId(id);

        if(configurationName != null)
        {
            TableColumnMonitor.setName(column, configurationName);
        }

        column.getProperties().put(ALIGNMENT, alignment);
        column.setCellFactory(formatted(Object::toString));

        if(revision == null)
        {
            column.setCellValueFactory(param -> Bindings.createObjectBinding(() -> value.apply(param.getValue())));
        }
        else
        {
            column.setCellValueFactory(param -> Bindings.createObjectBinding(() -> value.apply(param.getValue()),
                revision.apply(param.getValue())));
        }

        return column;
    }

    /**
     * Cell factory that shows the cell value formatted as text.
     * @param formatter for non-null values.  Can return null for an empty cell.
     */
    public static <S,T> Callback<TableColumn<S,T>,TableCell<S,T>> formatted(Function<T,String> formatter)
    {
        return column -> new AlignedCell<>(column)
        {
            @Override
            protected void updateItem(T item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.apply(item));
            }
        };
    }

    /**
     * Cell factory for alias values that shows the alias names in the alias color with the alias icon.
     * @param iconModel for the alias icons
     */
    public static <S> Callback<TableColumn<S,AliasDisplay>,TableCell<S,AliasDisplay>> alias(IconModel iconModel)
    {
        return column -> new AlignedCell<>(column)
        {
            @Override
            protected void updateItem(AliasDisplay item, boolean empty)
            {
                super.updateItem(item, empty);

                if(empty || item == null)
                {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                }
                else
                {
                    setText(item.text());

                    Icon icon = iconModel.getIcon(item.iconName());
                    setGraphic(icon != null && icon.getFxImage() != null ? new ImageView(icon.getFxImage()) : null);

                    java.awt.Color color = new java.awt.Color(item.color());
                    java.awt.Color background = UIManager.getColor("Table.background");
                    java.awt.Color readable = background != null ?
                        ThemeManager.getInstance().getReadableColor(color, background) : color;
                    String fill = toWebColor(readable.getRGB());
                    //An inline style, since the themed table row style otherwise replaces the text fill
                    setStyle("-fx-text-fill: " + fill + ";");
                }
            }
        };
    }

    /**
     * Formats the RGB value as a web color.
     * @param rgb color value
     * @return web color, e.g. #FF0000
     */
    public static String toWebColor(int rgb)
    {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /**
     * Table cell that aligns its contents with the alignment of its column.
     */
    public static class AlignedCell<S,T> extends TableCell<S,T>
    {
        /**
         * Constructs an instance
         * @param column of the cell
         */
        public AlignedCell(TableColumn<S,T> column)
        {
            if(column.getProperties().get(ALIGNMENT) instanceof Pos alignment)
            {
                setAlignment(alignment);
            }
        }
    }
}
