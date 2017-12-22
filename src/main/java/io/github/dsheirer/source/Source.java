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

import io.github.dsheirer.channel.heartbeat.IHeartbeatProvider;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.sample.real.IOverflowListener;
import io.github.dsheirer.source.tuner.frequency.IFrequencyChangeListener;
import io.github.dsheirer.source.tuner.frequency.IFrequencyChangeProvider;

/**
 * Abstract class to define the minimum functionality of a sample data provider.
 */
public abstract class Source extends Module implements IFrequencyChangeListener, IFrequencyChangeProvider,
    IHeartbeatProvider
{
    protected SampleType mSampleType;
    protected IOverflowListener mOverflowListener;

    public Source( SampleType sampleType )
    {
        mSampleType = sampleType;
    }
    
    public abstract int getSampleRate() throws SourceException;
    
    public abstract long getFrequency() throws SourceException;
    
    public abstract void dispose();
    
    public SampleType getSampleType()
    {
    	return mSampleType;
    }
    
    public void setSampleType( SampleType sampleType )
    {
    	mSampleType = sampleType;
    }

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
     * @param overflow true if overlow, false if normal
     */
    protected void broadcastOverflowState(boolean overflow)
    {
        if(mOverflowListener != null)
        {
            mOverflowListener.sourceOverflow(overflow);
        }
    }
}
