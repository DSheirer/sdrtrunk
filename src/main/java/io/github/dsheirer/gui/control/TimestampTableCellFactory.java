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

package io.github.dsheirer.gui.control;

import java.text.SimpleDateFormat;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Table cell factory that formats a long timestamp column value as a formatted date/time stamp.
 * @param <T> is the table row entity type
 */
public class TimestampTableCellFactory<T> implements Callback<TableColumn<T, Long>, TableCell<T, Long>>
{
    private SimpleDateFormat mSDF = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Override
    public TableCell<T, Long> call(TableColumn<T, Long> param)
    {
        return new TableCell<>()
        {
            @Override
            protected void updateItem(Long item, boolean empty)
            {
                super.updateItem(item, empty);

                if(empty)
                {
                    setText(null);
                }
                else
                {
                    if(item instanceof Long l)
                    {
                        String a = mSDF.format(l);
                        setText(a);
                    }
                    else
                    {
                        setText("Not a Long: " + item.getClass());
                    }
                }
            }
        };
    }
}
