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

package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.dsp.c4fm.C4FMMessageFramer;
import io.github.dsheirer.dsp.c4fm.C4FMSymbolProcessor;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.p25.phase2.sync.P25P2SoftSyncDetectorFactory;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 * P25 Phase 2 HDQPSK implementation of the C4FM symbol processor.  Processes the differentially demodulated sample
 * stream produced by the differential demodulator, which is in the same radians per sample domain as the C4FM
 * decoder's sample stream.
 *
 * The outbound (FDMA) super frame fragment (720 dibits) contains two S-ISCH sync patterns separated by 180 dibits, so
 * the sync interval alternates between 180 and 540 dibits.  The 20-symbol sync pattern has an equal quantity of +3 and
 * -3 symbols, which supports the default (mean) balance measurement approach of the parent class.
 *
 * The demodulated sample stream is phase wrapped, so this processor interpolates across the phase wrap (see
 * interpolate()).
 */
public class P25P2SymbolProcessor extends C4FMSymbolProcessor
{
    private static final int SYMBOL_RATE = 6000;
    //Equivalent outer symbol deviation for the differentially demodulated (+/- 3PI/4 radians per symbol) stream, so that
    //the nominal gain scalar is 1.0 (ie 3/8 x 6000 baud)
    private static final double OUTER_SYMBOL_DEVIATION = 2250.0; //Hertz
    //Sync patterns occur every 180 dibits (S-ISCH 1 to S-ISCH 2) or 540 dibits (S-ISCH 2 to S-ISCH 1)
    private static final int SYNC_INTERVAL = 180;
    //Allow one missed sync pattern (ie a full 720 dibit fragment) before declaring sync loss
    private static final int MAX_SYNC_INTERVAL = 720 + 2;
    //Tuning: the following values were tested against Phase 2 recordings with injected noise (valid MAC messages at
    //none/12/9/6 dB in-band C/N, two noise seeds).  Run to run variation between noise seeds is ~5% at 9 dB, which
    //exceeds the variation across the tested values unless noted.
    //Once synchronized, sync detections are only allowed within this many symbols of a sync interval boundary.
    //Tested 1-5 symbols: no difference.
    private static final int SYNC_WINDOW_SYMBOLS = 2;
    //Tested 160-1200 symbols and 1-10 frames: no difference with a single symbol equalizer span.
    private static final int EQUALIZER_TRAINING_PERIOD_SYMBOL_COUNT = 600;
    private static final int EQUALIZER_SKIP_TRAINING_FRAME_COUNT = 5;
    //Longer equalizer spans reduce valid messages at 9 dB: 3 symbols (-1.4%), 5 symbols (-4.2%), 7 symbols (-6.5%)
    private static final int EQUALIZER_SYMBOL_LENGTH = 1;
    //Disabling the continuous timing tracking (0) reduced valid messages 13% at 9 dB.  Tested 0.001-0.008.
    private static final double TIMING_TRACKING_GAIN = 0.002;
    //Sync thresholds scaled from the P25 Phase 1 LSM values for the shorter (20 vs 24 symbol) sync pattern.  Tested
    //detection 45-80 and optimized/equalized 60-100: lower optimized/equalized thresholds (60-65) reduced valid
    //messages, 100 reduced sync acquisition at 6 dB, and the remaining values were within the run to run variation.
    //The sync waveform validation threshold 0.75 reduced valid messages 36% at 9 dB, and 0.3-0.6 were within the run
    //to run variation.
    private static final float SYNC_THRESHOLD_DETECTED = 58;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 71;
    private static final float SYNC_THRESHOLD_EQUALIZED = 71;
    private static final double SYNC_WAVEFORM_VALIDATION_THRESHOLD = 0.45;
    //Phase wrap period of the demodulated sample stream: 2 * PI radians per symbol, in radians per sample
    private float mWrapPeriod = (float)(2.0 * Math.PI);

