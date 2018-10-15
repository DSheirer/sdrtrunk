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
package io.github.dsheirer.module.decode.lj1200;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.channel.metadata.AliasedStringAttributeMonitor;
import io.github.dsheirer.channel.metadata.Attribute;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
    public void receive(IMessage message)
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

    @Override
    public void start()
    {

    }

    @Override
    public void stop()
    {

    }

    @Override
    public void init()
    {

    }
}
