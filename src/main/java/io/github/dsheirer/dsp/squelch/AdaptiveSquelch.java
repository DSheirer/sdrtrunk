/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.dsp.squelch;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import java.util.Arrays;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptive power squelch control that optionally tracks the noise floor and auto-adjusts squelch threshold.
 *
 * Signal power is measured in 5 millisecond blocks so that all timing is independent of the sample rate.  The squelch
 * opens after consecutive blocks at or above the threshold.  It closes on a 20 millisecond power average that spans at
 * least one cycle of the lowest voice frequencies, so that modulation doesn't look like a loss of signal, and which
 * responds in a fixed time regardless of how strong the signal was.
 *
 * Closing occurs in two steps.  Audio is disabled as soon as the power average falls below the close threshold, which
 * limits the noise tail, while the squelch stays open for a hang period to bridge a brief signal fade without ending
 * the call.
 *
 * When auto-track is enabled, the noise floor is the lowest power from the recent intervals that look like noise, using
 * the ratio mean(power^2) / mean(power)^2, which is 2.0 for gaussian noise and approaches 1.0 for a steady carrier.  The
 * threshold is held at a margin over the noise floor, so a threshold below the noise floor can't latch the squelch open.
 * The squelch stays closed until the first noise floor measurement is complete.  A signal that doesn't look like noise
 * is excluded from the noise floor unless it persists for the carrier timeout, after which it's treated as the noise
 * floor so that a dead carrier doesn't hold the squelch open.  The noise floor recovers when the carrier ends.  A carrier
 * with voice modulation, such as a continuous broadcast, doesn't time out, and a timed out carrier that develops voice
 * modulation is removed from the noise floor.
 *
 * Initially modeled on: https://github.com/gnuradio/gnuradio/blob/master/gr-analog/lib/simple_squelch_cc_impl.cc
 */
public class AdaptiveSquelch implements Listener<SourceEvent>
{
    private static final Logger mLog = LoggerFactory.getLogger(AdaptiveSquelch.class);
    private static final double DEFAULT_SAMPLE_RATE = 50000.0;
    private static final double BLOCK_SECONDS = 0.005;
    private static final int OPEN_BLOCKS = 2; //10 milliseconds
    private static final int CLOSE_AVERAGE_BLOCKS = 4; //20 milliseconds
    private static final int HANG_BLOCKS = 20; //100 milliseconds
    private static final int POWER_INTERVAL_BLOCKS = 100; //500 milliseconds
    private static final int NOISE_WINDOW_INTERVALS = 20; //10 seconds
    private static final int CARRIER_TIMEOUT_INTERVALS = 120; //60 seconds
    private static final int MINIMUM_VOICE_INTERVALS = 3;
    private static final double NOISE_POWER_RATIO = 1.85;
    private static final float MINIMUM_SQUELCH_MARGIN = 4.0f; //6 dB
    private static final float CLOSE_THRESHOLD_RATIO = 0.5f; //3 dB below the open threshold
    private final float[] mCloseBlocks = new float[CLOSE_AVERAGE_BLOCKS];
    private int mCloseBlockPointer = 0;
    private float mCloseAccumulator = 0.0f;
    private int mBlockSize = (int)Math.round(DEFAULT_SAMPLE_RATE * BLOCK_SECONDS);
    private int mBlockSampleCount = 0;
    private float mBlockAccumulator = 0.0f;
    private int mOpenCount = 0;
    private int mHangCount = 0;
    private float mPower = 0.0f;
    private double mIntervalPower = 0.0;
    private double mIntervalPowerSquared = 0.0;
    private int mIntervalSampleCount = 0;
    private int mIntervalBlockCount = 0;
    private boolean mFirstIntervalComplete = false;
    private final float[] mNoiseHistory = new float[NOISE_WINDOW_INTERVALS];
    private int mNoiseHistoryPointer = 0;
    private int mNoiseHistoryCount = 0;
    private int mCarrierIntervalCount = 0;
    private int mVoiceIntervalCount = 0;
    private boolean mCarrierTimedOut = false;
    private boolean mCarrierDetected = false;
    private Float mNoiseFloorBeforeCarrier;
    private final VoiceModulationDetector mVoiceModulationDetector;
    private Float mNoiseFloor;
    private Float mLowestNoiseFloor;
    private float mUserThreshold;
    private boolean mSquelch = true;
    private float mSquelchThreshold;
    private boolean mSquelchChanged = false;
    private boolean mAutoTrackNoiseFloor;
    private Listener<SourceEvent> mSourceEventListener;

