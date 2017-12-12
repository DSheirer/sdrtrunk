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
package dsp.filter.channelizer;

import dsp.filter.FilterFactory;
import dsp.filter.Window;
import dsp.filter.design.FilterDesignException;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.complex.ComplexBuffer;

import java.text.DecimalFormat;

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
            throw new IllegalArgumentException("Channel count must be an even multiple of the oversample rate (2)");
        }

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
        super(sampleRate, (int)(sampleRate / DEFAULT_MINIMUM_CHANNEL_BANDWIDTH));

        float[] filterTaps = FilterFactory.getSincChannelizer(getChannelSampleRate(), getChannelCount(),
            tapsPerFilter, Window.WindowType.BLACKMAN_HARRIS_7, true);

        init(filterTaps);
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

            float[] filterTaps = FilterFactory.getSincChannelizer(getChannelSampleRate(), getChannelCount(),
                15, Window.WindowType.BLACKMAN_HARRIS_7, true);

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
    public void receive(ComplexBuffer complexBuffer)
    {
        float[] samples = complexBuffer.getSamples();

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

            for(int channel = 0; channel < half; channel++)
            {
                int outputIndex = 2 * (getChannelCount() - channel - 1);
                int sampleIndex = 2 * channel;
                int filterIndex = channel + half;

                processed[outputIndex] = filter(sampleIndex, mCoefficients[filterIndex], sampleIndexMap);
                processed[outputIndex + 1] = filter(sampleIndex + 1, mCoefficients[filterIndex], sampleIndexMap);
            }

            //The second half of the sample buffer was loaded using the previous column pointer - adjust for that now
            sampleIndexMap = mSampleIndexMap[(mColumnPointer == (mSamples.length - 1) ? 0 : mColumnPointer + 1)];

            for(int channel = half; channel < getChannelCount(); channel++)
            {
                int outputIndex = 2 * (getChannelCount() - channel - 1);
                int sampleIndex = 2 * channel;
                int filterIndex = channel - half;

                processed[outputIndex] = filter(sampleIndex, mCoefficients[filterIndex], sampleIndexMap);
                processed[outputIndex + 1] = filter(sampleIndex + 1, mCoefficients[filterIndex], sampleIndexMap);
            }
        }

        //Rotate each of the channels to the correct phase using the IFFT
        mFFT.complexInverse(processed, true);

        dispatch(processed);
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

    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        int channelCount = 4;
        int channelBandwidth = 12500;
        int sampleRate = channelCount * channelBandwidth;
        int tapsPerFilter = 14;

        int channelCenter = (int)(12500 * 1.5);

        float[] taps = new float[16];
        for(int x = 0; x < taps.length; x++)
        {
            taps[x] = x;
        }
        float[] samples = new float[32];
        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = x;
        }

        try
        {
//            ComplexPolyphaseChannelizerM2 channelizer = new ComplexPolyphaseChannelizerM2(sampleRate, tapsPerFilter);
            ComplexPolyphaseChannelizerM2 channelizer = new ComplexPolyphaseChannelizerM2(taps,sampleRate, channelCount);

//            Oscillator oscillator = new Oscillator(channelCenter, 2 * sampleRate);
//
//            ComplexBuffer complexBuffer = oscillator.generateComplexBuffer(1000);
            ComplexBuffer complexBuffer = new ComplexBuffer(samples);

            channelizer.receive(complexBuffer);
        }
        catch(Exception e)
        {
            mLog.error("Error", e);
        }

        mLog.debug("Finished!");
    }
}
