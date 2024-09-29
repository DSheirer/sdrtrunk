/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.gui.viewer;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.event.DecodeEventSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class that combines a single message with any decoder state events and decoded events that were produced by a
 * decoder state.
 */
public class MessagePackage
{
    private IMessage mMessage;
    private List<DecoderStateEvent> mDecoderStateEvents = new ArrayList<>();
    private List<DecodeEventSnapshot> mDecodeEvents = new ArrayList<>();
    private ChannelStartProcessingRequest mChannelStartProcessingRequest;
    private AudioSegment mAudioSegment;

    /**
     * Constructs an instance
     * @param message for this instance
     */
    public MessagePackage(IMessage message)
    {
        mMessage = message;
    }

    /**
     * Message for this instance
     */
    public IMessage getMessage()
    {
        return mMessage;
    }

    /**
     * List of decoder state events
     */
    public List<DecoderStateEvent> getDecoderStateEvents()
    {
        return mDecoderStateEvents;
    }

    /**
     * Adds the decoder state event to this instance
     */
    public void add(DecoderStateEvent event)
    {
        mDecoderStateEvents.add(event);
    }

    /**
     * List of decode event snapshots
     */
    public List<DecodeEventSnapshot> getDecodeEvents()
    {
        return mDecodeEvents;
    }

    /**
     * Adds the decode event to this instance
     */
    public void add(DecodeEventSnapshot event)
    {
        mDecodeEvents.add(event);
    }

    /**
     * Adds the channel start processing request
     */
    public void add(ChannelStartProcessingRequest request)
    {
        mChannelStartProcessingRequest = request;
    }

    /**
     * Message timeslot
     */
    public int getTimeslot()
    {
        return getMessage().getTimeslot();
    }

    /**
     * Message timestamp
     */
    public long getTimestamp()
    {
        return getMessage().getTimestamp();
    }

    /**
     * Message validity flag
     */
    public boolean isValid()
    {
        return getMessage().isValid();
    }

    /**
     * Message string representation
     */
    @Override
    public String toString()
    {
        return getMessage().toString();
    }

    /**
     * Count of decode events
     */
    public int getDecodeEventCount()
    {
        return mDecodeEvents.size();
    }

    /**
     * Count of decoder state events
     */
    public int getDecoderStateEventCount()
    {
        return mDecoderStateEvents.size();
    }

    /**
     * Count (0 or 1) of channel start processing requests.
     */
    public int getChannelStartProcessingRequestCount()
    {
        return mChannelStartProcessingRequest == null ? 0 : 1;
    }

    /**
     * Count (0 or 1) of audio segment.
     */
    public int getAudioSegmentCount()
    {
        return mAudioSegment == null ? 0 : 1;
    }

    public ChannelStartProcessingRequest getChannelStartProcessingRequest()
    {
        return mChannelStartProcessingRequest;
    }

    /**
     * Generated audio segment.
     * @return segment or null.
     */
    public AudioSegment getAudioSegment()
    {
        return mAudioSegment;
    }

    /**
     * Adds the audio segment to the package
     * @param audioSegment to add
     */
    public void add(AudioSegment audioSegment)
    {
        if(mAudioSegment != null)
        {
            throw new IllegalStateException("AudioSegment already set");
        }

        mAudioSegment = audioSegment;
    }
}
