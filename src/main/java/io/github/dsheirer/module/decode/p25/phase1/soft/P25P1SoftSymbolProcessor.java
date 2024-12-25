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

package io.github.dsheirer.module.decode.p25.phase1.soft;

import io.github.dsheirer.dsp.filter.interpolator.PhaseAwareLinearInterpolator;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.module.decode.dmr.DibitDelayLine;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetector;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetectorFactory;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SyncDetector;
import io.github.dsheirer.sample.Listener;
import java.nio.ByteBuffer;

public class P25P1SoftSymbolProcessor
{
    private static final int BUFFER_PROTECTED_REGION_DIBITS = 26; //Sync (24) plus 2
    private static final int BUFFER_WORKSPACE_LENGTH_DIBITS = 25; //This can be adjusted for efficiency
    private static final int BUFFER_LENGTH_DIBITS = BUFFER_PROTECTED_REGION_DIBITS + BUFFER_WORKSPACE_LENGTH_DIBITS;
    private static final int MAX_SYMBOLS_FOR_FINE_SYNC = 865; //Length of longest messages: LDU1 and LDU2
    private static final int MIN_SYMBOLS_FOR_TIMING_ADJUST = 72;
    private static final float MAX_POSITIVE_SOFT_SYMBOL = Dibit.D01_PLUS_3.getIdealPhase();
    private static final float MAX_NEGATIVE_SOFT_SYMBOL = Dibit.D11_MINUS_3.getIdealPhase();
    private static final float SYMBOL_QUADRANT_BOUNDARY = (float)(Math.PI / 2.0);
    private static final float SYNC_DETECTION_THRESHOLD = 70;
    private static final float[] SYNC_PATTERN_SYMBOLS = P25P1SyncDetector.syncPatternToSymbols();
    private static final Dibit[] SYNC_PATTERN_DIBITS = P25P1SyncDetector.syncPatternToDibits();
    private P25P1SoftSyncDetector mSyncDetector = P25P1SoftSyncDetectorFactory.getDetector();
    private P25P1SoftSyncDetector mSyncDetectorLag1 = P25P1SoftSyncDetectorFactory.getDetector();
    private P25P1SoftSyncDetector mSyncDetectorLag2 = P25P1SoftSyncDetectorFactory.getDetector();
    private DibitToByteBufferAssembler mDibitAssembler = new DibitToByteBufferAssembler(300);
    private P25P1SoftMessageFramer mMessageFramer;
    private DibitDelayLine mDibitDelayLine = new DibitDelayLine(24); //Length of the sync pattern
    private boolean mSyncLock = false;
    private float mLaggingSyncOffset1;
    private float mLaggingSyncOffset2;
    private float mObservedSamplesPerSymbol;
    private float mSamplesPerSymbol;
    private float mSamplePoint;
    private float[] mBuffer;
    private int mBufferLoadPointer;
    private int mBufferPointer;
    private int mBufferWorkspaceLength;
    private int mSymbolsSinceLastSync = 0;

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

