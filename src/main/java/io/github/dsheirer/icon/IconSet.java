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
package io.github.dsheirer.icon;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "iconset")
public class IconSet
{
    private String mDefaultIcon;
    private List<Icon> mIcons = new ArrayList<>();

    public IconSet()
    {
        //No-arg JAXB constructor
    }

    public void add(Icon icon)
    {
        mIcons.add(icon);
    }

    public void remove(Icon icon)
    {
        mIcons.remove(icon);
    }

    @JacksonXmlProperty(isAttribute = false, localName = "icon")
    public List<Icon> getIcons()
    {
        if(mIcons == null)
        {
            mIcons = new ArrayList<>();
        }

        return mIcons;
    }

    public void setIcons(List<Icon> icons)
    {
        mIcons = icons;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "default")
    public String getDefaultIcon()
    {
        return mDefaultIcon;
    }

    public void setDefaultIcon(String name)
    {
        mDefaultIcon = name;
    }
}
