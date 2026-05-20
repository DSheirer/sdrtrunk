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
import java.util.List;
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
    private static final int MIN_BLOCK_SIZE = 512;

    /**
     * K factor is the multiplier * std deviation which determines threshold
     */
    private static final float K_FACTOR = 3.0f;
    /**
     * Number of consecutive detections required before reporting a match.
     * The first and last detected tones in a transmission tend to be incorrect,
     *  so we require at least 2 detections up front and at the end.
     */
    private static final int OPEN_THRESHOLD_COUNT = 2;
    private static final int CLOSE_THRESHOLD_COUNT = 2;
    // Detection states
    private int mOpenCounter = 0;
    private int mCloseCounter = 0;
    private boolean mUnmuted = false;

    private final List<CTCSSCode> mTargetCodes;      // codes we are looking to match in this channel
    private final float[] mDetectingFrequencies;
    private final CTCSSCode[] mDetectingCodeArray;  // codes we are detecting

    // Goertzel coefficients for each target frequency
    private final double[] mCoefficients;

    // Listeners
    private final Listener<IMessage> mLoggingListener;
    private CTCSSDetectorListener mDetectionListener;

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
    public CTCSSDetector(List<CTCSSCode> targetCodes, Listener<IMessage> loggingListener)
    {
        mLoggingListener = loggingListener;     // for logging info
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
        mDetectionListener = listener;
    }

    /**
     * Processes a buffer of demodulated FM audio samples.
     * Goertzel analysis performed on a block of 512 audio samples and the relative power is calculated
     * for each CTCSS tone and placed into a frequency distribution array. The standard deviation of the array
     * is calculated to determine threshold based on the noise. The highest powered tone must exceed this threshold
     * to be considered for match detection. Testing revealed that there can be multiple peaks present in
     * the distribution that were considered to be interference rather than desired signals.
     *
     * @param samples demodulated audio samples
     */
    public void process(float[] samples)
    {
        double[] distribution = new double[mDetectingFrequencies.length];
        double maxPower = 0;
        int maxIndex = -1;
        if(samples == null || samples.length < MIN_BLOCK_SIZE)
        {
            return;
        }

        for(int i = 0; i < mDetectingFrequencies.length; i++)
        {
            double power = goertzel(samples, samples.length, mCoefficients[i]);
            distribution[i] = power;

            if(power > maxPower)
            {
                maxPower = power;
                maxIndex = i;
            }
        }
        // Testing shows that if the power is below approx. 100, the tone can't be detected reliably
        // The lowest-most and upper-most tones are not valid CTCSS tones.
        //CTCSSMessage message = new CTCSSMessage();      // uses timestamp at now
        if(maxPower < 100 || maxIndex == 0 || maxIndex == mDetectingFrequencies.length - 1)
        {
            // skip further detection
           // message.setInitialThreshold(false);
            //message.setMessage("Signal too weak or detected CTCSS tone is outside of range of valid tones");
            //mLoggingListener.receive(message);
            handleNoDetection();
            return;
        }
        //message.setInitialThreshold(true);

        // Determine threshold based on the noise level in the distribution using standard deviation.
        //  adjacent channel interference from strong signals will cause multiple spikes in the distribution
        double sum = 0.0;
        for (double num : distribution)
        {
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
            detectionLogicTree(detected);
        }
        else
        {
            handleNoDetection();
        }

        String s = MessageFormat.format("Detected CTCSS code: {0} maxPower: {1} 2nd threshold: {2} K_Factor: {3} ",
                mDetectingCodeArray[maxIndex].toString(),
                String.format("%.1f", maxPower),
                String.format("%.1f", threshold),
                String.format("%.1f",(maxPower - mean) / stdDev));
        //message.setMessage(s);
        //mLoggingListener.receive(message);      // send to Decoded Messages Log if enabled.
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
     * Handles detection logic of a CTCSS tone in the current block.
     */
    private void detectionLogicTree(CTCSSCode newCode)
    {
        /*
         * 	if muted
         * 		if newTone != null && newTone == setting
         * 			if openCounter >= OpenThreshold
         * 				unmute
         * 				closecounter = 0
         * 				report accepted
         * 			else
         * 				openCounter++
         *
         * 		else	// wrong tone
         * 			openCounter = 0
         *
         *
         * 	else // if not muted, call is ongoing
         * 		if newTone != null && newTone == setting
         * 			all is good - call continues
         * 			closeCounter = 0
         * 		else
         * 			if(closeCounter >= closeThreshold
         * 				mute call
         * 				openCounter = 0
         * 				report rejected
         * 			else
         * 				closeCounter++
         * 				all is still good, but there is a incorrect tone detected below the threshold counts
         * 				report accepted
         *
         * reset
         * 	mute call
         * 	openCounter = 0
         * 	closeCounter = 0
         */
        if (!mUnmuted)
        {
            if (newCode != null && mTargetCodes.contains(newCode))
            {
                if (mOpenCounter >= OPEN_THRESHOLD_COUNT)
                {
                    mUnmuted = true;
                    mCloseCounter = 0;
                    mDetectionListener.ctcssDetected(newCode);
                }
                else
                {
                    mOpenCounter++;
                }
            }
            else    // wrong tone or no tone
            {
                mOpenCounter = 0;
            }
        }
        else
        {
            if (newCode != null && mTargetCodes.contains(newCode))
            {
                // all is good, call continues
                mCloseCounter = 0;
            }
            else
            {
                if (mCloseCounter >= CLOSE_THRESHOLD_COUNT)
                {
                    mUnmuted = false;
                    mOpenCounter = 0;
                    if(newCode != null)
                        mDetectionListener.ctcssRejected(newCode);
                    else
                        mDetectionListener.ctcssLost();
                }
                else
                {
                    mCloseCounter++;
                    // all is still good with the call, but a bad tone was decoded and the closeCounter hasn't reached a threshold yet.
                }
            }
        }

        String toneString;
        if (newCode == null)
        {
            toneString = "NONE";
        }
        else
        {
            toneString = newCode.toString();
        }

        String mutedString;
        if (mUnmuted)
        {
            mutedString = "UNMUTED";
        }
        else
        {
            mutedString = "MUTED";
        }

        CTCSSCode setCode = mTargetCodes.getFirst();
        CTCSSMessage message = new CTCSSMessage(newCode);
        message.setInitialThreshold(true);
        String s = MessageFormat.format("CTCSS Configured: {0}, Detected {1}, Open Counts: {2}, Close counts: {3}, Audio: {4}",
                setCode,
                toneString,
                mOpenCounter,
                mCloseCounter,
                mutedString);
        message.setMessage(s);
        mLoggingListener.receive(message);
    }

    /**
     * Handles no CTCSS tone detected in the current block.
     */
    private void handleNoDetection()
    {
        detectionLogicTree(null);
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
        mLoggingListener.receive(message);
        mCloseCounter = 0;
        mOpenCounter = 0;
        mDetectionListener.ctcssLost();
    }
    public static record GoertzelResult(CTCSSCode code, CTCSSMessage debugMessage){}
}

