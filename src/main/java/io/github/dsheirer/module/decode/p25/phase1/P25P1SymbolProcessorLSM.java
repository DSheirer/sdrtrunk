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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.c4fm.C4FMMessageFramer;
import io.github.dsheirer.module.decode.FeedbackDecoder;

/**
 * P25 Phase 1 LSM implementation of the symbol processor.  Processes the differentially demodulated sample stream
 * produced by the DQPSKDifferentialDemodulator, which is in the same radians per sample domain as the C4FM decoder's
 * sample stream, so this processor shares the P25 C4FM sync detection, optimization and tuning parameters.
 *
 * Note: testing against LSM recordings without injected noise showed no meaningful difference across equalizer spans
 * of 1-11 symbols, with dynamic balance tracking disabled, or with the noise gate disabled.  With injected noise, a
 * single symbol equalizer span performed slightly better and dynamic balance tracking performed better.  Disabling the
 * continuous timing tracking significantly reduced valid messages on a simulcast recording.
 *
 * The demodulated sample stream is phase wrapped, so this processor interpolates across the phase wrap (see
 * interpolate()).  Measuring the sync correction balance error and the noise detection sample differences across the
 * phase wrap made no measurable difference, so those use the C4FM implementations.
 */
public class P25P1SymbolProcessorLSM extends P25P1SymbolProcessorC4FM
{
    //Sync detection thresholds and the equalizer span were tuned with noise injection testing against LSM recordings at
    //0-12 dB carrier to noise (50 kHz bandwidth).  Relative to the C4FM values (80/100/100, 85% validation, 7 symbol
    //equalizer), these increased valid messages 52% at 0 dB with 0.2% fewer valid messages without injected noise, and
    //no false message detections against noise.  The reference sync waveform validation threshold is the largest
    //contributor since sync is frequently lost and reacquired at low SNR (see below).
    private static final float SYNC_THRESHOLD_DETECTED = 70;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 85;
    private static final float SYNC_THRESHOLD_EQUALIZED = 85;
    //Reference sync waveform validation threshold for the first two sync detections after a sync loss.  At low SNR,
    //sync is frequently lost and reacquired, and true sync detections score 0.6 (median) at 6-9 dB carrier to noise
    //(in-band), so a higher threshold rejects most reacquisitions: 0.75 rejected 17% of true syncs at 9 dB and 44% at
    //6 dB.  False sync detections are rejected by the optimization score threshold before validation.  0.45 increased
    //valid messages 9% at 9 dB and 134% at 6 dB without false message detections against noise, while 0.3 produced a
    //false NID decode against noise.
    private static final double SYNC_WAVEFORM_VALIDATION_THRESHOLD = 0.45;
    private static final int EQUALIZER_SYMBOL_LENGTH = 1;
    //Phase wrap period of the demodulated sample stream: 2 * PI radians per symbol, in radians per sample
    private float mWrapPeriod = (float)(2.0 * Math.PI);

    /**
     * Constructs an instance
     *
     * @param feedbackDecoder parent decoder
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public P25P1SymbolProcessorLSM(FeedbackDecoder feedbackDecoder, C4FMMessageFramer messageFramer)
    {
        super(feedbackDecoder, messageFramer);
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

    @Override
    public int getEqualizerSymbolLength()
    {
        return EQUALIZER_SYMBOL_LENGTH;
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

    /**
     * Indicates if the symbol processor is synchronized to the signal (ie sync detected within the max message length)
     */
    public boolean isSynchronized()
    {
        return mSynchronized;
    }
}
