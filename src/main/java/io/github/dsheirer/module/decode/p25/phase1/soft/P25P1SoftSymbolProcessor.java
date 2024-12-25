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

package io.github.dsheirer.module.decode.p25.phase1.soft;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.dsp.filter.interpolator.PhaseAwareLinearInterpolator;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitDelayBuffer;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.edac.bch.BCH_63_16_23_P25;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetector;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetectorFactory;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SyncDetector;
import io.github.dsheirer.sample.Listener;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P25P1SoftSymbolProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(P25P1SoftSymbolProcessor.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(LOGGER);

    private static final int BUFFER_PROTECTED_REGION_DIBITS = 26; //Sync (24) plus 2
    private static final int BUFFER_WORKSPACE_LENGTH_DIBITS = 25; //This can be adjusted for efficiency
    private static final int BUFFER_LENGTH_DIBITS = BUFFER_PROTECTED_REGION_DIBITS + BUFFER_WORKSPACE_LENGTH_DIBITS;
    private static final int MAX_SYMBOLS_FOR_FINE_SYNC = 890; //Length of longest messages: LDU1 and LDU2
    private static final int MIN_SYMBOLS_FOR_TIMING_ADJUST = 72;
    private static final int NID_DIBIT_LENGTH = 33; //32 dibits (64 bits) plus 1 status symbol dibit
    private static final float MAX_POSITIVE_SOFT_SYMBOL = Dibit.D01_PLUS_3.getIdealPhase();
    private static final float MAX_NEGATIVE_SOFT_SYMBOL = Dibit.D11_MINUS_3.getIdealPhase();
    private static final float SYMBOL_QUADRANT_BOUNDARY = (float)(Math.PI / 2.0);
    private static final float SYNC_DETECTION_THRESHOLD = 65;
    private static final float[] SYNC_PATTERN_SYMBOLS = P25P1SyncDetector.syncPatternToSymbols();
    private static final Dibit[] SYNC_PATTERN_DIBITS = P25P1SyncDetector.syncPatternToDibits();
    private P25P1SoftSyncDetector mSyncDetector = P25P1SoftSyncDetectorFactory.getDetector();
    private P25P1SoftSyncDetector mSyncDetectorLag1 = P25P1SoftSyncDetectorFactory.getDetector();
    private P25P1SoftSyncDetector mSyncDetectorLag2 = P25P1SoftSyncDetectorFactory.getDetector();
    private DibitToByteBufferAssembler mDibitAssembler = new DibitToByteBufferAssembler(300);
    private P25P1SoftMessageFramer mMessageFramer;
    private NIDDibitDelayBuffer mDibitDelayBuffer = new NIDDibitDelayBuffer();
    private boolean mSyncLock = false;
    private float mLaggingSyncOffset1;
    private float mLaggingSyncOffset2;
    private float mObservedSamplesPerSymbol;
    private float mSamplePoint;
    private float[] mBuffer;
    private int mBufferLoadPointer;
    private int mBufferPointer;
    private int mBufferWorkspaceLength;
    private int mPreviousMessageSymbolLength;
    private int mSymbolsSinceLastSync = NID_DIBIT_LENGTH + 1; //Set higher than NID length (72) to prevent false initial NID calculation.
    private final BCH_63_16_23_P25 mBCHDecoder = new BCH_63_16_23_P25();
    public static final IntField NAC_FIELD = IntField.length12(0);
    public static final IntField DUID_FIELD = IntField.length4(12);
    private NACTracker mNACTracker = new NACTracker();
    private int mDebugTotalSymbols;
    private long mDebugSampleCount = 0;

    /**
     * Constructs an instance
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public P25P1SoftSymbolProcessor(P25P1SoftMessageFramer messageFramer)
    {
        mMessageFramer = messageFramer;
    }

    /**
     * Primary input method for receiving a stream of demodulated samples to process into symbols.
     * @param samples to process
     */
    public void receive(float[] samples)
    {
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

            while(mBufferPointer < (mBufferLoadPointer - 7)) //Interpolator needs 1 and optimizer needs 6 pad spaces
            {
                mBufferPointer++;
                mSamplePoint--;

                mDebugSampleCount++;

                if(mSamplePoint < 1)
                {
                    mSymbolsSinceLastSync++;
                    mDebugTotalSymbols++;

                    if(mSymbolsSinceLastSync > MAX_SYMBOLS_FOR_FINE_SYNC)
                    {
                        mSyncLock = false;
//                        mNACTracker.reset();
                    }

                    float softSymbol = PhaseAwareLinearInterpolator.calculate(mBuffer[mBufferPointer],
                            mBuffer[mBufferPointer + 1], mSamplePoint);

                    Dibit symbol = toSymbol(softSymbol);

                    mMessageFramer.receive(symbol);

                    //Store the symbol in the delay line for sync detection and NID processing so that we can correct
                    // the sync bits before sending to the dibit assembler for bitstream recording.
                    Dibit ejected = mDibitDelayBuffer.getAndPut(symbol);
                    mDibitAssembler.receive(ejected);

                    //Check for sync pattern
                    float lag1 = mBufferPointer + mSamplePoint - mLaggingSyncOffset1;
                    float lag2 = mBufferPointer + mSamplePoint - mLaggingSyncOffset2;
                    int lagIntegral1 = (int)Math.floor(lag1);
                    int lagIntegral2 = (int)Math.floor(lag2);
                    float softSymbolLag1 = PhaseAwareLinearInterpolator.calculate(mBuffer[lagIntegral1],
                            mBuffer[lagIntegral1 + 1], lag1 - lagIntegral1);
                    float softSymbolLag2 = PhaseAwareLinearInterpolator.calculate(mBuffer[lagIntegral2],
                            mBuffer[lagIntegral2 + 1], lag2 - lagIntegral2);
                    float scoreLag1 = mSyncDetectorLag1.process(softSymbolLag1);
                    float scoreLag2 = mSyncDetectorLag2.process(softSymbolLag2);
                    float scorePrimary = mSyncDetector.process(softSymbol);

                    //If we're sync locked, attempt to evaluate sync only by the primary sync detector
                    String tag = " - SCORE PRIMARY:" + scorePrimary + " LAG1:" + scoreLag1 + " LAG2:" + scoreLag2 + " SYMBOLS:" + mSymbolsSinceLastSync;
                    if(mSyncLock && scorePrimary > SYNC_DETECTION_THRESHOLD && optimize(0, "SYNC *PRIMARY*" + tag))
                    {
                        mPreviousMessageSymbolLength = mSymbolsSinceLastSync;
//                        System.out.println("SYNC PRIMARY - Score: " + scorePrimary + " Symbols Previous: " + mPreviousMessageSymbolLength + " Samples: " + mDebugSampleCount);
                        mSymbolsSinceLastSync = 0;
                    }
                    else if(mSymbolsSinceLastSync > 1 && scoreLag1 > scorePrimary && scoreLag1 > scoreLag2 &&
                            scoreLag1 > SYNC_DETECTION_THRESHOLD && optimize(-mLaggingSyncOffset1, "SYNC LAG 1" + tag))
                    {
                        mPreviousMessageSymbolLength = mSymbolsSinceLastSync;
//                        System.out.println("SYNC LAG 1 - Score: " + scoreLag1 + " Symbols Previous: " + mPreviousMessageSymbolLength + " Samples: " + mDebugSampleCount);
                        mSymbolsSinceLastSync = 0;
                    }
                    else if(mSymbolsSinceLastSync > 1 && scoreLag2 > scorePrimary &&
                            scoreLag2 > SYNC_DETECTION_THRESHOLD && optimize(-mLaggingSyncOffset2, "SYNC LAG 2" + tag))
                    {
                        mPreviousMessageSymbolLength = mSymbolsSinceLastSync;
//                        System.out.println("SYNC LAG 2 - Score: " + scoreLag2 + " Symbols Previous: " + mPreviousMessageSymbolLength + " Samples: " + mDebugSampleCount);
                        mSymbolsSinceLastSync = 0;
                    }
                    else if(scorePrimary > SYNC_DETECTION_THRESHOLD && optimize(0.0f, "SYNC PRIMARY" + tag))
                    {
                        mPreviousMessageSymbolLength = mSymbolsSinceLastSync;
//                        System.out.println("SYNC PRIMARY - Score: " + scorePrimary + " Symbols Previous: " + mPreviousMessageSymbolLength + " Samples: " + mDebugSampleCount);
                        mSymbolsSinceLastSync = 0;
                    }

                    //Process the NID at 33 symbols to verify the sync detection was correct and set/clear sync lock state
                    if(mSymbolsSinceLastSync == NID_DIBIT_LENGTH)
                    {
                        processNID();
                    }

                    //Add another symbol's worth of samples to the counter
                    mSamplePoint += mObservedSamplesPerSymbol;
                }
            }
        }
    }

    /**
     * Adjusts the symbol timing and symbol spacing to identify the best achievable sync correlation score and apply
     * those adjustments when the correlation score exceeds a positive sync detection threshold.
     * @param additionalOffset from current mBufferPointer and mSamplePoint.  This can be zero offset for the primary
     * sync detector or an offset for the lagging sync detectors.
     * @return true if there is a positive sync detection.
     */
    private boolean optimize(float additionalOffset, String prefix)
    {
        boolean debugLogging = false;
//        debugLogging = mDebugSampleCount < 22854518;

        //Offset is the start of the first sample of the first symbol of the sync pattern calculated from the current
        //buffer pointer and sample point which should be the final sample of the final symbol of the detected sync.
        float offset = (mBufferPointer + mSamplePoint) + additionalOffset - (mObservedSamplesPerSymbol * 23);

        //Find the optimal symbol timing
        float stepSize = mSyncLock ? (mObservedSamplesPerSymbol / 40.0f) : (mObservedSamplesPerSymbol / 10.0f);
        float stepSizeMin = 0.03f;
        float adjustment = 0.0f;
        float adjustmentMax = mObservedSamplesPerSymbol / 2.0f;
        float candidate = offset;

        int candidateIntegral = (int)Math.floor(candidate);
        float candidateFractional = candidate - candidateIntegral;
        float scoreCenter = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol);

        candidate = offset - stepSize;
        candidateIntegral = (int)Math.floor(candidate);
        candidateFractional = candidate - candidateIntegral;
        float scoreLeft = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol);

        candidate = offset + stepSize;
        candidateIntegral = (int)Math.floor(candidate);
        candidateFractional = candidate - candidateIntegral;
        float scoreRight = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol);

        StringBuilder debugSB = new StringBuilder();
        debugSB.append("-------------------------------------------------------------------------").append("\n");
        debugSB.append(prefix).append("\n");

        while(stepSize > stepSizeMin && Math.abs(adjustment) <= adjustmentMax)
        {
            if(scoreLeft > scoreRight && scoreLeft > scoreCenter)
            {
                debugSB.append("Optimize - LEFT: " + scoreLeft +
                        " Center: " + scoreCenter +
                        " Right: " + scoreRight +
                        " StepSize: " + stepSize +
                        " Adjustment: " + adjustment +
                        " Symbols Since Last Sync: " + mSymbolsSinceLastSync).append("\n");
                adjustment -= stepSize;
                scoreRight = scoreCenter;
                scoreCenter = scoreLeft;

                candidate = offset + adjustment - stepSize;
                candidateIntegral = (int)Math.floor(candidate);
                candidateFractional = candidate - candidateIntegral;
                scoreLeft = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol);
            }
            else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
            {
                debugSB.append("Optimize - Left: " + scoreLeft +
                        " Center: " + scoreCenter +
                        " RIGHT: " + scoreRight +
                        " StepSize: " + stepSize +
                        " Adjustment: " + adjustment +
                        " Symbols Since Last Sync: " + mSymbolsSinceLastSync).append("\n");
                adjustment += stepSize;
                scoreLeft = scoreCenter;
                scoreCenter = scoreRight;

                candidate = offset + adjustment + stepSize;
                candidateIntegral = (int)Math.floor(candidate);
                candidateFractional = candidate - candidateIntegral;
                scoreRight = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol);
            }
            else
            {
                debugSB.append("Optimize - Left: " + scoreLeft +
                        " CENTER: " + scoreCenter +
                        " Right: " + scoreRight +
                        " StepSize: " + stepSize +
                        " Adjustment: " + adjustment +
                        " Symbols Since Last Sync: " + mSymbolsSinceLastSync).append("\n");
                stepSize *= 0.5f;

                if(stepSize > stepSizeMin)
                {
                    candidate = offset + adjustment - stepSize;
                    candidateIntegral = (int)Math.floor(candidate);
                    candidateFractional = candidate - candidateIntegral;
                    scoreLeft = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol);

                    candidate = offset + adjustment + stepSize;
                    candidateIntegral = (int)Math.floor(candidate);
                    candidateFractional = candidate - candidateIntegral;
                    scoreRight = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol);
                }
            }

