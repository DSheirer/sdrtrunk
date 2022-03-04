package io.github.dsheirer.dsp.window;

/**
 * Base window implementation.
 */
public abstract class Window
{
    protected float[] mCoefficients;

    /**
     * Constructs an instance.
     * @param coefficients coefficients
     */
    public Window(float[] coefficients)
    {
        mCoefficients = coefficients;
    }

    /**
     * Validates that the window coefficients and samples array lengths are the same.
     * @param samples to validate
     */
    protected void validate(float[] samples)
    {
        if(mCoefficients.length != samples.length)
        {
            throw new IllegalArgumentException("Sample array length [" + samples.length +
                    "] must match window coefficients length[" + mCoefficients.length + "]");
        }
    }

    /**
     * Applies this window to the samples
     * @param samples to window
     * @return windowed samples
     */
    public abstract float[] apply(float[] samples);
}
