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
package module.decode.lj1200;

import alias.Alias;
import alias.AliasList;
import alias.id.AliasIDType;
import channel.metadata.AliasedStringAttributeMonitor;
import channel.metadata.Attribute;
import channel.metadata.AttributeChangeRequest;
import channel.state.DecoderState;
import channel.state.DecoderStateEvent;
import channel.state.DecoderStateEvent.Event;
import channel.state.State;
import message.Message;
import module.decode.DecoderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

public class LJ1200DecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(LJ1200DecoderState.class);

    private Set<String> mAddresses = new TreeSet<String>();

    private AliasedStringAttributeMonitor mAddressAttribute;

    public LJ1200DecoderState(AliasList aliasList)
    {
        super(aliasList);

        mAddressAttribute = new AliasedStringAttributeMonitor(Attribute.SECONDARY_ADDRESS_TO,
            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.SITE);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.LJ_1200;
    }

    @Override
    public void start(ScheduledExecutorService executor)
    {
    }

    @Override
    public void stop()
    {
    }

    @Override
    public void receive(Message message)
    {
        if(message instanceof LJ1200Message)
        {
            LJ1200Message lj = (LJ1200Message) message;

            if(lj.isValid())
            {
                String address = lj.getAddress();

                mAddressAttribute.process(address);
                mAddresses.add(address);

                broadcast(LJ1200CallEvent.getLJ1200Event(lj));

                broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));
            }
        }
        else if(message instanceof LJ1200TransponderMessage)
        {

        }
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("=============================\n");
        sb.append("Decoder:\tLJ-1200I\n\n");

        if(!mAddresses.isEmpty())
        {
            sb.append("Transponder Addresses:\n");

            List<String> addresses = new ArrayList<String>(mAddresses);

            Collections.sort(addresses);

            for(String address : addresses)
            {
                sb.append("\t");
                sb.append(address);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public void init()
    {
        /* No initialization steps */
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        if(event.getEvent() == Event.RESET)
        {
            resetState();
        }
    }

    private void resetState()
    {
        mAddressAttribute.reset();
    }

    @Override
    public void reset()
    {
        mAddresses.clear();
        resetState();
    }
}
