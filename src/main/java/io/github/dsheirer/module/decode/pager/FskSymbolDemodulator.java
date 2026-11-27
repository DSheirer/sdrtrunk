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

package io.github.dsheirer.module.decode.pager;

/**
 * Symbol timing recovery and level normalization for 2-level and 4-level FSK paging signals in FM demodulated
 * (discriminator) audio.
 *
 * Symbol timing is recovered from the zero crossings of the signal, which occur at the symbol boundaries.  Each
 * zero crossing nudges the symbol clock toward the boundary.  The symbol is sampled at the mid-point between
 * boundaries, using linear interpolation between the audio samples.
 *
 * The DC offset (from tuning error) and the amplitude (from deviation and demodulator gain) are tracked from the
 * sampled symbol values so that the emitted symbols are centered on zero and normalized so that the outer levels are
 * near +/- 1.0.  For 4-level signals the inner levels are near +/- 0.333.
 */
public class FskSymbolDemodulator
{
    private static final float LEVEL_GAIN = 0.05f;
    private static final float OUTER_LEVEL_THRESHOLD = 0.667f;
    private static final float MINIMUM_ENVELOPE = 1E-6f;
    private static final double TIMING_GAIN = 0.06;
    private static final float SYMMETRIC_TRANSITION_THRESHOLD = 0.4f;

    private final double mSampleRate;
    private final ISymbolListener mListener;
    private final float[] mAverageBuffer;
    private int mAveragePointer;
    private float mAverageSum;
    private double mSymbolRate;
    private double mPhaseIncrement;
    private double mPhase;
    private boolean mSymbolEmitted;
    private int mLevels = 2;
    private float mHigh;
    private float mLow;
    private float mPreviousSample;
    private float mPreviousCentered;
    private float mPreviousSymbol;
    private double mPendingTimingError;
    private boolean mPendingTimingErrorAvailable;

    /**
     * Constructs an instance
     * @param sampleRate of the demodulated audio
     * @param symbolRate initial symbol rate
     * @param averageLength of the moving average filter applied to the audio to reduce noise.  Use 1 for no filter.
     * Use approximately half the samples per symbol at low symbol rates.
     * @param listener to receive normalized symbols
     */
    public FskSymbolDemodulator(double sampleRate, double symbolRate, int averageLength, ISymbolListener listener)
    {
        mSampleRate = sampleRate;
        mAverageBuffer = new float[Math.max(1, averageLength)];
        mListener = listener;
        setSymbolRate(symbolRate);
        reset();
    }

    /**
     * Current symbol rate.
     */
    public double getSymbolRate()
    {
        return mSymbolRate;
    }

    /**
     * Changes the symbol rate while preserving symbol boundary alignment.  This is intended to be called from the
     * symbol listener immediately after a symbol is emitted, for a rate change by a factor of 2 (as used by FLEX).
     * When the rate increases, the
     * first faster symbol starts at the end of the current (just emitted) slower symbol.  When the rate decreases by
     * an integer factor, the current (just emitted) faster symbol is assumed to be the last faster symbol within the
     * slower symbol, which is the case when an even number of faster symbols was received.
     *
     * @param symbolRate to use
     */
    public void setSymbolRate(double symbolRate)
    {
        if(symbolRate * 2.5 > mSampleRate)
        {
            throw new IllegalArgumentException("Symbol rate [" + symbolRate + "] requires at least 2.5 samples per " +
                "symbol at sample rate [" + mSampleRate + "]");
        }

        if(mSymbolRate > 0 && symbolRate != mSymbolRate)
        {
            double ratio = symbolRate / mSymbolRate;

            if(ratio > 1.0)
            {
                //The remainder of the slower symbol (just emitted) must not produce a faster symbol, so the current
                //faster symbol is flagged as emitted and the first faster symbol is emitted after the next boundary.
                mPhase = (mPhase * ratio) % 1.0;
                mSymbolEmitted = true;
            }
            else
            {
                double factor = mSymbolRate / symbolRate;
                mPhase = ((mPhase + factor - 1.0) / factor) % 1.0;
                mSymbolEmitted = mPhase >= 0.5;
            }
        }

        mSymbolRate = symbolRate;
        mPhaseIncrement = mSymbolRate / mSampleRate;
    }

    /**
     * Sets the number of signalling levels (2 or 4) which controls how the outer level amplitude is tracked.
     */
    public void setLevels(int levels)
    {
        mLevels = levels;
    }

