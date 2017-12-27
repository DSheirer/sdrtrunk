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
package io.github.dsheirer.dsp.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Window factory and corresponding utility methods.
 */
public class Window
{
    private final static Logger mLog = LoggerFactory.getLogger(Window.class);

    private static final double TWO_PI = Math.PI * 2.0d;
    private static final double FOUR_PI = Math.PI * 4.0d;
    private static final double SIX_PI = Math.PI * 6.0d;
    private static final double EIGHT_PI = Math.PI * 8.0d;

    /**
     * Creates a window of the specified type and length.
     *
     * @param type of window
     * @param length of window
     * @return window of the specified type and length
     */
    public static double[] getWindow(WindowType type, int length)
    {
        switch(type)
        {
            case BLACKMAN:
                return getBlackman(length);
            case BLACKMAN_HARRIS_4:
                return getBlackmanHarris4(length);
            case BLACKMAN_HARRIS_7:
                return getBlackmanHarris7(length);
            case BLACKMAN_NUTALL:
                return getBlackmanNutall(length);
            case COSINE:
                return getCosine(length);
            case FLAT_TOP:
                return getFlatTop(length);
            case HAMMING:
                return getHamming(length);
            case HANN:
                return getHann(length);
            case NUTALL:
                return getNutall(length);
            case NONE:
            default:
                return getRectangular(length);
        }
    }

    /**
     * Creates an all-pass or rectangular window.
     *
     * @param length of the window
     * @return rectangular window
     */
    public static double[] getRectangular(int length)
    {
        double[] coefficients = new double[length];

        Arrays.fill(coefficients, 1.0d);

        return coefficients;
    }

    /**
     * Creates a cosine window of the specified length.
     *
     * @param length of the window
     * @return cosine window
     */
    public static double[] getCosine(int length)
    {
        double[] coefficients = new double[length];

        if(length % 2 == 0) //Even length
        {
            int half = (int)((length - 1) / 2);

            for(int x = -half; x < length / 2 + 1; x++)
            {
                coefficients[x + half] = Math.cos(
                    ((double)x * Math.PI) / ((double)length + 1.0d));
            }
        }
        else //Odd length
        {
            int half = (int)length / 2;

            for(int x = -half; x < half + 1; x++)
            {
                coefficients[x + half] = Math.cos(
                    ((double)x * Math.PI) / ((double)length + 1.0d));
            }
        }

        return coefficients;
    }

    /**
     * Creates a 3-term Blackman window.
     *
     * @param length of the window
     * @return Blackman window
     */
    public static double[] getBlackman(int length)
    {
        double[] coefficients = new double[length];

        double denominator = length - 1;

        double a0 = 0.426590713672;
        double a1 = 0.496560619089;
        double a2 = 0.0768486672399;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 -
                (a1 * Math.cos((TWO_PI * (double)x) / denominator)) +
                (a2 * Math.cos((FOUR_PI * (double)x) / denominator));
        }

