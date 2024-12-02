/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.sample.Listener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.FastMath;

/**
 * DMR symbol processor.  Processes blocks of differentially decoded samples to extract symbols from the sample
 * stream.
 *
 * Buffer Structure & Size = 129 Dibits: [2 pad][12 CACH][54 Message Prefix][24 Sync][24 workspace][1 pad]
 */
public class DmrSymbolProcessor2
{
    private static final float MAX_POSITIVE_SOFT_SYMBOL = Dibit.D01_PLUS_3.getIdealPhase();
    private static final float MAX_NEGATIVE_SOFT_SYMBOL = Dibit.D11_MINUS_3.getIdealPhase();
    private static final int BUFFER_PROTECTED_REGION_DIBITS = 92;
    private static final int BUFFER_WORKSPACE_LENGTH_DIBITS = 25; //This can be adjusted for efficiency
    private static final int BUFFER_LENGTH_DIBITS = BUFFER_PROTECTED_REGION_DIBITS + BUFFER_WORKSPACE_LENGTH_DIBITS;
    private static final float SYMBOL_DECISION_POSITIVE = (float)(Math.PI / 2.0);
    private static final float SYMBOL_DECISION_NEGATIVE = -SYMBOL_DECISION_POSITIVE;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 = (float)(Math.PI / 4.0);
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3 = 3.0f * IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1 = -IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3 = -IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3;
    private static final float MAXIMUM_DEVIATION_PERCENTAGE_SAMPLES_PER_SYMBOL = 0.0001f; // +/- 0.01%
    private static final float MAXIMUM_TIMING_ERROR = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 / 2.0f;
    private static final float SAMPLE_COUNTER_GAIN = 0.070f; //original: .4
    private static final float OBSERVED_SAMPLES_PER_SYMBOL_GAIN = 0.05f * SAMPLE_COUNTER_GAIN * SAMPLE_COUNTER_GAIN;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");

    private Interpolator mInterpolator = InterpolatorFactory.getInterpolator();
    private DmrSyncModeMonitor mSyncModeMonitor = new DmrSyncModeMonitor();
    private ScalarDmrSyncDetector2 mSyncDetector = new ScalarDmrSyncDetector2();
    private ScalarDmrSyncDetector2 mSyncDetectorLag1 = new ScalarDmrSyncDetector2();
    private ScalarDmrSyncDetector2 mSyncDetectorLag2 = new ScalarDmrSyncDetector2();
    private float mLaggingSyncOffset1;
    private float mLaggingSyncOffset2;
    //Dibit delay line sizing: CACH(12) + MESSAGE_PREFIX(54) + SYNC(24)
    private DibitDelayLine mDibitDelayLine = new DibitDelayLine(90);
    private float[] mBuffer;
    private int mBufferPointer;
    private int mBufferLoadPointer;
    private int mBufferWorkspaceLength;
    private float mSamplesPerSymbol;
    private float mObservedSamplesPerSymbol;
    private float mMaxSamplesPerSymbol;
    private float mMinSamplesPerSymbol;
    private float mSamplePoint;

    private long mDebugSyncEvaluate = 0;
    private float mDebugSyncDetectScore;
    private int mDebugTotalSyncDetects;
    private int mDebugSymbolCount;
    private int mDebugLastCounterAtSymbolDetect;
    private int mDebugTotalSyncBitErrors;
    private int mDebugFalseDetectCount;

    private Listener<Dibit> mDibitListener;

    public enum Mode {FINE, COARSE};
    private Mode mMode = Mode.COARSE;

    public DmrSymbolProcessor2()
    {
        mSyncModeMonitor.add(mSyncDetector);
        mSyncModeMonitor.add(mSyncDetectorLag1);
        mSyncModeMonitor.add(mSyncDetectorLag2);
    }

    /**
     * Registers a listener to receive the decoded dibits
     * @param listener to register
     */
    public void setListener(Listener<Dibit> listener)
    {
        mDibitListener = listener;
    }

    /**
     * Broadcasts the dibit to an optional registered listener.
     * @param dibit to broadcast.
     */
    private void broadcast(Dibit dibit)
    {
        if(mDibitListener != null)
        {
            mDibitListener.receive(dibit);
        }
    }

