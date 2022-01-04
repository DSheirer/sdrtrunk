/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.controller;

import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory that applies custom names to threads
 */
public class NamingThreadFactory implements ThreadFactory 
{
    private final static Logger mLog = LoggerFactory.getLogger(ThreadPool.class);

    private static final AtomicInteger mPoolNumber = new AtomicInteger(1);
    
    private final ThreadGroup mThreadGroup;
    
    private final AtomicInteger mThreadNumber = new AtomicInteger(1);
    
    private final String mNamePrefix;

    public NamingThreadFactory( String prefix ) 
    {
        mThreadGroup = Thread.currentThread().getThreadGroup();
        mNamePrefix = prefix + " thread ";
    }

    public Thread newThread( Runnable runnable ) 
    {
        Thread thread = new Thread(mThreadGroup, runnable,
              mNamePrefix + mThreadNumber.getAndIncrement(), 0 );
        
        if( thread.isDaemon() )
        {
            thread.setDaemon( false );
        }
        
        if( thread.getPriority() != Thread.NORM_PRIORITY )
        {
            thread.setPriority( Thread.NORM_PRIORITY );
        }

        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread t, Throwable e)
            {
                mLog.error("Error while executing runnable in scheduled thread pool [" + t.getName() + "]", e);
            }
        });

        return thread;
    }
}