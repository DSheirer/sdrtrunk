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
import java.util.Collections;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

/**
 * Adaptive equalizer for C4FM symbols.
 */
public class C4FMFractionalDecisionDirectedEqualizer
{
    private static final DecimalFormat DF = new DecimalFormat("0.0000");
    //Maximum possible error is PI/4 = 0.78, but we clip it to less than half that as the allowable error for tap updates
    private static final float MAX_ERROR = 0.3f;
    private float[] mTaps;
    private float[] mDecisions;
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

    /**
     * Constructs an instance
     *
     * @param tapCount for the equalizer, odd length.
     */
    public C4FMFractionalDecisionDirectedEqualizer(int tapCount)
    {
        NXDNStandardSyncDetector sd = new NXDNStandardSoftSyncDetectorScalar();
        mSyncPattern = sd.getSyncSymbols();
        mSyncDibits = sd.getSyncDibits();
        Collections.reverse(Arrays.asList(mSyncDibits));
        //Make it odd length
        tapCount = (tapCount % 2 == 1) ? tapCount : tapCount + 1;
        mDelay = tapCount;
        tapCount *= 2;
        mTaps = new float[tapCount];
        mTaps[mDelay - 1] = 1.0f;
        mTrainingSnapshot = Arrays.copyOf(mTaps, tapCount);
        mDecisions = new float[tapCount];
        DF.setPositivePrefix(" ");
        int bufferLength = (mSyncPattern.length * 2) + (2 * mDelay);
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

            float[] history = mBuffer.getAllReversed();
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
                for(symbolIndex = 0; symbolIndex < mSyncPattern.length; symbolIndex++)
                {
                    y = 0.0f;
                    for(index = 0; index < mTaps.length; index++)
                    {
                        y += mTaps[index] * history[index + symbolIndex];
                    }

                    error = mSyncDibits[symbolIndex].getIdealPhase() - y;
                    variance.increment(error);

                    for(index = 0; index < mTaps.length; index++)
                    {
                        mTaps[index] += 0.01f * error * history[index + symbolIndex];
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
        //Shift decisions to the right by 2 places to add new fractional and symbol.
        System.arraycopy(mDecisions, 0, mDecisions, 2, mDecisions.length - 2);
        mDecisions[1] = sample;
        mBuffer.put(sample);
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
        mDecisions[0] = sample;

        float y = 0f;
        int index;

        if(mEnabled)
        {
            for(index = 0; index < mTaps.length; index++)
            {
                y += mTaps[index] * mDecisions[index];
            }

            if(mDynamicTapUpdates)
            {
                float error = Dibit.getError(y);
                error = Math.clamp(error, -MAX_ERROR, MAX_ERROR);

                for(index = 0; index < mTaps.length; index++)
                {
                    mTaps[index] += mStepSize * error * mDecisions[index];
                }
            }

            float originalValue = mDecisions[mDelay];
            float originalError = Dibit.getError(originalValue);
            float residualError = Dibit.getError(y);
            float meanResidual = mMeanResidualError.get(residualError);
            System.out.println("IN: " + DF.format(mDecisions[mDelay]) +
                " OUT: " + DF.format(y) +
                " ORIG ERROR:" + DF.format(originalError) +
                " RESIDUAL:" + DF.format(residualError) +
                " MEAN:" + DF.format(meanResidual) +
                (mDynamicTapUpdates ? " ** DYNAMIC TAP UPDATE" : ""));

            mVariance.increment(residualError);
        }
        else
        {
            y = mDecisions[mDelay];
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