//            if(mSyncLock && Math.abs(adjustment) > 1.0)
//            {
//                //Abort trying to find anything better ... it's wasted effort
//                debugSB.append(">>> Aborting optimization - excessive adjustment\n");
//                stepSize = 0;
//            }
        }

        //If we didn't find an optimal correlation score above the 95 threshold, return a false sync.
        if(scoreCenter < 95)
        {
            return false;
        }

        if(additionalOffset != 0.0)
        {
            adjustment += additionalOffset;
        }

        float temp = adjustment;

        if(mSyncLock && Math.abs(adjustment) > 0.5)
        {
            debugSB.append("*** SYNC LOCK ADJUSTMENT [" + adjustment + "] WAS CONSTRAINED TO +/- 0.5").append(" Additional: " + additionalOffset + "\n");
            adjustment = Math.min(adjustment, 0.5f);
            adjustment = Math.max(adjustment, -0.5f);
        }

        mSamplePoint += adjustment;

        while(mSamplePoint < 0)
        {
            mSamplePoint++;
            mBufferPointer--;
        }

        while(mSamplePoint > 1)
        {
            mSamplePoint--;
            mBufferPointer++;
        }

        debugSB.append("Adjustment: " + adjustment + " Lock:" + mSyncLock + "\n");

        //Adjust the observed samples per symbol using the timing error measured across one or two bursts when we're in
        //fine sync mode and the timing error is not excessive.
        if(mSyncLock && Math.abs(adjustment) < 0.5 && mSymbolsSinceLastSync >= MIN_SYMBOLS_FOR_TIMING_ADJUST &&
                mSymbolsSinceLastSync <= MAX_SYMBOLS_FOR_FINE_SYNC)
        {
            debugSB.append("Observed SPS Before: " + mObservedSamplesPerSymbol).append("\n");
            mObservedSamplesPerSymbol += (float)((double)adjustment / (double)mSymbolsSinceLastSync * 0.2);
            debugSB.append("Observed SPS  After: " + mObservedSamplesPerSymbol).append("\n");
        }

        //Overwrite the most recent 24 dibits with the detected sync so there's no sync bit errors
        mDibitDelayBuffer.overwriteSync();

        if(debugLogging)
        {
            System.out.println(debugSB);
        }

        return true;
    }

    /**
     * Calculates the sync correlation score for sync pattern at the specified offsets and symbol timing.
     * @param bufferPointer to the start of the samples in the soft symbol buffer
     * @param fractional position to interpolate within the 8 samples starting at the buffer pointer.
     * @param samplesPerSymbol spacing to test for.
     * @return correlation score.
     */
    public float score(int bufferPointer, float fractional, float samplesPerSymbol)
    {
        float score = 0;
        int integral;

        for(int x = 0; x < 24; x++)
        {
            float softSymbol = PhaseAwareLinearInterpolator.calculate(mBuffer[bufferPointer], mBuffer[bufferPointer + 1], fractional);
            softSymbol = Math.min(softSymbol, MAX_POSITIVE_SOFT_SYMBOL);
            softSymbol = Math.max(softSymbol, MAX_NEGATIVE_SOFT_SYMBOL);
            score += softSymbol * SYNC_PATTERN_SYMBOLS[x];
            fractional += samplesPerSymbol;
            integral = (int)Math.floor(fractional);
            bufferPointer += integral;
            fractional -= integral;
        }

        return score;
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
        mObservedSamplesPerSymbol = samplesPerSymbol;
        mSamplePoint = samplesPerSymbol;
        mLaggingSyncOffset1 = samplesPerSymbol / 3;
        mLaggingSyncOffset2 = mLaggingSyncOffset1 * 2;
        mBufferWorkspaceLength = (int)Math.ceil(BUFFER_WORKSPACE_LENGTH_DIBITS * samplesPerSymbol);
        int bufferLength = (int)(Math.ceil(BUFFER_LENGTH_DIBITS * samplesPerSymbol));
        mBuffer = new float[bufferLength];
        mBufferLoadPointer = (int)Math.ceil(BUFFER_PROTECTED_REGION_DIBITS * samplesPerSymbol);
        mBufferPointer = mBufferLoadPointer;
    }

    /**
     * Checks/tests the contents of the data unit buffer for a valid NID after a sync pattern is detected
     */
    private void processNID()
    {
        int trackedNAC = mNACTracker.getTrackedNAC();

        CorrectedBinaryMessage nidMessage = mDibitDelayBuffer.getNIDMessage(0);
        mBCHDecoder.decode(nidMessage, trackedNAC);

        //A negative corrected bit count indicates failed to correct the message.
        boolean validNID = nidMessage.getCorrectedBitCount() >= 0;
        int nac = nidMessage.getInt(NAC_FIELD);

        if(validNID)
        {
            //The BCH decoder can over-correct the NID and produce an invalid NAC.  Compare it against the tracked NID to
            //flag it as invalid NID when this happens.  The NAC tracker will give us a value of 0 until it has enough
            //observations of a valid NID value.
            if((trackedNAC > 0) && trackedNAC != nac)
            {
                validNID = false;
            }
        }

        Dibit extra = null;

        //Sometimes we stuff an extra symbol ... test correcting the NID by shifting to an earlier offset
        if(!validNID)
        {
            CorrectedBinaryMessage nidMessageMinus1 = mDibitDelayBuffer.getNIDMessage(-1);
            mBCHDecoder.decode(nidMessageMinus1, trackedNAC);

            if(nidMessageMinus1.getCorrectedBitCount() >= 0)
            {
                int nacMinus1 = nidMessageMinus1.getInt(NAC_FIELD);

                //The BCH decoder can over-correct the NID and produce an invalid NAC.  Compare it against the tracked NID to
                //flag it as invalid NID when this happens.  The NAC tracker will give us a value of 0 until it has enough
                //observations of a valid NID value.
                if(trackedNAC == 0 || (trackedNAC > 0 && trackedNAC == nacMinus1))
                {
                    extra = mDibitDelayBuffer.getLast();
                    mDibitDelayBuffer.adjustPointer(-1);
                    nidMessage = nidMessageMinus1;
                    nac = nacMinus1;
                    validNID = true;
                }
            }
        }

        //Sometimes we drop an extra symbol ... test correcting the NID by shifting to a later offset
        if(!validNID)
        {
            CorrectedBinaryMessage nidMessagePlus1 = mDibitDelayBuffer.getNIDMessage(1);
            mBCHDecoder.decode(nidMessagePlus1, trackedNAC);

            if(nidMessagePlus1.getCorrectedBitCount() >= 0)
            {
                int nacPlus1 = nidMessagePlus1.getInt(NAC_FIELD);

                //The BCH decoder can over-correct the NID and produce an invalid NAC.  Compare it against the tracked NID to
                //flag it as invalid NID when this happens.  The NAC tracker will give us a value of 0 until it has enough
                //observations of a valid NID value.
                if(trackedNAC == 0 || (trackedNAC > 0 && trackedNAC == nacPlus1))
                {
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ EATING AN EXTRA BIT @@@@@@@@@@@@@@@@@@@@@@@@@@@2  -----------------------------------------");
                    mDibitDelayBuffer.adjustPointer(1);
                    nidMessage = nidMessagePlus1;
                    nac = nacPlus1;
                    validNID = true;
                }
            }
        }

        P25P1DataUnitID duid = P25P1DataUnitID.fromValue(nidMessage.getInt(DUID_FIELD));

        log(nidMessage, duid, nac, validNID);

        if(validNID)
        {
            //Update the NAC tracker with the observed, correctly decoded NAC value.
            mNACTracker.track(nac);
            mSyncLock = true;
            mMessageFramer.syncDetected(nac, duid);
        }
        else
        {
            mMessageFramer.syncDetectedInvalidNID(trackedNAC);
        }

        if(extra != null)
        {
            System.out.println("----------- ----- ------ ------- Stuffing an extra dibit: " + extra);
            mMessageFramer.receive(extra);
        }
    }

    private static void log(CorrectedBinaryMessage a, P25P1DataUnitID duid, int nac, boolean corrected)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\tNID: ").append(a);
        sb.append(" NAC:").append(nac);
        sb.append(" DUID:").append(duid);
        sb.append(" CORRECTED:").append(corrected);

        if(!corrected)
        {
            sb.append("\t\t** ERROR **");
        }

        System.out.println(sb);
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
            return sample > SYMBOL_QUADRANT_BOUNDARY ? Dibit.D01_PLUS_3 : Dibit.D00_PLUS_1;
        }
        else
        {
            return sample < -SYMBOL_QUADRANT_BOUNDARY ? Dibit.D11_MINUS_3 : Dibit.D10_MINUS_1;
        }
    }

    /**
     * Circular buffer for storing and accessing dibits and extract the NID.
     */
    public class NIDDibitDelayBuffer extends DibitDelayBuffer
    {
        /**
         * Constructs a dibit delay buffer
         */
        public NIDDibitDelayBuffer()
        {
            super(NID_DIBIT_LENGTH);
        }

        /**
         * Adjusts the pointer to correct a dibit stuff/delete as detected by the NID.
         * @param offset
         */
        public void adjustPointer(int offset)
        {
            mPointer += offset;

            if(mPointer < 0)
            {
                mPointer += NID_DIBIT_LENGTH;
            }
            else if(mPointer >= NID_DIBIT_LENGTH)
            {
                mPointer -= NID_DIBIT_LENGTH;
            }
        }

        /**
         * Replaces the most recent 24 dibits with the P25 Phase 1 sync pattern to erase any bit errors that might be
         * present in the received dibit stream with the known sync pattern.  This should only be invoked once we have
         * a strong correlation of the sync pattern.
         */
        public void overwriteSync()
        {
            int pointer = mPointer + (BUFFER_LENGTH_DIBITS - 24);

            if(pointer >= mBuffer.length)
            {
                pointer = 0;
            }

            for(Dibit dibit: SYNC_PATTERN_DIBITS)
            {
                mBuffer[pointer++] = dibit;

                if(pointer >= mBuffer.length)
                {
                    pointer = 0;
                }
            }
        }

        /**
         * Extracts the NID codeword from the dibit delay buffer.  The delay buffer is sized to 33 dibits which is 32
         * dibits for the NID and an extra dibit for the status symbol.  We extract the NID from the buffer once there
         * has been 33 dibits since the sync pattern detection.  The buffer pointer should be pointing to the first
         * dibit of the NID, since this is a circular buffer.
         *
         * @return message bits containing the NID.
         */
        public CorrectedBinaryMessage getNIDMessage(int offset)
        {
            //Capture just the 63-bit BCH protected NID codeword including the 64th parity bit which we ignore.
            CorrectedBinaryMessage nid = new CorrectedBinaryMessage(64);

            int bufferPointer = mPointer + offset;

            if(bufferPointer >= mBuffer.length)
            {
                bufferPointer -= mBuffer.length;
            }
            else if(bufferPointer < 0)
            {
                bufferPointer += mBuffer.length;
            }

            Dibit dibit = null;

            for(int x = 0; x < 33; x++)
            {
                if(x == 11)
                {
                    bufferPointer++; //Skip the status symbol that's in the middle of the NID
                }
                else
                {
                    dibit = mBuffer[bufferPointer++];
                    nid.add(dibit.getBit1(), dibit.getBit2());
                }

                if(bufferPointer >= mBuffer.length)
                {
                    bufferPointer = 0;
                }
            }

            return nid;
        }
    }
}
