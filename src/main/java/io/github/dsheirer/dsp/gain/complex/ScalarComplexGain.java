package io.github.dsheirer.dsp.gain.complex;

import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * Scalar implementation of complex gain
 */
public class ScalarComplexGain extends ComplexGain
{
    /**
     * Constructs an instance
     * @param gain to apply to complex samples
     */
    public ScalarComplexGain(float gain)
    {
        super(gain);
    }

    /**
     * Applies gain to the complex samples buffer
     * @param i samples to amplify
     * @param q samples to amplify
     * @return amplified samples
     */
    @Override public ComplexSamples apply(float[] i, float[] q)
    {
        for(int x = 0; x < i.length; x++)
        {
            i[x] *= mGain;
            q[x] *= mGain;
        }

        return new ComplexSamples(i, q);
    }
}
