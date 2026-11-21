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

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

/**
 * Button that opens the column configuration editor for a table.
 */
public class ColumnConfigurationButton extends Button
{
    private final TableColumnMonitor<?> mTableColumnMonitor;
    private ColumnConfigurationEditor mEditor;

    /**
     * Constructs an instance
     * @param tableColumnMonitor for the table to configure
     * @param title for the editor window
     */
    public ColumnConfigurationButton(TableColumnMonitor<?> tableColumnMonitor, String title)
    {
        super("Columns");
        mTableColumnMonitor = tableColumnMonitor;
        IconNode iconNode = new IconNode(FontAwesome.COLUMNS);
        //Follow the themed text color so that the icon is readable in light and dark themes
        iconNode.fillProperty().bind(textFillProperty());
        setGraphic(iconNode);
        setTooltip(new Tooltip("Choose the columns to show and the column order"));
        setOnAction(event -> {
            if(mEditor == null)
            {
                mEditor = new ColumnConfigurationEditor(mTableColumnMonitor, title);
            }

            mEditor.show(this);
        });
    }
}
