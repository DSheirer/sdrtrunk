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

package io.github.dsheirer.dsp.fm;

import io.github.dsheirer.dsp.magnitude.IMagnitudeCalculator;
import io.github.dsheirer.dsp.magnitude.MagnitudeFactory;
import io.github.dsheirer.dsp.squelch.AdaptiveSquelch;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FM Demodulator for demodulating complex samples and producing demodulated floating point samples.
 *
 * Implements listener of source events to process runtime squelch threshold change request events
 * which are forwarded to the power squelch control.
 */
public class SquelchingFMDemodulator implements ISquelchingDemodulator, Listener<SourceEvent>
{
    private static final Logger mLog = LoggerFactory.getLogger(SquelchingFMDemodulator.class);
    private static final float ZERO = 0.0f;
    private final AdaptiveSquelch mAdaptiveSquelch;
    private final IMagnitudeCalculator mMagnitude = MagnitudeFactory.getMagnitudeCalculator();
    private final IDemodulator mFmDemodulator = FmDemodulatorFactory.getFmDemodulator();
    private boolean mSquelchChanged = false;

    /**
     * Creates an FM demodulator instance with a default gain of 1.0.
     *
     * @param alpha decay value of the single pole IIR filter in range: 0.0 - 1.0.  The smaller the alpha value,
     * the slower the squelch response.
     * @param squelchThreshold in decibels.  Signal power must exceed this threshold value for unsquelch.
     * causes immediate mute and unmute.  Set to higher count to prevent mute/unmute flapping.
     * @param squelchAutoTrack to enable the squelch noise floor auto-tracking feature.
     */
    public SquelchingFMDemodulator(float alpha, float squelchThreshold, boolean squelchAutoTrack)
    {
        mAdaptiveSquelch = new AdaptiveSquelch(alpha, squelchThreshold, squelchAutoTrack);
    }

    /**
     * Set or update the sample rate for the squelch to adjust the power level notification rate.
     * @param sampleRate in hertz
     */
    public void setSampleRate(int sampleRate)
    {
        mAdaptiveSquelch.setSampleRate(sampleRate);
    }

    /**
     * Registers the listener to receive notifications of squelch change events from the power squelch.
     */
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mAdaptiveSquelch.setSourceEventListener(listener);
    }

    /**
     * Demodulates the complex (I/Q) sample arrays
     * @param i inphase samples
     * @param q quadrature samples
     * @return demodulated real samples
     */
    @Override
    public float[] demodulate(float[] i, float[] q)
    {
        setSquelchChanged(false);

        float[] demodulated = mFmDemodulator.demodulate(i, q);
        float[] magnitude = mMagnitude.calculate(i, q);

        for(int x = 0; x < magnitude.length; x++)
        {
            mAdaptiveSquelch.process(magnitude[x]);

            if(!(mAdaptiveSquelch.isUnmuted()))
            {
                demodulated[x] = ZERO;
            }

            if(mAdaptiveSquelch.isSquelchChanged())
            {
                setSquelchChanged(true);
            }
        }

        return demodulated;
    }

    /**
     * Sets the threshold for squelch control
     * @param threshold (dB)
     */
    public void setSquelchThreshold(float threshold)
    {
        mAdaptiveSquelch.setSquelchThreshold(threshold);
    }

    @Override
    public void setSquelchAutoTrack(boolean autoTrack)
    {
        mAdaptiveSquelch.setSquelchAutoTrack(autoTrack);
    }

    /**
     * Indicates if the squelch state has changed during the processing of buffer(s)
     */
    public boolean isSquelchChanged()
    {
        return mSquelchChanged;
    }

    /**
     * Sets or resets the squelch changed flag.
     */
    private void setSquelchChanged(boolean changed)
    {
        mSquelchChanged = changed;
    }

    /**
     * Indicates if the squelch state is currently muted
     */
    public boolean isMuted()
    {
        return mAdaptiveSquelch.isMuted();
    }

    @Override
    public void receive(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            //Only forward squelch threshold and auto-track current & change requests
            case REQUEST_CURRENT_SQUELCH_THRESHOLD:
            case REQUEST_CHANGE_SQUELCH_THRESHOLD:
            case REQUEST_CURRENT_SQUELCH_AUTO_TRACK:
            case REQUEST_CHANGE_SQUELCH_AUTO_TRACK:
                mAdaptiveSquelch.receive(sourceEvent);
                break;
        }
    }
}