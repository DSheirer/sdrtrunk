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
package io.github.dsheirer.sample;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcasts an item to multiple listeners
 */
public class Broadcaster<T> implements Listener<T>
{
    protected List<Listener<T>> mListeners = new CopyOnWriteArrayList<>();

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
        mListeners.clear();
    }

    /**
     * Indicates if this broadcaster has any listeners registered
     */
    public boolean hasListeners()
    {
        return !mListeners.isEmpty();
    }

    /**
     * Indicates if the listener is currently registered with this broadcaster
     */
    public boolean hasListener(Listener<T> listener)
    {
        return listener != null && mListeners.contains(listener);
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
        return mListeners;
    }

    /**
     * Registers the listener to receive elements from this broadcaster
     *
     * @param listener
     */
    public void addListener(Listener<T> listener)
    {
        if(listener != null && !mListeners.contains(listener))
        {
            mListeners.add(listener);
        }
    }

    /**
     * Deregisters the listener from receiving elements from this broadcaster
     */
    public void removeListener(Listener<T> listener)
    {
        if(listener != null && mListeners.contains(listener))
        {
            mListeners.remove(listener);
        }
    }

    /**
     * Deregisters all listeners from this broadcaster
     */
    public void clear()
    {
        mListeners.clear();
    }

    /**
     * Broadcasts the element to all registered listeners
     */
    public void broadcast(T t)
    {
        for(Listener<T> listener : mListeners)
        {
            listener.receive(t);
        }
    }
}
