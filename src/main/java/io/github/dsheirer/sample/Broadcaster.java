/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Broadcasts an item to multiple listeners
 */
public class Broadcaster<T> implements Listener<T>
{
    private final static Logger mLog = LoggerFactory.getLogger(Broadcaster.class);
    private boolean mDebug;
    private ReentrantLock mLock = new ReentrantLock();
    private List<Listener<T>> mListeners = new ArrayList<>();

    public Broadcaster()
    {
    }

    /**
     * Turns on/off debugging to troubleshoot and log which listeners are receiving an item.
     */
    public void setDebug(boolean debug)
    {
        mDebug = debug;
    }

    /**
     * Implements the Listener<T> interface to receive an element and broadcast that element to all registered
     * listeners.
     *
     * @param t element to broadcast
     */
    @Override
    public void receive(T t)
    {
        broadcast(t);
    }

    /**
     * Clear listeners to prepare for garbage collection
     */
    public void dispose()
    {
        mLock.lock();

        try
        {
            mListeners.clear();
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Indicates if this broadcaster has any listeners registered
     */
    public boolean hasListeners()
    {
        return !mListeners.isEmpty();
    }

    /**
     * The count of listeners currently registered with this broadcaster
     */
    public int getListenerCount()
    {
        return mListeners.size();
    }

    /**
     * The list of listeners currently registered with this broadcaster
     */
    public List<Listener<T>> getListeners()
    {
        return Collections.unmodifiableList(mListeners);
    }

    /**
     * Registers the listener to receive elements from this broadcaster
     *
     * @param listener
     */
    public void addListener(Listener<T> listener)
    {
        if(listener != null)
        {
            mLock.lock();

            try
            {
                if(!mListeners.contains(listener))
                {
                    mListeners.add(listener);
                }
            }
            finally
            {
                mLock.unlock();
            }
        }
    }

    /**
     * Deregisters the listener from receiving elements from this broadcaster
     */
    public void removeListener(Listener<T> listener)
    {
        if(listener != null)
        {
            mLock.lock();

            try
            {
                mListeners.remove(listener);
            }
            finally
            {
                mLock.unlock();
            }
        }
    }

    /**
     * Deregisters all listeners from this broadcaster
     */
    public void clear()
    {
        mLock.lock();

        try
        {
            mListeners.clear();
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Broadcasts the element to all registered listeners
     */
    public void broadcast(T t)
    {
        mLock.lock();

        try
        {
            if(mDebug)
            {
                for(Listener<T> listener : mListeners)
                {
                    mLog.debug("Sending [" + t + "] to listener [" + listener.getClass() + "]");
                    listener.receive(t);
                    mLog.debug("Finished sending to listener [" + listener.getClass() + "]");
                }
            }
            else
            {
                for(Listener<T> listener : mListeners)
                {
                    listener.receive(t);
                }
            }
        }
        finally
        {
            mLock.unlock();
        }
    }
}
