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
package io.github.dsheirer.source.mixer;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.adapter.ISampleAdapter;
import io.github.dsheirer.source.RealSource;
import io.github.dsheirer.source.SourceEvent;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;

public class RealMixerSource extends RealSource
{
    private long mFrequency;
    private MixerReader mMixerReader;

    /**
     * Real Mixer Source - constructs a reader on the mixer/sound card target
     * data line using the specified audio format (sample size, sample rate )
     * and broadcasts real buffers (float arrays) to all registered listeners.
     * Reads buffers sized to 10% of the sample rate specified in audio format.
     *
     * @param targetDataLine - mixer or sound card to be used
     * @param format - audio format
     * @param sampleAdapter - adapter to convert byte array data read from the
     * mixer into float array data.
     */
    public RealMixerSource(TargetDataLine targetDataLine, AudioFormat format, ISampleAdapter sampleAdapter)
    {
        mMixerReader = new MixerReader(format, targetDataLine, sampleAdapter, getHeartbeatManager());
    }

    /**
     * Registers a listener to receive source events
     *
     * @param listener
     */
	@Override
	public void setSourceEventListener(Listener<SourceEvent> listener)
	{
	    mMixerReader.setSourceEventListener(listener);
	}

    /**
     * Removes a listener from receiving source events
     */
	@Override
	public void removeSourceEventListener()
	{
	    mMixerReader.removeSourceEventListener();
	}

    /**
     * Current source event listener
     */
	@Override
	public Listener<SourceEvent> getSourceEventListener()
	{
		return mMixerReader.getSourceEventListener();
	}

    @Override
    public void reset()
    {
        stop();
    }

    @Override
    public void start()
    {
        if(!mMixerReader.isRunning())
        {
            mMixerReader.start();
        }
    }

    @Override
    public void stop()
    {
        if(mMixerReader.isRunning())
        {
            mMixerReader.stop();
        }
    }

    /**
     * Sets the listener to receive sample data in reusable buffers.
     */
    public void setListener(Listener<float[]> listener)
    {
        mMixerReader.setBufferListener(listener);
    }

    /**
     * Removes the listener from receiving sample data.
     * @param listener
     */
    public void removeListener(Listener<float[]> listener)
    {
        mMixerReader.removeBufferListener();
    }

    /**
     * Sample rate specified for the underlying target data line.
     */
    @Override
    public double getSampleRate()
    {
        return mMixerReader.getSampleRate();
    }

    /**
     * Returns the frequency of this source.  Default is 0 if the frequency hasn't been set.
     */
    public long getFrequency()
    {
        return mFrequency;
    }

    /**
     * Specify the frequency that will be returned from this source.  This may be useful if you are streaming an
     * external audio source in through the sound card and you want to specify a frequency for that source
     */
    public void setFrequency(long frequency)
    {
        mFrequency = frequency;
    }
}
