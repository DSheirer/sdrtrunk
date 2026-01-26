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

package io.github.dsheirer.module.decode.ctcss;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IRealBufferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Continuous Tone-Coded Squelch System (CTCSS) decoder designed to work with FM demodulated 8 kHz input audio samples.
 * Also known as PL (Private Line) by Motorola or Channel Guard by GE/Ericsson.
 *
 * This decoder uses the Goertzel algorithm to efficiently detect sub-audible tones in the 67-254 Hz range.
 * CTCSS tones are used to allow multiple user groups to share a single radio frequency.
 */
public class CTCSSDecoder extends Decoder implements IRealBufferListener, Listener<float[]>
{
    private static final Logger mLog = LoggerFactory.getLogger(CTCSSDecoder.class);

    // Sample rate for input audio
    private static final int SAMPLE_RATE = 8000;

    // Block size for Goertzel algorithm (500ms at 8kHz)
    private static final int BLOCK_SIZE = 4000;

    // Detection threshold - tone power must exceed other powers by this factor
    private static final double THRESHOLD_MULTIPLIER = 2.0;

    // Minimum power threshold to avoid false detections in silence
    private static final double MIN_POWER_THRESHOLD = 0.0001;

    // Low pass filter coefficients for isolating CTCSS tones
    private static float[] sLowPassFilterCoefficients;

    // Standard CTCSS tone frequencies (Hz)
    private static final float[] CTCSS_TONES = {
        67.0f, 69.3f, 71.9f, 74.4f, 77.0f, 79.7f, 82.5f, 85.4f, 88.5f, 91.5f,
        94.8f, 97.4f, 100.0f, 103.5f, 107.2f, 110.9f, 114.8f, 118.8f, 123.0f, 127.3f,
        131.8f, 136.5f, 141.3f, 146.2f, 151.4f, 156.7f, 162.2f, 167.9f, 173.8f, 179.9f,
        186.2f, 192.8f, 199.5f, 203.5f, 206.5f, 210.7f, 218.1f, 225.7f, 229.1f, 233.6f,
        241.8f, 250.3f, 254.1f
    };

    // Goertzel coefficients for each tone
    private final double[] mCoefficients;

    // Goertzel state variables
    private final double[] mQ1;
    private final double[] mQ2;

    // Power values for each tone
    private final double[] mPower;

    // Sample counter
    private int mSampleCount = 0;

    // Currently detected tone
    private CTCSSCode mCurrentCode = null;

    // Low pass filter
    private IRealFilter mLowPassFilter;

    // Consecutive detection counter for debouncing
    private int mConsecutiveDetections = 0;
    private CTCSSCode mPendingCode = null;
    private static final int MIN_CONSECUTIVE_DETECTIONS = 2;

    // === NEW: Tone loss threshold - require consecutive misses before clearing ===
    private int mToneLostCount = 0;
    private static final int TONE_LOST_THRESHOLD = 2;
    
    // === NEW: Periodic rebroadcast for late-starting audio segments ===
    private long mLastBroadcastTime = 0;
    private static final long REBROADCAST_INTERVAL_MS = 1000; // Re-broadcast every 1 second

    static
    {
        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                .sampleRate(8000)
                .gridDensity(16)
                .oddLength(true)
                .passBandCutoff(270)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .stopBandStart(350)
                .stopBandAmplitude(0.0)
                .stopBandRipple(0.03)
                .build();

        try
        {
            RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

            if(designer.isValid())
            {
                sLowPassFilterCoefficients = designer.getImpulseResponse();
            }
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Filter design error", fde);
        }
    }

    /**
     * Constructs an instance
     */
    public CTCSSDecoder()
    {
        int numTones = CTCSS_TONES.length;
        mCoefficients = new double[numTones];
        mQ1 = new double[numTones];
        mQ2 = new double[numTones];
        mPower = new double[numTones];

        // Calculate Goertzel coefficients for each tone
        for(int i = 0; i < numTones; i++)
        {
            double normalizedFreq = 2.0 * Math.PI * CTCSS_TONES[i] / SAMPLE_RATE;
            mCoefficients[i] = 2.0 * Math.cos(normalizedFreq);
        }

        if(sLowPassFilterCoefficients != null)
        {
            mLowPassFilter = FilterFactory.getRealFilter(sLowPassFilterCoefficients);
        }

        resetGoertzel();
    }

