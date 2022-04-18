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
package io.github.dsheirer.source.tuner.recording;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.UnsupportedAudioFileException;
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
     * @param tunerErrorListener to receive errors from this controller
      */
    public RecordingTunerController(ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        setMinimumFrequency(1000000l);
        setMaximumFrequency(3000000000l);
        setMiddleUnusableHalfBandwidth(DC_NOISE_BANDWIDTH);
        setUsableBandwidthPercentage(USABLE_BANDWIDTH_PERCENTAGE);
    }

    @Override
    public void start() throws SourceException
    {

    }

    @Override
    public void stop()
    {

    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.RECORDING;
    }

    /**
     * Sets the recording file and center frequency for this controller
     * @param recordingPath to play
     * @param centerFrequency of the recording
     * @throws IOException if there are any errors
     */
    private void setRecording(String recordingPath, long centerFrequency) throws IOException
    {
        if(mComplexWaveSource != null)
        {
            mComplexWaveSource.close();
            mComplexWaveSource = null;
        }

        if(recordingPath == null)
        {
            return;
        }

        mComplexWaveSource = new ComplexWaveSource(new File(recordingPath), true);
        mComplexWaveSource.setListener(complexSamples -> broadcast(complexSamples));

        try
        {
            mComplexWaveSource.open();
            mLog.info("Tuner Recording Loaded: " + recordingPath);
        }
        catch(UnsupportedAudioFileException e)
        {
            mLog.error("Unsupported audio format", e);
        }


        mCenterFrequency = centerFrequency;

        mLog.debug("Set recording center frequency to: " + mCenterFrequency);

        if(mCenterFrequency == 0)
        {
            mCenterFrequency = 100000000;
        }

        try
        {
            mFrequencyController.setFrequency(mCenterFrequency);
            mFrequencyController.setSampleRate((int)mComplexWaveSource.getSampleRate());
            mFrequencyController.broadcast(SourceEvent.recordingFileLoaded());
        }
        catch(SourceException e)
        {
            throw new IOException("Can't set frequency or sample rate", e);
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
    public void addBufferListener(Listener<INativeBuffer> listener)
    {
        super.addBufferListener(listener);

        if(mComplexWaveSource != null)
        {
            if(!mRunning)
            {
                mComplexWaveSource.start();
                mRunning = true;
            }
        }
    }

    @Override
    public void removeBufferListener(Listener<INativeBuffer> listener)
    {
        super.removeBufferListener(listener);

        if(!mNativeBufferBroadcaster.hasListeners() && mComplexWaveSource != null)
        {
            mComplexWaveSource.setListener((Listener<INativeBuffer>)null);
            mComplexWaveSource.stop();
            mRunning = false;
        }
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        //Invoke super for frequency, frequency correction and autoPPM
        super.apply(config);

        if(config instanceof RecordingTunerConfiguration rtc)
        {
            mCenterFrequency = rtc.getFrequency();

            try
            {
                setRecording(rtc.getPath(), rtc.getFrequency());
            }
            catch(IOException ioe)
            {
                mLog.debug("Error loading recording tuner baseband recording [" + rtc.getPath() + "] Error: " + ioe.getLocalizedMessage());
            }
        }
    }

    @Override
    public void setFrequency(long frequency) throws SourceException
    {
//        mLog.debug("Set frequency [" + frequency + "] request ignored");
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
        mCenterFrequency = frequency;
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
