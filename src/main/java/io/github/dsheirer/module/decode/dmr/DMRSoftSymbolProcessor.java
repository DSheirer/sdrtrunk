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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.dsp.filter.interpolator.PhaseAwareLinearInterpolator;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.module.decode.dmr.sync.DMRSoftSyncDetector;
import io.github.dsheirer.module.decode.dmr.sync.DMRSoftSyncDetectorFactory;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncDetectMode;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncModeMonitor;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import io.github.dsheirer.sample.Listener;
import java.nio.ByteBuffer;

/**
 * DMR soft symbol processor.  Processes a stream of differentially decoded samples to align to the symbol timing and
 * symbol period and extract symbols from the sample stream.
 *
 * Uses three soft sync correlation detectors to achieve initial sync lock with a primary sync detector and two lagging
 * sync detectors sampling at 1/3 and 2/3 of the symbol period to ensure the sync is detected while only calculating at
 * each symbol period interval.  Once a sync is detected, sync lock is declared and the lagging sync detectors are
 * disabled. Sync lock disables whenever the quantity of processed symbols exceeds one DMR burst length and the
 * lagging detectors re-enable.
 *
 * The sync mode monitor processes the sync detections to identify the dominant sync mode for the channel and then
 * disables processing of the two unused sync modes (BASE STATION, MOBILE STATION, or DIRECT MODE) for the remainder
 * of the channel.
 *
 * This processor uses a symbol delay buffer (dibit delay line) to retain a partial DMR burst from CACH up through the
 * sync field of the burst.  This allows the optimize method to resample the burst symbols once the sync is detected and
 * optimal symbol timing and symbol period are calculated.  This ensures the best symbol decisions are then passed to
 * the message framer along with the sync detection.
 *
 * Buffer Structure & Size = 129 Dibits: [2 pad][12 CACH][54 Message Prefix][24 Sync][24 workspace][1 pad]
 */
public class DMRSoftSymbolProcessor
{
    private static final int BUFFER_PROTECTED_REGION_DIBITS = 92;
    private static final int BUFFER_WORKSPACE_LENGTH_DIBITS = 25; //This can be adjusted for efficiency
    private static final int BUFFER_LENGTH_DIBITS = BUFFER_PROTECTED_REGION_DIBITS + BUFFER_WORKSPACE_LENGTH_DIBITS;
    private static final float MAX_POSITIVE_SOFT_SYMBOL = Dibit.D01_PLUS_3.getIdealPhase();
    private static final float MAX_NEGATIVE_SOFT_SYMBOL = Dibit.D11_MINUS_3.getIdealPhase();
    private static final float SAMPLES_PER_SYMBOL_ALLOWABLE_DEVIATION = 0.005f; //.5%
    private static final float SYMBOL_QUADRANT_BOUNDARY = (float)(Math.PI / 2.0);
    private static final float SYNC_DETECTION_THRESHOLD = 60;
    private DMRSyncModeMonitor mSyncModeMonitor = new DMRSyncModeMonitor();
    private DMRSoftSyncDetector mSyncDetector = DMRSoftSyncDetectorFactory.getDetector();
    private DMRSoftSyncDetector mSyncDetectorLag1 = DMRSoftSyncDetectorFactory.getDetector();
    private DMRSoftSyncDetector mSyncDetectorLag2 = DMRSoftSyncDetectorFactory.getDetector();
    private DibitToByteBufferAssembler mDibitAssembler = new DibitToByteBufferAssembler(300);
    //Dibit delay line sizing: CACH(12) + MESSAGE_PREFIX(54) + SYNC(24)
    private DibitDelayLine mDibitDelayLine = new DibitDelayLine(90);
    private DMRMessageFramer mMessageFramer;
    private boolean mSyncLock = false;
    private float mLaggingSyncOffset1;
    private float mLaggingSyncOffset2;
    private float mObservedSamplesPerSymbol;
    private float mSamplesPerSymbol;
    private float mMaxSamplesPerSymbol;
    private float mMinSamplesPerSymbol;
    private float mSamplePoint;
    private float[] mBuffer;
    private int mBufferLoadPointer;
    private int mBufferPointer;
    private int mBufferWorkspaceLength;
    private int mBufferInterpolatorReservedRegion;
    private int mSymbolsSinceLastSync = 0;

