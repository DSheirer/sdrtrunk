/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package ua.in.smartjava.channel.state;

import ua.in.smartjava.channel.metadata.Attribute;
import ua.in.smartjava.channel.metadata.AttributeChangeRequest;
import ua.in.smartjava.channel.state.DecoderStateEvent.Event;
import ua.in.smartjava.message.Message;
import ua.in.smartjava.module.decode.DecoderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Basic decoder ua.in.smartjava.channel state - provides the minimum ua.in.smartjava.channel state functionality
 * to support an always un-squelched ua.in.smartjava.audio decoder.
 */
public class AlwaysUnsquelchedDecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(AlwaysUnsquelchedDecoderState.class);

    private DecoderType mDecoderType;
    private String mChannelName;

    public AlwaysUnsquelchedDecoderState(DecoderType decoderType, String channelName)
    {
        super(null);

        mDecoderType = decoderType;
        mChannelName = channelName;
    }

    @Override
    public void init()
    {
    }

    @Override
    public void reset()
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
    public void receive(Message t)
    {
		/* Not implemented */
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        if(event.getEvent() == Event.RESET)
        {
            //Each time we're reset, set the PRIMARY TO attribute back to the ua.in.smartjava.channel name, otherwise we won't have
            //a primary ID for any ua.in.smartjava.audio produced by this state.
            broadcast(new AttributeChangeRequest<String>(Attribute.PRIMARY_ADDRESS_TO, mChannelName));
        }
    }

    @Override
    public DecoderType getDecoderType()
    {
        return mDecoderType;
    }

    @Override
    public void start(ScheduledExecutorService executor)
    {
        broadcast(new AttributeChangeRequest<String>(Attribute.PRIMARY_ADDRESS_TO, mChannelName));
        broadcast(new DecoderStateEvent(this, Event.ALWAYS_UNSQUELCH, State.IDLE));
    }

    @Override
    public void stop()
    {
    }
}
