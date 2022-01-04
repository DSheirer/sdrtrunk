/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.dsp.filter.cic;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.dsp.oscillator.IRealOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import org.apache.commons.math3.primes.Primes;

import java.util.Arrays;
import java.util.List;

/**
 * Non-Recursive Prime-Factor CIC Filter.
 *
 * Implements the CIC filter described in Understanding Digital Signal Processing, 3e, Lyons, on page 769.  This
 * filter contains multiple decimating stages each with a prime factor decimation rate.  Multiple stages are
 * cascaded to achieve the overall decimation rate.
 *
 * This filter currently supports a maximum decimation rate of 2801.  Higher decimation rates can be added by
 * adding additional prime factors to the PRIMES array.
 */
public class PrimeCicDecimationFilter
{
    private Stage mFirstStage;

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
    public PrimeCicDecimationFilter(double sampleRate, int decimation, double passFrequency, double stopFrequency)
        throws FilterDesignException
    {
        List<Integer> stageSizes = Primes.primeFactors(decimation);

        Stage stage = null;

        for(int x = 0; x < stageSizes.size(); x++)
        {
            Stage nextStage = new DecimatingStage(stageSizes.get(x), 1);

            if(stage != null)
            {
                stage.setChild(nextStage);
            }

            stage = nextStage;

            if(x == 0)
            {
                /* Reference to first stage -- will receive all samples */
                mFirstStage = stage;
            }
        }

        //stage should reference the last stage that was created at this point.  Create an
        //output and chain it to the final stage.
        if(stage != null)
        {
            double channelRate = sampleRate / (double)decimation;
            Stage output = new Output(channelRate, passFrequency, stopFrequency);
            stage.setChild(output);
        }
    }

    /**
     * Decimates the sample array.
     * @param samples to decimate
     * @return decimated samples
     */
    public float[] decimate(float[] samples)
    {
        return mFirstStage.process(samples);
    }

    /**
     * Base stage for all decimating and non-decimating stages
     */
    public abstract class Stage
    {
        private Stage mChild;

        /**
         * Sets the child stage to follow this stage
         * @param stage child
         */
        public void setChild(Stage stage)
        {
            mChild = stage;
        }

        /**
         * Indicates if this stage has an optional child stage that should process the samples
         * after this stage processes those samples.
         */
        protected boolean hasChild()
        {
            return mChild != null;
        }

        /**
         * Optional child stage that should process samples after this stage.
         */
        protected Stage getChild()
        {
            return mChild;
        }

        /**
         * Process the samples.  Note: original argument array will be reused if/when possible.
         * @param samples to process
         * @return processed samples.
         */
        public abstract float[] process(float[] samples);
    }

    /**
     * Decimating stage chains multiple non-decimating stages with a decimator.  The
     * number of stages in the chain is dictated by the order value and the size indicates
     * the decimation rate of this stage.
     */
    public class DecimatingStage extends Stage
    {
        private Stage mFirstStage;
        private Stage mDecimator;

        /**
         * Constructs a decimating stage
         * @param size
         * @param order
         */
        public DecimatingStage(int size, int order)
        {
            Stage stage = null;

            for(int x = 0; x < order; x++)
            {
                Stage nextStage;

                if(size == 2)
                {
                    nextStage = new NoDecimateTwoStage();
                }
                else
                {
                    nextStage = new NoDecimateStage(size);
                }

                //Chain each stage together
                if(stage != null)
                {
                    stage.setChild(nextStage);
                }

                stage = nextStage;

                if(x == 0)
                {
                    mFirstStage = stage;
                }
            }

            mDecimator = new Decimator(size);

            //Add a decimator to the end of the stage chain.
            if(stage != null)
            {
                stage.setChild(mDecimator);
            }
        }

        @Override
        public float[] process(float[] samples)
        {
            if(hasChild())
            {
                return getChild().process(mFirstStage.process(samples));
            }

            return mFirstStage.process(samples);
        }

        /**
         * Override the default behavior since this stage is a chain of stages and we want any external
         * child to be added to the final stage in the chain, which is the decimator.
         * @param stage child to add
         */
        @Override
        public void setChild(Stage stage)
        {
            mDecimator.setChild(stage);
        }
    }

    /**
     * Decimates samples of float arrays.
     */
    public class Decimator extends Stage
    {
        private float[] mResidual;
        private int mDecimationFactor;

        /**
         * Constructs an instance.
         * @param decimationFactor ratio of 1:decimate.
         */
        public Decimator(int decimationFactor)
        {
            mDecimationFactor = decimationFactor;
        }

