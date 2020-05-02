/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast;

public class BroadcastEvent
{
    private BroadcastConfiguration mBroadcastConfiguration;
    private AbstractAudioBroadcaster mAudioBroadcaster;
    private Event mEvent;

    /**
     * AliasEvent - event describing any changes to an alias
     *
     * @param configuration - alias that changed
     * @param event - change event
     */
    public BroadcastEvent(BroadcastConfiguration configuration, Event event)
    {
        mBroadcastConfiguration = configuration;
        mEvent = event;
    }

    public BroadcastEvent(AbstractAudioBroadcaster audioBroadcaster, Event event)
    {
        mAudioBroadcaster = audioBroadcaster;
        mEvent = event;
    }

    public BroadcastConfiguration getBroadcastConfiguration()
    {
        return mBroadcastConfiguration;
    }

    public AbstractAudioBroadcaster getAudioBroadcaster()
    {
        return mAudioBroadcaster;
    }

    /**
     * Indicates if this event pertains to an audio broadcaster
     */
    public boolean isAudioBroadcasterEvent()
    {
        return mAudioBroadcaster != null;
    }

    /**
     * Indicates if this event pertains to a broadcast configuration
     */
    public boolean isBroadcastConfigurationEvent()
    {
        return mBroadcastConfiguration != null;
    }

    public Event getEvent()
    {
        return mEvent;
    }

    /**
     * Channel events to describe the specific event
     */
    public enum Event
    {
        BROADCASTER_ADD,
        BROADCASTER_QUEUE_CHANGE,
        BROADCASTER_STATE_CHANGE,
        BROADCASTER_STREAMED_COUNT_CHANGE,
        BROADCASTER_AGED_OFF_COUNT_CHANGE,
        BROADCASTER_ERROR_COUNT_CHANGE,
        BROADCASTER_DELETE,

        CONFIGURATION_ADD,
        CONFIGURATION_CHANGE,
        CONFIGURATION_DELETE;
    }
}
