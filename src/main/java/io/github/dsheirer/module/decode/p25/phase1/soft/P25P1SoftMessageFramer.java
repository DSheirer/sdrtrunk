/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.soft;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.sample.Listener;

public class P25P1SoftMessageFramer implements Listener<Dibit>
{
    private Listener<IMessage> mMessageListener;
    private boolean mRunning = false;
    private int mDibitCounter = 0;
    private int mDibitSinceTimestampCounter = 0;
    private long mReferenceTimestamp = 0;

    @Override
    public void receive(Dibit dibit)
    {

    }

    /**
     * External trigger that a sync pattern is detected and the next arriving dibit is the start of the message that
     * contains that sync.
     */
    public void syncDetected()
    {

    }

    /**
     * Starts this framer dispatching messages
     */
    public void start()
    {
        mRunning = true;
    }

    /**
     * Stops this framer from dispatching messages
     */
    public void stop()
    {
        mRunning = false;
    }

    /**
     * Sets the listener to receive framed DMR messages.
     * @param listener for messages.
     */
    public void setListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Sets or updates the current dibit stream time from an incoming sample buffer.
     * @param time to use as a reference timestamp.
     */
    public void setTimestamp(long time)
    {
        mReferenceTimestamp = time;
        mDibitSinceTimestampCounter = 0;
    }

    /**
     * Calculates the timestamp accurate to the currently received dibit.
     * @return timestamp in milliseconds.
     */
    private long getTimestamp()
    {
        if(mReferenceTimestamp > 0)
        {
            return mReferenceTimestamp + (long)(1000.0 * mDibitSinceTimestampCounter / 4800);
        }
        else
        {
            mDibitSinceTimestampCounter = 0;
            return System.currentTimeMillis();
        }
    }
}
