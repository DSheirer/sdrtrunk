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
package io.github.dsheirer.module.decode.mdc1200;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.mdc1200.identifier.MDC1200Identifier;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class MDCDecoderState extends DecoderState
{
    private Set<MDC1200Identifier> mIdents = new TreeSet<>();
    private Set<MDC1200Identifier> mEmergencyIdents = new TreeSet<>();

    public MDCDecoderState()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.MDC1200;
    }

    @Override
    public void reset()
    {
        super.reset();
        mIdents.clear();
        mEmergencyIdents.clear();

        resetState();
    }

    @Override
    public void init() {}

    @Override
    public void receive(IMessage message)
    {
        if(message instanceof MDCMessage)
        {
            MDCMessage mdc = (MDCMessage)message;

            mIdents.add(mdc.getFromIdentifier());

            if(mdc.isEmergency())
            {
                mEmergencyIdents.add(mdc.getFromIdentifier());
            }

            MDCMessageType type = mdc.getMessageType();

            StringBuilder sb = new StringBuilder();

            sb.append("OPCODE ");
            sb.append(mdc.getOpcode());

            if(mdc.isBOT())
            {
                sb.append(" TYPE:BOT");
            }

            if(mdc.isEOT())
            {
                sb.append(" TYPE:EOT");
            }

            MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
            ic.remove(IdentifierClass.USER);
            ic.update(message.getIdentifiers());

            broadcast(MDCDecodeEvent.builder(getDecodeEventType(type), mdc.getTimestamp())
                .details(mdc.toString())
                .identifiers(ic)
                .build());

            switch(type)
            {
                case ANI:
                    broadcast(new DecoderStateEvent(this, Event.DECODE, State.CALL));
                    break;
                case ACKNOWLEDGE:
                case EMERGENCY:
                case PAGING:
                case STATUS:
                default:
                    broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));
                    break;
            }

        }
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("=============================\n");
        sb.append("Decoder:\tMDC-1200\n\n");
        sb.append("MDC-1200 Idents\n");

        if(mIdents.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {

            for (MDC1200Identifier mIdent : mIdents) {
                sb.append("  ").append(mIdent).append("\n");
            }
        }

        sb.append("MDC-1200 Emergency Idents\n");

        if(mEmergencyIdents.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            Iterator<MDC1200Identifier> it = mEmergencyIdents.iterator();

            while(it.hasNext())
            {

                for (MDC1200Identifier mIdent : mIdents) {
                    sb.append("  ").append(mIdent).append("\n");
                }
            }
        }

        return sb.toString();
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
                resetState();
                break;
            default:
                break;
        }
    }

    private DecodeEventType getDecodeEventType(MDCMessageType messageType) {
        switch(messageType)
        {
            case ANI:
                return DecodeEventType.ID_ANI;
            case ACKNOWLEDGE:
                return DecodeEventType.RESPONSE;
            case EMERGENCY:
                return DecodeEventType.EMERGENCY;
            case PAGING:
                return DecodeEventType.PAGE;
            case STATUS:
                return DecodeEventType.STATUS;
            default:
                return DecodeEventType.UNKNOWN;
        }
    }
}