                if(mSamplePoint < 1)
                {
                    mSymbolsSinceLastSync++;

                    if(mSymbolsSinceLastSync > MAX_SYMBOLS_FOR_FINE_SYNC)
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

                    if(mSyncLock && scorePrimary > SYNC_DETECTION_THRESHOLD && optimize(0.0f))
                    {
                        System.out.println("SYNC LOCK - Score: " + scorePrimary + " Symbols: " + mSymbolsSinceLastSync);
                        System.out.println("-------------------------------------------------------------------------");
                        mSymbolsSinceLastSync = 0;
                        mMessageFramer.syncDetected();
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
                                scoreLag1 > SYNC_DETECTION_THRESHOLD && optimize(-mLaggingSyncOffset1))
                        {
                            System.out.println("SYNC LAG 1 - Score: " + scoreLag1 + " Symbols: " + mSymbolsSinceLastSync);
                            System.out.println("-------------------------------------------------------------------------");
                            mSymbolsSinceLastSync = 0;
                            mMessageFramer.syncDetected();
                        }
                        else if(mSymbolsSinceLastSync > 1 && scoreLag2 > scorePrimary &&
                                scoreLag2 > SYNC_DETECTION_THRESHOLD && optimize(-mLaggingSyncOffset2))
                        {
                            System.out.println("SYNC LAG 2 - Score: " + scoreLag2 + " Symbols: " + mSymbolsSinceLastSync);
                            System.out.println("-------------------------------------------------------------------------");
                            mSymbolsSinceLastSync = 0;
                            mMessageFramer.syncDetected();
                        }
                        else if(scorePrimary > SYNC_DETECTION_THRESHOLD && optimize(0.0f))
                        {
                            System.out.println("SYNC PRIMARY - Score: " + scorePrimary + " Symbols: " + mSymbolsSinceLastSync);
                            System.out.println("-------------------------------------------------------------------------");
                            mSymbolsSinceLastSync = 0;
                            mMessageFramer.syncDetected();
                            mSyncLock = true;
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
     * @param additionalOffset from current mBufferPointer and mSamplePoint.  This can be zero offset for the primary
     * sync detector or an offset for the lagging sync detectors.
     * @return true if there is a positive sync detection.
     */
    private boolean optimize(float additionalOffset)
    {
        boolean debugLogging = true;

        //Offset is the start of the first sample of the first symbol of the sync pattern calculated from the current
        //buffer pointer and sample point which should be the final sample of the final symbol of the detected sync.
        float offset = (mBufferPointer + mSamplePoint) + additionalOffset - (mObservedSamplesPerSymbol * 23);

        //Find the optimal symbol timing
        float stepSize = mObservedSamplesPerSymbol / 10.0f;
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

        while(stepSize > stepSizeMin && Math.abs(adjustment) <= adjustmentMax)
        {
            if(scoreLeft > scoreRight && scoreLeft > scoreCenter)
            {
                debugSB.append("Optimize - LEFT: " + scoreLeft + " Center: " + scoreCenter + " Right: " + scoreRight + " StepSize: " + stepSize + " Symbols Since Last Sync: " + mSymbolsSinceLastSync).append("\n");
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
                debugSB.append("Optimize - Left: " + scoreLeft + " Center: " + scoreCenter + " RIGHT: " + scoreRight + " StepSize: " + stepSize + " Symbols Since Last Sync: " + mSymbolsSinceLastSync).append("\n");
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
                debugSB.append("Optimize - LEFT: " + scoreLeft + " CENTER: " + scoreCenter + " Right: " + scoreRight + " StepSize: " + stepSize + " Symbols Since Last Sync: " + mSymbolsSinceLastSync).append("\n");
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
        }

        //If we didn't find an optimal correlation score above the 95 threshold, return a false sync.
        if(scoreCenter < 95)
        {
            debugSB.append("Optimize Failed - Score: " + scoreCenter + " - False Sync!!").append("\n");

            if(debugLogging)
            {
                System.out.println(debugSB);
            }
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
        if(mSyncLock && adjustment < 0.5 && mSymbolsSinceLastSync >= MIN_SYMBOLS_FOR_TIMING_ADJUST &&
                mSymbolsSinceLastSync <= MAX_SYMBOLS_FOR_FINE_SYNC)
        {
            debugSB.append("Observed SPS Before: " + mObservedSamplesPerSymbol).append("\n");
            mObservedSamplesPerSymbol += (float)((double)adjustment / (double)mSymbolsSinceLastSync * 0.2);
            debugSB.append("Observed SPS  After: " + mObservedSamplesPerSymbol).append("\n");
        }

        //Overwrite the most recent 24 dibits with the detected sync so there's no sync bit errors
//TODO: move this up to the receive() method as part of each sync detect
        mDibitDelayLine.update(SYNC_PATTERN_DIBITS);

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
     * @param pattern to correlate against.
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
        mSamplesPerSymbol = samplesPerSymbol;
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
