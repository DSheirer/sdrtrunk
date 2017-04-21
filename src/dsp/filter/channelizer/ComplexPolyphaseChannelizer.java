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
import dsp.filter.fir.real.RealFIRFilter;
import dsp.mixer.Oscillator;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexPolyphaseChannelizer extends AbstractComplexPolyphaseChannelizer
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexPolyphaseChannelizer.class);

    private RealFIRFilter[] mInphaseFilters;
    private RealFIRFilter[] mQuadratureFilters;
    private int mFilterPointer = 0;
    private float[] mFilteredSamples;
    private FloatFFT_1D mFFT;

    /**
     * Maximally Decimated Polyphase Filter Bank (MDPFB) channelizer that divides the input frequency band into
     * equal bandwidth channels.
     *
     * @param coefficients of a prototype low-pass filter designed for the inbound sample rate with a cutoff frequency
     * equal to the channel bandwidth (sample rate / filters).  If you need to synthesize (combine two or more
     * filter outputs) a new bandwidth signal from the outputs of this filter, then the filter should be designed
     * as a nyquist filter with -6 dB attenuation at the channel bandwidth cutoff frequency
     *
     * @param channelCount - number of channels to output.
     * @param channelSampleRate - channel sample rate (after decimation)
     */
    public ComplexPolyphaseChannelizer(float[] coefficients, int channelCount, int channelSampleRate)
    {
        super(channelCount, channelSampleRate);

        initFilters(coefficients);
    }

    @Override
    protected void filter(float inphase, float quadrature)
    {
        int index = (getChannelCount() - mFilterPointer - 1) * 2;

        mFilteredSamples[index] = mInphaseFilters[mFilterPointer].filter(inphase);
        mFilteredSamples[index + 1] = mQuadratureFilters[mFilterPointer].filter(quadrature);

        if(mFilterPointer == 0)
        {
            calculate();
            mFilterPointer = getChannelCount() - 1;
        }
        else
        {
            mFilterPointer--;
        }
    }

    private void calculate()
    {
        float[] samples = new float[getChannelCount() * 2];
        System.arraycopy(mFilteredSamples, 0, samples, 0, mFilteredSamples.length);

        //FFT is executed in-place where the output overwrites the input
        mFFT.complexForward(samples);

        dispatch(samples);
    }

    /**
     * Distributes the filter taps to each of the polyphase filters
     *
     * @param taps
     */
    private void initFilters(float[] taps)
    {
        int tapCount = (int)Math.ceil((double)taps.length / (double)getChannelCount());

        float[][] coefficients = new float[getChannelCount()][tapCount];

        for(int tap = 0; tap < tapCount; tap++)
        {
            for(int filter = 0; filter < getChannelCount(); filter++)
            {
                int index = tap * getChannelCount() + filter;

                if(index < taps.length)
                {
                    coefficients[filter][tap] = taps[index];
                }
            }
        }

        mInphaseFilters = new RealFIRFilter[getChannelCount()];
        mQuadratureFilters = new RealFIRFilter[getChannelCount()];

        for(int filter = 0; filter < getChannelCount(); filter++)
        {
            mInphaseFilters[filter] = new RealFIRFilter(coefficients[filter], 1.0f);
            mQuadratureFilters[filter] = new RealFIRFilter(coefficients[filter], 1.0f);
        }

        mFilteredSamples = new float[getChannelCount() * 2];
        mFFT = new FloatFFT_1D(getChannelCount());
        mFilterPointer = getChannelCount() -1;
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        int symbolRate = 4800;
        int samplesPerSymbol = 2;
        int channels = 800;
        int channelBandwidth = 12500;
        int sampleRate = channels * channelBandwidth;

        float[] taps = FilterFactory.getLowPass( 10000000, 12500, 5600, Window.WindowType.BLACKMAN_HARRIS_7 );

        int channelCenter = (int)(12500 * 1.5);

        StringBuilder sb = new StringBuilder();
        sb.append("\nPolyphase TunerChannelizer\n");
        sb.append("Sample Rate:" + sampleRate + " Channels:" + channels + " Channel Rate:" + channelBandwidth + "\n");
        sb.append("Tap Count:" + taps.length + "\n");
        sb.append("Channel:" + channelCenter);

        mLog.debug(sb.toString());

        ComplexPolyphaseChannelizer channelizer = new ComplexPolyphaseChannelizer(taps, 8, channelBandwidth);

        Oscillator oscillator = new Oscillator(channelCenter, 2 * sampleRate);

        for(int x = 0; x < 2000; x++)
        {
            channelizer.filter(oscillator.getComplex().inphase(), oscillator.getComplex().quadrature());
            oscillator.rotate();
        }

        mLog.debug("Finished!");
    }
}
