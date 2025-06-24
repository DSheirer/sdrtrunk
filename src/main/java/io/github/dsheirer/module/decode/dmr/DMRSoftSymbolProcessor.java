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

import io.github.dsheirer.dsp.filter.interpolator.LinearInterpolator;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.module.decode.dmr.sync.DMRSoftSyncDetector;
import io.github.dsheirer.module.decode.dmr.sync.DMRSoftSyncDetectorFactory;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncDetectMode;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncModeMonitor;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.sync.visualizer.SyncResultsViewer;
import io.github.dsheirer.sample.Listener;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 * DMR soft symbol processor.  Processes a stream of differentially decoded samples to align to the symbol timing and
 * symbol period and extract symbols from the sample stream.
 *
 * Uses two soft sync correlation detectors to achieve initial fine sync with a primary sync detector and a lagging
 * sync detector sampling at 1/2 the symbol period to ensure the sync is detected while only calculating at each symbol
 * period interval.  Once a sync is detected, fine sync is declared and the lagging sync detector is disabled. Fine sync
 * disables whenever the quantity of processed symbols exceeds one DMR burst length and the lagging detector
 * re-enables.
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
 *
 * The equalizer calculates balance and gain values to apply to the differentially decoded sample stream to optimize
 * the constellation skew and openness. The equalizer is updated at each quality sync detection and then the equalizer
 * values are applied across the entire burst through to the next sync detection.  DMR sync patterns are made up of
 * equal quantities of +3 and -3 symbols which support the equalizer update approach.
 */
