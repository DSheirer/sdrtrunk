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
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconTableModel extends AbstractTableModel
{
    public static final String DEFAULT_ICON = "No Icon";
    public static final int COLUMN_IMAGE_ICON = 0;
    public static final int COLUMN_ICON_NAME = 1;

    private Icon mDefaultIcon;

    private Map<String, Icon> mIconMap = new HashMap<>();
    private List<Icon> mIcons = new ArrayList<>();

    public IconTableModel()
    {
        setDefaultIcon(DEFAULT_ICON);
    }

    public IconTableModel(IconSet iconSet)
    {
        mIconMap.clear();
        mIcons.clear();

        fireTableDataChanged();

        for(Icon icon: iconSet.getIcons())
        {
            add(icon);
        }

        setDefaultIcon(iconSet.getDefaultIcon());
    }

    /**
     * Adds the icon to the model
     */
    public void add(Icon icon)
    {
        if(icon != null)
        {
            mIcons.add(icon);
            mIconMap.put(icon.getName(), icon);

            int index = mIcons.indexOf(icon);
            fireTableRowsInserted(index, index);
        }
    }

    /**
     * Removes the icon from the model
     */
    public void remove(Icon icon)
    {
        if(icon != null && mIcons.contains(icon))
        {
            int index = mIcons.indexOf(icon);

            mIcons.remove(icon);
            mIconMap.remove(icon.getName());

            fireTableRowsDeleted(index, index);
        }

    }

    public Icon get(int row)
    {
        if(row < mIcons.size())
        {
            return mIcons.get(row);
        }

        return null;
    }

    @Override
    public int getRowCount()
    {
        return mIcons.size();
    }

    @Override
    public int getColumnCount()
    {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if(rowIndex < mIcons.size() && columnIndex < getColumnCount())
        {
            Icon icon = get(rowIndex);

            switch(columnIndex)
            {
                case COLUMN_IMAGE_ICON:
                    return icon.getIcon();
                case COLUMN_ICON_NAME:
                    if(rowIndex == mIcons.indexOf(getDefaultIcon()))
                    {
                        return icon.getName() + " (default)";
                    }
                    else
                    {
                        return icon.getName();
                    }
            }
        }

        return null;
    }

    @Override
    public String getColumnName(int column)
    {
        if(column == COLUMN_ICON_NAME)
        {
            return "Name";
        }

        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        if(columnIndex == COLUMN_IMAGE_ICON)
        {
            return ImageIcon.class;
        }

        return String.class;
    }

    /**
     * Icons in an array - suitable for use in a JComboBox
     */
    public Icon[] getIconsAsArray()
    {
        List<Icon> icons = new ArrayList<>(mIconMap.values());

        Collections.sort(icons);

        return icons.toArray(new Icon[icons.size()]);
    }

    /**
     * Returns the current default icon
     */
    public Icon getDefaultIcon()
    {
        return mDefaultIcon;
    }

    /**
     * Indicates if the icon argument is the current default icon
     */
    public boolean isDefaultIcon(Icon icon)
    {
        return icon == getDefaultIcon();
    }

    /**
     * Indicates if this model contains an icon with the specified name
     *
     * @param name to check
     * @return true if name is not null and not empty and the internal map contains an icon matching name
     */
    public boolean hasIcon(String name)
    {
        return name != null && !name.isEmpty() && mIconMap.containsKey(name);
    }

    public void setDefaultIcon(Icon icon)
    {
        if(icon != null)
        {
            int currentIndex = mIcons.indexOf(getDefaultIcon());

            mDefaultIcon = icon;

            int newIndex = mIcons.indexOf(icon);

            fireTableCellUpdated(currentIndex, COLUMN_ICON_NAME);
            fireTableCellUpdated(newIndex, COLUMN_ICON_NAME);
        }
    }

    /**
     * Sets the default icon to the icon with the specified name, or to a default icon of the named icon does not exist
     */
    public void setDefaultIcon(String name)
    {
        if(name == null)
        {
            name = DEFAULT_ICON;
        }

        Icon defaultIcon = mIconMap.get(name);

        if(defaultIcon != null)
        {
            setDefaultIcon(defaultIcon);
        }
        else
        {
            defaultIcon = new Icon(DEFAULT_ICON, "images/no_icon.png");
            add(defaultIcon);
            setDefaultIcon(defaultIcon);
        }
    }

    /**
     * Returns the named icon or the default icon if the named icon does not exist
     */
    public Icon getIcon(String name)
    {
        if(name == null || name.isEmpty())
        {
            return mDefaultIcon;
        }

        Icon icon = mIconMap.get(name);

        if(icon == null)
        {
            icon = mDefaultIcon;
        }

        return icon;
    }

    /**
     * Creates a new icon set from a snapshot of the icons contained in this model
     */
    public IconSet getIconSet()
    {
        IconSet iconSet = new IconSet();
        iconSet.setIcons(new ArrayList<Icon>(mIconMap.values()));
        iconSet.setDefaultIcon(getDefaultIcon().getName());
        return iconSet;
    }
}
