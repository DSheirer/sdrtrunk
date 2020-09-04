/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
package io.github.dsheirer.channel.state;

import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.string.SimpleStringIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic decoder channel state - provides the minimum channel state functionality
 * to support an always un-squelched audio decoder.
 */
public class AlwaysUnsquelchedDecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(AlwaysUnsquelchedDecoderState.class);
    private static final String NO_SQUELCH = "No Squelch";
    private Identifier mChannelNameIdentifier;

    private DecoderType mDecoderType;
    private String mChannelName;

    public AlwaysUnsquelchedDecoderState(DecoderType decoderType, String channelName)
    {
        mDecoderType = decoderType;
        mChannelName = (channelName != null && !channelName.isEmpty()) ? channelName : decoderType.name() + " CHANNEL";
        mChannelNameIdentifier = new SimpleStringIdentifier(mChannelName, IdentifierClass.USER, Form.CHANNEL_NAME, Role.TO);
    }

    @Override
    public void init()
    {
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary\n");
        sb.append("\tDecoder:\t");
        sb.append(mDecoderType);
        sb.append("\n\n");

        return sb.toString();
    }

    @Override
    public void receive(IMessage t)
    {
        /* Not implemented */
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        if(event.getEvent() == Event.REQUEST_RESET)
        {
            getIdentifierCollection().update(mChannelNameIdentifier);
        }
    }

    @Override
    public DecoderType getDecoderType()
    {
        return mDecoderType;
    }

    @Override
    public void start()
    {
        broadcast(new DecoderStateEvent(this, Event.REQUEST_ALWAYS_UNSQUELCH, State.IDLE));
        getIdentifierCollection().update(mChannelNameIdentifier);
    }

    @Override
    public void stop()
    {
    }
}
