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

import io.github.dsheirer.controller.NamingThreadFactory;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ThreadPool
{
    private final static Logger mLog = LoggerFactory.getLogger(ThreadPool.class);

    private static int CORES = Runtime.getRuntime().availableProcessors();
    public static ScheduledExecutorService SCHEDULED;

    static
    {
        //Create a scheduled thread pool sized according to the available processors/cores, minimum 2
        CORES = (FastMath.max(CORES, 2));

        SCHEDULED = Executors.newScheduledThreadPool(CORES, new NamingThreadFactory("sdrtrunk"));
    }

    /**
     * Application-wide shared thread pools and scheduled executor service.
     */
    public ThreadPool()
    {
    }

    public static void logSettings()
    {
        mLog.info("Application thread pool created with [" + CORES + "] threads");
    }
}
