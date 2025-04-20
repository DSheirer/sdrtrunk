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

package io.github.dsheirer.module.carrier;

import io.github.dsheirer.buffer.FloatAveragingBuffer;
import io.github.dsheirer.dsp.window.WindowFactory;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.jtransforms.fft.FloatFFT_1D;

/**
 * Calculates the carrier offset from channel center.  Processes incoming Complex Sample buffers to detect the carrier
 * signal and calculate a carrier offset measurement.
 * <p>
 * Calculates once a second by performing a 128-point FFT on each consecutive complex buffer.  Once it detects a
 * sequence of 5 consecutive buffers with sufficient SNR (>15 dB), it calculates the average offset across those 5
 * buffers and then averages across the 10 most recent high-SNR sequences.
 */
public class CarrierOffsetProcessor
{
    private static final int FFT_BIN_SIZE = 128;
    private static final int MEASUREMENT_COUNT_THRESHOLD = 5;
    private static final int SEARCH_WINDOW_MIDDLE = FFT_BIN_SIZE / 2;
    private static final long CALCULATION_TIME_INTERVAL_MS = 1000;
    private static final float CENTER_INDEX = FFT_BIN_SIZE / 2.0f - 1;
    private static final float[] WINDOW = WindowFactory.getWindow(WindowType.BLACKMAN_HARRIS_7, FFT_BIN_SIZE * 2);
    private static final FloatFFT_1D FFT = new FloatFFT_1D(FFT_BIN_SIZE);
    private final FloatAveragingBuffer mAveragingBuffer = new FloatAveragingBuffer(5);
    private final FloatAveragingBuffer mOffsetAverage = new FloatAveragingBuffer(10, 3);
    private final StandardDeviation mStandardDeviation = new StandardDeviation();
    private float mCarrierOffset;
    private float mResolution;
    private int mHalfWidth;
    private int mMeasurementCount = 0;
    private int mSearchWindowMin;
    private int mSearchWindowMax;
    private long mEstimatedOffset;
    private long mLastCalculationTimestamp = 0;

    /**
     * Constructs an instance.
     */
    public CarrierOffsetProcessor()
    {
        setSampleRate(50000.0);
    }

    /**
     * Update the current channel sample rate.
     * @param sampleRate in Hertz
     */
    public void setSampleRate(double sampleRate)
    {
        mResolution = (float)sampleRate / FFT_BIN_SIZE;
        mHalfWidth = (int)Math.floor(12500.0 / mResolution / 2.0); //Half of 12.5 kHz signal width
        mSearchWindowMin = SEARCH_WINDOW_MIDDLE - mHalfWidth;
        mSearchWindowMax = SEARCH_WINDOW_MIDDLE + mHalfWidth;
    }

    /**
     * Resets this processor whenever the source tuner PPM value is changed or updated, so that we can immediately
     * recalculate the carrier offset with the updated settings.
     */
    public void reset()
    {
        mMeasurementCount = 0;
        mLastCalculationTimestamp = 0;
        mEstimatedOffset = 0;
        mAveragingBuffer.reset();
        mStandardDeviation.clear();
        mOffsetAverage.reset();
    }

    /**
     * Estimated carrier frequency offset averaged across buffers that have sufficient SNR (>15 dB).
     * @return estimated average carrier offset.  Note: this method can be accessed after each invocation of process()
     * method.
     */
    public long getEstimatedOffset()
    {
        return mEstimatedOffset;
    }

    /**
     * Indicates if a signal was detected in the most recently processed sample buffer.  A value of false indicates the
     * signal was either not present, or the SNR was less than 15 dB and therefore this processor could not calculate
     * the carrier offest estimation.
     * @return true if the most recent buffer had a signal that was also strong enough to process.
     */
    public boolean hasCarrier()
    {
        return mCarrierOffset != Float.MAX_VALUE;
    }

