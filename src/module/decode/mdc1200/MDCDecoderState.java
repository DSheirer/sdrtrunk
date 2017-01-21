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
package module.decode.mdc1200;

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
import util.StringUtils;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

public class MDCDecoderState extends DecoderState
{
    private TreeSet<String> mIdents = new TreeSet<String>();
    private TreeSet<String> mEmergencyIdents = new TreeSet<String>();

    private AliasedStringAttributeMonitor mFromAttribute;
    private String mMessage;
    private String mMessageType;

    public MDCDecoderState(AliasList aliasList)
    {
        super(aliasList);

        mFromAttribute = new AliasedStringAttributeMonitor(Attribute.SECONDARY_ADDRESS_FROM,
            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.MDC1200);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.MDC1200;
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
    public void reset()
    {
        mIdents.clear();
        mEmergencyIdents.clear();

        resetState();
    }

    private void resetState()
    {
        mFromAttribute.reset();
        mMessage = null;
        mMessageType = null;
    }

    @Override
    public void receive(Message message)
    {
        if(message instanceof MDCMessage)
        {
            MDCMessage mdc = (MDCMessage) message;

            mIdents.add(mdc.getUnitID());

            if(mdc.isEmergency())
            {
                mEmergencyIdents.add(mdc.getUnitID());
            }

            mFromAttribute.process(mdc.getFromID());

            MDCMessageType type = mdc.getMessageType();

            setMessageType(type.getLabel());

            StringBuilder sb = new StringBuilder();

            sb.append("OPCODE ");
            sb.append(String.valueOf(mdc.getOpcode()));

            if(mdc.isBOT())
            {
                sb.append(" TYPE:BOT");
            }

            if(mdc.isEOT())
            {
                sb.append(" TYPE:EOT");
            }

            setMessage(sb.toString());

            MDCCallEvent event = MDCCallEvent.getMDCCallEvent(mdc);
            event.setAliasList(getAliasList());
            broadcast(event);

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

    public String getMessage()
    {
        return mMessage;
    }

    public void setMessage(String message)
    {
        if(!StringUtils.isEqual(mMessage, message))
        {
            mMessage = message;
            broadcast(new AttributeChangeRequest<String>(Attribute.MESSAGE, mMessage));
        }
    }

    public String getMessageType()
    {
        return mMessageType;
    }

    public void setMessageType(String type)
    {
        if(!StringUtils.isEqual(mMessageType, type))
        {
            mMessageType = type;
            broadcast(new AttributeChangeRequest<String>(Attribute.MESSAGE_TYPE, "MDC:" + mMessageType));
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
            Iterator<String> it = mIdents.iterator();

            while(it.hasNext())
            {
                String ident = it.next();

                sb.append("  ");
                sb.append(ident);
                sb.append(" ");

                if(hasAliasList())
                {
                    Alias alias = getAliasList().getMDC1200Alias(ident);

                    if(alias != null)
                    {
                        sb.append(alias.getName());
                    }
                }

                sb.append("\n");
            }
        }

        sb.append("MDC-1200 Emergency Idents\n");

        if(mEmergencyIdents.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            Iterator<String> it = mEmergencyIdents.iterator();

            while(it.hasNext())
            {
                String ident = it.next();

                sb.append("  ");
                sb.append(ident);
                sb.append(" ");

                if(hasAliasList())
                {
                    Alias alias = getAliasList().getMDC1200Alias(ident);

                    if(alias != null)
                    {
                        sb.append(alias.getName());
                    }
                }

                sb.append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public void init()
    {
        /* No initialization required */
    }

    /**
     * Responds to reset events issued by the channel state
     */
    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case RESET:
                resetState();
                break;
            default:
                break;
        }
    }
}
