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
import dsp.filter.design.FilterDesignException;
import dsp.filter.fir.FIRFilterSpecification;
import dsp.filter.fir.real.RealFIRFilter;
import dsp.filter.fir.remez.RemezFIRFilterDesigner;
import dsp.mixer.Oscillator;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.complex.ComplexBuffer;

import java.text.DecimalFormat;

public class PolyphaseChannelizer implements Listener<ComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelizer.class);

    private RealFIRFilter[] mFilters;
    private int mFilterPointer = 0;
    private int mChannelCount;
    private int mBlockSize;
    private float[] mFilteredSamples;
    private boolean mToggle = false;
    private FloatFFT_1D mFFT;

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
    public PolyphaseChannelizer(float[] taps, int channels)
    {
        assert(channels % 2 == 0);

        mChannelCount = channels;

        initFilters(taps);
    }

    public void filter(float inphase, float quadrature)
    {
        int iIndex = 2 * mFilterPointer;
        int qIndex = iIndex + 1;

        mFilteredSamples[iIndex] = mFilters[iIndex].filter(inphase);
        mFilteredSamples[qIndex] = mFilters[qIndex].filter(quadrature);

        if(mFilterPointer == 0)
        {
            calculate();
            mFilterPointer = mChannelCount - 1;
            mToggle = !mToggle;
        }
        else if(mFilterPointer == mBlockSize)
        {
            calculate();
            mFilterPointer--;
        }
        else
        {
            mFilterPointer--;
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

    private void calculate()
    {
        float[] samples = new float[mFilters.length];

        if(mToggle)
        {
            System.arraycopy(mFilteredSamples, 0, samples, mChannelCount, mChannelCount);
            System.arraycopy(mFilteredSamples, mChannelCount, samples, 0, mChannelCount);
        }
        else
        {
            System.arraycopy(mFilteredSamples, 0, samples, 0, mFilteredSamples.length);
        }

        //FFT is executed in-place where the output overwrites the input
        mFFT.complexForward(samples);

        dispatch(samples);
    }

    private void dispatch(float[] channels)
    {
//        StringBuilder sb = new StringBuilder();
//
//        for(int x = 0; x < channels.length; x += 2)
//        {
//            sb.append((x/2)).append("[")
//                .append(DECIMAL_FORMAT.format(magnitude(channels[x], channels[x + 1], mChannelCount))).append("] ");
//        }
//
//        mLog.debug("Out: " + sb.toString());
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

        float[][] coefficients = new float[mChannelCount][tapCount];

        for(int filter = 0; filter < mChannelCount; filter++)
        {
            for(int tap = 0; tap < tapCount; tap++)
            {
                int index = filter * mChannelCount + tap;

                if(index < taps.length)
                {
                    coefficients[filter][tap] = taps[index];
                }
            }
        }

        mFilters = new RealFIRFilter[mChannelCount * 2];

        for(int filter = 0; filter < mChannelCount; filter++)
        {
            //Inphase filter
            mFilters[2 * filter] = new RealFIRFilter(coefficients[filter], 1.0f);

            //Quadrature filter
            mFilters[2 * filter + 1] = new RealFIRFilter(coefficients[filter], 1.0f);
        }

        mFilteredSamples = new float[mFilters.length];
        mFFT = new FloatFFT_1D(mChannelCount);
        mBlockSize = mChannelCount / 2;
        mFilterPointer = mChannelCount -1;
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        int symbolRate = 4800;
        int samplesPerSymbol = 2;
        int symbolCount = 4;
        int channels = 10;
        int channelBandwidth = 12500;
        int sampleRate = channels * channelBandwidth;

        //Alpha is the residual channel bandwidth left over from the symbol rate and samples per symbol
        float alpha = ((float)channelBandwidth / (float)(symbolRate * samplesPerSymbol)) - 1.0f;

        float[] taps = FilterFactory.getRootRaisedCosine(samplesPerSymbol * channels, symbolCount, alpha);

        int channelCenter = (int)(12500 * 1.5);

        StringBuilder sb = new StringBuilder();
        sb.append("\nPolyphase Channelizer\n");
        sb.append("Sample Rate:" + sampleRate + " Channels:" + channels + " Channel Rate:" + channelBandwidth + "\n");
        sb.append("Alpha: " + alpha + " Tap Count:" + taps.length + "\n");
        sb.append("Channel:" + channelCenter);

        mLog.debug(sb.toString());

        PolyphaseChannelizer channelizer = new PolyphaseChannelizer(taps, 8);

        Oscillator oscillator = new Oscillator(channelCenter, 2 * sampleRate);

        for(int x = 0; x < 2000; x++)
        {
            channelizer.filter(oscillator.getComplex().inphase(), oscillator.getComplex().quadrature());
            oscillator.rotate();
        }

        mLog.debug("Finished!");
    }
}
