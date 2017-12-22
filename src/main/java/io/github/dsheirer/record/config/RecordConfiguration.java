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
package io.github.dsheirer.record.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.controller.config.Configuration;
import io.github.dsheirer.record.RecorderType;

import java.util.ArrayList;
import java.util.List;

public class RecordConfiguration extends Configuration
{
    private List<RecorderType> mRecorders = new ArrayList<>();

    public RecordConfiguration()
    {
    }

    @JacksonXmlProperty(isAttribute = false, localName = "recorder")
    public List<RecorderType> getRecorders()
    {
        return mRecorders;
    }

    public void setRecorders(List<RecorderType> recorders)
    {
        mRecorders = recorders;
    }

    public void addRecorder(RecorderType recorder)
    {
        mRecorders.add(recorder);
    }

    public void clearRecorders()
    {
        mRecorders.clear();
    }
}
