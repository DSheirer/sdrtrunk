/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.dsp.filter.interpolator.LinearInterpolator;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitDelayLine;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.edac.bch.BCH_63_16_23_P25;
import io.github.dsheirer.gui.viewer.sync.SyncResultsViewer;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetector;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetectorFactory;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SyncDetector;
import io.github.dsheirer.sample.Listener;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 * P25 Phase 1 decoder for C4FM/4FSK signals.  Processes PI/4 differential QPSK demodulated samples with soft sync
 * detection.  Optimizes symbol timing and PLL at each sync detection.
 *
 * Note: this demodulator applies a gain of ~1.2x to the decoded phase of each symbol to correct for an imbalance in the
 * decoded constellation and adjusts this gain at each sync detection based on how the decoded sync symbols align with
 * the ideal symbols in the sync pattern.  This compressed symbol constellation/gain imbalance is observed in both P25
 * and DMR 4FSK modulated signals when using PI/4 DQPSK demodulation and is possibly introduced by the pulse shaping
 * transmit filter.  This imbalance is not observed in LSM signals which likely use direct phase manipulation.
 */
public class P25P1DemodulatorC4FM
{
    private static final float EQUALIZER_LOOP_GAIN = 0.15f;
    private static final float EQUALIZER_MAXIMUM_PLL = (float)(Math.PI / 3.0); //+/- 800 Hz
    private static final float EQUALIZER_MAXIMUM_GAIN = 1.25f;
    private static final float EQUALIZER_RECALIBRATE_THRESHOLD = (float)(Math.PI / 8.0);
    private static final float SOFT_SYMBOL_QUADRANT_BOUNDARY = (float)(Math.PI / 2.0);
    private static final float SYNC_THRESHOLD_DETECTION = 80;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 80;
    private static final float SYNC_THRESHOLD_EQUALIZED = 110;
    private static final float TWO_PI = (float)(Math.PI * 2.0);
    private static final float[] SYNC_PATTERN_SYMBOLS = P25P1SyncDetector.syncPatternToSymbols();
    private static final int BUFFER_WORKSPACE_LENGTH = 1024;
    private static final int DIBIT_LENGTH_NID = 33; //32 dibits (64 bits) +1 status
    private static final int DIBIT_LENGTH_SYNC = 24;
    private static final int SYMBOL_RATE = 4800;
    private static final Correction INVALID_SYNC_DETECTION = new Correction(0d, Double.MAX_VALUE, 0f, 0f, 0f, 0f, 0);
    private static final Dibit[] SYNC_PATTERN_DIBITS = P25P1SyncDetector.syncPatternToDibits();
    private static final IntField NAC_FIELD = IntField.length12(0);
    private static final IntField DUID_FIELD = IntField.length4(12);

    private SyncResultsViewer mSyncResultsViewer;
    private final BCH_63_16_23_P25 mBCHDecoder = new BCH_63_16_23_P25();
    private final DibitDelayLine mSymbolDelayLine = new DibitDelayLine(DIBIT_LENGTH_SYNC);
    private final DibitToByteBufferAssembler mDibitAssembler = new DibitToByteBufferAssembler(300);
    private final Equalizer mEqualizer = new Equalizer();
    private final FeedbackDecoder mFeedbackDecoder;
    private final NACTracker mNACTracker = new NACTracker();
    private final P25P1MessageFramer mMessageFramer;
    private final P25P1SoftSyncDetector mSyncDetector = P25P1SoftSyncDetectorFactory.getDetector();
    private final P25P1SoftSyncDetector mSyncDetectorLagging = P25P1SoftSyncDetectorFactory.getDetector();
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

    /**
     * Constructs an instance
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public P25P1DemodulatorC4FM(P25P1MessageFramer messageFramer, FeedbackDecoder feedbackDecoder)
    {
        mMessageFramer = messageFramer;
        mFeedbackDecoder = feedbackDecoder;
    }

    /**
     * Resets the PLL
     */
    public void resetPLL()
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
        float softSymbol, scorePrimary;
        Dibit delayedSymbol;
        Correction correctionCandidate;

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

