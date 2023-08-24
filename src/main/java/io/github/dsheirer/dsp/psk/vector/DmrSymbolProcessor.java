/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.vector;

import io.github.dsheirer.dsp.filter.interpolator.Interpolator;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorFactory;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.FastMath;

/**
 * Extracts symbols from differentially decoded DQPSK sample stream and incrementally tracks symbol frequency as a
 * feedback to the differential decoder.
 */
public class DmrSymbolProcessor
{
    private static final float SYMBOL_DECISION_POSITIVE = (float)(Math.PI / 2.0);
    private static final float SYMBOL_DECISION_NEGATIVE = -SYMBOL_DECISION_POSITIVE;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 = (float)(Math.PI / 4.0);
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3 = 3.0f * IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1 = -IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3 = -IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3;
    private static final float MAXIMUM_DEVIATION_SAMPLES_PER_SYMBOL = 0.0005f; // original: .02


    private static final float SAMPLE_COUNTER_GAIN = 0.070f; //original: .4
//    private static final float SAMPLE_COUNTER_GAIN = 0.15f;


    private static final float OBSERVED_SAMPLES_PER_SYMBOL_GAIN = 0.05f * SAMPLE_COUNTER_GAIN * SAMPLE_COUNTER_GAIN;
//    private static final float OBSERVED_SAMPLES_PER_SYMBOL_GAIN = 0.1f * SAMPLE_COUNTER_GAIN * SAMPLE_COUNTER_GAIN;


    private static final float MAXIMUM_TIMING_ERROR = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 / 2.0f;
    private static final float TWO_PI = 2.0f * (float)Math.PI;
    private static final int INTERPOLATION_FILTER_LENGTH = 8;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");

    private Interpolator mInterpolator = InterpolatorFactory.getInterpolator();
    private float mSamplesPerSymbol;
    private float mObservedSamplesPerSymbol;
    private float mMaxSamplesPerSymbol;
    private float mMinSamplesPerSymbol;
    private float mSamplePoint = 1.2f;
    private float mPreviousSample;
    private float[] mDelayLine = new float[INTERPOLATION_FILTER_LENGTH * 2];
    private int mDelayLinePointer = 0;
    private List<Dibit> mSymbols = new ArrayList<>();
    private float mNoiseThreshold;
    private boolean mNoisy;

    private float mDebugSymbolPhase;
    private float mDebugSymbolTiming;
    private int mDebugSampleCounter;
    private int mDebugLastCounterAtSymbolDetect;
    private int mDebugSymbolCount;
    private int mDebugTotalSyncDetects;
    private float mDebugSyncDetectScore;
    private long mSyncEvaluate = 0;

    private ScalarDmrSyncDetector mSyncDetector = new ScalarDmrSyncDetector();
    private DQPSKLmsEqualizer mEqualizer = new DQPSKLmsEqualizer(12);
    private DibitDelayLine mDibitDelayLine = new DibitDelayLine(24);

    public DmrSymbolProcessor()
    {
    }

    /**
     * Observed samples per symbol.  Value is adjusted after each symbol period and can be used for differential
     * decoding of the DQPSK encoded sample stream to determine the interpolation offset across two symbols.
     * @return observed symbol period.
     */
    public float getObservedSamplesPerSymbol()
    {
        return mObservedSamplesPerSymbol;
    }

    /**
     * Retrieves accumulated symbols and clears the symbol buffer.
     * @return accumulated symbols.
     */
    public List<Dibit> getSymbolsAndClear()
    {
        List<Dibit> symbols = new ArrayList<>(mSymbols);
        mSymbols.clear();
        return symbols;
    }

    /**
     * Sets the starting samples per symbol value which should be the channel sample rate divided by the symbol rate.
     * @param samplesPerSymbol to use initially.
     */
    public void setSamplesPerSymbol(float samplesPerSymbol)
    {
        mSamplesPerSymbol = samplesPerSymbol;
        mObservedSamplesPerSymbol = samplesPerSymbol;
        mSamplePoint = samplesPerSymbol;
        //Set the min/max at +/-2% of the expected symbol period.
        mMaxSamplesPerSymbol = samplesPerSymbol * (1.0f + MAXIMUM_DEVIATION_SAMPLES_PER_SYMBOL);
        mMinSamplesPerSymbol = samplesPerSymbol * (1.0f - MAXIMUM_DEVIATION_SAMPLES_PER_SYMBOL);
        mNoiseThreshold = TWO_PI / mSamplesPerSymbol * 1.2f;
    }

    public void reset()
    {
        setSamplesPerSymbol(mSamplesPerSymbol);
        mDebugSymbolPhase = 0;
        mDebugSymbolTiming = 0;
    }

