package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.c4fm.C4FMMessageFramer;
import io.github.dsheirer.dsp.c4fm.C4FMSymbolProcessor;
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
    //Equalizer span in symbols (14x T/2 taps).  The default (sync length = 23 symbols / 46 taps) has more taps than the
    //24 training symbols.  Testing against C4FM recordings: 3-13 symbols all outperform 23, with 7 offering the best
    //combination of valid message count and zero CRC failures.
    private static final int EQUALIZER_SYMBOL_LENGTH = 7;
    //Continuous Gardner symbol timing tracking gain between sync detections.  LDU voice frames have 864 symbols between
    //sync patterns.  Testing: 0.002 reduced bit errors 4-7% on a conventional voice recording (clean and with injected
    //noise) with ~1% fewer valid messages on a control channel at high noise; higher gains were less consistent.
    private static final double TIMING_TRACKING_GAIN = 0.002;
    private static final int SYMBOL_RATE = 4800;
    private static final double OUTER_SYMBOL_DEVIATION = 1800.0; //Hertz
    private static final float SYNC_THRESHOLD_DETECTED = 80;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 100;
    private static final float SYNC_THRESHOLD_EQUALIZED = 100;
    private static final float SOFT_SYMBOL_QUADRANT_BOUNDARY = (float)(Math.PI / 2.0);
    private final int mPlusSymbolCount;
    private final int mMinusSymbolCount;

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

        //Count the +3 and -3 symbols in the sync pattern for balance averaging (11x Plus 3 and 13x Minus 3)
        int plusCount = 0;
        for(float patternSymbol : mSyncDetector.getPatternSymbols())
        {
            if(patternSymbol > 0)
            {
                plusCount++;
            }
        }
        mPlusSymbolCount = plusCount;
        mMinusSymbolCount = mSyncDetector.getDibitLength() - plusCount;
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
    public double getTimingTrackingGain()
    {
        return TIMING_TRACKING_GAIN;
    }

    @Override
    public int getEqualizerSymbolLength()
    {
        return EQUALIZER_SYMBOL_LENGTH;
    }

    @Override
    public int getEqualizerSkipTrainingFrameCount()
    {
        return EQUALIZER_SKIP_TRAINING_FRAME_COUNT;
    }

    @Override
    public int getSymbolRate()
    {
        return SYMBOL_RATE;
    }

    @Override
    public double getOuterSymbolDeviation()
    {
        return OUTER_SYMBOL_DEVIATION;
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
        if(scoreCenter < getSyncThresholdOptimized())
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
        int start = (int)Math.floor(offset - ((mSyncDetector.getDibitLength() - 1) * mSamplesPerSymbol));
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
        int syncLength = mSyncDetector.getDibitLength();
        float[] patternSymbols = mSyncDetector.getPatternSymbols();
        Dibit[] patternDibits = mSyncDetector.getPatternDibits();

        //Resample each sync symbol, starting with the first symbol and ending with the final (detected) symbol.
        double resampleStart = offset + additionalOffset + timingCorrection - ((syncLength - 1) * mSamplesPerSymbol);
        int resampleStartIntegral = (int)Math.floor(resampleStart);
        float symbol, resampledSoftSymbol;
        float balancePlus3Symbols = 0;
        float balanceMinus3Symbols = 0;
        float gainAccumulator = 0;
        Dibit resampledDibit;
        int bitErrorCount = 0;

        for(int x = 0; x < syncLength; x++)
        {
            if(resampleStartIntegral >= 0)
            {
                symbol = patternSymbols[x];
                resampledSoftSymbol = interpolate(mBuffer[resampleStartIntegral], mBuffer[resampleStartIntegral + 1],
                        resampleStart - resampleStartIntegral);
                resampledSoftSymbol = resampledSoftSymbol * mSampleEqualizer.gain() + mSampleEqualizer.balance();
                resampledDibit = Dibit.fromSample(resampledSoftSymbol);
                bitErrorCount += patternDibits[x].getBitErrorFrom(resampledDibit);

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

        //Average the balance error separately for the +3 and -3 symbols so that a gain error doesn't bias the balance
        balancePlus3Symbols /= -mPlusSymbolCount;
        balanceMinus3Symbols /= -mMinusSymbolCount;
        float balanceCorrection = (balancePlus3Symbols + balanceMinus3Symbols) / 2f;
        balanceCorrection = Math.min(balanceCorrection, SOFT_SYMBOL_QUADRANT_BOUNDARY);
        balanceCorrection = Math.max(balanceCorrection, -SOFT_SYMBOL_QUADRANT_BOUNDARY);

        //            System.out.println("Balance [" + balanceAverage + "] Plus3 [" + balancePlus3Symbols + "] Minus3 [" + balanceMinus3Symbols + "]");
        //Convert the accumulated magnitude error into a gain adjustment.  All P25 sync symbols are +/-3.
        gainAccumulator = getGainAdjustment(mSyncDetector.getDibitLength() * Dibit.D01_PLUS_3.getIdealPhase(),
                gainAccumulator);

        //Recalculate the optimization score using the adjusted equalizer settings, at the same (detector and timing
        //adjusted) offset where the sync symbols were resampled.
        if(!mSynchronized)
        {
            optimizationScore = score(offset + additionalOffset + timingCorrection, mSamplesPerSymbol,
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


}
