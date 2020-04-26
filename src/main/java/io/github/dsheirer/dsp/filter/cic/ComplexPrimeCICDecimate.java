/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
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
import org.apache.commons.math3.primes.Primes;

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
    public ComplexPrimeCICDecimate(double sampleRate, int decimation, double passFrequency, double stopFrequency)
        throws FilterDesignException
    {
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
        List<Integer> primeFactors = sPrimeFactors.get(decimation);
        if(primeFactors != null)
        {
            return primeFactors;
        }

        primeFactors = Primes.primeFactors(decimation);

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

        public Output(double outputSampleRate, double passFrequency, double stopFrequency) throws FilterDesignException
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

            if(mReusableComplexBufferListener == null)
            {
                removeListener();
            }
        }

        public void removeListener()
        {
            mReusableComplexBufferListener = new Listener<ReusableComplexBuffer>()
            {
                @Override
                public void receive(ReusableComplexBuffer reusableComplexBuffer)
                {
                    //empty receiver
                    reusableComplexBuffer.decrementUserCount();
                }
            };
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
        private float[] getLowPassFilter(double sampleRate, double passFrequency, double stopFrequency) throws FilterDesignException
        {
            //Use existing filter if we've already designed one
            float[] existingFilter = sLowPassFilters.get((int) sampleRate);
            if(existingFilter != null)
            {
                return existingFilter;
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
