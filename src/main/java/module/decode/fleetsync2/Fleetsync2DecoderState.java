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
package module.decode.fleetsync2;

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

public class Fleetsync2DecoderState extends DecoderState
{
    private TreeSet<String> mIdents = new TreeSet<String>();
    private TreeSet<String> mEmergencyIdents = new TreeSet<String>();

    private AliasedStringAttributeMonitor mFromAttribute;
    private AliasedStringAttributeMonitor mToAttribute;
    private String mMessage;
    private String mMessageType;
    private long mFrequency;

    public Fleetsync2DecoderState(AliasList aliasList)
    {
        super(aliasList);

        mFromAttribute = new AliasedStringAttributeMonitor(Attribute.SECONDARY_ADDRESS_FROM,
            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.FLEETSYNC);
        mToAttribute = new AliasedStringAttributeMonitor(Attribute.SECONDARY_ADDRESS_TO,
            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.FLEETSYNC);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.FLEETSYNC2;
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
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void init()
    {
    }

    /**
     * Resets the overall decoder state and clears any accumulated event details
     */
    @Override
    public void reset()
    {
        mIdents.clear();
        mEmergencyIdents.clear();

        resetState();
    }

    /**
     * Resets this decoder state
     */
    public void resetState()
    {
        mFromAttribute.reset();
        mToAttribute.reset();
        mMessage = null;
        mMessageType = null;
    }

    @Override
    public void receive(Message message)
    {
        if(message instanceof FleetsyncMessage)
        {
            FleetsyncMessage fleetsync = (FleetsyncMessage) message;

            if(fleetsync.isValid())
            {
                State state = State.CALL;

                mFromAttribute.process(fleetsync.getFromID());
                mIdents.add(fleetsync.getFromID());

                FleetsyncMessageType type = fleetsync.getMessageType();

                if(type != FleetsyncMessageType.ANI)
                {
                    mToAttribute.process(fleetsync.getToID());
                    mIdents.add(fleetsync.getToID());
                }

                setMessageType(type.getLabel());

                switch(type)
                {
                    case GPS:
                        setMessage(fleetsync.getGPSLocation());
                        state = State.DATA;
                        break;
                    case STATUS:
                        StringBuilder sb = new StringBuilder();
                        sb.append(fleetsync.getStatus());

                        Alias status = fleetsync.getStatusAlias();

                        if(status != null)
                        {
                            sb.append("/");
                            sb.append(status.getName());
                        }

                        setMessage(sb.toString());
                        state = State.DATA;
                        break;
                    case EMERGENCY:
                    case LONE_WORKER_EMERGENCY:
                        mEmergencyIdents.add(fleetsync.getFromID());
                        state = State.DATA;
                        break;
                    default:
                        break;
                }

                FleetsyncCallEvent fsCallEvent =
                    FleetsyncCallEvent.getFleetsync2Event(fleetsync, mFrequency);

                fsCallEvent.setAliasList(getAliasList());

                broadcast(fsCallEvent);

			    /* Broadcast decode event so that the channel state will 
                 * kick in and reset everything after a short delay */
                broadcast(new DecoderStateEvent(this, Event.DECODE, state));
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
        return "Fleetsync " + mMessageType;
    }

    public void setMessageType(String type)
    {
        if(!StringUtils.isEqual(mMessageType, type))
        {
            mMessageType = type;
            broadcast(new AttributeChangeRequest<String>(Attribute.MESSAGE_TYPE, getMessageType()));
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
            case RESET:
                resetState();
                break;
            case SOURCE_FREQUENCY:
                mFrequency = event.getFrequency();
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
            Iterator<String> it = mIdents.iterator();

            while(it.hasNext())
            {
                String ident = it.next();

                sb.append("  ");
                sb.append(formatIdent(ident));

                if(hasAliasList())
                {
                    Alias alias = getAliasList().getFleetsyncAlias(ident);

                    if(alias != null)
                    {
                        sb.append(" ");
                        sb.append(alias.getName());
                    }
                }

                sb.append("\n");
            }
        }

        sb.append("\nFleetsync Emergency Activations\n");

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
                sb.append(formatIdent(ident));

                if(hasAliasList())
                {
                    Alias alias = getAliasList().getFleetsyncAlias(ident);

                    if(alias != null)
                    {
                        sb.append(" ");
                        sb.append(alias.getName());
                    }

                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    public static String formatIdent(String ident)
    {
        StringBuilder sb = new StringBuilder();

        if(ident.length() == 7)
        {
            sb.append(ident.substring(0, 3));
            sb.append("-");
            sb.append(ident.substring(3, 7));

            return sb.toString();
        }
        else
        {
            return ident;
        }
    }
}
