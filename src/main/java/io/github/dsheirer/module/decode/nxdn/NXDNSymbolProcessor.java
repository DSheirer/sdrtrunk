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

package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.dsp.filter.interpolator.LinearInterpolator;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.nxdn.layer1.C4FMEqualizer;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.NXDNSoftSyncDetector;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.NXDNSyncDetector;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.NXDNSyncDetectorFactory;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.ReferenceSyncWaveform;
import io.github.dsheirer.sample.Listener;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 * NXDN symbol processor provides symbol timing recovery and sync detection.
 */
public class NXDNSymbolProcessor
{
    private static final float SYNC_THRESHOLD_DETECTION = 37;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 40;
    private static final float SYNC_THRESHOLD_EQUALIZED = 40;
    private static final float[] SYNC_SYMBOLS = NXDNSyncDetector.SYMBOLS;
    private static final int BUFFER_WORKSPACE_LENGTH = 1024;
    private static final int DIBIT_LENGTH_NID = 33; //32 dibits (64 bits) +1 status
    private static final int DIBIT_LENGTH_SYNC = 10;
    private static final Correction INVALID_SYNC_DETECTION = new Correction(0d, Double.MAX_VALUE, 0f, 0f, 0f, 0f, 0);
    private static final DecimalFormat DF = new DecimalFormat("00.000");
    private static final Dibit[] SYNC_DIBITS = NXDNSyncDetector.DIBITS;
    private final DibitToByteBufferAssembler mDibitAssembler = new DibitToByteBufferAssembler(300);
    private final FeedbackDecoder mFeedbackDecoder;
    private final NXDNMessageFramer mMessageFramer;
    private final NXDNSoftSyncDetector mSyncDetector = NXDNSyncDetectorFactory.getStandardDetector();
    private final NXDNSoftSyncDetector mSyncDetectorLagging = NXDNSyncDetectorFactory.getStandardDetector();
    private final SampleEqualizer mSampleEqualizer = new SampleEqualizer();
    private final C4FMEqualizer mSymbolEqualizer;
    private ReferenceSyncWaveform mReferenceSyncWaveform;
    private double mNoiseStandardDeviationThreshold;
    private double mSamplePoint;
    private double mSamplesPerSymbol;
    private double mTimingCorrectionThreshold;
    private float[] mBuffer;
    private float mLaggingSyncOffset;
    private float mOptimizeFineIncrement;
    private int mBufferPointer;
    private int mBufferReloadThreshold;
    private int mDelayedSyncNotificationSymbolsRemaining = 0;
    private int mSymbolsSinceLastSync = 0;
    private boolean mSynchronized = false;
    private boolean mDelayedSyncNotificationPending = false;

//    private final boolean mDebugVisualize = false;
//    private final int mDebugSymbolStart = 0;
//    private ISyncResultsListener mSyncResultsViewer;
//    private int mDebugSymbolCounter = 0;
//    private int mDebugSampleCounter = 0;

    /**
     * Constructs an instance
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public NXDNSymbolProcessor(NXDNMessageFramer messageFramer, FeedbackDecoder feedbackDecoder)
    {
        mMessageFramer = messageFramer;
        mFeedbackDecoder = feedbackDecoder;
        mSymbolEqualizer = new C4FMEqualizer(mSyncDetector.getSyncSymbols(), 9, 192 * 3, 10);
        DF.setPositivePrefix(" ");
    }

    /**
     * Finalize and prepare for shutdown.
     */
    public void dispose()
    {
        mDibitAssembler.flush();
    }

    /**
     * Resets the balance
     */
    public void resetBalance()
    {
        mSampleEqualizer.reset();
    }

