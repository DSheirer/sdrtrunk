package io.github.dsheirer.dsp.psk.vector;

import io.github.dsheirer.dsp.filter.interpolator.Interpolator;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorFactory;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.sample.Listener;
import org.apache.commons.math3.util.FastMath;

import java.text.DecimalFormat;

/**
 * DMR symbol processor.  Processes blocks of differentially decoded samples to extract symbols from the sample
 * stream.
 *
 * Buffer Structure & Size = 128 Dibits: [1 pad][12 CACH][66 Message Prefix][24 Sync][24 workspace][1 pad]
 */
public class DmrSymbolProcessor2
{
    private static final float MAX_POSITIVE_SOFT_SYMBOL = Dibit.D01_PLUS_3.getIdealPhase();
    private static final float MAX_NEGATIVE_SOFT_SYMBOL = Dibit.D11_MINUS_3.getIdealPhase();
    private static final int BUFFER_PROTECTED_REGION_DIBITS = 103;
    private static final int BUFFER_WORKSPACE_LENGTH_DIBITS = 25; //This can be adjusted for efficiency
    private static final int BUFFER_LENGTH_DIBITS = BUFFER_PROTECTED_REGION_DIBITS + BUFFER_WORKSPACE_LENGTH_DIBITS;
    private static final float SYMBOL_DECISION_POSITIVE = (float)(Math.PI / 2.0);
    private static final float SYMBOL_DECISION_NEGATIVE = -SYMBOL_DECISION_POSITIVE;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 = (float)(Math.PI / 4.0);
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3 = 3.0f * IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1 = -IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3 = -IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3;
    private static final float MAXIMUM_DEVIATION_SAMPLES_PER_SYMBOL = 0.00035f; // +/- 0.02%
    private static final float MAXIMUM_TIMING_ERROR = IDEAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 / 2.0f;
    private static final float SAMPLE_COUNTER_GAIN = 0.070f; //original: .4
    private static final float OBSERVED_SAMPLES_PER_SYMBOL_GAIN = 0.05f * SAMPLE_COUNTER_GAIN * SAMPLE_COUNTER_GAIN;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");

    private Interpolator mInterpolator = InterpolatorFactory.getInterpolator();
    private ScalarDmrSyncDetector mSyncDetector = new ScalarDmrSyncDetector();
    private ScalarDmrSyncDetector mSyncDetectorLag1 = new ScalarDmrSyncDetector();
    private ScalarDmrSyncDetector mSyncDetectorLag2 = new ScalarDmrSyncDetector();
    private float mLaggingSyncOffset1;
    private float mLaggingSyncOffset2;
    //Dibit delay line sizing: CACH(12) + MESSAGE_PREFIX(66) + SYNC(24)
    private DibitDelayLine mDibitDelayLine = new DibitDelayLine(102);
    private float[] mBuffer;
    private int mBufferResetPoint;
    private int mBufferPointer;
    private int mBufferLoadPointer;
    private int mBufferWorkspaceLength;
    private float mSamplesPerSymbol;
    private float mObservedSamplesPerSymbol;
    private float mMaxSamplesPerSymbol;
    private float mMinSamplesPerSymbol;
    private float mSamplePoint;
    private long mSyncEvaluate = 0;
    private float mDebugSyncDetectScore;
    private int mDebugTotalSyncDetects;
    private int mDebugSymbolCount;
    private int mDebugLastCounterAtSymbolDetect;
    private int mDebugTotalSyncBitErrors;

    private Listener<Dibit> mDibitListener;
    public enum Mode {FINE, COARSE};
    private Mode mMode = Mode.COARSE;

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

            int toCopy = Math.min(mBuffer.length - mBufferLoadPointer, samples.length - samplesPointer);
            System.arraycopy(samples, samplesPointer, mBuffer, mBufferLoadPointer, toCopy);
            samplesPointer += toCopy;
            mBufferLoadPointer += toCopy;

