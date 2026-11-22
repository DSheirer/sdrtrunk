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
package io.github.dsheirer.dsp.filter;

import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter128Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter256Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter512Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter64Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilterDefaultBit;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesignerWithLagrange;
import io.github.dsheirer.dsp.filter.halfband.RealHalfBandDecimationFilter;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter11Tap128Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter11Tap256Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter11Tap512Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter11Tap64Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter128Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter15Tap128Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter15Tap256Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter15Tap512Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter15Tap64Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter23Tap128Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter23Tap256Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter23Tap512Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter23Tap64Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter256Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter512Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter63Tap128Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter63Tap256Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter63Tap512Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter63Tap64Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter64Bit;
import io.github.dsheirer.dsp.window.WindowFactory;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.math3.util.FastMath;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for designing various types of filters.
 */
public class FilterFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(FilterFactory.class);
    private static final double PERFECT_RECONSTRUCTION_GAIN_AT_BAND_EDGE = -6.020599842071533; //decibel(0.5, 0.0)
    private static final double MARGIN_OF_ERROR = 0.0003;

    /**
     * Two-sided synthesizer filter cutoff, in channel bandwidths.  The synthesizer filter is centered on the boundary
     * between the two channels, but the desired signal can be located anywhere across that boundary, so the pass band
     * has to extend beyond the +/- 0.5 channel bandwidth centers of the channels.  It can't extend much further because
     * the channelizer transition regions of the outer channel edges alias into the composite beyond about +/- 0.72
     * channel bandwidths from the boundary.
     */
    private static final double SYNTHESIZER_CUTOFF_CHANNEL_BANDWIDTHS = 1.5;
    private static final Map<LowPassSpecification,float[]> REMEZ_LOW_PASS_FILTERS = new ConcurrentHashMap<>();

    /**
     * Generates coefficients for a unity-gain, windowed low-pass filter
     *
     * @param sampleRate - hertz
     * @param frequency - cutoff in hertz
     * @param length - filter length
     * @param window - to apply against the coefficients
     * @return coefficients
     */
    public static float[] getSinc(double sampleRate, long frequency, int length, WindowType window)
    {
        int evenLength = length % 2 == 0 ? length : length + 1;

        //Get unity response array (one element longer to align with IDFT size)
        float[] frequencyResponse = getUnityResponseArray(sampleRate, frequency, evenLength);

        //Apply Inverse DFT against frequency response unity values, leaving the
        //IDFT bin results in the frequency response array
        FloatFFT_1D idft = new FloatFFT_1D(evenLength);
        idft.realInverseFull(frequencyResponse, true);

        //Transfer the IDFT results to the return array
        float[] coefficients = new float[length];
        int middleCoefficient = (int)(length / 2);

        if(length % 2 == 0) //Even length
        {
            //The remaining idft bins from 1 to (middle - 1) are the mirror image
            //coefficients
            for(int x = 0; x < middleCoefficient; x++)
            {
                coefficients[middleCoefficient + x] = frequencyResponse[2 * x];
                coefficients[middleCoefficient - x] = frequencyResponse[2 * x];
            }
        }
        else
        {
            //Bin 0 of the idft is our center coefficient
            coefficients[middleCoefficient] = frequencyResponse[0];

            //The remaining idft bins from 1 to (middle - 1) are the mirror image
            //coefficients
            for(int x = 1; x < middleCoefficient; x++)
            {
                coefficients[middleCoefficient + x] = frequencyResponse[2 * x];
                coefficients[middleCoefficient - x] = frequencyResponse[2 * x];
            }
        }

        //Apply the window against the coefficients
        coefficients = WindowFactory.apply(window, coefficients);

        return coefficients;
    }

    /**
     * Normalizes all filter coefficients to achieve unity (1) gain, by ensuring
     * that the sum of the absolute value of all coefficients adds up to 1.
     *
     * @param coefficients
     * @return
     */
    public static float[] normalize(float[] coefficients)
    {
        float accumulator = 0;

        for (float coefficient : coefficients) {
            accumulator += FastMath.abs(coefficient);
        }

        for(int x = 0; x < coefficients.length; x++)
        {
            coefficients[x] = coefficients[x] / accumulator;
        }

        return coefficients;
    }

    /**
     * Normalizes all filter coefficients to achieve an overall objective gain, by ensuring
     * that the sum of the absolute value of all coefficients adds up to the objective.
     *
     * @param coefficients
     * @return
     */
    public static float[] normalize(float[] coefficients, float objective)
    {
        float accumulator = 0;

        for (float coefficient : coefficients) {
            accumulator += FastMath.abs(coefficient);
        }

        accumulator /= objective;

        for(int x = 0; x < coefficients.length; x++)
        {
            coefficients[x] = coefficients[x] / accumulator;
        }

        return coefficients;
    }

    /**
     * Constructs an array of unity (1) response values representing the
     * desired (pre-windowing) frequency response, used by the sync function
     *
     * Returns an array twice the length, filled with unity (1) response values
     * in the desired pass-band with the positive frequency response starting at
     * the lower end of the array, and the negative frequency response at the
     * higher end of the first half of the array.  The remaining zero-valued
     * indexes in the second half will store the results of the JTransforms
     * inverse DFT operation
     *
     * @param sampleRate
     * @param frequency
     * @param length
     * @return
     */
    public static float[] getUnityResponseArray(double sampleRate, long frequency, int length)
    {
        float[] unityArray = new float[length * 2];


        if(length % 2 == 0) //even length
        {
            int binCount = (int)((FastMath.round((float)frequency / (float)sampleRate * (float)length)));

            for(int x = 0; x < binCount; x++)
            {
                unityArray[x] = 1.0f;
                unityArray[length - 1 - x] = 1.0f;
            }
        }
        else //odd length
        {
            int binCount = (int)((FastMath.round((float)frequency / (float)sampleRate * (float)length + 0.5)));

            unityArray[0] = 1.0f;

            for(int x = 1; x < binCount; x++)
            {
                unityArray[x] = 1.0f;
                unityArray[length - x] = 1.0f;
            }
        }

        return unityArray;
    }

    /**
     * Applies a repeating sequence of 1, -1 to the coefficients to invert
     * the frequency response of the filter.  Used in converting a low-pass
     * filter to a high-pass filter.
     *
     * Inverts the sign of all odd index coefficients ( 1, 3, 5, etc.)
     * returning:
     * Index 0: same
     * Index 1: inverted
     * Index 2: same
     * ...
     * Index length - 1: same
     */
    public static float[] invert(float[] coefficients)
    {
        for(int x = 1; x < coefficients.length; x += 2)
        {
            coefficients[x] = -coefficients[x];
        }

        return coefficients;
    }

    /**
     * Generates filter coefficients for a unity-gain, odd-length, windowed,
     * low pass filter with passband from 0-hertz to the cutoff frequency.
     *
     * @param sampleRate - hertz
     * @param cutoff - frequency in hertz
     * @param filterLength - odd filter length
     * @param windowType - window to apply against the generated coefficients
     * @return
     */
    public static float[] getLowPass(double sampleRate, long cutoff, int filterLength, WindowType windowType)
    {
        return getSinc(sampleRate, cutoff, filterLength, windowType);
    }

    /**
     * Creates a low-pass filter with ~ 0.1 db ripple in the pass band
     *
     * Note: stop frequency - pass frequency defines the transition band.
     */
    /**
     * Creates a low-pass filter with ~0.1 db ripple in the pass band.  The
     * transition region is defined by the stop frequency minus the pass
     * frequency.
     *
     * Requires:
     * - passFrequency < stopFrequency
     * - stopFrequency <= sampleRate/2
     */
    public static float[] getLowPass(double sampleRate, int passFrequency, int stopFrequency, int attenuation,
                                     WindowType windowType, boolean forceOddLength)
    {
        if(stopFrequency < passFrequency || stopFrequency > (sampleRate / 2))
        {
            throw new IllegalArgumentException("FilterFactory - low pass filter pass frequency [" + passFrequency +
                "] must be less than the stop frequency [" + stopFrequency + "] and must be less than half [" +
                (int)(sampleRate / 2) + "] of the sample rate [" + sampleRate + "]");
        }

        int tapCount = getTapCount(sampleRate, passFrequency, stopFrequency, attenuation);

        if(forceOddLength)
        {
            if(tapCount % 2 == 0)
            {
                tapCount--;
            }
        }

        return getLowPass(sampleRate, passFrequency, tapCount, windowType);
    }

    /**
     * Generates filter coefficients for a unity-gain, odd-length, windowed,
     * high pass filter with passband from cutoff frequency to half the sample
     * rate.
     *
     * @param sampleRate - hertz
     * @param cutoff - frequency in hertz
     * @param filterLength - odd filter length
     * @param windowType - window to apply against the generated coefficients
     * @return
     */
    public static float[] getHighPass(int sampleRate, long cutoff, int filterLength, WindowType windowType)
    {
        //Convert the high frequency cutoff to its low frequency cutoff
        //equivalent, so that when we generate the low pass filter, prior to
        //inversion, its at the correct frequency
        long convertedCutoff = sampleRate / 2 - cutoff;

        return invert(getSinc(sampleRate, convertedCutoff, filterLength, windowType));
    }

    public static float[] getHighPass(int sampleRate, long stopFrequency, long passFrequency, int attenuation,
                                      WindowType windowType, boolean forceOddLength)
    {
        /* reverse the stop and pass frequency to get the low pass variant */
        int tapCount = getTapCount(sampleRate, stopFrequency, passFrequency, attenuation);

        if(forceOddLength)
        {
            if(tapCount % 2 == 0)
            {
                tapCount--;
            }
        }

        return invert(getLowPass(sampleRate, stopFrequency, tapCount, windowType));
    }

    /**
     * Utility to log the arrays of doubles with line breaks
     */
    public static String arrayToString(float[] array, boolean breaks)
    {
        StringBuilder sb = new StringBuilder();
        for(int x = 0; x < array.length; x++)
        {
            sb.append(x).append(": ").append(array[x]);

            if(breaks)
            {
                sb.append("\n");
            }
            else
            {
                if(x % 8 == 7)
                {
                    sb.append("\n");
                }
                else
                {
                    sb.append("\t");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Determines the number of fir filter taps required to produce the specified frequency response with passband
     * ripple near 0.1dB.
     *
     * Implements the algorithm from Understanding Digital Signal Processing, 3e, Lyons, section 5.10.5.
     *
     * @param sampleRate in hertz
     * @param pass pass frequency in hertz
     * @param stop stop frequency in hertz
     * @param attenuation in dB
     * @return
     */
    public static int getTapCount(double sampleRate, long pass, long stop, int attenuation)
    {
        double frequency = ((double)stop - (double)pass) / sampleRate;

        return (int)(FastMath.round((double)attenuation / (22.0d * frequency)));
    }

    /**
     * Determines decimation rate(s) for a polyphase decimation filter.
     *
     * @param sampleRate - starting sample rate
     * @param decimatedRate - final (decimated) output rate
     * @return - set of integer decimation rates for a single or multi-stage
     * polyphase filter decimation chain
     * @throws - AssertionException if sample rate is not a multiple of 48 kHz
     */
    public static int[] getPolyphaseDecimationRates(int sampleRate, int decimatedRate, long passFrequency, long stopFrequency)
    {
        int[] rates;

        if(sampleRate % decimatedRate != 0)
        {
            throw new IllegalArgumentException("Decimated rate must be an "
                + "integer multiple of sample rate");
        }

        int decimation = (int)(sampleRate / decimatedRate);

        //Decimation rates below 20 will use single stage polyphase filter
        if(decimation < 20)
        {
            rates = new int[1];
            rates[0] = decimation;
            return rates;
        }
        else
        {
            int optimalStage1 =
                getOptimalStageOneRate(sampleRate, decimation, passFrequency, stopFrequency);

            Set<Integer> factors = getFactors(decimation);

            int stage1 = findClosest(optimalStage1, factors);

            if(stage1 == decimation || stage1 == 1)
            {
                rates = new int[1];
                rates[0] = decimation;
                return rates;
            }
            else
            {
                rates = new int[2];

                int stage2 = (int)(decimation / stage1);

                if(stage1 > stage2)
                {
                    rates[0] = stage1;
                    rates[1] = stage2;
                }
                else
                {
                    rates[0] = stage2;
                    rates[1] = stage1;
                }
                return rates;
            }
        }
    }

    /**
     * Finds the factor that is closest to the desired factor, from an ordered list of factors.
     */
    private static int findClosest(int desiredFactor, Set<Integer> factors)
    {
        int bestFactor = 1;
        int bestDelta = desiredFactor;

        for(Integer factor : factors)
        {
            int testDelta = FastMath.abs(desiredFactor - factor);

            if(testDelta < bestDelta)
            {
                bestDelta = testDelta;
                bestFactor = factor;
            }
        }

        return bestFactor;
    }

    /**
     * Determines the factors that make up an integer.  Uses a brute force method to iterate all integers from 1 to
     * value, determining which factors are evenly divisible into the value.
     *
     * @param value - integer decimation value
     * @return - ordered set of factors for value
     */
    private static Set<Integer> getFactors(int value)
    {
        Set<Integer> factors = new TreeSet<Integer>();

        /* Brute force */
        for(int x = 1; x <= value; x++)
        {
            int remainder = (int)(value / x);

            if(remainder * x == value)
            {
                factors.add(x);
            }
        }

        return factors;
    }

    /**
     * Determines the optimal decimation rate for stage 1 of a two-stage poly-phase decimation filter chain, to produce
     * a final sample rate of 48 kHz using a pass bandwidth of 25 kHz.  Use for total decimation rates of 20 or higher.
     *
     * Implements the algorithm described in Lyons, Understanding Digital Signal Processing, 3e, section 10.2.1, page 511.
     *
     * @param sampleRate
     * @param decimation
     * @param passFrequency frequency of the pass band
     * @return optimum integer decimation rate for the first stage decimation
     * filter
     */
    public static int getOptimalStageOneRate(int sampleRate, int decimation, long passFrequency, long stopFrequency)
    {
        double ratio = getBandwidthRatio(passFrequency, stopFrequency);

        double numerator = 1.0d - (FastMath.sqrt((double)decimation * ratio / (2.0d - ratio)));

        double denominator = 2.0d - (ratio * (decimation + 1.0d));

        int retVal = (int)(2.0d * decimation * (numerator / denominator));

        return retVal;
    }

    /**
     * Determines the F ratio as described in Lyons, Understanding Digital Signal Processing, 3e, section 10.2.1, page 511
     * Used in conjunction with the optimal stage one decimation rate method above.
     */
    private static double getBandwidthRatio(long passFrequency, long stopFrequency)
    {
        assert (passFrequency < stopFrequency);

        return ((double)(stopFrequency - passFrequency) /
            (double)stopFrequency);
    }

    /**
     * Assumes that the pass band is 1/4 of the output sample rate.
     *
     * Assumes the stop band is: pass + (pass * .25).
     *
     * @param outputSampleRate
     * @return
     */
    public static float[] getCICCleanupFilter(int outputSampleRate, int passFrequency, int attenuation, WindowType window)
    {
        int taps = getTapCount(outputSampleRate, passFrequency, passFrequency + 1500,
            attenuation);

        /* Make tap count odd */
        if(taps % 2 == 0)
        {
            taps++;
        }

        float[] frequencyResponse =
            getCICResponseArray(outputSampleRate, passFrequency, taps);

        //Apply Inverse DFT against frequency response unity values, leaving the
        //IDFT bin results in the frequency response array
        FloatFFT_1D idft = new FloatFFT_1D(taps);
        idft.realInverseFull(frequencyResponse, false);

        //Transfer the IDFT results to the odd length return array
        float[] coefficients = new float[taps];
        int middleCoefficient = (int)(taps / 2);

        //Bin 0 of the idft is our center coefficient
        coefficients[middleCoefficient] = frequencyResponse[0];

        //The remaining idft bins from 1 to (middle - 1) are the mirror image
        //coefficients
        for(int x = 1; x <= middleCoefficient; x++)
        {
            coefficients[middleCoefficient + x] = frequencyResponse[2 * x];
            coefficients[middleCoefficient - x] = frequencyResponse[2 * x];
        }

        //Apply the window against the coefficients
//		coefficients = Window.apply( window, coefficients );

        normalize(coefficients);

        return coefficients;
    }

    public static float[] getCICResponseArray(int sampleRate, int frequency, int length)
    {
        float[] cicArray = new float[length * 2];

        int binCount = (int)((FastMath.round(
            (double)frequency / (double)sampleRate * 2.0d * (double)length)));

        cicArray[0] = 1.0f;

        float unityResponse = (float)(FastMath.sin(1.0d / (double)length) /
            (1.0d / (double)length));

        for(int x = 1; x <= binCount; x++)
        {
            /* Calculate unity response plus amplification for droop */
            float compensated = 1.0f + (unityResponse - (float)(FastMath.sin((double)x / (double)length) / ((double)x / (double)length)));

            cicArray[x] = compensated;
            cicArray[length - x] = compensated;
        }

        return cicArray;
    }

    /**
     * Designs an integrate-and-dump filter.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second (4800 for P25 Phase 1)
     * @return filter taps, symmetric, summing to 1.0 (unity DC gain)
     * @throws IllegalArgumentException if either rate is non-positive or the sample rate is below the symbol rate
     */
    public static float[] getIntegrateAndDump(double sampleRate, double symbolRate)
    {
        if(sampleRate <= 0 || symbolRate <= 0)
        {
            throw new IllegalArgumentException("Sample rate and symbol rate must be positive");
        }

        double samplesPerSymbol = sampleRate / symbolRate;

        if(samplesPerSymbol < 1.0)
        {
            throw new IllegalArgumentException("Sample rate must be at least the symbol rate");
        }

        double halfWidth = samplesPerSymbol / 2.0;

        //Candidate length: odd and long enough to span the window plus a partial tap on each edge
        int length = 2 * (int)Math.ceil(halfWidth + 0.5) + 1;
        double center = (length - 1) / 2.0;
        double[] weights = new double[length];

        for(int i = 0; i < length; i++)
        {
            //Each tap represents the unit interval [x - 0.5, x + 0.5] centered on its position
            double x = i - center;
            double low = Math.max(x - 0.5, -halfWidth);
            double high = Math.min(x + 0.5, halfWidth);
            weights[i] = Math.max(0.0, high - low);
        }

        //Trim zero-weight taps from the edges (symmetric, so trim equally)
        final double epsilon = 1e-9;
        int start = 0;
        while(start < length / 2 && weights[start] < epsilon)
        {
            start++;
        }
        int end = length - start;

        //Normalize to unity DC gain: the weights sum to samplesPerSymbol
        float[] taps = new float[end - start];
        for(int i = start; i < end; i++)
        {
            taps[i - start] = (float)(weights[i] / samplesPerSymbol);
        }

        return taps;
    }

    /**
     * Creates a filter from the filter specification using the remez exchange design algorithm
     *
     * @param specification
     * @return filter coefficients
     * @throws FilterDesignException if the filter cannot be designed
     */
    public static float[] getTaps(FIRFilterSpecification specification) throws FilterDesignException
    {
        RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

        if(designer.isValid())
        {
            return designer.getImpulseResponse();
        }

        return null;
    }

    /**
     * Creates a low pass filter using the remez exchange design algorithm with 0.01 dB pass band and stop band ripple.
     * See getRemezLowPass(double, double, double, double, double).
     *
     * @param sampleRate in Hertz
     * @param passBandCutoff in Hertz
     * @param stopBandStart in Hertz
     * @return filter coefficients
     * @throws IllegalStateException if the filter cannot be designed
     */
    public static float[] getRemezLowPass(double sampleRate, double passBandCutoff, double stopBandStart)
    {
        return getRemezLowPass(sampleRate, passBandCutoff, 0.01, stopBandStart, 0.01);
    }

    /**
     * Creates a unity gain low pass filter using the remez exchange design algorithm.  Designed filters are cached and
     * reused, since the design is computationally expensive and decoders request the same filters each time they start.
     *
     * @param sampleRate in Hertz
     * @param passBandCutoff in Hertz
     * @param passBandRipple in decibels
     * @param stopBandStart in Hertz
     * @param stopBandRipple in decibels
     * @return filter coefficients, a copy that the caller can modify (ie RealFIRFilter reverses its coefficients)
     * @throws IllegalStateException if the filter cannot be designed
     */
    public static float[] getRemezLowPass(double sampleRate, double passBandCutoff, double passBandRipple,
                                          double stopBandStart, double stopBandRipple)
    {
        LowPassSpecification key = new LowPassSpecification(sampleRate, passBandCutoff, passBandRipple, stopBandStart,
                stopBandRipple);

        //Filter cache is shared across decoder instances that may be started from different threads
        float[] coefficients = REMEZ_LOW_PASS_FILTERS.get(key);

        if(coefficients == null)
        {
            FIRFilterSpecification specification = FIRFilterSpecification
                    .lowPassBuilder()
                    .sampleRate(sampleRate)
                    .passBandCutoff(passBandCutoff)
                    .passBandAmplitude(1.0).passBandRipple(passBandRipple)
                    .stopBandAmplitude(0.0).stopBandStart(stopBandStart)
                    .stopBandRipple(stopBandRipple).build();

            try
            {
                coefficients = getTaps(specification);

                if(coefficients != null)
                {
                    REMEZ_LOW_PASS_FILTERS.put(key, coefficients);
                }
            }
            catch(Exception e) //FilterDesignException or runtime failures from the Remez designer
            {
                mLog.error("Error designing low pass filter for " + key, e);
            }
        }

        if(coefficients == null)
        {
            throw new IllegalStateException("Unable to design low pass filter for " + key);
        }

        return Arrays.copyOf(coefficients, coefficients.length);
    }

    /**
     * Remez low pass filter design parameters, used as the filter cache key.
     */
    private record LowPassSpecification(double sampleRate, double passBandCutoff, double passBandRipple,
                                        double stopBandStart, double stopBandRipple)
    {
        @Override
        public String toString()
        {
            return "sample rate [" + sampleRate + "] pass band [" + passBandCutoff + "] ripple [" + passBandRipple +
                    "] stop band [" + stopBandStart + "] ripple [" + stopBandRipple + "]";
        }
    }

    /**
     * Designs an inverse sinc (x / sin(x)) filter that compensates for the sinc (integrate and dump) response of a
     * transmitter's shaping filter (ie NXDN).  The response is inverse sinc up to the band edge of the companion root
     * raised cosine filter at (1 + alpha) * symbolRate / 2, and is held at the band edge gain above it.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second
     * @param alpha roll-off factor of the companion RRC filter, defining the band edge (1 + alpha) * symbolRate / 2
     * @param symbolSpan filter length in symbol periods
     * @return filter taps, odd length (minimum 9), linear phase, unity gain at DC
     */
    public static float[] getInverseSincFilter(double sampleRate, double symbolRate, double alpha, int symbolSpan)
    {
        if(sampleRate <= 0 || symbolRate <= 0)
        {
            throw new IllegalArgumentException("Sample rate and symbol rate must be positive");
        }

        if(alpha < 0 || alpha > 1)
        {
            throw new IllegalArgumentException("Alpha must be in the range 0 - 1");
        }

        if(symbolSpan < 1)
        {
            throw new IllegalArgumentException("Symbol span must be at least 1");
        }

        double bandEdge = (1.0 + alpha) * symbolRate / 2.0;

        if(bandEdge >= sampleRate / 2.0)
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate +
                "] is too low for symbol rate [" + symbolRate + "] - band edge exceeds Nyquist");
        }

        int length = Math.max(getOddLength(symbolSpan * sampleRate / symbolRate), 9);
        double symbolPeriod = 1.0 / symbolRate;
        double edgeGain = inverseSinc(bandEdge * symbolPeriod);

        //Least-squares weight applied beyond the band edge, relative to a weight of 1.0 in band.
        double stopBandWeight = 0.01;

        double[] taps = getLeastSquaresSymmetricTaps(sampleRate, length, frequency -> frequency <= bandEdge ?
                new double[]{inverseSinc(frequency * symbolPeriod), 1.0} : new double[]{edgeGain, stopBandWeight});

        return normalizeToUnityDC(taps);
    }

    /**
     * Designs a root raised cosine filter from the closed form impulse response, normalized to unity gain at DC.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second
     * @param alpha roll-off factor (0 - 1]
     * @param symbolSpan filter length in symbol periods
     * @return filter taps, odd length, linear phase
     */
    public static float[] getRootRaisedCosineFilter(double sampleRate, double symbolRate, double alpha, int symbolSpan)
    {
        double[] taps = getRootRaisedCosineTaps(sampleRate, symbolRate, alpha, symbolSpan);
        float[] output = new float[taps.length];

        for(int n = 0; n < taps.length; n++)
        {
            output[n] = (float)taps[n];
        }

        return output;
    }

    /**
     * Designs a receive filter that is a root raised cosine filter convolved with an inverse sinc filter (see
     * getInverseSincFilter()).  Cascaded with a transmit filter that is a root raised cosine and sinc filter (ie NXDN),
     * the overall response is a raised cosine Nyquist filter with no intersymbol interference at the symbol sampling
     * instants.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second
     * @param alpha roll-off factor (0 - 1]
     * @param rrcSymbolSpan root raised cosine filter length in symbol periods
     * @param inverseSincSymbolSpan inverse sinc filter length in symbol periods
     * @return filter taps, odd length (sum of both filter lengths - 1), linear phase, unity gain at DC
     */
    public static float[] getRootRaisedCosineInverseSincFilter(double sampleRate, double symbolRate, double alpha,
                                                               int rrcSymbolSpan, int inverseSincSymbolSpan)
    {
        double[] rrc = getRootRaisedCosineTaps(sampleRate, symbolRate, alpha, rrcSymbolSpan);
        float[] inverseSinc = getInverseSincFilter(sampleRate, symbolRate, alpha, inverseSincSymbolSpan);

        //Full linear convolution - both filters are odd length and symmetric, so the result is too
        double[] combined = new double[rrc.length + inverseSinc.length - 1];

        for(int i = 0; i < rrc.length; i++)
        {
            for(int j = 0; j < inverseSinc.length; j++)
            {
                combined[i + j] += rrc[i] * inverseSinc[j];
            }
        }

        //Normalize to unity gain at DC so the discriminator output keeps its deviation scaling
        return normalizeToUnityDC(combined);
    }

    /**
     * Designs a band limited integrate and dump receive filter for the FM discriminator output (ie P25 Phase 1 C4FM).
     *
     * TIA-102.BAAA specifies a raised cosine (alpha 0.2) Nyquist filter cascaded with an inverse sinc (x / sin(x))
     * shaping filter at the transmitter, and an integrate and dump filter (sinc response) at the receiver, so that the
     * overall response is the raised cosine with no intersymbol interference at the symbol sampling instants.
     *
     * The transmitted signal is band limited to (1 + alpha) * symbolRate / 2, but an integrate and dump filter has
     * slowly decaying sidelobes that pass the FM discriminator noise above the signal band, where the discriminator
     * noise power spectral density rises with the square of frequency.  This produces a linear phase FIR filter with
     * the integrate and dump (sinc) response across the signal band and a stopband above it, which preserves the zero
     * ISI property while rejecting the out of band noise.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second
     * @param alpha roll-off factor of the transmitter's raised cosine filter
     * @param stopBandStart frequency in Hertz where the stopband starts.  The passband extends to the signal band
     * edge of (1 + alpha) * symbolRate / 2 and the region between the passband edge and the stopband start is a
     * transition band that is excluded from the fit.
     * @param symbolSpan filter length in symbol periods
     * @return filter taps, odd length, linear phase, unity gain at DC
     */
    public static float[] getBandLimitedIntegrateAndDump(double sampleRate, double symbolRate, double alpha,
                                                         double stopBandStart, int symbolSpan)
    {
        if(sampleRate <= 0 || symbolRate <= 0)
        {
            throw new IllegalArgumentException("Sample rate and symbol rate must be positive");
        }

        if(symbolSpan < 1)
        {
            throw new IllegalArgumentException("Symbol span must be at least 1");
        }

        double nyquist = sampleRate / 2.0;
        double passBandEdge = (1.0 + alpha) * symbolRate / 2.0;

        if(stopBandStart <= passBandEdge || stopBandStart >= nyquist)
        {
            throw new IllegalArgumentException("Stop band start [" + stopBandStart + "] must be between the pass band " +
                    "edge [" + passBandEdge + "] and Nyquist [" + nyquist + "]");
        }

        int length = getOddLength(symbolSpan * sampleRate / symbolRate);
        double symbolPeriod = 1.0 / symbolRate;

        //Least-squares weight applied in the stopband, relative to a weight of 1.0 in the passband.
        double stopBandWeight = 1.0;

        double[] taps = getLeastSquaresSymmetricTaps(sampleRate, length, frequency -> {
            if(frequency <= passBandEdge)
            {
                return new double[]{sinc(frequency * symbolPeriod), 1.0};
            }
            else if(frequency >= stopBandStart)
            {
                return new double[]{0.0, stopBandWeight};
            }

            return null; //Transition band
        });

        return normalizeToUnityDC(taps);
    }

    /**
     * Designs a one symbol period integrator (moving sum) for the FM discriminator output of a differentially encoded
     * QPSK (ie P25 LSM / CQPSK) signal.  The FM discriminator produces the phase change between consecutive samples,
     * so summing one symbol period of the discriminator output produces the (unwrapped) phase change across the symbol
     * period, which is the differential demodulation of the symbol: arg(x[n] * conj(x[n - samplesPerSymbol])), modulo
     * 2 * PI.
     *
     * A fractional samples per symbol value is handled by weighting the final tap with the fractional remainder, which
     * is equivalent to linearly interpolating the delayed (unwrapped) phase.
     *
     * @param samplesPerSymbol must be at least 1.0
     * @return filter taps scaled to unity gain at DC so the output is in radians per sample, like the C4FM receive
     * filter output.
     */
    public static float[] getDifferentialIntegrator(double samplesPerSymbol)
    {
        if(samplesPerSymbol < 1.0)
        {
            throw new IllegalArgumentException("Samples per symbol [" + samplesPerSymbol + "] must be at least 1.0");
        }

        int integral = (int)Math.floor(samplesPerSymbol);
        double fractional = samplesPerSymbol - integral;
        float[] taps = new float[fractional > 0.0 ? integral + 1 : integral];
        Arrays.fill(taps, 0, integral, (float)(1.0 / samplesPerSymbol));

        if(fractional > 0.0)
        {
            taps[integral] = (float)(fractional / samplesPerSymbol);
        }

        return taps;
    }

    /**
     * Root raised cosine filter taps in double precision, normalized to unity gain at DC.  See
     * getRootRaisedCosineFilter().
     */
    private static double[] getRootRaisedCosineTaps(double sampleRate, double symbolRate, double alpha, int symbolSpan)
    {
        if(sampleRate <= 0 || symbolRate <= 0)
        {
            throw new IllegalArgumentException("Sample rate and symbol rate must be positive");
        }

        if(alpha <= 0 || alpha > 1)
        {
            throw new IllegalArgumentException("Alpha must be in the range (0 - 1]");
        }

        if(symbolSpan < 1)
        {
            throw new IllegalArgumentException("Symbol span must be at least 1");
        }

        int length = getOddLength(symbolSpan * sampleRate / symbolRate);
        int middle = (length - 1) / 2;
        double[] taps = new double[length];
        double sum = 0.0;

        for(int n = 0; n < length; n++)
        {
            //Time in symbol periods relative to the filter center
            double t = (n - middle) * symbolRate / sampleRate;
            double value;

            if(Math.abs(t) < 1e-9)
            {
                value = 1.0 - alpha + 4.0 * alpha / Math.PI;
            }
            else if(Math.abs(Math.abs(t) - 1.0 / (4.0 * alpha)) < 1e-9)
            {
                //Limit at the singularity t = +/- T / (4 * alpha)
                value = (alpha / Math.sqrt(2.0)) *
                    ((1.0 + 2.0 / Math.PI) * Math.sin(Math.PI / (4.0 * alpha)) +
                     (1.0 - 2.0 / Math.PI) * Math.cos(Math.PI / (4.0 * alpha)));
            }
            else
            {
                double numerator = Math.sin(Math.PI * t * (1.0 - alpha)) +
                    4.0 * alpha * t * Math.cos(Math.PI * t * (1.0 + alpha));
                double denominator = Math.PI * t * (1.0 - Math.pow(4.0 * alpha * t, 2.0));
                value = numerator / denominator;
            }

            taps[n] = value;
            sum += value;
        }

        for(int n = 0; n < length; n++)
        {
            taps[n] /= sum;
        }

        return taps;
    }

    /**
     * Odd tap count that is at least the specified length, so the filter has an integer group delay of
     * (length - 1) / 2 samples.
     */
    private static int getOddLength(double length)
    {
        int oddLength = (int)Math.ceil(length);

        if(oddLength % 2 == 0)
        {
            oddLength++;
        }

        return oddLength;
    }

    /**
     * Desired response and least-squares weight for each frequency grid point of getLeastSquaresSymmetricTaps().
     */
    private interface LeastSquaresObjective
    {
        /**
         * @param frequency in Hertz
         * @return {desired response, weight} at the frequency, or null to exclude it from the fit (ie transition band)
         */
        double[] evaluate(double frequency);
    }

    /**
     * Designs a symmetric (linear phase) FIR filter whose response is a weighted least-squares fit to the objective.
     *
     * @param sampleRate in Hertz
     * @param length of the filter, odd
     * @param objective desired response and weight at each frequency
     * @return filter taps, not normalized
     */
    private static double[] getLeastSquaresSymmetricTaps(double sampleRate, int length, LeastSquaresObjective objective)
    {
        double nyquist = sampleRate / 2.0;
        int middle = (length - 1) / 2;
        int coefficientCount = middle + 1;

        //A symmetric filter has the zero-phase response A(f) = c[0] + sum(m=1..middle) c[m] * 2cos(2*pi*f*m/fs)
        //where c[m] = h[middle - m] = h[middle + m]. Accumulate the weighted normal equations (B'WB)c = B'Wd over a
        //dense frequency grid (16 points per coefficient) from DC to Nyquist.
        double[][] normal = new double[coefficientCount][coefficientCount];
        double[] rhs = new double[coefficientCount];
        double[] basis = new double[coefficientCount];
        int gridSize = 16 * coefficientCount;

        for(int g = 0; g <= gridSize; g++)
        {
            double frequency = nyquist * g / gridSize;
            double[] desiredAndWeight = objective.evaluate(frequency);

            if(desiredAndWeight == null)
            {
                continue;
            }

            double desired = desiredAndWeight[0];
            double weight = desiredAndWeight[1];
            basis[0] = 1.0;

            for(int m = 1; m < coefficientCount; m++)
            {
                basis[m] = 2.0 * Math.cos(2.0 * Math.PI * frequency * m / sampleRate);
            }

            for(int i = 0; i < coefficientCount; i++)
            {
                double weighted = weight * basis[i];
                rhs[i] += weighted * desired;

                for(int j = i; j < coefficientCount; j++)
                {
                    normal[i][j] += weighted * basis[j];
                }
            }
        }

        //Fill in the lower triangle of the symmetric normal matrix
        for(int i = 0; i < coefficientCount; i++)
        {
            for(int j = 0; j < i; j++)
            {
                normal[i][j] = normal[j][i];
            }
        }

        double[] coefficients = solve(normal, rhs);

        //Expand to the full symmetric impulse response
        double[] taps = new double[length];

        for(int m = 0; m < coefficientCount; m++)
        {
            taps[middle - m] = coefficients[m];
            taps[middle + m] = coefficients[m];
        }

        return taps;
    }

    /**
     * Normalizes the filter taps to unity gain at DC.
     * @param taps to normalize
     * @return normalized taps
     */
    private static float[] normalizeToUnityDC(double[] taps)
    {
        double sum = 0.0;

        for(double tap : taps)
        {
            sum += tap;
        }

        float[] output = new float[taps.length];

        for(int n = 0; n < taps.length; n++)
        {
            output[n] = (float)(taps[n] / sum);
        }

        return output;
    }

    /**
     * Sinc: sin(x) / x where x = pi * f * T, the frequency response of an integrate and dump filter of length T.
     *
     * @param normalizedFrequency f * T (frequency divided by the symbol rate)
     */
    private static double sinc(double normalizedFrequency)
    {
        if(normalizedFrequency == 0.0)
        {
            return 1.0;
        }

        double x = Math.PI * normalizedFrequency;
        return Math.sin(x) / x;
    }

    /**
     * Inverse sinc: x / sin(x) where x = pi * f * T.
     *
     * @param normalizedFrequency f * T (frequency divided by the symbol rate)
     */
    private static double inverseSinc(double normalizedFrequency)
    {
        if(normalizedFrequency == 0.0)
        {
            return 1.0;
        }

        double x = Math.PI * normalizedFrequency;
        return x / Math.sin(x);
    }

    /**
     * Solves Ax = b using Gaussian elimination with partial pivoting. A and b are modified.
     */
    private static double[] solve(double[][] a, double[] b)
    {
        int n = b.length;

        for(int column = 0; column < n; column++)
        {
            int pivot = column;

            for(int row = column + 1; row < n; row++)
            {
                if(Math.abs(a[row][column]) > Math.abs(a[pivot][column]))
                {
                    pivot = row;
                }
            }

            double[] swapRow = a[column];
            a[column] = a[pivot];
            a[pivot] = swapRow;
            double swapValue = b[column];
            b[column] = b[pivot];
            b[pivot] = swapValue;

            for(int row = column + 1; row < n; row++)
            {
                double factor = a[row][column] / a[column][column];
                b[row] -= factor * b[column];

                for(int k = column; k < n; k++)
                {
                    a[row][k] -= factor * a[column][k];
                }
            }
        }

        double[] x = new double[n];

        for(int row = n - 1; row >= 0; row--)
        {
            double sum = b[row];

            for(int k = row + 1; k < n; k++)
            {
                sum -= a[row][k] * x[k];
            }

            x[row] = sum / a[row][row];
        }

        return x;
    }

    /**
     * Evaluates the filter to determine the magnitude response (dB) at the specified frequency.
     *
     * @param filter to evaluate
     * @param frequency to calculate (0 <> 0.5)
     * @return magnitude frequency response in decibels
     */
    public static double evaluate(float[] filter, double frequency)
    {
        double real = 0.0;
        double imag = 0.0;

        for(int x = 0; x < filter.length; x++)
        {
            real += filter[x] * FastMath.cos(FastMath.PI * frequency * (double)x);
            imag += filter[x] * FastMath.sin(FastMath.PI * frequency * (double)x);
        }

        return decibel(real, imag);
    }

    /**
     * Calculates the decibel magnitude of the real and imaginary complex sample
     *
     * @param real (x-axis) value
     * @param imag (y-axis) value
     * @return magnitude in decibels
     */
    public static double decibel(double real, double imag)
    {
        return (float)(10.0 * FastMath.log10(FastMath.pow(real, 2.0) + FastMath.pow(imag, 2.0)));
    }


    public static float[] getRemezChannelizer(int channelBandwidth, int channels, int tapsPerChannel, double alpha,
                                              double passRipple, double stopRipple) throws FilterDesignException
    {
        FIRFilterSpecification specification = FIRFilterSpecification.channelizerBuilder()
            .sampleRate(channels * channelBandwidth * 4)
            .channels(channels)
            .channelBandwidth(channelBandwidth)
            .tapsPerChannel(tapsPerChannel)
            .alpha(alpha)
            .passRipple(passRipple)
            .stopRipple(stopRipple)
            .build();

        RemezFIRFilterDesignerWithLagrange designer = new RemezFIRFilterDesignerWithLagrange(specification);

        float[] taps = designer.getImpulseResponse();

        double bandEdgeFrequency = (double)channelBandwidth / (double)(channels * channelBandwidth * 2);

        double response = decibel(designer.getFrequencyResponse(FastMath.cos(bandEdgeFrequency * FastMath.PI)), 0.0);
        mLog.debug("Frequency Response at 1.0: " + response);
        response = decibel(designer.getFrequencyResponse(FastMath.cos(bandEdgeFrequency * FastMath.PI * 2.0)), 0.0);
        mLog.debug("Frequency Response at 2.0: " + response);

        return taps;
    }

    /**
     * Polyphase M2 synthesizer sync filter.  Designed for multiple M2 oversampled channel inputs and
     * an M*channel count output.
     *
     * @param channelSampleRate per input channel
     * @param channelBandwidth per input channel
     * @param channels input count
     * @param tapsPerChannel minumum.  This may be increased to meet the band edge -6.02dB requirement
     * @return filter
     * @throws FilterDesignException
     */
    public static float[] getSincM2Synthesizer(double channelSampleRate, double channelBandwidth, int channels,
                                               int tapsPerChannel) throws FilterDesignException
    {
        int filterLength = (channels * tapsPerChannel) - 1;

        double cutoff = (channelBandwidth * SYNTHESIZER_CUTOFF_CHANNEL_BANDWIDTHS) / (channelSampleRate * (double)channels);

        float[] taps = FilterFactory.getKaiserSinc(filterLength, cutoff, 80.0f);

        //This is an odd length filter - increase the length by 1 by pre-padding a zero coefficient
        float[] extendedTaps = new float[taps.length + 1];
        System.arraycopy(taps, 0, extendedTaps, 1, taps.length);

        return extendedTaps;
    }

    /**
     * Creates a windowed-sync (Nyquist) M/2 prototype FIR filter for use with a Polyphase Channelizer/Synthesizer.
     * The filter is designed for x2 oversampling of each channel and the filter cutoff frequency is incrementally
     * adjusted to achieve a -6.02 dB attenuation at the channel band edge to enable perfect reconstruction of adjacent
     * channels.
     *
     * The odd length sync filter that meets the design objectives will be increased in length by pre-padding a zero
     * coefficient to produce a filter with a length that is an integral of the channel count.
     *
     * Note: the tapsPerChannel value will be iteratively increased (up to 10x) if a filter cannot be designed to meet
     * the channel band edge attenuation objective.
     *
     * Note: a channelizer filter designed using a Blackman-Harris-7 window and 18-21 taps per channel creates a filter
     * with attenuation in range of -60 to -80 dB at the edge of the 2x sampled channel.
     *
     * @param channelBandwidth - full bandwidth of the channel in hertz
     * @param channels - number of channels
     * @param tapsPerChannel - desired filter tap count per channel (may be increased to meet objective)
     * @param windowType - to use when designing the windowed-sync filter
     * @param logResults - to log a debug summary of the filter characteristics
     *
     * @return filter coefficients
     *
     * @throws FilterDesignException if a filter cannot be designed with a band edge attenuation of -6.02 dB
     */

    /**
     * Polyphase M2 channelizer sync filter.  Designed for an M*channel input and an M2 channel output.
     *
     * @param channelBandwidth per channel
     * @param channels count
     * @param tapsPerChannel minimum.  This may be increased to meet the band edge -6.02dB requirement
     * @param logResults to log the results of the design
     * @return filter
     * @throws FilterDesignException if the filter cannot be designed with a band edge of -6.02dB
     */
    public static float[] getSincM2Channelizer(double channelBandwidth, int channels, int tapsPerChannel,
                                               boolean logResults) throws FilterDesignException
    {
        int currentTapsPerChannel = tapsPerChannel;
        int filterLength = (channels * currentTapsPerChannel) - 1;

        double sampleRate = channelBandwidth * channels;
        double bandEdge = channelBandwidth / sampleRate;
        double cutoffFrequency = bandEdge / 2.0;
        double increment = cutoffFrequency * 0.1;

        //Get an initial filter and band edge frequency response using the channel bandwidth as a cutoff
        float[] taps = null;

        taps = FilterFactory.getKaiserSinc(filterLength, cutoffFrequency, 80.0f);
        double response = FilterFactory.evaluate(taps, bandEdge);

        //Set cutoff adjustment threshold - we'll test cutoff frequencies to around 1 hertz resolution
        double incrementThreshold = 1.0 / sampleRate;

        //Iteratively evaluate filters to find the filter with the highest cutoff frequency that meets the objective
        while(increment > incrementThreshold)
        {
            //If the current cutoff meets the objective, test if a higher cutoff also meets the objective
            if(matchesObjective(response, PERFECT_RECONSTRUCTION_GAIN_AT_BAND_EDGE, MARGIN_OF_ERROR) &&
                (cutoffFrequency + increment <= bandEdge))
            {
                float[] higherCutoffTaps = FilterFactory.getKaiserSinc(filterLength, cutoffFrequency + increment, 80.0f);
                double higherCutoffResponse = FilterFactory.evaluate(higherCutoffTaps, bandEdge);

                if(matchesObjective(higherCutoffResponse, PERFECT_RECONSTRUCTION_GAIN_AT_BAND_EDGE, MARGIN_OF_ERROR))
                {
                    cutoffFrequency += increment;
                    taps = higherCutoffTaps;
                    response = higherCutoffResponse;
                }
                else
                {
                    increment /= 2.0;
                }
            }
            //If the current cutoff meets the objective, decrease the increment to test for smaller resolution cutoffs
            else if(matchesObjective(response, PERFECT_RECONSTRUCTION_GAIN_AT_BAND_EDGE, MARGIN_OF_ERROR))
            {
                increment /= 2.0;
            }
            //Decrease the cutoff frequency - we can't meet the objective
            else
            {
                cutoffFrequency -= increment;

                if(cutoffFrequency <= 0)
                {
                    if(logResults)
                    {
                        mLog.debug("Warning: cannot design channelizer filter with tap count [" + currentTapsPerChannel +
                            "] increasing tap count and starting over");
                    }

                    currentTapsPerChannel++;

                    //After we've attempted increasing the taps per channel 10x, give up
                    if(currentTapsPerChannel > (tapsPerChannel + 10))
                    {
                        throw new FilterDesignException("Couldn't design filter with taps per channel count in the " +
                            "range of " + tapsPerChannel + " - " + (tapsPerChannel + 10) + " Sample Rate:" + sampleRate + " Channels:" + channels);
                    }

                    filterLength = channels * currentTapsPerChannel - 1;
                    cutoffFrequency = channelBandwidth / sampleRate;
                    increment = cutoffFrequency * 0.1;
                }

                taps = FilterFactory.getKaiserSinc(filterLength, cutoffFrequency, 80.0f);
                response = FilterFactory.evaluate(taps, bandEdge);
            }
        }

        //This will probably never happen, but ...
        if(!matchesObjective(response, PERFECT_RECONSTRUCTION_GAIN_AT_BAND_EDGE, MARGIN_OF_ERROR))
        {
            throw new FilterDesignException("Cannot design filter to specifications");
        }

        //This is an odd length filter - increase the length by 1 by pre-padding a zero coefficient
        float[] extendedTaps = new float[taps.length + 1];
        System.arraycopy(taps, 0, extendedTaps, 1, taps.length);

        if(logResults)
        {
            mLog.debug("Polyphase Channelizer Filter Design Summary");
            mLog.debug("-----------------------------------------------------");
            mLog.debug("Input Sample Rate: " + sampleRate);
            mLog.debug("Channel Bandwidth: " + channelBandwidth);
            mLog.debug("Channels: " + channels);
            mLog.debug("Window Type: " + WindowType.KAISER.name());
            mLog.debug("Taps Per Channel - Requested:" + tapsPerChannel + " Actual:" + ((double)extendedTaps.length / (double)channels));
            mLog.debug("Filter Length: " + (extendedTaps.length));
            mLog.debug("Requested Cutoff Frequency:  " + (sampleRate * bandEdge));
            mLog.debug("Actual Cutoff Frequency:  " + (sampleRate * cutoffFrequency));
            mLog.debug("Attenuation at 0.25 Channels:  " + evaluate(taps, bandEdge * 0.25) + "\tFrequency: " + (sampleRate * bandEdge * 0.25) + "  [" + (bandEdge * 0.25) + "]");
            mLog.debug("Attenuation at 0.50 Channels:  " + evaluate(taps, bandEdge * 0.50) + "\tFrequency: " + (sampleRate * bandEdge * 0.50) + "  [" + (bandEdge * 0.50) + "]");
            mLog.debug("Attenuation at 0.75 Channels:  " + evaluate(taps, bandEdge * 0.75) + "\tFrequency: " + (sampleRate * bandEdge * 0.75) + "  [" + (bandEdge * 0.75) + "]");
            mLog.debug("Attenuation        OBJECTIVE:  " + PERFECT_RECONSTRUCTION_GAIN_AT_BAND_EDGE);
            mLog.debug("Attenuation at 1.00 Channels:  " + evaluate(taps, bandEdge * 1.00) + "\tFrequency: " + (sampleRate * bandEdge * 1.0) + "  [" + (bandEdge * 1.0) + "]");
            mLog.debug("Attenuation at 1.25 Channels:  " + evaluate(taps, bandEdge * 1.25) + "\tFrequency: " + (sampleRate * bandEdge * 1.25) + "  [" + (bandEdge * 1.25) + "]");
            mLog.debug("Attenuation at 1.50 Channels:  " + evaluate(taps, bandEdge * 1.50) + "\tFrequency: " + (sampleRate * bandEdge * 1.5) + "  [" + (bandEdge * 1.5) + "]");
            mLog.debug("Attenuation at 1.75 Channels:  " + evaluate(taps, bandEdge * 1.75) + "\tFrequency: " + (sampleRate * bandEdge * 1.75) + "  [" + (bandEdge * 1.75) + "]");
            mLog.debug("Attenuation at 2.00 Channels:  " + evaluate(taps, bandEdge * 2.00) + "\tFrequency: " + (sampleRate * bandEdge * 2.0) + "  [" + (bandEdge * 2.0) + "]");
        }

        return extendedTaps;
    }

    /**
     * Creates a windowed-sync (Nyquist) filter.
     *
     * @param cutoff frequency (0.0 <> 0.5)
     * @param length of the filter - must be odd-length
     * @param windowType to use in designing the filter
     * @return filter coefficients.
     * @throws FilterDesignException if the requested length is not odd
     */
    public static float[] getSinc(double cutoff, int length, WindowType windowType)
    {
        if(length % 2 == 0)
        {
            throw new IllegalArgumentException("Sinc filters must be odd-length");
        }

        float[] coefficients = new float[length];
        int half = length / 2;

        float[] window = WindowFactory.getWindow(windowType, length);

        double scalor = 2.0 * cutoff;
        double piScalor = FastMath.PI * scalor;

        coefficients[half] = (float)(1.0 * scalor * window[half]);

        for(int x = 1; x <= half; x++)
        {
            double a = piScalor * x;
            double coefficient = scalor * (FastMath.sin(a) / a);

            coefficient *= window[half + x];
            coefficients[half + x] = (float)coefficient;
            coefficients[half - x] = (float)coefficient;
        }

        return coefficients;
    }

    /**
     * Creates a windowed-sync (Nyquist) filter.
     *
     * @param cutoff frequency (0.0 <> 0.5)
     * @param length of the filter - must be odd-length
     * @param windowType to use in designing the filter
     * @return filter coefficients.
     * @throws FilterDesignException if the requested length is not odd
     */
    public static float[] getInverseSync(double cutoff, int length, WindowType windowType)
    {
        if(length % 2 == 0)
        {
            throw new IllegalArgumentException("Sinc filters must be odd-length");
        }

        float[] coefficients = new float[length];
        int half = length / 2;

        float[] window = WindowFactory.getWindow(windowType, length);

        double scalor = 2.0 * cutoff;
        double piScalor = FastMath.PI * scalor;

        coefficients[half] = (float)(1.0 * scalor * window[half]);

        for(int x = 1; x <= half; x++)
        {
            double a = piScalor * x;
            double coefficient = scalor * (a / FastMath.sin(a));

            coefficient *= window[half + x];
            coefficients[half + x] = (float)coefficient;
            coefficients[half - x] = (float)coefficient;
        }

        return coefficients;
    }

    /**
     * Creates a Nyquist filter using a Kaiser Window.
     *
     * @param cutoff frequency (0.0 <> 0.5)
     * @param length of the filter - must be odd-length
     * @param attenuation desired for adjacent channels
     * @return filter coefficients.
     * @throws FilterDesignException if the requested length is not odd
     */
    public static float[] getKaiserSinc(int length, double cutoff, float attenuation) throws FilterDesignException
    {
        if(length % 2 == 0)
        {
            throw new FilterDesignException("Sinc filters must be odd-length");
        }

        float[] coefficients = new float[length];
        int half = length / 2;

        float[] window = WindowFactory.getKaiser(length, attenuation);

        double scalor = 2.0 * cutoff;
        double piScalor = FastMath.PI * scalor;

        coefficients[half] = (float)(1.0 * scalor * window[half]);

        for(int x = 1; x <= half; x++)
        {
            double a = piScalor * x;
            double coefficient = scalor * FastMath.sin(a) / a;

            coefficient *= window[half + x];
            coefficients[half + x] = (float)coefficient;
            coefficients[half - x] = (float)coefficient;
        }

        return coefficients;
    }

    /**
     * Creates a symmetrical half-band filter where all odd-indexed coefficients except center are zero
     * valued.  The non-center coefficients are normalized to sum to 0.5 to compensate for the window, which provides
     * unity gain at DC and a null at the Nyquist frequency.
     * @param length N is odd where m is an integer multiple in the formula: N = 4m + 3 (e.g. 7, 11, 15, etc.)
     * @param windowType to apply to the filter.
     * @return coefficients
     */
    public static float[] getHalfBand(int length, WindowType windowType)
    {
        if((length - 3) % 4 != 0)
        {
            throw new IllegalArgumentException("Half Band filter length (N) must be an odd length where m must be an " +
                    "integer multiple in (N=4m+3), e.g. 7, 11, 15, etc");
        }

        float[] window = WindowFactory.getWindow(windowType, length);
        float[] taps = new float[length];

        int halfLength = length / 2;

        for(int x = 0; x < length; x++)
        {
            int offset = x - halfLength;

            if(offset == 0)
            {
                taps[x] = 0.5f; //No window here - window coefficient should be 1.0 anyway
            }
            else if((x % 2) == 0)
            {
                //Only calculate the even-index taps, leaving odd-index taps at default value of zero
                taps[x] = (float)((Math.sin(offset * Math.PI / 2) / (offset * Math.PI)) * window[x]);
            }
        }

        double sum = 0.0;

        for(int x = 0; x < length; x += 2)
        {
            sum += taps[x];
        }

        for(int x = 0; x < length; x += 2)
        {
            taps[x] = (float)(taps[x] * 0.5 / sum);
        }

        return taps;
    }

    /**
     * Creates the optimal FIR filter implementation using calibration data to select
     * from among the scalar and vector implementations.
     * @param coefficients for the filter
     * @return fir filter implementation
     */
    public static IRealFilter getRealFilter(float[] coefficients)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.FILTER_FIR);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorRealFIRFilterDefaultBit(coefficients);
            case VECTOR_SIMD_64:
                return new VectorRealFIRFilter64Bit(coefficients);
            case VECTOR_SIMD_128:
                return new VectorRealFIRFilter128Bit(coefficients);
            case VECTOR_SIMD_256:
                return new VectorRealFIRFilter256Bit(coefficients);
            case VECTOR_SIMD_512:
                return new VectorRealFIRFilter512Bit(coefficients);
            case UNCALIBRATED:
            case SCALAR:
            default:
                return new RealFIRFilter(coefficients);
        }
    }

    /**
     * Constructs the optimal decimation filter implementation for filter length and window type
     * using calibration data to select among scalar and vector implementation options.
     * @param length of decimation filter
     * @param windowType for designing the filter
     * @return filter implementation
     */
    public static IRealDecimationFilter getRealDecimationFilter(int length, WindowType windowType)
    {
        float[] coefficients = getHalfBand(length, windowType);

        switch(length)
        {
            case 11:
                switch(CalibrationManager.getInstance().getImplementation(CalibrationType.FILTER_HALF_BAND_REAL_11_TAP))
                {
                    case VECTOR_SIMD_64:
                        return new VectorRealHalfBandDecimationFilter11Tap64Bit(coefficients);
                    case VECTOR_SIMD_128:
                        return new VectorRealHalfBandDecimationFilter11Tap128Bit(coefficients);
                    case VECTOR_SIMD_256:
                        return new VectorRealHalfBandDecimationFilter11Tap256Bit(coefficients);
                    case VECTOR_SIMD_512:
                        return new VectorRealHalfBandDecimationFilter11Tap512Bit(coefficients);
                    case SCALAR:
                    case UNCALIBRATED:
                    default:
                        return new RealHalfBandDecimationFilter(coefficients);
                }
            case 15:
                switch(CalibrationManager.getInstance().getImplementation(CalibrationType.FILTER_HALF_BAND_REAL_15_TAP))
                {
                    case VECTOR_SIMD_64:
                        return new VectorRealHalfBandDecimationFilter15Tap64Bit(coefficients);
                    case VECTOR_SIMD_128:
                        return new VectorRealHalfBandDecimationFilter15Tap128Bit(coefficients);
                    case VECTOR_SIMD_256:
                        return new VectorRealHalfBandDecimationFilter15Tap256Bit(coefficients);
                    case VECTOR_SIMD_512:
                        return new VectorRealHalfBandDecimationFilter15Tap512Bit(coefficients);
                    case SCALAR:
                    case UNCALIBRATED:
                    default:
                        return new RealHalfBandDecimationFilter(coefficients);
                }
            case 23:
                switch(CalibrationManager.getInstance().getImplementation(CalibrationType.FILTER_HALF_BAND_REAL_23_TAP))
                {
                    case VECTOR_SIMD_64:
                        return new VectorRealHalfBandDecimationFilter23Tap64Bit(coefficients);
                    case VECTOR_SIMD_128:
                        return new VectorRealHalfBandDecimationFilter23Tap128Bit(coefficients);
                    case VECTOR_SIMD_256:
                        return new VectorRealHalfBandDecimationFilter23Tap256Bit(coefficients);
                    case VECTOR_SIMD_512:
                        return new VectorRealHalfBandDecimationFilter23Tap512Bit(coefficients);
                    case SCALAR:
                    case UNCALIBRATED:
                    default:
                        return new RealHalfBandDecimationFilter(coefficients);
                }
            case 63:
                switch(CalibrationManager.getInstance().getImplementation(CalibrationType.FILTER_HALF_BAND_REAL_63_TAP))
                {
                    case VECTOR_SIMD_64:
                        return new VectorRealHalfBandDecimationFilter63Tap64Bit(coefficients);
                    case VECTOR_SIMD_128:
                        return new VectorRealHalfBandDecimationFilter63Tap128Bit(coefficients);
                    case VECTOR_SIMD_256:
                        return new VectorRealHalfBandDecimationFilter63Tap256Bit(coefficients);
                    case VECTOR_SIMD_512:
                        return new VectorRealHalfBandDecimationFilter63Tap512Bit(coefficients);
                    case SCALAR:
                    case UNCALIBRATED:
                    default:
                        return new RealHalfBandDecimationFilter(coefficients);
                }
            default:
                switch(CalibrationManager.getInstance().getImplementation(CalibrationType.FILTER_HALF_BAND_REAL_DEFAULT))
                {
                    case VECTOR_SIMD_64:
                        return new VectorRealHalfBandDecimationFilter64Bit(coefficients);
                    case VECTOR_SIMD_128:
                        return new VectorRealHalfBandDecimationFilter128Bit(coefficients);
                    case VECTOR_SIMD_256:
                        return new VectorRealHalfBandDecimationFilter256Bit(coefficients);
                    case VECTOR_SIMD_512:
                        return new VectorRealHalfBandDecimationFilter512Bit(coefficients);
                    case SCALAR:
                    case UNCALIBRATED:
                    default:
                        return new RealHalfBandDecimationFilter(coefficients);
                }
        }
    }

    /**
     * Compares two doubles for equals and avoids any rounding error that are present.
     *
     * @param a value to compare
     * @param objective value to compare
     * @return true if they are equivalent within the margin of error
     */
    private static boolean matchesObjective(double a, double objective, double marginOfError)
    {
        return FastMath.abs(a - objective) <= marginOfError;
    }

    public static void main(String[] args)
    {
        DecimalFormat df = new DecimalFormat("0.000000");

        int length = 49;

        try
        {
            float [] taps = FilterFactory.getSinc(0.5, length, WindowType.NONE);

            for(int x = 0; x < length; x++)
            {
                mLog.debug("Tap: " + x + " Value: " + df.format(taps[x]));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        mLog.debug("Done!");
    }
}
