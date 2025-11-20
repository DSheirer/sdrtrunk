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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.filter.interpolator.LinearInterpolator;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitDelayLine;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.gui.viewer.sync.FMSyncResultsViewer;
import io.github.dsheirer.gui.viewer.sync.ISyncResultsListener;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.NXDNSyncDetector;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.NXDNSyncDetectorFactory;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetector;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetector;
import io.github.dsheirer.sample.Listener;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 * NXDN symbol processor provides symbol timing recovery and sync detection.
 */
public class NXDNSymbolProcessor
{
    private static final float SOFT_SYMBOL_QUADRANT_BOUNDARY = (float)(Math.PI / 2.0);
    private static final float SYNC_THRESHOLD_DETECTION = 35;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 40;
    private static final float SYNC_THRESHOLD_EQUALIZED = 40;
    private static final float TWO_PI = (float)(Math.PI * 2.0);
    private static final int BUFFER_WORKSPACE_LENGTH = 1024;
    private static final int DIBIT_LENGTH_NID = 33; //32 dibits (64 bits) +1 status
    private static final int DIBIT_LENGTH_SYNC = 10;
    private static final Correction INVALID_SYNC_DETECTION = new Correction(0d, Double.MAX_VALUE, 0f, 0f, 0f, 0f, 0);

    private ISyncResultsListener mSyncResultsViewer;
    private final DibitDelayLine mSymbolDelayLine = new DibitDelayLine(DIBIT_LENGTH_SYNC);
    private final DibitToByteBufferAssembler mDibitAssembler = new DibitToByteBufferAssembler(300);
    private final Equalizer mEqualizer = new Equalizer();
    private final FeedbackDecoder mFeedbackDecoder;
    private final NXDNMessageFramer mMessageFramer;
    private final NXDNStandardSoftSyncDetector mSyncDetector = NXDNSyncDetectorFactory.getStandardDetector();
    private final NXDNStandardSoftSyncDetector mSyncDetectorLagging = NXDNSyncDetectorFactory.getStandardDetector();
    private final NXDNControlSoftSyncDetector mControlSoftSyncDetector = NXDNSyncDetectorFactory.getControlDetector();
    private boolean mFineSync = false;
    private double mMaxFineSyncTimingAdjustment;
    private double mNoiseStandardDeviationThreshold;
    private double mSamplePoint;
    private double mSamplePointAdjustment;
    private double mSamplePointAdjustmentIncrement;
    private double mSamplePointAdjustmentMax;
    private double mSamplesPerSymbol;
    private float[] mBuffer;
    private float mLaggingSyncOffset;
    private float mOptimizeFineIncrement;
    private int mBufferPointer;
    private int mBufferReloadThreshold;
    private int mSymbolsSinceLastSync = 0;
    private int mDebugSymbolCounter = 0;
    private int mDebugSampleCounter = 0;
    private boolean mSynchronized = false;

//    private int mDebugStart = 1_096_400; //NXDN4800 traffic channel sample 1
    private int mDebugStart = 10_000_000;
//    private int mDebugStart = 0;

