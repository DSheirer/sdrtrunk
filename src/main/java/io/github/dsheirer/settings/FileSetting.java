/*
 * *********************************************************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 * *********************************************************************************************************************
 */
package io.github.dsheirer.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.io.File;

public class FileSetting extends Setting
{
    private String mPath;

    public FileSetting()
    {
        super();
    }

    public FileSetting(String name, String path)
    {
        super(name);
        setPath(path);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public SettingType getType()
    {
        return SettingType.FILE_SETTING;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "path")
    public String getPath()
    {
        return mPath;
    }

    public void setPath(String value)
    {
        mPath = value;
    }

    @JsonIgnore
    public File getFile()
    {
        return new File(mPath);
    }
}
