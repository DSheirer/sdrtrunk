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

import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Tuner controller for playback of baseband complex recording files.
 */
public class RecordingTunerController extends TunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(RecordingTunerController.class);

    public static final int DC_NOISE_BANDWIDTH = 0;
    public static final double USABLE_BANDWIDTH_PERCENTAGE = 1.00;
    private ComplexWaveSource mComplexWaveSource;
    private String mPath;
    private long mCenterFrequency;
    private boolean mRunning;

    /**
     * Constructs an instance
     * @param tunerErrorListener to receive errors from this controller
      */
    public RecordingTunerController(ITunerErrorListener tunerErrorListener, String path, long centerFrequency)
    {
        super(tunerErrorListener);
        mPath = path;
        mCenterFrequency = centerFrequency;
        if(mCenterFrequency == 0)
        {
            mCenterFrequency = 100000000;
        }

        setMinimumFrequency(1000000l);
        setMaximumFrequency(3000000000l);
        setMiddleUnusableHalfBandwidth(DC_NOISE_BANDWIDTH);
        setUsableBandwidthPercentage(USABLE_BANDWIDTH_PERCENTAGE);
    }

    @Override
    public void start() throws SourceException
    {
        if(mComplexWaveSource == null)
        {
            try
            {
                mComplexWaveSource = new ComplexWaveSource(new File(mPath), true);
            }
            catch(IOException ioe)
            {
                mLog.error("Error", ioe);
                setErrorMessage(ioe.getMessage() + " File:" + mPath);
                return;
            }

            mComplexWaveSource.setListener(complexSamples -> broadcast(complexSamples));

            try
            {
                mComplexWaveSource.open();
                mComplexWaveSource.start();
                mLog.info("Tuner Recording Loaded: " + mPath);
            }
            catch(IOException | UnsupportedAudioFileException e)
            {
                mLog.error("Error", e);
                setErrorMessage(e.getMessage() + " File:" + mPath);
                return;
            }

            try
            {
                mFrequencyController.setFrequency(mCenterFrequency);
                mFrequencyController.setSampleRate((int)mComplexWaveSource.getSampleRate());
                mFrequencyController.broadcast(SourceEvent.recordingFileLoaded());
            }
            catch(SourceException e)
            {
                mLog.error("Error", e);
                setErrorMessage(e.getMessage());
            }
        }
    }

    @Override
    public void stop()
    {
        if(mComplexWaveSource != null)
        {
            try
            {
                mComplexWaveSource.stop();
                mComplexWaveSource.close();
            }
            catch(IOException ioe)
            {
                mLog.error("Ignoring - error stopping baseband recording playback - " + ioe.getLocalizedMessage());
            }

            mComplexWaveSource = null;
        }
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.RECORDING;
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
