/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package io.github.dsheirer.dsp.filter.cic;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Filters;
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter2;
import io.github.dsheirer.dsp.filter.halfband.real.HalfBandFilter;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableBufferAssembler;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import io.github.dsheirer.sample.decimator.RealDecimator;
import io.github.dsheirer.sample.real.RealSampleListener;
import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.primes.Primes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RealPrimeCICDecimate
{
    private final static Logger mLog = LoggerFactory.getLogger(RealPrimeCICDecimate.class);

    private List<DecimatingStage> mDecimatingStages = new ArrayList<DecimatingStage>();
    private DecimatingStage mFirstDecimatingStage;
    private Output mOutput;

    private int mOutputBufferSize = 2048;

    /**
     * Non-Recursive Prime-Factor CIC Filter with float sample array inputs and
     * decimated float sample output.
     *
     * Implements the CIC filter described in Understanding Digital Signal
     * Processing, 3e, Lyons, on page 769.  This filter is comprised of multiple
     * decimating stages each with a prime factor decimation rate.  Multiple
     * stages are cascaded to achieve the overall decimation rate.
     *
     * This filter supports a maximum decimation rate of 700.  This filter can
     * be adapted to higher decimation rates by adding additional prime factors
     * to the PRIMES array.
     *
     * @param decimation - overall decimation rate
     * @param order - filter order
     */
    public RealPrimeCICDecimate(int decimation, int order, int passFrequency, int attenuation, WindowType windowType)
    {
        this(decimation, order, passFrequency, attenuation, windowType, 2048);
    }

    public RealPrimeCICDecimate(int decimation, int order, int passFrequency, int attenuation, WindowType windowType, int outputBufferSize)
    {
        Validate.isTrue(decimation <= 700);

        mOutputBufferSize = outputBufferSize;

        List<Integer> stageSizes = Primes.primeFactors(decimation);

        for(int x = 0; x < stageSizes.size(); x++)
        {
            DecimatingStage stage = new DecimatingStage(stageSizes.get(x), order);

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

        mOutput = new Output(48000, passFrequency, attenuation, windowType,
            outputBufferSize);

        mDecimatingStages.get(mDecimatingStages.size() - 1)
            .setListener(mOutput);
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
    public void setListener(Listener<ReusableFloatBuffer> listener)
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
     * Primary input method for receiving real buffers.
     */
    public void receive(ReusableFloatBuffer buffer)
    {
        if(mFirstDecimatingStage != null)
        {
            for(float sample : buffer.getSamples())
            {
                mFirstDecimatingStage.receive(sample);
            }
        }

        buffer.decrementUserCount();
    }

    /**
     * Decimating stage combines multiple CIC stages with a decimator.  The
     * number of stages is indicated by the order value and the size indicates
     * the decimation rate of this stage.
     */
    public class DecimatingStage implements RealSampleListener
    {
        private ArrayList<Stage> mStages = new ArrayList<Stage>();
        private Stage mFirstStage;
        private RealDecimator mDecimator;

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

            mDecimator = new RealDecimator(size);

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
        public void receive(float sample)
        {
            mFirstStage.receive(sample);
        }

        public void setListener(RealSampleListener listener)
        {
            mDecimator.setListener(listener);
        }
    }

    /**
     * Single non-decimating CIC stage component.  Uses a circular buffer and a
     * running average internally to implement the stage so that stage size has
     * essentially no impact on the computational requirements of the stage
     */
    public class Stage implements RealSampleListener
    {
        protected RealSampleListener mListener;

        private float[] mSamples;

        protected float mSum;

        private int mSamplePointer = 0;
        private int mSize;

        protected float mGain;

        public Stage()
        {
        }

        public Stage(int size)
        {
            mSize = size - 1;

            mSamples = new float[mSize];

            mGain = 1.0f / (float)size;
        }

        public void dispose()
        {
            mListener = null;
        }

        public void receive(float sample)
        {
            /* Subtract the oldest sample and add back in the newest */
            mSum = mSum - mSamples[mSamplePointer] + sample;

            /* Overwrite the oldest sample with the newest */
            mSamples[mSamplePointer] = sample;

            mSamplePointer++;

            if(mSamplePointer >= mSize)
            {
                mSamplePointer = 0;
            }

            if(mListener != null)
            {
                mListener.receive(mSum * mGain);
            }
        }

        public void setListener(RealSampleListener listener)
        {
            mListener = listener;
        }
    }

    /**
     * Size 2 stage that removes the unnecessary circular buffer management for
     * a two-stage.
     */
    public class TwoStage extends Stage
    {
        public TwoStage()
        {
            super();

            mGain = 0.5f;
        }

        public void receive(float sample)
        {
            float sum = mSum + sample;

            mSum = sample;

            if(mListener != null)
            {
                mListener.receive(sum * mGain);
            }
        }
    }

    /**
     * Output adapter - applies gain correction, applies cleanup filter, casts
     * double-valued samples into float-valued complex sample and sends the
     * result to the registered listener.
     */
    public class Output implements RealSampleListener
    {
        /* Decimated output buffers will contain 1024 samples */
        private ReusableBufferAssembler mAssembler;
        private Listener<ReusableFloatBuffer> mReusableBufferListener;

        private RealFIRFilter2 mCleanupFilter;
        private HalfBandFilter mHalfBandFilter = new HalfBandFilter(
            Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO.getCoefficients(), 0.4f, false);

        public Output(int outputSampleRate, int passFrequency, int attenuation, WindowType windowType, int outputBufferSize)
        {
            mCleanupFilter = new RealFIRFilter2(FilterFactory.getCICCleanupFilter(outputSampleRate, passFrequency,
                attenuation, windowType), 0.4f);

            mAssembler = new ReusableBufferAssembler(outputBufferSize);
            mAssembler.setListener(new Listener<ReusableFloatBuffer>()
            {
                @Override
                public void receive(ReusableFloatBuffer reusableFloatBuffer)
                {

                    if(mReusableBufferListener != null)
                    {
                        ReusableFloatBuffer cleanupFiltered = mCleanupFilter.filter(reusableFloatBuffer);
                        ReusableFloatBuffer halfBandFiltered = mHalfBandFilter.filter(cleanupFiltered);
                        mReusableBufferListener.receive(halfBandFiltered);
                    }
                    else
                    {
                        reusableFloatBuffer.decrementUserCount();
                    }
                }
            });
        }

        public void dispose()
        {
            mAssembler.dispose();
            mCleanupFilter.dispose();
            mHalfBandFilter.dispose();
        }

        /**
         * Receiver method for the output adapter to receive a filtered,
         * decimated sample, apply gain correction, apply cleanup filtering
         * and output the sample values as a complex sample.
         */
        @Override
        public void receive(float sample)
        {
            mAssembler.receive(sample * 32.0f);
        }

        /**
         * Adds a listener to receive output samples
         */
        public void setListener(Listener<ReusableFloatBuffer> listener)
        {
            mReusableBufferListener = listener;
        }

        /**
         * Removes the listener from receiving output samples
         */
        public void removeListener()
        {
            mReusableBufferListener = null;
        }
    }
}