    /**
     * Primary input method for receiving a stream of demodulated samples to process into symbols.
     * @param samples to process
     */
    public void process(float[] samples)
    {
        //Create shadow copy of heap variables on stack
        double samplePoint = mSamplePoint;
        double samplesPerSymbol = mSamplesPerSymbol;
        int bufferPointer = mBufferPointer;
        int bufferReloadThreshold = mBufferReloadThreshold;
        int symbolsSinceLastSync = mSymbolsSinceLastSync;

        int samplesPointer = 0;
        float softSymbol, scorePrimary;
        Correction correctionCandidate;

        if(!mSynchronized && samples.length > 400)
        {
            StandardDeviation sd = new StandardDeviation();

            float accumulator = 0.0f;

            for(int i = 0; i < 400; i++)
            {
                accumulator += samples[i];
                sd.increment(samples[i]);
            }

            //Only apply a balance correction when we have signal.  Noise = .45-.55, Signal = 0.00 - 0.20
            if(sd.getResult() < 0.3)
            {
                accumulator /= 400;
                accumulator += mSampleEqualizer.mBalance;
                mSampleEqualizer.mBalance -= (accumulator * .3f);
            }
        }

        while(samplesPointer < samples.length)
        {
            //Note: buffer pointer can become greater than reload threshold during timing optimization at sync detect
            if(bufferPointer >= bufferReloadThreshold)
            {
                //Do reload
                int copyLength = Math.min(BUFFER_WORKSPACE_LENGTH, samples.length - samplesPointer);
                System.arraycopy(mBuffer, copyLength, mBuffer, 0, mBuffer.length - copyLength);
                System.arraycopy(samples, samplesPointer, mBuffer, mBuffer.length - copyLength, copyLength);
                samplesPointer += copyLength;
                bufferPointer -= copyLength;
            }

            while(bufferPointer < bufferReloadThreshold)
            {
                bufferPointer++;
                samplePoint--;

//                mDebugSampleCounter++;

                if(samplePoint < 1)
                {
//                    mDebugSymbolCounter++;
//
//                    if(mDebugSymbolCounter % 10 == 0)
//                    {
//                        visualizeSymbols(bufferPointer, samplePoint);
//                    }

                    symbolsSinceLastSync++;

                    //Clear synchronized state when we miss a sync period.
                    if(mSynchronized && symbolsSinceLastSync > 192)
                    {
                        mSampleEqualizer.reset();
                        mSymbolEqualizer.syncLost();
                        mSynchronized = false;
                    }

                    softSymbol = mSampleEqualizer.getEqualizedSymbol(mBuffer[bufferPointer], mBuffer[bufferPointer + 1], samplePoint);

                    //Check for lagging sync pattern
                    float lag = (float)(bufferPointer + samplePoint - mLaggingSyncOffset);
                    int lagIntegral = (int)Math.floor(lag);
                    float softSymbolLag = mSampleEqualizer.getEqualizedSymbol(mBuffer[lagIntegral], mBuffer[lagIntegral + 1], lag - lagIntegral);
                    float scoreLag = mSyncDetectorLagging.process(softSymbolLag);
                    scorePrimary = mSyncDetector.process(softSymbol);

                    correctionCandidate = INVALID_SYNC_DETECTION;

                    if(scorePrimary > SYNC_THRESHOLD_DETECTION && scorePrimary > scoreLag)
                    {
                        correctionCandidate = mSampleEqualizer.optimize(0, scorePrimary, (bufferPointer + samplePoint));
                    }
                    else if(scoreLag > SYNC_THRESHOLD_DETECTION)
                    {
                        correctionCandidate = mSampleEqualizer.optimize(-mLaggingSyncOffset, scoreLag, (bufferPointer + samplePoint));
                    }

                    //Equalizer processes current soft symbol and returns a delayed soft symbol that is equalized
                    softSymbol = mSymbolEqualizer.process(softSymbolLag, softSymbol);

                    //Broadcast the decoded soft symbol to an optionally registered listener (ie Symbol view in channel panel).
                    Dibit symbol = Dibit.fromSample(softSymbol);
                    mFeedbackDecoder.broadcast(softSymbol);

//                    if(mDebugVisualize)
//                    {
//                        getSyncResultsViewer().symbol(softSymbol);
//                    }

                    //When the message framer emits at least one valid message (ie CRC check passes), send the current
                    // sample equalizer's balance value as a pseudo-PLL error measurement.
                    if(mMessageFramer.process(symbol))
                    {
                        mFeedbackDecoder.processPLLError(mSampleEqualizer.balance());
                    }

                    mDibitAssembler.receive(symbol);

                    //Do a final validation against the reference sync waveform
                    if(correctionCandidate.isValid())
                    {
                        correctionCandidate = validateSyncWaveform(correctionCandidate, bufferPointer, samplePoint);
                    }

                    if(correctionCandidate.isValid())
                    {
                        mSampleEqualizer.apply(correctionCandidate);

                        if(mSynchronized)
                        {
                            double adjustment = Math.clamp(correctionCandidate.getTimingCorrection(),
                                    -mTimingCorrectionThreshold, mTimingCorrectionThreshold);
                            samplePoint += adjustment;
                        }
                        else
                        {
                            samplePoint += correctionCandidate.getTimingCorrection();
                        }

                        mDelayedSyncNotificationPending = true;
                        mDelayedSyncNotificationSymbolsRemaining = mSymbolEqualizer.getDelaySymbolCount();
                        mSynchronized = true;
                        symbolsSinceLastSync = 0;

                        //If the equalizer was disabled, it has bad samples contaminating the training buffer and we
                        // need to prime that buffer with aligned samples prior to sync detection training.
                        if(!mSymbolEqualizer.isEnabled())
                        {
                            int symbols = mSyncDetector.getSyncSymbols().length + mSymbolEqualizer.getLength() - mSymbolEqualizer.getDelaySymbolCount();
                            double offset = bufferPointer + samplePoint - ((symbols - 1) * samplesPerSymbol) - mLaggingSyncOffset;

                            float mid, sym;
                            for(int s = 0; s < symbols; s++)
                            {
                                //Mid symbol

                                mid = mSampleEqualizer.getEqualizedSymbol(offset);
                                offset += mLaggingSyncOffset;
                                sym = mSampleEqualizer.getEqualizedSymbol(offset);
                                offset += mLaggingSyncOffset;
                                mSymbolEqualizer.process(mid, sym);
                            }
                        }

                        mSymbolEqualizer.syncDetected();
                        mSymbolEqualizer.enable();
//                        visualizeSyncDetect(scorePrimary, true, "DETECT - POST", bufferPointer, samplePoint);
                    }
                    else if(mDelayedSyncNotificationPending)
                    {
                        mDelayedSyncNotificationSymbolsRemaining--;

                        if(mDelayedSyncNotificationSymbolsRemaining <= 0)
                        {
                            mMessageFramer.syncDetected();
                            mDelayedSyncNotificationPending = false;
                        }
                    }

                    //Add another symbol's worth of samples to the counter for processing
                    samplePoint += samplesPerSymbol;
                }
            }
        }

        //Copy shadow variables back to the heap
        mBufferPointer = bufferPointer;
        mSamplePoint = samplePoint;
        mSymbolsSinceLastSync = symbolsSinceLastSync;
    }

