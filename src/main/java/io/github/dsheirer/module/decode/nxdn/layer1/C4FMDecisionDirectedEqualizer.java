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

package io.github.dsheirer.module.decode.nxdn.layer1;

import io.github.dsheirer.buffer.FloatAveragingBuffer;
import io.github.dsheirer.buffer.FloatCircularBuffer;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorScalar;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSyncDetector;
import java.text.DecimalFormat;
import java.util.Arrays;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

/**
 * Adaptive equalizer for C4FM symbols.
 */
public class C4FMDecisionDirectedEqualizer
{
    private static final DecimalFormat DF = new DecimalFormat("0.0000");
    //Maximum possible error is PI/4 = 0.78, but we clip it to less than half that as the allowable error for tap updates
    private static final float MAX_ERROR = 0.3f;
    private float[] mTaps;
    private float[] mDecisions;
    private int mIndex = 0;
    private float mStepSize = 0.001f; //mu in range 0.0001 to 0.01
    private boolean mEnabled = false;
    private int mDelay = 0;
    private FloatAveragingBuffer mMeanResidualError = new FloatAveragingBuffer(30, 10);

    private FloatCircularBuffer mBuffer;
    private Variance mVariance = new Variance();
    private float[] mTrainingSnapshot;
    private float[] mSyncPattern;
    private Dibit[] mSyncDibits;
    private boolean mSyncDetected = false;
    private int mTrainingSymbolsRemaining = 0;
    private boolean mDynamicTapUpdates = false;
    private boolean mFractionalSpacing = false;

    /**
     * Constructs an instance
     *
     * @param tapCount for the equalizer, odd length.
     */
    public C4FMDecisionDirectedEqualizer(int tapCount, boolean fractionalSpaced)
    {
        mFractionalSpacing = fractionalSpaced;
        /**
         * TODO: need a flag to indicate if we're fractional spaced or not, and then we can either double the
         * tap count or not.  Then the training sequence can know if it has captured fractional samples or
         * not and train accordingly.
         */
        NXDNStandardSyncDetector sd = new NXDNStandardSoftSyncDetectorScalar();
        mSyncPattern = sd.getSyncSymbols();
        mSyncDibits = sd.getSyncDibits();
        //Make it odd length
        tapCount = (tapCount % 2 == 1) ? tapCount : tapCount + 1;

        if(fractionalSpaced)
        {
            tapCount *= 2;
        }

        mDelay = tapCount / 2;
        mTaps = new float[tapCount];

//        if(mFractionalSpacing)
//        {
//            mTaps[mDelay - 1] = 1.0f;
//        }
//        else
//        {
            mTaps[mDelay] = 1.0f;
//        }
        mTrainingSnapshot = Arrays.copyOf(mTaps, tapCount);
        mDecisions = new float[tapCount];
        DF.setPositivePrefix(" ");

        int bufferLength = fractionalSpaced ? (mSyncPattern.length * 2) : mSyncPattern.length;

        if(fractionalSpaced)
        {
            bufferLength += (2 * (mDelay - 1));
        }
        else
        {
            bufferLength += (2 * mDelay);
        }
        mBuffer = new FloatCircularBuffer(bufferLength);
    }

    /**
     * Enable the equalizer when a valid sync pattern is detected
     */
    public void enable()
    {
        mEnabled = true;
    }

    /**
     * Disable the equalizer when the signal is lost so that noise doesn't contaminate the equalizer taps.
     */
    public void disable()
    {
        mEnabled = false;
    }

    /**
     * Notifies the equalizer that a sync pattern is detected and queues a training sequence once a couple more
     * symbols arrive once the equalizer delay line has aligned to output the sync pattern.
     */
    public void syncDetected()
    {
        mSyncDetected = true;
        mTrainingSymbolsRemaining = mDelay;
        System.out.println("Training mode countdown started ....");
    }

    public void syncLost()
    {
        //TODO: handle this.
    }

