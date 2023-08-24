/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.vector;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.window.WindowType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jtransforms.fft.FloatFFT_1D;

public class DmrChannelEqualizer
{
    private int mBufferPointer;
    private float[] mBuffer = new float[512];
    private float[] mEqualizerTaps = new float[128];
    private float[] mIdealFrequencyDomain = new float[256];
    private FloatFFT_1D mFFT = new FloatFFT_1D(256);

    public DmrChannelEqualizer()
    {
        createIdeal();
        createInitial();
    }

    private void createInitial()
    {
        try
        {
            float[] taps = FilterFactory.getSinc(50000f, 129, WindowType.BLACKMAN_HARRIS_7);

            for(int x = 0; x < 129; x++)
            {
//                System.out.println(taps[x]);
            }

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void process(float value)
    {
        mBuffer[mBufferPointer] = value;
        mBuffer[mBufferPointer + 256] = value;
        mBufferPointer++;
        mBufferPointer %= 256;
    }

    private void calculateSpectrum(float[] samples)
    {
        mFFT.realForward(samples);

        float[] magnitudes = new float[128];
        float i, q;

        StringBuilder sb = new StringBuilder();
        for(int x = 0; x < 128; x++)
        {
            i = samples[2 * x];
            q = samples[2 * x + 1];
            magnitudes[x] = (float)Math.sqrt(i * i + q * q);
            sb.append(magnitudes[x]).append("\n");
        }

        System.out.println("Equalizer: \n" + sb);
    }

    private static float[] getMagnitudes(float[] samples)
    {
        float[] magnitudes = new float[128];
        float i, q;

        for(int x = 0; x < 128; x++)
        {
            i = samples[2 * x];
            q = samples[2 * x + 1];
            magnitudes[x] = (float)Math.sqrt(i * i + q * q);
        }

        return magnitudes;
    }

    public void calculate()
    {
        float[] samples = Arrays.copyOfRange(mBuffer, mBufferPointer, mBufferPointer + 256);

        //Time domain to frequency domain
        mFFT.realForward(samples);
        float[] delta = new float[256];

        for(int x = 0; x < 256; x++)
        {
            delta[x] = mIdealFrequencyDomain[x] - samples[x];
        }

        //Back to time domain for the coefficient adjustments
        mFFT.realInverse(delta, false);

        //Update equalizer taps

        for(int x = 0; x < 256; x++)
        {
            System.out.println(delta[x]);
        }

        calculateSpectrum(delta);
    }

    public void createIdeal()
    {
        float sampleRate = 50000.0f;
        float symbolRate = 4800.0f;
        float samplesPerSymbol = sampleRate / symbolRate;
        Dibit[] bs_data = new Dibit[]{
                Dibit.D11_MINUS_3, Dibit.D01_PLUS_3, //D
                Dibit.D11_MINUS_3, Dibit.D11_MINUS_3, //F
                Dibit.D11_MINUS_3, Dibit.D11_MINUS_3, //F
                Dibit.D01_PLUS_3, Dibit.D01_PLUS_3, //5
                Dibit.D01_PLUS_3, Dibit.D11_MINUS_3, //7
                Dibit.D11_MINUS_3, Dibit.D01_PLUS_3, //D
                Dibit.D01_PLUS_3, Dibit.D11_MINUS_3, //7
                Dibit.D01_PLUS_3, Dibit.D01_PLUS_3, //5
                Dibit.D11_MINUS_3, Dibit.D01_PLUS_3, //D
                Dibit.D11_MINUS_3, Dibit.D11_MINUS_3, //F
                Dibit.D01_PLUS_3, Dibit.D01_PLUS_3, //5
                Dibit.D11_MINUS_3, Dibit.D01_PLUS_3 //D
        };


        float[] coefficients = null;

        try
        {
//            coefficients = FilterFactory.getSinc(0.5, 83, WindowType.BLACKMAN_HARRIS_7);
//            coefficients = FilterFactory.getRootRaisedCosine(samplesPerSymbol, 2, .5f);
            coefficients = FilterFactory.getSinc(sampleRate, 4800, 41, WindowType.BLACKMAN_HARRIS_7);
        }
        catch(Exception e)
        {
            e.printStackTrace();;
        }
        IRealFilter pulseShapingFilter = new RealFIRFilter(coefficients);

        List<Float> ideal = new ArrayList<>();

        float accumulator = 0f;

        float[] plus3 = new float[]{(float)(Math.PI / 4.0 * 3.0)};
        float[] minus3 = new float[]{-(float)(Math.PI / 4.0 * 3.0)};
        float[] zero = new float[]{0f};

        for(Dibit dibit: bs_data)
        {
            while(accumulator < samplesPerSymbol)
            {
                switch(dibit)
                {
                    case D01_PLUS_3:
                        ideal.add(pulseShapingFilter.filter(plus3)[0]);
                        break;
                    case D11_MINUS_3:
                        ideal.add(pulseShapingFilter.filter(minus3)[0]);
                        break;
                }

                accumulator++;
            }

            accumulator -= samplesPerSymbol;
        }

        for(int x = 0; x < coefficients.length / 2 + 4; x++)
        {
            ideal.add(pulseShapingFilter.filter(zero)[0]);
        }

        while(ideal.size() > 256)
        {
            ideal.remove(0);
        }

        for(int x = 0; x < 256; x++)
        {
            mIdealFrequencyDomain[x] = ideal.get(x);
        }
    }

    public static void main(String[] args)
    {
        System.out.println("Starting ...");

        DmrChannelEqualizer equalizer = new DmrChannelEqualizer();
        equalizer.createIdeal();
        System.out.println("Finished!");
    }
}