    /**
     * Processes the differentially decoded samples to produce symbol decisions and dynamically track and adapt to
     * the observed symbol frequency by adjusting the samples per symbol value.
     * @param samples that have been differentially decoded
     */
    public void process(float[] samples)
    {
        //Temporarily transfer heap variables for use on the stack
        float previousSample = mPreviousSample;
        float samplePoint = mSamplePoint;
        int delayLinePointer = mDelayLinePointer;

        float currentSample = 0f;
        float noiseThreshold = mNoiseThreshold;
        boolean noisy = mNoisy;
        boolean resetNoisy = false;
        boolean debugLogSamples = false;
        boolean debugLogCorrelationSyncDetects = true;
        boolean debugFixSyncSymbols = true;
        float syncDetectThreshold = 80;

        for(int x = 0; x < samples.length; x++)
        {
            mDebugSampleCounter++;
            samplePoint--;
            currentSample = samples[x];

            //Detect and unroll phase wrapping
            if(Math.abs(currentSample - previousSample) > Math.PI)
            {
                if(0 < currentSample && currentSample < Math.PI && previousSample < 0)
                {
                    currentSample = currentSample - TWO_PI;
                }
                else if(0 > currentSample && currentSample > -Math.PI && previousSample > 0)
                {
                    currentSample = currentSample + TWO_PI;
                }
            }

            if(Math.abs(currentSample - previousSample) > noiseThreshold)
            {
                noisy = true;
            }

            //Fill up the delay line to use with the interpolator
            mDelayLine[delayLinePointer] = currentSample;
            mDelayLine[delayLinePointer + INTERPOLATION_FILTER_LENGTH] = currentSample;

            //Increment delay line pointer and keep pointer in bounds
            delayLinePointer++;
            delayLinePointer %= INTERPOLATION_FILTER_LENGTH;

            if(samplePoint < 1.0f)
            {
                mDebugSymbolCount++;
                //Calculate the interpolated symbol
                float interpolatedSample = mInterpolator.filter(mDelayLine, delayLinePointer, samplePoint);
                Dibit symbol = toSymbol(interpolatedSample);

                mDebugSyncDetectScore = mSyncDetector.process(interpolatedSample);

                mSyncEvaluate = Long.rotateLeft(mSyncEvaluate, 2);
                mSyncEvaluate |= symbol.getValue();
                mSyncEvaluate &= 0xFFFFFFFFFFFFl;

                //Interpolated sample is in the delay line between indices 3 and 4.  Use those two samples to detect
                //phasor rotation direction when calculating phase error of the interpolated sample against the ideal
                //phase for that quadrant.
                float timingError = noisy ? 0 : calculate(symbol, mDelayLine[delayLinePointer + 3], interpolatedSample,
                        mDelayLine[delayLinePointer + 4]);

                //Adjust observed samples per symbol based on timing error
                mObservedSamplesPerSymbol = mObservedSamplesPerSymbol + (timingError * OBSERVED_SAMPLES_PER_SYMBOL_GAIN);

                //Constrain observed samples per symbol to min/max values
                mObservedSamplesPerSymbol = FastMath.min(mObservedSamplesPerSymbol, mMaxSamplesPerSymbol);
                mObservedSamplesPerSymbol = FastMath.max(mObservedSamplesPerSymbol, mMinSamplesPerSymbol);

                //Add another symbol's worth of samples to the counter and adjust sample timing based on timing error
                samplePoint += (mObservedSamplesPerSymbol + (timingError * SAMPLE_COUNTER_GAIN));

                //debug
                mDebugSymbolTiming += (timingError * SAMPLE_COUNTER_GAIN);
                mDebugSymbolTiming %= mSamplesPerSymbol;
                mDebugSymbolPhase = interpolatedSample;

                //Equalize and reevaluate the symbol
                if(noisy)
                {
                    mEqualizer.processNoUpdate(symbol, interpolatedSample);
                }
                else
                {
                    interpolatedSample = mEqualizer.process(symbol, interpolatedSample);
                    symbol = toSymbol(interpolatedSample);
                }

                if(mDebugSyncDetectScore > syncDetectThreshold)
                {
                    Dibit[] syncDibits = DMRSyncPattern.BASE_STATION_DATA.toDibits();
                    mDibitDelayLine.update(syncDibits);
                    mSymbols.add(syncDibits[0]);
                }
                else
                {
                    mSymbols.add(mDibitDelayLine.insert(symbol));
                }

                resetNoisy = true;
            }

            float temp = (mSamplesPerSymbol - mObservedSamplesPerSymbol) * 10f;
            int missCount = Long.bitCount(mSyncEvaluate ^ DMRSyncPattern.BASE_STATION_DATA.getPattern());

            if(resetNoisy && debugLogCorrelationSyncDetects && mDebugSyncDetectScore > syncDetectThreshold)
            {
                mEqualizer.syncDetected(DMRSyncPattern.BASE_STATION_DATA.toDibits());
                mDebugTotalSyncDetects++;
                int elapsed = mDebugSymbolCount - mDebugLastCounterAtSymbolDetect;

                StringBuilder sb2 = new StringBuilder();
                long delta = mSyncEvaluate ^ DMRSyncPattern.BASE_STATION_DATA.getPattern();

                for(int y = 0; y < 48; y++)
                {
                    long mask = Long.rotateLeft(1l, 47 - y);

                    if((mask & delta) == mask)
                    {
                        sb2.append("x");
                    }
                    else
                    {
                        sb2.append(".");
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("[").append(sb2).append("] ");
                sb.append("Detect - Symbol Count [").append(mDebugSymbolCount);
                sb.append("] Next [").append(mDebugSymbolCount + 144);
                sb.append("] Elapsed [").append(elapsed);
                sb.append("] Score [").append(DECIMAL_FORMAT.format(mDebugSyncDetectScore));
                sb.append("] Miss [").append(missCount);
                sb.append("] Total Detects [").append(mDebugTotalSyncDetects).append("]");
                System.out.println(sb);
                mDebugLastCounterAtSymbolDetect = mDebugSymbolCount;

//                //Fix the symbols based on the detect
//                if(debugFixSyncSymbols)
//                {
//                    Dibit[] sync = DMRSyncPattern.BASE_STATION_DATA.toDibits();
//
//                    for(int y = 0; y < sync.length; y++)
//                    {
//                        int offset = mSymbols.size() - sync.length + y;
//
//                        if(offset >= 0)
//                        {
//                            mSymbols.set(offset, sync[y]);
//                        }
//                    }
//                }
            }

            if(debugLogSamples)
            {
                System.out.println(DECIMAL_FORMAT.format(currentSample) + "," +
                        DECIMAL_FORMAT.format(mDebugSymbolPhase) + "," +
                        DECIMAL_FORMAT.format(mDebugSymbolTiming) + "," +
                        DECIMAL_FORMAT.format(temp) + "," +
                        DECIMAL_FORMAT.format(mDebugSyncDetectScore) + " " +  (mDebugSyncDetectScore > 80 ? "<<<<<<<<<<<<< " : "") +
                        mDebugSymbolCount);
            }

            previousSample = currentSample;

            if(resetNoisy)
            {
                resetNoisy = false;
                noisy = false;
            }
        }

        //Reassign stack variables back onto the heap
        mPreviousSample = previousSample;
        mSamplePoint = samplePoint;
        mDelayLinePointer = delayLinePointer;
        mNoisy = noisy;

        if(mDebugSampleCounter == 328280)
        {
            int a = 0;
        }
    }

    /**
     * Decodes the sample value to determine the correct QPSK quadrant and maps the value to a Dibit symbol.
     * @param sample in radians.
     * @return symbol decision.
     */
    private static Dibit toSymbol(float sample)
    {
        if(sample > 0)
        {
            return sample > SYMBOL_DECISION_POSITIVE ? Dibit.D01_PLUS_3 : Dibit.D00_PLUS_1;
        }
        else
        {
            return sample < SYMBOL_DECISION_NEGATIVE ? Dibit.D11_MINUS_3 : Dibit.D10_MINUS_1;
        }
    }

    /**
     * Calculates the timing error from the sample relative to the ideal sample point.
     * @param symbol decision
     * @param preceding sample, relative to the symbol decision sample.
     * @param symbolPhase interpolated, for the symbol decision.
     * @param following sample, relative to the symbol decision sample.
     * @return error signal in radians
     */
    public static float calculate(Dibit symbol, float preceding, float symbolPhase, float following)
    {
        float ideal = 0.0f;

        switch(symbol)
        {
            case D01_PLUS_3:
                ideal = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3;
                break;
            case D00_PLUS_1:
                ideal = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
                break;
            case D10_MINUS_1:
                ideal = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1;
                break;
            case D11_MINUS_3:
                ideal = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3;
                break;
        }

        float error = ideal - symbolPhase;

        //Constrain error signal
        if(error > 0.0)
        {
            error = FastMath.min(error, MAXIMUM_TIMING_ERROR);
        }
        else
        {
            error = FastMath.max(error, -MAXIMUM_TIMING_ERROR);
        }

        return preceding < following ? error : -error;
    }
}
