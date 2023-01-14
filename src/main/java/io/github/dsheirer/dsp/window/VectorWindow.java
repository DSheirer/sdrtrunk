package io.github.dsheirer.dsp.window;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector SIMD window implementation.
 */
public class VectorWindow extends Window
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;

    /**
     * Constructs an instance.
     *
     * @param coefficients coefficients
     */
    public VectorWindow(float[] coefficients)
    {
        super(coefficients);
    }

    @Override public float[] apply(float[] samples)
    {
        validate(samples);

        int x = 0;

        //Applies the coefficients to the samples in multiples of the SIMD lane width
        for(; x < VECTOR_SPECIES.loopBound(mCoefficients.length); x += VECTOR_SPECIES.length())
        {
            FloatVector.fromArray(VECTOR_SPECIES, samples, x)
                    .mul(FloatVector.fromArray(VECTOR_SPECIES, mCoefficients, x))
                    .intoArray(samples, x);
        }

        //Cleanup loop to calculate the final indices that are not a multiple of the SIMD lane width
        for(; x < mCoefficients.length; x++)
        {
            samples[x] *= mCoefficients[x];
        }

        return samples;
    }
}