    /**
     * Resets the timing, level and filter state, for example when the audio stream is interrupted.
     */
    public void reset()
    {
        mPhase = 0.0;
        mSymbolEmitted = false;
        mHigh = 0.0f;
        mLow = 0.0f;
        mPreviousSample = 0.0f;
        mPreviousCentered = 0.0f;
        mPreviousSymbol = 0.0f;
        mPendingTimingErrorAvailable = false;
        mAverageSum = 0.0f;
        mAveragePointer = 0;
        java.util.Arrays.fill(mAverageBuffer, 0.0f);
    }

    /**
     * Processes a buffer of demodulated audio samples.
     */
    public void process(float[] samples)
    {
        for(float sample: samples)
        {
            process(sample);
        }
    }

    /**
     * Processes a single demodulated audio sample.
     */
    public void process(float sample)
    {
        float filtered = filter(sample);
        float center = (mHigh + mLow) * 0.5f;
        float centered = filtered - center;

        double previousPhase = mPhase;
        double phase = previousPhase + mPhaseIncrement;

        //Timing error from zero crossings that should be aligned with symbol boundaries (integer phase values)
        if((centered >= 0.0f) != (mPreviousCentered >= 0.0f))
        {
            double fraction = mPreviousCentered / (mPreviousCentered - centered);
            double crossing = previousPhase + fraction * mPhaseIncrement;
            double error = crossing - Math.rint(crossing);

            if(mLevels == 2)
            {
                phase -= TIMING_GAIN * error;
            }
            else
            {
                //For 4-level, a crossing is only aligned with the symbol boundary when the levels on either side are
                //symmetric about zero (e.g. +1 to -1, or +1/3 to -1/3).  The error is held until the next symbol is
                //decided, and only applied when the transition was symmetric.
                mPendingTimingError = error;
                mPendingTimingErrorAvailable = true;
            }
        }

        boolean emit = false;
        float symbol = 0.0f;

        if(!mSymbolEmitted && phase >= 0.5)
        {
            double fraction = (0.5 - previousPhase) / (phase - previousPhase);
            fraction = Math.max(0.0, Math.min(1.0, fraction));
            symbol = (float)(mPreviousSample + fraction * (filtered - mPreviousSample));
            mSymbolEmitted = true;
            emit = true;
        }

        if(phase >= 1.0)
        {
            phase -= 1.0;
            mSymbolEmitted = false;
        }
        else if(phase < 0.0)
        {
            phase += 1.0;
            mSymbolEmitted = true;
        }

        mPhase = phase;
        mPreviousSample = filtered;
        mPreviousCentered = centered;

        if(emit)
        {
            float envelope = Math.max((mHigh - mLow) * 0.5f, MINIMUM_ENVELOPE);
            float normalized = (symbol - center) / envelope;
            updateLevels(symbol, center, envelope);

            if(mPendingTimingErrorAvailable)
            {
                if(Math.abs(normalized + mPreviousSymbol) < SYMMETRIC_TRANSITION_THRESHOLD)
                {
                    mPhase -= TIMING_GAIN * mPendingTimingError;
                }

                mPendingTimingErrorAvailable = false;
            }

            mPreviousSymbol = normalized;
            mListener.receive(normalized);
        }
    }

    /**
     * Updates the tracked high and low (outer) levels from the symbol value.  For 4-level signalling, only symbols
     * that are decided as outer levels are used so that the inner levels don't pull the outer levels inward.
     */
    private void updateLevels(float symbol, float center, float envelope)
    {
        if(mLevels == 4)
        {
            if(symbol > center + OUTER_LEVEL_THRESHOLD * envelope)
            {
                mHigh += LEVEL_GAIN * (symbol - mHigh);
            }
            else if(symbol < center - OUTER_LEVEL_THRESHOLD * envelope)
            {
                mLow += LEVEL_GAIN * (symbol - mLow);
            }
        }
        else if(symbol > center)
        {
            mHigh += LEVEL_GAIN * (symbol - mHigh);
        }
        else
        {
            mLow += LEVEL_GAIN * (symbol - mLow);
        }
    }

    /**
     * Moving average filter
     */
    private float filter(float sample)
    {
        if(mAverageBuffer.length == 1)
        {
            return sample;
        }

        mAverageSum += sample - mAverageBuffer[mAveragePointer];
        mAverageBuffer[mAveragePointer] = sample;
        mAveragePointer = (mAveragePointer + 1) % mAverageBuffer.length;

        //Recalculate the running sum once per pass through the buffer to prevent floating point drift
        if(mAveragePointer == 0)
        {
            float sum = 0.0f;

            for(float value: mAverageBuffer)
            {
                sum += value;
            }

            mAverageSum = sum;
        }

        return mAverageSum / mAverageBuffer.length;
    }

    /**
     * Listener for normalized symbols.
     */
    public interface ISymbolListener
    {
        /**
         * Receives a normalized symbol value where the outer levels are near +/- 1.0
         */
        void receive(float symbol);
    }
}
