/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package dsp.filter.cic;

import dsp.filter.FilterFactory;
import dsp.filter.Filters;
import dsp.filter.Window.WindowType;
import dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import dsp.filter.halfband.complex.HalfBandFilter_CB_CB;
import org.apache.commons.lang3.Validate;
import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.ComplexSampleListener;
import sample.complex.ComplexToComplexBufferAssembler;
import sample.decimator.ComplexDecimator;

import java.util.ArrayList;
import java.util.List;

public class ComplexPrimeCICDecimate implements Listener<ComplexBuffer>
{
    public static int[] PRIMES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53,
        59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151,
        157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251,
        257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359,
        367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463,
        467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593,
        599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691};

    private ArrayList<DecimatingStage> mDecimatingStages =
        new ArrayList<DecimatingStage>();

    private DecimatingStage mFirstDecimatingStage;

    private Output mOutput;

    /**
     * Non-Recursive Prime-Factor CIC Filter with float sample array inputs and
     * decimated, single paired i/q float sample output.
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
    public ComplexPrimeCICDecimate(int decimation, int order,
                                   int passFrequency, int attenuation, WindowType windowType)
    {
        Validate.isTrue(decimation <= 700);


        List<Integer> stageSizes = getPrimeFactors(decimation);

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

        mOutput = new Output(48000, passFrequency, attenuation, windowType);

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
    public void setListener(Listener<ComplexBuffer> listener)
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
     * Calculates the prime factors of the decimation rate up to a maximum
     * decimation rate of 700.  If you wish to have decimation rates higher
     * than 700, then add additional prime factors to the PRIMES array.
     *
     * @param decimation - integral decimation rate
     * @return - ordered list (smallest to largest) of prime factors
     */
    public static List<Integer> getPrimeFactors(int decimation)
    {
        ArrayList<Integer> stages = new ArrayList<Integer>();

        int pointer = 0;

        while(decimation > 0 && pointer < PRIMES.length)
        {
            int prime = PRIMES[pointer];

            if(decimation % prime == 0)
            {
                stages.add(prime);

                decimation /= prime;
            }
            else
            {
                pointer++;
            }
        }

        return stages;
    }

    /**
     * Primary input method for receiving sample arrays composed as I,Q,I,Q, etc.
     */
    @Override
    public void receive(ComplexBuffer buffer)
    {
        if(mFirstDecimatingStage != null)
        {
            float[] samples = buffer.getSamples();

            for(int x = 0; x < samples.length; x += 2)
            {
                mFirstDecimatingStage.receive(samples[x], samples[x + 1]);
            }
        }
    }

    /**
     * Decimating stage combines multiple CIC stages with a decimator.  The
     * number of stages is indicated by the order value and the size indicates
     * the decimation rate of this stage.
     */
    public class DecimatingStage implements ComplexSampleListener
    {
        private ArrayList<Stage> mStages = new ArrayList<Stage>();
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
     * Single non-decimating CIC stage component.  Uses a circular buffer and a
     * running average internally to implement the stage so that stage size has
     * essentially no impact on the computational requirements of the stage
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

        public Stage()
        {
        }

        public Stage(int size)
        {
            mSize = size - 1;

            mISamples = new float[mSize];
            mQSamples = new float[mSize];

            mGain = 1.0f / (float)size;
        }

        public void dispose()
        {
            mListener = null;
        }

        public void receive(float i, float q)
        {
			/* Subtract the oldest sample and add back in the newest */
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

        public void receive(float i, float q)
        {
            float iSum = mISum + i;
            float qSum = mQSum + q;

            mISum = i;
            mQSum = q;

            if(mListener != null)
            {
                mListener.receive((iSum * mGain), (qSum * mGain));
            }
        }
    }


    /**
     * Output adapter - applies gain correction and cleanup filter and broadcast
     * to registered listener.
     */
    public class Output implements ComplexSampleListener
    {
        /* Decimated output buffers will contain 1024 samples */
        private ComplexToComplexBufferAssembler mAssembler =
            new ComplexToComplexBufferAssembler(2048);

        private ComplexFIRFilter_CB_CB mCleanupFilter;
        private HalfBandFilter_CB_CB mHalfBandFilter = new HalfBandFilter_CB_CB(
            Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO.getCoefficients(), 0.4f, false);

        public Output(int outputSampleRate, int passFrequency, int attenuation,
                      WindowType windowType)
        {
            mCleanupFilter = new ComplexFIRFilter_CB_CB(FilterFactory
                .getCICCleanupFilter(outputSampleRate,
                    passFrequency,
                    attenuation,
                    windowType), 0.4f);

            //Bypassing the CIC cleanup filter for now
            mAssembler.setListener(mHalfBandFilter);

//			mAssembler.setListener( mCleanupFilter );
//			mCleanupFilter.setListener( mHalfBandFilter );
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
        public void receive(float inphase, float quadrature)
        {
            mAssembler.receive(inphase, quadrature);
        }

        /**
         * Adds a listener to receive output samples
         */
        public void setListener(Listener<ComplexBuffer> listener)
        {
            mHalfBandFilter.setListener(listener);
        }

        /**
         * Removes the listener from receiving output samples
         */
        public void removeListener()
        {
            mHalfBandFilter.removeListener();
        }
    }
}
