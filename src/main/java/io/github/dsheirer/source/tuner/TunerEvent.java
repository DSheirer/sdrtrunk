/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.source.tuner;

public class TunerEvent
{
    private Tuner mTuner;
    private Event mEvent;

    public TunerEvent(Tuner tuner, Event event)
    {
        mTuner = tuner;
        mEvent = event;
    }

    public Tuner getTuner()
    {
        return mTuner;
    }

    public Event getEvent()
    {
        return mEvent;
    }

    public enum Event
    {
        CHANNEL_COUNT,
        ERROR_STATE,
        FREQUENCY_UPDATED,
        FREQUENCY_ERROR_UPDATED,
        LOCK_STATE_CHANGE,
        MEASURED_FREQUENCY_ERROR_UPDATED,
        SAMPLE_RATE_UPDATED,

        CLEAR_MAIN_SPECTRAL_DISPLAY,
        REQUEST_MAIN_SPECTRAL_DISPLAY,
        REQUEST_NEW_SPECTRAL_DISPLAY;
    }
}
