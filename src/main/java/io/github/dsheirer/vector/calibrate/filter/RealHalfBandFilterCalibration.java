/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.vector.calibrate.filter;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.List;

/**
 * Real half-band decimation filter calibration for one filter length.
 *
 * Vector implementations can have a different constant delay than the scalar implementation, and a delay of an odd
 * number of input samples selects the other decimation phase, so the output isn't sample-aligned with the scalar
 * output.  Each candidate, including scalar, is instead verified against the full-rate reference convolution at
 * whichever input sample delay fits best.
 */
public abstract class RealHalfBandFilterCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 2048;
    private static final int MAXIMUM_DELAY = 80;

    private final float[] mCoefficients;
    protected final float[] mSamples;

    /**
     * Constructs an instance
     * @param type of calibration
     * @param length of the filter coefficients used for the calibration
     */
    private RealHalfBandFilterCalibration(CalibrationType type, int length)
    {
        super(type);
        mCoefficients = FilterFactory.getHalfBand(length, WindowType.BLACKMAN);
        mSamples = getFloatSamples(BUFFER_SIZE);
    }

    /**
     * Copy of the filter coefficients for constructing a filter instance.
     */
    protected float[] getCoefficients()
    {
        return mCoefficients.clone();
    }

    /**
     * Calibration for the 11-tap filter implementations.
     */
    public static RealHalfBandFilterCalibration tap11()
    {
        return new RealHalfBandFilterCalibration(CalibrationType.FILTER_HALF_BAND_REAL_11_TAP, 11)
        {
            @Override protected List<Candidate> getCandidates()
            {
                //Each candidate is its own lambda so that the filter call is a monomorphic call site
                return List.of(
                    candidate(Implementation.SCALAR, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.SCALAR, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_64, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_64, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_128, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_128, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_256, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_256, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_512, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_512, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }));
            }
        };
    }

    /**
     * Calibration for the 15-tap filter implementations.
     */
    public static RealHalfBandFilterCalibration tap15()
    {
        return new RealHalfBandFilterCalibration(CalibrationType.FILTER_HALF_BAND_REAL_15_TAP, 15)
        {
            @Override protected List<Candidate> getCandidates()
            {
                //Each candidate is its own lambda so that the filter call is a monomorphic call site
                return List.of(
                    candidate(Implementation.SCALAR, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.SCALAR, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_64, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_64, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_128, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_128, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_256, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_256, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_512, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_512, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }));
            }
        };
    }

    /**
     * Calibration for the 23-tap filter implementations.
     */
    public static RealHalfBandFilterCalibration tap23()
    {
        return new RealHalfBandFilterCalibration(CalibrationType.FILTER_HALF_BAND_REAL_23_TAP, 23)
        {
            @Override protected List<Candidate> getCandidates()
            {
                //Each candidate is its own lambda so that the filter call is a monomorphic call site
                return List.of(
                    candidate(Implementation.SCALAR, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.SCALAR, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_64, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_64, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_128, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_128, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_256, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_256, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_512, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_512, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }));
            }
        };
    }

    /**
     * Calibration for the 63-tap filter implementations.
     */
    public static RealHalfBandFilterCalibration tap63()
    {
        return new RealHalfBandFilterCalibration(CalibrationType.FILTER_HALF_BAND_REAL_63_TAP, 63)
        {
            @Override protected List<Candidate> getCandidates()
            {
                //Each candidate is its own lambda so that the filter call is a monomorphic call site
                return List.of(
                    candidate(Implementation.SCALAR, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.SCALAR, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_64, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_64, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_128, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_128, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_256, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_256, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_512, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_512, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }));
            }
        };
    }

    /**
     * Calibration for the default (any length) filter implementations, measured with a 63-tap filter.
     */
    public static RealHalfBandFilterCalibration defaultLength()
    {
        return new RealHalfBandFilterCalibration(CalibrationType.FILTER_HALF_BAND_REAL_DEFAULT, 63)
        {
            @Override protected List<Candidate> getCandidates()
            {
                //Each candidate is its own lambda so that the filter call is a monomorphic call site
                return List.of(
                    candidate(Implementation.SCALAR, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.SCALAR, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_64, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_64, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_128, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_128, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_256, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_256, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }),
                    candidate(Implementation.VECTOR_SIMD_512, () -> {
                        IRealDecimationFilter filter = FilterFactory.getRealDecimationFilter(
                            getType(), Implementation.VECTOR_SIMD_512, getCoefficients());
                        return () -> filter.decimateReal(mSamples);
                    }));
            }
        };
    }

    /**
     * Verifies the candidate output against the reference convolution of the input, at the best fitting delay.
     */
    @Override protected Verification verify(Implementation implementation, float[][] reference, float[][] output)
    {
        float[] decimated = output[0];
        int runs = decimated.length / (BUFFER_SIZE / 2);

        if(output.length != 1 || decimated.length != runs * BUFFER_SIZE / 2 || runs < 2)
        {
            return new Verification(implementation, Double.POSITIVE_INFINITY, getTolerance(),
                "unexpected output length " + decimated.length);
        }

        //Each run processes the same buffer, so the input is the buffer repeated for each run
        float[] input = new float[runs * BUFFER_SIZE];

        for(int run = 0; run < runs; run++)
        {
            System.arraycopy(mSamples, 0, input, run * BUFFER_SIZE, BUFFER_SIZE);
        }

        int bestDelay = 0;
        double bestError = Double.POSITIVE_INFINITY;

        for(int delay = 0; delay <= MAXIMUM_DELAY; delay++)
        {
            double error = error(decimated, input, delay, bestError);

            if(error < bestError)
            {
                bestError = error;
                bestDelay = delay;
            }
        }

        return new Verification(implementation, bestError, getTolerance(), "at input sample delay " + bestDelay);
    }

    /**
     * Maximum error between the decimated output and the reference convolution of the input delayed by the number of
     * input samples.  Skips the first buffer, where the filter is filling.
     * @param limit stops checking once the error exceeds this value
     */
    private double error(float[] decimated, float[] input, int delay, double limit)
    {
        double maxError = 0.0;

        for(int x = BUFFER_SIZE / 2; x < decimated.length && maxError < limit; x++)
        {
            double expected = 0.0;

            for(int k = 0; k < mCoefficients.length; k++)
            {
                int index = 2 * x + k - delay;

                if(index >= 0 && index < input.length)
                {
                    expected += mCoefficients[k] * input[index];
                }
            }

            maxError = Math.max(maxError, error(expected, decimated[x]));
        }

        return maxError;
    }
}
