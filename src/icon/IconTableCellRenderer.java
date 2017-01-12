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
package icon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class IconTableCellRenderer extends JLabel implements TableCellRenderer
{
    private final static Logger mLog = LoggerFactory.getLogger(IconTableCellRenderer.class);

    private static final long serialVersionUID = 1L;

    private IconManager mIconManager;

    public IconTableCellRenderer(IconManager iconManager)
    {
        mIconManager = iconManager;
        setOpaque(true);
        setHorizontalAlignment(JLabel.CENTER);
        setPreferredSize(new Dimension(40,34));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column)
    {
        if(value instanceof ImageIcon)
        {
            //Get a scaled version of the icon
            setIcon(mIconManager.getScaledIcon((ImageIcon)value, 24));
        }
        else
        {
            setIcon(null);
        }

        if(isSelected)
        {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        }
        else
        {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }

        return this;
    }
}