    /**
     * Resets the Goertzel state variables
     */
    private void resetGoertzel()
    {
        mSampleCount = 0;
        for(int i = 0; i < CTCSS_TONES.length; i++)
        {
            mQ1[i] = 0.0;
            mQ2[i] = 0.0;
            mPower[i] = 0.0;
        }
    }

    /**
     * Decoder type
     */
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.CTCSS;
    }

    /**
     * Implementation of the IRealBufferListener interface
     */
    @Override
    public Listener<float[]> getBufferListener()
    {
        return this::receive;
    }

    /**
     * Processes demodulated 8 kHz audio samples to detect CTCSS tones
     * @param samples to process
     */
    @Override
    public void receive(float[] samples)
    {
        if(getMessageListener() == null)
        {
            return;
        }

        float[] filtered;
        if(mLowPassFilter != null)
        {
            filtered = mLowPassFilter.filter(samples);
        }
        else
        {
            filtered = samples;
        }

        for(float sample : filtered)
        {
            // Goertzel feedback stage for each tone
            for(int i = 0; i < CTCSS_TONES.length; i++)
            {
                double q0 = mCoefficients[i] * mQ1[i] - mQ2[i] + sample;
                mQ2[i] = mQ1[i];
                mQ1[i] = q0;
            }

            mSampleCount++;

            // Check if block is complete
            if(mSampleCount >= BLOCK_SIZE)
            {
                calculatePowerAndDetect();
                resetState();
            }
        }
    }

 /**
     * Calculate power at each frequency and detect tone
     */
    private void calculatePowerAndDetect()
    {
        double maxPower = 0.0;
        double totalPower = 0.0;
        int maxIndex = -1;

        // Calculate power for each tone using Goertzel feed-forward
        for(int i = 0; i < CTCSS_TONES.length; i++)
        {
            mPower[i] = mQ1[i] * mQ1[i] + mQ2[i] * mQ2[i] - mQ1[i] * mQ2[i] * mCoefficients[i];
            totalPower += mPower[i];

            if(mPower[i] > maxPower)
            {
                maxPower = mPower[i];
                maxIndex = i;
            }
        }

        // Determine if a tone is detected
        CTCSSCode detectedCode = null;

        if(maxIndex >= 0 && maxPower >= MIN_POWER_THRESHOLD)
        {
            double otherPower = totalPower - maxPower;

            if(maxPower > otherPower * THRESHOLD_MULTIPLIER)
            {
                detectedCode = CTCSSCode.fromFrequency(CTCSS_TONES[maxIndex]);
            }
        }

        // Debouncing - require consecutive detections
        if(detectedCode != null)
        {
            // === NEW: Reset lost counter when tone detected ===
            mToneLostCount = 0;
            
            if(detectedCode.equals(mPendingCode))
            {
                mConsecutiveDetections++;
            }
            else
            {
                mPendingCode = detectedCode;
                mConsecutiveDetections = 1;
            }

            if(mConsecutiveDetections >= MIN_CONSECUTIVE_DETECTIONS)
            {
                // === NEW: Periodic rebroadcast logic ===
                long now = System.currentTimeMillis();
                
                // Broadcast if: new tone detected OR enough time has passed since last broadcast
                if(!detectedCode.equals(mCurrentCode) || (now - mLastBroadcastTime) >= REBROADCAST_INTERVAL_MS)
                {
                    mCurrentCode = detectedCode;
                    mLastBroadcastTime = now;
                    getMessageListener().receive(new CTCSSMessage(mCurrentCode, System.currentTimeMillis()));
                }
                // === END NEW ===
            }
        }
        else
        {
            mPendingCode = null;
            mConsecutiveDetections = 0;
            
            // === NEW: Tone loss threshold - don't clear immediately ===
            mToneLostCount++;

            if(mToneLostCount >= TONE_LOST_THRESHOLD && mCurrentCode != null)
            {
                // Send tone lost message before clearing
                getMessageListener().receive(new CTCSSMessage(mCurrentCode, System.currentTimeMillis(), true));
                mCurrentCode = null;
                mLastBroadcastTime = 0; // Reset so next detection broadcasts immediately
            }
            // === END NEW ===
        }
    }

    /**
     * Reset state variables for next block
     */
    private void resetState()
    {
        mSampleCount = 0;
        for(int i = 0; i < CTCSS_TONES.length; i++)
        {
            mQ1[i] = 0.0;
            mQ2[i] = 0.0;
        }
    }
}
