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

package io.github.dsheirer.dsp.c4fm;

import io.github.dsheirer.dsp.filter.interpolator.LinearInterpolator;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.gui.viewer.sync.FMSyncResultsViewer;
import io.github.dsheirer.gui.viewer.sync.ISyncResultsListener;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.sample.Listener;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 * C4FM symbol processor provides sync detection, symbol timing recovery and symbol decision stream.
 *
 * This processor employs two sync detectors staged 1/2 symbol period apart as a primary and lagging detector.  When a
 * sync is detected, it applies an optimization process to adjust symbol timing, constellation gain and constellation
 * balance similar to a phase locked loop (PLL).  For protocols that consistently transmit a sync pattern at routine
 * intervals, the PLL only adjusts at each sync interval.  For protocols (ie P25 Phase 1 C4FM) with variable length
 * messaging and longer message lengths, the processor employs decision directed feedback to dynamically adjust the
 * constellation balance like a dynamic PLL.
 *
 * Subclass implementations can override the default behavior to provide protocol specific implementations tuned to
 * the features of the protocol.  The general workflow is:
 *
 * 1. Detect the sync pattern
 * 2. Optimize on the sync pattern and produce a Correction candidate
 * 3. Verify the correction candidate and the waveform of the sync pattern with the correction applied.
 * 4. Apply the correction.
 * 5. Produce a stream of symbol decisions
 * 6. (Optional) adjust constellation balance at each symbol interval between sync detections, similar to a PLL.
 */
public abstract class C4FMSymbolProcessor
{
    private static final DecimalFormat DEBUG_DF = new DecimalFormat("00.0000");
    protected final Correction INVALID_SYNC_DETECTION = new Correction(0d, Double.MAX_VALUE, 0f, 0f, 0f, 0f, 0);
    protected final ISoftC4FMSyncDetector mSyncDetector;
    protected final SampleEqualizer mSampleEqualizer;
    private final DibitToByteBufferAssembler mDibitAssembler = new DibitToByteBufferAssembler(300);
    private final FeedbackDecoder mFeedbackDecoder;
    private final C4FMMessageFramer mMessageFramer;
    private final ISoftC4FMSyncDetector mSyncDetectorLagging;
    //Detector (primary or lagging) that triggered the current sync detection.  For multi-pattern sync detectors, this
    //detector's most recently detected sync pattern is used for optimization, validation and equalizer training.
    private ISoftC4FMSyncDetector mActiveSyncDetector;
    private final C4FMEqualizer mSymbolEqualizer;
    private static final double SYMBOL_TIMING_GAIN = 0.25;
    //Clip applied to the Gardner timing error (radians squared) to limit the impact of FM discriminator clicks
    private static final float TIMING_ERROR_CLIP = 2.0f;
    private float mPreviousTimingSymbol = 0.0f;
    //Ideal phase of the outer (+/-3) symbols in the equalized symbol domain, as defined by Dibit.D01_PLUS_3
    private static final double IDEAL_OUTER_SYMBOL_PHASE = 3.0 * Math.PI / 4.0;
    //Allowable gain deviation from the nominal gain, as a fraction of the nominal gain.
    private static final double SAMPLE_GAIN_TOLERANCE = 0.1;
    private double mNoiseStandardDeviationThreshold;
    private double mSamplePoint;
    protected double mSamplesPerSymbol;
    protected float[] mBuffer;
    private float mLaggingSyncOffset;
    protected float mOptimizeFineIncrement;
    private int mBufferPointer;
    private int mBufferReloadThreshold;
    private int mDelayedSyncNotificationSymbolsRemaining = 0;
    private int mSymbolsSinceLastSync = 0;
    private int mConsecutiveSyncPeriods = 0;
    private int mSyncWorkspaceLength;
    private int mBufferReloadLength;
    protected boolean mSynchronized = false;
    private boolean mDelayedSyncNotificationPending = false;

    private final boolean mDebugVisualizeSync = false;
    private final boolean mDebugVisualizeSymbols = false;
    private final boolean mDebugLogDetectionScores = false;
    private final int mDebugSymbolStart = 0;
    private ISyncResultsListener mDebugSyncResultsViewer;
    private int mDebugSymbolCounter = 0;
    private int mDebugSampleCounter = 0;

    /**
     * Constructs an instance
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public C4FMSymbolProcessor(FeedbackDecoder feedbackDecoder, C4FMMessageFramer messageFramer,
                               ISoftC4FMSyncDetector primary, ISoftC4FMSyncDetector lagging)
    {
        mMessageFramer = messageFramer;
        mFeedbackDecoder = feedbackDecoder;
        mSyncDetector = primary;
        mSyncDetectorLagging = lagging;
        mActiveSyncDetector = primary;
        mSymbolEqualizer = new C4FMEqualizer(mSyncDetector.getPatternSymbols(), getEqualizerSymbolLength(),
                getEqualizerTrainingPeriodDurationSymbolCount(), getEqualizerSkipTrainingFrameCount());
        mSampleEqualizer = new SampleEqualizer(isDynamicBalanceTrackingRequired());
        DEBUG_DF.setPositivePrefix(" ");
    }

    /**
     * Indicates if the processor requires dynamic balance tracking using a decision directed PLL approach, to combat
     * multi-path fading.
     * @return true for dynamic tracking.
     */
    public abstract boolean isDynamicBalanceTrackingRequired();