        /**
         * Decimates the sample array by combining the array with any residual from the previous decimation
         * operation, decimating, and then storing any residual samples leftover from the decimation.
         * @param samples to decimate
         * @return decimated samples array.
         */
        public float[] process(float[] samples)
        {
            if(mResidual != null)
            {
                float[] expanded = new float[mResidual.length + samples.length];
                System.arraycopy(mResidual, 0, expanded, 0, mResidual.length);
                System.arraycopy(samples, 0, expanded, mResidual.length, samples.length);
                samples = expanded;
                mResidual = null;
            }

            float[] decimated = new float[samples.length / mDecimationFactor];

            for(int x = 0; x < decimated.length; x++)
            {
                decimated[x] = samples[x * mDecimationFactor];
            }

            int residual = samples.length % mDecimationFactor;

            if(residual > 0)
            {
                mResidual = Arrays.copyOfRange(samples, samples.length - residual, samples.length);
            }

            if(hasChild())
            {
                return getChild().process(decimated);
            }

            return decimated;
        }
    }

    /**
     * Single, non-decimating CIC stage component.  Uses a circular buffer and a running average internally to implement
     * the stage so that stage size has essentially no impact on the computational requirements of the stage
     */
    public class NoDecimateStage extends Stage
    {
        private float[] mSampleBuffer;
        private int mSamplePointer = 0;
        private int mSize;
        protected float mSum;
        protected float mGain;

        /**
         * Constructs an instance.
         * @param size of the stage
         */
        public NoDecimateStage(int size)
        {
            mSize = size - 1;
            mSampleBuffer = new float[mSize];
            float baseGain = 1.0f / (float)size;
            mGain = baseGain * (1.0f / (1.0f - baseGain));
        }

        /**
         * Alternate constructor that specifies gain only.
         * @param gain to apply
         */
        protected NoDecimateStage(float gain)
        {
            mGain = gain;
        }

        @Override
        public float[] process(float[] samples)
        {
            float sum = mSum;
            float gain = mGain;
            float size = mSize;
            int pointer = mSamplePointer;

            for(int x = 0; x < samples.length; x++)
            {
                /* Subtract the oldest sample and add in the newest sample */
                sum -= (mSampleBuffer[pointer] + samples[x]);

                /* Overwrite the oldest sample with the newest */
                mSampleBuffer[pointer] = samples[x];
                pointer++;
                pointer %= size;
                samples[x] = (sum * gain);
            }

            mSum = sum;
            mSamplePointer = pointer;

            if(hasChild())
            {
                return getChild().process(samples);
            }

            return samples;
        }
    }

    /**
     * Size 2 stage that eliminates the unnecessary circular buffer management for a two-stage.
     */
    public class NoDecimateTwoStage extends NoDecimateStage
    {
        public NoDecimateTwoStage()
        {
            //Use gain of 1/2 or 0.5
            super(0.5f);
        }

        @Override
        public float[] process(float[] samples)
        {
            float previousSample = mSum;
            float temp;

            for(int x = 0; x < samples.length; x++)
            {
                //Calculate average of the current and previous samples
                temp = (previousSample + samples[x]) * 0.5f;
                previousSample = samples[x];
                samples[x] = temp;
            }

            mSum = previousSample;

            if(hasChild())
            {
                return getChild().process(samples);
            }

            return samples;
        }
    }


    /**
     * Output adapter - applies gain correction and cleanup filter and broadcast
     * to registered listener.
     */
    public class Output extends Stage
    {
        private IRealFilter mFilter;

        public Output(double outputSampleRate, double passFrequency, double stopFrequency) throws FilterDesignException
        {
            //This may throw an exception if we can't design a filter for the sample rate and pass/stop frequencies
            float[] coefficients = getLowPassFilter(outputSampleRate, passFrequency, stopFrequency);
            mFilter = FilterFactory.getRealFilter(coefficients);
        }

        @Override
        public float[] process(float[] samples)
        {
            if(hasChild())
            {
                return getChild().process(mFilter.filter(samples));
            }

            return mFilter.filter(samples);
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
            return designer.getImpulseResponse();
        }
    }

    public static void main(String[] args)
    {
        double sampleRate = 25.0;
        int decimation = 5;
        double finalRate = sampleRate / decimation;
        IRealOscillator oscillator = OscillatorFactory.getRealOscillator(1.0, sampleRate);

        try
        {
            PrimeCicDecimationFilter filter = new PrimeCicDecimationFilter(sampleRate, decimation, 1.5, 2.0);

            for(int x = 0; x < 20; x++)
            {
                float[] samples = oscillator.generate(5000);
                float[] decimated = filter.decimate(samples);
                System.out.println(Arrays.toString(decimated));
            }

        }
        catch(FilterDesignException fde)
        {
            fde.printStackTrace();
        }


    }
}