    /**
     * Constructs an instance
     *
     * @param squelchThreshold in decibels.  Signal power must exceed this threshold value for unsquelch.
     * @param autoTrack the noise floor when squelched and periodically readjust the squelch threshold.
     * @param bandwidth of the channel in hertz
     */
    public AdaptiveSquelch(float squelchThreshold, boolean autoTrack, double bandwidth)
    {
        mVoiceModulationDetector = new VoiceModulationDetector(DEFAULT_SAMPLE_RATE, bandwidth,
                (int)Math.round(DEFAULT_SAMPLE_RATE * BLOCK_SECONDS) * POWER_INTERVAL_BLOCKS);
        setSquelchThreshold(squelchThreshold);
        mAutoTrackNoiseFloor = autoTrack;
    }

    /**
     * Sets the sample rate to size the power measurement blocks.
     * @param sampleRate in hertz
     */
    public void setSampleRate(int sampleRate)
    {
        mBlockSize = Math.max(1, (int)Math.round(sampleRate * BLOCK_SECONDS));
        mVoiceModulationDetector.configure(sampleRate, mBlockSize * POWER_INTERVAL_BLOCKS);
    }

    /**
     * Squelch threshold value
     * @return value in decibels
     */
    public float getSquelchThreshold()
    {
        return toDb(mSquelchThreshold);
    }

    /**
     * Formats the value as decibels
     * @param value to format
     * @return formatted value.
     */
    private float toDb(float value)
    {
        return 10.0f * (float)FastMath.log10(value);
    }

    /**
     * Converts from a decibel value
     * @param valueDb in decibels
     * @return not decibels
     */
    private float fromDb(float valueDb)
    {
        return (float)FastMath.pow(10.0, valueDb / 10.0);
    }

    /**
     * Enables or disables the squelch auto-tracking feature where the squelch threshold is auto-adjusted based on the
     * observed channel power when the signal is not present (ie squelch state).
     * @param autoTrack true to turn on auto-track
     */
    public void setSquelchAutoTrack(boolean autoTrack)
    {
        mAutoTrackNoiseFloor = autoTrack;
        broadcast(SourceEvent.squelchAutoTrack(getSquelchAutoTrack()));

    }

    /**
     * Current squelch auto-track setting.
     * @return true if auto-track is enabled.
     */
    public boolean getSquelchAutoTrack()
    {
        return mAutoTrackNoiseFloor;
    }

    /**
     * Sets the squelch threshold.  With auto-track enabled, the threshold is treated as a margin over the current noise
     * floor, subject to the minimum squelch margin.  The margin is stored relative to the lowest observed noise floor so
     * that a change made while a timed out carrier has raised the noise floor applies once that carrier ends.
     * @param squelchThreshold in decibels
     */
    public void setSquelchThreshold(float squelchThreshold)
    {
        float threshold = fromDb(squelchThreshold);

        if(mAutoTrackNoiseFloor && mNoiseFloor != null)
        {
            mUserThreshold = mLowestNoiseFloor * Math.max(MINIMUM_SQUELCH_MARGIN, threshold / mNoiseFloor);
            updateTrackedThreshold();
        }
        else
        {
            mUserThreshold = threshold;
            applySquelchThreshold(mUserThreshold);
        }
    }

    /**
     * Applies the squelch threshold.  This method is synchronized to coordinate changes requested by the user/UI thread
     * and auto-track changes made by the stream processing thread.
     *
     * @param squelchThreshold value
     */
    private synchronized void applySquelchThreshold(float squelchThreshold)
    {
        mSquelchThreshold = squelchThreshold;
        broadcast(SourceEvent.squelchThreshold(null, getSquelchThreshold()));
    }

    /**
     * Sets the squelch state
     * @param squelch true to squelch and false to unsquelch
     */
    private void setSquelch(boolean squelch)
    {
        mSquelch = squelch;
        mSquelchChanged = true;
    }

    /**
     * Processes a complex IQ sample and changes squelch state when the signal power is above or below the
     * threshold value.
     * @param inphase complex sample component
     * @param quadrature complex sample component
     */
    public void process(float inphase, float quadrature)
    {
        process(inphase * inphase + quadrature * quadrature);
    }

    /**
     * Processes a complex IQ sample and updates the squelch state at the end of each power measurement block.
     * @param magnitude of a complex sample (inphase * inphase + quadrature * quadrature)
     */
    public void process(float magnitude)
    {
        mBlockAccumulator += magnitude;
        mBlockSampleCount++;
        mIntervalPower += magnitude;
        mIntervalPowerSquared += (double)magnitude * magnitude;
        mIntervalSampleCount++;

        //Voice detection is only needed for a carrier, so that channels with only noise don't incur this cost.
        if(mCarrierDetected)
        {
            mVoiceModulationDetector.receive((float)Math.sqrt(magnitude));
        }

        if(mBlockSampleCount >= mBlockSize)
        {
            float power = mBlockAccumulator / mBlockSampleCount;
            mBlockAccumulator = 0.0f;
            mBlockSampleCount = 0;
            processBlock(power);
        }
    }