    /**
     * Constructs an instance
     *
     * @param feedbackDecoder parent decoder
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public P25P2SymbolProcessor(FeedbackDecoder feedbackDecoder, C4FMMessageFramer messageFramer)
    {
        super(feedbackDecoder, messageFramer, P25P2SoftSyncDetectorFactory.getDetector(),
                P25P2SoftSyncDetectorFactory.getDetector());
    }

    @Override
    public void setSamplesPerSymbol(float samplesPerSymbol)
    {
        mWrapPeriod = (float)(2.0 * Math.PI / samplesPerSymbol);
        super.setSamplesPerSymbol(samplesPerSymbol);
    }

    /**
     * Interpolates across the phase wrap.  The differentially demodulated sample stream is wrapped to +/- PI radians
     * per symbol and the outer symbols (+/- 3PI/4) are only PI/4 from the wrap, so consecutive samples near an outer
     * symbol can straddle the wrap (ie +3.0 and -3.0) where linear interpolation produces a value near zero.  This
     * unwraps the second sample relative to the first, interpolates, and wraps the result to +/- PI radians per symbol.
     */
    @Override
    protected float interpolate(float sample1, float sample2, double mu)
    {
        float wrap = mWrapPeriod;
        float delta = sample2 - sample1;
        delta -= wrap * Math.round(delta / wrap);
        float interpolated = (float)(sample1 + delta * mu);
        return interpolated - wrap * Math.round(interpolated / wrap);
    }

    /**
     * Calculates the standard deviation of the sample to sample differences across the phase wrap, for the noise
     * detection at each sync detection.  Each sample difference that straddles the phase wrap (ie +3.0 to -3.0) is
     * wrapped to +/- PI radians per symbol.  Testing against PCWIN recordings: the unwrapped differences (C4FM
     * implementation) exceed the noise threshold for most of the valid sync detections, which prevented
     * synchronization on 2 of 6 recordings.
     * @param offset to the sample representing the final symbol in the detected sync pattern.
     * @return standard deviation.
     */
    @Override
    public double getStandardDeviation(double offset)
    {
        StandardDeviation standardDeviation = new StandardDeviation();
        float wrap = mWrapPeriod;
        int start = (int)Math.floor(offset - ((mSyncDetector.getDibitLength() - 1) * mSamplesPerSymbol));
        int end = (int)Math.ceil(offset);
        end = Math.min(end, mBuffer.length - 1);
        float delta;

        for(int i = start; i < end; i++)
        {
            delta = mBuffer[i] - mBuffer[i + 1];
            delta -= wrap * Math.round(delta / wrap);
            standardDeviation.increment(delta);
        }

        return standardDeviation.getResult();
    }

    /**
     * Once synchronized with at least two consecutive sync detections, only allows sync detections within a small
     * window around each expected sync interval boundary to prevent false sync detections from timeslot payload.
     * @param symbolsSinceLastSync count of symbols since the final symbol of the previous sync detection.
     * @return true if a sync detection is allowed.
     */
    @Override
    protected boolean isSyncDetectionAllowed(int symbolsSinceLastSync)
    {
        if(!mSynchronized || getConsecutiveSyncPeriods() < 2)
        {
            return true;
        }

        int offset = symbolsSinceLastSync % SYNC_INTERVAL;
        return Math.min(offset, SYNC_INTERVAL - offset) <= SYNC_WINDOW_SYMBOLS;
    }

    /**
     * Indicates if the symbol processor is synchronized to the signal (ie sync detected within the max sync interval)
     */
    public boolean isSynchronized()
    {
        return mSynchronized;
    }

    @Override
    public boolean isDynamicBalanceTrackingRequired()
    {
        return true;
    }

    @Override
    public int getMaxMessageDibitLength()
    {
        return MAX_SYNC_INTERVAL;
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
    public int getEqualizerSymbolLength()
    {
        return EQUALIZER_SYMBOL_LENGTH;
    }

    @Override
    public double getTimingTrackingGain()
    {
        return TIMING_TRACKING_GAIN;
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

    @Override
    public double getSyncWaveformValidationThreshold()
    {
        return SYNC_WAVEFORM_VALIDATION_THRESHOLD;
    }
}
