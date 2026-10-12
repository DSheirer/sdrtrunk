package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.c4fm.C4FMMessageFramer;
import io.github.dsheirer.dsp.c4fm.C4FMSymbolProcessor;
import io.github.dsheirer.dsp.filter.interpolator.LinearInterpolator;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetectorFactory;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 * P25 Phase 1 C4FM implementation of the symbol processor.
 */
public class P25P1SymbolProcessorC4FM extends C4FMSymbolProcessor
{
    private static final int MAX_MESSAGE_DIBIT_LENGTH = P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1.getElapsedDibitLength();

    //TODO: adjust each of these values to the P25 waveform
    private static final int EQUALIZER_TRAINING_PERIOD_SYMBOL_COUNT = 600;
    private static final int EQUALIZER_SKIP_TRAINING_FRAME_COUNT = 5;
    private static final float SAMPLE_EQUALIZER_GAIN_MINIMUM = 4.0f;
    private static final float SAMPLE_EQUALIZER_GAIN_MAXIMUM = 6.2f;
    private static final float SYNC_THRESHOLD_DETECTED = 80;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 100;
    private static final float SYNC_THRESHOLD_EQUALIZED = 100;
    private static final float SOFT_SYMBOL_QUADRANT_BOUNDARY = (float)(Math.PI / 2.0);

    /**
     * Constructs an instance
     *
     * @param feedbackDecoder parent decoder
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public P25P1SymbolProcessorC4FM(FeedbackDecoder feedbackDecoder, C4FMMessageFramer messageFramer)
    {
        super(feedbackDecoder, messageFramer, P25P1SoftSyncDetectorFactory.getDetector(),
                P25P1SoftSyncDetectorFactory.getDetector());
    }

    /**
     * Dynamic balance tracking (ie decision directed PLL adjustments) is enabled for this decoder.
     */
    @Override
    public boolean isDynamicBalanceTrackingRequired()
    {
        return true;
    }

    @Override
    public int getMaxMessageDibitLength()
    {
        return MAX_MESSAGE_DIBIT_LENGTH;
    }

    @Override
    public int getEqualizerTrainingPeriodDurationSymbolCount()
    {
        return EQUALIZER_TRAINING_PERIOD_SYMBOL_COUNT;
    }

    @Override
    public int getEqualizerSkipTrainingFrameCount()
    {
        return EQUALIZER_SKIP_TRAINING_FRAME_COUNT;
    }

    @Override
    public float getSampleGainMinimum()
    {
        return SAMPLE_EQUALIZER_GAIN_MINIMUM;
    }

    @Override
    public float getSampleGainMaximum()
    {
        return SAMPLE_EQUALIZER_GAIN_MAXIMUM;
    }

    @Override
    public float getSyncThresholdDetected()
    {
        return SYNC_THRESHOLD_DETECTED;
    }

    @Override
    public float getSyncThresholdOptimized()
    {
        return SYNC_THRESHOLD_OPTIMIZED;
    }

    @Override
    public float getSyncThresholdEqualized()
    {
        return SYNC_THRESHOLD_EQUALIZED;
    }