    /**
     * Updates the squelch state from a block power measurement.
     * @param power of the block
     */
    private void processBlock(float power)
    {
        mCloseAccumulator += power - mCloseBlocks[mCloseBlockPointer];
        mCloseBlocks[mCloseBlockPointer] = power;
        mCloseBlockPointer = (mCloseBlockPointer + 1) % CLOSE_AVERAGE_BLOCKS;

        if(mSquelch)
        {
            //With auto-track, the threshold isn't reliable until there's a noise floor measurement.
            boolean ready = mFirstIntervalComplete || !mAutoTrackNoiseFloor;
            mOpenCount = (ready && power >= mSquelchThreshold) ? mOpenCount + 1 : 0;

            if(mOpenCount >= OPEN_BLOCKS)
            {
                mOpenCount = 0;
                mHangCount = 0;
                //Seed the close average so that it doesn't start with the blocks from before the signal arrived.
                Arrays.fill(mCloseBlocks, power);
                mCloseAccumulator = power * CLOSE_AVERAGE_BLOCKS;
                setSquelch(false);
            }
        }
        else if(mCloseAccumulator / CLOSE_AVERAGE_BLOCKS >= mSquelchThreshold * CLOSE_THRESHOLD_RATIO)
        {
            mHangCount = 0;
        }
        else
        {
            mHangCount++;

            if(mHangCount >= HANG_BLOCKS)
            {
                setSquelch(true);
            }
        }

        updatePowerInterval();
    }

    /**
     * Counts power blocks to provide periodic power level notifications and noise floor updates.
     */
    private void updatePowerInterval()
    {
        mIntervalBlockCount++;

        if(mIntervalBlockCount >= POWER_INTERVAL_BLOCKS)
        {
            double mean = mIntervalPower / mIntervalSampleCount;
            mPower = (float)mean;
            broadcast(SourceEvent.channelPowerLevel(null, 10.0 * Math.log10(mPower)));

            if(mAutoTrackNoiseFloor && mean > 0.0)
            {
                updateNoiseFloor(mIntervalPowerSquared / mIntervalSampleCount / (mean * mean));
            }

            mFirstIntervalComplete = true;
            mIntervalPower = 0.0;
            mIntervalPowerSquared = 0.0;
            mIntervalSampleCount = 0;
            mIntervalBlockCount = 0;
        }
    }

    /**
     * Updates the noise floor history from the current interval power and updates the squelch threshold.
     * @param powerRatio mean(power^2) / mean(power)^2 for the interval
     */
    private void updateNoiseFloor(double powerRatio)
    {
        boolean voice = mVoiceModulationDetector.analyze();
        mCarrierDetected = powerRatio < NOISE_POWER_RATIO;

        if(mCarrierDetected)
        {
            updateCarrier(voice);
        }
        else
        {
            resetCarrierTracking();
            mCarrierTimedOut = false;
            mNoiseFloorBeforeCarrier = null;
            addNoisePower(mPower);
        }

        if(mNoiseHistoryCount > 0)
        {
            updateTrackedThreshold();
        }
    }

    /**
     * Updates the carrier timeout from an interval that doesn't look like noise.  A carrier that lasts for the timeout
     * period is treated as the noise floor, unless it has voice modulation.
     * @param voice true if voice modulation was detected in this interval
     */
    private void updateCarrier(boolean voice)
    {
        mCarrierIntervalCount++;

        if(voice)
        {
            mVoiceIntervalCount++;
        }

        boolean hasVoice = mVoiceIntervalCount >= MINIMUM_VOICE_INTERVALS;

        if(mCarrierTimedOut && hasVoice)
        {
            restoreNoiseFloor();
            resetCarrierTracking();
        }
        else if(mCarrierTimedOut)
        {
            addNoisePower(mPower);
        }
        else if(mCarrierIntervalCount >= CARRIER_TIMEOUT_INTERVALS && !hasVoice)
        {
            //Replace the history so that the carrier takes effect immediately.
            mCarrierTimedOut = true;
            mNoiseFloorBeforeCarrier = mNoiseFloor;
            mNoiseHistoryCount = 0;
            addNoisePower(mPower);
        }

        if(mCarrierIntervalCount >= CARRIER_TIMEOUT_INTERVALS)
        {
            resetCarrierTracking();
        }
    }

