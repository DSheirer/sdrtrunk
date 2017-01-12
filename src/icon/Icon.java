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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "icon")
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

    @XmlAttribute(name = "name")
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

    @XmlAttribute(name = "path")
    public String getPath()
    {
        return mPath;
    }

    public void setPath(String path)
    {
        mPath = path;
    }

    @XmlTransient
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
