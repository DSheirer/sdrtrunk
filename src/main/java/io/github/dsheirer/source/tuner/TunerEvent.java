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

package io.github.dsheirer.source.tuner;

/**
 * Tuner generated events
 */
public class TunerEvent
{
    private Tuner mTuner;
    private Event mEvent;
    private long mCenterFrequency;

    /**
     * Constructs an instance
     * @param tuner that is generating the event
     * @param event from the tuner
     */
    public TunerEvent(Tuner tuner, Event event)
    {
        mTuner = tuner;
        mEvent = event;
    }

    /**
     * Constructs an instance with an optional center frequency for the spectral display to focus on
     * @param tuner that is generating the event
     * @param event from the tuner
     * @param centerFrequency in hertz that the spectral display should center on
     */
    public TunerEvent(Tuner tuner, Event event, long centerFrequency)
    {
        mTuner = tuner;
        mEvent = event;
        mCenterFrequency = centerFrequency;
    }

    /**
     * Tuner source for the event
     */
    public Tuner getTuner()
    {
        return mTuner;
    }

    /**
     * Indicates if the tuner for this event is non-null
     */
    public boolean hasTuner()
    {
        return mTuner != null;
    }

    /**
     * Center frequency hint for the spectral display.  When non-zero, the spectral display should
     * center/zoom on this frequency after processing the event.
     * @return center frequency in hertz, or 0 if not specified
     */
    public long getCenterFrequency()
    {
        return mCenterFrequency;
    }

    /**
     * Indicates if a center frequency hint is specified.
     */
    public boolean hasCenterFrequency()
    {
        return mCenterFrequency != 0;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Tuner Event [").append(getEvent().name()).append("] for tuner [");
        if(hasTuner())
        {
            sb.append(getTuner());
        }
        else
        {
            sb.append("No Tuner");
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * Event type
     */
    public Event getEvent()
    {
        return mEvent;
    }

    public enum Event
    {
        UPDATE_CHANNEL_COUNT,
        UPDATE_FREQUENCY,
        UPDATE_FREQUENCY_ERROR,
        UPDATE_LOCK_STATE,
        UPDATE_MEASURED_FREQUENCY_ERROR,
        UPDATE_SAMPLE_RATE,

        NOTIFICATION_ERROR_STATE,
        NOTIFICATION_SHUTTING_DOWN,

        REQUEST_CLEAR_MAIN_SPECTRAL_DISPLAY,
        REQUEST_ENABLE_RSP_SLAVE_DEVICE,
        REQUEST_MAIN_SPECTRAL_DISPLAY,
        REQUEST_NEW_SPECTRAL_DISPLAY;
    }
}
