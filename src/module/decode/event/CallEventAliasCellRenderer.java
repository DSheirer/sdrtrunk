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
package module.decode.event;

import alias.Alias;
import icon.IconManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Custom renderer for JTable cells that contain Alias objects.  Renders the
 * alias name, and if there is an icon name, attempts to get that icon from
 * the settings manager and render it.
 */
public class CallEventAliasCellRenderer extends DefaultTableCellRenderer
{
    private static final long serialVersionUID = 1L;

    private IconManager mIconManager;

    public CallEventAliasCellRenderer(IconManager iconManager)
    {
        super();
        mIconManager = iconManager;
    }

    public void setValue(Object obj)
    {
        if(obj != null && obj instanceof Alias)
        {
            Alias alias = (Alias) obj;

            setText(alias.getName());

            ImageIcon icon = getIcon(alias, IconManager.DEFAULT_ICON_SIZE);

            if(icon != null)
            {
                setIcon(icon);
            }
        }
        else
        {
            setText(" ");
            setIcon(null);
        }
    }

    private ImageIcon getIcon(Alias alias, int height)
    {
        if(mIconManager != null && alias != null)
        {
            return mIconManager.getIcon(alias.getIconName(), height);
        }

        return null;
    }
}