    /**
     * Equalizer tap training period symbol count.  When an equalizer training is triggered by a sync detection, this
     * defines the training period length in symbols.
     * @return training period duration as a count of symbols.
     */
    public abstract int getEqualizerTrainingPeriodDurationSymbolCount();

    /**
     * Symbol equalizer span in symbols (odd values; even values are reduced by one).  Defaults to the sync pattern
     * length.  The equalizer is trained against the sync pattern, so a span much longer than the delay spread of the
     * channel adds unknowns that are fit to noise in the sync pattern.
     *
     * Note: this is invoked from the constructor, so implementations must not depend on subclass instance state.
     * @return equalizer span in symbols.
     */
    public int getEqualizerSymbolLength()
    {
        return C4FMEqualizer.getDefaultSymbolLength(mSyncDetector.getDibitLength());
    }

    /**
     * Continuous (per-symbol) Gardner symbol timing tracking loop gain, as a fraction of the samples per symbol per
     * unit of timing error, applied between sync detections.  Zero (default) disables continuous timing tracking,
     * relying on the timing corrections applied at each sync detection.  Protocols with long intervals between sync
     * patterns (ie P25 LDU voice frames) can override this to track timing drift between sync detections.
     * @return timing tracking gain
     */
    public double getTimingTrackingGain()
    {
        return 0.0;
    }

    /**
     * Once trained, number of frames (sync detections) to skip until a full-retraining.  Equalizer training can be
     * skipped at each sync detection if the variance of the soft symbol values relative to the ideal symbol values
     * doesn't exceed a threshold.  This count defines how many sync detections can be skipped before triggering an
     * updated equalizer training.
     */
    public abstract int getEqualizerSkipTrainingFrameCount();

    /**
     * Maximum message dibit length
     * @return length in dibits
     */
    public abstract int getMaxMessageDibitLength();

    /**
     * Symbol rate of the protocol.
     * @return symbols per second.
     */
    public abstract int getSymbolRate();

    /**
     * Frequency deviation of the outer (+/-3) symbols as transmitted by the protocol.
     * @return deviation in Hertz.
     */
    public abstract double getOuterSymbolDeviation();

    /**
     * Nominal gain to apply to the FM demodulated (radians per sample) waveform, normalized to samples per symbol,
     * that scales the outer (+/-3) symbols to the ideal 3*PI/4 phase used by the symbol slicer.
     *
     * The FM demodulator produces 2*PI*deviation/sampleRate radians per sample and the receive filter has unity gain
     * at DC and at the (Nyquist) symbol sampling instants, so multiplying by samples-per-symbol yields
     * 2*PI*deviation/symbolRate radians per symbol.  The remaining scalar maps that to 3*PI/4.
     *
     * Examples: P25 (1800 Hz @ 4800) = 1.0, NXDN48 (1050 Hz @ 2400) = 0.857, NXDN96 (2400 Hz @ 4800) = 0.75
     *
     * @return nominal gain scalar to apply to the samples per symbol value.
     */
    public double getNominalGainScalar()
    {
        double outerSymbolPhase = 2.0 * Math.PI * getOuterSymbolDeviation() / getSymbolRate();
        return IDEAL_OUTER_SYMBOL_PHASE / outerSymbolPhase;
    }

    /**
     * Threshold score for a sync detection.
     */
    public abstract float getSyncThresholdDetected();

    /**
     * Threshold score following a sync detection where the waveform is resampled at optimized slicer timing.
     */
    public abstract float getSyncThresholdOptimized();

    /**
     * Threshold score following a sync detection and timing optimization where the waveform is optimized for gain
     * and balance.
     */
    public abstract float getSyncThresholdEqualized();

    /**
     * Notification that a sync detection was validated and the correction applied.  Subclasses can override this to
     * capture protocol specific details (ie the detected sync pattern) from the sync detector that triggered the
     * detection.  The message framer is notified of the sync detection once the final sync symbol emerges from the
     * equalizer delay line, which can be after this method is invoked.
     * @param syncDetector that triggered the sync detection.
     */
    protected void syncDetected(ISoftC4FMSyncDetector syncDetector)
    {
        //No-op default implementation
    }

    /**
     * Indicates if a sync detection is allowed at the current symbol.  Protocols with fixed length framing (ie DMR)
     * can override this to only allow sync detections at the expected frame boundaries once synchronized, to prevent
     * false sync detections from the frame payload.  Default implementation always allows sync detection.
     * @param symbolsSinceLastSync count of symbols since the final symbol of the previous sync detection.
     * @return true if a sync detection is allowed.
     */
    protected boolean isSyncDetectionAllowed(int symbolsSinceLastSync)
    {
        return true;
    }