    /**
     * Constructs an instance
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public NXDNSymbolProcessor(NXDNMessageFramer messageFramer, FeedbackDecoder feedbackDecoder)
    {
        mMessageFramer = messageFramer;
        mFeedbackDecoder = feedbackDecoder;
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
        mEqualizer.reset();
    }

    /**
     * Primary input method for receiving a stream of demodulated samples to process into symbols.
     * @param samples to process
     */
    public void process(float[] samples)
    {
        //Create shadow copy of heap variables on stack
        boolean fineSync = mFineSync;
        double samplePoint = mSamplePoint;
        double samplesPerSymbol = mSamplesPerSymbol;
        int bufferPointer = mBufferPointer;
        int bufferReloadThreshold = mBufferReloadThreshold;
        int symbolsSinceLastSync = mSymbolsSinceLastSync;

        int samplesPointer = 0;
        float softSymbol, scorePrimary, scoreControl;
        Dibit delayedSymbol;
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
                accumulator += mEqualizer.mBalance;
                mEqualizer.mBalance -= (accumulator * .3f);
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

                mDebugSampleCounter++;

                if(samplePoint < 1)
                {
                    mDebugSymbolCounter++;

                    if(mDebugSymbolCounter % 20 == 0)
                    {
                        visualizeSymbols(bufferPointer, samplePoint);
                    }

                    symbolsSinceLastSync++;

                    //Clear synchronized state when we miss more than two sync periods (384 symbols per period).
                    if(mSynchronized && symbolsSinceLastSync > 800)
                    {
                        mEqualizer.reset();
                    }

                    softSymbol = mEqualizer.getEqualizedSymbol(mBuffer[bufferPointer], mBuffer[bufferPointer + 1], samplePoint);


                    //Broadcast the decoded soft symbol to an optionally registered listener (ie Symbol view in channel panel).
                    mFeedbackDecoder.broadcast(softSymbol);
                    Dibit symbol = toSymbol(softSymbol);
                    samplePoint += mEqualizer.getAdjustment(softSymbol, symbol, bufferPointer);

                    //Debug utility for viewing demodulation process
//                    getSyncResultsViewer().symbol(softSymbol);

                    mMessageFramer.process(symbol);

                    //We delay sending the symbol to the dibit assembler so that we have a chance to fully correct
                    //detected sync patterns in the delay buffer before they are sent downstream for recording.  This
                    //ensures that replay of the recorded demodulated bit stream has fully corrected sync patterns
                    //when possible.
                    delayedSymbol = mSymbolDelayLine.insert(symbol);
                    mDibitAssembler.receive(delayedSymbol);

                    scorePrimary = mSyncDetector.process(softSymbol);
                    scoreControl = mControlSoftSyncDetector.process(softSymbol);
                    correctionCandidate = INVALID_SYNC_DETECTION;

                    //Check for lagging sync pattern
                    float lag = (float)(bufferPointer + samplePoint - mLaggingSyncOffset);
                    int lagIntegral = (int)Math.floor(lag);
                    float softSymbolLag = mEqualizer.getEqualizedSymbol(mBuffer[lagIntegral], mBuffer[lagIntegral + 1], lag - lagIntegral);
                    float scoreLag = mSyncDetectorLagging.process(softSymbolLag);

                    boolean sync = (scorePrimary > SYNC_THRESHOLD_DETECTION) || (scoreLag > SYNC_THRESHOLD_DETECTION);

//                    if(scorePrimary > 30 || scoreLag > 30)
//                    {
//                        System.out.println(mDebugSymbolCounter +
//                                "\tPRIMARY:" + scorePrimary +
//                                "\tLAG:" + scoreLag +
//                                "\tCONTROL:" + scoreControl +
//                                (sync ? " <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< sync detected: " + symbolsSinceLastSync : ""));
//                    }


                    if(scorePrimary > 40)
                    {
                        correctionCandidate = mEqualizer.optimize(mSyncDetector, 0, scorePrimary,
                                bufferPointer + samplePoint);
                    }
                    else if(scoreLag > 40)
                    {
                        correctionCandidate = mEqualizer.optimize(mSyncDetectorLagging, -mLaggingSyncOffset,
                                scorePrimary, bufferPointer + samplePoint);
                    }

                    if(correctionCandidate.isValid())
                    {
                        mEqualizer.apply(correctionCandidate);
                        samplePoint += correctionCandidate.getTimingCorrection();

                        mMessageFramer.syncDetected();
                        symbolsSinceLastSync = 0;
                        mSynchronized = true;
                        visualizeSyncDetect(scorePrimary, true, "test", bufferPointer, samplePoint);
                    }

                    //Add another symbol's worth of samples to the counter for processing
                    samplePoint += samplesPerSymbol;
                }
            }
        }

        //Copy shadow variables back to the heap
        mBufferPointer = bufferPointer;
        mFineSync = fineSync;
        mSamplePoint = samplePoint;
        mSymbolsSinceLastSync = symbolsSinceLastSync;
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
        mMaxFineSyncTimingAdjustment = samplesPerSymbol * .2; // 1/5th of a symbol period
        mNoiseStandardDeviationThreshold = .2;
        mSamplePoint = samplesPerSymbol;
        mSamplePointAdjustmentMax = samplesPerSymbol / 2;
        mSamplePointAdjustmentIncrement = samplesPerSymbol / 100.0;
        mOptimizeFineIncrement = samplesPerSymbol / 200f;
        mLaggingSyncOffset = samplesPerSymbol / 2;
        int bufferLength = BUFFER_WORKSPACE_LENGTH + (int)(Math.ceil((DIBIT_LENGTH_SYNC + DIBIT_LENGTH_NID + 2) * samplesPerSymbol));
        mBuffer = new float[bufferLength];
        mBufferReloadThreshold = mBuffer.length - (int)Math.ceil(samplesPerSymbol * (DIBIT_LENGTH_NID + 1));
        mBufferPointer = mBufferReloadThreshold;
        mEqualizer.configure(samplesPerSymbol);
    }

    /**
     * Resamples the NID dibits using the supplied adjustment.  If the NID dibits pass error correction, notifies the
     * message framer with the extracted NAC and DUID values and overwrites the NID dibit delay buffer with the
     * static sync symbols and the resampled NID dibits so that the message framer and dibit assembler receive the
     * optimally sampled sync and NID dibit sequences.
     * @param correction (timing) to apply when resampling the NID.
     */
    private void validateNID(Correction correction, double bufferOffset)
    {
        //Current sample point is pointing to the not yet corrected final sync symbol.  Add in the candidate timing
        // correction and resample the NID starting at the next symbol.
        double pointer = bufferOffset + correction.getTimingCorrection() + mSamplesPerSymbol;
        double fractional;
        int integral;
        float softSymbol = 0;
        Dibit symbol;

        //Capture just the 63-bit BCH protected NID codeword including the 64th parity bit which we ignore.
        CorrectedBinaryMessage candidateNID = new CorrectedBinaryMessage(64);
        Dibit[] resampledNIDSymbols = new Dibit[33];

        for(int x = 0; x < DIBIT_LENGTH_NID; x++)
        {
            integral = (int) Math.floor(pointer);
            fractional = pointer - integral;
            softSymbol = mEqualizer.getEqualizedSymbol(mBuffer[integral], mBuffer[integral + 1], fractional, correction);
            symbol = toSymbol(softSymbol);
            resampledNIDSymbols[x] = symbol;

            //Skip the status symbol at dibit 11
            if(x != 11)
            {
                candidateNID.add(symbol.getBit1(), symbol.getBit2());
            }

            pointer += mSamplesPerSymbol;
        }

        //If error correction fails, return the original correction candidate
        if(candidateNID.getCorrectedBitCount() < 0)
        {
            correction.addCorrectedBitCount(candidateNID.getCorrectedBitCount());
            return;
        }

        correction.addCorrectedBitCount(candidateNID.getCorrectedBitCount());
    }

    /**
     * Decodes the sample value to determine the correct QPSK quadrant and maps the value to a Dibit symbol.
     * @param sample in radians.
     * @return symbol decision.
     */
    public static Dibit toSymbol(float sample)
    {
        if(sample > 0)
        {
            return sample > SOFT_SYMBOL_QUADRANT_BOUNDARY ? Dibit.D01_PLUS_3 : Dibit.D00_PLUS_1;
        }
        else
        {
            return sample < -SOFT_SYMBOL_QUADRANT_BOUNDARY ? Dibit.D11_MINUS_3 : Dibit.D10_MINUS_1;
        }
    }

    /**
     * Debug viewer UI for visualizing sync detections in the sample stream/waveform.
     */
    private ISyncResultsListener getSyncResultsViewer()
    {
        if(mSyncResultsViewer == null)
        {
            mSyncResultsViewer = new FMSyncResultsViewer();
        }

        return mSyncResultsViewer;
    }

    /**
     * Debug method to visualize the contents of the sync detection for the samples and symbols and symbol timing.
     * @param score from the sync detector
     * @param primary sync detector (true) or secondary (false)
     */
    public void visualizeSyncDetect(float score, boolean primary, String tag, int bufferPointer, double samplePoint)
    {
        if(true)
        {
            return;
        }
//        if(mDebugSampleCounter < mDebugStart)
//        {
//            return;
//        }

        int offset = 3;
        int length = (int)Math.ceil(mSamplesPerSymbol * 9) + (2 * offset);
        int end = bufferPointer + offset;
        int start = end - length;
        float[] symbols = new float[10];
        float[] samples = Arrays.copyOfRange(mBuffer, start, end);
        for(int i = 0; i < samples.length; i++)
        {
            samples[i] = mEqualizer.equalize(samples[i]);
        }

        double symbolPointer = bufferPointer + samplePoint - (mSamplesPerSymbol * 9);
        int symbolIntegral = (int)Math.floor(symbolPointer);
        double mu = symbolPointer - symbolIntegral;

        for(int x = 0; x < 10; x++)
        {
            symbols[x] = mEqualizer.getEqualizedSymbol(mBuffer[symbolIntegral], mBuffer[symbolIntegral + 1], mu);
            symbolPointer += mSamplesPerSymbol;
            symbolIntegral = (int)Math.floor(symbolPointer);
            mu = symbolPointer - symbolIntegral;
        }

        float[] syncIntervals = new float[10];
        int adjust = bufferPointer - length + offset;
        double pointer = bufferPointer + samplePoint - adjust;

        for(int x = 9; x >= 0; x--)
        {
            syncIntervals[x] = (float)pointer;
            pointer -= mSamplesPerSymbol;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        getSyncResultsViewer().receive(symbols, mSyncDetector.getSyncSymbols(), samples, syncIntervals, mEqualizer.mBalance, mEqualizer.mGain,
                "Score: " + score + (primary ? " PRIMARY " : " SECONDARY ") + (mFineSync ? "FINE " : "COARSE ") +
                        " EQ-B:" + mEqualizer.mBalance + " EQ-G:" + mEqualizer.mGain + " " + tag + " SAMPLES:" + mDebugSampleCounter, countDownLatch);

        try
        {
            countDownLatch.await();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Debug method to visualize the symbol decisions
     */
    public void visualizeSymbols(int bufferPointer, double samplePoint)
    {
        if(true)
        {
            return;
        }
//        if(mDebugSampleCounter < mDebugStart)
//        {
//            return;
//        }
        int offset = 3;
        int length = (int)Math.ceil(mSamplesPerSymbol * 9) + (2 * offset);
        int end = bufferPointer + offset;
        int start = end - length;
        float[] symbols = new float[10];
        float[] decisions = new float[10];
        float[] samples = Arrays.copyOfRange(mBuffer, start, end);
        for(int i = 0; i < samples.length; i++)
        {
            samples[i] = mEqualizer.equalize(samples[i]);
        }

        double symbolPointer = bufferPointer + samplePoint - (mSamplesPerSymbol * 9);
        int symbolIntegral = (int)Math.floor(symbolPointer);
        double mu = symbolPointer - symbolIntegral;

        for(int x = 0; x < 10; x++)
        {
            symbols[x] = mEqualizer.getEqualizedSymbol(mBuffer[symbolIntegral], mBuffer[symbolIntegral + 1], mu);
            decisions[x] = toSymbol(symbols[x]).getIdealPhase();
            symbolPointer += mSamplesPerSymbol;
            symbolIntegral = (int)Math.floor(symbolPointer);
            mu = symbolPointer - symbolIntegral;
        }

        float[] syncIntervals = new float[10];
        int adjust = bufferPointer - length + offset;
        double pointer = bufferPointer + samplePoint - adjust;

        for(int x = 9; x >= 0; x--)
        {
            syncIntervals[x] = (float)pointer;
            pointer -= mSamplesPerSymbol;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        getSyncResultsViewer().receive(symbols, decisions, samples, syncIntervals, mEqualizer.mBalance, mEqualizer.mGain,
                "Balance:" + mEqualizer.mBalance + " Gain:" + mEqualizer.mGain + " SAMPLES:" + mDebugSampleCounter, countDownLatch);

        try
        {
            countDownLatch.await();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Indicates if the samples in the sample buffer that contain a detected sync pattern have a standard deviation
     * that is lower than the expected sample to sample deviation for a modulated signal.  False detects against
     * noise tend to have a standard deviation that is 2x the expected value.
     * @param offset to the sample representing the final symbol in the detected sync pattern.
     * @return true if the standard deviation is more than expected.
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
        private final double mAdditionalOffset;
        private final float mBalanceAdjustment;
        private final float mGainAdjustment;
        private final float mDetectionScore;
        private final float mOptimizationScore;
        private final double mTimingAdjustment;
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

            if(hasValidTiming())
            {
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
            }
            else
            {
                sb.append("Correction - INVALID TIMING");
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
         * Additional offset adjustment based on the location of the triggering sync detector.  The primary sync
         * detector has no additional offset, but a lagging sync detector has a lagging offset.
         * @return addtional offset from lagging sync detector.
         */
        public double getAdditionalOffset()
        {
            return mAdditionalOffset;
        }

        /**
         * Original sync detection score.
         * @return score
         */
        public float getDetectionScore()
        {
            return mDetectionScore;
        }

        /**
         * Timing optimization score.
         * @return score
         */
        public float getOptimizationScore()
        {
            return mOptimizationScore;
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
         * Adds to the bit error count with the number of bit errors corrected in the NID by the BCH decoder
         * @param correctedBitCount for the corrected NID
         */
        public void addCorrectedBitCount(int correctedBitCount)
        {
            mCorrectedBitCount += correctedBitCount;
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

        /**
         * Indicates if the original sync detection score was high quality and is a higher score than the
         * optimization score.  This indicates a failed optimization that can be reevaluated for NID detection using
         * no timing adjustment.
         * @return true if high quality.
         */
        public boolean hasHighQualityDetectionScore()
        {
            return getDetectionScore() > SYNC_THRESHOLD_OPTIMIZED;
        }

        /**
         * Indicates if the equalization score is high quality.
         * @return true if high quality
         */
        public boolean hasHighQualityEqualizationScore()
        {
            return getOptimizationScore() >= getDetectionScore() && getOptimizationScore() > SYNC_THRESHOLD_EQUALIZED;
        }
    }

    /**
     * Equalizer
     *
     * Note: equalizer is initialized with a gain value of 1.219, as determined through significant testing against
     * both P25 and DMR C4FM modulated samples that employ pulse shaping filters.  Gain value is adjusted and
     * optimized with each sync detection.
     */
    class Equalizer
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
         * Updates the initial, min and max gain values from the samples per symbol
         * @param samplesPerSymbol for the incoming stream
         */
        public void configure(double samplesPerSymbol)
        {
            System.out.println("Equalizer is configuring for: " + samplesPerSymbol);
            float ratio = (float)samplesPerSymbol / 5.208f;
            float gain = 5.7f * ratio;
            mGainInitial = gain;
            mGain = gain;
            mGainMax = gain + 0.5f;
            mGainMin = gain - 0.5f;
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
        public Correction optimize(NXDNSyncDetector syncDetector, double additionalOffset, float detectionScore, double bufferOffset)
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
            boolean fineSync = mFineSync;
            double samplesPerSymbol = mSamplesPerSymbol;
            double stepSize = samplesPerSymbol / (fineSync ? 16.0 : 8.0); //Start at 1/8th for coarse & 1/16th for fine
            double stepSizeMin = mOptimizeFineIncrement;
            double adjustment = 0.0;

            //In coarse sync mode, constrain max adjustment to half a symbol period so that a lagging sync detect doesn't
            //preempt a primary sync detect prematurely and cause a significant timing correction.  So, we constrain the
            //max correction to a half symbol period for coarse sync mode.
            double adjustmentMax = fineSync ? samplesPerSymbol : (samplesPerSymbol / 2.0);
            double candidate = offset;

            float scoreCenter = score(syncDetector, candidate, samplesPerSymbol, calculatedBalanceCorrection);

            candidate = offset - stepSize;
            float scoreLeft = score(syncDetector, candidate, samplesPerSymbol, calculatedBalanceCorrection);

            candidate = offset + stepSize;
            float scoreRight = score(syncDetector, candidate, samplesPerSymbol, calculatedBalanceCorrection);

            while(stepSize > stepSizeMin && Math.abs(adjustment) <= adjustmentMax)
            {
                if(scoreLeft > scoreRight && scoreLeft > scoreCenter)
                {
                    adjustment -= stepSize;
                    scoreRight = scoreCenter;
                    scoreCenter = scoreLeft;

                    candidate = offset + adjustment - stepSize;
                    scoreLeft = score(syncDetector, candidate, samplesPerSymbol, calculatedBalanceCorrection);
                }
                else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
                {
                    adjustment += stepSize;
                    scoreLeft = scoreCenter;
                    scoreCenter = scoreRight;

                    candidate = offset + adjustment + stepSize;
                    scoreRight = score(syncDetector, candidate, samplesPerSymbol, calculatedBalanceCorrection);
                }
                else
                {
                    stepSize *= 0.5f;

                    if(stepSize > stepSizeMin)
                    {
                        candidate = offset + adjustment - stepSize;
                        scoreLeft = score(syncDetector, candidate, samplesPerSymbol, calculatedBalanceCorrection);

                        candidate = offset + adjustment + stepSize;
                        scoreRight = score(syncDetector, candidate, samplesPerSymbol, calculatedBalanceCorrection);
                    }
                }
            }

            //If we didn't find an optimal correlation score above the threshold, return a false sync.
            if(scoreCenter < SYNC_THRESHOLD_OPTIMIZED)
            {
                return INVALID_SYNC_DETECTION;
            }

            return getCorrection(syncDetector, additionalOffset, adjustment, detectionScore, scoreCenter, bufferOffset,
                    calculatedBalanceCorrection);
        }

        /**
         * Equalize the symbol by applying balance correction and multiplying with gain value.
         * @param symbol to equalize.
         * @return equalized symbol
         */
        public float equalize(float symbol)
        {
            return (symbol + mBalance) * mGain;
        }

        /**
         * Adjusts the symbol balance from the symbol error to address multi-path and fading between sync periods.
         */
        public double getAdjustment(float softSymbol, Dibit symbol, int bufferPointer)
        {
            double adjustment;

            if(softSymbol < symbol.getIdealPhase())
            {
                if(mBuffer[bufferPointer] > mBuffer[bufferPointer + 1])
                {
                    adjustment = -mSamplePointAdjustmentIncrement;
                }
                else
                {
                    adjustment = mSamplePointAdjustmentIncrement;
                }
            }
            else
            {
                if(mBuffer[bufferPointer] > mBuffer[bufferPointer + 1])
                {
                    adjustment = mSamplePointAdjustmentIncrement;
                }
                else
                {
                    adjustment = -mSamplePointAdjustmentIncrement;
                }
            }

            if(Math.abs(mSamplePointAdjustment + adjustment) <= mSamplePointAdjustmentMax)
            {
                mSamplePointAdjustment += adjustment;
                return adjustment;
            }

            return 0;
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
         * Equalizes both samples using the candidate correction values and returns the interpolated value at the
         * mu offset.
         * @param sample1 to equalize
         * @param sample2 to equalize
         * @param mu interpolation between samples 1 and 2
         * @param correction to apply when equalizing samples 1 and 2
         * @return interpolated value.
         */
        public float getEqualizedSymbol(float sample1, float sample2, double mu, Correction correction)
        {
            sample1 = (sample1 + mBalance + correction.getBalanceAdjustment()) * (mGain + correction.getGainAdjustment());
            sample2 = (sample2 + mBalance + correction.getBalanceAdjustment()) * (mGain + correction.getGainAdjustment());

            if(sample1 > Math.PI)
            {
                sample1 -= TWO_PI;
            }
            else if(sample1 < -Math.PI)
            {
                sample1 += TWO_PI;
            }

            if(sample2 > Math.PI)
            {
                sample2 -= TWO_PI;
            }
            else if(sample2 < -Math.PI)
            {
                sample2 += TWO_PI;
            }

            return LinearInterpolator.calculate(sample1, sample2, mu);
        }

        /**
         * Calculates the sync correlation score at the specified offset and samples per symbol interval using the
         * current equalizer balance and gain values.
         *
         * @param offset to the final symbol in the soft symbol buffer
         * @param samplesPerSymbol spacing to test for.
         * @return correlation score.
         */
        public float score(NXDNSyncDetector syncDetector, double offset, double samplesPerSymbol, float balance)
        {
            return score(syncDetector, offset, samplesPerSymbol, balance, mGain);
        }

        /**
         * Calculates the sync correlation score at the specified offset and samples per symbol interval.
         * @param offset to the final symbol in the soft symbol buffer
         * @param samplesPerSymbol spacing to test for.
         * @return correlation score.
         */
        public float score(NXDNSyncDetector syncDetector, double offset, double samplesPerSymbol, float balance, float gain)
        {
            int maxPointer = mBuffer.length - 1;
            float softSymbol;

            double pointer = offset - (samplesPerSymbol * (syncDetector.getSyncPatternDibitLength() - 1));
            int bufferPointer = (int)Math.floor(pointer);
            double fractional = pointer - bufferPointer;

            float score = 0;

            for(int x = 0; x < syncDetector.getSyncPatternDibitLength(); x++)
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

                score += softSymbol * syncDetector.getSyncSymbols()[x];
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
//            System.out.println("Applying [" + mDebugSampleCounter + "] Correction: " + correction);
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
        public Correction getCorrection(NXDNSyncDetector syncDetector, double additionalOffset, double timingCorrection,
                                        float detectionScore, float optimizationScore, double offset, float balanceCorrection)
        {
            double resampleStart = offset + additionalOffset + timingCorrection - (mSamplesPerSymbol * (syncDetector.getSyncPatternDibitLength() - 1));
            int resampleStartIntegral = (int)Math.floor(resampleStart);
            float symbol, gainAccumulator = 0, resampledSoftSymbol;
            Dibit resampledDibit;
            int bitErrorCount = 0;

            for(int x = 0; x < syncDetector.getSyncPatternDibitLength(); x++)
            {
                if(resampleStartIntegral >= 0)
                {
                    symbol = syncDetector.getSyncSymbols()[x];
                    resampledSoftSymbol = LinearInterpolator.calculate(mBuffer[resampleStartIntegral],
                            mBuffer[resampleStartIntegral + 1], resampleStart - resampleStartIntegral);
                    resampledSoftSymbol = (resampledSoftSymbol + balanceCorrection) * mGain;
                    resampledDibit = toSymbol(resampledSoftSymbol);
                    bitErrorCount += syncDetector.getSyncDibits()[x].getBitErrorFrom(resampledDibit);
                    gainAccumulator += Math.abs(symbol) - Math.abs(resampledSoftSymbol);
                }

                resampleStart += mSamplesPerSymbol;
                resampleStartIntegral = (int)Math.floor(resampleStart);
            }

            gainAccumulator /= syncDetector.getSyncPatternDibitLength();

            //Recalculate the optimization score using the adjusted equalizer settings
            if(!mSynchronized)
            {
                balanceCorrection = -getBalance(offset + timingCorrection);
                optimizationScore = score(syncDetector, offset + timingCorrection, mSamplesPerSymbol,
                        balanceCorrection, mGain + gainAccumulator);
            }

            return new Correction(additionalOffset, timingCorrection, balanceCorrection, gainAccumulator, detectionScore,
                    optimizationScore, bitErrorCount);
        }
    }
}
