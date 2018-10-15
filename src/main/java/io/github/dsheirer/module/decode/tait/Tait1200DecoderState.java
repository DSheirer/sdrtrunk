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
package io.github.dsheirer.module.decode.tait;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.channel.metadata.AliasedStringAttributeMonitor;
import io.github.dsheirer.channel.metadata.Attribute;
import io.github.dsheirer.channel.metadata.AttributeChangeRequest;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.CallEvent;
import io.github.dsheirer.util.StringUtils;
import org.jdesktop.swingx.mapviewer.GeoPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class Tait1200DecoderState extends DecoderState
{
    private TreeSet<String> mIdents = new TreeSet<String>();

    private AliasedStringAttributeMonitor mFromAttribute;
    private AliasedStringAttributeMonitor mToAttribute;
    private String mMessage;
    private String mMessageType;

    public Tait1200DecoderState(AliasList aliasList)
    {
        super(aliasList);

        mFromAttribute = new AliasedStringAttributeMonitor(Attribute.SECONDARY_ADDRESS_FROM,
                getAttributeChangeRequestListener(), getAliasList(), AliasIDType.TALKGROUP);
        mToAttribute = new AliasedStringAttributeMonitor(Attribute.SECONDARY_ADDRESS_TO,
                getAttributeChangeRequestListener(), getAliasList(), AliasIDType.TALKGROUP);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.TAIT_1200;
    }

    @Override
    public void reset()
    {
        mIdents.clear();

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

    private void resetState()
    {
        mFromAttribute.reset();
        mToAttribute.reset();
        mMessage = null;
        mMessageType = null;
    }

    @Override
    public void receive(IMessage message)
    {
        if(message instanceof Tait1200GPSMessage)
        {
            Tait1200GPSMessage gps = (Tait1200GPSMessage) message;

            mFromAttribute.process(gps.getFromID());
            mIdents.add(gps.getFromID());

            mToAttribute.process(gps.getToID());
            mIdents.add(gps.getToID());

            GeoPosition position = gps.getGPSLocation();

            if(position != null)
            {
                setMessage(gps.getGPSLocation().toString().replace("[", "")
                        .replace("]", ""));
            }

            setMessageType("GPS");

            broadcast(new Tait1200CallEvent(CallEvent.CallEventType.GPS, getAliasList(),
                    gps.getFromID(), gps.getToID(), gps.getGPSLocation().toString()));

            broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));
        }
        else if(message instanceof Tait1200ANIMessage)
        {
            Tait1200ANIMessage ani = (Tait1200ANIMessage) message;
            mFromAttribute.process(ani.getFromID());
            mIdents.add(ani.getFromID());

            mToAttribute.process(ani.getToID());
            mIdents.add(ani.getToID());

            setMessage(null);
            setMessageType("ANI");

            broadcast(new Tait1200CallEvent(CallEvent.CallEventType.ID_ANI, getAliasList(),
                    ani.getFromID(), ani.getToID(), "ANI"));

            broadcast(new DecoderStateEvent(this, Event.DECODE, State.CALL));
        }
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("=============================\n");
        sb.append("Decoder:\tTait-1200I\n\n");

        if(!mIdents.isEmpty())
        {
            sb.append("Radio Identifiers:\n");

            List<String> idents = new ArrayList<String>(mIdents);

            Collections.sort(idents);

            for(String ident : idents)
            {
                sb.append("\t");
                sb.append(ident);

                if(hasAliasList())
                {
                    Alias alias = getAliasList().getTalkgroupAlias(ident);

                    if(alias != null)
                    {
                        sb.append("\t");
                        sb.append(alias.getName());
                    }
                }
                sb.append("\n");
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
            case RESET:
                resetState();
                break;
            default:
                break;
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
            broadcast(new AttributeChangeRequest<String>(Attribute.MESSAGE, getMessage()));
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
            broadcast(new AttributeChangeRequest<String>(Attribute.MESSAGE_TYPE, getMessageType()));
        }
    }
}
