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
import java.text.DecimalFormat;
import java.util.Arrays;

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

    /**
     * Constructs an instance
     *
     * @param tapCount for the equalizer, odd length.
     */
    public C4FMDecisionDirectedEqualizer(int tapCount)
    {
        tapCount = 16;
        mDelay = tapCount / 2;
        mTaps = new float[tapCount];
        mTaps[mDelay] = 1.0f;
        mDecisions = new float[tapCount];
        DF.setPositivePrefix(" ");
    }

    public void enable()
    {
        mEnabled = true;
    }

    public void disable()
    {
        mEnabled = false;
    }

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
        mDecisions[mIndex] = sample;

        float y = 0f;

        if(mEnabled)
        {
            int di = mIndex;
            for(int ti = 0; ti < mTaps.length; ti++)
            {
                y += mTaps[ti] * mDecisions[di];
                if(--di < 0)
                {
                    di = mTaps.length - 1;
                }
            }

            float error = Dibit.fromSample(y).getIdealPhase() - y;
            error = Math.clamp(error, -MAX_ERROR, MAX_ERROR);

            di = mIndex;

            for(int ti = 0; ti < mTaps.length; ti++)
            {
                mTaps[ti] += mStepSize * error * mDecisions[di];
                if(--di < 0)
                {
                    di = mTaps.length - 1;
                }
            }

                        int originalIndex = mIndex - mDelay;
                        if(originalIndex < 0)
                        {
                            originalIndex += mTaps.length;
                        }

                        float originalValue = mDecisions[originalIndex];
                        float originalError = Dibit.fromSample(originalValue).getIdealPhase() - originalValue;
                        float residualError = Dibit.fromSample(y).getIdealPhase() - y;
                        float meanResidual = mMeanResidualError.get(residualError);
                        System.out.println("IN: " + DF.format(mDecisions[originalIndex]) +
                            " OUT: " + DF.format(y) +
                            " ORIG ERROR:" + DF.format(originalError) +
                            " RESIDUAL:" + DF.format(residualError) +
                            " MEAN:" + DF.format(meanResidual));
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

        return y;
    }
}
