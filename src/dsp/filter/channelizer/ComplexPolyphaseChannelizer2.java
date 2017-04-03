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
import dsp.mixer.Oscillator;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.complex.ComplexBuffer;

import java.text.DecimalFormat;
import java.util.Arrays;

public class ComplexPolyphaseChannelizer2 implements Listener<ComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexPolyphaseChannelizer2.class);

    private float[][] mFilterCoefficients;


    private float[][] mBuffer;
    private int mBufferRowPointer = 0;
    private int mBufferColumnPointer = 0;
    private int[][] mIndexMap;
    private int mStrideLength;
    private int mStrideCount;

    private int mFilterPointer = 0;
    private int mChannelCount;
    private float[] mFilteredSamples;
    private FloatFFT_1D mFFT;
    private ChannelDistributor mChannelDistributor;

    private DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0000");

    /**
     * Creates a polyphase filter bank that divides the input frequency band into evenly spaced channels that
     * are oversampled by 2x for output.
     *
     * @param taps of a low-pass filter designed for the inbound sample rate with a cutoff frequency
     * equal to the channel bandwidth (sample rate / filters).  If you need to synthesize (combine two or more
     * filte outputs) a new bandwidth signal from the outputs of this filter, then the filter should be designed
     * as a nyquist filter with -6 dB attenuation at the channel bandwidth cutoff frequency
     *
     * @param channels - number of filters/channels to output.  Since this filter bank oversamples each filter
     * output, this number must be even (divisible by the oversample rate).
     */
    public ComplexPolyphaseChannelizer2(float[] taps, int channels)
    {
        if(channels % 2 != 0)
        {
            throw new IllegalArgumentException("Channel count must be a multiple of 2");
        }

        mChannelCount = channels;

        initFilters(taps);
    }

    public void filter(float inphase, float quadrature)
    {
        mBuffer[mBufferRowPointer][mBufferColumnPointer] = quadrature;

        calculate();
        mBufferRowPointer--;

        mBuffer[mBufferRowPointer][mBufferColumnPointer] = inphase;
        calculate();
        mBufferRowPointer--;

        mStrideCount++;

        if(mStrideCount >= mStrideLength)
        {
            dispatch();
            mStrideCount = 0;
        }

        //Adjust the circular buffer pointers, wrapping as necessary
        if(mBufferRowPointer < 0)
        {
            mBufferRowPointer += mBuffer.length;
            mBufferColumnPointer--;

            if(mBufferColumnPointer < 0)
            {
                mBufferColumnPointer += mBuffer[mBufferRowPointer].length;
            }
        }
    }

    @Override
    public void receive(ComplexBuffer complexBuffer)
    {
        float[] samples = complexBuffer.getSamples();

        for(int x = 0; x < samples.length; x += 2)
        {
            filter(samples[x], samples[x + 1]);
        }
    }

    private void dispatch()
    {
        //Create a copy of the filtered samples
        float[] samples = Arrays.copyOf(mFilteredSamples, mFilteredSamples.length);

        //Copy the first half of the samples copy back to the second half of the array to prepare for the next iteration
        System.arraycopy(samples, 0, mFilteredSamples, mChannelCount, mChannelCount);

        //FFT is executed in-place where the output overwrites the input
        mFFT.complexForward(samples);

        dispatch(samples);
    }

    /**
     * Calculates the value of the current data row and the current filter coefficient set and assigns the value to the
     * current filtered samples array
     */
    private void calculate()
    {
        float accumulator = 0.0f;

        float[] coefficients = mFilterCoefficients[mBufferRowPointer / 2];
        float[] samples = mBuffer[mBufferRowPointer];

        for( int x = 0; x < coefficients.length; x++ )
        {
            int column = mIndexMap[mBufferColumnPointer][x];

            accumulator += coefficients[ x ] * samples[column];
        }

        if(mBufferRowPointer < mChannelCount)
        {
            mFilteredSamples[mBufferRowPointer] = accumulator;
        }
        else
        {
            mFilteredSamples[mBufferRowPointer - mChannelCount] = accumulator;
        }
    }

    /**
     * Dispatches the filtered, de-spun samples as an array of channel I/Q sample pairs
     * @param channels arranged as: channel0i, channel0q, channel1i, channel1q ... channelN-1i, channelN-1q
     */
    private void dispatch(float[] channels)
    {
        if(mChannelDistributor != null)
        {
            mChannelDistributor.receive(channels);
        }
    }

    public void setChannelDistributor(ChannelDistributor channelDistributor)
    {
        mChannelDistributor = channelDistributor;
    }

    private double magnitude(float real, float imaginary, int fftSize)
    {
        return Math.sqrt(Math.pow(real, 2.0) + Math.pow(imaginary, 2.0));
    }

    /**
     * Distributes the filter taps to each of the polyphase filters
     *
     * @param taps
     */
    private void initFilters(float[] taps)
    {
        int tapCount = (int)Math.ceil((double)taps.length / (double)mChannelCount);

        mBuffer = new float[mChannelCount * 2][tapCount];
        mFilterCoefficients = new float[mChannelCount][tapCount];

        for(int tap = 0; tap < tapCount; tap++)
        {
            for(int filter = 0; filter < mChannelCount; filter++)
            {
                int index = tap * mChannelCount + filter;

                if(index < taps.length)
                {
                    mFilterCoefficients[filter][tap] = taps[index];
                }
            }
        }

        generateIndexMap(tapCount);

        mFilteredSamples = new float[mChannelCount * 2];
        mFFT = new FloatFFT_1D(mChannelCount);
        mFilterPointer = mChannelCount -1;
        mStrideLength = mChannelCount / 2;  //channel count * 2 / 2
        mStrideCount = 0;
        mBufferRowPointer = mChannelCount - 1;  //Start filling data half way into buffer
        mBufferColumnPointer = mBuffer[mBufferRowPointer].length - 1;
    }

    /**
     * Generates a circular buffer index map to support lookup of the translated
     * index based on the current buffer pointer and the desired sample index.
     */
    private void generateIndexMap( int size )
    {
        mIndexMap = new int[ size ][ size ];

        for( int x = 0; x < size; x++ )
        {
            for( int y = 0; y < size; y++ )
            {
                int z = x + y;

                mIndexMap[ x ][ y ] = z < size ? z : z - size;
            }
        }
    }


    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        int channelBandwidth = 12500;
        int channels = 4;
        int tapsPerChannel = 15;

        float[] taps = null;

        try
        {
            taps = FilterFactory.getSincChannelizer( channelBandwidth, channels, tapsPerChannel,
                Window.WindowType.BLACKMAN_HARRIS_7, true);

        }
        catch(FilterDesignException fde)
        {
            mLog.error("Error in filter design", fde);
        }

        int channelCenter = (int)(channelBandwidth * 1.5);

        StringBuilder sb = new StringBuilder();
        sb.append("\nPolyphase Channelizer\n");
        sb.append("Sample Rate:" + (channelBandwidth * channels) + "\n");
        sb.append("Channels:" + channels + "\n");
        sb.append("Channel Rate:" + channelBandwidth + "\n");
        sb.append("Tap Count:" + taps.length + "\n");
        sb.append("Channel:" + channelCenter + "\n");

        mLog.debug(sb.toString());

        ComplexPolyphaseChannelizer2 channelizer = new ComplexPolyphaseChannelizer2(taps, channels);

        Oscillator oscillator = new Oscillator(channelCenter, 2 * (channelBandwidth * channels));

        for(int x = 0; x < 2000; x++)
        {
            channelizer.filter(oscillator.getComplex().inphase(), oscillator.getComplex().quadrature());
            oscillator.rotate();
        }

        mLog.debug("Finished!");
    }
}
