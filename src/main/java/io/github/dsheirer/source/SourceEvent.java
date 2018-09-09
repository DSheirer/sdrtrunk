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
        NOTIFICATION_MEASURED_FREQUENCY_ERROR,
        NOTIFICATION_MEASURED_FREQUENCY_ERROR_SYNC_LOCKED,
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
    private SourceEvent(Event event, Source source, Number value, String eventDescription)
    {
        mEvent = event;
        mSource = source;
        mValue = value;
        mEventDescription = eventDescription;
    }

    /**
     * Private constructor.  Use the static constructor methods to create an event.
     */
    private SourceEvent(Event event, Number value, String eventDescription)
    {
        this(event, null, value, eventDescription);
    }

    /**
     * Private constructor.  Use the static constructor methods to create an event.
     */
    private SourceEvent(Event event, Number value)
    {
        this(event, value, null);
    }

    /**
     * Private constructor.  Use the static constructor methods to create an event.
     */
    private SourceEvent(Event event, Source source)
    {
        this(event, source, 0);
    }

    private SourceEvent(Event event, Source source, Number value)
    {
        this(event, source, value, null);
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
     * Sets the source associated with this event
     */
    public void setSource(Source source)
    {
        mSource = source;
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
     * Creates a new frequency error measurement notification event.
     *
     * @param frequencyError in hertz
     */
    public static SourceEvent frequencyErrorMeasurement(long frequencyError)
    {
        return new SourceEvent(Event.NOTIFICATION_MEASURED_FREQUENCY_ERROR, frequencyError);
    }

    /**
     * Creates a new frequency error measurement notification event.  This event is different from
     * the raw frequency error measurement and indicates that the current state of the PLL is
     * locked and tracking the signal.
     *
     * @param frequencyError in hertz
     */
    public static SourceEvent frequencyErrorMeasurementSyncLocked(long frequencyError, String eventDescription)
    {
        return new SourceEvent(Event.NOTIFICATION_MEASURED_FREQUENCY_ERROR_SYNC_LOCKED, frequencyError, eventDescription);
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
     * Creates a new start sample stream request event.  This method uses the current system time in
     * milliseconds as the requested sample start time.
     */
    public static SourceEvent startSampleStreamRequest(Source source)
    {
        return startSampleStreamRequest(source, System.currentTimeMillis());
    }

    /**
     * Creates a new start sample stream request event and requests that samples (when available) for the
     * specified timestamp be pre-loaded into the source.  The timestamp only applies for sources that incorporate
     * a sample delay buffer.
     */
    public static SourceEvent startSampleStreamRequest(Source source, long timestamp)
    {
        return new SourceEvent(Event.REQUEST_START_SAMPLE_STREAM, source, timestamp);
    }

    /**
     * Creates a new stop sample stream request event
     */
    public static SourceEvent stopSampleStreamRequest(Source source)
    {
        return new SourceEvent(Event.REQUEST_STOP_SAMPLE_STREAM, source);
    }

    /**
     * Creates a new stop sample stream notification event
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
               " SOURCE:" + (mSource != null ? mSource.toString() : "(null)") +
               " DESCRIPTION:" + (mEventDescription != null ? mEventDescription : "");
    }
}
