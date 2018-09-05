/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *	   Root Raised Cosine filter designer:
 *	   Copyright 2002,2007,2008,2012,2013 Free Software Foundation, Inc.
 *	   http://gnuradio.org/redmine/projects/gnuradio/repository/changes/gr-filter
 *	   /lib/firdes.cc?rev=435b1d166f0c7092bbd5e1f788e75dbb6ade3a4b
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
package io.github.dsheirer.dsp.filter;

import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesignerWithLagrange;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility methods for designing various types of filters.
 */
public class FilterFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(FilterFactory.class);
    private static final double PERFECT_RECONSTRUCTION_GAIN_AT_BAND_EDGE = -6.020599842071533; //decibel(0.5, 0.0)
    private static final double MARGIN_OF_ERROR = 0.0003;

    /**
     * Generates coefficients for a unity-gain, windowed low-pass filter
     *
     * @param sampleRate - hertz
     * @param frequency - cutoff in hertz
     * @param length - filter length
     * @param window - to apply against the coefficients
     * @return
     */
    public static float[] getSinc(double sampleRate, long frequency, int length, Window.WindowType window)
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
        coefficients = Window.apply(window, coefficients);

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

        for(int x = 0; x < coefficients.length; x++)
        {
            accumulator += Math.abs(coefficients[x]);
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

        for(int x = 0; x < coefficients.length; x++)
        {
            accumulator += Math.abs(coefficients[x]);
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
            int binCount = (int)((Math.round((float)frequency / (float)sampleRate * (float)length)));

            for(int x = 0; x < binCount; x++)
            {
                unityArray[x] = 1.0f;
                unityArray[length - 1 - x] = 1.0f;
            }
        }
        else //odd length
        {
            int binCount = (int)((Math.round((float)frequency / (float)sampleRate * (float)length + 0.5)));

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
    public static float[] getLowPass(double sampleRate, long cutoff, int filterLength, Window.WindowType windowType)
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
                                     Window.WindowType windowType, boolean forceOddLength)
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
    public static float[] getHighPass(int sampleRate, long cutoff, int filterLength, Window.WindowType windowType)
    {
        //Convert the high frequency cutoff to its low frequency cutoff
        //equivalent, so that when we generate the low pass filter, prior to
        //inversion, its at the correct frequency
        long convertedCutoff = sampleRate / 2 - cutoff;

        return invert(getSinc(sampleRate, convertedCutoff, filterLength, windowType));
    }

    public static float[] getHighPass(int sampleRate, long stopFrequency, long passFrequency, int attenuation,
                                      Window.WindowType windowType, boolean forceOddLength)
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
            sb.append(x + ": " + array[x]);

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

        return (int)(Math.round((double)attenuation / (22.0d * frequency)));
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
            int testDelta = Math.abs(desiredFactor - factor);

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

        double numerator = 1.0d - (Math.sqrt((double)decimation * ratio / (2.0d - ratio)));

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
    public static float[] getCICCleanupFilter(int outputSampleRate, int passFrequency, int attenuation, Window.WindowType window)
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

        int binCount = (int)((Math.round(
            (double)frequency / (double)sampleRate * 2.0d * (double)length)));

        cicArray[0] = 1.0f;

        float unityResponse = (float)(Math.sin(1.0d / (double)length) /
            (1.0d / (double)length));

        for(int x = 1; x <= binCount; x++)
        {
            /* Calculate unity response plus amplification for droop */
            float compensated = 1.0f + (unityResponse - (float)(Math.sin((double)x / (double)length) / ((double)x / (double)length)));

            cicArray[x] = compensated;
            cicArray[length - x] = compensated;
        }

        return cicArray;
    }

    /**
     * Creates root raised cosine filter coefficients with a tap count equal to the symbols x samplesPerSymbol + 1.
     *
     * Symbol count should be an even integer.
     * Ported to java from gnuradio/filter/firdes.cc
     *
     * For 40db attenuation, calculate the number of symbols based on the following formula:
     * Symbols = -44 * alpha + 33
     *
     * Polyphase Channelizer notes:
     * -Set samples Per Symbol at 2 (or more) * number of channels
     * -Set symbolCount to sufficient size to produce required attenuation
     * -Set symbol rate in hertz
     * -Set alpha = (desired channel bandwidth / (symbol rate * samples per symbol) - 1.0
     * -Channel bandwidth must be larger than symbol rate * samples per symbol, in order for there to be a positive alpha
     *
     * @param samplesPerSymbol - number of samples per symbol
     * @param symbolCount - number of symbol intervals for the filter.  This directly impacts the filter size
     * @param alpha - roll-off factor.
     * @return - filter coefficients
     */
    public static float[] getRootRaisedCosine(int samplesPerSymbol, int symbolCount, float alpha)
    {
        int taps = samplesPerSymbol * symbolCount;

        float scale = 0;

        float[] coefficients = new float[taps];

        for(int x = 0; x < taps; x++)
        {
            float index = (float)x - ((float)taps / 2.0f);

            float x1 = (float)Math.PI * index / (float)samplesPerSymbol;
            float x2 = 4.0f * alpha * index / (float)samplesPerSymbol;
            float x3 = x2 * x2 - 1.0f;

            float numerator, denominator;

            if(Math.abs(x3) >= 0.000001)
            {
                if(x != taps / 2)
                {
                    numerator = (float)Math.cos((1.0 + alpha) * x1) +
                        (float)Math.sin((1.0f - alpha) * x1) / (4.0f * alpha * index / (float)samplesPerSymbol);
                }
                else
                {
                    numerator = (float)Math.cos((1.0f + alpha) * x1) + (1.0f - alpha) * (float)Math.PI / (4.0f * alpha);
                }

                denominator = x3 * (float)Math.PI;
            }
            else
            {
                if(alpha == 1.0f)
                {
                    coefficients[x] = -1.0f;
                    continue;
                }

                x3 = (1.0f - alpha) * x1;
                x2 = (1.0f + alpha) * x1;

                numerator = (float)(Math.sin(x2) * (1.0f + alpha) * Math.PI - Math.cos(x3) * ((1.0 - alpha) * Math.PI *
                    (double)samplesPerSymbol) / (4.0 * alpha * index) + Math.sin(x3) * (double)samplesPerSymbol *
                    (double)samplesPerSymbol / (4.0 * alpha * index * index));

                denominator = (float)(-32.0 * Math.PI * alpha * alpha * index / (double)samplesPerSymbol);
            }

            coefficients[x] = 4.0f * alpha * numerator / denominator;

            scale += coefficients[x];
        }

        /**
         * Normalize (scale) coefficients to 1.0 sum and apply gain
         */
        for(int x = 0; x < taps; x++)
        {
            coefficients[x] = coefficients[x] / scale;
        }

        return coefficients;
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
            real += filter[x] * Math.cos(Math.PI * frequency * (double)x);
            imag += filter[x] * Math.sin(Math.PI * frequency * (double)x);
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
        return (float)(10.0 * Math.log10(Math.pow(real, 2.0) + Math.pow(imag, 2.0)));
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

        double response = decibel(designer.getFrequencyResponse(Math.cos(bandEdgeFrequency * Math.PI)), 0.0);
        mLog.debug("Frequency Response at 1.0: " + response);
        response = decibel(designer.getFrequencyResponse(Math.cos(bandEdgeFrequency * Math.PI * 2.0)), 0.0);
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

        double cutoff = (channelBandwidth * 1.10) / (channelSampleRate * (double)channels);

        //Design the prototype synthesizer with 105% of the channel bandwidth produced by the channelizer.
        float[] taps = FilterFactory.getKaiserSinc(filterLength, cutoff, 80.0);

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

        taps = FilterFactory.getKaiserSinc(filterLength, cutoffFrequency, 80.0);
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
                float[] higherCutoffTaps = FilterFactory.getKaiserSinc(filterLength, cutoffFrequency + increment, 80.0);
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

                taps = FilterFactory.getKaiserSinc(filterLength, cutoffFrequency, 80.0);
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
            mLog.debug("Window Type: " + Window.WindowType.KAISER.name());
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
    public static float[] getSinc(double cutoff, int length, Window.WindowType windowType) throws FilterDesignException
    {
        if(length % 2 == 0)
        {
            throw new FilterDesignException("Sinc filters must be odd-length");
        }

        float[] coefficients = new float[length];
        int half = length / 2;

        double[] window = Window.getWindow(windowType, length);

        double scalor = 2.0 * cutoff;
        double piScalor = Math.PI * scalor;

        coefficients[half] = (float)(1.0 * scalor * window[half]);

        for(int x = 1; x <= half; x++)
        {
            double a = piScalor * x;
            double coefficient = scalor * Math.sin(a) / a;

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
    public static float[] getKaiserSinc(int length, double cutoff, double attenuation) throws FilterDesignException
    {
        if(length % 2 == 0)
        {
            throw new FilterDesignException("Sinc filters must be odd-length");
        }

        float[] coefficients = new float[length];
        int half = length / 2;

        double[] window = Window.getKaiser(length, attenuation);

        double scalor = 2.0 * cutoff;
        double piScalor = Math.PI * scalor;

        coefficients[half] = (float)(1.0 * scalor * window[half]);

        for(int x = 1; x <= half; x++)
        {
            double a = piScalor * x;
            double coefficient = scalor * Math.sin(a) / a;

            coefficient *= window[half + x];
            coefficients[half + x] = (float)coefficient;
            coefficients[half - x] = (float)coefficient;
        }

        return coefficients;
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
        return Math.abs(a - objective) <= marginOfError;
    }

    public static void main(String[] args)
    {
        float[] taps = null;

        try
        {
            taps = FilterFactory.getSincM2Channelizer(12500.0, 800, 9, true);
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Error", fde);
        }

        mLog.debug("Done: " + Arrays.toString(taps));
    }
}
