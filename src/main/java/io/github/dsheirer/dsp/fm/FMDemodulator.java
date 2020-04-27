/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.dsp.fm;

import io.github.dsheirer.sample.buffer.ComplexBuffer;
import io.github.dsheirer.sample.buffer.FloatBuffer;
import io.github.dsheirer.sample.complex.Complex;
import org.apache.commons.math3.util.FastMath;

/**
 * FM Demodulator for demodulating complex samples and producing demodulated floating point samples.
 */
public class FMDemodulator
{
    private Object mReusableBufferQueue = new Object();
    private float mPreviousI = 0.0f;
    private float mPreviousQ = 0.0f;
    protected float mGain;

    /**
     * Creates an FM demodulator instance with a default gain of 1.0.
     */
    public FMDemodulator()
    {
        this(1.0f);
    }

    /**
     * Creates an FM demodulator instance and applies the gain value to each demodulated output sample.
     * @param gain to apply to demodulated samples.
     */
    public FMDemodulator(float gain)
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

    /**
     * Demodulates the complex baseband sample buffer and returns a demodulated reusable buffer with the user count
     * set to 1.  The complex baseband buffer's user count is decremented after demodulation.
     *
     * @param basebandSampleBuffer containing samples to demodulate
     * @return demodulated sample buffer.
     */
    public FloatBuffer demodulate(ComplexBuffer basebandSampleBuffer)
    {
        FloatBuffer buffer = new FloatBuffer(new float[basebandSampleBuffer.getSampleCount()]);
        FloatBuffer demodulatedBuffer = buffer;

        float[] basebandSamples = basebandSampleBuffer.getSamples();
        float[] demodulatedSamples = demodulatedBuffer.getSamples();

        for(int x = 0; x < basebandSamples.length; x += 2)
        {
            demodulatedSamples[x / 2] = demodulate(basebandSamples[x], basebandSamples[x + 1]);
        }

        return demodulatedBuffer;
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