    /**
     * Validates the sync detection and candidate correction settings against a reference instance of the sync detection
     * waveform.  If the detected sync correlates well against the reference sync then the correction is valid.
     * Otherwise, this method returns the static invalid sync correction to signal that the sync detection was invalid.
     * @param correction to evaluate
     * @param bufferPointer for the final (optimized) symbol in the buffer
     * @param samplePoint offset from the buffer pointer to the final symbol
     * @return the original correction or the static invalid sync correction
     */
    private Correction validateSyncWaveform(Correction correction, int bufferPointer, double samplePoint)
    {
        float gain = mSampleEqualizer.mGain + correction.getGainAdjustment();
        float balance = mSampleEqualizer.mBalance + correction.getBalanceAdjustment();
        double offset = bufferPointer + samplePoint + correction.getTimingCorrection() - mReferenceSyncWaveform.samples().length;
        int integral = (int)Math.ceil(offset);
        double mu = integral - offset;
        double accumulator = 0.0;
        int index;

        //Evaluate the reference waveform in reverse order since the final symbol is aligned to the final sample
        for(int x = mReferenceSyncWaveform.samples().length - 1; x >= 0; x--)
        {
            index = integral + x;
            float sample = LinearInterpolator.calculate(mBuffer[index], mBuffer[index + 1], mu);
            sample += balance;
            sample *= gain;
            sample = Math.clamp(sample, -mReferenceSyncWaveform.maxSample(), mReferenceSyncWaveform.maxSample());
            accumulator += mReferenceSyncWaveform.samples()[x] * sample;
        }

        double threshold = mReferenceSyncWaveform.idealCorrelationScore() * 0.85; //85% of the ideal correlation score

        //Return the correction as valid if the correlation score exceeds the threshold, otherwise return the static
        // invalid sync detection to nullify the correction.
        if(accumulator > threshold)
        {
            return correction;
        }

        return INVALID_SYNC_DETECTION;
    }

    /**
     * Registers the listener to receive demodulated bit stream buffers.
     * @param listener to register
     */
    public void setBufferListener(Listener<ByteBuffer> listener)
    {
        mDibitAssembler.setBufferListener(listener);
    }

