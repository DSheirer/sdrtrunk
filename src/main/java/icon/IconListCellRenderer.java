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

import javax.swing.*;
import java.awt.*;

public class IconListCellRenderer extends JLabel implements ListCellRenderer<Icon>
{
    private static final long serialVersionUID = 1L;

    private IconManager mIconManager;

    public IconListCellRenderer(IconManager iconManager)
    {
        mIconManager = iconManager;
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Icon> list, Icon icon, int index,
                                                  boolean isSelected, boolean cellHasFocus)
    {
        //Get a scaled version of the icon
        setIcon(mIconManager.getIcon(icon.getName(), 24));

        if(mIconManager.getModel().isDefaultIcon(icon))
        {
            setText(icon.getName() + " (default)");
        }
        else
        {
            setText(icon.getName());
        }

        if(isSelected)
        {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }
        else
        {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }
}
