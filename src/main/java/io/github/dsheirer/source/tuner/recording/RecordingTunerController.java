/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.source.tuner.recording;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class RecordingTunerController extends TunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(RecordingTunerController.class);

    public static final int DC_NOISE_BANDWIDTH = 0;
    public static final double USABLE_BANDWIDTH_PERCENTAGE = 1.00;
    private ComplexWaveSource mComplexWaveSource;
    private long mCenterFrequency;
    private boolean mRunning;

    /**
     * Tuner controller testing implementation.
      */
    public RecordingTunerController()
    {
        super(0, 0, DC_NOISE_BANDWIDTH, USABLE_BANDWIDTH_PERCENTAGE);

    }

    /**
     * Sets the recording file and center frequency for this controller
     * @param recording to play
     * @param centerFrequency of the recording
     * @throws IOException if there are any errors
     */
    private void setRecording(File recording, long centerFrequency) throws IOException
    {
        mComplexWaveSource = new ComplexWaveSource(recording, true);
        mCenterFrequency = centerFrequency;

        try
        {
            mFrequencyController.setFrequency(mCenterFrequency);
            mFrequencyController.setSampleRate((int)mComplexWaveSource.getSampleRate());
        }
        catch(SourceException e)
        {
            throw new IOException("Can't set frequency or sample rate");
        }
    }

    @Override
    public int getBufferSampleCount()
    {
        if(mComplexWaveSource != null)
        {
            return mComplexWaveSource.getBufferSampleCount();
        }

        return 0;
    }

    @Override
    public void dispose()
    {
        //no-op
    }

    @Override
    public void addBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        if(mComplexWaveSource != null)
        {
            mComplexWaveSource.setListener(listener);

            if(!mRunning)
            {
                mComplexWaveSource.start();
                mRunning = true;
            }
        }
    }

    @Override
    public void removeBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        if(mComplexWaveSource != null)
        {
            mComplexWaveSource.setListener((Listener<ReusableComplexBuffer>)null);
            mComplexWaveSource.stop();
            mRunning = false;
        }
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        mLog.error("Request to apply tuner configuration was ignored");
    }

    /**
     * Current center frequency for this tuner
     * @throws SourceException
     */
    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mCenterFrequency;
    }

    /**
     * Sets the center frequency for this tuner
     * @param frequency in hertz
     * @throws SourceException
     */
    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        mLog.debug("Request to set frequency [" + frequency + "] ignored");
    }

    /**
     * Current sample rate for this tuner controller
     */
    @Override
    public double getCurrentSampleRate()
    {
        if(mComplexWaveSource != null)
        {
            return mComplexWaveSource.getSampleRate();
        }

        return 0d;
    }
}
