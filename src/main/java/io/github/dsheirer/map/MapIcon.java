/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingType;


public class MapIcon extends Setting implements Comparable<MapIcon>
{

    private String mPath;

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public SettingType getType()
    {
        return SettingType.MAP_ICON;
    }

    /**
     * Only map icons created at runtime can be marked as non-editable, and
     * therefore this property is transient.
     */
    @JsonIgnore
    private boolean mEditable;

    @JsonIgnore
    private boolean mDefaultIcon;

    /**
     * Wrapper class for a map icon.
     *
     * @param name - name of the icon - also used as key to lookup the icon
     * @param path - file path to the icon
     * @param editable - defines if the map icon or details can be edited
     *
     * Note: the default icons are constructed with editable = false, so that
     * they cannot be deleted from the Icon Manager editor window
     */
    public MapIcon(String name, String path, boolean editable)
    {
        super(name);
        mPath = path;
        mEditable = editable;
    }

    public MapIcon(String name, String path)
    {
        this(name, path, true);
    }

    /**
     * Don't use this constructor.  This is used by JAXB to unmarshall saved
     * map icons.
     */
    public MapIcon()
    {
        mEditable = true;
    }


    @JacksonXmlProperty(isAttribute = true, localName = "editable")
    public boolean isEditable()
    {
        return mEditable;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "default")
    public boolean isDefaultIcon()
    {
        return mDefaultIcon;
    }

    public void setDefaultIcon(boolean isDefault)
    {
        mDefaultIcon = isDefault;
    }


    @JacksonXmlProperty(isAttribute = true, localName = "path")
    public String getPath()
    {
        return mPath;
    }

    public void setPath(String path)
    {
        mPath = path;
    }

    public String toString()
    {
        if(mDefaultIcon)
        {
            return getName() + " (default)";
        }
        else
        {
            return getName();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof MapIcon)
        {
            MapIcon other = (MapIcon)obj;

            return other.getName().contentEquals(getName()) &&
                other.getPath().contentEquals(getPath());
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode() + getPath().hashCode();
    }

    /**
     * Sort order is determined by the icon name
     */
    @Override
    public int compareTo(MapIcon other)
    {
        return getName().compareTo(other.getName());
    }
}
