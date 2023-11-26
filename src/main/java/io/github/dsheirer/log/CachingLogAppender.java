/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * ****************************************************************************
 */

package io.github.dsheirer.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary logging appender to capture console messages during Spring Boot and include them in the log file that
 * is started by the ApplicationLog class.
 *
 * This appender is specified in the logback.xml that is used by Spring Boot to capture all logging.
 */
public class CachingLogAppender extends AppenderBase<ILoggingEvent>
{
    private static List<ILoggingEvent> sLOGGING_EVENTS = new ArrayList<>();
    private static boolean mEnabled = true;

    /**
     * Constructs an instance
     */
    public CachingLogAppender()
    {
    }

    @Override
    protected void append(ILoggingEvent eventObject)
    {
        if(mEnabled)
        {
            if(!sLOGGING_EVENTS.contains(eventObject))
            {
                sLOGGING_EVENTS.add(eventObject);
            }
        }
    }

    /**
     * Disables further caching of logging events.
     */
    public List<ILoggingEvent> disable()
    {
        List<ILoggingEvent> events = new ArrayList<>(sLOGGING_EVENTS);
        sLOGGING_EVENTS.clear();
        mEnabled = false;
        return events;
    }
}
