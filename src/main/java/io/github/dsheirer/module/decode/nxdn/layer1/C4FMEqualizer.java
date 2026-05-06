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
import io.github.dsheirer.dsp.symbol.Dibit;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Adaptive equalizer for C4FM symbols.
 */
public class C4FMEqualizer
{
    private static final DecimalFormat DF = new DecimalFormat("0.0000");
    //Maximum possible error is PI/4 = 0.78, but we clip it to less than half that as the allowable error for tap updates
    private static final float MAX_SYMBOL_ERROR = 0.3f;
    private static final double TRAINING_IMPROVEMENT_GOAL_PER_ITERATION = 0.001f;
    private static final double TRAINING_IMPROVEMENT_THRESHOLD = 0.16f;
    private static final double TRAINING_SKIP_THRESHOLD = 0.05;
    private static final int TRAINING_ITERATIONS_MAX = 50;
    private float[] mTaps;
    private float[] mBuffer;
    private float mStepSize = 0.01f; //mu in range 0.0001 to 0.01
    private boolean mEnabled = false;
    private int mDelay = 0;
    private int mSyncDelay;
    private FloatAveragingBuffer mMeanResidualError = new FloatAveragingBuffer(30, 10);

    private Variance mVariance = new Variance();
    private float[] mTrainingSnapshot;
    private float[] mSyncSymbols;
    private boolean mSyncDetected = false;
    private int mTrainingSymbolsRemaining = 0;
    private boolean mDynamicTapUpdates = false;

    private int mTrainingCount = 0;

    /**
     * Constructs an instance
     *
     * @param tapCount for the equalizer, odd length.
     */
    public C4FMEqualizer(float[] syncSymbols, int tapCount)
    {
        mSyncSymbols = new float[syncSymbols.length];
        //Store the sync pattern in reverse order
        for(int x = 0; x < syncSymbols.length; x++)
        {
            mSyncSymbols[syncSymbols.length - x - 1] = syncSymbols[x];
        }

        //Make the tap count odd length for equal delay on either side of the main tap
        tapCount = (tapCount % 2 == 1) ? tapCount : tapCount + 1;
        mDelay = tapCount; //Use tap count as delay before we double it
        mSyncDelay = tapCount / 2; //Symbol countdown from sync detection to sync symbols centered in the buffer
        tapCount *= 2;
        mTaps = new float[tapCount];
        mTaps[mDelay] = 1.0f; //Center tap
        mTrainingSnapshot = Arrays.copyOf(mTaps, tapCount);
        DF.setPositivePrefix(" ");
        int bufferLength = mSyncSymbols.length * 2 + mTaps.length - 2;
        mBuffer = new float[bufferLength];
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
        mTrainingSymbolsRemaining = mSyncDelay;
        System.out.println("Training mode countdown started ....");
    }

    public void syncLost()
    {
        //TODO: handle this.
    }

    private void train()
    {
        mSyncDetected = false;

        //DEBUG - skip the first training session
        mTrainingCount++;
        if(mTrainingCount < 2)
        {
            return;
        }

        if(mVariance.getN() <= mSyncDelay || mVariance.getResult() > 0.05)
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

            float[] history = mBuffer;
            float y, error;
            int symbolIndex, index;

            Variance variance = new Variance();

            int attempt = 0;
            double previousVariance = 1.0;
            double improvement;

            do
            {
                previousVariance = variance.getN() == 0 ? 20.0 : variance.getResult();
                variance.clear();
                for(symbolIndex = 0; symbolIndex < mSyncSymbols.length; symbolIndex++)
                {
                    y = 0.0f;
                    for(index = 0; index < mTaps.length; index++)
                    {
                        y += mTaps[index] * history[index + (2 * symbolIndex)];
                    }

                    error = mSyncSymbols[symbolIndex] - y;
                    System.out.println("\tAttempt " + attempt + " Symbol:" + symbolIndex +
                            " Value:" + mSyncSymbols[symbolIndex] + " Y:" + y + " Error:" + error);
                    variance.increment(error);

                    for(index = 0; index < mTaps.length; index++)
                    {
                        mTaps[index] += 0.01f * error * history[index + (2 * symbolIndex)];
                    }
                }

                System.out.println("Attempt " + attempt + " Variance:" + DF.format(variance.getResult()));
                attempt++;

                improvement = previousVariance - variance.getResult();
            }
            while(attempt < TRAINING_ITERATIONS_MAX && improvement > TRAINING_IMPROVEMENT_GOAL_PER_ITERATION);

            System.out.println("Training against sync sequence complete");

            //Use the trained taps if they converge, else revert to the previous trained taps.
            if(variance.getResult() < TRAINING_IMPROVEMENT_THRESHOLD)
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
        else if(mVariance.getN() >= mSyncDelay && mVariance.getResult() < TRAINING_SKIP_THRESHOLD)
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
        mTaps[mDelay] = 1.0f;
    }

    /**
     * Inserts a fractionally spaced sample into the delay queue.  Note: this is an insert only operation
     * and the taps are not updated until the next symbol is processed.
     * @param sample that is fractionally spaced between symbol periods.
     */
    public void processSample(float sample)
    {
        //Shift buffer samples to the right by 2 places to add new fractional sample and symbol.
        System.arraycopy(mBuffer, 0, mBuffer, 2, mBuffer.length - 2);
        mBuffer[0] = sample;
    }

    /**
     * Processes a soft sample (ie symbol), updates the equalizer taps, and returns the equalized symbol.
     *
     * @param sample to equalizer=
     * @return equalized symbol.
     */
    public float processSymbol(float sample)
    {
        mBuffer[1] = sample;

        float y = 0f;
        int index;

        if(mEnabled)
        {
            for(index = 0; index < mTaps.length; index++)
            {
                y += mTaps[index] * mBuffer[index];
            }

            if(mDynamicTapUpdates)
            {
                float error = Dibit.getError(y);
                error = Math.clamp(error, -MAX_SYMBOL_ERROR, MAX_SYMBOL_ERROR);

                for(index = 0; index < mTaps.length; index++)
                {
                    mTaps[index] += mStepSize * error * mBuffer[index];
                }
            }

            float originalValue = mBuffer[mDelay];
            float originalError = Dibit.getError(originalValue);
            float residualError = Dibit.getError(y);
            float meanResidual = mMeanResidualError.get(residualError);
            System.out.println("IN: " + DF.format(mBuffer[mDelay]) +
                " OUT: " + DF.format(y) +
                " ORIG ERROR:" + DF.format(originalError) +
                " RESIDUAL:" + DF.format(residualError) +
                " MEAN:" + DF.format(meanResidual) +
                (mDynamicTapUpdates ? " ** DYNAMIC TAP UPDATE" : ""));

            mVariance.increment(residualError);
        }
        else
        {
            y = mBuffer[mDelay];
        }

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
