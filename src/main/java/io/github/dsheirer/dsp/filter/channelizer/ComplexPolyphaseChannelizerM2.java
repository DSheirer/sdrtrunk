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
package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexPolyphaseChannelizerM2 extends AbstractComplexPolyphaseChannelizer
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexPolyphaseChannelizerM2.class);

    private static final int DEFAULT_MINIMUM_CHANNEL_BANDWIDTH = 12500;

    private float[][] mSamples;
    private float[][] mCoefficients;
    private int[][] mSampleIndexMap;
    private int mColumnPointer;
    private int mRowPointer;
    private int mBlockSize;
    private FloatFFT_1D mFFT;
    private PolyphaseChannelResultsBuffer mChannelResultsBuffer;
    private int mChannelResultsBufferSize = 2500;
    private SampleTimestampManager mTimestampManager;

    /**
     * Non-Maximally Decimated Polyphase Filter Bank (NMDPFB) channelizer that divides the input frequency band into
     * equal bandwidth channels that are each oversampled by 2x for output.
     *
     * @param taps of a low-pass filter designed for the inbound sample rate with a cutoff frequency
     * equal to the channel bandwidth (sample rate / filters).  If you need to synthesize (combine two or more
     * filter outputs) a new bandwidth signal from the outputs of this filter, then the filter should be designed
     * as a nyquist filter with -6 dB attenuation at the channel bandwidth cutoff frequency
     *
     * @param sampleRate of the incoming sample stream
     * @param channelCount - number of filters/channels to output.  Since this filter bank oversamples each filter
     * output, this number must be even (divisible by the 2x oversample rate).
     */
    public ComplexPolyphaseChannelizerM2(float[] taps, int sampleRate, int channelCount)
    {
        super(sampleRate, channelCount);

        if(channelCount % 2 != 0)
        {
            throw new IllegalArgumentException("Channel count must be an even multiple of the over-sample rate (2x)");
        }

        double oversampledChannelSampleRate = getChannelSampleRate() * 2.0;
        mTimestampManager = new SampleTimestampManager(oversampledChannelSampleRate);

        init(taps);
    }

    /**
     * Non-Maximally Decimated Polyphase Filter Bank (NMDPFB) channelizer that divides the input frequency band into
     * equal bandwidth channels that are each oversampled by 2x for output.
     *
     * Note: this constructor uses a default minimum channel bandwidth of 12.5 kHz.  The number of channels is calculate
     * from the sample rate and this default minimum channel bandwidth.  The resulting per-channel bandwidth will be
     * between 12.5 and 25.0 kHz with the output per-channel sample rate of 2x the bandwidth.
     *
     * @param sampleRate to be channelized.
     * @param tapsPerFilter to use when designing the filter
     */
    public ComplexPolyphaseChannelizerM2(double sampleRate, int tapsPerFilter) throws FilterDesignException
    {
        super(sampleRate, getChannelCount(sampleRate));

        float[] filterTaps = FilterFactory.getSincM2Channelizer(getChannelSampleRate(), getChannelCount(),
            tapsPerFilter, WindowType.BLACKMAN_HARRIS_7, true);

        double oversampledChannelSampleRate = getChannelSampleRate() * 2.0;
        mTimestampManager = new SampleTimestampManager(oversampledChannelSampleRate);

        init(filterTaps);
    }

    /**
     * Calculates the multiple of two number of channels that can be channelized from the specified sample rate so that
     * each channel has a minimum bandwidth of the default channel bandwidth (12.5 kHz).
     * @param sampleRate to channelize
     * @return number of multiple of two channels that can be channelized.
     */
    private static int getChannelCount(double sampleRate)
    {
        int channels = (int)(sampleRate / DEFAULT_MINIMUM_CHANNEL_BANDWIDTH);

        if(channels % 2 != 0)
        {
            channels--;
        }

        mLog.debug("Sample Rate [" + sampleRate + "] will produce [" + channels + "] channels at [" + (sampleRate / (double)channels) + "] each");
        return channels;
    }

    /**
     * Updates this channelizer to use the new sample rate.  This method creates a new filter suitable for the
     * sample rate and reinitializes all internal data structures to prepare for processing the new sample rate.
     * @param sampleRate in hertz
     */
    @Override
    public void setSampleRate(double sampleRate)
    {
        try
        {
            super.setSampleRate(sampleRate);

            float[] filterTaps = FilterFactory.getSincM2Channelizer(getChannelSampleRate(), getChannelCount(),
                15, WindowType.BLACKMAN_HARRIS_7, true);

            init(filterTaps);
        }
        catch(FilterDesignException fde)
        {
            throw new IllegalArgumentException("Cannot create a channelizer filter for the specified sample rate [" +
                sampleRate + "]");
        }
    }

    /**
     * Receives the complex sample buffer and processes the results through the channelizer.
     */
    @Override
    public void receive(ReusableComplexBuffer reusableComplexBuffer)
    {
        //Use the buffer's reference timestamp to update our timestamp manager (for timestamping output buffers)
        mTimestampManager.setReferenceTimestamp(reusableComplexBuffer.getTimestamp());

        float[] samples = reusableComplexBuffer.getSamples();

        int samplesPointer = 0;

        while(samplesPointer < samples.length)
        {
            int lengthToCopy = mBlockSize - (mRowPointer % mBlockSize);

            if((samplesPointer + lengthToCopy) > samples.length)
            {
                lengthToCopy = samples.length - samplesPointer;
            }

            System.arraycopy(samples, samplesPointer, mSamples[mColumnPointer], mRowPointer, lengthToCopy);
            samplesPointer += lengthToCopy;
            mRowPointer += lengthToCopy;

            if(mRowPointer % mBlockSize == 0)
            {
                process();
            }
        }

        //Decrement the user count to let the originator know we're done with their buffer
        reusableComplexBuffer.decrementUserCount();
    }

    /**
     * Processes the sample buffer for each new block of sample data that is loaded and distributes the results to any
     * registered channel listeners.
     */
    private void process()
    {
        int sampleRowCount = getChannelCount() * 2;

        float[] processed = new float[sampleRowCount];


        //Top of block processing
        if(mRowPointer >= sampleRowCount)
        {
            int[] sampleIndexMap = mSampleIndexMap[mColumnPointer];

            for(int channel = 0; channel < getChannelCount(); channel++)
            {
                int outputIndex = 2 * (getChannelCount() - channel - 1);
                int sampleIndex = 2 * channel;

                float[] filter = mCoefficients[channel];

                processed[outputIndex] = filter((sampleIndex), mCoefficients[channel], sampleIndexMap);
                processed[outputIndex + 1] = filter((sampleIndex + 1), mCoefficients[channel], sampleIndexMap);
            }

            mRowPointer = 0;
            mColumnPointer--;

            if(mColumnPointer < 0)
            {
                mColumnPointer = mSamples.length - 1;
            }
        }
        //Mid-point processing
        else
        {
            int[] sampleIndexMap = mSampleIndexMap[mColumnPointer];

            int half = getChannelCount() / 2;

//            int outputOffset = getChannelCount() - half - 1;
            int outputOffset = getChannelCount() - 1;
            int outputIndex;
            int sampleIndex;
            int filterIndex;

            for(int channel = 0; channel < half; channel++)
            {
                outputIndex = 2 * (outputOffset - channel);
                sampleIndex = 2 * channel;
                filterIndex = channel + half;

                processed[outputIndex] = filter(sampleIndex, mCoefficients[filterIndex], sampleIndexMap);
                processed[outputIndex + 1] = filter(sampleIndex + 1, mCoefficients[filterIndex], sampleIndexMap);
            }

            //The second half of the sample buffer was loaded using the previous column pointer - adjust for that now
            sampleIndexMap = mSampleIndexMap[(mColumnPointer == (mSamples.length - 1) ? 0 : mColumnPointer + 1)];
//            outputOffset = getChannelCount() + half - 1;
            outputOffset = getChannelCount() - 1;

            for(int channel = half; channel < getChannelCount(); channel++)
            {
                outputIndex = 2 * (outputOffset - channel);
                sampleIndex = 2 * channel;
                filterIndex = channel - half;

                processed[outputIndex] = filter(sampleIndex, mCoefficients[filterIndex], sampleIndexMap);
                processed[outputIndex + 1] = filter(sampleIndex + 1, mCoefficients[filterIndex], sampleIndexMap);
            }
        }

        //Rotate each of the channels to the correct phase using the IFFT
        mFFT.complexInverse(processed, true);

        processChannelResults(processed);
    }

    /**
     * Buffers the channel results and dispatches the channel results buffer to the channel output processors when full.
     * @param channelResults
     */
    private void processChannelResults(float[] channelResults)
    {
        if(mChannelResultsBuffer == null)
        {
            mChannelResultsBuffer = new PolyphaseChannelResultsBuffer(mTimestampManager.getCurrentTimestamp(),
                mChannelResultsBufferSize);
        }

        try
        {
            mChannelResultsBuffer.add(channelResults);
        }
        catch(IllegalArgumentException iae)
        {
            //If the buffer is full (unlikely) or the channel results array length has changed (possible), flush the
            //current buffer, create a new one, and store the current results
            flushChannelResultsBuffer();

            mChannelResultsBuffer = new PolyphaseChannelResultsBuffer(mTimestampManager.getCurrentTimestamp(),
                mChannelResultsBufferSize);

            mChannelResultsBuffer.add(channelResults);
        }

        if(mChannelResultsBuffer.isFull())
        {
            flushChannelResultsBuffer();
        }

        //Each channel results array is equivalent to one sample from our timestamp manager's perspective
        mTimestampManager.increment();
    }

    /**
     * Dispatches the non-empty channel results buffer to the channel output processors and nullifies the reference to
     * the buffer so that a new buffer can be created upon receiving the next channel results.
     */
    private void flushChannelResultsBuffer()
    {
        if(mChannelResultsBuffer != null && !mChannelResultsBuffer.isEmpty())
        {
            dispatch(mChannelResultsBuffer);
        }

        mChannelResultsBuffer = null;
    }

    /**
     * Calculates the filtered output for polyphase filter arm.
     *
     * @param sampleRow index pointing to a row in the mSamples array to filter
     * @param coefficients to use in filtering the samples
     * @param sampleIndexMap to translate the column index offset based on the current sample column pointer
     * @return filtered sample for the row
     */
    private float filter(int sampleRow, float[] coefficients, int[] sampleIndexMap)
    {
        float accumulator = 0.0f;

        for(int column = 0; column < mSamples.length; column++)
        {
            accumulator += mSamples[sampleIndexMap[column]][sampleRow] * coefficients[column];
        }

        return accumulator;
    }


    /**
     * Initializes the channelizer structures.  Distributes the prototype filter coefficients to each of the polyphase
     * filters, sets up the sample buffer array and pointers and initializes the FFT.
     *
     * These structures resemble those described by Fred Harris in Multirate Signal Processing for Communications
     * Systems, p230-233.  However, instead of swapping the input sample buffers and swapping the output sample array
     * on every other processing block, we simply swap the filter coefficient banks and leave the input sample buffer
     * and output array indexes aligned.  However, the entire structure (sample buffer, coefficients and output array)
     * is inverted (row zero index is M-1 and M-1 is zero index) to facilitate easy loading of data into the input
     * sample buffer array using Java's efficient System.arrayCopy() utility.
     *
     * Note: the filter coefficients array is organized (row,column) for ease of accessing each filter arm row during
     * convolution.  The samples array is organized (column,row) for ease/efficiency in loading each row of data.  Also,
     * the input sample array implements a circular buffer where the mCoefficientsMap provides a lookup to each sample
     * index based on the current value of the mColumnPointer.
     *
     * @param coefficients of the prototype filter for this channelizer
     */
    private void init(float[] coefficients)
    {
        int columnCount = (int)Math.ceil((double)coefficients.length / (double)getChannelCount());
        int filterRowCount = getChannelCount();
        int sampleRowCount = getChannelCount() * 2;

        //Sample and filter coefficient arrays are specifically setup as column/row and row/column
        mSamples = new float[columnCount][sampleRowCount];
        mCoefficients = new float[filterRowCount][columnCount];

        mColumnPointer = columnCount - 1;
        mBlockSize = getChannelCount();
        mRowPointer = mBlockSize;

        //Setup the FFT
        mFFT = new FloatFFT_1D(getChannelCount());

        //Setup the polyphase filters

        //Setup the index map for the sample array circular buffer
        initIndexMap(columnCount);

        int coefficientIndex = 0;

        //Load filter columns left-2-right (samples are loaded in reverse column order, right-2-left, for easy convolution)
        for(int columnIndex = 0; columnIndex < columnCount; columnIndex++)
        {
            //Load filter rows bottom to top, opposite of sample row loading and sample output ordering
            for(int rowIndex = filterRowCount - 1; rowIndex >= 0; rowIndex--)
            {
                mCoefficients[rowIndex][columnIndex] = coefficients[coefficientIndex++];
            }
        }
    }

    /**
     * Generates a circular buffer index map to support lookup of the translated index based on the current column
     * pointer and the desired sample index.
     */
    private void initIndexMap(int size)
    {
        mSampleIndexMap = new int[size][size];

        for(int row = 0; row < size; row++)
        {
            for(int column = 0;column < size;column++)
            {
                int offset = row + column;

                mSampleIndexMap[row][column] = ((offset < size) ? offset : offset - size);
            }
        }
    }
}
