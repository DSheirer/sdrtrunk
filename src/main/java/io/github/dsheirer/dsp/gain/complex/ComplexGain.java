package io.github.dsheirer.dsp.gain.complex;

import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * Gain control to apply a fixed level of gain to complex samples.
 */
public abstract class ComplexGain
{
    protected float mGain;

    /**
     * Constructs an instance
     * @param gain to apply to complex samples
     */
    public ComplexGain(float gain)
    {
        mGain = gain;
    }

    /**
     * Gain value that will be applied to complex samples
     * @return gain value
     */
    protected float getGain()
    {
        return mGain;
    }

    /**
     * Sets the gain value to apply to the complex samples
     * @param gain value
     */
    protected void setGain(float gain)
    {
        mGain = gain;
    }

    /**
     * Applies a gain value to the complex samples.
     *
     * Note: the gain is applied directly to the buffer, modifying and returning the
     * original sample buffer.
     * @param samples to amplify
     * @return amplified samples
     */
    public ComplexSamples apply(ComplexSamples samples)
    {
        return apply(samples.i(), samples.q());
    }

    /**
     * Applies a gain value to the complex samples.
     *
     * Note: the gain is applied directly to the buffer, modifying and returning the
     * original sample buffer.
     * @param i samples to amplify
     * @param q samples to amplify
     * @return amplified samples
     */
    public abstract ComplexSamples apply(float[] i, float q[]);
}