    /**
     * Processes the complex sample buffer by calculating the detected carrier offset and mixing the buffer samples by
     * remove the calculated offset.  The current detected carrier offset is available via the getEstimatedOffset()
     * method.
     *
     * @return true if the estimated carrier offset value was updated.
     */
    public boolean process(ComplexSamples samples)
    {
        if(mLastCalculationTimestamp + CALCULATION_TIME_INTERVAL_MS < samples.timestamp())
        {
            int bufferOffset = 0;

            while((bufferOffset + FFT_BIN_SIZE) <= samples.length())
            {
                mCarrierOffset = calculateCarrierOffset(samples, bufferOffset);

                //Float max value indicates no signal or low SNR - reset the counter so that we can keep trying.
                if(mCarrierOffset == Float.MAX_VALUE)
                {
                    mMeasurementCount = 0;
                    mStandardDeviation.clear();
                }
                else
                {
                    mMeasurementCount++;
                    mAveragingBuffer.add(mCarrierOffset);
                }

                if(mMeasurementCount >= MEASUREMENT_COUNT_THRESHOLD)
                {
                    if(mStandardDeviation.getResult() < 5.0)
                    {
                        mOffsetAverage.add(mAveragingBuffer.getAverage());
                        mLastCalculationTimestamp = samples.timestamp();

                        //Set the buffer offset to the sample length to exit the loop
                        bufferOffset = samples.length();
                    }

                    mMeasurementCount = 0;
                    mStandardDeviation.clear();
                }

                bufferOffset += FFT_BIN_SIZE;
            }

            mEstimatedOffset = (long)mOffsetAverage.getAverage();
            return true;
        }

        return false;
    }

    /**
     * Calculate the detected peak signal offset from the sample buffer.
     * @param complexSamples to process
     * @param offset into the complex samples buffer to calculate
     * @return estimated carrier offset from center of the channel.
     */
    private float calculateCarrierOffset(ComplexSamples complexSamples, int offset)
    {
        float[] samples = complexSamples.toInterleaved(offset, FFT_BIN_SIZE).samples();
        WindowFactory.apply(WINDOW, samples);
        FFT.complexForward(samples);
        float[] magnitudes = ComplexDecibelConverter.convert(samples);
        float peakIndex = findPeak(magnitudes);

        //Return float max value for Low SNR or no signal
        if(peakIndex == Float.MAX_VALUE)
        {
            return Float.MAX_VALUE;
        }

        mStandardDeviation.increment(peakIndex);
        return mResolution * (peakIndex - CENTER_INDEX);
    }

    /**
     * Finds the FFT bin with the largest magnitude within a window of +/- 12.5 kHz of the center bin.
     * @param magnitudes from the FFT measurement.
     * @return center bin or Float.MAX_VALUE to indicate no signal or low SNR.
     */
    private float findPeak(float[] magnitudes)
    {
        float peakValue = -150.0f;
        int peakIndex = 0;
        int min = mSearchWindowMin;
        int max = mSearchWindowMax;
        int half = mHalfWidth;

        //Find the peak magnitude within twice the search window width of the center indices.
        for(int x = min; x <= max; x++)
        {
            if(magnitudes[x] > peakValue)
            {
                peakValue = magnitudes[x];
                peakIndex = x;
            }
        }

        //From there, find the center point that best fits the signal to the search window width.  We need a minimum
        // SNR for this to work.
        int left = peakIndex - half;
        int right = peakIndex + half;

        //Do we have enough SNR?
        float snr = peakValue - ((magnitudes[left] + magnitudes[right]) / 2.0f);

        if(snr > 15.0f)
        {
            float minimumSearchThreshold = peakValue - 16.0f; //Stop at bin values 16 db lower.

            while(magnitudes[left] < minimumSearchThreshold && left < peakIndex)
            {
                left++;
            }

            while(magnitudes[right] < minimumSearchThreshold && right > peakIndex)
            {
                right--;
            }

            int binWidth = right - left;

            if(binWidth == 0)
            {
                return peakIndex;
            }
            else
            {
                return left + (binWidth / 2.0f);
            }
        }

        //No signal or low SNR - return MAX VALUE to indicate no signal.
        return Float.MAX_VALUE;
    }
}
