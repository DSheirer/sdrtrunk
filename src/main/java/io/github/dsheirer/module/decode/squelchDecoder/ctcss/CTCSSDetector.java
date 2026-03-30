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

package io.github.dsheirer.module.decode.squelchDecoder.ctcss;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.MessageFormat;
import java.util.Set;

/**
 * Real-time CTCSS tone detector using the Goertzel algorithm.
 *
 * Processing approach:
 * - Analyze an 8 KHz audio block of 512 samples
 * - Runs Goertzel on each standard frequency
 * - Run a standard deviation on the array of detected tones to determine a threshold
 * - Reports the strongest matching tone if it exceeds the threshold
 * Processing can be logged when the decoded messages log is turned on in the channel configuration of the playlist
 */
public class CTCSSDetector
{
    private static final Logger mLog = LoggerFactory.getLogger(CTCSSDetector.class);
    private static final double RESAMPLED_AUDIO_SAMPLE_RATE = 8000.0;

    /**
     * K factor is the multiplier * std deviation which determines threshold
     */
    private static final float K_FACTOR = 3.0f;
    /**
     * Number of consecutive detections required before reporting a match.
     * The first and last detected tones in a transmission tend to be incorrect,
     *  so we require at least 2 detections up front and at the end.
     */
    private static final int CONFIRMATION_COUNT = 2;
    private static final int LOSS_COUNT = 2;
    // Detection states
    private CTCSSCode mPreviousDetectedCode = null;
    private int mConfirmationCounter = 0;
    private int mLossCounter = 0;
    private boolean mUnmuted = false;               // used for event logging

    private final Set<CTCSSCode> mTargetCodes;      // codes we are looking to match in this channel
    private final float[] mDetectingFrequencies;
    private final CTCSSCode[] mDetectingCodeArray;  // codes we are detecting
    private final int mBlockSize;

    // Goertzel coefficients for each target frequency
    private final double[] mCoefficients;

    private float[] mSampleBuffer;


    private final Listener<IMessage> mMessageListener;

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
     * //@param targetCodes the set of CTCSS codes to accept as matches. If null or empty, accepts all standard codes.
     * //@param sampleRate the sample rate of the input audio in Hz
     */
    public CTCSSDetector(Set<CTCSSCode> targetCodes, Listener<IMessage> messageListener)
    {
        mMessageListener = messageListener;     // for logging info
        mBlockSize = 512;
        mTargetCodes = targetCodes;

        // Detect all codes including one on each end of the spectrum (DETECTING_CODES)
        // We find the strongest tone across all frequencies, then check if it's in our allowed set.
        Set<CTCSSCode> allCodes = CTCSSCode.DETECTING_CODES;
        mDetectingCodeArray = allCodes.toArray(new CTCSSCode[0]);
        mDetectingFrequencies = new float[mDetectingCodeArray.length];
        mCoefficients = new double[mDetectingCodeArray.length];

        for(int i = 0; i < mDetectingCodeArray.length; i++)
        {
            mDetectingFrequencies[i] = mDetectingCodeArray[i].getFrequency();
        }

        mSampleBuffer = new float[mBlockSize];

        // Pre-compute Goertzel coefficients: 2 * cos(2π * freq / sampleRate)
        for(int i = 0; i < mDetectingFrequencies.length; i++)
        {
            double normalizedFreq = mDetectingFrequencies[i] / RESAMPLED_AUDIO_SAMPLE_RATE;
            mCoefficients[i] = (2.0 * Math.cos(2.0 * Math.PI * normalizedFreq));
        }
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
        System.arraycopy(samples, 0, mSampleBuffer, 0, samples.length);
        analyzeBlock();
    }

    /**
     * Analyzes the current block of samples using a Goertzel algorithm for each ctcss frequency.
     * TODO: new detection algorithm to store the powers in to an array
     *  and subtract out (zero) the peak and the adjacent powers. Then find the next peak.
     *  then make sure the first peak is more than 6dB more than the next. This will take care of
     *  muting channels with interference where there might be more than 1 tone present.
     *
     * TODO: Also use the Goertzel algo that is in the DSP folder.  Might want to compare the
     *  double precision math with the single precision to see if it makes a difference.
     *
     */
    private void analyzeBlock()
    {
        double[] distribution = new double[mDetectingFrequencies.length];

        double maxPower = 0;
        int maxIndex = -1;

        for(int i = 0; i < mDetectingFrequencies.length; i++)
        {
            double power = goertzel(mSampleBuffer, mBlockSize, mCoefficients[i]);
            distribution[i] = power;

            if(power > maxPower)
            {
                maxPower = power;
                maxIndex = i;
            }
        }
        // Testing shows that if the power is below approx. 100, the tone can't be detected reliably
        // The lowest-most and upper-most tones are not valid CTCSS tones.
        CTCSSMessage message = new CTCSSMessage();      // uses timestamp at now
        if(maxPower < 100 || maxIndex == 0 || maxIndex == mDetectingFrequencies.length - 1)
        {
            // skip further detection
            message.setInitialThreshold(false);
            message.setMessage("Signal too week or detected CTCSS tone is outside of range of valid tones");
            mMessageListener.receive(message);
            handleNoDetection();
            return;
        }
        message.setInitialThreshold(true);

        // Determine threshold based on the noise level in the distribution using standard deviation.
        //  Co-channel interference from strong signals will cause multiple spikes in the distribution
        double sum = 0.0;
        for (double num : distribution) {
            sum += num;
        }
        double mean = sum / distribution.length;

        double varianceSum = 0.0;
        for (double num : distribution) {
            varianceSum += Math.pow(num - mean, 2);
        }

        double variance = varianceSum / (distribution.length - 1);      // sample type of std dev
        double stdDev = Math.sqrt(variance);
        double threshold = (stdDev * K_FACTOR) + mean;

        if(maxPower > threshold)
        {
            CTCSSCode detected = mDetectingCodeArray[maxIndex];
            handleDetection(detected);
        }
        else
        {
            handleNoDetection();
        }
        // TODO: check if listener exists first to save on processing?
        String s = MessageFormat.format("Detected CTCSS code: {0} maxPower: {1} 2nd threshold: {2} K_Factor: {3} ",
                mDetectingCodeArray[maxIndex].toString(),
                String.format("%.1f", maxPower),
                String.format("%.1f", threshold),
                String.format("%.1f",(maxPower - mean) / stdDev));
        message.setMessage(s);
        mMessageListener.receive(message);      // send to Decoded Messages Log if enabled.
    }