    private void train()
    {
        mSyncDetected = false;

        if(mVariance.getN() <= mDelay || mVariance.getResult() > 0.05)
        {
            if(mVariance.getResult() > 0.225)
            {
                mTaps = Arrays.copyOf(mTrainingSnapshot, mTrainingSnapshot.length);
                System.out.println("Starting Training Taps: " + Arrays.toString(mTaps) + " Current Variance: " + mVariance.getResult() + " >> REUSED PREVIOUS TRAINING <<");
            }
            else
            {
                System.out.println("Starting Training Taps: " + Arrays.toString(mTaps) + " Current Variance: " + mVariance.getResult() + " ### GOOD DYNAMICS ###");
            }

            float[] history = mBuffer.getAll();
            float y, error;
            int historyIndex, symbolIndex, tapIndex;

            Variance variance = new Variance();

            int attempt = 0;
            double previousVariance = 1.0;
            double improvement;

            int symbolIndexMultiplier = mFractionalSpacing ? 2 : 1;

            do
            {
                previousVariance = variance.getN() == 0 ? 20.0 : variance.getResult();
                variance.clear();
                for(symbolIndex = 0; symbolIndex < mSyncPattern.length; symbolIndex++)
                {
                    y = 0.0f;
                    historyIndex = mDecisions.length - 1 + (symbolIndex * symbolIndexMultiplier);

                    for(tapIndex = 0; tapIndex < mTaps.length; tapIndex++)
                    {
                        y += mTaps[tapIndex] * history[historyIndex - tapIndex];
                    }

                    error = mSyncDibits[symbolIndex].getIdealPhase() - y;
                    variance.increment(error);

                    historyIndex = mDecisions.length - 1 + (symbolIndex * symbolIndexMultiplier);

                    for(tapIndex = 0; tapIndex < mTaps.length; tapIndex++)
                    {
                        mTaps[tapIndex] += 0.01f * error * history[historyIndex - tapIndex];
                    }
                }

                System.out.println("Attempt " + attempt + " Variance:" + DF.format(variance.getResult()));
                attempt++;

                improvement = previousVariance - variance.getResult();
            }
            while(attempt < 50 && improvement > 0.001);

            System.out.println("Training against sync sequence complete");

            //Use the trained taps if the reduce the variance, otherwise revert to the previous trained taps.
            if(variance.getResult() < 0.15)
            {
                mTrainingSnapshot = Arrays.copyOf(mTaps, mTaps.length);
                System.out.println("TAPS: " + Arrays.toString(mTaps) + " ***UPDATED WITH THIS TRAINING ITERATION***");
                mDynamicTapUpdates = true;
            }
            else
            {
                mTaps = Arrays.copyOf(mTrainingSnapshot, mTrainingSnapshot.length);
                System.out.println("TAPS: " + Arrays.toString(mTaps) + " ### REVERTED ###");
                mDynamicTapUpdates = false;
            }
        }
        else if(mVariance.getN() >= mDelay && mVariance.getResult() < 0.05)
        {
            System.out.println("Skipping retrain - variance is acceptable");
            //Snapshot the current taps as the training sequence
            mTrainingSnapshot = Arrays.copyOf(mTaps, mTaps.length);
            mDynamicTapUpdates = true;
        }

        System.out.println("*** Sync Detect Training Complete - Variance: " + mVariance.getResult() + " Count:" + mVariance.getN());
        mVariance.clear();
    }

    /*
     * Resets the equalizer taps
     */
    private void reset()
    {
        Arrays.fill(mTaps, 0f);
        mTaps[2] = 1.0f;
    }

    /**
     * Inserts an optional, fractionally spaced sample into the delay queue.  Note: this is an insert only operation
     * and the taps are not updated until the next symbol is processed.
     * @param sample that is fractionally spaced between symbol periods.
     */
    public void processSample(float sample)
    {
        mDecisions[mIndex] = sample;
        mIndex = ++mIndex % mTaps.length;
    }

    /**
     * Processes a soft sample (ie symbol), updates the equalizer taps, and returns the equalized symbol.
     *
     * @param sample to equalizer=
     * @return equalized symbol.
     */
    public float processSymbol(float sample)
    {
        mBuffer.put(sample);
        mDecisions[mIndex] = sample;

        float y = 0f;

        if(mEnabled)
        {
            int decisionIndex = mIndex;
            for(int ti = 0; ti < mTaps.length; ti++)
            {
                y += mTaps[ti] * mDecisions[decisionIndex];
                if(--decisionIndex < 0)
                {
                    decisionIndex = mTaps.length - 1;
                }
            }

            if(mDynamicTapUpdates)
            {
                float error = Dibit.getError(y);
                error = Math.clamp(error, -MAX_ERROR, MAX_ERROR);

                decisionIndex = mIndex;

                for(int tapIndex = 0; tapIndex < mTaps.length; tapIndex++)
                {
                    mTaps[tapIndex] += mStepSize * error * mDecisions[decisionIndex];
                    if(--decisionIndex < 0)
                    {
                        decisionIndex = mTaps.length - 1;
                    }
                }
            }

            int originalIndex = mIndex - mDelay;
            if(originalIndex < 0)
            {
                originalIndex += mTaps.length;
            }

            float originalValue = mDecisions[originalIndex];
            float originalError = Dibit.getError(originalValue);
            float residualError = Dibit.getError(y);
            float meanResidual = mMeanResidualError.get(residualError);
            System.out.println("IN: " + DF.format(mDecisions[originalIndex]) +
                " OUT: " + DF.format(y) +
                " ORIG ERROR:" + DF.format(originalError) +
                " RESIDUAL:" + DF.format(residualError) +
                " MEAN:" + DF.format(meanResidual) +
                (mDynamicTapUpdates ? " ** DYNAMIC TAP UPDATE" : ""));

            mVariance.increment(residualError);
        }
        else
        {
            int previous = mIndex - 1;

            if(previous < 0)
            {
                previous = mTaps.length - 1;
            }

            y = mDecisions[previous];
        }

        mIndex = ++mIndex % mTaps.length;

        if(mSyncDetected)
        {
            if(--mTrainingSymbolsRemaining <= 0)
            {
                train();
            }
        }

        return y;
    }
}
