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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

@JacksonXmlRootElement(localName = "icon")
public class Icon implements Comparable<Icon>
{
    private final static Logger mLog = LoggerFactory.getLogger(Icon.class);

    protected String mName;
    private String mPath;
    private ImageIcon mImageIcon;

    public Icon()
    {
        //No-arg JAXB constructor
    }

    public Icon(String name, String path)
    {
        mName = name;
        mPath = path;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String getName()
    {
        return mName;
    }

    public void setName(String name)
    {
        mName = name;
    }

    public String toString()
    {
        return mName;
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

    @JsonIgnore
    public ImageIcon getIcon()
    {
        if(mImageIcon == null && mPath != null)
        {
            try
            {
                mImageIcon = new ImageIcon(mPath);
            }
            catch(Exception e)
            {
                mLog.error("Error loading Icon [" + mPath + "]", e);
            }
        }

        return mImageIcon;
    }

    @Override
    public int compareTo(Icon otherIcon)
    {
        return getName().compareTo(otherIcon.getName());
    }
}