public class DMRSoftSymbolProcessor
{
    private static final int BUFFER_PROTECTED_REGION_DIBITS = 92;
    private static final int BUFFER_WORKSPACE_LENGTH_DIBITS = 25; //This can be adjusted for efficiency
    private static final int BUFFER_LENGTH_DIBITS = BUFFER_PROTECTED_REGION_DIBITS + BUFFER_WORKSPACE_LENGTH_DIBITS;
    private static final float EQUALIZER_LOOP_GAIN = 0.3f;
    private static final float MAXIMUM_EQUALIZER_BALANCE = (float)(Math.PI / 4.0);
    private static final float MAXIMUM_EQUALIZER_GAIN = 1.35f;
    private static final float MAXIMUM_POSITIVE_SAMPLE_PHASE = 3.5f;
    private static final float MAXIMUM_NEGATIVE_SAMPLE_PHASE = -3.5f;
    private static final float SAMPLES_PER_SYMBOL_ALLOWABLE_DEVIATION = 0.005f; //.5%
    private static final float SYMBOL_QUADRANT_BOUNDARY = (float)(Math.PI / 2.0);
    private static final float SYNC_DETECTION_THRESHOLD = 60;
    private static final float SYNC_OPTIMIZED_THRESHOLD = 80;
    private static final float SYNC_EQUALIZED_THRESHOLD = 100;
    private static final float TWO_PI = (float)(Math.PI * 2.0);
    private DMRSyncModeMonitor mSyncModeMonitor = new DMRSyncModeMonitor();
    private DMRSoftSyncDetector mSyncDetector = DMRSoftSyncDetectorFactory.getDetector();
    private DMRSoftSyncDetector mSyncDetectorSecondary = DMRSoftSyncDetectorFactory.getDetector();
    private DibitToByteBufferAssembler mDibitAssembler = new DibitToByteBufferAssembler(300);
    //Dibit delay line sizing: CACH(12) + MESSAGE_PREFIX(54) + SYNC(24)
    private DibitDelayLine mDibitDelayLine = new DibitDelayLine(90);
    private DMRMessageFramer mMessageFramer;
    private boolean mFineSync = false;
    private boolean mEqualizerInitialized = false;
    private double mNoiseStandardDeviationThreshold;
    private double mSecondarySyncOffset;
    private double mSamplesPerSymbol;
    private double mObservedSamplesPerSymbol;
    private double mSamplePoint;
    private float mEqualizerBalance = 0.0f;
    private float mEqualizerGain = 1.0f;
    private float mOptimizeFineIncrement;
    private float[] mBuffer;
    private int mBufferInterpolatorReservedRegion;
    private int mBufferLoadPointer;
    private int mBufferPointer;
    private int mBufferWorkspaceLength;
    private int mSymbolsSinceLastSync = 0;
    private SyncResultsViewer mSyncResultsViewer;

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
        mSyncModeMonitor.add(mSyncDetectorSecondary);
    }

    /**
     * Sets base station sync detection mode.  This disables detection for MOBILE and DIRECT sync patterns.  This can
     * be used for traffic channels that are known to be part of a base station.
     * @param enabled true for base mode only, false otherwise.
     */
    public void setBaseStationMode(boolean enabled)
    {
        mSyncModeMonitor.setMode(enabled ? DMRSyncDetectMode.BASE_ONLY : DMRSyncDetectMode.AUTOMATIC);
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

            for(int x = mBufferPointer; x < mBufferLoadPointer; x++)
            {
                //Unwrap phases
                if(mBuffer[x - 1] > 1.5f && mBuffer[x] < -1.5f)
                {
                    mBuffer[x] += TWO_PI;
                }
                else if(mBuffer[x - 1] < -1.5f && mBuffer[x] > 1.5f)
                {
                    mBuffer[x] -= TWO_PI;
                }

                //Apply equalizer adjustments
                mBuffer[x] += mEqualizerBalance;
                mBuffer[x] *= mEqualizerGain;

                //Allow the equalized buffer samples to exceed PI (3.14) by a small factor (3.5) to ensure the optimize
                // and equalizer update functions work correctly.  The toSymbol() method will correctly map any symbols
                // that exceed +/- PI into the correct quadrant.
                mBuffer[x] = Math.min(mBuffer[x], MAXIMUM_POSITIVE_SAMPLE_PHASE);
                mBuffer[x] = Math.max(mBuffer[x], MAXIMUM_NEGATIVE_SAMPLE_PHASE);
            }

            float softSymbol, primaryScore, secondaryScore;
            double secondary;
            int secondaryIntegral;

            while(mBufferPointer < (mBufferLoadPointer - mBufferInterpolatorReservedRegion))
            {
                mBufferPointer++;
                mSamplePoint--;

                if(mSamplePoint < 1)
                {
                    if(mSymbolsSinceLastSync > 144)
                    {
                        mFineSync = false;
                    }

                    softSymbol = LinearInterpolator.calculate(mBuffer[mBufferPointer], mBuffer[mBufferPointer + 1], mSamplePoint);
                    mSyncDetector.process(softSymbol);
                    Dibit symbol = toSymbol(softSymbol);

                    //Store the symbol in the delay line and broadcast the delayed ejected symbol to the message framer
                    //and the bitstream assembler.
                    Dibit ejected = mDibitDelayLine.insert(symbol);
                    mMessageFramer.receive(ejected);
                    mDibitAssembler.receive(ejected);
                    mSymbolsSinceLastSync++;

                    if(mFineSync)
                    {
                        if(mSymbolsSinceLastSync >= 144)
                        {
                            if(mMessageFramer.isVoiceSuperFrame())
                            {
                                mSymbolsSinceLastSync -= 144;
                            }
                            else
                            {
                                primaryScore = mSyncDetector.calculate();

                                //Debug - visualize the sync to show samples and symbol timing
//                                if(primaryScore > SYNC_DETECTION_THRESHOLD)
//                                {
//                                    visualizeSyncDetect(mSyncDetector.getDetectedPattern(), primaryScore, true);
//                                }

                                if(primaryScore > SYNC_DETECTION_THRESHOLD && optimizeFine(mSyncDetector.getDetectedPattern()))
                                {
                                    //Update the sync mode monitor and message framer
                                    mSyncModeMonitor.detected(mSyncDetector.getDetectedPattern());
                                    mMessageFramer.syncDetected(mSyncDetector.getDetectedPattern());
                                    mSymbolsSinceLastSync = 0;
                                }
                                else
                                {
                                    mFineSync = false;
                                    mSyncDetectorSecondary.reset();
                                    secondary = mBufferPointer + mSamplePoint - mSecondarySyncOffset;
                                    secondaryIntegral = (int)Math.floor(secondary);
                                    softSymbol = LinearInterpolator.calculate(mBuffer[secondaryIntegral], mBuffer[secondaryIntegral + 1], secondary - secondaryIntegral);
                                    mSyncDetectorSecondary.process(softSymbol);
                                }
                            }
                        }
                    }
                    else
                    {
                        primaryScore = mSyncDetector.calculate();
                        secondary = mBufferPointer + mSamplePoint - mSecondarySyncOffset;
                        secondaryIntegral = (int)Math.floor(secondary);
                        softSymbol = LinearInterpolator.calculate(mBuffer[secondaryIntegral], mBuffer[secondaryIntegral + 1], secondary - secondaryIntegral);
                        secondaryScore = mSyncDetectorSecondary.processAndCalculate(softSymbol);

                        if(primaryScore > SYNC_DETECTION_THRESHOLD && optimizeCoarse(mSyncDetector.getDetectedPattern(), 0))
                        {
                            mMessageFramer.syncDetected(mSyncDetector.getDetectedPattern());
                            mFineSync = true;
                            mSymbolsSinceLastSync = 0;
                        }
                        else if(secondaryScore > SYNC_DETECTION_THRESHOLD && optimizeCoarse(mSyncDetectorSecondary.getDetectedPattern(), -mSecondarySyncOffset))
                        {
                            mMessageFramer.syncDetected(mSyncDetectorSecondary.getDetectedPattern());
                            mFineSync = true;
                            mSymbolsSinceLastSync = 0;
                        }
                    }

                    //Add another symbol's worth of samples to the counter
                    mSamplePoint += mObservedSamplesPerSymbol;
                }
            }
        }
    }

    /**
     * Indicates if the samples in the sample buffer that contain a detected sync pattern have a standard deviation
     * that is lower than the expected sample to sample deviation for a modulated signal.  False detects against
     * noise tend to have a standard deviation that is 2x the expected value.
     * @param offset to the sample representing the final symbol in the detected sync pattern.
     * @return true if the standard deviation is more than expected.
     */
    private boolean isNoisy(double offset)
    {
        StandardDeviation standardDeviation = new StandardDeviation();
        int start = (int)Math.floor(offset - (23 * mObservedSamplesPerSymbol));
        int end = (int)Math.ceil(offset);
        end = Math.min(end, mBuffer.length - 1);

        for(int i = start; i < end; i++)
        {
            standardDeviation.increment(mBuffer[i] - mBuffer[i + 1]);
        }

        return standardDeviation.getResult() > mNoiseStandardDeviationThreshold;
    }

    /**
     * Adjusts the symbol timing and symbol spacing to identify the best achievable sync correlation score and apply
     * those adjustments when the correlation score exceeds a positive sync detection threshold.
     * @param pattern that was detected
     * @param additionalOffset from current mBufferPointer and mSamplePoint.  This can be zero offset for the primary
     * sync detector or an offset for the lagging sync detectors.
     * @return true if there is a positive sync detection.
     */
    private boolean optimizeCoarse(DMRSyncPattern pattern, double additionalOffset)
    {
        //Offset is the start of the first sample of the first symbol of the sync pattern calculated from the current
        //buffer pointer and sample point which should be the final sample of the final symbol of the detected sync.
        double offset = mBufferPointer + mSamplePoint + additionalOffset;

        //Reject any sync detections where the sample:sample standard deviation exceeds the noise threshold.
        if(isNoisy(offset))
        {
            return false;
        }

        //Find the optimal symbol timing
        double stepSize = mSamplesPerSymbol / 8.0; //Start at 1/8th of the samples per symbol
        double stepSizeMin = mOptimizeFineIncrement;
        double adjustment = 0.0;
        double adjustmentMax = mSamplesPerSymbol / 4.0;
        double candidate = offset;

        float scoreCenter = score(candidate, mObservedSamplesPerSymbol, pattern);

        candidate = offset - stepSize;
        float scoreLeft = score(candidate, mObservedSamplesPerSymbol, pattern);

        candidate = offset + stepSize;
        float scoreRight = score(candidate, mObservedSamplesPerSymbol, pattern);

        while(stepSize > stepSizeMin && Math.abs(adjustment) <= adjustmentMax)
        {
            if(scoreLeft > scoreRight && scoreLeft > scoreCenter)
            {
                adjustment -= stepSize;
                scoreRight = scoreCenter;
                scoreCenter = scoreLeft;

                candidate = offset + adjustment - stepSize;
                scoreLeft = score(candidate, mObservedSamplesPerSymbol, pattern);
            }
            else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
            {
                adjustment += stepSize;
                scoreLeft = scoreCenter;
                scoreCenter = scoreRight;

                candidate = offset + adjustment + stepSize;
                scoreRight = score(candidate, mObservedSamplesPerSymbol, pattern);
            }
            else
            {
                stepSize *= 0.5f;

                if(stepSize > stepSizeMin)
                {
                    candidate = offset + adjustment - stepSize;
                    scoreLeft = score(candidate, mObservedSamplesPerSymbol, pattern);

                    candidate = offset + adjustment + stepSize;
                    scoreRight = score(candidate, mObservedSamplesPerSymbol, pattern);
                }
            }
        }

        //If we didn't find an optimal correlation score above the 95 threshold, return a false sync.
        if(scoreCenter < 95)
        {
            return false;
        }

        adjustment += additionalOffset;
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

        boolean resample = !mEqualizerInitialized || (Math.abs(adjustment) > 0.25);

        updateEqualizer(pattern);
//        visualizeSyncDetect(pattern, scoreCenter, true);

        //If the equalizer was just initialized or the timing error adjustment is high enough, resample the symbols.
        // Otherwise, overwrite the captured sync pattern in the delay buffer with the detected sync pattern to
        // eliminate any sync bit errors.
        if(resample)
        {
            double resamplePointer = mBufferPointer + mSamplePoint;
            resamplePointer -= (89 * mObservedSamplesPerSymbol); //Start at 89 (+ 1 current = 90) symbols
            int integral;

            for(int x = 0; x < 66; x++)
            {
                integral = (int)Math.floor(resamplePointer);

                if(integral >= 0)
                {
                    float resampledSoftSymbol = LinearInterpolator.calculate(mBuffer[integral], mBuffer[integral + 1], resamplePointer - integral);
                    mDibitDelayLine.insert(toSymbol(resampledSoftSymbol));
                }
                else
                {
                    //This shouldn't happen since there's 2x dibits of padding on the front side, but just in case.
                    mDibitDelayLine.insert(Dibit.D01_PLUS_3);
                }

                resamplePointer += mObservedSamplesPerSymbol;
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
     * Performs fine-grained symbol timing optimization of +/- .4% of the samples per symbol as needed.
     * @param pattern that was detected
     * @return true if detection score exceeds the optimized and equalized threshold for a quality sync detect.
     */
    private boolean optimizeFine(DMRSyncPattern pattern)
    {
        //Offset is the sample for the last symbol of the detected sync pattern.
        double offset = mBufferPointer + mSamplePoint;
        float currentScore = score(offset, mObservedSamplesPerSymbol, pattern);
        double candidate = offset - mOptimizeFineIncrement;
        float candidateScore = score(candidate, mObservedSamplesPerSymbol, pattern);
        boolean adjusted = false;

        if(candidateScore > currentScore)
        {
            mSamplePoint -= mOptimizeFineIncrement;
            adjusted = true;
        }
        else
        {
            candidate = offset + mOptimizeFineIncrement;
            candidateScore = score(candidate, mObservedSamplesPerSymbol, pattern);

            if(candidateScore > currentScore)
            {
                mSamplePoint += mOptimizeFineIncrement;
                adjusted = true;
            }
        }

        if(adjusted)
        {
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
        }

        if(candidateScore > SYNC_OPTIMIZED_THRESHOLD)
        {
            updateEqualizer(pattern);
        }

        return candidateScore > SYNC_EQUALIZED_THRESHOLD;
    }

    /**
     * Update the equalizer balance and gain when the sync pattern is detected in the sample buffer.  This method
     * resamples the symbols and compares each soft symbol to the ideal symbol phase to develop average error
     * measurements for balance and gain.  On initial sync detection, the equalizer settings are applied to the
     * samples in the buffer allowing the symbols to be resampled during coarse sync acquisition.
     * @param pattern that was detected.
     */
    private void updateEqualizer(DMRSyncPattern pattern)
    {
        float[] symbols = pattern.toSymbols();
        double resampleStart = mBufferPointer + mSamplePoint;
        int resampleStartIntegral = (int)Math.floor(resampleStart);
        float resampledSoftSymbol = LinearInterpolator.calculate(mBuffer[resampleStartIntegral],
                mBuffer[resampleStartIntegral + 1], resampleStart - resampleStartIntegral);
        float balanceAccumulator = resampledSoftSymbol - symbols[23];
        float gainAccumulator = Math.abs(symbols[23]) - Math.abs(resampledSoftSymbol);

        resampleStart -= (23 * mObservedSamplesPerSymbol); //Start at 89 (+ 1 current = 90) symbols
        resampleStartIntegral = (int)Math.floor(resampleStart);

        for(int x = 0; x < 23; x++)
        {
            if(resampleStartIntegral >= 0)
            {
                resampledSoftSymbol = LinearInterpolator.calculate(mBuffer[resampleStartIntegral],
                        mBuffer[resampleStartIntegral + 1], resampleStart - resampleStartIntegral);
                balanceAccumulator += resampledSoftSymbol - symbols[x];
                gainAccumulator += Math.abs(symbols[x]) - Math.abs(resampledSoftSymbol);
            }

            resampleStart += mObservedSamplesPerSymbol;
            resampleStartIntegral = (int)Math.floor(resampleStart);
        }

        //Average the accumulated error over 24 sync symbols
        balanceAccumulator /= -24.0f;
        gainAccumulator /= (24.0f * Dibit.D01_PLUS_3.getIdealPhase());

        if(mEqualizerInitialized)
        {
            //Limit equalizer adjustments at each sync after the initial equalizer setup.
            mEqualizerBalance += (balanceAccumulator * EQUALIZER_LOOP_GAIN);
            mEqualizerGain += (gainAccumulator * EQUALIZER_LOOP_GAIN);
        }
        else
        {
            mEqualizerBalance += balanceAccumulator;
            mEqualizerGain += gainAccumulator;
        }

        //Constrain balance to +/- PI/4
        mEqualizerBalance = Math.min(mEqualizerBalance, MAXIMUM_EQUALIZER_BALANCE);
        mEqualizerBalance = Math.max(mEqualizerBalance, -MAXIMUM_EQUALIZER_BALANCE);

        //Constrain gain between 1.0f and 1.35f
        mEqualizerGain = Math.min(mEqualizerGain, MAXIMUM_EQUALIZER_GAIN);
        mEqualizerGain = Math.max(mEqualizerGain, 1.0f);

        if(!mEqualizerInitialized)
        {
            //Apply the initial gain settings to the samples in the buffer so that the symbols can be resampled.
            for(int x = 0; x < mBufferPointer; x++)
            {
                mBuffer[x] = (mBuffer[x] + mEqualizerBalance) * mEqualizerGain;
            }

            mEqualizerInitialized = true;
        }
    }

    /**
     * Debug method to visualize the contents of the sample buffer.
     */
    public void visualizeBufferContents()
    {
        //This will block until the viewer is constructed and showing.
        if(mSyncResultsViewer == null)
        {
            mSyncResultsViewer = new SyncResultsViewer();
        }

        int offset = 3;
        int length = (int)Math.ceil(mObservedSamplesPerSymbol * 90) + (2 * offset);
        int end = mBufferPointer + offset;
        int start = end - length;
        float[] symbols = new float[90];
        float[] samples = Arrays.copyOfRange(mBuffer, start, end);

        float[] intervals = new float[90];

        int adjust = mBufferPointer - length + offset;
        double pointer = mBufferPointer + mSamplePoint - adjust;

        for(int x = 89; x >= 0; x--)
        {
            intervals[x] = (float)pointer;
            pointer -= mObservedSamplesPerSymbol;
        }

        float[] sync = new float[90];
        Arrays.fill(sync, -1.57f); //Cutoff between -1 & -1 to show symbol timings

        CountDownLatch countDownLatch = new CountDownLatch(1);
        mSyncResultsViewer.receive(symbols, sync, samples, intervals, "BUFFER CONTENTS", countDownLatch);

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
     * Debug method to visualize the contents of the sync detection for the samples and symbols and symbol timing.
     * @param pattern that was detected.
     * @param score from the sync detector
     * @param primary sync detector (true) or secondary (false)
     */
    public void visualizeSyncDetect(DMRSyncPattern pattern, float score, boolean primary)
    {
        //This will block until the viewer is constructed and showing.
        if(mSyncResultsViewer == null)
        {
            mSyncResultsViewer = new SyncResultsViewer();
        }

        int offset = 3;
        int length = (int)Math.ceil(mObservedSamplesPerSymbol * 23) + (2 * offset);
        int end = mBufferPointer + offset;
        int start = end - length;
        float[] symbols = new float[24];
        float[] samples = Arrays.copyOfRange(mBuffer, start, end);

        float[] intervals = new float[24];

        int adjust = mBufferPointer - length + offset;
        double pointer = mBufferPointer + mSamplePoint - adjust;

        double symbolPointer = mBufferPointer + mSamplePoint - (mObservedSamplesPerSymbol * 23);
        int symbolIntegral = (int)Math.floor(symbolPointer);
        double mu = symbolPointer - symbolIntegral;

        for(int x = 0; x < 24; x++)
        {
            symbols[x] = LinearInterpolator.calculate(mBuffer[symbolIntegral], mBuffer[symbolIntegral + 1], mu);
            symbolPointer += mObservedSamplesPerSymbol;
            symbolIntegral = (int)Math.floor(symbolPointer);
            mu = symbolPointer - symbolIntegral;
        }

        for(int x = 23; x >= 0; x--)
        {
            intervals[x] = (float)pointer;
            pointer -= mObservedSamplesPerSymbol;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        mSyncResultsViewer.receive(symbols, pattern.toSymbols(), samples, intervals,
                pattern + " Score: " + score + (primary ? " PRIMARY" : " SECONDARY"), countDownLatch);

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
     * Calculates the sync correlation score for sync pattern ending at the specified offset and samples per symbol interval.
     * @param offset to the final symbol in the soft symbol buffer
     * @param samplesPerSymbol spacing to test for.
     * @param pattern to correlate against.
     * @return correlation score.
     */
    public float score(double offset, double samplesPerSymbol, DMRSyncPattern pattern)
    {
        float[] symbols = pattern.toSymbols();
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
            }
            else
            {
                softSymbol = 0.0f;
            }

            score += softSymbol * symbols[x];
            pointer += samplesPerSymbol;
            bufferPointer = (int)Math.floor(pointer);
            fractional = pointer - bufferPointer;
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
        float maxSamplesPerSymbol = samplesPerSymbol * (1.0f + SAMPLES_PER_SYMBOL_ALLOWABLE_DEVIATION);
        mOptimizeFineIncrement = samplesPerSymbol * .004f; //Adjust at .4%
        //Protected region for score() method to avoid IndexOutOfBounds = Max Adjustment + Max Step Size for Max SPS
        mBufferInterpolatorReservedRegion = (int)Math.ceil((maxSamplesPerSymbol / 2.0f) + (maxSamplesPerSymbol / 10.0f));
        mObservedSamplesPerSymbol = mSamplesPerSymbol;
        mNoiseStandardDeviationThreshold = Dibit.D01_PLUS_3.getIdealPhase() * 2 / mSamplesPerSymbol * 1.2; //120% of optimal
        mSamplePoint = mSamplesPerSymbol;
        mSecondarySyncOffset = mSamplesPerSymbol / 2.0;
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
