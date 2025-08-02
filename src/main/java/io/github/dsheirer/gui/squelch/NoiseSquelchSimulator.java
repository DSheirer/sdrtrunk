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

package io.github.dsheirer.gui.squelch;

import io.github.dsheirer.dsp.squelch.INoiseSquelchController;
import io.github.dsheirer.dsp.squelch.NoiseSquelchState;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simple simulator with integrated timer to produce random noise squelch state values to support debug and testing of
 * noise squelch view.
 */
public class NoiseSquelchSimulator implements INoiseSquelchController
{
    private Listener<NoiseSquelchState> mListener;
    private float mNoiseOpenThreshold = 0.1f;
    private float mNoiseCloseThreshold = 0.2f;
    private int mHysteresisOpenThreshold = 4;
    private int mHysteresisCloseThreshold = 6;
    private int mHysteresis;
    private int mHysteresisCounter;
    private boolean mSquelchOverride;
    private boolean mSquelch;
    private ScheduledFuture<?> mTimerFuture;
    private Random mRandom = new Random();

    @Override
    public void setNoiseSquelchStateListener(Listener<NoiseSquelchState> listener)
    {
        mListener = listener;

        if(mListener != null)
        {
            mTimerFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::dispatch, 0, 90, TimeUnit.MILLISECONDS);
        }
        else if(mTimerFuture != null)
        {
            mTimerFuture.cancel(false);
            mTimerFuture = null;
        }
    }

    /**
     * Sets the open and close noise thresholds in range 0.0 to 1.0, recommend: 0.1 open and 0.2 close
     * @param open for the open noise variance calculation.
     * @param close for the close noise variance calculation.
     */
    @Override
    public void setNoiseThreshold(float open, float close)
    {
        mNoiseOpenThreshold = open;
        mNoiseCloseThreshold = close;
    }

    @Override
    public void setHysteresisThreshold(int open, int close)
    {
        if(mHysteresisOpenThreshold != open || mHysteresisCloseThreshold != close)
        {
            System.out.println("Setting hysteresis thresholds - open:" + open + " close:" + close);
            mHysteresisOpenThreshold = open;
            mHysteresisCloseThreshold = close;
        }
    }

    @Override
    public void setSquelchOverride(boolean override)
    {
        mSquelchOverride = override;
    }

    private void dispatch()
    {
        if(mListener != null)
        {
            mHysteresisCounter++;
            if(mHysteresisCounter > 10)
            {
                mHysteresis = (int)(mRandom.nextFloat() * 10);
                mHysteresis = Math.min(mHysteresis, mHysteresisCloseThreshold);
            }
            float noise = mRandom.nextFloat() * 0.5f;
            mSquelch = noise > mNoiseOpenThreshold;
            mListener.receive(new NoiseSquelchState(mSquelch, mSquelchOverride, noise, mNoiseOpenThreshold,
                    mNoiseCloseThreshold, mHysteresis, mHysteresisOpenThreshold, mHysteresisCloseThreshold));
        }
    }
}
