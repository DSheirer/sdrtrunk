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

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;

/**
 * Interface for FM demodulator that provides squelch control.
 */
public interface ISquelchingDemodulator extends IDemodulator
{
    /**
     * Sets the sample rate so that the squelch can adjust the channel power level broadcast notification rate
     * @param sampleRate in hertz
     */
    void setSampleRate(int sampleRate);

    /**
     * Indicates if the squelch state has changed.
     */
    boolean isSquelchChanged();

    /**
     * Indicates if the squelch state is currently muted
     */
    boolean isMuted();

    /**
     * Sets the threshold to use for power squelch
     * @param threshold in decibels
     */
    void setSquelchThreshold(float threshold);

    /**
     * Enables or disables the squelch auto-tracking feature where the squelch threshold is auto-adjusted based on the
     * observed channel power when the signal is not present (ie squelch state).
     * @param autoTrack true to enable
     */
    void setSquelchAutoTrack(boolean autoTrack);

    /**
     * Registers the listener to receive notifications of squelch change events from the power squelch.
     */
    void setSourceEventListener(Listener<SourceEvent> listener);

    /**
     * Receives a source event
     */
    void receive(SourceEvent sourceEvent);
}