            //TODO: this can be made smarter to increment by chunks versus one at a time
            while(mBufferPointer < (mBufferLoadPointer - 14)) //Interpolator needs 8 and optimizer needs 5-6
            {
                mBufferPointer++;
                mSamplePoint--;

                if(mSamplePoint < 1)
                {
                    mDebugSymbolCount++;

                    float softSymbol = mInterpolator.filter(mBuffer, mBufferPointer, mSamplePoint);
                    Dibit symbol = toSymbol(softSymbol);

                    mSyncEvaluate = Long.rotateLeft(mSyncEvaluate, 2);
                    mSyncEvaluate |= symbol.getValue();
                    mSyncEvaluate &= 0xFFFFFFFFFFFFl;

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

                    int elapsed = mDebugSymbolCount - mDebugLastCounterAtSymbolDetect;

//TODO: the optimizeSync() calls are not passing the correct offset ... I was rushing to invoke the method across all
//TODO: three detectors and it's not aligned for the primary detector any more, or for the lagging detectors.

                    if(elapsed > 2 && scoreLag1 > mDebugSyncDetectScore && scoreLag1 > scoreLag2 && scoreLag1 > syncDetectThreshold)
                    {
                        System.out.println("\nLag 1 $$$$$$$$$$$$$$$$$$$$$ --- >>> Lagging Sync Detected - Score [" + scoreLag1 + "/" + mDebugSyncDetectScore + "] at symbol [" + mDebugSymbolCount + "]   $$$$$$$$$$$$$$$$$$$$$$");
                        optimizeSync(DMRSyncPattern.BASE_STATION_DATA, mLaggingSyncOffset1, mDebugSyncDetectScore);
                    }
                    else if(elapsed > 2 && scoreLag2 > mDebugSyncDetectScore && scoreLag2 > syncDetectThreshold)
                    {
                        System.out.println("\nLag 2 $$$$$$$$$$$$$$$$$$$$$ --- >>> Lagging Sync Detected - Score [" + scoreLag2 + "/" + mDebugSyncDetectScore + "] at symbol [" + mDebugSymbolCount + "]   $$$$$$$$$$$$$$$$$$$$$$");
                        optimizeSync(DMRSyncPattern.BASE_STATION_DATA, mLaggingSyncOffset2, mDebugSyncDetectScore);
                    }
                    else if(mDebugSyncDetectScore > syncDetectThreshold)
                    {
                        int missCount = Long.bitCount(mSyncEvaluate ^ DMRSyncPattern.BASE_STATION_DATA.getPattern());
                        mDebugTotalSyncDetects++;
                        mDebugTotalSyncBitErrors += missCount;

                        StringBuilder sb2 = new StringBuilder();
                        long delta = mSyncEvaluate ^ DMRSyncPattern.BASE_STATION_DATA.getPattern();

                        for(int y = 0; y < 48; y++)
                        {
                            long mask = Long.rotateLeft(1L, 47 - y);

                            if((mask & delta) == mask)
                            {
                                sb2.append("x");
                            }
                            else
                            {
                                sb2.append(".");
                            }
                        }

                        if(delta > 0)
                        {
                            optimizeSync(DMRSyncPattern.BASE_STATION_DATA, 0.0f, mDebugSyncDetectScore);
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append("[").append(sb2).append("] ");
                        sb.append("Sync At [").append(mDebugSymbolCount);
                        sb.append("] Next At [").append(mDebugSymbolCount + 144);
                        sb.append("] Elapsed Symbols [").append(elapsed);
                        sb.append("] Sync Score [").append(DECIMAL_FORMAT.format(mDebugSyncDetectScore));
                        sb.append("] Sync Bit Errors [").append(missCount);
                        sb.append("] Cumulative Errs [").append(mDebugTotalSyncBitErrors);
                        sb.append("] Avg/Sync [").append(mDebugTotalSyncBitErrors / mDebugTotalSyncDetects);
                        sb.append("] Cumulative Detects [").append(mDebugTotalSyncDetects).append("]");
                        System.out.println(sb);
                        mDebugLastCounterAtSymbolDetect = mDebugSymbolCount;
                    }
                    else //No sync detect
                    {
                        //Interpolated sample is in the delay line between indices 3 and 4.  Use those two samples to detect
                        //phasor rotation direction when calculating phase error of the interpolated sample against the ideal
                        //phase for that quadrant.
                        float timingError = calculate(symbol, mBuffer[mBufferPointer + 3], softSymbol, mBuffer[mBufferPointer + 4]);

                        //Adjust observed samples per symbol based on timing error
                        mObservedSamplesPerSymbol = mObservedSamplesPerSymbol + (timingError * OBSERVED_SAMPLES_PER_SYMBOL_GAIN);

                        //Constrain observed samples per symbol to min/max values
                        mObservedSamplesPerSymbol = FastMath.min(mObservedSamplesPerSymbol, mMaxSamplesPerSymbol);
                        mObservedSamplesPerSymbol = FastMath.max(mObservedSamplesPerSymbol, mMinSamplesPerSymbol);

                        mSamplePoint += (timingError * SAMPLE_COUNTER_GAIN);
                    }

                    //Add another symbol's worth of samples to the counter and adjust sample timing based on timing error
                    mSamplePoint += mObservedSamplesPerSymbol;
                }
            }
            //Do work here to move the mBufferPointer along.
        }
    }

    private void evaluate(DMRSyncPattern pattern, int bufferPointer, float samplePoint, float correlationScore)
    {
        //baseline is the start of the first sample of the first symbol of the sync pattern calculated from the current
        //buffer pointer and sample point which should be the final sample of the final symbol of the detected sync.
        float baseline = (bufferPointer + samplePoint) - (mSamplesPerSymbol * 24);
        int baselineIntegral = (int)Math.floor(baseline);
        float baselineFractional = baseline - baselineIntegral;

        for(int x = -5; x <= 5; x++)
        {
            float score = score(baselineIntegral + x, baselineFractional, mSamplesPerSymbol, pattern);

            if(x < 0)
            {
                System.out.println("\tOffset: " + x + " Legacy [" + correlationScore + "] optimized [" + score + "]");
            }
            else if(x == 0)
            {
                System.out.println("\tOffset:  " + x + " Legacy [" + correlationScore + "] optimized [" + score + "] <<<<<<");
            }
            else
            {
                System.out.println("\tOffset:  " + x + " Legacy [" + correlationScore + "] optimized [" + score + "]");
            }
        }
    }

    /**
     * Adjusts the symbol timing and symbol spacing to achieve the best sync correlation score
     * @param pattern that was detected
     * @param offset from current mBufferPointer and mSamplePoint.  This can be zero offset for the primary sync
     * detector or an offset for the lagging sync detectors.
     * @param correlationScore that triggered the original sync detect
     * @return true if symbol timing was adjusted and the updated sync score exceeds the optimized sync threshold.
     */
    private boolean optimizeSync(DMRSyncPattern pattern, float offset, float correlationScore)
    {
        //baseline is the start of the first sample of the first symbol of the sync pattern calculated from the current
        //buffer pointer and sample point which should be the final sample of the final symbol of the detected sync.
        float baseline = (mBufferPointer + mSamplePoint) + offset - (mSamplesPerSymbol * 24);
        int baselineIntegral = (int)Math.floor(baseline);
        float baselineFractional = baseline - baselineIntegral;

        for(int x = -5; x <= 5; x++)
        {
            float score = score(baselineIntegral + x, baselineFractional, mSamplesPerSymbol, pattern);

            if(x < 0)
            {
                System.out.println("\tOffset: " + x + " Legacy [" + correlationScore + " optimized [" + score + "]");
            }
            else if(x == 0)
            {
                System.out.println("\tOffset:  " + x + " Legacy [" + correlationScore + " optimized [" + score + "] <<<<<<");
            }
            else
            {
                System.out.println("\tOffset:  " + x + " Legacy [" + correlationScore + " optimized [" + score + "]");
            }
        }

        //Find the optimal position
        float stepSize = mSamplesPerSymbol / 10.0f;

        float adjustmentMax = mSamplesPerSymbol / 2.0f;
        float adjustment = 0.0f;
        float stepSizeMin = 0.03f;
        float candidate = baseline;

        int candidateIntegral = (int)Math.floor(candidate);
        float candidateFractional = candidate - candidateIntegral;
        float scoreCenter = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);

        candidate = baseline - stepSize;
        candidateIntegral = (int)Math.floor(candidate);
        candidateFractional = candidate - candidateIntegral;
        float scoreLeft = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);

        candidate = baseline + stepSize;
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

                candidate = baseline + adjustment - stepSize;
                candidateIntegral = (int)Math.floor(candidate);
                candidateFractional = candidate - candidateIntegral;
                scoreLeft = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);
            }
            else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
            {
                adjustment += stepSize;
                scoreLeft = scoreCenter;
                scoreCenter = scoreRight;

                candidate = baseline + adjustment + stepSize;
                candidateIntegral = (int)Math.floor(candidate);
                candidateFractional = candidate - candidateIntegral;
                scoreRight = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);
            }
            else
            {
                stepSize *= 0.5f;

                if(stepSize > stepSizeMin)
                {
                    candidate = baseline + adjustment - stepSize;
                    candidateIntegral = (int)Math.floor(candidate);
                    candidateFractional = candidate - candidateIntegral;
                    scoreLeft = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);

                    candidate = baseline + adjustment + stepSize;
                    candidateIntegral = (int)Math.floor(candidate);
                    candidateFractional = candidate - candidateIntegral;
                    scoreRight = score(candidateIntegral, candidateFractional, mSamplesPerSymbol, pattern);
                }
            }
        }


        //If we didn't get a correlation score above 90 after optimization return false to signal a false sync.
        if(scoreCenter < 90)
        {
            System.out.println("\n################ Aborting optimization - False Sync Detected ###################\n");
            mObservedSamplesPerSymbol = mSamplesPerSymbol;
            return false;
        }

        System.out.println("Timing Adjustment: " + adjustment + " Score:" + scoreCenter);

        if(offset != 0.0)
        {
            adjustment += offset;
            System.out.println("Timing Adjustment: " + adjustment + " With Offset [" + offset + "]");
        }

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

        //Reset the baseline to the updated buffer and sample pointers
        baseline = (mBufferPointer + mSamplePoint) - (mSamplesPerSymbol * 24);
        baselineIntegral = (int)Math.floor(baseline);
        baselineFractional = baseline - baselineIntegral;

        //Samples Per Symbol adjustment ********************
        adjustmentMax = mSamplesPerSymbol * MAXIMUM_DEVIATION_SAMPLES_PER_SYMBOL;
        System.out.println("Min: " + (mSamplesPerSymbol - adjustmentMax) + " Tgt:" + mSamplesPerSymbol + " Max: " + (mSamplesPerSymbol + adjustmentMax));
        adjustment = 0.0f;
        stepSizeMin = 0.00005f;
        stepSize = adjustmentMax / 10.0f;

        //We keep the updated pointer and sample point values and now focus on samples per symbol
        candidate = mSamplesPerSymbol - stepSize;
        scoreLeft = score(baselineIntegral, baselineFractional, candidate, pattern);
        candidate = mSamplesPerSymbol + stepSize;;
        scoreRight = score(baselineIntegral, baselineFractional, candidate, pattern);

        while(stepSize > stepSizeMin && Math.abs(adjustment) <= adjustmentMax)
        {
            if(scoreLeft > scoreRight && scoreLeft > scoreCenter)
            {
                adjustment -= stepSize;
                scoreRight = scoreCenter;
                scoreCenter = scoreLeft;
                candidate = mSamplesPerSymbol + adjustment - stepSize;
                scoreLeft = score(baselineIntegral, baselineFractional, candidate, pattern);
            }
            else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
            {
                adjustment += stepSize;
                scoreLeft = scoreCenter;
                scoreCenter = scoreRight;
                candidate = mSamplesPerSymbol + adjustment + stepSize;
                scoreRight = score(baselineIntegral, baselineFractional, candidate, pattern);
            }
            else
            {
                stepSize *= 0.5f;

                if(stepSize > stepSizeMin)
                {
                    candidate = mSamplesPerSymbol + adjustment - stepSize;
                    scoreLeft = score(baselineIntegral, baselineFractional, candidate, pattern);
                    candidate = mSamplesPerSymbol + adjustment + stepSize;
                    scoreRight = score(baselineIntegral, baselineFractional, candidate, pattern);
                }
            }
        }

        System.out.println("SPS Current: " + mObservedSamplesPerSymbol);
        System.out.println("SPS Updated: " + (mSamplesPerSymbol + adjustment) + " Adjustment: " + adjustment + " Score:" + scoreCenter);

        mObservedSamplesPerSymbol = mSamplesPerSymbol + adjustment;

        //Redo all symbol decisions in the delay line with the optimized timing
        float resampleStart = mBufferPointer + mSamplePoint;
        resampleStart -= (102 * mObservedSamplesPerSymbol);

        int resampleStartIntegral;
        for(int x = 0; x < 78; x++)
        {
            resampleStartIntegral = (int)Math.floor(resampleStart);
            float updatedSoftSymbol = mInterpolator.filter(mBuffer, resampleStartIntegral, resampleStart - resampleStartIntegral);
            Dibit updatedSymbol = toSymbol(updatedSoftSymbol);
            Dibit ejected = mDibitDelayLine.insert(updatedSymbol);
//            System.out.println("Replaced [" + ejected + "] with [" + updatedSymbol + "]");
            resampleStart += mObservedSamplesPerSymbol;
        }

        for(Dibit dibit: DMRSyncPattern.BASE_STATION_DATA.toDibits())
        {
            Dibit ejected = mDibitDelayLine.insert(dibit);
//            System.out.println("Replaced Sync [" + ejected + "] with [" + dibit + "]");
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
        mMaxSamplesPerSymbol = samplesPerSymbol * (1.0f + MAXIMUM_DEVIATION_SAMPLES_PER_SYMBOL);
        mMinSamplesPerSymbol = samplesPerSymbol * (1.0f - MAXIMUM_DEVIATION_SAMPLES_PER_SYMBOL);
        mBufferWorkspaceLength = (int)Math.ceil(BUFFER_WORKSPACE_LENGTH_DIBITS * samplesPerSymbol);
        int bufferLength = (int)(Math.ceil(BUFFER_LENGTH_DIBITS * samplesPerSymbol));
        mBuffer = new float[bufferLength];
        mBufferResetPoint = bufferLength - mBufferWorkspaceLength;
        mBufferLoadPointer = mBufferResetPoint;
        mBufferPointer = mBufferResetPoint;
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
