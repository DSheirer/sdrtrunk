/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.sample;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Delivers to a listener that isn't thread safe from producers that can overlap.  Dispatcher.stop() doesn't wait for
 * an in-progress buffer, so when a producer is replaced, the outgoing dispatcher thread can still be inside the listener
 * when the incoming dispatcher thread delivers its first buffer.  This class makes the incoming thread wait for the
 * outgoing thread to leave the listener, which means that the thread that replaces the producer doesn't have to wait.
 *
 * Producers should acquire no other locks before calling receive(), so that this is always the outermost lock.
 *
 * @param <T> type of element delivered to the listener
 */
public class SerializedListener<T> implements Listener<T>
{
    private final ReentrantLock mLock = new ReentrantLock();
    private volatile Listener<T> mListener;

    /**
     * Sets or changes the listener.  This does not wait for a delivery that is in progress to the previous listener.
     * @param listener to receive elements, or null to ignore elements.
     */
    public void setListener(Listener<T> listener)
    {
        mListener = listener;
    }

    /**
     * Delivers the element to the listener once no other thread is delivering to the listener.
     * @param t element to deliver
     */
    @Override
    public void receive(T t)
    {
        mLock.lock();

        try
        {
            Listener<T> listener = mListener;

            if(listener != null)
            {
                listener.receive(t);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }
}
