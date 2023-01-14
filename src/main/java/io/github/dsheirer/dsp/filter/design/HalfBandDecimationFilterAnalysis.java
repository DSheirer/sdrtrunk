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

package io.github.dsheirer.dsp.filter.design;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.window.WindowType;
import org.apache.commons.math3.util.FastMath;
import org.jtransforms.fft.FloatFFT_1D;

import java.text.DecimalFormat;

/**
 * Develop tables that illustrate Half-Band filter attenutation at various filter lenths and decimation by two scenarios.
 */
public class HalfBandDecimationFilterAnalysis
{
    private static final int FFT_SIZE = 4096;
    private static FloatFFT_1D FFT = new FloatFFT_1D(FFT_SIZE);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");

    private static final WindowType WINDOW_TYPE = WindowType.BLACKMAN;
    private static final int[] TAP_LENGTHS = new int[]{7,11,15,19,23,27,31,35,39,43,47,51,55,59,63,67,71,75,79,83,87,91,95,99,103,107};
    private static final int BY_2 = (int)(FFT_SIZE / 2 * (1.0 / 2.0));
    private static final int BY_2_25 = (int)(FFT_SIZE / 2 * (1.0 / 2.25));
    private static final int BY_2_50 = (int)(FFT_SIZE / 2 * (1.0 / 2.5));
    private static final int BY_2_75 = (int)(FFT_SIZE / 2 * (1.0 / 2.75));
    private static final int BY_3 = (int)(FFT_SIZE / 2 * (1.0 / 3.0));
    private static final int BY_4 = (int)(FFT_SIZE / 2 * (1.0 / 4.0));
    private static final int BY_8 = (int)(FFT_SIZE / 2 * (1.0 / 8.0));
    private static final int BY_16 = (int)(FFT_SIZE / 2 * (1.0 / 16.0));
    private static final int BY_32 = (int)(FFT_SIZE / 2 * (1.0 / 32.0));
    private static final int BY_64 = (int)(FFT_SIZE / 2 * (1.0 / 64.0));
    private static final int BY_128 = (int)(FFT_SIZE / 2 * (1.0 / 128.0));
    private static final int BY_256 = (int)(FFT_SIZE / 2 * (1.0 / 256.0));
    private static final int BY_512 = (int)(FFT_SIZE / 2 * (1.0 / 512.0));
    private static final int BY_1024 = (int)(FFT_SIZE / 2 * (1.0 / 1024.0));

    public HalfBandDecimationFilterAnalysis()
    {
    }

    public static void analyze()
    {
        for(WindowType windowType: WindowType.NO_PARAMETER_WINDOWS)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Window: " + windowType).append("\n");
            sb.append("Length\t\t2x\t\t2.25x\t2.50x\t2.75x\t3x\t\t4x\t\t8x\t\t16x\t\t32x\t\t64x\t\t128x\t256x\t512x\t1024x\n");

            for(int length: TAP_LENGTHS)
            {
                float[] taps = getTaps(length, windowType);
                float[] dft = getDFT(taps);
                float[] decibels = convertDFTToDecibel(dft, FFT_SIZE / 2);
                float x2Cutoff = getCutoff(BY_2, decibels);
                float x225Cutoff = getCutoff(BY_2_25, decibels);
                float x250Cutoff = getCutoff(BY_2_50, decibels);
                float x275Cutoff = getCutoff(BY_2_75, decibels);
                float x3Cutoff = getCutoff(BY_3, decibels);
                float x4Cutoff = getCutoff(BY_4, decibels);
                float x8Cutoff = getCutoff(BY_8, decibels);
                float x16Cutoff = getCutoff(BY_16, decibels);
                float x32Cutoff = getCutoff(BY_32, decibels);
                float x64Cutoff = getCutoff(BY_64, decibels);
                float x128Cutoff = getCutoff(BY_128, decibels);
                float x256Cutoff = getCutoff(BY_256, decibels);
                float x512Cutoff = getCutoff(BY_512, decibels);
                float x1024Cutoff = getCutoff(BY_1024, decibels);

                sb.append(length).append("\t\t\t");
                sb.append(DECIMAL_FORMAT.format(x2Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x225Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x250Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x275Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x3Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x4Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x8Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x16Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x32Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x64Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x128Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x256Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x512Cutoff)).append("\t");
                sb.append(DECIMAL_FORMAT.format(x1024Cutoff)).append("\n");
            }

            System.out.println(sb.toString());
        }
    }

    /**
     * Calculates the maximum cutoff starting at the specified index through the end index.
     * @param index
     * @return
     */
    private static float getCutoff(int index, float[] decibels)
    {
        float cutoff = -200.0f;

        int startIndex = decibels.length - index;

        for(int x = startIndex; x < decibels.length; x++)
        {
            if(decibels[x] > cutoff)
            {
                cutoff = decibels[x];
            }
        }

        return cutoff;
    }

    /**
     * Creates a half-band filter for the specified filter lenth and window type.
     * @param filterLength for the filter
     * @param windowType to apply to the taps during filter creation
     * @return filter
     */
    private static float[] getTaps(int filterLength, WindowType windowType)
    {
        return FilterFactory.getHalfBand(filterLength, windowType);
    }

    private static float[] getDFT(float[] taps)
    {
        float[] dft = new float[FFT_SIZE];
        System.arraycopy(taps, 0, dft, 0, taps.length);
        FFT.realForward(dft);
        return dft;
    }

    public static float[] convertDFTToDecibel(float[] dft, int tapLength)
    {
        float[] decibels = new float[dft.length / 2];

        int index = 0;

        for(int x = 0; x < decibels.length; x++)
        {
            index = x * 2;

            decibels[x] = 20.0f * (float) FastMath.log10(((dft[index] * dft[index]) +
                    (dft[index + 1] * dft[index + 1])));
        }

        return decibels;
    }

    public static void main(String[] args)
    {
        HalfBandDecimationFilterAnalysis.analyze();
    }
}
