/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.cic;

import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter2;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferAssembler;
import io.github.dsheirer.sample.complex.ComplexSampleListener;
import io.github.dsheirer.sample.decimator.ComplexDecimator;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Non-Recursive Prime-Factor CIC Filter.
 *
 * Implements the CIC filter described in Understanding Digital Signal Processing, 3e, Lyons, on page 769.  This
 * filter is comprised of multiple decimating stages each with a prime factor decimation rate.  Multiple stages are
 * cascaded to achieve the overall decimation rate.
 *
 * This filter currently supports a maximum decimation rate of 2801.  Higher decimation rates can be added by
 * adding additional prime factors to the PRIMES array.
 */
public class ComplexPrimeCICDecimate implements Listener<ReusableComplexBuffer>
{
//    private final static Logger mLog = LoggerFactory.getLogger(ComplexPrimeCICDecimate.class);

    /**
     * PRIME numbers to use for decimation stage sizing.  This list contains values that should
     * support input sample rates up to 70 MHz with an output channel rate of at least 25 kHz.
     */
    public static int[] PRIMES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53,
        59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151,
        157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251,
        257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359,
        367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463,
        467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593,
        599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701,
        709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827,
        829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941, 947, 953,
        967, 971, 977, 983, 991, 997, 997, 1009, 1013, 1019, 1021, 1031, 1033, 1039, 1049, 1051,
        1061, 1063, 1069, 1087, 1091, 1093, 1097, 1103, 1109, 1117, 1123, 1129, 1151, 1153, 1163,
        1171, 1181, 1187, 1193, 1201, 1213, 1217, 1223, 1229, 1231, 1237, 1249, 1259, 1277, 1279,
        1283, 1289, 1291, 1297, 1301, 1303, 1307, 1319, 1321, 1327, 1361, 1367, 1373, 1381, 1399,
        1409, 1423, 1427, 1429, 1433, 1439, 1447, 1451, 1453, 1459, 1471, 1481, 1483, 1487, 1489,
        1493, 1499, 1511, 1523, 1531, 1543, 1549, 1553, 1559, 1567, 1571, 1579, 1583, 1597, 1601,
        1607, 1609, 1613, 1619, 1621, 1627, 1637, 1657, 1663, 1667, 1669, 1693, 1697, 1699, 1709,
        1721, 1723, 1733, 1741, 1747, 1753, 1759, 1777, 1783, 1787, 1789, 1801, 1811, 1823, 1831,
        1847, 1861, 1867, 1871, 1873, 1877, 1879, 1889, 1901, 1907, 1913, 1931, 1933, 1949, 1951,
        1973, 1979, 1987, 1993, 1997, 1999, 2003, 2011, 2017, 2027, 2029, 2039, 2053, 2063, 2069,
        2081, 2083, 2087, 2089, 2099, 2111, 2113, 2129, 2131, 2137, 2141, 2143, 2153, 2161, 2179,
        2203, 2207, 2213, 2221, 2237, 2239, 2243, 2251, 2267, 2269, 2273, 2281, 2287, 2293, 2297,
        2309, 2311, 2333, 2339, 2341, 2347, 2351, 2357, 2371, 2377, 2381, 2383, 2389, 2393, 2399,
        2411, 2417, 2423, 2437, 2441, 2447, 2459, 2467, 2473, 2477, 2503, 2521, 2531, 2539, 2543,
        2549, 2551, 2557, 2579, 2591, 2593, 2609, 2617, 2621, 2633, 2647, 2657, 2659, 2663, 2671,
        2677, 2683, 2687, 2689, 2693, 2699, 2707, 2711, 2713, 2719, 2729, 2731, 2741, 2749, 2753,
        2767, 2777, 2789, 2791, 2797, 2801
    };

    private static Map<Integer,float[]> sLowPassFilters = new HashMap();
    private static Map<Integer,List<Integer>> sPrimeFactors = new HashMap();

    private List<DecimatingStage> mDecimatingStages = new ArrayList<DecimatingStage>();
    private DecimatingStage mFirstDecimatingStage;
    private Output mOutput;

    /**
     * Constructs a new decimation filter.
     *
     * An example setup for a 2.4 MHz input sample rate and 25 kHz output channel rate with a desired 12.5 kHz channel
     * pass band might be:
     * sampleRate = 2,400,000.0
     * decimation = 96
     * passFrequency = 6250 (1/4 of the channel rate)
     * stopFrequency = 7000
     *
     * Note: the transition bandwidth (stop - start) directly impacts the size of the low-pass cleanup filter,
     * where smaller transition bandwidth creates a longer filter that requires more processing effort.
     *
     * @param sampleRate of the input sample stream
     * @param decimation - overall decimation rate
     * @param passFrequency for the final cleanup filter
     * @param stopFrequency for the final cleanup filter
     * @throws FilterDesignException if a final low-pass cleanup filter cannot be created for the output channel rate
     *                               and specified pass/stop frequencies.
     */
    public ComplexPrimeCICDecimate(double sampleRate, int decimation, int passFrequency, int stopFrequency)
        throws FilterDesignException
    {
        Validate.isTrue(decimation <= PRIMES[PRIMES.length - 1]);

        List<Integer> stageSizes = getPrimeFactors(decimation);

        for(int x = 0; x < stageSizes.size(); x++)
        {
            DecimatingStage stage = new DecimatingStage(stageSizes.get(x), 1);

            mDecimatingStages.add(stage);

            if(x == 0)
            {
                /* Reference to first stage -- will receive all samples */
                mFirstDecimatingStage = stage;
            }
            else
            {
                /* Wire the current stage to the previous stage */
                mDecimatingStages.get(x - 1).setListener(stage);
            }
        }

        double channelRate = sampleRate / (double)decimation;

        mOutput = new Output(channelRate, passFrequency, stopFrequency);

        mDecimatingStages.get(mDecimatingStages.size() - 1).setListener(mOutput);
    }

    public void dispose()
    {
        for(DecimatingStage stage : mDecimatingStages)
        {
            stage.dispose();
        }

        mDecimatingStages.clear();
        mDecimatingStages = null;
        mFirstDecimatingStage = null;

        mOutput.dispose();
        mOutput = null;
    }

    /**
     * Adds a listener to receive the output of this CIC decimation filter
     */
    public void setListener(Listener<ReusableComplexBuffer> listener)
    {
        mOutput.setListener(listener);
    }

    /**
     * Removes listener from output of this CIC decimation filter
     */
    public void removeListener()
    {
        mOutput.removeListener();
    }

    /**
     * Calculates the prime factors of the decimation rate.  If you wish to have decimation rates higher than the
     * highest rate listed in the PRIMES array, then add additional prime factors to the PRIMES array.
     *
     * @param decimation - integral decimation rate
     * @return - ordered list (smallest to largest) of prime factors
     */
    public static List<Integer> getPrimeFactors(int decimation)
    {
        if(decimation > PRIMES[PRIMES.length - 1])
        {
            throw new IllegalArgumentException("Decimation rates higher than [" + PRIMES[PRIMES.length - 1] +
                "] are not supported.  Update the PRIMES array to add support for higher decimation rates");
        }

        if(sPrimeFactors.containsKey(decimation))
        {
            return sPrimeFactors.get(decimation);
        }

        List<Integer> primeFactors = new ArrayList<Integer>();

        int pointer = 0;

        while(decimation > 0 && pointer < PRIMES.length)
        {
            int prime = PRIMES[pointer];

            if(decimation % prime == 0)
            {
                primeFactors.add(prime);
                decimation /= prime;
            }
            else
            {
                pointer++;
            }
        }

        sPrimeFactors.put(decimation, primeFactors);

        return primeFactors;
    }

    /**
     * Primary input method for receiving sample arrays composed as I,Q,I,Q, etc.
     */
    @Override
    public void receive(ReusableComplexBuffer buffer)
    {
        if(mFirstDecimatingStage != null)
        {
            float[] samples = buffer.getSamples();

            for(int x = 0; x < samples.length; x += 2)
            {
                mFirstDecimatingStage.receive(samples[x], samples[x + 1]);
            }
        }

        buffer.decrementUserCount();
    }

    /**
     * Decimating stage combines multiple CIC stages with a decimator.  The
     * number of stages is indicated by the order value and the size indicates
     * the decimation rate of this stage.
     */
    public class DecimatingStage implements ComplexSampleListener
    {
        private List<Stage> mStages = new ArrayList<Stage>();
        private Stage mFirstStage;
        private ComplexDecimator mDecimator;

        public DecimatingStage(int size, int order)
        {
            for(int x = 0; x < order; x++)
            {
                Stage stage;

                if(size == 2)
                {
                    stage = new TwoStage();
                }
                else
                {
                    stage = new Stage(size);
                }

                mStages.add(stage);

                if(x == 0)
                {
                    mFirstStage = stage;
                }
                else
                {
                    mStages.get(x - 1).setListener(stage);
                }
            }

            mDecimator = new ComplexDecimator(size);
            mStages.get(mStages.size() - 1).setListener(mDecimator);
        }

        public void dispose()
        {
            for(Stage stage : mStages)
            {
                stage.dispose();
            }

            mStages.clear();
            mDecimator.dispose();
            mDecimator = null;
            mFirstStage = null;
            mStages = null;
        }

        @Override
        public void receive(float i, float q)
        {
            mFirstStage.receive(i, q);
        }

        public void setListener(ComplexSampleListener listener)
        {
            mDecimator.setListener(listener);
        }
    }

    /**
     * Single non-decimating CIC stage component.  Uses a circular buffer and a running average internally to implement
     * the stage so that stage size has essentially no impact on the computational requirements of the stage
     */
    public class Stage implements ComplexSampleListener
    {
        protected ComplexSampleListener mListener;

        private float[] mISamples;
        private float[] mQSamples;

        protected float mISum;
        protected float mQSum;

        private int mSamplePointer = 0;
        private int mSize;

        protected float mGain;

        protected Stage()
        {
        }

        public Stage(int size)
        {
            mSize = size - 1;

            mISamples = new float[mSize];
            mQSamples = new float[mSize];

            float baseGain = 1.0f / (float)size;
            mGain = baseGain * (1.0f / (1.0f - baseGain));
        }

        public void dispose()
        {
            mListener = null;
        }

        public void receive(float i, float q)
        {
            /* Subtract the oldest sample and add in the newest sample */
            mISum = mISum - mISamples[mSamplePointer] + i;
            mQSum = mQSum - mQSamples[mSamplePointer] + q;

            /* Overwrite the oldest sample with the newest */
            mISamples[mSamplePointer] = i;
            mQSamples[mSamplePointer] = q;

            mSamplePointer++;

            if(mSamplePointer >= mSize)
            {
                mSamplePointer = 0;
            }

            if(mListener != null)
            {
                mListener.receive((mISum * mGain), (mQSum * mGain));
            }
        }

        public void setListener(ComplexSampleListener listener)
        {
            mListener = listener;
        }
    }

    /**
     * Size 2 stage that eliminates the unnecessary circular buffer management for a two-stage.
     */
    public class TwoStage extends Stage
    {
        public TwoStage()
        {
            mGain = 0.5f;
        }

        public void receive(float i, float q)
        {
            if(mListener != null)
            {
                mListener.receive(((mISum + i) * mGain), ((mQSum + q) * mGain));
            }

            mISum = i;
            mQSum = q;
        }
    }


    /**
     * Output adapter - applies gain correction and cleanup filter and broadcast
     * to registered listener.
     */
    public class Output implements ComplexSampleListener
    {
        /* Decimated output buffers will contain 1024 complex samples */
        private ReusableComplexBufferAssembler mBufferAssembler;
        private ComplexFIRFilter2 mLowPassFilter;
        private Listener<ReusableComplexBuffer> mReusableComplexBufferListener;

        public Output(double outputSampleRate, int passFrequency, int stopFrequency) throws FilterDesignException
        {
            mBufferAssembler = new ReusableComplexBufferAssembler(2400, outputSampleRate);

            //This may throw an exception if we can't design a filter for the sample rate and pass/stop frequencies
            float[] filterCoefficients = getLowPassFilter(outputSampleRate, passFrequency, stopFrequency);
            mLowPassFilter = new ComplexFIRFilter2(filterCoefficients, 1.0f);

            mBufferAssembler.setListener(new Listener<ReusableComplexBuffer>()
            {
                @Override
                public void receive(ReusableComplexBuffer reusableComplexBuffer)
                {
                    if(mReusableComplexBufferListener != null)
                    {
                        ReusableComplexBuffer filteredBuffer = mLowPassFilter.filter(reusableComplexBuffer);
                        mReusableComplexBufferListener.receive(filteredBuffer);
//                        mReusableComplexBufferListener.receive(reusableComplexBuffer);
                    }
                    else
                    {
                        reusableComplexBuffer.decrementUserCount();
                    }
                }
            });
        }

        public void dispose()
        {
            mBufferAssembler.dispose();
            mLowPassFilter.dispose();
        }

        /**
         * Interface for receiving CIC decimated output samples
         */
        @Override
        public void receive(float inphase, float quadrature)
        {
            mBufferAssembler.receive(inphase, quadrature);
        }

        public void setListener(Listener<ReusableComplexBuffer> listener)
        {
            mReusableComplexBufferListener = listener;
        }

        public void removeListener()
        {
            mReusableComplexBufferListener = null;
        }

        /**
         * Creates a low-pass filter to use as the final cleanup filter for the decimated output stream
         *
         * @param sampleRate for the final output channel rate
         * @param passFrequency for half of the desired channel rate
         * @param stopFrequency for the attenuated band
         * @return a newly designed filter or a previously designed (cached) filter
         * @throws FilterDesignException
         */
        private float[] getLowPassFilter(double sampleRate, int passFrequency, int stopFrequency) throws FilterDesignException
        {
            //Use existing filter if we've already designed one
            if(sLowPassFilters.containsKey((int)sampleRate))
            {
                return sLowPassFilters.get((int)sampleRate);
            }

            FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                .sampleRate(sampleRate)
                .gridDensity(16)
                .passBandCutoff(passFrequency)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .stopBandStart(stopFrequency)
                .stopBandAmplitude(0.0)
                .stopBandRipple(0.01)
                .build();

            RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

            //This will throw an exception if the filter cannot be designed
            float[] taps = designer.getImpulseResponse();

            sLowPassFilters.put((int)sampleRate, taps);

            return taps;
        }
    }
}
