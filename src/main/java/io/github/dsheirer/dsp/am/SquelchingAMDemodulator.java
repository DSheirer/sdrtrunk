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

package io.github.dsheirer.dsp.am;

import io.github.dsheirer.dsp.fm.ISquelchingDemodulator;
import io.github.dsheirer.dsp.magnitude.IMagnitudeCalculator;
import io.github.dsheirer.dsp.magnitude.MagnitudeFactory;
import io.github.dsheirer.dsp.squelch.AdaptiveSquelch;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;

/**
 * AM demodulator with integrated squelch control.
 */
public class SquelchingAMDemodulator implements ISquelchingDemodulator, Listener<SourceEvent>
{
    private static final float ZERO = 0.0f;
    private final IMagnitudeCalculator mMagnitudeCalculator = MagnitudeFactory.getMagnitudeCalculator();
    private final IAmDemodulator mAmDemodulator;
    private final AdaptiveSquelch mAdaptiveSquelch;
    private boolean mSquelchChanged = false;

    /**
     * Constructs an instance
     * @param gain to apply to the demodulated AM samples (e.g. 500.0f)
     * @param alpha decay value of the single pole IIR filter in range: 0.0 - 1.0.  The smaller the alpha value,
     * the slower the squelch response.
     * @param squelchThreshold in decibels.  Signal power must exceed this threshold value for unsquelch.
     * @param squelchAutoTrack to enable the squelch noise floor auto tracking feature.
     */
    public SquelchingAMDemodulator(float gain, float alpha, float squelchThreshold, boolean squelchAutoTrack)
    {
        mAmDemodulator = AmDemodulatorFactory.getAmDemodulator(gain);
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
     * Demodulates the complex sample arrays.
     * @param i inphase sample array
     * @param q quadrature sample array
     * @return demodulated AM samples with gain applied.
     */
    public float[] demodulate(float[] i, float[] q)
    {
        mAdaptiveSquelch.setSquelchChanged(false);
        setSquelchChanged(false);

        float[] magnitude = mMagnitudeCalculator.calculate(i, q);
        float[] demodulated = mAmDemodulator.demodulateMagnitude(magnitude);

        for(int x = 0; x < i.length; x++)
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

    /**
     * Process source events initiated by the timer and end-user.
     * @param sourceEvent to process.
     */
    @Override
    public void receive(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            //Only forward squelch threshold & auto-track request events
            case REQUEST_CURRENT_SQUELCH_THRESHOLD:
            case REQUEST_CHANGE_SQUELCH_THRESHOLD:
            case REQUEST_CURRENT_SQUELCH_AUTO_TRACK:
            case REQUEST_CHANGE_SQUELCH_AUTO_TRACK:
            {
                mAdaptiveSquelch.receive(sourceEvent);
            }
        }
    }
}
