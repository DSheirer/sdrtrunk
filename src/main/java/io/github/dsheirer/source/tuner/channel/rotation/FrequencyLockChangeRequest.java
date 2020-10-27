/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.channel.rotation;

import io.github.dsheirer.module.ModuleEventBusMessage;

/**
 * Request to lock or unlock a frequency from use.
 */
public class FrequencyLockChangeRequest extends ModuleEventBusMessage
{
    private long mFrequency;
    private boolean mLock;

    /**
     * Constructs an instance
     * @param frequency for the request
     * @param lock true if this is a lock or false if this is an unlock
     */
    public FrequencyLockChangeRequest(long frequency, boolean lock)
    {
        mFrequency = frequency;
        mLock = lock;
    }

    /**
     * Frequency for this request
     */
    public long getFrequency()
    {
        return mFrequency;
    }

    /**
     * Indicates if this request is to lock the frequency
     */
    public boolean isLockRequest()
    {
        return mLock;
    }

    /**
     * Indicates if this request is to unlock the frequency
     */
    public boolean isUnlockRequest()
    {
        return !mLock;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("FREQUENCY ").append(isLockRequest() ? "LOCK" : "UNLOCK").append(" REQUEST: ").append(getFrequency());
        return sb.toString();
    }

    /**
     * Utility method to create a lock request
     * @param frequency to lock
     * @return request
     */
    public static FrequencyLockChangeRequest lock(long frequency)
    {
        return new FrequencyLockChangeRequest(frequency, true);
    }

    /**
     * Utility method to create an unlock request
     * @param frequency to unlock
     * @return request
     */
    public static FrequencyLockChangeRequest unlock(long frequency)
    {
        return new FrequencyLockChangeRequest(frequency, false);
    }
}
