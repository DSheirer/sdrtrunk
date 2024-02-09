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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fleetsync2DecoderState extends DecoderState
{
    private Map<FleetsyncIdentifier,Integer> mFromIdentCountsMap = new HashMap<>();
    private Map<FleetsyncIdentifier,Integer> mToIdentCountsMap = new HashMap<>();

    public Fleetsync2DecoderState()
    {
        super();
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.FLEETSYNC2;
    }

    @Override
    public void init() {}

    @Override
    public void receive(IMessage message)
    {
        if(message.isValid() && message instanceof Fleetsync2Message)
        {
            Fleetsync2Message fleetsync = (Fleetsync2Message)message;

            getIdentifierCollection().update(fleetsync.getIdentifiers());

            FleetsyncIdentifier from = fleetsync.getFromIdentifier();

            if(mFromIdentCountsMap.containsKey(from))
            {
                mFromIdentCountsMap.put(from, mFromIdentCountsMap.get(from) + 1);
            }
            else
            {
                mFromIdentCountsMap.put(from, 1);
            }

            FleetsyncIdentifier to = fleetsync.getToIdentifier();

            if(to != null)
            {
                if(mToIdentCountsMap.containsKey(to))
                {
                    mToIdentCountsMap.put(to, mToIdentCountsMap.get(to) + 1);
                }
                else
                {
                    mToIdentCountsMap.put(to, 1);
                }
            }

            switch(fleetsync.getMessageType())
            {
                case ANI:
                case EMERGENCY:
                case LONE_WORKER_EMERGENCY:
                    DecodeEvent aniEvent = getDecodeEvent(fleetsync, getDecodeEventType(fleetsync.getMessageType()));
                    broadcast(aniEvent);
                    broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.DECODE, State.CALL));
                    break;
                case ACKNOWLEDGE:
                case PAGING:
                case STATUS:
                case UNKNOWN:
                    DecodeEvent statusEvent = getDecodeEvent(fleetsync, getDecodeEventType(fleetsync.getMessageType()));
                    broadcast(statusEvent);
                    broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.DECODE, State.DATA));
                    break;
                case GPS:
                    PlottableDecodeEvent plottableDecodeEvent = PlottableDecodeEvent.plottableBuilder(DecodeEventType.GPS, fleetsync.getTimestamp())
                        .channel(getCurrentChannel())
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

    private DecodeEvent getDecodeEvent(Fleetsync2Message fleetsync, DecodeEventType eventType) {
        return DecodeEvent.builder(eventType, fleetsync.getTimestamp())
                .channel(getCurrentChannel())
                .details(fleetsync.getMessageType() + " " + fleetsync)
                .identifiers(getIdentifierCollection().copyOf())
                .protocol(Protocol.FLEETSYNC)
                .build();
    }

    private DecodeEventType getDecodeEventType(FleetsyncMessageType fleetsyncMessageType) {
        switch (fleetsyncMessageType) {
            case ANI:
                return DecodeEventType.ID_ANI;
            case EMERGENCY:
            case LONE_WORKER_EMERGENCY:
                return DecodeEventType.EMERGENCY;
            case ACKNOWLEDGE:
                return DecodeEventType.ACKNOWLEDGE;
            case PAGING:
                return DecodeEventType.PAGE;
            case STATUS:
                return DecodeEventType.STATUS;
            case GPS:
                return DecodeEventType.GPS;
            default:
                return DecodeEventType.UNKNOWN;
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

        if(mFromIdentCountsMap.isEmpty() && mToIdentCountsMap.isEmpty())
        {
            sb.append("Fleetsync Idents\n");
            sb.append("  None\n");
        }
        else
        {
            sb.append("Fleetsync From Idents\n");

            List<FleetsyncIdentifier> fromIdents = new ArrayList<>(mFromIdentCountsMap.keySet());
            Collections.sort(fromIdents);
            for(FleetsyncIdentifier from: fromIdents)
            {
                sb.append("  ").append(from.formatted()).append(" - Count:").append(mFromIdentCountsMap.get(from)).append("\n");
            }

            sb.append("\nFleetsync To Idents\n");

            List<FleetsyncIdentifier> toIdents = new ArrayList<>(mToIdentCountsMap.keySet());
            Collections.sort(toIdents);
            for(FleetsyncIdentifier to: toIdents)
            {
                sb.append("  ").append(to.formatted()).append(" - Count:").append(mToIdentCountsMap.get(to)).append("\n");
            }
        }

        return sb.toString();
    }
}
