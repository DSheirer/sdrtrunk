/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.dsp.squelch;

import io.github.dsheirer.dsp.filter.iir.SinglePoleIirFilter;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Power monitor.  Provides periodic broadcast of current channel power from I/Q sample buffers.
 */
public class PowerMonitor
{
    private static final Logger mLog = LoggerFactory.getLogger(PowerMonitor.class);
    private int mPowerLevelBroadcastCount = 0;
    private int mPowerLevelBroadcastThreshold;
    private Listener<SourceEvent> mSourceEventListener;
    private final SinglePoleIirFilter mPowerFilter = new SinglePoleIirFilter(0.1f);

    /**
     * Constructs an instance
     */
    public PowerMonitor()
    {
        mPowerLevelBroadcastThreshold = 12500; //Based on a default sample rate of 25 kHz
    }

    /**
     * Sets the sample rate to effect the frequency of power level notifications where the notifications are
     * sent twice a second.
     * @param sampleRate in hertz
     */
    public void setSampleRate(int sampleRate)
    {
        mPowerLevelBroadcastThreshold = sampleRate / 2;
    }

    /**
     * Processes I&Q complex baseband sample buffers.
     */
    public void process(float[] i, float[] q)
    {
        mPowerLevelBroadcastCount += i.length;

        if(mPowerLevelBroadcastCount > mPowerLevelBroadcastThreshold && i.length >= 10)
        {
            double iSquared, qSquared;
            for(int x = 0; x < 10; x++)
            {
                iSquared = Math.pow(i[x], 2.0f);
                qSquared = Math.pow(q[x], 2.0f);
                mPowerFilter.filter((float)(iSquared + qSquared));
            }

            mPowerLevelBroadcastCount -= mPowerLevelBroadcastThreshold;
            broadcast(SourceEvent.channelPowerLevel(null, 10.0 * Math.log10(mPowerFilter.getValue())));
        }
    }

    /**
     * Registers the listener to receive power level notifications and squelch threshold requests
     */
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventListener = listener;
    }

    /**
     * Broadcasts the source event to an optional register listener
     */
    public void broadcast(SourceEvent event)
    {
        if(mSourceEventListener != null)
        {
            mSourceEventListener.receive(event);
        }
    }
}
