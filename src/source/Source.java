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
package source;

import module.Module;
import sample.SampleType;
import sample.real.IOverflowListener;

/**
 * Abstract class to define the minimum functionality of a sample data provider.
 */
public abstract class Source extends Module implements ISourceEventListener, ISourceEventProvider
{
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
    public abstract int getSampleRate() throws SourceException;

    /**
     * Center frequency for this source in Hertz
     *
     * @throws SourceException if there is an issue in determining the center frequency for this source
     */
    public abstract long getFrequency() throws SourceException;

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
    protected void broadcastOverflowState(boolean overflow)
    {
        if(mOverflowListener != null)
        {
            mOverflowListener.sourceOverflow(overflow);
        }
    }
}
