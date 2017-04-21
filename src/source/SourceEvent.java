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
package source;

public class SourceEvent
{
    public enum Event
    {
        NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE,
        NOTIFICATION_CHANNEL_SAMPLE_RATE_CHANGE,
        NOTIFICATION_FREQUENCY_CHANGE,
        NOTIFICATION_FREQUENCY_CORRECTION_CHANGE,
        NOTIFICATION_POLYPHASE_CHANNEL_COUNT_CHANGE,
        NOTIFICATION_SAMPLE_RATE_CHANGE,

        REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE,
        REQUEST_FREQUENCY_CHANGE,
        REQUEST_START_SAMPLE_STREAM,
        REQUEST_STOP_SAMPLE_STREAM;
    }

    private Event mEvent;
    private Number mValue;
    private Source mSource;

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
     * Creates a new frequency change event
     *
     * @param frequency in hertz
     */
    public static SourceEvent frequencyChange(long frequency)
    {
        return new SourceEvent(Event.NOTIFICATION_FREQUENCY_CHANGE, frequency);
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
    public static SourceEvent sampleRateChange(int sampleRate)
    {
        return new SourceEvent(Event.NOTIFICATION_SAMPLE_RATE_CHANGE, sampleRate);
    }

    /**
     * Creates a new channel sample rate change event
     *
     * @param sampleRate in hertz
     */
    public static SourceEvent channelSampleRateChange(int sampleRate)
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
        return new SourceEvent(Event.NOTIFICATION_POLYPHASE_CHANNEL_COUNT_CHANGE, channelCount);
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
    public static SourceEvent startSampleStream(Source source)
    {
        return new SourceEvent(Event.REQUEST_START_SAMPLE_STREAM, source);
    }

    /**
     * Creates a new stop sample stream request event
     */
    public static SourceEvent stopSampleStream(Source source)
    {
        return new SourceEvent(Event.REQUEST_STOP_SAMPLE_STREAM, source);
    }
}