    public void process(float[] samples)
    {
        float syncDetectThreshold = 60;

        int samplesPointer = 0;

        while(samplesPointer < samples.length)
        {
            if(mBufferLoadPointer == mBuffer.length)
            {
                System.arraycopy(mBuffer, mBufferWorkspaceLength, mBuffer, 0, mBuffer.length - mBufferWorkspaceLength);
                mBufferLoadPointer -= mBufferWorkspaceLength;
                mBufferPointer -= mBufferWorkspaceLength;
            }

            int copyLength = Math.min(mBuffer.length - mBufferLoadPointer, samples.length - samplesPointer);
            System.arraycopy(samples, samplesPointer, mBuffer, mBufferLoadPointer, copyLength);
            samplesPointer += copyLength;
            mBufferLoadPointer += copyLength;

            //TODO: this can be made smarter to increment by chunks versus one at a time
            while(mBufferPointer < (mBufferLoadPointer - 14)) //Interpolator needs 8 and optimizer needs 6 pad spaces
            {
                mBufferPointer++;
                mSamplePoint--;

                if(mSamplePoint < 1)
                {
                    mDebugSymbolCount++;
                    float softSymbol = mInterpolator.filter(mBuffer, mBufferPointer, mSamplePoint);
                    Dibit symbol = toSymbol(softSymbol);
                    mDebugSyncEvaluate = Long.rotateLeft(mDebugSyncEvaluate, 2);
                    mDebugSyncEvaluate |= symbol.getValue();
                    mDebugSyncEvaluate &= 0xFFFFFFFFFFFFl;

                    //Store the symbol in the delay line and broadcast the delayed ejected symbol.
                    broadcast(mDibitDelayLine.insert(symbol));

                    mDebugSyncDetectScore = mSyncDetector.process(softSymbol);

                    float lag1 = mBufferPointer + mSamplePoint - mLaggingSyncOffset1;
                    float lag2 = mBufferPointer + mSamplePoint - mLaggingSyncOffset2;
                    int lagIntegral1 = (int)Math.floor(lag1);
                    int lagIntegral2 = (int)Math.floor(lag2);
                    float softSymbolLag1 = mInterpolator.filter(mBuffer, lagIntegral1, lag1 - lagIntegral1);
                    float softSymbolLag2 = mInterpolator.filter(mBuffer, lagIntegral2, lag2 - lagIntegral2);
                    float scoreLag1 = mSyncDetectorLag1.process(softSymbolLag1);
                    float scoreLag2 = mSyncDetectorLag2.process(softSymbolLag2);

                    int debugElapsed = mDebugSymbolCount - mDebugLastCounterAtSymbolDetect;

                    if(debugElapsed > 2 && scoreLag1 > mDebugSyncDetectScore && scoreLag1 > scoreLag2 && scoreLag1 > syncDetectThreshold)
                    {
                        if(optimize(mSyncDetectorLag1.getDetectedPattern(), -mLaggingSyncOffset1, scoreLag1))
                        {
                            mSyncModeMonitor.detected(mSyncDetectorLag1.getDetectedPattern());
                            System.out.println("LAG 1  Sync [" + mSyncDetectorLag1.getDetectedPattern().name() + "] Elapsed [" + debugElapsed + "] Symbol [" + mDebugSymbolCount + "] Score [" + scoreLag1 + "]");
                            mDebugTotalSyncDetects++;
                            mDebugLastCounterAtSymbolDetect = mDebugSymbolCount;
                        }
                        else
                        {
                            mDebugFalseDetectCount++;
                            System.out.println("\t\tLAG 1 **FALSE** Sync [" + mSyncDetectorLag1.getDetectedPattern().name() + "] Score [" + scoreLag1 + "] Symbol [" + mDebugSymbolCount + "] Elapsed [" + debugElapsed + "]");
                        }
                    }
                    else if(debugElapsed > 2 && scoreLag2 > mDebugSyncDetectScore && scoreLag2 > syncDetectThreshold)
                    {
                        if(optimize(mSyncDetectorLag2.getDetectedPattern(), -mLaggingSyncOffset2, scoreLag2))
                        {
                            mSyncModeMonitor.detected(mSyncDetectorLag2.getDetectedPattern());
                            System.out.println("LAG 2  Sync [" + mSyncDetectorLag2.getDetectedPattern().name() + "] Elapsed [" + debugElapsed + "] Symbol [" + mDebugSymbolCount + "] Score [" + scoreLag2 + "]");
                            mDebugTotalSyncDetects++;
                            mDebugLastCounterAtSymbolDetect = mDebugSymbolCount;
                        }
                        else
                        {
                            mDebugFalseDetectCount++;
                            System.out.println("\t\tLAG 2 **FALSE** Sync [" + mSyncDetectorLag2.getDetectedPattern().name() + "] Score [" + scoreLag2 + "] Symbol [" + mDebugSymbolCount + "] Elapsed [" + debugElapsed + "]");
                        }
                    }
                    else if(mDebugSyncDetectScore > syncDetectThreshold)
                    {
                        if(optimize(mSyncDetector.getDetectedPattern(), 0.0f, mDebugSyncDetectScore))
                        {
                            mSyncModeMonitor.detected(mSyncDetector.getDetectedPattern());
                            System.out.println("NORMAL Sync [" + mSyncDetector.getDetectedPattern().name() + "] Elapsed [" + debugElapsed + "] Symbol [" + mDebugSymbolCount + "] Score [" + mDebugSyncDetectScore + "]");
                            long delta = mDebugSyncEvaluate ^ mSyncDetector.getDetectedPattern().getPattern();
                            int missCount = Long.bitCount(mDebugSyncEvaluate ^ mSyncDetector.getDetectedPattern().getPattern());
                            mDebugTotalSyncDetects++;
                            mDebugTotalSyncBitErrors += missCount;

                            StringBuilder sb = new StringBuilder();
                            sb.append("[");

                            for(int y = 0; y < 48; y++)
                            {
                                long mask = Long.rotateLeft(1L, 47 - y);

                                if((mask & delta) == mask)
                                {
                                    sb.append("x");
                                }
                                else
                                {
                                    sb.append(".");
                                }
                            }

                            sb.append("] Sync [").append(mSyncDetector.getDetectedPattern());
                            sb.append("] At [").append(mDebugSymbolCount);
                            sb.append("] Next At [").append(mDebugSymbolCount + 144);
                            sb.append("] Elapsed Symbols [").append(debugElapsed);
                            sb.append("] Sync Score [").append(DECIMAL_FORMAT.format(mDebugSyncDetectScore));
                            sb.append("] Sync Bit Errors [").append(missCount);
                            sb.append("] Cumulative Errs [").append(mDebugTotalSyncBitErrors);
                            sb.append("] Avg/Sync [").append(mDebugTotalSyncBitErrors / mDebugTotalSyncDetects);
                            sb.append("] False Detects [").append(mDebugFalseDetectCount);
                            sb.append("] Cumulative Detects [").append(mDebugTotalSyncDetects).append("]");
                            System.out.println(sb);
                            mDebugLastCounterAtSymbolDetect = mDebugSymbolCount;
                        }
                        else
                        {
                            mDebugFalseDetectCount++;
                            System.out.println("\t\tNORMAL **FALSE** Sync [" + mSyncDetector.getDetectedPattern().name() + "] Score [" + mDebugSyncDetectScore + "] Symbol [" + mDebugSymbolCount + "] Elapsed [" + debugElapsed + "]");
                        }
                    }
                    else //No sync detect
                    {
                        //Interpolated sample is in the delay line between indices 3 and 4.  Use those two samples to detect
                        //phasor rotation direction when calculating phase error of the interpolated sample against the ideal
                        //phase for that quadrant.
//                        float timingError = calculate(symbol, mBuffer[mBufferPointer + 3], softSymbol, mBuffer[mBufferPointer + 4]);
//
//                        //Adjust observed samples per symbol based on timing error
//                        mObservedSamplesPerSymbol = mObservedSamplesPerSymbol + (timingError * OBSERVED_SAMPLES_PER_SYMBOL_GAIN);
//
//                        //Constrain observed samples per symbol to min/max values
//                        mObservedSamplesPerSymbol = FastMath.min(mObservedSamplesPerSymbol, mMaxSamplesPerSymbol);
//                        mObservedSamplesPerSymbol = FastMath.max(mObservedSamplesPerSymbol, mMinSamplesPerSymbol);
//
//                        mSamplePoint += (timingError * SAMPLE_COUNTER_GAIN);
                    }

                    //Add another symbol's worth of samples to the counter and adjust sample timing based on timing error
                    mSamplePoint += mObservedSamplesPerSymbol;
                }
            }
            //Do work here to move the mBufferPointer along.
        }
    }