    /**
     * Number of consecutive sync periods (capped at 3) where a sync was detected within the max message length.
     */
    protected int getConsecutiveSyncPeriods()
    {
        return mConsecutiveSyncPeriods;
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
        //Establish buffer size based on incoming sample array length which should be consistent across decoder lifecycle
        if(mBuffer == null)
        {
            mBuffer = new float[mSyncWorkspaceLength + samples.length];
            mBufferReloadThreshold = mBuffer.length - mBufferReloadLength;
            mBufferPointer = mBufferReloadThreshold;
        }

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
                //Convert the raw (radians per sample) mean into the equalized symbol domain where the balance is
                //applied, and move the balance partway toward cancelling it.
                float residualBalance = (accumulator / 400) * mSampleEqualizer.mGain + mSampleEqualizer.mBalance;
                mSampleEqualizer.mBalance -= (residualBalance * .3f);
            }
        }

        while(samplesPointer < samples.length)
        {
            //Note: buffer pointer can become greater than reload threshold during timing optimization at sync detect
            if(bufferPointer >= bufferReloadThreshold)
            {
                int copyLength = samples.length;
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

                    if(mDebugSymbolCounter % mSyncDetector.getDibitLength() == 0)
                    {
                        visualizeSymbols(bufferPointer, samplePoint, symbolsSinceLastSync);
                    }

                    symbolsSinceLastSync++;

                    //Clear synchronized state when we miss a sync period.
                    if(mSynchronized && symbolsSinceLastSync > getMaxMessageDibitLength())
                    {
                        mSynchronized = false;
                        mSymbolEqualizer.syncLost();
                        mConsecutiveSyncPeriods = 0;
                    }

                    softSymbol = mSampleEqualizer.getEqualizedSymbol(mBuffer[bufferPointer], mBuffer[bufferPointer + 1], samplePoint);
                    float timingSymbol = softSymbol;

                    //Process dynamic updates to the PLL when supported by the decoder
                    softSymbol = mSampleEqualizer.correct(softSymbol);

                    //Check for lagging sync pattern
                    float lag = (float)(bufferPointer + samplePoint - mLaggingSyncOffset);
                    int lagIntegral = (int)Math.floor(lag);
                    float softSymbolLag = mSampleEqualizer.getEqualizedSymbol(mBuffer[lagIntegral], mBuffer[lagIntegral + 1], lag - lagIntegral);

                    //Gardner symbol timing error detector using the half-symbol (lagging) sample between the previous and
                    //current symbols.  A positive error means the symbol is sampled late, so the next sample is advanced.
                    double timingAdjustment = 0.0;

                    if(mSynchronized && getTimingTrackingGain() > 0)
                    {
                        float timingError = softSymbolLag * (mPreviousTimingSymbol - timingSymbol);
                        timingError = Math.clamp(timingError, -TIMING_ERROR_CLIP, TIMING_ERROR_CLIP);
                        timingAdjustment = getTimingTrackingGain() * samplesPerSymbol * timingError;
                    }

                    mPreviousTimingSymbol = timingSymbol;
                    float scoreLag = mSyncDetectorLagging.process(softSymbolLag);
                    scorePrimary = mSyncDetector.process(softSymbol);

                    if(mDebugLogDetectionScores)
                    {
                        boolean exceeds = scorePrimary > 45 || scoreLag > 45;
                        System.out.println("DETECT - Primary [" + DEBUG_DF.format(scorePrimary) + "] Lag [" + DEBUG_DF.format(scoreLag) + "] at [" + mDebugSymbolCounter + "]" + (exceeds ? " <<<<<<<<<<<<<<<<<<<<<<<<<<" : ""));
                    }

                    correctionCandidate = INVALID_SYNC_DETECTION;

                    if(isSyncDetectionAllowed(symbolsSinceLastSync))
                    {
                        if(scorePrimary > getSyncThresholdDetected() && scorePrimary > scoreLag)
                        {
                            mActiveSyncDetector = mSyncDetector;
                            correctionCandidate = optimize(0, scorePrimary, (bufferPointer + samplePoint));
                        }
                        else if(scoreLag > getSyncThresholdDetected() && symbolsSinceLastSync > 1)
                        {
                            mActiveSyncDetector = mSyncDetectorLagging;
                            correctionCandidate = optimize(-mLaggingSyncOffset, scoreLag, (bufferPointer + samplePoint));
                        }
                    }

                    //Equalizer processes current soft symbol and returns a delayed soft symbol that is equalized
                    softSymbol = mSymbolEqualizer.process(softSymbolLag, softSymbol);

                    //Broadcast the decoded soft symbol to an optionally registered listener (ie Symbol view in channel panel).
                    Dibit symbol = Dibit.fromSample(softSymbol);
                    mFeedbackDecoder.broadcast(softSymbol);

                    if(mDebugVisualizeSync)
                    {
                        getDebugSyncResultsViewer().symbol(softSymbol);
                    }

                    //When the message framer emits at least one valid message (ie CRC check passes) and we have at least
                    // 2 consecutive sync detections, send the current sample equalizer's balance value as a pseudo-PLL
                    // error measurement so the tuner can self adjust to correct PPM.  The balance is applied in the
                    // equalized symbol domain, so divide by gain to convert to radians per sample for the decoder.
                    if(mMessageFramer.process(symbol, softSymbol) && mConsecutiveSyncPeriods > 1)
                    {
                        mFeedbackDecoder.processPLLError(mSampleEqualizer.balanceOptimized() / mSampleEqualizer.gain());
                    }

                    mDibitAssembler.receive(symbol);

                    //Do a final validation against the reference sync waveform for the first two sync detections
                    //following a sync loss.
                    if(correctionCandidate.isValid() && mConsecutiveSyncPeriods < 3)
                    {
                        correctionCandidate = validateSyncWaveform(correctionCandidate, bufferPointer, samplePoint);
                    }

                    if(correctionCandidate.isValid())
                    {
                        mConsecutiveSyncPeriods = Math.min(3, mConsecutiveSyncPeriods + 1);

                        if(mDebugLogDetectionScores)
                        {
                            System.out.println("DETECT - Primary [" + DEBUG_DF.format(scorePrimary) + "] Lag [" + DEBUG_DF.format(scoreLag) + "] Optimized [" + correctionCandidate.mOptimizationScore + "] at samples [" + mDebugSampleCounter + "] symbols [" + mDebugSymbolCounter + "]");
                        }

                        mSampleEqualizer.apply(correctionCandidate);
                        syncDetected(mActiveSyncDetector);

                        if(mSynchronized)
                        {
                            samplePoint += (correctionCandidate.getTimingCorrection() * SYMBOL_TIMING_GAIN);
                        }
                        else
                        {
                            samplePoint += correctionCandidate.getTimingCorrection();
                        }
                        //Notify the framer once the final sync symbol emerges from the equalizer delay line.  The
                        //countdown only runs on subsequent symbols, so with zero delay (the current symbol is the final
                        //sync symbol and the framer already received it) notify immediately.
                        mDelayedSyncNotificationSymbolsRemaining = mSymbolEqualizer.getDelaySymbolCount();

                        if(mDelayedSyncNotificationSymbolsRemaining <= 0)
                        {
                            mMessageFramer.syncDetected();
                            mDelayedSyncNotificationPending = false;
                        }
                        else
                        {
                            mDelayedSyncNotificationPending = true;
                        }
                        mSynchronized = true;
                        symbolsSinceLastSync = 0;

                        //If the equalizer was disabled, it has bad samples contaminating the training buffer, and we
                        // need to prime that buffer with aligned samples prior to sync detection training.
                        if(!mSymbolEqualizer.isEnabled())
                        {
                            int symbols = mSyncDetector.getDibitLength() + mSymbolEqualizer.getLength() - mSymbolEqualizer.getDelaySymbolCount();
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

                        mSymbolEqualizer.syncDetected(mActiveSyncDetector.getPatternSymbols());
                        mSymbolEqualizer.enable();
                        visualizeSyncDetect(scorePrimary, true, "DETECT - POST", bufferPointer, samplePoint);
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

                    //Add another symbol's worth of samples to the counter for processing, including the continuous timing
                    //adjustment unless a sync detection just corrected the timing for this symbol.
                    if(correctionCandidate.isValid())
                    {
                        timingAdjustment = 0.0;
                    }

                    samplePoint += samplesPerSymbol + timingAdjustment;
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
     * waveform.  If the detected sync correlates well against the reference sync then the detected sync and candidate
     * correction is valid. Otherwise, this method returns the static invalid sync correction to signal that the sync
     * detection was invalid.
     * @param candidate to evaluate
     * @param bufferPointer for the final (optimized) symbol in the buffer
     * @param samplePoint offset from the buffer pointer to the final symbol
     * @return the original correction or the static invalid sync correction
     */
    private Correction validateSyncWaveform(Correction candidate, int bufferPointer, double samplePoint)
    {
        ReferenceSyncWaveform referenceSyncWaveform = mActiveSyncDetector.getReferenceWaveform(mSamplesPerSymbol);
        float gain = mSampleEqualizer.mGain + candidate.getGainAdjustment();
        float balance = mSampleEqualizer.mBalance + candidate.getBalanceAdjustment();
        double offset = bufferPointer + samplePoint + candidate.getTimingCorrection() - referenceSyncWaveform.samples().length;
        int integral = (int)Math.ceil(offset);
        double mu = integral - offset;
        double accumulator = 0.0;
        int index;

        //Evaluate the reference waveform in reverse order since the final symbol is aligned to the final sample
        for(int x = referenceSyncWaveform.samples().length - 1; x >= 0; x--)
        {
            index = integral + x;
            float sample = LinearInterpolator.calculate(mBuffer[index], mBuffer[index + 1], mu);
            sample *= gain;
            sample += balance;
            sample = Math.clamp(sample, -referenceSyncWaveform.maxSample(), referenceSyncWaveform.maxSample());
            accumulator += referenceSyncWaveform.samples()[x] * sample;
        }

        double threshold = referenceSyncWaveform.idealCorrelationScore() * 0.85; //85% of the ideal correlation score

        //Return the correction as valid if the correlation score exceeds the threshold, otherwise return the static
        // invalid sync detection to nullify the correction.
        if(accumulator > threshold)
        {
            return candidate;
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
        mNoiseStandardDeviationThreshold = .25;
        mOptimizeFineIncrement = samplesPerSymbol / 200.0f;
        mLaggingSyncOffset = samplesPerSymbol / 2.0f;
        //Make the sync workspace long enough to hold the detected sync waveform +/- one symbol period for timing optimization
        mSyncWorkspaceLength = (int)Math.ceil((mSyncDetector.getDibitLength() + 1) * samplesPerSymbol);
        mBufferReloadLength = (int)Math.ceil(samplesPerSymbol);
        mSampleEqualizer.configure(samplesPerSymbol);
        //Pre-generate the reference sync waveform for the primary sync pattern
        mSyncDetector.getReferenceWaveform(samplesPerSymbol);
//        System.out.println("Samples Per Symbol: " + samplesPerSymbol);
    }

    /**
     * Debug viewer UI for visualizing sync detections in the sample stream/waveform.
     */
    private ISyncResultsListener getDebugSyncResultsViewer()
    {
        if(mDebugSyncResultsViewer == null)
        {
            mDebugSyncResultsViewer = new FMSyncResultsViewer();
        }

        return mDebugSyncResultsViewer;
    }

    /**
     * Debug method to visualize the contents of the sync detection for the samples and symbols and symbol timing.
     * @param score from the sync detector
     * @param primary sync detector (true) or secondary (false)
     */
    public void visualizeSyncDetect(float score, boolean primary, String tag, int bufferPointer, double samplePoint)
    {
        if(!mDebugVisualizeSync || mDebugSymbolCounter < mDebugSymbolStart)
        {
            return;
        }

        int integral = (int)Math.floor(samplePoint);
        samplePoint -= integral;
        bufferPointer += integral;

        int offset = 3;
        int length = (int)Math.ceil(mSamplesPerSymbol * (mSyncDetector.getDibitLength() - 1)) + (2 * offset);
        int end = bufferPointer + offset;
        int start = end - length;
        float[] symbols = new float[mSyncDetector.getDibitLength()];
        float[] samples = Arrays.copyOfRange(mBuffer, start, end);
        for(int i = 0; i < samples.length; i++)
        {
            samples[i] = mSampleEqualizer.equalize(samples[i]);
        }

        double symbolPointer = bufferPointer + samplePoint - (mSamplesPerSymbol * (mSyncDetector.getDibitLength() - 1));
        int symbolIntegral = (int)Math.floor(symbolPointer);
        double mu = symbolPointer - symbolIntegral;

        for(int x = 0; x < mSyncDetector.getDibitLength(); x++)
        {
            symbols[x] = mSampleEqualizer.getEqualizedSymbol(mBuffer[symbolIntegral], mBuffer[symbolIntegral + 1], mu);
            symbolPointer += mSamplesPerSymbol;
            symbolIntegral = (int)Math.floor(symbolPointer);
            mu = symbolPointer - symbolIntegral;
        }

        float[] syncIntervals = new float[mSyncDetector.getDibitLength()];
        int adjust = bufferPointer - length + offset;
        double pointer = bufferPointer + samplePoint - adjust;

        for(int x = (mSyncDetector.getDibitLength() - 1); x >= 0; x--)
        {
            syncIntervals[x] = (float)pointer;
            pointer -= mSamplesPerSymbol;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        getDebugSyncResultsViewer().receive(symbols, mSyncDetector.getPatternSymbols(), samples, syncIntervals, mSampleEqualizer.mBalance, mSampleEqualizer.mGain,
                tag + " Score: " + score + (primary ? " PRIMARY " : " SECONDARY ") + (mSynchronized ? "FINE " : "COARSE ") +
                        " EQ-BO:" + DEBUG_DF.format(mSampleEqualizer.mBalanceOptimized) +
                        " EQ-B:" + DEBUG_DF.format(mSampleEqualizer.mBalance) +
                        " EQ-G:" + DEBUG_DF.format(mSampleEqualizer.mGain) + " " +
                        " SAM:" + mDebugSampleCounter +
                        " SYM:" + mDebugSymbolCounter +
                        " DUR:" + DEBUG_DF.format(mDebugSymbolCounter / 2400.0),
                countDownLatch);

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
    public void visualizeSymbols(int bufferPointer, double samplePoint, int symbolsSinceLastSync)
    {
        if(!mDebugVisualizeSymbols || mDebugSymbolCounter < mDebugSymbolStart)
        {
            return;
        }

        int syncLength = mSyncDetector.getDibitLength();

        int offset = 3;
        int length = (int)Math.ceil(mSamplesPerSymbol * (syncLength - 1)) + (2 * offset);
        int end = bufferPointer + offset;
        int start = end - length;
        float[] symbols = new float[syncLength];
        float[] decisions = new float[syncLength];
        float[] samples = Arrays.copyOfRange(mBuffer, start, end);
        for(int i = 0; i < samples.length; i++)
        {
            samples[i] = mSampleEqualizer.equalize(samples[i]);
        }

        double symbolPointer = bufferPointer + samplePoint - (mSamplesPerSymbol * (syncLength - 1));
        int symbolIntegral = (int)Math.floor(symbolPointer);
        double mu = symbolPointer - symbolIntegral;

        for(int x = 0; x < syncLength; x++)
        {
            symbols[x] = mSampleEqualizer.getEqualizedSymbol(mBuffer[symbolIntegral], mBuffer[symbolIntegral + 1], mu);
            decisions[x] = Dibit.fromSample(symbols[x]).getIdealPhase();
            symbolPointer += mSamplesPerSymbol;
            symbolIntegral = (int)Math.floor(symbolPointer);
            mu = symbolPointer - symbolIntegral;
        }

        float[] syncIntervals = new float[syncLength];
        int adjust = bufferPointer - length + offset;
        double pointer = bufferPointer + samplePoint - adjust;

        for(int x = syncLength - 1; x >= 0; x--)
        {
            syncIntervals[x] = (float)pointer;
            pointer -= mSamplesPerSymbol;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        getDebugSyncResultsViewer().receive(symbols, decisions, samples, syncIntervals, mSampleEqualizer.mBalance, mSampleEqualizer.mGain,
                "Balance:" + mSampleEqualizer.mBalance + " Gain:" + mSampleEqualizer.mGain +
                        " SAM:" + mDebugSampleCounter + " SYM:" + mDebugSymbolCounter +
                        " DUR:" + DEBUG_DF.format(mDebugSymbolCounter / 2400.0) +
                        " ELAPSED:" + symbolsSinceLastSync, countDownLatch);

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
     * Calculates the standard deviation of the samples at the specified offset that aligns with a sync detection.
     * @param offset to the sample representing the final symbol in the detected sync pattern.
     * @return standard deviation.
     */
    public double getStandardDeviation(double offset)
    {
        StandardDeviation standardDeviation = new StandardDeviation();
        int start = (int)Math.floor(offset - ((mSyncDetector.getDibitLength() - 1) * mSamplesPerSymbol));
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
    public class Correction
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
            sb.append("] Adjustments-Timing [").append(DEBUG_DF.format(mTimingAdjustment));
            sb.append("] Balance [").append(DEBUG_DF.format(mBalanceAdjustment));
            sb.append("] Gain [").append(DEBUG_DF.format(mGainAdjustment));
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
            return mOptimizationScore > getSyncThresholdEqualized();
        }
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

        //Absolute balance (equalized symbol domain) that cancels the DC offset measured across the sync pattern
        float measuredBalance = -getBalance(offset) * mSampleEqualizer.mGain;

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

        float scoreCenter = score(candidate, samplesPerSymbol, measuredBalance);

        candidate = offset - stepSize;
        float scoreLeft = score(candidate, samplesPerSymbol, measuredBalance);

        candidate = offset + stepSize;
        float scoreRight = score(candidate, samplesPerSymbol, measuredBalance);

        while(stepSize > stepSizeMin && Math.abs(adjustment) <= adjustmentMax)
        {
            if(scoreLeft > scoreRight && scoreLeft > scoreCenter)
            {
                adjustment -= stepSize;
                scoreRight = scoreCenter;
                scoreCenter = scoreLeft;

                candidate = offset + adjustment - stepSize;
                scoreLeft = score(candidate, samplesPerSymbol, measuredBalance);
            }
            else if(scoreRight > scoreLeft && scoreRight > scoreCenter)
            {
                adjustment += stepSize;
                scoreLeft = scoreCenter;
                scoreCenter = scoreRight;

                candidate = offset + adjustment + stepSize;
                scoreRight = score(candidate, samplesPerSymbol, measuredBalance);
            }
            else
            {
                stepSize *= 0.5f;

                if(stepSize > stepSizeMin)
                {
                    candidate = offset + adjustment - stepSize;
                    scoreLeft = score(candidate, samplesPerSymbol, measuredBalance);

                    candidate = offset + adjustment + stepSize;
                    scoreRight = score(candidate, samplesPerSymbol, measuredBalance);
                }
            }
        }

        //If we didn't find an optimal correlation score above the threshold, return a false sync.
        if(scoreCenter < getSyncThresholdOptimized())
        {
            return INVALID_SYNC_DETECTION;
        }

        return getCorrection(additionalOffset, adjustment, detectionScore, scoreCenter, bufferOffset,
                measuredBalance);
    }

    /**
     * Sample equalizer providing gain and balance (DC offset compensation).
     *
     * Note: equalizer is initialized with a nominal gain derived from the samples per symbol and the protocol's outer
     * symbol deviation and symbol rate (see getNominalGainScalar()), and is constrained to +/- 10% of that nominal
     * value.  Gain value is adjusted and optimized with each sync detection.
     */
    public class SampleEqualizer
    {
        private static final float EQUALIZER_LOOP_GAIN = 0.05f;
        //Balance values are in the equalized symbol domain (radians per symbol, same scale as the ideal symbol phases).
        //PI/2 equates to +/- symbolRate / (4 * nominal gain scalar) Hz: P25 1200 Hz, NXDN48 700 Hz, NXDN96 1600 Hz
        private static final float EQUALIZER_MAXIMUM_BALANCE = (float)(Math.PI / 2.0);
        private static final float EQUALIZER_RECALIBRATE_THRESHOLD = (float)(Math.PI / 8.0);
        private static final float DYNAMIC_BALANCE_ERROR_THRESHOLD = (float)(Math.PI / 8.0);
        private static final float DYNAMIC_BALANCE_SWING = (float)(Math.PI / 12.0);
        private static final float DYNAMIC_BALANCE_GAIN = 0.25f;
        private float mBalanceOptimized;
        private float mBalanceMinimum = -DYNAMIC_BALANCE_ERROR_THRESHOLD;
        private float mBalanceMaximum = DYNAMIC_BALANCE_ERROR_THRESHOLD;
        private float mBalance = 0.0f;
        private float mGain = 1.0f;
        private float mGainInitial = 1.0f;
        private float mGainMax = 1.0f;
        private float mGainMin = 1.0f;
        private final boolean mDynamicBalanceTracking;

        /**
         * Constructs an instance.  Note: gain values are established when configure() is invoked with the
         * samples per symbol.
         * @param dynamicBalanceTracking to enable dynamic balance tracking
         */
        public SampleEqualizer(boolean dynamicBalanceTracking)
        {
            mDynamicBalanceTracking = dynamicBalanceTracking;
        }

        /**
         * Applies decision directed correction to the constellation balance when dynamic balance tracking is enabled
         * and the processor is synchronized following a valid sync detection.  In periods of high noise or when the
         * signal has stopped but we haven't yet detected it, we apply a correction that gradually restores the balance
         * back towards the optimized balance decided at the last valid sync detection.  This ensures that when the
         * signal returns, the balance can be reestablished quickly.
         * @param symbol sliced at the correct timing
         */
        public float correct(float symbol)
        {
            if(mDynamicBalanceTracking && mSynchronized)
            {
                Dibit dibit = Dibit.fromSample(symbol);
                float adjustment = (dibit.getIdealPhase() - symbol);

                if(Math.abs(adjustment) < DYNAMIC_BALANCE_ERROR_THRESHOLD)
                {
                    adjustment *= DYNAMIC_BALANCE_GAIN;
                    mBalance += adjustment;
                    mBalance = Math.clamp(mBalance, mBalanceMinimum, mBalanceMaximum);
                    return symbol + adjustment;
                }
                else
                {
                    //If symbols are too noisy, gradually restore balance to optimized level
                    mBalance += (mBalanceOptimized - mBalance) * DYNAMIC_BALANCE_GAIN;
                }
            }

            return symbol;
        }

        /**
         * Applies equalizer correction settings.
         *
         * @param correction settings to apply
         */
        public void apply(Correction correction)
        {
            float balanceCorrection = correction.getBalanceAdjustment();

//            System.out.println("Correction [" + DEBUG_DF.format(balanceCorrection) +
//                    "] Balance [" + DEBUG_DF.format(mBalance) +
//                    "] Applying [" + DEBUG_DF.format(balanceCorrection * EQUALIZER_LOOP_GAIN) +
//                    "] Synchronized [" + mSynchronized + "] " + correction);

            //Re-initialize the equalizer any time the balance correction value exceeds the threshold.
            if(mSynchronized && Math.abs(balanceCorrection) > EQUALIZER_RECALIBRATE_THRESHOLD)
            {
                reset();
            }

            //Limit equalizer adjustments using a control loop at each sync detect after the initial equalizer setup or
            //apply as new settings on initial sync detect or any time an excess balance correction is supplied.
            if(mSynchronized)
            {
                mBalanceOptimized += (mBalance - mBalanceOptimized + balanceCorrection) * EQUALIZER_LOOP_GAIN;
//                mBalanceOptimized = mBalance + (balanceCorrection * EQUALIZER_LOOP_GAIN);
                mGain += (correction.getGainAdjustment() * EQUALIZER_LOOP_GAIN);
            }
            else
            {
                mBalanceOptimized = mBalance + balanceCorrection;
                mGain += correction.getGainAdjustment();
            }

            mBalanceOptimized = Math.clamp(mBalanceOptimized, -EQUALIZER_MAXIMUM_BALANCE, EQUALIZER_MAXIMUM_BALANCE);
            mBalanceMinimum = mBalanceOptimized - DYNAMIC_BALANCE_SWING;
            mBalanceMaximum = mBalanceOptimized + DYNAMIC_BALANCE_SWING;
            mBalance = mBalanceOptimized;
            mGain = Math.clamp(mGain, mGainMin, mGainMax);
        }

        /**
         * Current balance that represents a traditional PLL signal offset value.
         * @return balance.
         */
        public float balance()
        {
            return mBalance;
        }

        /**
         * Balance as optimized at the last sync detection
         * @return balance optimized
         */
        public float balanceOptimized()
        {
            return mBalanceOptimized;
        }

        /**
         * Current gain
         */
        public float gain()
        {
            return mGain;
        }

        /**
         * Updates the initial, min and max gain values from the samples per symbol and the protocol's nominal
         * gain scalar.
         * @param samplesPerSymbol for the incoming stream
         */
        public void configure(double samplesPerSymbol)
        {
            double nominalGain = samplesPerSymbol * getNominalGainScalar();
            mGainInitial = (float)nominalGain;
            mGain = mGainInitial;

            //Allow the gain to float up/down by a fraction of the nominal gain
            float tolerance = (float)(nominalGain * SAMPLE_GAIN_TOLERANCE);
            mGainMax = mGain + tolerance;
            mGainMin = mGain - tolerance;
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
            //Calculate interpolated sample and pass through the dynamic balance correction method.
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

    }

    /**
     * Calculates the sync correlation score at the specified offset and samples per symbol interval using the
     * current equalizer gain and supplied balance value.
     *
     * @param offset to the final symbol in the soft symbol buffer
     * @param samplesPerSymbol spacing to test for.     * @param balance for the calculation
     * @return correlation score.
     */
    public float score(double offset, double samplesPerSymbol, float balance)
    {
        return score(offset, samplesPerSymbol, balance, mSampleEqualizer.mGain);
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
        float[] patternSymbols = mActiveSyncDetector.getPatternSymbols();

        double pointer = offset - (samplesPerSymbol * (patternSymbols.length - 1));
        int bufferPointer = (int)Math.floor(pointer);
        double fractional = pointer - bufferPointer;

        float score = 0;
        float sample1, sample2;
        float idealSymbol;

        for(int x = 0; x < patternSymbols.length; x++)
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

            idealSymbol = patternSymbols[x];

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
     * Converts the magnitude error measured across the sync pattern symbols into an (additive) gain adjustment for the
     * sample equalizer.  The soft symbols were measured with the current gain applied, so the gain that maps them to
     * the ideal symbol magnitudes is currentGain * idealMagnitudeSum / softMagnitudeSum.
     *
     * @param idealMagnitudeSum sum of the absolute ideal symbol values across the sync pattern
     * @param magnitudeErrorSum sum of (|ideal symbol| - |soft symbol|) across the sync pattern
     * @return gain adjustment to add to the current gain value, or zero if the soft symbol magnitudes are degenerate.
     */
    protected float getGainAdjustment(float idealMagnitudeSum, float magnitudeErrorSum)
    {
        float softMagnitudeSum = idealMagnitudeSum - magnitudeErrorSum;

        if(softMagnitudeSum <= 0 || idealMagnitudeSum <= 0)
        {
            return 0.0f;
        }

        return mSampleEqualizer.mGain * (idealMagnitudeSum / softMagnitudeSum - 1.0f);
    }

    /**
     * Calculates the mean of sync pattern detection for the samples.
     * @param offset for the sync detection which is just after the sample of the last symbol in the sync pattern
     * @return mean for the samples in this range
     */
    public float getBalance(double offset)
    {
        Mean mean = new Mean();
        int start = (int)Math.floor(offset - ((mSyncDetector.getDibitLength() - 1) * mSamplesPerSymbol));
        int end = (int)Math.ceil(offset);

        for(int i = start; i <= end; i++)
        {
            mean.increment(mBuffer[i]);
        }

        return (float)mean.getResult();
    }

    /**
     * Calculates a correction update for the equalizer balance and gain when the sync pattern is detected in the
     * sample buffer, using the supplied timing correction argument.  Resamples the sync symbols and compares each
     * soft symbol to the ideal symbol phase to develop average error measurements for balance and gain.  On initial
     * sync detection, the equalizer settings are applied to the samples in the buffer allowing the symbols to be
     * resampled during coarse sync acquisition.
     */
    public Correction getCorrection(double additionalOffset, double timingCorrection,
                                    float detectionScore, float optimizationScore, double offset, float measuredBalance)
    {
        float[] patternSymbols = mActiveSyncDetector.getPatternSymbols();
        Dibit[] patternDibits = mActiveSyncDetector.getPatternDibits();
        double resampleStart = offset + additionalOffset + timingCorrection - (mSamplesPerSymbol * (patternSymbols.length - 1));
        int resampleStartIntegral = (int)Math.floor(resampleStart);
        float symbol, gainAccumulator = 0, idealMagnitudeAccumulator = 0, resampledSoftSymbol;
        Dibit resampledDibit;
        int bitErrorCount = 0;

        for(int x = 0; x < patternSymbols.length; x++)
        {
            if(resampleStartIntegral >= 0)
            {
                symbol = patternSymbols[x];
                resampledSoftSymbol = LinearInterpolator.calculate(mBuffer[resampleStartIntegral],
                        mBuffer[resampleStartIntegral + 1], resampleStart - resampleStartIntegral);
                resampledSoftSymbol = resampledSoftSymbol * mSampleEqualizer.mGain + measuredBalance;
                resampledDibit = Dibit.fromSample(resampledSoftSymbol);
                bitErrorCount += patternDibits[x].getBitErrorFrom(resampledDibit);
                gainAccumulator += Math.abs(symbol) - Math.abs(resampledSoftSymbol);
                idealMagnitudeAccumulator += Math.abs(symbol);
            }

            resampleStart += mSamplesPerSymbol;
            resampleStartIntegral = (int)Math.floor(resampleStart);
        }

        //Convert the accumulated magnitude error into a gain adjustment
        gainAccumulator = getGainAdjustment(idealMagnitudeAccumulator, gainAccumulator);

        //Recalculate the optimization score using the adjusted equalizer settings
        if(!mSynchronized)
        {
            double optimizedFinalSymbolOffset = offset + additionalOffset + timingCorrection;
            float adjustedGain = mSampleEqualizer.mGain + gainAccumulator;
            measuredBalance = -getBalance(optimizedFinalSymbolOffset) * adjustedGain;
            optimizationScore = score(optimizedFinalSymbolOffset, mSamplesPerSymbol, measuredBalance, adjustedGain);
        }

        //Correction carries the balance as an adjustment relative to the current equalizer balance
        float balanceAdjustment = measuredBalance - mSampleEqualizer.mBalance;

        return new Correction(additionalOffset, timingCorrection, balanceAdjustment, gainAccumulator, detectionScore,
                optimizationScore, bitErrorCount);
    }
}