    /**
     * On sync detection, calculates the optimal timing adjustment that achieves the highest sync correlation score
     * or returns an INVALID_SYNC_DETECTION sentinel value if the correlation score doesn't exceed the threshold.
     *
     * @param additionalOffset from current bufferPointer and samplePoint to adjust for which sync detector
     * triggered the event. This can be zero offset for the primary sync detector or a lagging (ie half symbol)
     * offset for the lagging sync detector.
     * @return optimized timing adjustment or NO_OPTIMIZATION sentinel value.
     */
    @Override
    public Correction optimize(double additionalOffset, float detectionScore, double bufferOffset)
    {
        //Offset points to the final symbol/sample in the buffer for the detected sync pattern.
        double offset = bufferOffset + additionalOffset;

        //Reject any sync detections where the sample:sample standard deviation exceeds the noise threshold.
        if(isNoisy(offset))
        {
            return INVALID_SYNC_DETECTION;
        }

        //Find the optimal symbol timing
        double samplesPerSymbol = mSamplesPerSymbol;
        double stepSize = samplesPerSymbol / (mSynchronized ? 16.0 : 8.0); //Start at 1/8th for coarse & 1/16th for fine
        double stepSizeMin = mOptimizeFineIncrement;
        double adjustment = 0.0;

        //In coarse sync mode, constrain max adjustment to half a symbol period so that a lagging sync detect doesn't
        //preempt a primary sync detect prematurely and cause a significant timing correction.  So, we constrain the
        //max correction to a half symbol period for coarse sync mode.
        double adjustmentMax = samplesPerSymbol / 2.0;
        double candidate = offset;

        float scoreCenter = score(candidate, samplesPerSymbol);

        candidate = offset - stepSize;
        float scoreLeft = score(candidate, samplesPerSymbol);

        candidate = offset + stepSize;
        float scoreRight = score(candidate, samplesPerSymbol);

        while(stepSize > stepSizeMin && Math.abs(adjustment) <= adjustmentMax)
        {
            if(scoreLeft > scoreRight && scoreLeft > scoreCenter)
            {
                adjustment -= stepSize;
                scoreRight = scoreCenter;
                scoreCenter = scoreLeft;

                candidate = offset + adjustment - stepSize;
                scoreLeft = score(candidate, samplesPerSymbol);
            }
            else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
            {
                adjustment += stepSize;
                scoreLeft = scoreCenter;
                scoreCenter = scoreRight;

                candidate = offset + adjustment + stepSize;
                scoreRight = score(candidate, samplesPerSymbol);
            }
            else
            {
                stepSize *= 0.5f;

                if(stepSize > stepSizeMin)
                {
                    candidate = offset + adjustment - stepSize;
                    scoreLeft = score(candidate, samplesPerSymbol);

                    candidate = offset + adjustment + stepSize;
                    scoreRight = score(candidate, samplesPerSymbol);
                }
            }
        }

        //If we didn't find an optimal correlation score above the threshold, return a false sync.
        if(scoreCenter < SYNC_THRESHOLD_OPTIMIZED)
        {
            return INVALID_SYNC_DETECTION;
        }

        return getCorrection(additionalOffset, adjustment, detectionScore, scoreCenter, bufferOffset);
    }

    /**
     * Indicates if the samples in the sample buffer that contain a detected sync pattern have a standard deviation
     * that is lower than the expected sample to sample deviation for a modulated signal.  False detects against
     * noise tend to have a standard deviation that is 2x the expected value.
     * @param offset to the sample representing the final symbol in the detected sync pattern.
     * @return true if the standard deviation is more than expected.
     */
    public boolean isNoisy(double offset)
    {
        StandardDeviation standardDeviation = new StandardDeviation();
        int start = (int)Math.floor(offset - (23 * mSamplesPerSymbol));
        int end = (int)Math.ceil(offset);
        end = Math.min(end, mBuffer.length - 1);

        for(int i = start; i < end; i++)
        {
            standardDeviation.increment(mBuffer[i] - mBuffer[i + 1]);
        }

        double threshold = Dibit.D01_PLUS_3.getIdealPhase() * 2 / mSamplesPerSymbol * 1.2; //120% of optimal
        return standardDeviation.getResult() > threshold;
    }