                //Unwrap phases
                for(int x = mBuffer.length - copyLength; x < mBuffer.length; x++)
                {
                    if(mBuffer[x - 1] > 1.5f && mBuffer[x] < -1.5f)
                    {
                        mBuffer[x] += TWO_PI;
                    }
                    else if(mBuffer[x - 1] < -1.5f && mBuffer[x] > 1.5f)
                    {
                        mBuffer[x] -= TWO_PI;
                    }
                }
            }

            while(bufferPointer < bufferReloadThreshold)
            {
                bufferPointer++;
                samplePoint--;

                if(samplePoint < 1)
                {
                    symbolsSinceLastSync++;
                    softSymbol = mEqualizer.getEqualizedSymbol(mBuffer[bufferPointer], mBuffer[bufferPointer + 1], samplePoint);

                    //Debug utility for viewing demodulation process
//                    getSyncResultsViewer().symbol(softSymbol);

                    //Broadcast the decoded soft symbol to an optionally registered listener (ie Symbol view in channel panel).
                    mFeedbackDecoder.broadcast(softSymbol);
                    Dibit symbol = toSymbol(softSymbol);
                    samplePoint += mEqualizer.getAdjustment(softSymbol, symbol, bufferPointer);

                    mMessageFramer.process(symbol);

                    //We delay sending the symbol to the dibit assembler so that we have a chance to fully correct
                    //detected sync patterns in the delay buffer before they are sent downstream for recording.  This
                    //ensures that replay of the recorded demodulated bit stream has fully corrected sync patterns
                    //when possible.
                    delayedSymbol = mSymbolDelayLine.insert(symbol);
                    mDibitAssembler.receive(delayedSymbol);

                    scorePrimary = mSyncDetector.process(softSymbol);
                    correctionCandidate = INVALID_SYNC_DETECTION;

                    if(fineSync)
                    {
                        if(symbolsSinceLastSync > 1 && scorePrimary > SYNC_THRESHOLD_DETECTION)
                        {
                            correctionCandidate = mEqualizer.optimize(0, scorePrimary,
                                    bufferPointer + samplePoint);
                        }
                    }
                    else
                    {
                        //Check for lagging sync pattern
                        float lag = (float)(bufferPointer + samplePoint - mLaggingSyncOffset);
                        int lagIntegral = (int)Math.floor(lag);
                        float softSymbolLag = mEqualizer.getEqualizedSymbol(mBuffer[lagIntegral], mBuffer[lagIntegral + 1], lag - lagIntegral);
                        float scoreLag = mSyncDetectorLagging.process(softSymbolLag);

                        if(symbolsSinceLastSync > 1)
                        {
                            if(scorePrimary > SYNC_THRESHOLD_DETECTION && scorePrimary > scoreLag)
                            {
                                correctionCandidate = mEqualizer.optimize(0.0f, scorePrimary,
                                        bufferPointer + samplePoint);
                            }

                            if(correctionCandidate == INVALID_SYNC_DETECTION && scoreLag > SYNC_THRESHOLD_DETECTION)
                            {
                                correctionCandidate = mEqualizer.optimize(-mLaggingSyncOffset, scoreLag,
                                        bufferPointer + samplePoint);
                            }
                        }
                    }

                    if(correctionCandidate.hasValidTiming())
                    {
                        validateNID(correctionCandidate, bufferPointer + samplePoint);

                        //Detect when the correction candidate fails to validate the NID and we have a high-quality
                        //original sync detection score.  Attempt to validate the NID with no timing correction, using
                        //the original sync detection timing offset.
                        if(!correctionCandidate.isValid() && correctionCandidate.hasHighQualityDetectionScore())
                        {
                            //Remove the candidate timing adjustment from the correction and use just the detection
                            //score as both detection and optimization. This limits the correction to just the equalizer
                            //update/adjustment.
                            correctionCandidate = mEqualizer.getCorrection(correctionCandidate.getAdditionalOffset(),
                            0.0, correctionCandidate.getDetectionScore(),
                                    correctionCandidate.getDetectionScore(), bufferPointer + samplePoint);
                            validateNID(correctionCandidate, bufferPointer + samplePoint);
                        }

                        //Apply candidate correction values to timing and equalizer when we have a valid SYNC/NID
                        if(correctionCandidate.isValid())
                        {
                            if(fineSync)
                            {
                                double adjustment = correctionCandidate.getTimingCorrection();

                                if(adjustment < 1)
                                {
                                    adjustment = Math.max(adjustment, -mMaxFineSyncTimingAdjustment);
                                    adjustment = Math.min(adjustment, mMaxFineSyncTimingAdjustment);
                                }

                                samplePoint += adjustment;
                            }
                            else
                            {
                                samplePoint += correctionCandidate.getTimingCorrection();
                            }

                            //Reset the sample point intra-sync adjustment value.
                            mSamplePointAdjustment = 0;

                            mEqualizer.apply(correctionCandidate);

                            //broadcast equalizer balance as the current frequency offset
                            mFeedbackDecoder.processPLLError(mEqualizer.mPll, SYMBOL_RATE);
                        }

                        if(correctionCandidate.isValid() || correctionCandidate.hasHighQualityDetectionScore() ||
                                (correctionCandidate.hasHighQualityOptimizationScore() && mMessageFramer.isComplete()))
                        {
                            mMessageFramer.syncDetected();
                            fineSync = true;
                            symbolsSinceLastSync = 0;

                            //Overwrite the sync symbols in the symbol delay line to eliminate any decoding errors
                            for(Dibit syncSymbol: SYNC_PATTERN_DIBITS)
                            {
                                mSymbolDelayLine.insert(syncSymbol);
                            }
                        }
                    }

                    if(fineSync ^ mMessageFramer.isAssembling())
                    {
                        fineSync = mMessageFramer.isAssembling();
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
        mNoiseStandardDeviationThreshold = Dibit.D01_PLUS_3.getIdealPhase() * 2 / mSamplesPerSymbol * 1.2; //120% of optimal
        mSamplePoint = samplesPerSymbol;
        mSamplePointAdjustmentMax = samplesPerSymbol / 2;
        mSamplePointAdjustmentIncrement = samplesPerSymbol / 100.0;
        mOptimizeFineIncrement = samplesPerSymbol / 200f;
        mLaggingSyncOffset = samplesPerSymbol / 2;
        int bufferLength = BUFFER_WORKSPACE_LENGTH + (int)(Math.ceil((DIBIT_LENGTH_SYNC + DIBIT_LENGTH_NID + 2) * samplesPerSymbol));
        mBuffer = new float[bufferLength];
        mBufferReloadThreshold = mBuffer.length - (int)Math.ceil(samplesPerSymbol * (DIBIT_LENGTH_NID + 1));
        mBufferPointer = mBufferReloadThreshold;
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

        int trackedNAC = mNACTracker.getTrackedNAC();
        mBCHDecoder.decode(candidateNID, trackedNAC);

        //If error correction fails, return the original correction candidate
        if(candidateNID.getCorrectedBitCount() < 0)
        {
            correction.addCorrectedBitCount(candidateNID.getCorrectedBitCount());
            return;
        }

        int nac = candidateNID.getInt(NAC_FIELD);
        P25P1DataUnitID duid = P25P1DataUnitID.fromValue(candidateNID.getInt(DUID_FIELD));

        //The BCH decoder can over-correct the NID and produce an invalid NAC.  Compare it against the tracked NID to
        //flag it as invalid NID when this happens.  The NAC tracker will give us a value of 0 until it has enough
        //observations of a valid NID value.
        mNACTracker.track(nac);

        if(trackedNAC > 0 && trackedNAC != nac)
        {
            return;
        }

        correction.setNID(nac, duid);
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
    private SyncResultsViewer getSyncResultsViewer()
    {
        if(mSyncResultsViewer == null)
        {
            mSyncResultsViewer = new SyncResultsViewer();
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
        int offset = 3;
        int length = (int)Math.ceil(mSamplesPerSymbol * 23) + (2 * offset);
        int end = bufferPointer + offset;
        int start = end - length;
        float[] symbols = new float[24];
        float[] samples = Arrays.copyOfRange(mBuffer, start, end);
        for(int i = 0; i < samples.length; i++)
        {
            samples[i] = mEqualizer.equalize(samples[i]);
        }

        double symbolPointer = bufferPointer + samplePoint - (mSamplesPerSymbol * 23);
        int symbolIntegral = (int)Math.floor(symbolPointer);
        double mu = symbolPointer - symbolIntegral;

        for(int x = 0; x < 24; x++)
        {
            symbols[x] = mEqualizer.getEqualizedSymbol(mBuffer[symbolIntegral], mBuffer[symbolIntegral + 1], mu);
            symbolPointer += mSamplesPerSymbol;
            symbolIntegral = (int)Math.floor(symbolPointer);
            mu = symbolPointer - symbolIntegral;
        }

        float[] syncIntervals = new float[24];
        int adjust = bufferPointer - length + offset;
        double pointer = bufferPointer + samplePoint - adjust;

        for(int x = 23; x >= 0; x--)
        {
            syncIntervals[x] = (float)pointer;
            pointer -= mSamplesPerSymbol;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        getSyncResultsViewer().receive(symbols, SYNC_PATTERN_SYMBOLS, samples, syncIntervals, mEqualizer.mPll, mEqualizer.mGain,
                "Score: " + score + (primary ? " PRIMARY " : " SECONDARY ") + (mFineSync ? "FINE " : "COARSE ") +
                        " EQ-B:" + mEqualizer.mPll + " EQ-G:" + mEqualizer.mGain + " " + tag, countDownLatch);

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
        int offset = 3;
        int length = (int)Math.ceil(mSamplesPerSymbol * 23) + (2 * offset);
        int end = bufferPointer + offset;
        int start = end - length;
        float[] symbols = new float[24];
        float[] decisions = new float[24];
        float[] samples = Arrays.copyOfRange(mBuffer, start, end);
        for(int i = 0; i < samples.length; i++)
        {
            samples[i] = mEqualizer.equalize(samples[i]);
        }

        double symbolPointer = bufferPointer + samplePoint - (mSamplesPerSymbol * 23);
        int symbolIntegral = (int)Math.floor(symbolPointer);
        double mu = symbolPointer - symbolIntegral;

        for(int x = 0; x < 24; x++)
        {
            symbols[x] = mEqualizer.getEqualizedSymbol(mBuffer[symbolIntegral], mBuffer[symbolIntegral + 1], mu);
            decisions[x] = toSymbol(symbols[x]).getIdealPhase();
            symbolPointer += mSamplesPerSymbol;
            symbolIntegral = (int)Math.floor(symbolPointer);
            mu = symbolPointer - symbolIntegral;
        }

        float[] syncIntervals = new float[24];
        int adjust = bufferPointer - length + offset;
        double pointer = bufferPointer + samplePoint - adjust;

        for(int x = 23; x >= 0; x--)
        {
            syncIntervals[x] = (float)pointer;
            pointer -= mSamplesPerSymbol;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        getSyncResultsViewer().receive(symbols, decisions, samples, syncIntervals, mEqualizer.mPll, mEqualizer.mGain,
                "PLL:" + mEqualizer.mPll + " Gain:" + mEqualizer.mGain, countDownLatch);

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

        return standardDeviation.getResult() > mNoiseStandardDeviationThreshold;
    }

    /**
     * Symbol timing and equalizer correction candidate settings
     */
    public static class Correction
    {
        private final double mAdditionalOffset;
        private final float mPllAdjustment;
        private final float mGainAdjustment;
        private final float mDetectionScore;
        private final float mOptimizationScore;
        private final double mTimingAdjustment;
        private int mNAC;
        private P25P1DataUnitID mDataUnitID = P25P1DataUnitID.PLACE_HOLDER;
        private int mCorrectedBitCount = 0;

        /**
         * Constructs an instance
         * @param timingAdjustment correction for symbol sampling
         * @param pllAdjustment correction for equalizer
         * @param gainAdjustment correction for equalizer
         * @param bitErrorCount in the detected sync pattern
         */
        public Correction(double additionalOffset, double timingAdjustment, float pllAdjustment, float gainAdjustment,
                          float detectionScore, float optimizationScore, int bitErrorCount)
        {
            mAdditionalOffset = additionalOffset;
            mTimingAdjustment = timingAdjustment;
            mPllAdjustment = pllAdjustment;
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
                    sb.append("\tCORRECTION - NAC [").append(mNAC);
                    sb.append("] DUID [").append(mDataUnitID.toString());
                    sb.append("] Bit Errors [").append(mCorrectedBitCount).append("]");
                }
                else
                {
                    sb.append("CORRECTION - INVALID NID");
                }

                sb.append(" At Offset [").append(mAdditionalOffset);
                sb.append("] Adjustments-Timing [").append(mTimingAdjustment);
                sb.append("] PLL [").append(mPllAdjustment);
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
         * Equalizer PLL correction value.
         * @return correction
         */
        public float getPllAdjustment()
        {
            return mPllAdjustment;
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
         * Sets the detected data unit ID.
         * @param dataUnitID that was detected.
         */
        public void setNID(int nac, P25P1DataUnitID dataUnitID)
        {
            mNAC = nac;
            mDataUnitID = dataUnitID;
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
         * Detected data unit ID
         * @return data unit ID
         */
        public P25P1DataUnitID getDataUnitID()
        {
            return mDataUnitID;
        }

        public int getNAC()
        {
            return mNAC;
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
            return mDataUnitID != P25P1DataUnitID.PLACE_HOLDER && getOptimizationScore() > SYNC_THRESHOLD_EQUALIZED;
        }

        /**
         * Indicates if the original sync detection score was high quality and is a higher score thant the
         * optimization score.  This indicates a failed optimization that can be reevaluated for NID detection using
         * no timing adjustment.
         * @return true if the detection score is high quality.
         */
        public boolean hasHighQualityDetectionScore()
        {
            return getDetectionScore() > SYNC_THRESHOLD_EQUALIZED;
        }

        /**
         * Indicates if the optimization score is high quality, higher than the equalization threshold.
         * @return true if high quality
         */
        public boolean hasHighQualityOptimizationScore()
        {
            return getOptimizationScore() >= getDetectionScore() && getOptimizationScore() > SYNC_THRESHOLD_EQUALIZED;
        }
    }

    /**
     * Equalizer and Phase-Locked Loop (PLL)
     *
     * Note: equalizer is initialized with a gain value of 1.219, as determined through significant testing against
     * both P25 and DMR C4FM modulated samples that employ pulse shaping filters.  Gain value is adjusted and
     * optimized with each sync detection.
     */
    class Equalizer
    {
        private boolean mInitialized = false;
        private float mPll = 0.0f;
        private float mGain = 1.219f;

        /**
         * Reset after center frequency change or PPM change.
         */
        public void reset()
        {
            mInitialized = false;
            mPll = 0.0f;
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
            if(isNoisy(offset))
            {
                return INVALID_SYNC_DETECTION;
            }

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
         * Equalize the symbol by applying PLL correction and multiplying with gain value.
         * @param symbol to equalize.
         * @return equalized symbol
         */
        public float equalize(float symbol)
        {
            return (symbol + mPll) * mGain;
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
            sample1 = (sample1 + mPll + correction.getPllAdjustment()) * (mGain + correction.getGainAdjustment());
            sample2 = (sample2 + mPll + correction.getPllAdjustment()) * (mGain + correction.getGainAdjustment());

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
        public float score(double offset, double samplesPerSymbol)
        {
            return score(offset, samplesPerSymbol, mPll, mGain);
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

                score += softSymbol * SYNC_PATTERN_SYMBOLS[x];
                pointer += samplesPerSymbol;
                bufferPointer = (int)Math.floor(pointer);
                fractional = pointer - bufferPointer;
            }

            return score;
        }

        /**
         * Indicates if this equalizer is initialized, meaning it has processed at least 1x sync detection to establish
         * initial gain and balance values.
         *
         * @return true if initialized.
         */
        public boolean isInitialized()
        {
            return mInitialized;
        }

        /**
         * Applies equalizer correction settings.
         *
         * @param correction settings to apply
         */
        public void apply(Correction correction)
        {
            //Re-initialize the equalizer any time the balance correction value exceeds the threshold.
            if(mInitialized && Math.abs(correction.getPllAdjustment()) > EQUALIZER_RECALIBRATE_THRESHOLD)
            {
                mInitialized = false;
            }

            //Limit equalizer adjustments using a control loop at each sync detect after the initial equalizer setup or
            //apply as new settings on initial sync detect or any time an excess balance correction is supplied.
            if(mInitialized)
            {
                mPll += (correction.getPllAdjustment() * EQUALIZER_LOOP_GAIN);
                mGain += (correction.getGainAdjustment() * EQUALIZER_LOOP_GAIN);
            }
            else
            {
                mPll += correction.getPllAdjustment();
                mGain += correction.getGainAdjustment();
            }

            //Constrain to +/- PI/2 (+/- 1200 Hertz of offset)
            mPll = Math.min(mPll, EQUALIZER_MAXIMUM_PLL);
            mPll = Math.max(mPll, -EQUALIZER_MAXIMUM_PLL);

            //Constrain gain between 1.0f and 1.25f
            mGain = Math.min(mGain, EQUALIZER_MAXIMUM_GAIN);
            mGain = Math.max(mGain, 1.0f);
            mInitialized = true;
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
            float symbol = SYNC_PATTERN_SYMBOLS[23];
            float resampledSoftSymbol = LinearInterpolator.calculate(mBuffer[resampleStartIntegral],
                    mBuffer[resampleStartIntegral + 1], resampleStart - resampleStartIntegral);
            resampledSoftSymbol = (resampledSoftSymbol + mPll) * mGain;

            float balancePlus3Symbols = 0;
            float balanceMinus3Symbols = resampledSoftSymbol - symbol;
            float gainAccumulator = Math.abs(symbol) - Math.abs(resampledSoftSymbol);
            Dibit resampledDibit = toSymbol(resampledSoftSymbol);
            int bitErrorCount = SYNC_PATTERN_DIBITS[23].getBitErrorFrom(resampledDibit);

            resampleStart -= (23 * mSamplesPerSymbol);
            resampleStartIntegral = (int)Math.floor(resampleStart);

            for(int x = 0; x < 23; x++)
            {
                if(resampleStartIntegral >= 0)
                {
                    symbol = SYNC_PATTERN_SYMBOLS[x];
                    resampledSoftSymbol = LinearInterpolator.calculate(mBuffer[resampleStartIntegral],
                            mBuffer[resampleStartIntegral + 1], resampleStart - resampleStartIntegral);
                    resampledSoftSymbol = (resampledSoftSymbol + mPll) * mGain;
                    resampledDibit = toSymbol(resampledSoftSymbol);
                    bitErrorCount += SYNC_PATTERN_DIBITS[x].getBitErrorFrom(resampledDibit);

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
            if(!isInitialized())
            {
                optimizationScore = score(offset + timingCorrection, mSamplesPerSymbol,
                        mPll + balanceCorrection, mGain + gainAccumulator);
            }

            return new Correction(additionalOffset, timingCorrection, balanceCorrection, gainAccumulator, detectionScore,
                    optimizationScore, bitErrorCount);
        }
    }
}
