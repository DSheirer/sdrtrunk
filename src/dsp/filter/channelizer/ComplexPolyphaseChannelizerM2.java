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

import buffer.ReverseFloatCircularBuffer;
import dsp.filter.FilterFactory;
import dsp.filter.Window;
import dsp.mixer.Oscillator;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.complex.ComplexBuffer;

import java.text.DecimalFormat;

public class ComplexPolyphaseChannelizerM2 extends AbstractComplexPolyphaseChannelizer
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexPolyphaseChannelizerM2.class);

    private ReverseFloatCircularBuffer[] mDataBuffers;
    private float[][] mFilterCoefficients;
    private int mFilterPointer = 0;
    private FloatFFT_1D mFFT;

    private DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0000");

    /**
     * Non-Maximally Decimated Polyphase Filter Bank (NMDPFB) channelizer that divides the input frequency band into
     * equal bandwidth channels that are each oversampled by 2x for output.
     *
     * @param taps of a low-pass filter designed for the inbound sample rate with a cutoff frequency
     * equal to the channel bandwidth (sample rate / filters).  If you need to synthesize (combine two or more
     * filter outputs) a new bandwidth signal from the outputs of this filter, then the filter should be designed
     * as a nyquist filter with -6 dB attenuation at the channel bandwidth cutoff frequency
     * @param channels - number of filters/channels to output.  Since this filter bank oversamples each filter
     * output, this number must be even (divisible by the oversample rate).
     * @param channelSampleRate for each decimated, oversampled channel
     */
    public ComplexPolyphaseChannelizerM2(float[] taps, int channels, int channelSampleRate)
    {
        super(channels, channelSampleRate);

        if(channels % 2 != 0)
        {
            throw new IllegalArgumentException("Channel count must be an even multiple of the oversample rate (2)");
        }

        initFilters(taps);
    }

    protected void filter(float inphase, float quadrature)
    {
        try
        {
            mDataBuffers[mFilterPointer--].put(quadrature);
            mDataBuffers[mFilterPointer].put(inphase);

            if(mFilterPointer == 0)
            {
                calculate();
                mFilterPointer = getChannelCount() * 2;
            }
            else if(mFilterPointer == getChannelCount())
            {
                calculate();
            }

            mFilterPointer--;
        }
        catch(Exception e)
        {
            mLog.error("Error!", e);
        }
    }

    private void calculate()
    {
        float[] samples = new float[getChannelCount() * 2];

        if(mFilterPointer == 0)
        {
            for(int x = 0; x < mFilterCoefficients.length; x++)
            {
                int index = x * 2;

                //Inphase sample
                samples[index] = convolve(mFilterCoefficients[x], mDataBuffers[index]);

                //Quadrature sample
                samples[index + 1] = convolve(mFilterCoefficients[x], mDataBuffers[index + 1]);

            }
        }
        else
        {
            int half = mFilterCoefficients.length / 2;

            for(int x = 0; x < half; x++)
            {
                int index = x * 2;
                int filterIndex = x + half;

                //Inphase sample
                samples[index] = convolve(mFilterCoefficients[filterIndex], mDataBuffers[index]);

                //Quadrature sample
                samples[index + 1] = convolve(mFilterCoefficients[filterIndex], mDataBuffers[index + 1]);
            }

            for(int x = half; x < mFilterCoefficients.length; x++)
            {
                int index = x * 2;
                int filterIndex = x - half;

                //Inphase sample
                samples[index] = convolve(mFilterCoefficients[filterIndex], mDataBuffers[index]);

                //Quadrature sample
                samples[index + 1] = convolve(mFilterCoefficients[filterIndex], mDataBuffers[index + 1]);
            }
        }

        //IFFT is executed in-place with the output overwriting the input
        mFFT.complexInverse(samples, true);

        dispatch(samples);
    }

    /**
     * Filters the samples contained in the buffer against the filter coefficients
     *
     * @param coefficients of the sub filter
     * @param buffer containing data samples
     * @return
     */
    private float convolve(float[] coefficients, ReverseFloatCircularBuffer buffer)
    {
        float accumulator = 0.0f;

        for(int x = 0; x < coefficients.length; x++)
        {
            accumulator += coefficients[x] * buffer.get(x);
        }

        return accumulator;
    }

    /**
     * Distributes the filter taps to each of the polyphase filters
     *
     * @param taps
     */
    private void initFilters(float[] taps)
    {
        int tapCount = (int)Math.ceil((double)taps.length / (double)getChannelCount());

        mDataBuffers = new ReverseFloatCircularBuffer[getChannelCount() * 2];

        for(int x = 0; x < getChannelCount() * 2; x++)
        {
            mDataBuffers[x] = new ReverseFloatCircularBuffer(tapCount);
        }

        mFilterCoefficients = new float[getChannelCount()][tapCount];

        for(int tap = 0; tap < tapCount; tap++)
        {
            for(int filter = 0; filter < getChannelCount(); filter++)
            {
                int index = tap * getChannelCount() + filter;

                if(index < taps.length)
                {
                    mFilterCoefficients[filter][tap] = taps[index];
                }
            }
        }

        mFFT = new FloatFFT_1D(getChannelCount());
        mFilterPointer = getChannelCount() - 1; //Start at M/2
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        int symbolRate = 4800;
        int samplesPerSymbol = 2;
        int channels = 800;
        int channelBandwidth = 12500;
        int sampleRate = channels * channelBandwidth;

        float[] taps = FilterFactory.getLowPass(10000000, 12500, 5600, Window.WindowType.BLACKMAN_HARRIS_7);

        int channelCenter = (int)(12500 * 1.5);

        StringBuilder sb = new StringBuilder();
        sb.append("\nPolyphase TunerChannelizer\n");
        sb.append("Sample Rate:" + sampleRate + " Channels:" + channels + " Channel Rate:" + channelBandwidth + "\n");
        sb.append("Tap Count:" + taps.length + "\n");
        sb.append("Channel:" + channelCenter);

        mLog.debug(sb.toString());

        ComplexPolyphaseChannelizerM2 channelizer = new ComplexPolyphaseChannelizerM2(taps, 8, channelBandwidth * 2);

        Oscillator oscillator = new Oscillator(channelCenter, 2 * sampleRate);

        for(int x = 0; x < 2000; x++)
        {
            channelizer.filter(oscillator.getComplex().inphase(), oscillator.getComplex().quadrature());
            oscillator.rotate();
        }

        mLog.debug("Finished!");
    }
}
