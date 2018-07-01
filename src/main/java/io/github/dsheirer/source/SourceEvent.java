/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.source;

import java.util.EnumSet;

public class SourceEvent
{
    public enum Event
    {
        NOTIFICATION_CHANNEL_COUNT_CHANGE,
        NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE,
        NOTIFICATION_CHANNEL_SAMPLE_RATE_CHANGE,
        NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_LOCKED,
        NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_UNLOCKED,
        NOTIFICATION_FREQUENCY_CHANGE,
        NOTIFICATION_FREQUENCY_CORRECTION_CHANGE,
        NOTIFICATION_SAMPLE_RATE_CHANGE,
        NOTIFICATION_STOP_SAMPLE_STREAM,

        REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE,
        REQUEST_FREQUENCY_CHANGE,
        REQUEST_SOURCE_DISPOSE,
        REQUEST_START_SAMPLE_STREAM,
        REQUEST_STOP_SAMPLE_STREAM;

        public static EnumSet<Event> NOTIFICATION_EVENTS =
            EnumSet.range(NOTIFICATION_CHANNEL_COUNT_CHANGE, NOTIFICATION_STOP_SAMPLE_STREAM);
        public static EnumSet<Event> REQUEST_EVENTS =
            EnumSet.range(NOTIFICATION_CHANNEL_COUNT_CHANGE, NOTIFICATION_SAMPLE_RATE_CHANGE);
    }

    private Event mEvent;
    private Number mValue;
    private Source mSource;
    private String mEventDescription;

    /**
     * Private constructor.  Use the static constructor methods to create an event.
     */
    private SourceEvent(Event event, Number value, String eventDescription)
    {
        mEvent = event;
        mValue = value;
        mEventDescription = eventDescription;
    }

    /**
     * Private constructor.  Use the static constructor methods to create an event.
     */
    private SourceEvent(Event event, Number value)
    {
        mEvent = event;
        mValue = value;
    }

    /**
     * Private constructor.  Use the static constructor methods to create an event.
     */
    private SourceEvent(Event event, Source source)
    {
        mEvent = event;
        mSource = source;
    }

    /**
     * Event description.
     */
    public Event getEvent()
    {
        return mEvent;
    }

    /**
     * Indicates if this is a notification event
     */
    public boolean isNotificationEvent()
    {
        return Event.NOTIFICATION_EVENTS.contains(mEvent);
    }

    /**
     * Indicates if this is a request event
     */
    public boolean isRequestEvent()
    {
        return Event.REQUEST_EVENTS.contains(mEvent);
    }

    /**
     * Value associated with the event.
     */
    public Number getValue()
    {
        return mValue;
    }

    public boolean hasValue()
    {
        return mValue != null;
    }

    /**
     * Source associated with the event
     */
    public Source getSource()
    {
        return mSource;
    }

    /**
     * Indicates if this event contains an optional source
     */
    public boolean hasSource()
    {
        return mSource != null;
    }

    /**
     * Creates a new locked state change event
     */
    public static SourceEvent lockedState()
    {
        return new SourceEvent(Event.NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_LOCKED, 1);
    }

    /**
     * Creates a new unlocked state change event
     */
    public static SourceEvent unlockedState()
    {
        return new SourceEvent(Event.NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_UNLOCKED, 0);
    }

    /**
     * Creates a new frequency change event
     *
     * @param frequency in hertz
     */
    public static SourceEvent frequencyChange(long frequency)
    {
        return new SourceEvent(Event.NOTIFICATION_FREQUENCY_CHANGE, frequency);
    }

    /**
     * Creates a new frequency change event
     *
     * @param frequency in hertz
     * @param eventDescription to add a tag to the event
     */
    public static SourceEvent frequencyChange(long frequency, String eventDescription)
    {
        return new SourceEvent(Event.NOTIFICATION_FREQUENCY_CHANGE, frequency, eventDescription);
    }

    /**
     * Creates a new frequency correction change event
     *
     * @param frequencyCorrection in hertz
     */
    public static SourceEvent frequencyCorrectionChange(long frequencyCorrection)
    {
        return new SourceEvent(Event.NOTIFICATION_FREQUENCY_CORRECTION_CHANGE, frequencyCorrection);
    }

    /**
     * Creates a new channel frequency correction change event
     *
     * @param frequencyCorrection in hertz
     */
    public static SourceEvent channelFrequencyCorrectionChange(long frequencyCorrection)
    {
        return new SourceEvent(Event.NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE, frequencyCorrection);
    }

    /**
     * Creates a new sample rate change event
     *
     * @param sampleRate in hertz
     */
    public static SourceEvent sampleRateChange(double sampleRate)
    {
        return new SourceEvent(Event.NOTIFICATION_SAMPLE_RATE_CHANGE, sampleRate);
    }

    /**
     * Creates a new sample rate change event
     *
     * @param sampleRate in hertz
     * @param eventDescription to include with the event
     */
    public static SourceEvent sampleRateChange(double sampleRate, String eventDescription)
    {
        return new SourceEvent(Event.NOTIFICATION_SAMPLE_RATE_CHANGE, sampleRate, eventDescription);
    }

    /**
     * Creates a new channel sample rate change event
     *
     * @param sampleRate in hertz
     */
    public static SourceEvent channelSampleRateChange(double sampleRate)
    {
        return new SourceEvent(Event.NOTIFICATION_CHANNEL_SAMPLE_RATE_CHANGE, sampleRate);
    }

    /**
     * Creates a new channel count change event
     *
     * @param channelCount
     */
    public static SourceEvent channelCountChange(int channelCount)
    {
        return new SourceEvent(Event.NOTIFICATION_CHANNEL_COUNT_CHANGE, channelCount);
    }

    /**
     * Creates a new frequency change request event
     *
     * @param frequency requested
     */
    public static SourceEvent frequencyRequest(long frequency)
    {
        return new SourceEvent(Event.REQUEST_FREQUENCY_CHANGE, frequency);
    }

    /**
     * Creates a new channel frequency correction change request event
     *
     * @param frequency requested
     */
    public static SourceEvent channelFrequencyCorrectionRequest(long frequency)
    {
        return new SourceEvent(Event.REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE, frequency);
    }

    /**
     * Creates a new start sample stream request event
     */
    public static SourceEvent startSampleStreamRequest(Source source)
    {
        return new SourceEvent(Event.REQUEST_START_SAMPLE_STREAM, source);
    }

    /**
     * Creates a new stop sample stream request event
     */
    public static SourceEvent stopSampleStreamRequest(Source source)
    {
        return new SourceEvent(Event.REQUEST_STOP_SAMPLE_STREAM, source);
    }

    /**
     * Creates a new stop sample stream request event
     */
    public static SourceEvent stopSampleStreamNotification(Source source)
    {
        return new SourceEvent(Event.NOTIFICATION_STOP_SAMPLE_STREAM, source);
    }

    /**
     * Creates a request to dispose of the source.  This is normally used for TunerChannelSource to notify the
     * parent source manager that the channel is no longer needed
     */
    public static SourceEvent sourceDisposeRequest(Source source)
    {
        return new SourceEvent(Event.REQUEST_SOURCE_DISPOSE, source);
    }

    @Override
    public String toString()
    {
        return "SOURCE EVENT:" + mEvent +
               " VALUE:" + (mValue != null ? mValue : "(empty)") +
               " SOURCE:" + (mSource != null ? mSource.getClass() : "(null)") +
               " DESCRIPTION:" + (mEventDescription != null ? mEventDescription : "");
    }
}
