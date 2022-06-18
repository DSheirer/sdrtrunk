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

package io.github.dsherer.sdrplay.test.listener;

import com.github.dsheirer.sdrplay.callback.IStreamListener;
import com.github.dsheirer.sdrplay.callback.StreamCallbackParameters;
import com.github.dsheirer.sdrplay.device.TunerSelect;
import io.github.dsherer.sdrplay.test.Window;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple stream listener for debug/testing.  Provides logging
 */
public class LoggingStreamListener implements IStreamListener
{
    private static final Logger mLog = LoggerFactory.getLogger(LoggingStreamListener.class);
    private final String mLabel;
    private final TunerSelect mTunerSelect;
    private long mSampleCount = 0;
    private static final int FFT_SIZE = 8192;
    private short[] mISamples = new short[FFT_SIZE];
    private short[] mQSamples = new short[FFT_SIZE];
    private ISampleCountListener mSampleCountListener;

    /**
     * Constructs an instance.
     * @param label to use as a prefix for logging
     * @param tunerSelect to indicate which tuner is producing the samples.
     */
    public LoggingStreamListener(String label, TunerSelect tunerSelect)
    {
        mLabel = label;
        mTunerSelect = tunerSelect;
    }

    @Override
    public TunerSelect getTunerSelect()
    {
        return mTunerSelect;
    }

    /**
     * Registers the listener to be notified of the cumulative sample count received by this stream listener.
     */
    public void setSampleCountListener(ISampleCountListener listener)
    {
        mSampleCountListener = listener;
    }

    public short[] getISamples()
    {
        return mISamples;
    }

    public short[] getQSamples()
    {
        return mQSamples;
    }

    /**
     * Resets the sample counter and the log emission interval counter
     */
    public void reset()
    {
        mSampleCount = 0;
    }

    /**
     * Calculates the estimated sample rate using the sample count and the provided elapsed milliseconds value.
     * @param elapsedMilliseconds value
     */
    public void logSampleRate(long elapsedMilliseconds)
    {
        DecimalFormat df = new DecimalFormat("0.000");

        mLog.info(mLabel + " - " + NumberFormat.getNumberInstance().format(mSampleCount) +
                " samples captured in " + df.format(elapsedMilliseconds / 1E3d) +
                " secs. Approximate Sample Rate:" +
                df.format(mSampleCount / (elapsedMilliseconds / 1E3d) / 1E6d) + " MHz");
    }

    /**
     * Sample count
     */
    public long getSampleCount()
    {
        return mSampleCount;
    }

    @Override
    public void processStream(short[] xi, short[] xq, StreamCallbackParameters parameters,
                              boolean reset)
    {
        //Retain latest set of samples for post run FFT
        System.arraycopy(mISamples, xi.length, mISamples, 0, mISamples.length - xi.length);
        System.arraycopy(xi, 0, mISamples, mISamples.length - xi.length, xi.length);
        System.arraycopy(mQSamples, xq.length, mQSamples, 0, mQSamples.length - xq.length);
        System.arraycopy(xq, 0, mQSamples, mQSamples.length - xq.length, xq.length);

        if(reset || parameters.isGainReductionChanged() || parameters.isRfFrequencyChanged() || parameters.isSampleRateChanged())
        {
            List<String> changes = new ArrayList<>();
            if(reset)
            {
                changes.add("TUNER RESET");
            }
            if(parameters.isGainReductionChanged())
            {
                changes.add("GAIN REDUCTION");
            }
            if(parameters.isRfFrequencyChanged())
            {
                changes.add("RF CENTER FREQUENCY");
            }
            if(parameters.isSampleRateChanged())
            {
                changes.add("SAMPLE RATE");
            }

            mLog.info(mLabel + " - Parameters Changed " + changes);
        }

        mSampleCount += parameters.getNumberSamples();

        if(mSampleCountListener != null)
        {
            mSampleCountListener.sampleCount(mSampleCount);
        }
    }

    public void logSpectrum()
    {
        if(mISamples != null && mQSamples != null)
        {
            float multiplier = 1.0f / Short.MAX_VALUE;

            float[] samples = new float[FFT_SIZE * 2];

            for(int x = 0; x < FFT_SIZE; x ++)
            {
                samples[x * 2] = mISamples[x] * multiplier;
                samples[x * 2 + 1] = mQSamples[x] * multiplier;
            }

            Window.apply(Window.WindowType.BLACKMAN, samples);

            FloatFFT_1D fft = new FloatFFT_1D(FFT_SIZE);
            fft.complexForward(samples);

            double[] db = new double[FFT_SIZE];

            //dB scaling factor for 14-bit sample size
            double scalor = -20.0 * Math.log10(1.0 / Math.pow(2.0, 13));

            for(int x = 0; x < FFT_SIZE; x++)
            {
                double magnitude = Math.pow(samples[x * 2], 2.0) + Math.pow(samples[x * 2 + 1], 2.0);
                db[x] = ((10.0 * FastMath.log10(magnitude) / FFT_SIZE) * scalor * 100) - 72;
            }

            double[] db_arranged = new double[FFT_SIZE];
            System.arraycopy(db, 0, db_arranged, FFT_SIZE / 2, FFT_SIZE / 2);
            System.arraycopy(db, FFT_SIZE / 2, db_arranged, 0, FFT_SIZE / 2);

            StringBuilder sb = new StringBuilder();
            sb.append("\n\n===========================================  " + mLabel +
                    " FFT Spectrum  ===========================================================\n\n");
            for(int x = 0; x < db_arranged.length; x++)
            {
                sb.append(db_arranged[x]).append("\n");
            }
            mLog.info(mLabel + " - Calculated Spectrum (FFT): " + sb);
        }
        else
        {
            mLog.info(mLabel + " either I or Q samples were null ... no fft for you today");
        }
    }
}