    /**
     * Resets the counts for the current carrier evaluation period.
     */
    private void resetCarrierTracking()
    {
        mCarrierIntervalCount = 0;
        mVoiceIntervalCount = 0;
    }

    /**
     * Removes a timed out carrier from the noise floor, restoring the noise floor from before the carrier.  If there's
     * no noise floor from before the carrier, the user threshold is restored until the noise floor can be measured.
     */
    private void restoreNoiseFloor()
    {
        mCarrierTimedOut = false;
        mNoiseHistoryCount = 0;

        if(mNoiseFloorBeforeCarrier != null)
        {
            addNoisePower(mNoiseFloorBeforeCarrier);
            mNoiseFloorBeforeCarrier = null;
        }
        else
        {
            mNoiseFloor = null;
            mLowestNoiseFloor = null;
            applySquelchThreshold(mUserThreshold);
        }
    }

    /**
     * Adds the power to the noise history.
     * @param power of the interval
     */
    private void addNoisePower(float power)
    {
        mNoiseHistory[mNoiseHistoryPointer] = power;
        mNoiseHistoryPointer = (mNoiseHistoryPointer + 1) % NOISE_WINDOW_INTERVALS;
        mNoiseHistoryCount = Math.min(mNoiseHistoryCount + 1, NOISE_WINDOW_INTERVALS);
    }

    /**
     * Sets the squelch threshold to the margin over the current noise floor, where the margin is the larger of the
     * minimum margin and the amount that the user threshold exceeds the lowest observed noise floor.
     */
    private void updateTrackedThreshold()
    {
        float noiseFloor = Float.MAX_VALUE;

        for(int x = 0; x < mNoiseHistoryCount; x++)
        {
            int index = (mNoiseHistoryPointer - 1 - x + NOISE_WINDOW_INTERVALS) % NOISE_WINDOW_INTERVALS;
            noiseFloor = Math.min(noiseFloor, mNoiseHistory[index]);
        }

        mNoiseFloor = noiseFloor;
        mLowestNoiseFloor = (mLowestNoiseFloor == null) ? noiseFloor : Math.min(mLowestNoiseFloor, noiseFloor);
        float threshold = noiseFloor * Math.max(MINIMUM_SQUELCH_MARGIN, mUserThreshold / mLowestNoiseFloor);

        if(threshold != mSquelchThreshold)
        {
            applySquelchThreshold(threshold);
        }
    }

    /**
     * Indicates if the current state is muted
     */
    public boolean isMuted()
    {
        return mSquelch;
    }

    /**
     * Indicates if the current state is unmuted
     */
    public boolean isUnmuted()
    {
        return !mSquelch;
    }

    /**
     * Indicates if audio should be passed.  This is false during the hang period, while the signal is below the close
     * threshold but the squelch is still open.
     */
    public boolean isAudioEnabled()
    {
        return !mSquelch && mHangCount == 0;
    }

    /**
     * Current power level
     * @return current power level in dB
     */
    public float getPower()
    {
        return (float)(10.0 * Math.log10(mPower));
    }

    /**
     * Indicates if the squelch state has changed (muted > unmuted, or vice-versa)
     */
    public boolean isSquelchChanged()
    {
        return mSquelchChanged;
    }

    /**
     * Sets or resets the squelch changed flag
     */
    public void setSquelchChanged(boolean changed)
    {
        mSquelchChanged = changed;
    }

    /**
     * Registers the listener to receive power level notifications and squelch threshold requests
     */
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventListener = listener;
    }

    /**
     * Broadcasts the source event to an optional register listener
     */
    private void broadcast(SourceEvent event)
    {
        if(mSourceEventListener != null)
        {
            mSourceEventListener.receive(event);
        }
    }

    /**
     * Primary method to receive requests for squelch threshold change
     */
    @Override
    public void receive(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case REQUEST_CHANGE_SQUELCH_THRESHOLD:
                setSquelchThreshold(sourceEvent.getValue().floatValue());
                broadcast(SourceEvent.squelchUserThreshold(toDb(mUserThreshold)));
                break;
            case REQUEST_CURRENT_SQUELCH_THRESHOLD:
                broadcast(SourceEvent.squelchThreshold(null, getSquelchThreshold()));
                break;
            case REQUEST_CHANGE_SQUELCH_AUTO_TRACK:
                setSquelchAutoTrack(sourceEvent.getValue().intValue() == 1 ? true : false);
                break;
            case REQUEST_CURRENT_SQUELCH_AUTO_TRACK:
                broadcast(SourceEvent.squelchAutoTrack(getSquelchAutoTrack()));
                break;
        }
    }
}
