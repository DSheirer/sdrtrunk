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

import io.github.dsheirer.module.decode.ctcss.CTCSSCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Real-time CTCSS tone detector using the Goertzel algorithm.
 *
 * Analyzes demodulated FM audio to detect sub-audible CTCSS tones (67.0 - 254.1 Hz).
 * The Goertzel algorithm is an efficient way to compute the energy at specific frequencies
 * without performing a full FFT.
 *
 * Processing approach:
 * - Collects samples into blocks (sized for ~2 cycles of the lowest target tone)
 * - Runs Goertzel on each target frequency
 * - Compares detected energy against a noise floor estimate
 * - Reports the strongest matching tone if it exceeds the threshold
 */
public class CTCSSDetector
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CTCSSDetector.class);

    /**
     * Minimum SNR (signal-to-noise ratio in dB) for a tone to be considered detected.
     * CTCSS tones are typically 10-15% of peak deviation, so they're relatively weak.
     */
    private static final float DETECTION_THRESHOLD_DB = 6.0f;

    /**
     * Minimum dB by which the strongest Goertzel bin must exceed the average of its
     * nearest neighbor bins. A real CTCSS tone produces a sharp narrowband peak that
     * easily clears this threshold. Broadband interference distributes energy evenly
     * across all bins and fails this check.
     *
     * This is the threshold for tones above 120 Hz. For lower tones, the threshold
     * is reduced because closely-spaced Goertzel bins cause significant spectral
     * leakage (e.g., 97.4/100.0/103.5 Hz are only ~3 Hz apart, so a real 100 Hz
     * tone produces nearly equal energy in all three bins — only ~1 dB separation).
     */
    private static final float NARROWBAND_THRESHOLD_DB = 3.0f;

    /**
     * Narrowband threshold for tones at or below this frequency (Hz).
     * Below this cutoff, we use a relaxed narrowband threshold to account for
     * spectral leakage between closely-spaced low-frequency Goertzel bins.
     */
    private static final float LOW_FREQ_NARROWBAND_CUTOFF = 120.0f;

    /**
     * Relaxed narrowband threshold for low-frequency tones (<= 120 Hz).
     * At 100 Hz, real tones show ~1 dB above neighbors due to bin spacing.
     * 0.5 dB still rejects true broadband interference (which produces 0 dB difference).
     */
    private static final float LOW_FREQ_NARROWBAND_THRESHOLD_DB = 0.5f;

    /**
     * Number of neighbor bins to average on each side of the detected bin for the
     * narrowband check (2 above + 2 below = 4 total neighbors).
     */
    private static final int NARROWBAND_NEIGHBOR_COUNT = 2;

    /**
     * Number of consecutive detections required before reporting a match.
     * Prevents false triggers from transient energy.
     * 3 blocks at ~75ms each = ~225ms detection latency (within normal radio PL decode time).
     */
    private static final int CONFIRMATION_COUNT = 3;

    /**
     * Number of consecutive misses before declaring tone lost.
     */
    private static final int LOSS_COUNT = 4;

    private final Set<CTCSSCode> mTargetCodes;
    private final float[] mTargetFrequencies;
    private final CTCSSCode[] mTargetCodeArray;
    private final float mSampleRate;
    private final int mBlockSize;

    // Goertzel coefficients for each target frequency
    private final float[] mCoefficients;

    // Sample accumulator
    private float[] mSampleBuffer;
    private int mSampleIndex = 0;

    // Detection state
    private CTCSSCode mDetectedCode = null;
    private int mConfirmationCounter = 0;
    private int mLossCounter = 0;

    // Tone survey: log top tones periodically regardless of threshold
    private static final int SURVEY_INTERVAL_BLOCKS = 40; // ~every 3 seconds at 75ms/block
    private int mSurveyBlockCounter = 0;

    // Callback
    private CTCSSDetectorListener mListener;

    /**
     * Listener interface for CTCSS detection events
     */
    public interface CTCSSDetectorListener
    {
        void ctcssDetected(CTCSSCode code);
        void ctcssRejected(CTCSSCode code);
        void ctcssLost();
    }

    /**
     * Constructs a CTCSS detector for a specific set of target tones.
     *
     * @param targetCodes the set of CTCSS codes to accept as matches. If null or empty, accepts all standard codes.
     * @param sampleRate the sample rate of the input audio in Hz
     */
    public CTCSSDetector(Set<CTCSSCode> targetCodes, float sampleRate)
    {
        mSampleRate = sampleRate;

        if(targetCodes == null || targetCodes.isEmpty())
        {
            mTargetCodes = CTCSSCode.STANDARD_CODES;
        }
        else
        {
            mTargetCodes = targetCodes;
        }

        // Always scan ALL standard CTCSS tones to prevent spectral leakage false matches.
        // We find the strongest tone across all frequencies, then check if it's in our allowed set.
        Set<CTCSSCode> allCodes = CTCSSCode.STANDARD_CODES;
        mTargetCodeArray = allCodes.toArray(new CTCSSCode[0]);
        mTargetFrequencies = new float[mTargetCodeArray.length];
        mCoefficients = new float[mTargetCodeArray.length];

        for(int i = 0; i < mTargetCodeArray.length; i++)
        {
            mTargetFrequencies[i] = mTargetCodeArray[i].getFrequency();
        }

        // Block size: enough samples for ~2.5 cycles of the lowest frequency we care about
        // Lowest CTCSS tone is 67.0 Hz. At 8000 Hz sample rate: 8000/67 * 2.5 ≈ 299 samples
        // We use a power-of-nothing block — just enough for good frequency resolution
        float lowestFreq = Float.MAX_VALUE;
        for(float freq : mTargetFrequencies)
        {
            if(freq > 0 && freq < lowestFreq)
            {
                lowestFreq = freq;
            }
        }

        // Block size targets sufficient frequency resolution to discriminate closely-spaced
        // CTCSS tones (e.g. 131.8 Hz vs 146.2 Hz = 14.4 Hz apart). Frequency resolution is
        // sampleRate / blockSize, so we need blockSize >= sampleRate / 14 for ~1 Hz margin.
        // We use the larger of: 5 cycles of the lowest tone, or sampleRate / 12 (for ~12 Hz resolution).
        int blockByCycles = (int)(mSampleRate / lowestFreq * 5.0f);
        int blockByResolution = (int)(mSampleRate / 12.0f);
        int blockSize = Math.max(blockByCycles, blockByResolution);

        // Clamp to reasonable range
        if(blockSize < 256)
        {
            blockSize = 256;
        }
        else if(blockSize > 4096)
        {
            blockSize = 4096;
        }

        mBlockSize = blockSize;

        mSampleBuffer = new float[mBlockSize];

        // Pre-compute Goertzel coefficients: 2 * cos(2π * freq / sampleRate)
        for(int i = 0; i < mTargetFrequencies.length; i++)
        {
            double normalizedFreq = mTargetFrequencies[i] / mSampleRate;
            mCoefficients[i] = (float)(2.0 * Math.cos(2.0 * Math.PI * normalizedFreq));
        }

        LOGGER.debug("CTCSSDetector initialized: {} target tones, block size {}, sample rate {}",
                mTargetCodeArray.length, mBlockSize, mSampleRate);
    }

    /**
     * Sets the listener for detection events.
     */
    public void setListener(CTCSSDetectorListener listener)
    {
        mListener = listener;
    }

    /**
     * Processes a buffer of demodulated FM audio samples.
     * Samples are accumulated into blocks, and Goertzel analysis is performed on each complete block.
     *
     * @param samples demodulated audio samples
     */
    public void process(float[] samples)
    {
        if(samples == null || samples.length == 0)
        {
            return;
        }

        int offset = 0;

        while(offset < samples.length)
        {
            int remaining = mBlockSize - mSampleIndex;
            int toCopy = Math.min(remaining, samples.length - offset);

            System.arraycopy(samples, offset, mSampleBuffer, mSampleIndex, toCopy);
            mSampleIndex += toCopy;
            offset += toCopy;

            if(mSampleIndex >= mBlockSize)
            {
                analyzeBlock();
                mSampleIndex = 0;
            }
        }
    }

    /**
     * Analyzes the current block of samples using Goertzel algorithm for each target frequency.
     *
     * Two checks must pass for a detection:
     * 1. The strongest bin must exceed the total-energy SNR threshold (existing check)
     * 2. The strongest bin must exceed the average of its nearest neighbor bins by
     *    NARROWBAND_THRESHOLD_DB (narrowband check — rejects broadband interference)
     */
    private void analyzeBlock()
    {
        // Compute total energy (noise floor estimate)
        float totalEnergy = 0;
        for(int i = 0; i < mBlockSize; i++)
        {
            totalEnergy += mSampleBuffer[i] * mSampleBuffer[i];
        }
        totalEnergy /= mBlockSize;

        // Avoid division by zero
        if(totalEnergy < 1e-10f)
        {
            handleNoDetection();
            return;
        }

        // Run Goertzel for each target frequency and store all power values
        float[] powers = new float[mTargetFrequencies.length];
        float maxPower = 0;
        int maxIndex = -1;

        for(int i = 0; i < mTargetFrequencies.length; i++)
        {
            powers[i] = goertzel(mSampleBuffer, mBlockSize, mCoefficients[i]);

            if(powers[i] > maxPower)
            {
                maxPower = powers[i];
                maxIndex = i;
            }
        }

        // Normalize power relative to total energy
        float normalizedPower = maxPower / (totalEnergy * mBlockSize);

        // Convert to dB
        float snrDB = (float)(10.0 * Math.log10(normalizedPower + 1e-10));

        // Periodic tone survey: log top 3 tones regardless of threshold
        mSurveyBlockCounter++;
        if(mSurveyBlockCounter >= SURVEY_INTERVAL_BLOCKS)
        {
            mSurveyBlockCounter = 0;

            // Find top 3 bins
            int[] topIdx = {-1, -1, -1};
            float[] topPow = {0, 0, 0};
            for(int i = 0; i < powers.length; i++)
            {
                if(powers[i] > topPow[0])
                {
                    topPow[2] = topPow[1]; topIdx[2] = topIdx[1];
                    topPow[1] = topPow[0]; topIdx[1] = topIdx[0];
                    topPow[0] = powers[i]; topIdx[0] = i;
                }
                else if(powers[i] > topPow[1])
                {
                    topPow[2] = topPow[1]; topIdx[2] = topIdx[1];
                    topPow[1] = powers[i]; topIdx[1] = i;
                }
                else if(powers[i] > topPow[2])
                {
                    topPow[2] = powers[i]; topIdx[2] = i;
                }
            }

            StringBuilder sb = new StringBuilder("CTCSS survey: ");
            for(int rank = 0; rank < 3; rank++)
            {
                if(topIdx[rank] >= 0)
                {
                    float np = topPow[rank] / (totalEnergy * mBlockSize);
                    float db = (float)(10.0 * Math.log10(np + 1e-10));
                    if(rank > 0) sb.append(", ");
                    sb.append(String.format("#%d %s (%.1f Hz) %.1f dB", rank + 1,
                            mTargetCodeArray[topIdx[rank]], mTargetFrequencies[topIdx[rank]], db));
                }
            }
            LOGGER.debug(sb.toString());
        }

        if(maxIndex >= 0 && snrDB > DETECTION_THRESHOLD_DB)
        {
            // Note: the narrowband check (comparing strongest bin vs neighbors) was removed
            // in ap-14.9.7. CTCSS tones are sub-audible (~10-15% of peak deviation), so when
            // voice audio is present, the broadband voice energy dominates ALL Goertzel bins
            // equally. The CTCSS tone only adds ~0.3-0.5 dB above neighbors during active voice,
            // which is below any useful narrowband threshold. This caused tone squelch to never
            // open during transmissions. The 3-block confirmation count is sufficient to reject
            // broadband interference — random noise won't consistently peak at the same CTCSS
            // frequency 3 times in a row across 42 possible tones (<0.06% probability).

            CTCSSCode detected = mTargetCodeArray[maxIndex];

            // For low-frequency tones, spectral leakage can cause a neighboring non-target bin
            // to narrowly beat the actual target bin. If a target tone is within 2 bins of the
            // winner and within 2 dB, prefer the target tone — it's almost certainly the real tone.
            if(!mTargetCodes.contains(detected) && mTargetFrequencies[maxIndex] <= LOW_FREQ_NARROWBAND_CUTOFF)
            {
                for(int offset = 1; offset <= 2; offset++)
                {
                    int belowIdx = maxIndex - offset;
                    int aboveIdx = maxIndex + offset;

                    if(belowIdx >= 0 && mTargetCodes.contains(mTargetCodeArray[belowIdx]))
                    {
                        float diffDB = (float)(10.0 * Math.log10((maxPower / (powers[belowIdx] + 1e-10f)) + 1e-10));
                        if(diffDB < 2.0f)
                        {
                            LOGGER.trace("CTCSS preferring target {} over winner {} (diff={} dB)",
                                    mTargetCodeArray[belowIdx], detected, String.format("%.1f", diffDB));
                            detected = mTargetCodeArray[belowIdx];
                            maxIndex = belowIdx;
                            maxPower = powers[belowIdx];
                            break;
                        }
                    }
                    if(aboveIdx < mTargetCodeArray.length && mTargetCodes.contains(mTargetCodeArray[aboveIdx]))
                    {
                        float diffDB = (float)(10.0 * Math.log10((maxPower / (powers[aboveIdx] + 1e-10f)) + 1e-10));
                        if(diffDB < 2.0f)
                        {
                            LOGGER.trace("CTCSS preferring target {} over winner {} (diff={} dB)",
                                    mTargetCodeArray[aboveIdx], detected, String.format("%.1f", diffDB));
                            detected = mTargetCodeArray[aboveIdx];
                            maxIndex = aboveIdx;
                            maxPower = powers[aboveIdx];
                            break;
                        }
                    }
                }
            }

            // Log at DEBUG only on confirmation transitions; TRACE for every block
            if(mConfirmationCounter < CONFIRMATION_COUNT ||
               (mDetectedCode != detected))
            {
                LOGGER.debug("CTCSS tone {} ({} Hz) detected: SNR={} dB confirm={}/{} target={}",
                        detected, String.format("%.1f", mTargetFrequencies[maxIndex]),
                        String.format("%.1f", snrDB),
                        mConfirmationCounter, CONFIRMATION_COUNT,
                        mTargetCodes.contains(detected) ? "YES" : "NO");
            }
            else
            {
                LOGGER.trace("CTCSS tone {} ({} Hz) detected: SNR={} dB confirm={}/{} target={}",
                        detected, String.format("%.1f", mTargetFrequencies[maxIndex]),
                        String.format("%.1f", snrDB),
                        mConfirmationCounter, CONFIRMATION_COUNT,
                        mTargetCodes.contains(detected) ? "YES" : "NO");
            }

            handleDetection(detected);
        }
        else
        {
            // Log near-misses at TRACE to avoid flooding overnight logs
            if(maxIndex >= 0 && snrDB > (DETECTION_THRESHOLD_DB - 3.0f))
            {
                LOGGER.trace("CTCSS tone {} ({} Hz) below threshold: SNR={} dB (need {} dB)",
                        mTargetCodeArray[maxIndex], String.format("%.1f", mTargetFrequencies[maxIndex]),
                        String.format("%.1f", snrDB), DETECTION_THRESHOLD_DB);
            }
            handleNoDetection();
        }
    }

    /**
     * Goertzel algorithm - computes the energy at a specific frequency.
     *
     * @param samples the input samples
     * @param numSamples number of samples to process
     * @param coefficient pre-computed 2*cos(2π*f/fs)
     * @return the magnitude squared at the target frequency
     */
    private float goertzel(float[] samples, int numSamples, float coefficient)
    {
        float s0 = 0;
        float s1 = 0;
        float s2 = 0;

        for(int i = 0; i < numSamples; i++)
        {
            s0 = samples[i] + coefficient * s1 - s2;
            s2 = s1;
            s1 = s0;
        }

        // Magnitude squared = s1^2 + s2^2 - coefficient * s1 * s2
        return s1 * s1 + s2 * s2 - coefficient * s1 * s2;
    }

    /**
     * Handles detection of a CTCSS tone in the current block.
     * Only reports to the listener if the detected tone is in the allowed (target) set.
     */
    private void handleDetection(CTCSSCode code)
    {
        mLossCounter = 0;

        // Only accept tones that are in our allowed set
        if(!mTargetCodes.contains(code))
        {
            // Track confirmed rejections — only notify after same wrong tone seen CONFIRMATION_COUNT times
            if(mDetectedCode == code)
            {
                if(mConfirmationCounter < CONFIRMATION_COUNT)
                {
                    mConfirmationCounter++;

                    if(mConfirmationCounter >= CONFIRMATION_COUNT && mListener != null)
                    {
                        mListener.ctcssRejected(code);
                    }
                }
                // Already confirmed rejected — don't spam listener
            }
            else
            {
                mDetectedCode = code;
                mConfirmationCounter = 1;
            }
            return;
        }

        if(mDetectedCode == code)
        {
            // Same tone detected again — increment confirmation
            if(mConfirmationCounter < CONFIRMATION_COUNT)
            {
                mConfirmationCounter++;

                if(mConfirmationCounter >= CONFIRMATION_COUNT && mListener != null)
                {
                    mListener.ctcssDetected(code);
                }
            }
            // Already confirmed — just keep reporting
            else if(mListener != null)
            {
                mListener.ctcssDetected(code);
            }
        }
        else
        {
            // Different tone — restart confirmation
            mDetectedCode = code;
            mConfirmationCounter = 1;
        }
    }

    /**
     * Handles no CTCSS tone detected in the current block.
     */
    private void handleNoDetection()
    {
        if(mDetectedCode != null)
        {
            mLossCounter++;

            if(mLossCounter >= LOSS_COUNT)
            {
                mDetectedCode = null;
                mConfirmationCounter = 0;

                if(mListener != null)
                {
                    mListener.ctcssLost();
                }
            }
        }
    }

    /**
     * Resets the detector state.
     */
    public void reset()
    {
        mSampleIndex = 0;
        mDetectedCode = null;
        mConfirmationCounter = 0;
        mLossCounter = 0;
    }

    /**
     * Returns the currently detected CTCSS code, or null if none detected.
     */
    public CTCSSCode getDetectedCode()
    {
        return (mConfirmationCounter >= CONFIRMATION_COUNT) ? mDetectedCode : null;
    }
}
