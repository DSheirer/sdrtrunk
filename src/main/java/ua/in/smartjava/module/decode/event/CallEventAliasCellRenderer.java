/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package ua.in.smartjava.module.decode.event;

import ua.in.smartjava.alias.Alias;
import ua.in.smartjava.icon.IconManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Custom renderer for JTable cells that contain Alias objects.  Renders the
 * ua.in.smartjava.alias name, and if there is an ua.in.smartjava.icon name, attempts to get that ua.in.smartjava.icon from
 * the settings manager and render it.
 */
public class CallEventAliasCellRenderer extends DefaultTableCellRenderer
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(CallEventAliasCellRenderer.class);

    private IconManager mIconManager;

    public CallEventAliasCellRenderer(IconManager iconManager)
    {
        super();
        mIconManager = iconManager;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if(value instanceof Alias)
        {
            Alias alias = (Alias)value;

            label.setText(alias.getName());
            label.setForeground(alias.getDisplayColor());
            label.setIcon(mIconManager.getIcon(alias.getIconName(), IconManager.DEFAULT_ICON_SIZE));
        }
        else
        {
            label.setText("");
            label.setForeground(table.getForeground());
            label.setIcon(null);
        }

        return label;
    }
}
