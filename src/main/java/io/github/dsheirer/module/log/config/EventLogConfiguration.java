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
package io.github.dsheirer.module.log.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.controller.config.Configuration;
import io.github.dsheirer.module.log.EventLogType;

import java.util.ArrayList;
import java.util.List;

@JsonSubTypes.Type(value = EventLogConfiguration.class, name = "eventLogConfiguration")
@JacksonXmlRootElement(localName = "event_log_configuration")
public class EventLogConfiguration extends Configuration
{
    protected List<EventLogType> mLoggers = new ArrayList<EventLogType>();

    public EventLogConfiguration()
    {
    }

    @JacksonXmlProperty(isAttribute = false, localName = "logger")
    public List<EventLogType> getLoggers()
    {
        return mLoggers;
    }

    public void setLoggers(ArrayList<EventLogType> loggers)
    {
        mLoggers = loggers;
    }

    public void addLogger(EventLogType logger)
    {
        mLoggers.add(logger);
    }

    public void clear()
    {
        mLoggers.clear();
    }
}