    /**
     * Constructs an instance
     * @param messageFramer to receive demodulated symbols/dibits and sync detect notifications
     */
    public DMRSoftSymbolProcessor(DMRMessageFramer messageFramer)
    {
        if(messageFramer == null)
        {
            throw new IllegalArgumentException("Message framer cannot be null");
        }

        mMessageFramer = messageFramer;
        mSyncModeMonitor.add(mSyncDetector);
        mSyncModeMonitor.add(mSyncDetectorLag1);
        mSyncModeMonitor.add(mSyncDetectorLag2);
    }

    /**
     * Sets base station sync detection mode.  This disables detection for MOBILE and DIRECT sync patterns.  This can
     * be used for traffic channels that are known to be part of a base station.
     * @param baseStationMode true for base mode only, false otherwise.
     */
    public void setBaseStationMode(boolean baseStationMode)
    {
        if(baseStationMode)
        {
            mSyncModeMonitor.setMode(DMRSyncDetectMode.BASE_ONLY);
        }
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
     * Sets the reference timestamp from the incoming sample stream to use for assigning a timestamp to framed messages.
     * @param timestamp to set for reference
     */
    public void setTimestamp(long timestamp)
    {
        mMessageFramer.setTimestamp(timestamp);
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

            while(mBufferPointer < (mBufferLoadPointer - mBufferInterpolatorReservedRegion))
            {
                mBufferPointer++;
                mSamplePoint--;

                if(mSamplePoint < 1)
                {
                    mSymbolsSinceLastSync++;

                    if(mSymbolsSinceLastSync > 144)
                    {
                        mSyncLock = false;
                    }

                    float softSymbol = PhaseAwareLinearInterpolator.calculate(mBuffer[mBufferPointer],
                            mBuffer[mBufferPointer + 1], mSamplePoint);

                    Dibit symbol = toSymbol(softSymbol);

                    //Store the symbol in the delay line and broadcast the delayed ejected symbol to the message framer
                    //and the bitstream assembler.
                    Dibit ejected = mDibitDelayLine.insert(symbol);
                    mMessageFramer.receive(ejected);
                    mDibitAssembler.receive(ejected);

                    float scorePrimary = mSyncDetector.process(softSymbol);

                    if(mSyncLock && scorePrimary > SYNC_DETECTION_THRESHOLD &&
                            optimize(mSyncDetector.getDetectedPattern(), 0.0f))
                    {
                        mSymbolsSinceLastSync = 0;
                        mSyncModeMonitor.detected(mSyncDetector.getDetectedPattern());
                        mMessageFramer.syncDetected(mSyncDetector.getDetectedPattern());
                    }
                    else
                    {
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

                        if(mSymbolsSinceLastSync > 1 && scoreLag1 > scorePrimary && scoreLag1 > scoreLag2 &&
                                scoreLag1 > SYNC_DETECTION_THRESHOLD &&
                                optimize(mSyncDetectorLag1.getDetectedPattern(), -mLaggingSyncOffset1))
                        {
                            mSymbolsSinceLastSync = 0;
                            mSyncModeMonitor.detected(mSyncDetectorLag1.getDetectedPattern());
                            mMessageFramer.syncDetected(mSyncDetectorLag1.getDetectedPattern());
                        }
                        else if(mSymbolsSinceLastSync > 1 && scoreLag2 > scorePrimary &&
                                scoreLag2 > SYNC_DETECTION_THRESHOLD &&
                                optimize(mSyncDetectorLag2.getDetectedPattern(), -mLaggingSyncOffset2))
                        {
                            mSymbolsSinceLastSync = 0;
                            mSyncModeMonitor.detected(mSyncDetectorLag2.getDetectedPattern());
                            mMessageFramer.syncDetected(mSyncDetectorLag2.getDetectedPattern());
                        }
                        else if(scorePrimary > SYNC_DETECTION_THRESHOLD &&
                                optimize(mSyncDetector.getDetectedPattern(), 0.0f))
                        {
                            mSymbolsSinceLastSync = 0;
                            mSyncLock = true;
                            mSyncModeMonitor.detected(mSyncDetector.getDetectedPattern());
                            mMessageFramer.syncDetected(mSyncDetector.getDetectedPattern());
                        }
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
     * @param pattern that was detected
     * @param additionalOffset from current mBufferPointer and mSamplePoint.  This can be zero offset for the primary
     * sync detector or an offset for the lagging sync detectors.
     * @return true if there is a positive sync detection.
     */
    private boolean optimize(DMRSyncPattern pattern, float additionalOffset)
    {
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
        float scoreCenter = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol, pattern);

        candidate = offset - stepSize;
        candidateIntegral = (int)Math.floor(candidate);
        candidateFractional = candidate - candidateIntegral;
        float scoreLeft = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol, pattern);

        candidate = offset + stepSize;
        candidateIntegral = (int)Math.floor(candidate);
        candidateFractional = candidate - candidateIntegral;
        float scoreRight = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol, pattern);

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
                scoreLeft = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol, pattern);
            }
            else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
            {
                adjustment += stepSize;
                scoreLeft = scoreCenter;
                scoreCenter = scoreRight;

                candidate = offset + adjustment + stepSize;
                candidateIntegral = (int)Math.floor(candidate);
                candidateFractional = candidate - candidateIntegral;
                scoreRight = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol, pattern);
            }
            else
            {
                stepSize *= 0.5f;

                if(stepSize > stepSizeMin)
                {
                    candidate = offset + adjustment - stepSize;
                    candidateIntegral = (int)Math.floor(candidate);
                    candidateFractional = candidate - candidateIntegral;
                    scoreLeft = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol, pattern);

                    candidate = offset + adjustment + stepSize;
                    candidateIntegral = (int)Math.floor(candidate);
                    candidateFractional = candidate - candidateIntegral;
                    scoreRight = score(candidateIntegral, candidateFractional, mObservedSamplesPerSymbol, pattern);
                }
            }
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

        //Adjust the observed samples per symbol using the timing error measured across one or two bursts when we're in
        //fine sync mode and the timing error is not excessive.
        if(mSyncLock && Math.abs(adjustment) < 0.5 && mSymbolsSinceLastSync > 143 && mSymbolsSinceLastSync < 289)
        {
            mObservedSamplesPerSymbol += (float)((double)adjustment / (double)mSymbolsSinceLastSync * 0.2);

            if(mObservedSamplesPerSymbol > mMaxSamplesPerSymbol)
            {
                mObservedSamplesPerSymbol = mMaxSamplesPerSymbol;
            }
            else if(mObservedSamplesPerSymbol < mMinSamplesPerSymbol)
            {
                mObservedSamplesPerSymbol = mMinSamplesPerSymbol;
            }
        }

        //If the timing error adjustment is high enough, resample the symbols.  Otherwise, overwrite the captured sync
        //pattern in the delay buffer with the detected sync pattern to eliminate any sync bit errors.
        if(Math.abs(adjustment) > 0.05)
        {
            float resampleStart = mBufferPointer + mSamplePoint;
            resampleStart -= (89 * mObservedSamplesPerSymbol); //Start at 89 (+ 1 current = 90) symbols
            int resampleStartIntegral;

            for(int x = 0; x < 66; x++)
            {
                resampleStartIntegral = (int)Math.floor(resampleStart);

                if(resampleStartIntegral >= 0)
                {
                    float resampledSoftSymbol = PhaseAwareLinearInterpolator.calculate(mBuffer[resampleStartIntegral],
                            mBuffer[resampleStartIntegral + 1], resampleStart - resampleStartIntegral);
                    mDibitDelayLine.insert(toSymbol(resampledSoftSymbol));
                }
                else
                {
                    //This shouldn't happen since there's 2x dibits of padding on the front side, but just in case.
                    mDibitDelayLine.insert(Dibit.D01_PLUS_3);
                }

                resampleStart += mObservedSamplesPerSymbol;
            }

            //We don't need to resample the sync region ... just use the actual sync dibit values.
            for(Dibit dibit: pattern.toDibits())
            {
                mDibitDelayLine.insert(dibit);
            }
        }
        else
        {
            //Overwrite the most recent 24 dibits with the detected sync so there's no sync bit errors
            mDibitDelayLine.update(pattern.toDibits());
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
        float score = 0;
        float[] symbols = pattern.toSymbols();
        int integral, maxPointer = mBuffer.length - 1;
        float softSymbol = 0.0f;

        for(int x = 0; x < 24; x++)
        {
            if(bufferPointer < maxPointer)
            {
                softSymbol = PhaseAwareLinearInterpolator.calculate(mBuffer[bufferPointer], mBuffer[bufferPointer + 1], fractional);
                softSymbol = Math.min(softSymbol, MAX_POSITIVE_SOFT_SYMBOL);
                softSymbol = Math.max(softSymbol, MAX_NEGATIVE_SOFT_SYMBOL);
            }
            else
            {
                softSymbol = 0.0f;
            }

            score += softSymbol * symbols[x];
            fractional += samplesPerSymbol;
            integral = (int)Math.floor(fractional);
            bufferPointer += integral;
            fractional -= integral;
        }

        return score;
    }

    /**
     * Sets or updates the samples per symbol
     * @param samplesPerSymbol to apply.
     */
    public void setSamplesPerSymbol(float samplesPerSymbol)
    {
        mSamplesPerSymbol = samplesPerSymbol;
        mMaxSamplesPerSymbol = samplesPerSymbol * (1.0f + SAMPLES_PER_SYMBOL_ALLOWABLE_DEVIATION);
        mMinSamplesPerSymbol = samplesPerSymbol * (1.0f - SAMPLES_PER_SYMBOL_ALLOWABLE_DEVIATION);
        //Protected region for score() method to avoid IndexOutOfBounds = Max Adjustment + Max Step Size for Max SPS
        mBufferInterpolatorReservedRegion = (int)Math.ceil((mMaxSamplesPerSymbol / 2.0f) + (mMaxSamplesPerSymbol / 10.0f));
        mObservedSamplesPerSymbol = mSamplesPerSymbol;
        mSamplePoint = mSamplesPerSymbol;
        mLaggingSyncOffset1 = mSamplesPerSymbol / 3;
        mLaggingSyncOffset2 = mLaggingSyncOffset1 * 2;
        mBufferWorkspaceLength = (int)Math.ceil(BUFFER_WORKSPACE_LENGTH_DIBITS * mSamplesPerSymbol);
        int bufferLength = (int)(Math.ceil(BUFFER_LENGTH_DIBITS * mSamplesPerSymbol));
        mBuffer = new float[bufferLength];
        mBufferLoadPointer = (int)Math.ceil(BUFFER_PROTECTED_REGION_DIBITS * mSamplesPerSymbol);
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
            return sample > SYMBOL_QUADRANT_BOUNDARY ? Dibit.D01_PLUS_3 : Dibit.D00_PLUS_1;
        }
        else
        {
            return sample < -SYMBOL_QUADRANT_BOUNDARY ? Dibit.D11_MINUS_3 : Dibit.D10_MINUS_1;
        }
    }
}