    /**
     * Adjusts the symbol timing and symbol spacing to identify the best achievable sync correlation score and apply
     * those adjustments when the correlation score exceeds a positive sync detection threshold.
     * @param pattern that was detected
     * @param additionalOffset from current mBufferPointer and mSamplePoint.  This can be zero offset for the primary
     * sync detector or an offset for the lagging sync detectors.
     * @param correlationScore that triggered the original sync detect (to be included in the debug logging)
     * @return true if there is a positive sync detection.
     */
    private boolean optimize(DMRSyncPattern pattern, float additionalOffset, float correlationScore)
    {
        //Offset is the start of the first sample of the first symbol of the sync pattern calculated from the current
        //buffer pointer and sample point which should be the final sample of the final symbol of the detected sync.
        float offset = (mBufferPointer + mSamplePoint) + additionalOffset - (mSamplesPerSymbol * 23);
        int offsetIntegral = (int)Math.floor(offset);
        float offsetFractional = offset - offsetIntegral;

        StringBuilder sb = new StringBuilder();

        for(int x = -2; x <= 2; x++)
        {
            float score = score(offsetIntegral + x, offsetFractional, mSamplesPerSymbol, pattern);

            if(x < 0)
            {
                sb.append("\tOffset: " + x + " Legacy [" + correlationScore + " optimized [" + score + "]").append("\n");
            }
            else if(x == 0)
            {
                sb.append("\tOffset:  " + x + " Legacy [" + correlationScore + " optimized [" + score + "] <<<<<<").append("\n");
            }
            else
            {
                sb.append("\tOffset:  " + x + " Legacy [" + correlationScore + " optimized [" + score + "]").append("\n");
            }
        }

        //Find the optimal symbol timing
        float stepSize = mSamplesPerSymbol / 10.0f;
        float stepSizeMin = 0.03f;
        float adjustment = 0.0f;
        float adjustmentMax = mSamplesPerSymbol / 2.0f;
        float candidate = offset;

        int candidateIntegral = (int)Math.floor(candidate);
        float candidateFractional = candidate - candidateIntegral;
        float scoreCenter = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);

