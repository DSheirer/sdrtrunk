/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.fleetsync2;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.module.decode.fleetsync2.identifier.FleetsyncIdentifier;
import io.github.dsheirer.module.decode.fleetsync2.message.Fleetsync2Message;
import io.github.dsheirer.protocol.Protocol;

import java.util.Set;
import java.util.TreeSet;

public class Fleetsync2DecoderState extends DecoderState
{
    private Set<FleetsyncIdentifier> mIdents = new TreeSet<>();

    public Fleetsync2DecoderState()
    {
        super();
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.FLEETSYNC2;
    }

    /**
     * Resets the overall decoder state and clears any accumulated event details
     */
    @Override
    public void reset()
    {
        super.reset();
        mIdents.clear();
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

    @Override
    public void receive(IMessage message)
    {
        if(message.isValid() && message instanceof Fleetsync2Message)
        {
            Fleetsync2Message fleetsync = (Fleetsync2Message)message;

            getIdentifierCollection().update(fleetsync.getIdentifiers());

            mIdents.add(fleetsync.getFromIdentifier());
            mIdents.add(fleetsync.getToIdentifier());

            switch(fleetsync.getMessageType())
            {
                case ANI:
                case EMERGENCY:
                case LONE_WORKER_EMERGENCY:
                    DecodeEvent aniEvent = DecodeEvent.builder(fleetsync.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(fleetsync.getMessageType().toString())
                        .details(fleetsync.toString())
                        .identifiers(getIdentifierCollection().copyOf())
                        .build();

                    broadcast(aniEvent);
                    broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.DECODE, State.CALL));
                    break;
                case ACKNOWLEDGE:
                case PAGING:
                case STATUS:
                case UNKNOWN:
                    DecodeEvent statusEvent = DecodeEvent.builder(fleetsync.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(fleetsync.getMessageType().toString())
                        .details(fleetsync.toString())
                        .identifiers(getIdentifierCollection().copyOf())
                        .build();
                    broadcast(statusEvent);
                    broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.DECODE, State.DATA));
                    break;
                case GPS:
                    PlottableDecodeEvent plottableDecodeEvent = PlottableDecodeEvent.plottableBuilder(fleetsync.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.GPS.toString())
                        .details(fleetsync.toString())
                        .identifiers(getIdentifierCollection().copyOf())
                        .protocol(Protocol.FLEETSYNC)
                        .build();
                    broadcast(plottableDecodeEvent);
                    broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.DECODE, State.DATA));
                    break;
            }

            getIdentifierCollection().remove(IdentifierClass.USER);
        }
    }

    /**
     * Responds to reset events issued by the channel state
     */
    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_RESET:
                reset();
                break;
            default:
                break;
        }
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("=============================\n");
        sb.append("Decoder:\tFleetsync II\n\n");
        sb.append("Fleetsync Idents\n");

        if(mIdents.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {

            for (FleetsyncIdentifier mIdent : mIdents) {
                sb.append("  ").append(mIdent.formatted()).append("\n");
            }
        }

        return sb.toString();
    }
}
