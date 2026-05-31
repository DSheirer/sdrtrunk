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

import io.github.dsheirer.channel.state.DecoderStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * or by viewing the messages tab in the Now Playing window.
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
    private boolean mMuted = true;
    private int mRejectedQualification = 0;
    private CTCSSCode mPreviousRejectedCode = null;

    private final List<CTCSSCode> mTargetCodes;      // codes we are looking to match in this channel
    private final float[] mDetectingFrequencies;
    private final CTCSSCode[] mDetectingCodeArray;  // codes we are detecting
    private CTCSSMessage mCTCSSMessage = null;
    // Goertzel coefficients for each target frequency
    private final double[] mCoefficients;

    /**
     * Constructs a CTCSS detector for a specific set of target tones.
     *
     * //@param targetCodes the set of CTCSS codes to accept as matches. If null or empty, accepts all standard codes.
     * //@param sampleRate the sample rate of the input audio in Hz
     */
    public CTCSSDetector(List<CTCSSCode> targetCodes)
    {
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
     * Processes a buffer of demodulated FM audio samples.
     * Goertzel analysis performed on a block of 512 audio samples and the relative power is calculated
     * for each CTCSS tone and placed into a frequency distribution array. The standard deviation of the array
     * is calculated to determine threshold based on the noise. The highest powered tone must exceed this threshold
     * to be considered for match detection. Testing revealed that there can be multiple peaks present in
     * the distribution that were considered to be interference rather than desired signals.
     *
     * @param samples demodulated audio samples
     */
    public CTCSSMessage process(float[] samples)
    {
        mCTCSSMessage = new CTCSSMessage(mTargetCodes.getFirst());
        double[] distribution = new double[mDetectingFrequencies.length];
        double maxPower = 0;
        int maxIndex = -1;
        if(samples == null || samples.length < MIN_BLOCK_SIZE)
        {
            return null;
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
        if(maxPower < 100 || maxIndex == 0 || maxIndex == mDetectingFrequencies.length - 1)
        {
            // skip further detection
            mCTCSSMessage.setFirstThreshold(false);
            mCTCSSMessage.setMessage("Signal too weak or detected CTCSS tone is outside of range of valid tones");
            detectionLogicTree(null);
            return mCTCSSMessage;
        }

        mCTCSSMessage.setFirstThreshold(true);
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
        mCTCSSMessage.setPower(maxPower);
        mCTCSSMessage.setPowerThreshold(threshold);
        if(maxPower > threshold)
        {
            CTCSSCode detected = mDetectingCodeArray[maxIndex];
            detectionLogicTree(detected);
        }
        else
        {
            detectionLogicTree(null);   // set the code to null
        }

        return mCTCSSMessage;
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
     * @param newCode currently detected CTCSS code, or null if no code detected
     */
    private void detectionLogicTree(CTCSSCode newCode)
    {
        if (mMuted)
        {
            if (newCode != null && mTargetCodes.contains(newCode))
            {
                if (mOpenCounter >= OPEN_THRESHOLD_COUNT)
                {
                    // unmute and report detected code
                    mMuted = false;
                    mCTCSSMessage.setMutedStatus(false);
                    mCTCSSMessage.setCTCSSCode(newCode);
                    mCTCSSMessage.setMessage("Correct tone detected, thresholds passed, now unmuting.");
                    mCTCSSMessage.setCallEvent(DecoderStateEvent.Event.START);
                    mCTCSSMessage.setCodeState(CTCSSMessage.SquelchCodeState.ACCEPTED);
                    mCloseCounter = 0;
                }
                else
                {
                    // increment counter and wait for next audio buffer
                    mOpenCounter++;
                    mCTCSSMessage.setCTCSSCode(newCode);
                    mCTCSSMessage.setMessage("Waiting for consecutive correct detections.");
                }
            }
            else    // wrong tone or no tone, still muted
            {
                mOpenCounter = 0;
                mCTCSSMessage.setMutedStatus(true);
                mCTCSSMessage.setCTCSSCode(newCode);
                if(newCode != null)
                {
                    /*
                     * Qualify the rejected code so that the information in the Now Playing -> Details tab
                     * is accurate in case someone doesn't know the correct squelch code for this channel.
                     * We don't want to display codes that are just single detections in noise.
                     */
                    if(mRejectedQualification == OPEN_THRESHOLD_COUNT)
                    {
                        if(newCode == mPreviousRejectedCode)
                        {
                            mCTCSSMessage.setCodeState(CTCSSMessage.SquelchCodeState.REJECTED);
                            // this will increment past the OPEN_THRESHOLD_COUNT so the rejected state is only sent once. (== above)
                            mRejectedQualification++;
                        }
                        else
                        {
                            mRejectedQualification = 0;
                            mPreviousRejectedCode = newCode;
                        }
                    }
                    else
                    {
                        if(newCode == mPreviousRejectedCode)
                        {
                            mRejectedQualification++;

                        }
                        else
                        {
                            mRejectedQualification = 0;
                            mPreviousRejectedCode = newCode;
                        }
                    }
                }
                else
                {
                    mCTCSSMessage.setCodeState(CTCSSMessage.SquelchCodeState.LOST);
                }
            }
        }
        else    // unmuted and call is ongoing
        {
            if (newCode != null && mTargetCodes.contains(newCode))
            {
                // all is good, call continues
                mCloseCounter = 0;
                mCTCSSMessage.setMutedStatus(false);
                mCTCSSMessage.setCTCSSCode(newCode);
                mCTCSSMessage.setCallEvent(DecoderStateEvent.Event.CONTINUATION);
            }
            else
            {
                if (mCloseCounter >= CLOSE_THRESHOLD_COUNT)
                {
                    // Mute audio and report rejected code or lost tone
                    mMuted = true;
                    mCTCSSMessage.setMutedStatus(true);
                    mCTCSSMessage.setCTCSSCode(newCode);
                    mCTCSSMessage.setMessage("Incorrect tone detected or lost, now muting.");
                    mCTCSSMessage.setCallEvent(DecoderStateEvent.Event.END);
                    mOpenCounter = 0;
                    if(newCode != null)
                    {
                        mCTCSSMessage.setCodeState(CTCSSMessage.SquelchCodeState.REJECTED);
                    }
                    else
                    {
                        mCTCSSMessage.setCodeState(CTCSSMessage.SquelchCodeState.LOST);
                    }
                }
                else
                {
                    // all is still good with the call, but a bad tone was decoded and the closeCounter hasn't
                    //  reached a threshold yet.
                    mCloseCounter++;
                    mCTCSSMessage.setMutedStatus(false);
                    mCTCSSMessage.setCTCSSCode(newCode);
                    mCTCSSMessage.setMessage("Waiting for consecutive incorrect or no tone.");
                    mCTCSSMessage.setCallEvent(DecoderStateEvent.Event.CONTINUATION);
                }
            }
        }
    }

    /**
     * Resets the detector state. Called when the noise squelch closes
     */
    public CTCSSMessage reset()
    {
        CTCSSMessage message = new CTCSSMessage(mTargetCodes.getFirst());
        message.setMessage("Noise squelch closed.");
        message.setCTCSSCode(null);     // clears CTCSSIdentifier
        message.setMutedStatus(true);
        message.setCallEvent(DecoderStateEvent.Event.END);
        message.setCodeState(CTCSSMessage.SquelchCodeState.LOST);
        mMuted = true;
        mCloseCounter = 0;
        mOpenCounter = 0;
        mPreviousRejectedCode = null;
        mRejectedQualification = 0;
        return message;
    }

}

