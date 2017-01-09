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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconModel extends DefaultListModel<Icon>
{
    public static final String DEFAULT_ICON = "No Icon";

    private Icon mDefaultIcon;
    private Map<String, Icon> mIconMap = new HashMap<>();

    public IconModel(IconSet iconSet)
    {
        for(Icon icon : iconSet.getIcons())
        {
            this.addElement(icon);
        }

        String defaultIconName = iconSet.getDefaultIcon();

        if(defaultIconName == null)
        {
            defaultIconName = DEFAULT_ICON;
        }

        Icon defaultIcon = mIconMap.get(defaultIconName);

        if(defaultIcon != null)
        {
            mDefaultIcon = defaultIcon;
        }
        else
        {
            mDefaultIcon = new Icon(DEFAULT_ICON, "images/no_icon.png");
        }
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

    public Icon getDefaultIcon()
    {
        return mDefaultIcon;
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
            mDefaultIcon = icon;

            int index = indexOf(icon);

            fireContentsChanged(icon, index, index);
        }
    }

    /**
     * Returns the named icon or the default icon if the named icon does not exist
     */
    public Icon getIcon(String name)
    {
        if(name == null)
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

    @Override
    public void addElement(Icon icon)
    {
        super.addElement(icon);

        mIconMap.put(icon.getName(), icon);
    }

    @Override
    public boolean removeElement(Object object)
    {
        if(object instanceof Icon)
        {
            mIconMap.remove(((Icon) object).getName());
        }

        return super.removeElement(object);
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