        candidate = offset - stepSize;
        candidateIntegral = (int)Math.floor(candidate);
        candidateFractional = candidate - candidateIntegral;
        float scoreLeft = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);

        candidate = offset + stepSize;
        candidateIntegral = (int)Math.floor(candidate);
        candidateFractional = candidate - candidateIntegral;
        float scoreRight = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);

        while(stepSize > stepSizeMin && Math.abs(adjustment) <= adjustmentMax)
        {
            if(scoreLeft > scoreRight && scoreLeft > scoreCenter)
            {
                adjustment -= stepSize;
                scoreRight = scoreCenter;
                scoreCenter = scoreLeft;

                candidate = offset + adjustment - stepSize;
                candidateIntegral = (int)Math.floor(candidate);
                candidateFractional = candidate - candidateIntegral;
                scoreLeft = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);
            }
            else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
            {
                adjustment += stepSize;
                scoreLeft = scoreCenter;
                scoreCenter = scoreRight;

                candidate = offset + adjustment + stepSize;
                candidateIntegral = (int)Math.floor(candidate);
                candidateFractional = candidate - candidateIntegral;
                scoreRight = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);
            }
            else
            {
                stepSize *= 0.5f;

                if(stepSize > stepSizeMin)
                {
                    candidate = offset + adjustment - stepSize;
                    candidateIntegral = (int)Math.floor(candidate);
                    candidateFractional = candidate - candidateIntegral;
                    scoreLeft = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);

                    candidate = offset + adjustment + stepSize;
                    candidateIntegral = (int)Math.floor(candidate);
                    candidateFractional = candidate - candidateIntegral;
                    scoreRight = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);
                }
            }
        }

        //If we didn't find an optimal correlation score above the 90 threshold, return a false sync.
        if(scoreCenter < 90)
        {
            return false;
        }

        if(additionalOffset != 0.0)
        {
            System.out.println(sb + "\tTiming Adjustment [" + adjustment + "] Plus Offset [" + additionalOffset + "] Total [" +
                    (adjustment + additionalOffset) + "] Score [" + scoreCenter + "]");
            adjustment += additionalOffset;
        }
        else
        {
            System.out.println(sb + "\tTiming Adjustment [" + adjustment +  "] Score [" + scoreCenter + "]");
        }

        boolean updated = (adjustment != 0.0f);

        //Adjust the buffer pointer and sample point to the optimal values before we test samples per symbol (SPS)
        if(adjustment < 0.0f)
        {
            int adjustmentIntegral = (int)Math.ceil(adjustment);
            mBufferPointer += adjustmentIntegral;
            mSamplePoint += (adjustment - adjustmentIntegral);
        }
        else
        {
            int adjustmentIntegral = (int)Math.floor(adjustment);
            mBufferPointer += adjustmentIntegral;
            mSamplePoint += (adjustment - adjustmentIntegral);
        }

        while(mSamplePoint < 0)
        {
            mBufferPointer++;
            mSamplePoint++;
        }

        //Update the offset to the optimized buffer and sample pointers
        offset = (mBufferPointer + mSamplePoint) - (mSamplesPerSymbol * 23);
        offsetIntegral = (int)Math.floor(offset);
        offsetFractional = offset - offsetIntegral;

        //Samples Per Symbol adjustment ********************
        adjustmentMax = mSamplesPerSymbol * MAXIMUM_DEVIATION_PERCENTAGE_SAMPLES_PER_SYMBOL;
        adjustment = 0.0f;
        stepSizeMin = 0.00002f;
        stepSize = adjustmentMax / 20.0f;

        //We keep the updated pointer and sample point values and now focus on samples per symbol
        candidate = mSamplesPerSymbol - stepSize;
        scoreLeft = score(offsetIntegral, offsetFractional, candidate, pattern);
        candidate = mSamplesPerSymbol + stepSize;;
        scoreRight = score(offsetIntegral, offsetFractional, candidate, pattern);

        while(stepSize > stepSizeMin && Math.abs(adjustment) <= adjustmentMax)
        {
            if(scoreLeft > scoreRight && scoreLeft > scoreCenter)
            {
                adjustment -= stepSize;
                scoreRight = scoreCenter;
                scoreCenter = scoreLeft;
                candidate = mSamplesPerSymbol + adjustment - stepSize;
                scoreLeft = score(offsetIntegral, offsetFractional, candidate, pattern);
            }
            else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
            {
                adjustment += stepSize;
                scoreLeft = scoreCenter;
                scoreCenter = scoreRight;
                candidate = mSamplesPerSymbol + adjustment + stepSize;
                scoreRight = score(offsetIntegral, offsetFractional, candidate, pattern);
            }
            else
            {
                stepSize *= 0.5f;

                if(stepSize > stepSizeMin)
                {
                    candidate = mSamplesPerSymbol + adjustment - stepSize;
                    scoreLeft = score(offsetIntegral, offsetFractional, candidate, pattern);
                    candidate = mSamplesPerSymbol + adjustment + stepSize;
                    scoreRight = score(offsetIntegral, offsetFractional, candidate, pattern);
                }
            }
        }

        System.out.println("\tSPS Current: " + mObservedSamplesPerSymbol + " Updated: " + (mSamplesPerSymbol + adjustment) + " Adjustment: " + adjustment + " Score:" + scoreCenter);

        if(mObservedSamplesPerSymbol != (mSamplesPerSymbol + adjustment))
        {
            updated = true;
            mObservedSamplesPerSymbol = (mSamplesPerSymbol + adjustment);
        }

