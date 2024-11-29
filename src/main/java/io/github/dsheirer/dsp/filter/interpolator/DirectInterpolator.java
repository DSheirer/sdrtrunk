package io.github.dsheirer.dsp.filter.interpolator;

public class DirectInterpolator
{
    /**
     * Calculates an interpolated value between x1 and x2 at a linear position mu between 0.0 and 1.0
     * @param x1 first value
     * @param x2 second value
     * @param mu offset between first and second values in range 0.0 to 1.0
     * @return interpolated value.
     */
    public static float linear(float x1, float x2, float mu)
    {
        if((x1 > 1.6 && x2 < -1.6) || (x1 < -1.6 && x2 > 1.6))
        {
            return x2;
        }

        return x1 + ((x2 - x1) * mu);
    }
}
