package io.github.dsheirer.dsp.filter.interpolator;

import io.github.dsheirer.sample.complex.Complex;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Arrays;

public class RealInterpolator extends Interpolator
{
    private final static Logger mLog = LoggerFactory.getLogger(RealInterpolator.class);

    private float mGain;

    /**
     * Provides an interpolated sample point along an eight-sample waveform representation using 128 filters to
     * provide approximate interpolation with a resolution of 1/128th of a sample.
     *
     * @param gain to apply to the interpolated sample
     */
    public RealInterpolator(float gain)
    {
        mGain = gain;
    }

    /**
     * Calculates an interpolated value from eight samples that start at the offset into the sample array.  The
     * interpolated sample will fall within the middle of the eight sample array, between indexes offset+3 and
     * offset+4.  The mu argument is translated into an index position between 0 and 128, where a mu of 0.0 will be
     * converted to index zero and will be equal to the sample at index offset+3 and a mu of 1.0 will be equal to
     * the sample at offset+4.  All mu values between 0.0 and 1.0 will be converted to a 1 - 127 index and will
     * produce an approximated value from among 127 interpolated sample points between indexes offset+3 and offset+4.
     *
     * @param samples - sample array of length at least offset + 7
     * @param mu - interpolated sample position between 0.0 and 1.0
     * @return - interpolated sample value
     */
    public float filter(float[] samples, int offset, float mu)
    {
        /* Ensure we have enough samples in the array */
        Validate.isTrue(samples.length >= offset + 7);

        /* Identify the filter bank that corresponds to mu */
        int index = (int)(NSTEPS * mu);

        float accumulator = (TAPS[index][7] * samples[offset]);
        accumulator += (TAPS[index][6] * samples[offset + 1]);
        accumulator += (TAPS[index][5] * samples[offset + 2]);
        accumulator += (TAPS[index][4] * samples[offset + 3]);
        accumulator += (TAPS[index][3] * samples[offset + 4]);
        accumulator += (TAPS[index][2] * samples[offset + 5]);
        accumulator += (TAPS[index][1] * samples[offset + 6]);
        accumulator += (TAPS[index][0] * samples[offset + 7]);

        return accumulator * mGain;
    }

    public Complex filter(float[] iSamples, float[] qSamples, int offset, float mu)
    {
        float i = filter(iSamples, offset, mu);
        float q = filter(qSamples, offset, mu);

        return new Complex(i, q);
    }

    public static void main(String[] args)
    {
        RealInterpolator interpolator = new RealInterpolator(1.0f);
        DecimalFormat decimalFormat = new DecimalFormat("0.0000");

        double TWO_PI = Math.PI * 2.0;

		float[] samples = new float[16];

		for(int x = 0; x < 16; x++)
        {
            samples[x] = (float)Math.sin(TWO_PI * (double)x / 8.0);
        }

        mLog.debug("Samples: " + Arrays.toString(samples));

        for(float x = 0.0f; x <= 1.01f; x += 0.1f)
        {
            mLog.debug(decimalFormat.format(x) + ": " + decimalFormat.format(interpolator.filter(samples, 1, x)));
        }

    }
}