    /**
     * Goertzel algorithm - computes the energy at a specific frequency.
     *
     * @param samples the input samples
     * @param numSamples number of samples to process
     * @param coefficient pre-computed 2*cos(2π*f/fs)
     * @return the magnitude squared at the target frequency
     */
    private double goertzel(float[] samples, int numSamples, double coefficient)
    {
        double s0 = 0;
        double s1 = 0;
        double s2 = 0;

        for(int i = 0; i < numSamples; i++)
        {
            s0 = samples[i] + (coefficient * s1) - s2;
            s2 = s1;
            s1 = s0;
        }

        // Magnitude squared = s1^2 + s2^2 - coefficient * s1 * s2
        return (s1 * s1) + (s2 * s2) - (coefficient * s1 * s2);
    }

    /**
     * Handles detection of a CTCSS tone in the current block.
     * Only reports to the listener if the detected tone is in the allowed (target) set.
     */
    private void handleDetection(CTCSSCode newCode)
    {
        // since we now have detection, reset the loss counter
        mLossCounter = 0;

        // If tone is not present in the set...
        if(!mTargetCodes.contains(newCode))
        {
            // Track confirmed rejections — only notify after same wrong tone seen CONFIRMATION_COUNT times
            if(mPreviousDetectedCode == newCode)
            {
                if(mConfirmationCounter < CONFIRMATION_COUNT)
                {
                    mConfirmationCounter++;

                    if(mConfirmationCounter >= CONFIRMATION_COUNT && mListener != null)
                    {
                        if(mUnmuted)
                        {
                            CTCSSMessage message = new CTCSSMessage();
                            message.setInitialThreshold(false);
                            String s = MessageFormat.format("Configured CTCSS code: {0}, wrong tone detected {1} times, audio will be MUTED",
                                    mPreviousDetectedCode.toString(),
                                    CONFIRMATION_COUNT);
                            message.setMessage(s);
                            mMessageListener.receive(message);
                        }
                        mListener.ctcssRejected(newCode);
                        mUnmuted = false;
                    }
                }
                // Already confirmed rejected
            }
            else
            {
                mPreviousDetectedCode = newCode;
                mConfirmationCounter = 1;
            }
            return;
        }

        if(mPreviousDetectedCode == newCode)
        {
            // Same tone detected again -- increment confirmation
            if(mConfirmationCounter < CONFIRMATION_COUNT)
            {
                mConfirmationCounter++;

                if(mConfirmationCounter >= CONFIRMATION_COUNT && mListener != null)
                {
                    mListener.ctcssDetected(newCode);
                    if(!mUnmuted)
                    {
                        CTCSSMessage message = new CTCSSMessage();
                        message.setInitialThreshold(true);
                        String s = MessageFormat.format("Configured CTCSS code: {0}, correct tone detected {1} times, audio will be UN-MUTED",
                                newCode.toString(),
                                CONFIRMATION_COUNT);
                        message.setMessage(s);
                        mMessageListener.receive(message);
                        mUnmuted = true;
                    }
                }
            }
            // Already confirmed -- just keep reporting
            else if(mListener != null)
            {
                mListener.ctcssDetected(newCode);
            }
        }
        else
        {
            // Different tone -- restart confirmation
            mPreviousDetectedCode = newCode;
            mConfirmationCounter = 1;
        }
    }

    /**
     * Handles no CTCSS tone detected in the current block.
     */
    private void handleNoDetection()
    {
        if(mPreviousDetectedCode != null)
        {
            mLossCounter++;

            if(mLossCounter >= LOSS_COUNT)
            {
                if(mUnmuted)
                {
                    CTCSSMessage message = new CTCSSMessage();
                    message.setInitialThreshold(false);
                    String s = MessageFormat.format("Configured CTCSS code: {0}, no tone detected {1} times, audio will be MUTED",
                            mPreviousDetectedCode.toString(),
                            LOSS_COUNT);
                    message.setMessage(s);
                    mMessageListener.receive(message);
                }
                mPreviousDetectedCode = null;
                mConfirmationCounter = 0;
                mUnmuted = false;
                if(mListener != null)
                {
                    mListener.ctcssLost();
                }
            }
        }
    }

    /**
     * Resets the detector state. Called when the noise squelch closes
     */
    public void reset()
    {
        CTCSSMessage message = new CTCSSMessage();
        message.setInitialThreshold(false);
        message.setMessage("Noise squelch closed, audio will be MUTED");
        mUnmuted = false;
        mMessageListener.receive(message);
        mPreviousDetectedCode = null;
        mConfirmationCounter = 0;
        mLossCounter = 0;
    }
}

