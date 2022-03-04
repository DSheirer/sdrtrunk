package io.github.dsheirer.dsp.window;

/**
 * Scalar window implementation.
 */
public class ScalarWindow extends Window
{
    /**
     * Constructs an instance.
     *
     * @param coefficients coefficients
     */
    public ScalarWindow(float[] coefficients)
    {
        super(coefficients);
    }

    @Override public float[] apply(float[] samples)
    {
        validate(samples);

        for(int x = 0; x < mCoefficients.length; x++)
        {
            samples[x] *= mCoefficients[x];
        }

        return samples;
    }
}