//        updated = false;

        //If we updated either the symbol timing or the symbol duration, resample the cached symbols at the optimized
        //buffer pointer and sample point to improve the quality of the symbols.
        if(updated)
        {
            List<Dibit> ejectedDibits = new ArrayList<>();
            List<Dibit> resampledDibits = new ArrayList<>();
            float resampleStart = mBufferPointer + mSamplePoint;
            resampleStart -= (89 * mObservedSamplesPerSymbol); //Start at 89 (+ 1 current = 90) symbols
            int resampleStartIntegral;

            for(int x = 0; x < 66; x++)
            {
                resampleStartIntegral = (int)Math.floor(resampleStart);

                if(resampleStartIntegral >= 0)
                {
                    float resampledSoftSymbol = mInterpolator.filter(mBuffer, resampleStartIntegral, resampleStart - resampleStartIntegral);
                    Dibit resampledSymbol = toSymbol(resampledSoftSymbol);
                    resampledDibits.add(resampledSymbol);
                    Dibit ejected = mDibitDelayLine.insert(resampledSymbol);
                    ejectedDibits.add(ejected);
                }
                else
                {
                    //This shouldn't happen since there's 2x dibits of padding on the front side, but just in case.
                    mDibitDelayLine.insert(Dibit.D01_PLUS_3);
                }

                resampleStart += mObservedSamplesPerSymbol;
            }

            //We don't need to resample the sync region ... just use the optimal sync dibit values.
            for(Dibit dibit: pattern.toDibits())
            {
                resampledDibits.add(dibit);
                Dibit ejected = mDibitDelayLine.insert(dibit);
                ejectedDibits.add(ejected);
            }

            for(int x = 0; x < ejectedDibits.size(); x++)
            {
                Dibit ejected = ejectedDibits.get(x);
                Dibit resampled = resampledDibits.get(x);
                if(ejected != resampled)
                {
                    System.out.println("------------------>>> " + x + " E:" + ejected + "\tR:" + resampled + " ** CORRECTED **");
                }
            }
        }

        return true;
    }

    /**
     * Calculates the sync correlation score for sync pattern at the specified offsets and symbol timing.
     * @param bufferPointer to the start of the samples in the soft symbol buffer
     * @param fractional position to interpolate within the 8 samples starting at the buffer pointer.
     * @param samplesPerSymbol spacing to test for.
     * @param pattern to correlate against.
     * @return correlation score.
     */
    public float score(int bufferPointer, float fractional, float samplesPerSymbol, DMRSyncPattern pattern)
    {
        return score(bufferPointer, fractional, samplesPerSymbol, pattern, false);
    }

    /**
     * Calculates the sync correlation score for sync pattern at the specified offsets and symbol timing.
     * @param bufferPointer to the start of the samples in the soft symbol buffer
     * @param fractional position to interpolate within the 8 samples starting at the buffer pointer.
     * @param samplesPerSymbol spacing to test for.
     * @param pattern to correlate against.
     * @param logSymbols to log the soft symbols used in the scoring process.
     * @return correlation score.
     */
    public float score(int bufferPointer, float fractional, float samplesPerSymbol, DMRSyncPattern pattern, boolean logSymbols)
    {
        float score = 0;
        float[] symbols = pattern.toSymbols();
        float[] softSymbols = new float[24];
        int integral;

        for(int x = 0; x < 24; x++)
        {
            float softSymbol = mInterpolator.filter(mBuffer, bufferPointer, fractional);
            softSymbol = Math.min(softSymbol, MAX_POSITIVE_SOFT_SYMBOL);
            softSymbol = Math.max(softSymbol, MAX_NEGATIVE_SOFT_SYMBOL);

            if(logSymbols)
            {
//                System.out.println(x + " Pointer [" + bufferPointer + "] Fractional [" + fractional + "] Soft Symbol [" + softSymbol + "]");
            }

            softSymbols[x] = softSymbol;

            score += softSymbol * symbols[x];

            fractional += samplesPerSymbol;
            integral = (int)fractional;
            bufferPointer += integral;
            fractional -= integral;
        }

        if(logSymbols)
        {
//            System.out.println("   Score Symbols: " + Arrays.toString(symbols));
//            System.out.println("    Soft Symbols: " + Arrays.toString(softSymbols));
        }

        return score;
    }

    /**
     * Sets or updates the samples per symbol.
     * @param samplesPerSymbol to apply.
     */
    public void setSamplesPerSymbol(float samplesPerSymbol)
    {
        mSamplesPerSymbol = samplesPerSymbol;
        mObservedSamplesPerSymbol = mSamplesPerSymbol;
        mSamplePoint = samplesPerSymbol;
        mLaggingSyncOffset1 = samplesPerSymbol / 3;
        mLaggingSyncOffset2 = mLaggingSyncOffset1 * 2;
        //Set the min/max at +/-2% of the expected symbol period.
        mMaxSamplesPerSymbol = samplesPerSymbol * (1.0f + MAXIMUM_DEVIATION_PERCENTAGE_SAMPLES_PER_SYMBOL);
        mMinSamplesPerSymbol = samplesPerSymbol * (1.0f - MAXIMUM_DEVIATION_PERCENTAGE_SAMPLES_PER_SYMBOL);
        mBufferWorkspaceLength = (int)Math.ceil(BUFFER_WORKSPACE_LENGTH_DIBITS * samplesPerSymbol);
        int bufferLength = (int)(Math.ceil(BUFFER_LENGTH_DIBITS * samplesPerSymbol));
        mBuffer = new float[bufferLength];
        mBufferLoadPointer = (int)Math.ceil(BUFFER_PROTECTED_REGION_DIBITS * samplesPerSymbol);
        mBufferPointer = mBufferLoadPointer;
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
