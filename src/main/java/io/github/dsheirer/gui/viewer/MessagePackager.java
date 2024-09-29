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

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventSnapshot;
import io.github.dsheirer.module.decode.event.IDecodeEvent;

/**
 * Utility for combining a message and decoder state events.
 */
public class MessagePackager
{
    private MessagePackage mMessagePackage;

    /**
     * Constructs an instance
     */
    public MessagePackager()
    {
    }

    /**
     * Adds an audio segment.
     * @param audioSegment to add
     */
    public void add(AudioSegment audioSegment)
    {
        if(mMessagePackage != null)
        {
            mMessagePackage.add(audioSegment);
        }
    }

    /**
     * Adds the message and creates a new MessageWithEvents instance, wrapping the message, ready to also receive any
     * decode events and decoder state events.  The previous message with events is overwritten.
     * @param message to wrap.
     */
    public void add(IMessage message)
    {
        mMessagePackage = new MessagePackage(message);
    }

    /**
     * Access the current message with events.
     */
    public MessagePackage getMessageWithEvents()
    {
        return mMessagePackage;
    }

    /**
     * Adds the decoder state event to the current message with events.
     * @param event to add
     */
    public void add(DecoderStateEvent event)
    {
        if(mMessagePackage != null)
        {
            mMessagePackage.add(event);
        }
    }

    /**
     * Adds the decode event to the current message with events.
     * @param event to add
     */
    public void add(IDecodeEvent event)
    {
        if(mMessagePackage != null)
        {
            if(event instanceof DecodeEvent decodeEvent)
            {
                try
                {
                    DecodeEventSnapshot snapshot = decodeEvent.getSnapshot();
                    mMessagePackage.add(snapshot);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Subscription to receive channel start processing requests via the event bus from the traffic channel manager
     * @param request sent from the traffic channel manager.
     */
    @Subscribe
    public void process(ChannelStartProcessingRequest request)
    {
        mMessagePackage.add(request);
    }
}
