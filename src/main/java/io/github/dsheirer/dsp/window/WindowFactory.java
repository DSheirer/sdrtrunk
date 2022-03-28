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

package io.github.dsheirer.dsp.window;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Window factory and corresponding utility methods.
 */
public class WindowFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(WindowFactory.class);

    private static final float PI = (float)FastMath.PI;
    private static final float TWO_PI = (float)FastMath.PI * 2.0f;
    private static final float FOUR_PI = (float)FastMath.PI * 4.0f;
    private static final float SIX_PI = (float)FastMath.PI * 6.0f;
    private static final float EIGHT_PI = (float)FastMath.PI * 8.0f;

    /**
     * Creates a window of the specified type and length.
     *
     * @param type of window
     * @param length of window
     * @return window of the specified type and length
     */
    public static float[] getWindow(WindowType type, int length)
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
            case KAISER:
                throw new IllegalArgumentException("Kaiser Window cannot be created via this method.  Use the getKaiser() method instead.");
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
    public static float[] getRectangular(int length)
    {
        float[] coefficients = new float[length];

        Arrays.fill(coefficients, 1.0f);

        return coefficients;
    }

    /**
     * Creates a cosine window of the specified length.
     *
     * @param length of the window
     * @return cosine window
     */
    public static float[] getCosine(int length)
    {
        float[] coefficients = new float[length];

        if(length % 2 == 0) //Even length
        {
            int half = (int)((length - 1) / 2);

            for(int x = -half; x < length / 2 + 1; x++)
            {
                coefficients[x + half] = (float)FastMath.cos(
                    ((float)x * PI) / ((float)length + 1.0f));
            }
        }
        else //Odd length
        {
            int half = (int)length / 2;

            for(int x = -half; x < half + 1; x++)
            {
                coefficients[x + half] = (float)FastMath.cos(((float)x * PI) / ((float)length + 1.0f));
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
    public static float[] getBlackman(int length)
    {
        float[] coefficients = new float[length];

        float denominator = length - 1;

        float a0 = 0.426590713672f;
        float a1 = 0.496560619089f;
        float a2 = 0.0768486672399f;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 -
                (a1 * (float)FastMath.cos((TWO_PI * (float)x) / denominator)) +
                (a2 * (float)FastMath.cos((FOUR_PI * (float)x) / denominator));
        }

        return coefficients;
    }

    /**
     * Creates a 4-term Nutall window
     *
     * @param length of the window
     * @return Nutall window
     */
    public static float[] getNutall(int length)
    {
        float[] coefficients = new float[length];

        float denominator = length - 1;
        float a0 = 0.355768f;
        float a1 = 0.487396f;
        float a2 = 0.144232f;
        float a3 = 0.012604f;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 -
                (a1 * (float)FastMath.cos(TWO_PI * (float)x / denominator)) +
                (a2 * (float)FastMath.cos(FOUR_PI * (float)x / denominator)) -
                (a3 * (float)FastMath.cos(SIX_PI * (float)x / denominator));
        }

        return coefficients;
    }

    /**
     * Creates a 4-term Blackman-Nutall window
     *
     * @param length of the window
     * @return Blackman-Nutall window
     */
    public static float[] getBlackmanNutall(int length)
    {
        float[] coefficients = new float[length];

        float denominator = length - 1;
        float a0 = 0.3635819f;
        float a1 = 0.4891775f;
        float a2 = 0.1365995f;
        float a3 = 0.0106411f;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 -
                (a1 * (float)FastMath.cos(TWO_PI * (float)x / denominator)) +
                (a2 * (float)FastMath.cos(FOUR_PI * (float)x / denominator)) -
                (a3 * (float)FastMath.cos(SIX_PI * (float)x / denominator));
        }

        return coefficients;
    }

    /**
     * Creates a 4-term Blackman-Harris window
     *
     * @param length of the window
     * @return Blackman-Harris window
     */
    public static float[] getBlackmanHarris4(int length)
    {
        float[] coefficients = new float[length];

        float denominator = length - 1;
        float a0 = 0.35875f;
        float a1 = 0.48829f;
        float a2 = 0.14128f;
        float a3 = 0.01168f;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 -
                (a1 * (float)FastMath.cos(TWO_PI * (float)x / denominator)) +
                (a2 * (float)FastMath.cos(FOUR_PI * (float)x / denominator)) -
                (a3 * (float)FastMath.cos(SIX_PI * (float)x / denominator));
        }

        return coefficients;
    }

    /**
     * Creates a 7-term Blackman-Harris window
     *
     * @param length of the window
     * @return Blackman-Harris window
     */
    public static float[] getBlackmanHarris7(int length)
    {
        float[] coefficients = new float[length];

        float denominator = length - 1;

        float a0 = 0.27105140069342f;
        float a1 = 0.43329793923448f;
        float a2 = 0.21812299954311f;
        float a3 = 0.06592544638803f;
        float a4 = 0.01081174209837f;
        float a5 = 0.00077658482522f;
        float a6 = 0.00001388721735f;


        for(int x = 0; x < length; x++)
        {
            float w = TWO_PI * (float)x / denominator;

            coefficients[x] = a0 -
                (a1 * (float)FastMath.cos(w)) +
                (a2 * (float)FastMath.cos(2.0 * w)) -
                (a3 * (float)FastMath.cos(3.0 * w)) +
                (a4 * (float)FastMath.cos(4.0 * w)) -
                (a5 * (float)FastMath.cos(5.0 * w)) +
                (a6 * (float)FastMath.cos(6.0 * w));
        }

        return coefficients;
    }

    /**
     * Creates a Hamming window
     *
     * @param length of the window
     * @return Hamming window
     */
    public static float[] getHamming(int length)
    {
        float[] coefficients = new float[length];

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = 0.54f - (0.46f * (float)FastMath.cos((TWO_PI * x) / (length - 1)));
        }

        return coefficients;
    }

    /**
     * Creates a Hann window
     *
     * @param length of the window
     * @return Hann window
     */
    public static float[] getHann(int length)
    {
        float[] coefficients = new float[length];

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = 0.5f - (0.5f * (float)FastMath.cos(TWO_PI * (float)x / (length - 1)));
        }

        return coefficients;
    }

    /**
     * Creates a 4-term Blackman-Harris window
     *
     * @param length of the window
     * @return Blackman-Harris window
     */
    public static float[] getFlatTop(int length)
    {
        float[] coefficients = new float[length];

        float denominator = length - 1;

        float a0 = 0.215578948f;
        float a1 = 0.41663158f;
        float a2 = 0.277263158f;
        float a3 = 0.083578947f;
        float a4 = 0.006947368f;

        for(int x = 0; x < length; x++)
        {
            coefficients[x] = a0 -
                (a1 * (float)FastMath.cos(TWO_PI * (float)x / denominator)) +
                (a2 * (float)FastMath.cos(FOUR_PI * (float)x / denominator)) -
                (a3 * (float)FastMath.cos(SIX_PI * (float)x / denominator)) +
                (a4 * (float)FastMath.cos(EIGHT_PI * (float)x / denominator));
        }

        return coefficients;
    }

    /**
     * Calculates the beta (roll-off factor) for a Kaiser window based on the specified attenuation.
     *
     * @param attenuation in decibels
     * @return beta value in the range of 0.0 to 1.0
     */
    public static float getKaiserBeta(float attenuation)
    {
        if(attenuation > 50.0)
        {
            return 0.1102f * (attenuation - 8.7f);
        }
        else if(attenuation >= 21.0)
        {
            return (0.5842f * (float)FastMath.pow(attenuation - 21.0, 0.4)) + (0.07886f * (attenuation - 21.0f));
        }
        else
        {
            return 0.0f;
        }
    }

    /**
     * Creates a Kaiser window.
     *
     * @param length of the window
     * @param attenuation desired
     * @return window
     */
    public static float[] getKaiser(int length, float attenuation)
    {
        float[] coefficients = new float[length];

        float beta = getKaiserBeta(attenuation);

        float betaBesselZerothOrder = getBesselZerothOrder(beta);

        float temp;

        for(int x = 0; x < coefficients.length; x++)
        {
            temp = beta * (float)FastMath.sqrt(1.0 - FastMath.pow(2.0 * x / (length - 1) - 1.0, 2));
            coefficients[x] = getBesselZerothOrder(temp) / betaBesselZerothOrder;
        }

        return coefficients;
    }


    /**
     * Zeroth order modified Bessel function.
     *
     * @param x Value.
     * @return Return value.
     */
    public final static float getBesselZerothOrder(final float x)
    {
        float f = 1;
        final float x2 = x * x * 0.25f;
        float xc = x2;
        float v = 1 + x2;
        for (int i = 2; i < 100; i++)
        {
            f *= i;
            xc *= x2;
            final float a = xc / (f * f);
            v += a;
            if (a < 1e-20) break;
        }

        return v;
    }

    /**
     * Apply the window against an array of float-type samples
     */
    public static float[] apply(float[] coefficients, float[] samples)
    {
        for(int x = 0; x < coefficients.length; x++)
        {
            samples[x] = samples[x] * coefficients[x];
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
        float[] coefficients = getWindow(type, samples.length);

        return apply(coefficients, samples);
    }

    public static void main(String[] args)
    {
        float[] kaiser = getKaiser(16, 80.0f);

        mLog.debug("Window:" + Arrays.toString(kaiser));
    }
}