    /**
     * Indicates if there is a registered buffer listener
     */
    public boolean hasBufferListener()
    {
        return mDibitAssembler.hasBufferListeners();
    }

    /**
     * Sets or updates the samples per symbol
     * @param samplesPerSymbol to apply.
     */
    public void setSamplesPerSymbol(float samplesPerSymbol)
    {
        mSamplesPerSymbol = samplesPerSymbol;
        mSamplePoint = samplesPerSymbol;
        mTimingCorrectionThreshold = samplesPerSymbol / 30;
        mNoiseStandardDeviationThreshold = .2;
        mOptimizeFineIncrement = samplesPerSymbol / 200.0f;
        mLaggingSyncOffset = samplesPerSymbol / 2.0f;
        int bufferLength = BUFFER_WORKSPACE_LENGTH + (int)(Math.ceil((DIBIT_LENGTH_SYNC + DIBIT_LENGTH_NID + 2) * samplesPerSymbol));
        mBuffer = new float[bufferLength];
        mBufferReloadThreshold = mBuffer.length - (int)Math.ceil(samplesPerSymbol * (DIBIT_LENGTH_NID + 1));
        mBufferPointer = mBufferReloadThreshold;
        mSampleEqualizer.configure(samplesPerSymbol);
        mReferenceSyncWaveform = mSyncDetector.getReferenceWaveform(samplesPerSymbol);
    }

//    /**
//     * Debug viewer UI for visualizing sync detections in the sample stream/waveform.
//     */
//    private ISyncResultsListener getSyncResultsViewer()
//    {
//        if(mSyncResultsViewer == null)
//        {
//            mSyncResultsViewer = new FMSyncResultsViewer();
//        }
//
//        return mSyncResultsViewer;
//    }
//
//    /**
//     * Debug method to visualize the contents of the sync detection for the samples and symbols and symbol timing.
//     * @param score from the sync detector
//     * @param primary sync detector (true) or secondary (false)
//     */
//    public void visualizeSyncDetect(float score, boolean primary, String tag, int bufferPointer, double samplePoint)
//    {
//        if(!mDebugVisualize)
//        {
//            return;
//        }
//
//        int integral = (int)Math.floor(samplePoint);
//        samplePoint -= integral;
//        bufferPointer += integral;
//
//        int offset = 3;
//        int length = (int)Math.ceil(mSamplesPerSymbol * 9) + (2 * offset);
//        int end = bufferPointer + offset;
//        int start = end - length;
//        float[] symbols = new float[10];
//        float[] samples = Arrays.copyOfRange(mBuffer, start, end);
//        for(int i = 0; i < samples.length; i++)
//        {
//            samples[i] = mSampleEqualizer.equalize(samples[i]);
//        }
//
//        double symbolPointer = bufferPointer + samplePoint - (mSamplesPerSymbol * 9);
//        int symbolIntegral = (int)Math.floor(symbolPointer);
//        double mu = symbolPointer - symbolIntegral;
//
//        for(int x = 0; x < 10; x++)
//        {
//            symbols[x] = mSampleEqualizer.getEqualizedSymbol(mBuffer[symbolIntegral], mBuffer[symbolIntegral + 1], mu);
//            symbolPointer += mSamplesPerSymbol;
//            symbolIntegral = (int)Math.floor(symbolPointer);
//            mu = symbolPointer - symbolIntegral;
//        }
//
//        float[] syncIntervals = new float[10];
//        int adjust = bufferPointer - length + offset;
//        double pointer = bufferPointer + samplePoint - adjust;
//
//        for(int x = 9; x >= 0; x--)
//        {
//            syncIntervals[x] = (float)pointer;
//            pointer -= mSamplesPerSymbol;
//        }
//
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        getSyncResultsViewer().receive(symbols, mSyncDetector.getSyncSymbols(), samples, syncIntervals, mSampleEqualizer.mBalance, mSampleEqualizer.mGain,
//                tag + " Score: " + score + (primary ? " PRIMARY " : " SECONDARY ") + (mSynchronized ? "FINE " : "COARSE ") +
//                        " EQ-B:" + mSampleEqualizer.mBalance + " EQ-G:" + mSampleEqualizer.mGain + " " +
//                        " SAM:" + mDebugSampleCounter + " SYM:" + mDebugSymbolCounter + " DUR:" + DF.format(mDebugSymbolCounter / 2400.0),
//                countDownLatch);
//
//        try
//        {
//            countDownLatch.await();
//        }
//        catch(InterruptedException e)
//        {
//            throw new RuntimeException(e);
//        }
//    }
//
//    /**
//     * Debug method to visualize the symbol decisions
//     */
//    public void visualizeSymbols(int bufferPointer, double samplePoint)
//    {
//        if(!mDebugVisualize)
//        {
//            return;
//        }
//
//        if(mDebugSymbolCounter < mDebugSymbolStart)
//        {
//            return;
//        }
//
//        int offset = 3;
//        int length = (int)Math.ceil(mSamplesPerSymbol * 9) + (2 * offset);
//        int end = bufferPointer + offset;
//        int start = end - length;
//        float[] symbols = new float[10];
//        float[] decisions = new float[10];
//        float[] samples = Arrays.copyOfRange(mBuffer, start, end);
//        for(int i = 0; i < samples.length; i++)
//        {
//            samples[i] = mSampleEqualizer.equalize(samples[i]);
//        }
//
//        double symbolPointer = bufferPointer + samplePoint - (mSamplesPerSymbol * 9);
//        int symbolIntegral = (int)Math.floor(symbolPointer);
//        double mu = symbolPointer - symbolIntegral;
//
//        for(int x = 0; x < 10; x++)
//        {
//            symbols[x] = mSampleEqualizer.getEqualizedSymbol(mBuffer[symbolIntegral], mBuffer[symbolIntegral + 1], mu);
//            decisions[x] = Dibit.fromSample(symbols[x]).getIdealPhase();
//            symbolPointer += mSamplesPerSymbol;
//            symbolIntegral = (int)Math.floor(symbolPointer);
//            mu = symbolPointer - symbolIntegral;
//        }
//
//        float[] syncIntervals = new float[10];
//        int adjust = bufferPointer - length + offset;
//        double pointer = bufferPointer + samplePoint - adjust;
//
//        for(int x = 9; x >= 0; x--)
//        {
//            syncIntervals[x] = (float)pointer;
//            pointer -= mSamplesPerSymbol;
//        }
//
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        getSyncResultsViewer().receive(symbols, decisions, samples, syncIntervals, mSampleEqualizer.mBalance, mSampleEqualizer.mGain,
//                "Balance:" + mSampleEqualizer.mBalance + " Gain:" + mSampleEqualizer.mGain +
//                        " SAM:" + mDebugSampleCounter + " SYM:" + mDebugSymbolCounter + " DUR:" + DF.format(mDebugSymbolCounter / 2400.0), countDownLatch);
//
//        try
//        {
//            countDownLatch.await();
//        }
//        catch(InterruptedException e)
//        {
//            throw new RuntimeException(e);
//        }
//    }

    /**
     * Calculates the standard deviation of the samples at the specified offset that aligns with a sync detection.
     * @param offset to the sample representing the final symbol in the detected sync pattern.
     * @return standard deviation.
     */
    public double getStandardDeviation(double offset)
    {
        StandardDeviation standardDeviation = new StandardDeviation();
        int start = (int)Math.floor(offset - (9 * mSamplesPerSymbol));
        int end = (int)Math.ceil(offset);
        end = Math.min(end, mBuffer.length - 1);

        for(int i = start; i < end; i++)
        {
            standardDeviation.increment(mBuffer[i] - mBuffer[i + 1]);
        }

        return standardDeviation.getResult();
    }

    /**
     * Symbol timing and equalizer correction candidate settings
     */
    public static class Correction
    {
        private final double mTimingAdjustment;
        private final double mAdditionalOffset;
        private final float mBalanceAdjustment;
        private final float mGainAdjustment;
        private final float mDetectionScore;
        private final float mOptimizationScore;
        private int mCorrectedBitCount = 0;

        /**
         * Constructs an instance
         * @param timingAdjustment correction for symbol sampling
         * @param balanceAdjustment correction for equalizer
         * @param gainAdjustment correction for equalizer
         * @param bitErrorCount in the detected sync pattern
         */
        public Correction(double additionalOffset, double timingAdjustment, float balanceAdjustment, float gainAdjustment,
                          float detectionScore, float optimizationScore, int bitErrorCount)
        {
            mAdditionalOffset = additionalOffset;
            mTimingAdjustment = timingAdjustment;
            mBalanceAdjustment = balanceAdjustment;
            mGainAdjustment = gainAdjustment;
            mDetectionScore = detectionScore;
            mOptimizationScore = optimizationScore;
            mCorrectedBitCount = bitErrorCount;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            if(isValid())
            {
                sb.append("\tCORRECTION - Bit Errors [").append(mCorrectedBitCount).append("]");
            }
            else
            {
                sb.append("CORRECTION - INVALID");
            }

            sb.append(" At Offset [").append(mAdditionalOffset);
            sb.append("] Adjustments-Timing [").append(mTimingAdjustment);
            sb.append("] Balance [").append(mBalanceAdjustment);
            sb.append("] Gain [").append(mGainAdjustment);
            sb.append("] Detect Score [").append(mDetectionScore);
            sb.append("] Optimize Score [").append(mOptimizationScore);
            sb.append("]");

            if(!hasValidTiming())
            {
                sb.append(" >> INVALID TIMING");
            }

            return sb.toString();
        }

        /**
         * Candidate timing correction
         * @return correction
         */
        public double getTimingCorrection()
        {
            return mAdditionalOffset + mTimingAdjustment;
        }

        /**
         * Equalizer balance correction value.
         * @return correction
         */
        public float getBalanceAdjustment()
        {
            return mBalanceAdjustment;
        }

        /**
         * Equalizer gain correction value
         * @return correction.
         */
        public float getGainAdjustment()
        {
            return mGainAdjustment;
        }

        /**
         * Number of bits that were corrected across the SYNC and NID
         * @return corrected bit count.
         */
        public int getCorrectedBitCount()
        {
            return mCorrectedBitCount;
        }

        /**
         * Indicates if this correction has valid timing adjustment information.
         * @return true if the timing correction is not the default (ie no timing) double max value.
         */
        public boolean hasValidTiming()
        {
            return getTimingCorrection() != Double.MAX_VALUE;
        }

        /**
         * Indicates if this correction has a valid NID or the optimization (or detection) score is high quality.
         * @return true if valid.
         */
        public boolean isValid()
        {
            return mOptimizationScore > SYNC_THRESHOLD_EQUALIZED;
        }
    }

    /**
     * Sample equalizer providing gain and balance (DC offset compensation).
     *
     * Note: equalizer is initialized with a gain value of 1.219, as determined through significant testing against
     * both P25 and DMR C4FM modulated samples that employ the same pulse shaping filters.  Gain value is adjusted and
     * optimized with each sync detection.
     */
    class SampleEqualizer
    {
        private static final float EQUALIZER_LOOP_GAIN = 0.15f;
        private static final float EQUALIZER_MAXIMUM_BALANCE = (float)(Math.PI / 3.0); //+/- 800 Hz
        private static final float EQUALIZER_RECALIBRATE_THRESHOLD = (float)(Math.PI / 8.0);
        private float mBalance = 0.0f;
        private float mGain = 5.7f;
        private float mGainInitial = 5.7f;
        private float mGainMax = 6.2f;
        private float mGainMin = 5.2f;

        /**
         * Current balance that represents a traditional PLL signal offset value.
         * @return balance.
         */
        public float balance()
        {
            return mBalance;
        }

        /**
         * Updates the initial, min and max gain values from the samples per symbol
         * @param samplesPerSymbol for the incoming stream
         */
        public void configure(double samplesPerSymbol)
        {
            //Testing across a range of sample rates (5.2 to 6.25) indicates gain has linear correlation with samples per symbol
            mGainInitial = (float)(samplesPerSymbol * 1.185);
            mGain = mGainInitial;
            mGainMax = mGain + 0.1f;
            mGainMin = mGain - 0.1f;
        }

        /**
         * Reset after center frequency change or PPM change.
         */
        public void reset()
        {
            mSynchronized = false;
            mGain = mGainInitial;
        }

        /**
         * Calculates the mean of sync pattern detection for the samples starting at 9 symbol periods before and ending
         * at the tenth symbol.
         * @param offset for the sync detection which is just after the sample of the tenth symbol in the sync pattern
         * @return mean for the samples in this range
         */
        public float getBalance(double offset)
        {
            Mean mean = new Mean();
            int start = (int)Math.floor(offset - (9 * mSamplesPerSymbol));
            int end = (int)Math.ceil(offset);

            for(int i = start; i <= end; i++)
            {
                mean.increment(mBuffer[i]);
            }

            return (float)mean.getResult();
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
        public Correction optimize(double additionalOffset, float detectionScore, double bufferOffset)
        {
            //Offset points to the final symbol/sample in the buffer for the detected sync pattern.
            double offset = bufferOffset + additionalOffset;

            //Reject any sync detections where the sample:sample standard deviation exceeds the noise threshold.
            double standardDeviation = getStandardDeviation(offset);

            if(standardDeviation > mNoiseStandardDeviationThreshold)
            {
                return INVALID_SYNC_DETECTION;
            }

            float calculatedBalanceCorrection = -getBalance(offset);

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

            float scoreCenter = score(candidate, samplesPerSymbol, calculatedBalanceCorrection);

            candidate = offset - stepSize;
            float scoreLeft = score(candidate, samplesPerSymbol, calculatedBalanceCorrection);

            candidate = offset + stepSize;
            float scoreRight = score(candidate, samplesPerSymbol, calculatedBalanceCorrection);

            while(stepSize > stepSizeMin && Math.abs(adjustment) <= adjustmentMax)
            {
                if(scoreLeft > scoreRight && scoreLeft > scoreCenter)
                {
                    adjustment -= stepSize;
                    scoreRight = scoreCenter;
                    scoreCenter = scoreLeft;

                    candidate = offset + adjustment - stepSize;
                    scoreLeft = score(candidate, samplesPerSymbol, calculatedBalanceCorrection);
                }
                else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
                {
                    adjustment += stepSize;
                    scoreLeft = scoreCenter;
                    scoreCenter = scoreRight;

                    candidate = offset + adjustment + stepSize;
                    scoreRight = score(candidate, samplesPerSymbol, calculatedBalanceCorrection);
                }
                else
                {
                    stepSize *= 0.5f;

                    if(stepSize > stepSizeMin)
                    {
                        candidate = offset + adjustment - stepSize;
                        scoreLeft = score(candidate, samplesPerSymbol, calculatedBalanceCorrection);

                        candidate = offset + adjustment + stepSize;
                        scoreRight = score(candidate, samplesPerSymbol, calculatedBalanceCorrection);
                    }
                }
            }

            //If we didn't find an optimal correlation score above the threshold, return a false sync.
            if(scoreCenter < SYNC_THRESHOLD_OPTIMIZED)
            {
                return INVALID_SYNC_DETECTION;
            }

            return getCorrection(additionalOffset, adjustment, detectionScore, scoreCenter, bufferOffset,
                    calculatedBalanceCorrection);
        }

        /**
         * Equalize the symbol by applying balance correction and multiplying with gain value.
         * @param symbol to equalize.
         * @return equalized symbol
         */
        public float equalize(float symbol)
        {
            return symbol * mGain + mBalance;
        }

        /**
         * Equalizes both samples with optional phase wrap detection and returns the interpolated value at the mu offset.
         *
         * @param sample1 of the sample stream
         * @param sample2 of the sample stream
         * @param mu interpolation point between sample 1 and sample 2
         * @return equalized symbol
         */
        public float getEqualizedSymbol(float sample1, float sample2, double mu)
        {
            sample1 = equalize(sample1);
            sample2 = equalize(sample2);
            return LinearInterpolator.calculate(sample1, sample2, mu);
        }

        /**
         * Gets the interpolated sample value at the specified offset and equalizes it with the current equalizer settings.
         * @param offset to the sample
         * @return equalized value.
         */
        public float getEqualizedSymbol(double offset)
        {
            int integral = (int)Math.floor(offset);

            if(integral >= 0)
            {
                return getEqualizedSymbol(mBuffer[integral], mBuffer[integral + 1], offset - integral);
            }

            return 0;
        }

        /**
         * Calculates the sync correlation score at the specified offset and samples per symbol interval using the
         * current equalizer gain and supplied balance value.
         *
         * @param offset to the final symbol in the soft symbol buffer
         * @param samplesPerSymbol spacing to test for.
         * @param balance for the calculation
         * @return correlation score.
         */
        public float score(double offset, double samplesPerSymbol, float balance)
        {
            return score(offset, samplesPerSymbol, balance, mGain);
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

            double pointer = offset - (samplesPerSymbol * 9);
            int bufferPointer = (int)Math.floor(pointer);
            double fractional = pointer - bufferPointer;

            float score = 0;
            float sample1, sample2;
            float idealSymbol;

            for(int x = 0; x < 10; x++)
            {
                if(bufferPointer < maxPointer)
                {
                    sample1 = mBuffer[bufferPointer] * gain + balance;
                    sample2 = mBuffer[bufferPointer + 1] * gain + balance;
                    softSymbol = LinearInterpolator.calculate(sample1, sample2, fractional);
                }
                else
                {
                    softSymbol = 0.0f;
                }

                idealSymbol = SYNC_SYMBOLS[x];

                //Constrain the contribution of the soft symbol to +/- the ideal symbol value.
                if(idealSymbol > 0)
                {
                    softSymbol = Math.clamp(softSymbol, -idealSymbol, idealSymbol);
                }
                else
                {
                    softSymbol = Math.clamp(softSymbol, idealSymbol, -idealSymbol);
                }

                score += softSymbol * idealSymbol;
                pointer += samplesPerSymbol;
                bufferPointer = (int)Math.floor(pointer);
                fractional = pointer - bufferPointer;
            }

            return score;
        }

        /**
         * Applies equalizer correction settings.
         *
         * @param correction settings to apply
         */
        public void apply(Correction correction)
        {
            float remainingBalanceToCorrect = correction.getBalanceAdjustment() - mBalance;

            //Re-initialize the equalizer any time the balance correction value exceeds the threshold.
            if(mSynchronized && Math.abs(remainingBalanceToCorrect) > EQUALIZER_RECALIBRATE_THRESHOLD)
            {
                reset();
            }

            //Limit equalizer adjustments using a control loop at each sync detect after the initial equalizer setup or
            //apply as new settings on initial sync detect or any time an excess balance correction is supplied.
            if(mSynchronized)
            {
                mBalance += (remainingBalanceToCorrect * EQUALIZER_LOOP_GAIN);
                mGain += (correction.getGainAdjustment() * EQUALIZER_LOOP_GAIN);
            }
            else
            {
                mBalance += remainingBalanceToCorrect;
                mGain += correction.getGainAdjustment();
            }

            mBalance = Math.min(mBalance, EQUALIZER_MAXIMUM_BALANCE);
            mBalance = Math.max(mBalance, -EQUALIZER_MAXIMUM_BALANCE);
            mGain = Math.min(mGain, mGainMax);
            mGain = Math.max(mGain, mGainMin);
        }

        /**
         * Calculates a correction update for the equalizer balance and gain when the sync pattern is detected in the
         * sample buffer, using the supplied timing correction argument.  Resamples the sync symbols and compares each
         * soft symbol to the ideal symbol phase to develop average error measurements for balance and gain.  On initial
         * sync detection, the equalizer settings are applied to the samples in the buffer allowing the symbols to be
         * resampled during coarse sync acquisition.
         */
        public Correction getCorrection(double additionalOffset, double timingCorrection,
                                        float detectionScore, float optimizationScore, double offset, float balanceCorrection)
        {
            double resampleStart = offset + additionalOffset + timingCorrection - (mSamplesPerSymbol * 9);
            int resampleStartIntegral = (int)Math.floor(resampleStart);
            float symbol, gainAccumulator = 0, resampledSoftSymbol;
            Dibit resampledDibit;
            int bitErrorCount = 0;

            for(int x = 0; x < 10; x++)
            {
                if(resampleStartIntegral >= 0)
                {
                    symbol = SYNC_SYMBOLS[x];
                    resampledSoftSymbol = LinearInterpolator.calculate(mBuffer[resampleStartIntegral],
                            mBuffer[resampleStartIntegral + 1], resampleStart - resampleStartIntegral);
                    resampledSoftSymbol = resampledSoftSymbol * mGain + balanceCorrection;
                    resampledDibit = Dibit.fromSample(resampledSoftSymbol);
                    bitErrorCount += SYNC_DIBITS[x].getBitErrorFrom(resampledDibit);
                    gainAccumulator += Math.abs(symbol) - Math.abs(resampledSoftSymbol);
                }

                resampleStart += mSamplesPerSymbol;
                resampleStartIntegral = (int)Math.floor(resampleStart);
            }

            gainAccumulator /= 10;

            //Recalculate the optimization score using the adjusted equalizer settings
            if(!mSynchronized)
            {
                double optimizedFinalSymbol = offset + additionalOffset + timingCorrection;
                balanceCorrection = -getBalance(optimizedFinalSymbol);
                optimizationScore = score(optimizedFinalSymbol, mSamplesPerSymbol, balanceCorrection, mGain + gainAccumulator);
            }

            return new Correction(additionalOffset, timingCorrection, balanceCorrection, gainAccumulator, detectionScore,
                    optimizationScore, bitErrorCount);
        }
    }
}
