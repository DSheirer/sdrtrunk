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

package io.github.dsheirer.module.decode.nbfm;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dcs.DCSCode;
import io.github.dsheirer.module.decode.dcs.DCSDecoder;
import io.github.dsheirer.module.decode.dcs.DCSMessage;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Channel-level DCS (Digital-Coded Squelch) detector for use in NBFMDecoder.
 *
 * Wraps the existing DCSDecoder (which decodes 134.4 bps signalling from 8 kHz audio)
 * and adds channel-level filtering logic: accept/reject/lost callbacks based on a
 * configured set of allowed DCS codes.
 *
 * Operates identically to CTCSSDetector in concept:
 * - Feeds 8 kHz resampled audio to the underlying DCSDecoder
 * - When a DCS code is detected, checks if it's in the allowed set
 * - Reports detected (allowed), rejected (wrong code), or lost (no code for a period)
 *
 * DCS codes repeat at ~5.84 Hz (every ~171ms). We require CONFIRMATION_COUNT consecutive
 * detections of the same code before reporting, and LOSS_COUNT consecutive decode cycles
 * with no detection before reporting lost.
 */
public class DCSDetector
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DCSDetector.class);

    /**
     * Number of consecutive detections of the same code required before reporting.
     */
    private static final int CONFIRMATION_COUNT = 2;

    /**
     * Number of audio blocks processed without a DCS detection before declaring lost.
     * At 8 kHz with typical buffer sizes, this gives roughly 500ms-1s of silence.
     */
    private static final int LOSS_COUNT = 6;

    /**
     * Minimum interval between loss counter increments, in samples processed.
     * DCS repeats every ~1369 samples at 8 kHz. We check every ~1400 samples.
     */
    private static final int LOSS_CHECK_INTERVAL_SAMPLES = 1400;

    /**
     * Minimum ratio of low-band energy (DCS sub-audible range, <200 Hz) to wideband energy
     * for a DCS detection to be considered valid. Real DCS modulation concentrates energy
     * below 200 Hz. Broadband digital interference has roughly equal energy across all
     * frequencies, producing a ratio near 1.0. A ratio threshold of 1.5 means the low band
     * must have at least 50% more energy density than the wideband average.
     */
    private static final float DCS_BAND_RATIO_THRESHOLD = 1.5f;

    private final Set<DCSCode> mTargetCodes;
    private final DCSDecoder mDCSDecoder;

    // Detection state
    private DCSCode mDetectedCode = null;
    private int mConfirmationCounter = 0;
    private int mLossCounter = 0;
    private int mSamplesSinceLastDetection = 0;
    private int mTotalSamplesProcessed = 0;

    // Band energy tracking for broadband interference rejection
    private float mLowBandEnergy = 0;
    private float mWideBandEnergy = 0;
    private boolean mSignalQualityValid = true;

    // Callback
    private DCSDetectorListener mListener;

    /**
     * Listener interface for DCS detection events
     */
    public interface DCSDetectorListener
    {
        void dcsDetected(DCSCode code);
        void dcsRejected(DCSCode code);
        void dcsLost();
    }

    /**
     * Constructs a DCS detector for channel-level filtering.
     *
     * @param targetCodes the set of DCS codes to accept. Audio is only passed when one of these is detected.
     */
    public DCSDetector(Set<DCSCode> targetCodes)
    {
        mTargetCodes = targetCodes;

        // Create the underlying DCS decoder and register our message listener
        mDCSDecoder = new DCSDecoder();
        mDCSDecoder.setMessageListener(new Listener<IMessage>()
        {
            @Override
            public void receive(IMessage message)
            {
                if(message instanceof DCSMessage dcsMessage)
                {
                    handleDetection(dcsMessage.getDCSCode());
                }
            }
        });

        LOGGER.debug("DCSDetector initialized with {} target code(s)", targetCodes.size());
    }

    /**
     * Sets the listener for detection events.
     */
    public void setListener(DCSDetectorListener listener)
    {
        mListener = listener;
    }

    /**
     * Processes a buffer of 8 kHz demodulated FM audio samples.
     * Feeds them to the underlying DCSDecoder for 134.4 bps decoding.
     * Also computes band energy ratio to detect broadband interference.
     *
     * @param samples demodulated audio samples at 8 kHz
     */
    public void process(float[] samples)
    {
        if(samples == null || samples.length == 0)
        {
            return;
        }

        // Compute band energy ratio to detect broadband interference.
        // DCS modulation is below 200 Hz, so real DCS signals concentrate energy in the low band.
        // Broadband digital interference spreads energy evenly across all frequencies.
        // We use a simple time-domain approximation: low-frequency energy correlates with
        // sample-to-sample differences being small (slowly varying signal), while wideband
        // energy produces large sample-to-sample differences.
        float lowBand = 0;
        float wideBand = 0;
        for(int i = 1; i < samples.length; i++)
        {
            float sample = samples[i] * samples[i];
            float diff = samples[i] - samples[i - 1];
            wideBand += sample;
            // Low-frequency content: energy minus high-frequency component (proportional to diff^2)
            lowBand += sample - (diff * diff);
        }

        if(samples.length > 1)
        {
            // Exponential moving average for stability across buffers
            mWideBandEnergy = mWideBandEnergy * 0.7f + (wideBand / samples.length) * 0.3f;
            mLowBandEnergy = mLowBandEnergy * 0.7f + (Math.max(0, lowBand) / samples.length) * 0.3f;

            // Update signal quality flag
            if(mWideBandEnergy > 1e-10f)
            {
                float bandRatio = mLowBandEnergy / mWideBandEnergy;
                mSignalQualityValid = bandRatio >= DCS_BAND_RATIO_THRESHOLD;
            }
            else
            {
                mSignalQualityValid = true; // No signal — don't block
            }
        }

        // Feed audio to the DCS decoder — it will call our message listener when a code is found
        mDCSDecoder.receive(samples);

        // Track samples since last detection for loss detection
        mSamplesSinceLastDetection += samples.length;
        mTotalSamplesProcessed += samples.length;

        // Check for loss: if we've processed enough samples without a detection, increment loss counter
        if(mSamplesSinceLastDetection >= LOSS_CHECK_INTERVAL_SAMPLES)
        {
            // No DCS code detected in this interval
            if(mDetectedCode != null)
            {
                mLossCounter++;

                if(mLossCounter >= LOSS_COUNT)
                {
                    mDetectedCode = null;
                    mConfirmationCounter = 0;

                    if(mListener != null)
                    {
                        mListener.dcsLost();
                    }
                }
            }

            mSamplesSinceLastDetection = 0;
        }
    }

    /**
     * Handles detection of a DCS code from the underlying decoder.
     * Applies confirmation counting before reporting accepted or rejected codes.
     */
    private void handleDetection(DCSCode code)
    {
        if(code == null || code == DCSCode.UNKNOWN)
        {
            return;
        }

        // Note: broadband interference check removed in ap-14.10. Real DCS signals mixed with
        // voice audio produce low band ratios (0.05-0.40), well below any useful threshold.
        // The DCS 23-bit codeword + parity check is sufficient to reject false detections.

        // Reset loss tracking — we just got a detection
        mLossCounter = 0;
        mSamplesSinceLastDetection = 0;

        float bandRatio = (mWideBandEnergy > 1e-10f) ? (mLowBandEnergy / mWideBandEnergy) : 0;
        LOGGER.trace("DCS code {} detected (ratio={} confirm={}/{} target={})",
                code, String.format("%.3f", bandRatio), mConfirmationCounter, CONFIRMATION_COUNT,
                mTargetCodes.contains(code) ? "YES" : "NO");

        // Check if this code is in our allowed set
        if(!mTargetCodes.contains(code))
        {
            // Wrong code — track for confirmed rejection
            if(mDetectedCode == code)
            {
                if(mConfirmationCounter < CONFIRMATION_COUNT)
                {
                    mConfirmationCounter++;

                    if(mConfirmationCounter >= CONFIRMATION_COUNT && mListener != null)
                    {
                        mListener.dcsRejected(code);
                    }
                }
                // Already confirmed rejected
            }
            else
            {
                mDetectedCode = code;
                mConfirmationCounter = 1;
            }
            return;
        }

        // Code is in our allowed set
        if(mDetectedCode == code)
        {
            // Same code again — increment confirmation
            if(mConfirmationCounter < CONFIRMATION_COUNT)
            {
                mConfirmationCounter++;

                if(mConfirmationCounter >= CONFIRMATION_COUNT && mListener != null)
                {
                    mListener.dcsDetected(code);
                }
            }
            // Already confirmed — keep reporting
            else if(mListener != null)
            {
                mListener.dcsDetected(code);
            }
        }
        else
        {
            // Different code — restart confirmation
            mDetectedCode = code;
            mConfirmationCounter = 1;
        }
    }

    /**
     * Resets the detector state.
     */
    public void reset()
    {
        mDetectedCode = null;
        mConfirmationCounter = 0;
        mLossCounter = 0;
        mSamplesSinceLastDetection = 0;
        mLowBandEnergy = 0;
        mWideBandEnergy = 0;
        mSignalQualityValid = true;
    }

    /**
     * Returns the currently detected DCS code, or null if none confirmed.
     */
    public DCSCode getDetectedCode()
    {
        return (mConfirmationCounter >= CONFIRMATION_COUNT) ? mDetectedCode : null;
    }
}
