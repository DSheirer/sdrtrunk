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
package io.github.dsheirer.dsp.fm;

import io.github.dsheirer.sample.complex.Complex;
import org.apache.commons.math3.util.FastMath;

/**
 * FM Demodulator for demodulating complex samples and producing demodulated floating point samples.
 */
public class ScalarFMDemodulator implements IFmDemodulator
{
    private float mPreviousI = 0.0f;
    private float mPreviousQ = 0.0f;
    protected float mGain;

    /**
     * Creates an FM demodulator instance with a default gain of 1.0.
     */
    public ScalarFMDemodulator()
    {
        this(1.0f);
    }

    /**
     * Creates an FM demodulator instance and applies the gain value to each demodulated output sample.
     * @param gain to apply to demodulated samples.
     */
    public ScalarFMDemodulator(float gain)
    {
        mGain = gain;
    }

    /**
     * Demodulates the I/Q sample via complex sample multiplication by the complex conjugates of the most recently
     * demodulated complex sample (ie previous sample).  Each new sample overwrites the previously stored sample to
     * allow future invocations of the method to simply use a new sample value.
     *
     * @param currentI of the sample
     * @param currentQ of the sample
     * @return demodulated sample
     */
    public float demodulate(float currentI, float currentQ)
    {
        /**
         * Multiply the current sample against the complex conjugate of the
         * previous sample to derive the phase delta between the two samples
         *
         * Negating the previous sample quadrature produces the conjugate
         */
        double inphase = (currentI * mPreviousI) - (currentQ * -mPreviousQ);
        double quadrature = (currentQ * mPreviousI) + (currentI * -mPreviousQ);

        double angle = 0.0f;

        //Check for divide by zero
        if(inphase != 0)
        {
            /**
             * Use the arc-tangent of quadrature divided by inphase to
             * get the phase angle (+/-) which was directly manipulated by the
             * original message waveform during the modulation.  This value now
             * serves as the instantaneous amplitude of the demodulated signal
             */
            double denominator = 1.0d / inphase;
            angle = FastMath.atan((double)quadrature * denominator);
        }

        /**
         * Store the current sample to use during the next iteration
         */
        mPreviousI = currentI;
        mPreviousQ = currentQ;

        return (float)(angle * mGain);
    }

    public float[] demodulate(float[] i, float[] q)
    {
        float[] demodulated = new float[i.length];

        float demodI, demodQ;

        //Demodulate the first sample
        demodI = (i[0] * mPreviousI) - (q[0] * -mPreviousQ);
        demodQ = (q[0] * mPreviousI) + (i[0] * -mPreviousQ);

        //Check for divide by zero
        if(demodI != 0)
        {
            demodulated[0] = (float)FastMath.atan(demodQ / demodI);
        }
        else
        {
            demodulated[0] = (float)FastMath.atan(demodQ / Float.MIN_VALUE);
        }

        //Store last sample to previous for processing with next sample buffer
        mPreviousI = i[i.length - 1];
        mPreviousQ = q[q.length - 1];

        //Demodulate the remainder of the sample array
        for(int x = 1; x < i.length; x++)
        {
            demodI = (i[x] * i[x - 1]) - (q[x] * -q[x - 1]);
            demodQ = (q[x] * i[x - 1]) + (i[x] * -q[x - 1]);

            //Check for divide by zero
            if(demodI != 0)
            {
                demodulated[x] = (float)FastMath.atan(demodQ / demodI);
            }
            else
            {
                demodulated[x] = (float)FastMath.atan(demodQ / Float.MIN_VALUE);
            }
        }

        return demodulated;
    }

    /**
     * Demodulates the complex samples and returns the demodulated value.
     * @param previous
     * @param current
     * @return
     */
    public static double demodulate(Complex previous, Complex current)
    {
        double inphase = (current.inphase() * previous.inphase()) - (current.quadrature() * -previous.quadrature());
        double quadrature = (current.quadrature() * previous.inphase()) + (current.inphase() * -previous.quadrature());

        double angle = 0.0f;

        //Check for divide by zero
        if(inphase != 0)
        {
            /**
             * Use the arc-tangent of quadrature divided by inphase to
             * get the phase angle (+/-) which was directly manipulated by the
             * original message waveform during the modulation.  This value now
             * serves as the instantaneous amplitude of the demodulated signal
             */
            double denominator = 1.0d / inphase;
            angle = FastMath.atan((double)quadrature * denominator);
        }

        return angle;
    }

    public void dispose()
    {
        //no-op
    }

    /**
     * Resets this demodulator by zeroing the stored previous sample.
     */
    public void reset()
    {
        mPreviousI = 0.0f;
        mPreviousQ = 0.0f;
    }

    /**
     * Sets the gain to the specified level.
     */
    public void setGain(float gain)
    {
        mGain = gain;
    }
}