        return coefficients;
    }

    /**
     * Creates a 4-term Nutall window
     *
     * @param length of the window
     * @return Nutall window
     */
    public static double[] getNutall(int length)
    {
        double[] coefficients = new double[length];

        double denominator = length - 1;
        double a0 = 0.355768;
        double a1 = 0.487396;
        double a2 = 0.144232;
        double a3 = 0.012604;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 -
                (a1 * Math.cos(TWO_PI * (double)x / denominator)) +
                (a2 * Math.cos(FOUR_PI * (double)x / denominator)) -
                (a3 * Math.cos(SIX_PI * (double)x / denominator));
        }

        return coefficients;
    }

    /**
     * Creates a 4-term Blackman-Nutall window
     *
     * @param length of the window
     * @return Blackman-Nutall window
     */
    public static double[] getBlackmanNutall(int length)
    {
        double[] coefficients = new double[length];

        double denominator = length - 1;
        double a0 = 0.3635819;
        double a1 = 0.4891775;
        double a2 = 0.1365995;
        double a3 = 0.0106411;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 -
                (a1 * Math.cos(TWO_PI * (double)x / denominator)) +
                (a2 * Math.cos(FOUR_PI * (double)x / denominator)) -
                (a3 * Math.cos(SIX_PI * (double)x / denominator));
        }

        return coefficients;
    }

    /**
     * Creates a 4-term Blackman-Harris window
     *
     * @param length of the window
     * @return Blackman-Harris window
     */
    public static double[] getBlackmanHarris4(int length)
    {
        double[] coefficients = new double[length];

        double denominator = length - 1;
        double a0 = 0.35875;
        double a1 = 0.48829;
        double a2 = 0.14128;
        double a3 = 0.01168;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 -
                (a1 * Math.cos(TWO_PI * (double)x / denominator)) +
                (a2 * Math.cos(FOUR_PI * (double)x / denominator)) -
                (a3 * Math.cos(SIX_PI * (double)x / denominator));
        }

        return coefficients;
    }

    /**
     * Creates a 7-term Blackman-Harris window
     *
     * @param length of the window
     * @return Blackman-Harris window
     */
    public static double[] getBlackmanHarris7(int length)
    {
        double[] coefficients = new double[length];

        double denominator = length - 1;

        double a0 = 0.27105140069342;
        double a1 = 0.43329793923448;
        double a2 = 0.21812299954311;
        double a3 = 0.06592544638803;
        double a4 = 0.01081174209837;
        double a5 = 0.00077658482522;
        double a6 = 0.00001388721735;


        for(int x = 0; x < length; x++)
        {
            double w = TWO_PI * (double)x / denominator;

            coefficients[x] = a0 -
                (a1 * Math.cos(w)) +
                (a2 * Math.cos(2.0 * w)) -
                (a3 * Math.cos(3.0 * w)) +
                (a4 * Math.cos(4.0 * w)) -
                (a5 * Math.cos(5.0 * w)) +
                (a6 * Math.cos(6.0 * w));
        }

        return coefficients;
    }

    /**
     * Creates a Hamming window
     *
     * @param length of the window
     * @return Hamming window
     */
    public static double[] getHamming(int length)
    {
        double[] coefficients = new double[length];

        double a0 = 0.54;
        double a1 = 0.46;
        double denominator = length;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 - (a1 * Math.cos(TWO_PI * (double)x / denominator));
        }

        return coefficients;
    }

    /**
     * Creates a Hann window
     *
     * @param length of the window
     * @return Hann window
     */
    public static double[] getHann(int length)
    {
        double[] coefficients = new double[length];

        double a0 = 0.5;
        double a1 = 0.5;
        double denominator = length;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 - (a1 * Math.cos(TWO_PI * (double)x / denominator));
        }

        return coefficients;
    }

    /**
     * Creates a 4-term Blackman-Harris window
     *
     * @param length of the window
     * @return Blackman-Harris window
     */
    public static double[] getFlatTop(int length)
    {
        double[] coefficients = new double[length];

        double denominator = length - 1;

        double a0 = 0.215578948;
        double a1 = 0.41663158;
        double a2 = 0.277263158;
        double a3 = 0.083578947;
        double a4 = 0.006947368;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 -
                (a1 * Math.cos(TWO_PI * (double)x / denominator)) +
                (a2 * Math.cos(FOUR_PI * (double)x / denominator)) -
                (a3 * Math.cos(SIX_PI * (double)x / denominator)) +
                (a4 * Math.cos(EIGHT_PI * (double)x / denominator));
        }

        return coefficients;
    }


    private static double getKaiserBeta(double attenuation)
    {
        if(attenuation > 50.0)
        {
            return 0.1102 * (attenuation - 8.7);
        }
        else if(attenuation >= 21.0)
        {
            return (0.5842 * Math.pow(attenuation - 21.0, 0.4)) + (0.07886 * (attenuation - 21.0));
        }
        else
        {
            return 0.0;
        }
    }

    /**
     * Apply the window against an array of float-type samples
     */
    public static float[] apply(double[] coefficients, float[] samples)
    {
        for(int x = 0; x < coefficients.length; x++)
        {
            samples[x] = (float)(samples[x] * coefficients[x]);
        }

        return samples;
    }

    /**
     * Apply the window type against the float array of samples
     *
     * @param type of window to use
     * @param samples to be windowed
     * @return windowed samples
     */
    public static float[] apply(WindowType type, float[] samples)
    {
        double[] coefficients = getWindow(type, samples.length);

        return apply(coefficients, samples);
    }

    /**
     * Apply the window against an array of double-type samples
     */
    public static double[] apply(double[] coefficients, double[] samples)
    {
        for(int x = 0; x < coefficients.length; x++)
        {
            samples[x] = samples[x] * coefficients[x];
        }

        return samples;
    }

    /**
     * Apply the window type against the double array of samples
     *
     * @param type of window to use
     * @param samples to be windowed
     * @return windowed samples
     */
    public static double[] apply(WindowType type, double[] samples)
    {
        double[] coefficients = getWindow(type, samples.length);

        return apply(coefficients, samples);
    }

    /**
     * Window types
     */
    public enum WindowType
    {
        BLACKMAN("Blackman"),
        BLACKMAN_HARRIS_4("Blackman-Harris 4"),
        BLACKMAN_HARRIS_7("Blackman-Harris 7"),
        BLACKMAN_NUTALL("Blackman-Nutall"),
        COSINE("Cosine"),
        FLAT_TOP("Flat Top"),
        HAMMING("Hamming"),
        HANN("Hann"),
        NUTALL("Nutall"),
        NONE("None");

        private String mLabel;

        WindowType(String label)
        {
            mLabel = label;
        }

        public String toString()
        {
            return mLabel;
        }
    }
}
