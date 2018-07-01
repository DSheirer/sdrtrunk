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
package io.github.dsheirer.source;

import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.IOverflowListener;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.source.heartbeat.HeartbeatManager;
import io.github.dsheirer.source.heartbeat.IHeartbeatProvider;

/**
 * Abstract class to define the minimum functionality of a sample data provider.
 */
public abstract class Source extends Module implements ISourceEventListener, ISourceEventProvider, IHeartbeatProvider
{
    private HeartbeatManager mHeartbeatManager = new HeartbeatManager();

    /**
     * Heartbeat manager for this source
     */
    public HeartbeatManager getHeartbeatManager()
    {
        return mHeartbeatManager;
    }


    protected IOverflowListener mOverflowListener;

    /**
     * Indicates the type of samples provided by this source: real or complex
     */
    public abstract SampleType getSampleType();

    /**
     * Sample rate provided by this source
     *
     * @throws SourceException if there is an issue determining the sample rate for this source
     */
    public abstract double getSampleRate();

    /**
     * Center frequency for this source in Hertz
     *
     * @throws SourceException if there is an issue in determining the center frequency for this source
     */
    public abstract long getFrequency();

    /**
     * Process any cleanup actions to prepare for garbage collection of this source
     */
    public abstract void dispose();

    /**
     * Registers the listener to receive overflow state changes.  Use null argument to clear the listener
     */
    public void setOverflowListener(IOverflowListener listener)
    {
        mOverflowListener = listener;
    }

    /**
     * Broadcasts an overflow state
     *
     * @param overflow true if overflow, false if normal
     */
    public void broadcastOverflowState(boolean overflow)
    {
        if(mOverflowListener != null)
        {
            mOverflowListener.sourceOverflow(overflow);
        }
    }
}