    /**
     * Calculates a correction update for the equalizer balance and gain when the sync pattern is detected in the
     * sample buffer, using the supplied timing correction argument.  Resamples the sync symbols and compares each
     * soft symbol to the ideal symbol phase to develop average error measurements for balance and gain.  On initial
     * sync detection, the equalizer settings are applied to the samples in the buffer allowing the symbols to be
     * resampled during coarse sync acquisition.
     */
    public Correction getCorrection(double additionalOffset, double timingCorrection, float detectionScore,
                                    float optimizationScore, double offset)
    {
        double resampleStart = offset + additionalOffset + timingCorrection;
        int resampleStartIntegral = (int)Math.floor(resampleStart);
        float symbol = mSyncDetector.getPatternSymbols()[23];
        float resampledSoftSymbol = LinearInterpolator.calculate(mBuffer[resampleStartIntegral],
                mBuffer[resampleStartIntegral + 1], resampleStart - resampleStartIntegral);
        resampledSoftSymbol = resampledSoftSymbol * mSampleEqualizer.gain() + mSampleEqualizer.balance();

        float balancePlus3Symbols = 0;
        float balanceMinus3Symbols = resampledSoftSymbol - symbol;
        float gainAccumulator = Math.abs(symbol) - Math.abs(resampledSoftSymbol);
        Dibit resampledDibit = Dibit.fromSample(resampledSoftSymbol);
        int bitErrorCount = mSyncDetector.getPatternDibits()[23].getBitErrorFrom(resampledDibit);

        resampleStart -= (23 * mSamplesPerSymbol);
        resampleStartIntegral = (int)Math.floor(resampleStart);

        for(int x = 0; x < 23; x++)
        {
            if(resampleStartIntegral >= 0)
            {
                symbol = mSyncDetector.getPatternSymbols()[x];
                resampledSoftSymbol = LinearInterpolator.calculate(mBuffer[resampleStartIntegral],
                        mBuffer[resampleStartIntegral + 1], resampleStart - resampleStartIntegral);
                resampledSoftSymbol = resampledSoftSymbol * mSampleEqualizer.gain() + mSampleEqualizer.balance();
                resampledDibit = Dibit.fromSample(resampledSoftSymbol);
                bitErrorCount += mSyncDetector.getPatternDibits()[x].getBitErrorFrom(resampledDibit);

                if(symbol > 0)
                {
                    balancePlus3Symbols += (resampledSoftSymbol - symbol);
                }
                else
                {
                    balanceMinus3Symbols += (resampledSoftSymbol - symbol);
                }

                gainAccumulator += Math.abs(symbol) - Math.abs(resampledSoftSymbol);
            }

            resampleStart += mSamplesPerSymbol;
            resampleStartIntegral = (int)Math.floor(resampleStart);
        }

        balancePlus3Symbols /= -11.0f; //There are 11x Plus 3 and 13x Minus 3 symbols in the sync pattern.
        balanceMinus3Symbols /= -13.0f;
        float balanceCorrection = (balancePlus3Symbols + balanceMinus3Symbols) / 2f;
        balanceCorrection = Math.min(balanceCorrection, SOFT_SYMBOL_QUADRANT_BOUNDARY);
        balanceCorrection = Math.max(balanceCorrection, -SOFT_SYMBOL_QUADRANT_BOUNDARY);

        //            System.out.println("Balance [" + balanceAverage + "] Plus3 [" + balancePlus3Symbols + "] Minus3 [" + balanceMinus3Symbols + "]");
        gainAccumulator /= (24.0f * Dibit.D01_PLUS_3.getIdealPhase());

        //Recalculate the optimization score using the adjusted equalizer settings
        if(!mSynchronized)
        {
            optimizationScore = score(offset + timingCorrection, mSamplesPerSymbol,
                    mSampleEqualizer.balance() + balanceCorrection, mSampleEqualizer.gain() + gainAccumulator);
        }

        return new Correction(additionalOffset, timingCorrection, balanceCorrection, gainAccumulator, detectionScore,
                optimizationScore, bitErrorCount);
    }


    /**
     * Calculates the sync correlation score at the specified offset and samples per symbol interval using the
     * current equalizer balance and gain values.
     *
     * @param offset to the final symbol in the soft symbol buffer
     * @param samplesPerSymbol spacing to test for.
     * @return correlation score.
     */
    public float score(double offset, double samplesPerSymbol)
    {
        return score(offset, samplesPerSymbol, mSampleEqualizer.balance(), mSampleEqualizer.gain());
    }

    /**
     * Calculates the sync correlation score at the specified offset and samples per symbol interval.
     * @param offset to the final symbol in the soft symbol buffer
     * @param samplesPerSymbol spacing to test for.
     * @return correlation score.
     */
    public float score(double offset, double samplesPerSymbol, float balance, float gain)
    {
        int maxPointer = mBuffer.length - 1;
        float softSymbol;

        double pointer = offset - (samplesPerSymbol * 23.0);
        int bufferPointer = (int)Math.floor(pointer);
        double fractional = pointer - bufferPointer;

        float score = 0;

        for(int x = 0; x < 24; x++)
        {
            if(bufferPointer < maxPointer)
            {
                softSymbol = LinearInterpolator.calculate(mBuffer[bufferPointer], mBuffer[bufferPointer + 1], fractional);
                softSymbol = (softSymbol + balance) * gain;
            }
            else
            {
                softSymbol = 0.0f;
            }

            score += softSymbol * mSyncDetector.getPatternSymbols()[x];
            pointer += samplesPerSymbol;
            bufferPointer = (int)Math.floor(pointer);
            fractional = pointer - bufferPointer;
        }

        return score;
    }


}
