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

import io.github.dsheirer.dsp.filter.iir.SinglePoleIirFilter;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import java.time.Duration;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptive squelch control that optionally tracks the noise floor and auto-adjusts squelch threshold.
 *
 * Initially modeled on: https://github.com/gnuradio/gnuradio/blob/master/gr-analog/lib/simple_squelch_cc_impl.cc
 */
public class AdaptiveSquelch implements Listener<SourceEvent>
{
    private static final Logger mLog = LoggerFactory.getLogger(AdaptiveSquelch.class);
    private static final long AUTO_ADJUST_PERIOD_MS = Duration.ofSeconds(5).toMillis();
    private SinglePoleIirFilter mFilter;
    private float mPower = 0.0f;
    private boolean mSquelch;
    private float mSquelchThreshold;
    private Float mNoiseFloor;
    private float mSquelchBuffer;
    private boolean mSquelchChanged = false;
    private int mPowerLevelBroadcastCount = 0;
    private int mPowerLevelBroadcastThreshold;
    private boolean mAutoTrackNoiseFloor;
    private long mLastAutoAdjustTimestamp;
    private Listener<SourceEvent> mSourceEventListener;

    /**
     * Constructs an instance
     *
     * Testing against a 12.5 kHz analog FM modulated signal, the following parameters provided a
     * good responsiveness.  A threshold of -80.0 dB seemed to trigger significant flapping during un-squelching.
     *   - alpha: 0.0001
     *   - threshold: 78.0 dB
     *
     * @param alpha decay value of the single pole IIR filter in range: 0.0 - 1.0.  The smaller the alpha value,
     * the slower the squelch response.
     * @param squelchThreshold in decibels.  Signal power must exceed this threshold value for unsquelch.
     * @param autoTrack the noise floor when squelched and periodically readjust the squelch threshold.
     */
    public AdaptiveSquelch(float alpha, float squelchThreshold, boolean autoTrack)
    {
        mFilter = new SinglePoleIirFilter(alpha);
        setSquelchThreshold(squelchThreshold);
        mAutoTrackNoiseFloor = autoTrack;
        mLastAutoAdjustTimestamp = System.currentTimeMillis();
        mPowerLevelBroadcastThreshold = 25000; //Based on a default sample rate of 50 kHz, so 2x/second
    }

    /**
     * Sets the sample rate to effect the frequency of power level notifications where the notifications are
     * sent twice a second.
     * @param sampleRate in hertz
     */
    public void setSampleRate(int sampleRate)
    {
        mPowerLevelBroadcastThreshold = sampleRate / 2;
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
     * Sets the squelch threshold
     * @param squelchThreshold in decibels
     */
    public void setSquelchThreshold(float squelchThreshold)
    {
        float squelchThresholdDb = fromDb(squelchThreshold);
        setSquelchThreshold(squelchThresholdDb, true);
    }

    /**
     * Sets the squelch threshold.  This method is synchronized to coordinate changes requested by the user/UI thread
     * and auto-track changes made by the stream processing thread.
     *
     * @param squelchThreshold value
     * @param resetNoiseFloor to reset the noise floor and squelch threshold
     */
    private synchronized void setSquelchThreshold(float squelchThreshold, boolean resetNoiseFloor)
    {
        mSquelchThreshold = squelchThreshold;

        if(resetNoiseFloor)
        {
            //Set noise floor to null so that both the noise floor and the squelch buffer get updated after first power reading
            mNoiseFloor = null;
        }

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
     * Processes a complex IQ sample and changes squelch state when the signal power is above or below the
     * threshold value.
     * @param magnitude of a complex sample (inphase * inphase + quadrature * quadrature)
     */
    public void process(float magnitude)
    {
        mPower = mFilter.filter(magnitude);

        if(mSquelch && mPower >= mSquelchThreshold)
        {
            setSquelch(false);
        }
        else if(!mSquelch && mPower < mSquelchThreshold)
        {
            setSquelch(true);
        }

        mPowerLevelBroadcastCount++;
        if(mPowerLevelBroadcastCount % mPowerLevelBroadcastThreshold == 0)
        {
            mPowerLevelBroadcastCount = 0;
            broadcast(SourceEvent.channelPowerLevel(null, 10.0 * Math.log10(mPower)));

            if(mSquelch)
            {
                adjustSquelchThreshold();
            }
        }
    }

    /**
     * With the user specified squelch threshold, establishes a squelch buffer (delta) from the actual noise floor and
     * then automatically updates the squelch threshold to be the current noise floor plus this threshold value.
     *
     * Note: this method should only be invoked from a squelch (true) state.
     */
    private void adjustSquelchThreshold()
    {
        if(mAutoTrackNoiseFloor)
        {
            if(mNoiseFloor == null)
            {
                //We set the squelch buffer just once, after the squelch threshold is set and noise floor is set to null
                mSquelchBuffer = mSquelchThreshold - mPower;
            }

            mNoiseFloor = mPower;

            if((System.currentTimeMillis() - mLastAutoAdjustTimestamp) > AUTO_ADJUST_PERIOD_MS)
            {
                setSquelchThreshold(mNoiseFloor + mSquelchBuffer, false);
                mLastAutoAdjustTimestamp = System.currentTimeMillis();
            }
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
