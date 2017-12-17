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
package io.github.dsheirer.util;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.commons.io.FileUtils;

/**
 * Custom logging plugin for logging memory usage with logback
 *
 * Add the following to the logback.xml configuration file to use this custom converter:
 *
 * <conversionRule conversionWord="memory_usage" converterClass="MemoryUsageLogger" />
 *
 * Update the encoder pattern to include %memory_usage within the pattern
 */
public class MemoryUsageLogger extends ClassicConverter
{
    @Override
    public String convert(ILoggingEvent iLoggingEvent)
    {
        //Method argument is ignored - we simply return memory usage statistics
        long allocated = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long used = allocated - free;

        int usedPercentage = (int)((double)(used) / (double)allocated * 100.0);

        StringBuilder sb = new StringBuilder();

        sb.append("[").append(FileUtils.byteCountToDisplaySize(used).replace(" ", ""));
        sb.append("/").append(FileUtils.byteCountToDisplaySize(allocated).replace(" ", ""));
        sb.append(" ").append(usedPercentage).append("%]");

        return sb.toString();
    }
}
