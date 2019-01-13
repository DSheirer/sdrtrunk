/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.channel.state.ChangeChannelTimeoutEvent;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.mpt1327.channel.MPT1327Channel;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class MPT1327DecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(MPT1327DecoderState.class);

    private TreeSet<String> mIdents = new TreeSet<String>();
    private HashMap<String,ArrayList<String>> mGroups = new HashMap<String,ArrayList<String>>();

    private String mSite;

    private int mChannelNumber;
    private ChannelType mChannelType;

    private long mFrequency = 0;
    private long mCallTimeout;
    private MPT1327TrafficChannelManager mMPT1327TrafficChannelManager;

    /**
     * Constructs an MPT-1327 decoder state with an optional traffic channel manager for allocating traffic channels
     * when go to channel (GTC) messages are detected.
     */
    public MPT1327DecoderState(MPT1327TrafficChannelManager trafficChannelManager, ChannelType channelType, long callTimeout)
    {
        mMPT1327TrafficChannelManager = trafficChannelManager;
        mChannelType = channelType;
        mCallTimeout = callTimeout;
    }

    /**
     * Constructs an MPT1327 decoder state that does not allocate traffic channels
     */
    public MPT1327DecoderState(ChannelType channelType, long callTimeout)
    {
        this(null, channelType, callTimeout);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.MPT1327;
    }

    public ChannelType getChannelType()
    {
        return mChannelType;
    }

    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void receive(IMessage message)
    {
        if(message.isValid())
        {
            if(message instanceof MPT1327Message)
            {
                MPT1327Message mpt = ((MPT1327Message)message);

                switch(mpt.getMessageType())
                {
                    case ACK:
                        mIdents.add(mpt.getFromID());

                        MPT1327Message.IdentType identType = mpt.getIdent1Type();

                        if(identType == MPT1327Message.IdentType.REGI)
                        {
                            MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                            ic.remove(IdentifierClass.USER);
                            ic.update(mpt.getIdentifiers());

                            broadcast(DecodeEvent.builder(message.getTimestamp())
                                .protocol(Protocol.MPT1327)
                                .eventDescription("Register")
                                .identifiers(ic)
                                .build());
                        }
                        else
                        {
                            MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                            ic.remove(IdentifierClass.USER);
                            ic.update(mpt.getIdentifiers());

                            broadcast(DecodeEvent.builder(message.getTimestamp())
                                .protocol(Protocol.MPT1327)
                                .eventDescription("Response")
                                .details("ACK " + identType.getLabel())
                                .identifiers(ic)
                                .build());
                        }

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
                        break;
                    case ACKB:
                    case ACKE:
                    case ACKI:
                    case ACKQ:
                    case ACKT:
                    case ACKV:
                    case ACKX:
                        MutableIdentifierCollection icACK = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icACK.remove(IdentifierClass.USER);
                        icACK.update(mpt.getIdentifiers());

                        broadcast(DecodeEvent.builder(message.getTimestamp())
                            .protocol(Protocol.MPT1327)
                            .eventDescription("Acknowledge")
                            .details(mpt.getMessageType().getDescription())
                            .identifiers(icACK)
                            .build());

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
                        break;
                    case AHYC:
                        MutableIdentifierCollection icAHY = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icAHY.remove(IdentifierClass.USER);
                        icAHY.update(mpt.getIdentifiers());

                        broadcast(DecodeEvent.builder(message.getTimestamp())
                            .protocol(Protocol.MPT1327)
                            .eventDescription("Command")
                            .details("Send Short Data Message")
                            .identifiers(icAHY)
                            .build());

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
                        break;
                    case AHYQ:
                        MutableIdentifierCollection icAHYQ = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icAHYQ.remove(IdentifierClass.USER);
                        icAHYQ.update(mpt.getIdentifiers());

                        broadcast(DecodeEvent.builder(message.getTimestamp())
                            .protocol(Protocol.MPT1327)
                            .eventDescription("Command")
                            .details("Send Status Message")
                            .identifiers(icAHYQ)
                            .build());

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
                        break;
                    case ALH:
                    case ALHD:
                    case ALHE:
                    case ALHF:
                    case ALHR:
                    case ALHS:
                    case ALHX:
                        setSite(mpt.getSiteID());
                        broadcast(new DecoderStateEvent(this, Event.START, State.CONTROL));
                        break;
                    case GTC:
                        if(mMPT1327TrafficChannelManager != null)
                        {
                            mMPT1327TrafficChannelManager.processChannelGrant(mpt);
                        }
                        else
                        {
                            MPT1327Channel channel = MPT1327Channel.create(mpt.getChannel());
                            MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                            ic.remove(IdentifierClass.USER);
                            ic.update(mpt.getIdentifiers());

                            broadcast(DecodeEvent.builder(mpt.getTimestamp())
                                .eventDescription("Call Detect")
                                .details(mpt.getMessage())
                                .channel(channel)
                                .identifiers(ic)
                                .build());
                        }
                        break;
                    case HEAD_PLUS1:
                    case HEAD_PLUS2:
                    case HEAD_PLUS3:
                    case HEAD_PLUS4:
                        MutableIdentifierCollection icHEAD = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icHEAD.remove(IdentifierClass.USER);
                        icHEAD.update(mpt.getIdentifiers());

                        broadcast(DecodeEvent.builder(message.getTimestamp())
                            .protocol(Protocol.MPT1327)
                            .eventDescription("Short Data Message")
                            .details(mpt.getMessage())
                            .identifiers(icHEAD)
                            .build());

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
                        break;

                    /* Traffic Channel Events */
                    case CLEAR:
                        mChannelNumber = mpt.getChannel();

                        broadcast(new DecoderStateEvent(this, Event.END, State.FADE));
                        break;
                    case MAINT:
                        getIdentifierCollection().update(mpt.getToIdentifier());

                        if(mChannelType == ChannelType.STANDARD)
                        {
                            //When we receive a MAINT message and we're configured as a standard channel, apply the call
                            // timeout specified by the user.  Otherwise we'll be using the shorter default call timeout
                            broadcast(new ChangeChannelTimeoutEvent(this, mChannelType, mCallTimeout));

                            MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                            ic.remove(IdentifierClass.USER);
                            ic.update(mpt.getIdentifiers());

                            broadcast(DecodeEvent.builder(mpt.getTimestamp())
                                .identifiers(ic)
                                .eventDescription("Call In Progress")
                                .build());
                            broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

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

    protected void resetState()
    {
        super.resetState();

        /**
         * If this is a standard channel, reset the fade timeout to the default
         * timeout.  Once processing is underway, if we get a MAINT message,
         * this indicates we're processing a traffic channel as a standard
         * channel, so we'll issue a different call timeout then.
         */
        if(mChannelType == ChannelType.STANDARD)
        {
            broadcast(new ChangeChannelTimeoutEvent(this, mChannelType,
                DecodeConfiguration.DEFAULT_CALL_TIMEOUT_SECONDS * 1000));
        }
    }

    public String getSite()
    {
        return mSite;
    }

    /**
     * Set the site number.
     */
    public void setSite(String site)
    {
        if(!StringUtils.isEqual(mSite, site))
        {
            mSite = site;
        }
    }

    public int getChannelNumber()
    {
        return mChannelNumber;
    }

    /**
     * Set the channel number.  This is used primarily for traffic channels since
     * the channel will already have been identified prior to the traffic
     * channel being created.
     */
    public void setChannelNumber(int channel)
    {
        if(mChannelNumber != channel)
        {
            mChannelNumber = channel;
        }
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary - Decoder: MPT-1327\n");
        sb.append(DIVIDER1);
        sb.append("Site: ");

        if(mSite != null)
        {
            sb.append(mSite).append("\n");

        }
        else
        {
            sb.append("Unknown\n");
        }

        sb.append(DIVIDER2).append("Talkgroups: ");

        if(mGroups.isEmpty())
        {
            sb.append("None\n");
        }
        else
        {
            List<String> talkgroups = new ArrayList<String>(mGroups.keySet());
            Collections.sort(talkgroups);

            for(String talkgroup : talkgroups)
            {
                sb.append("\n ").append(talkgroup);

                sb.append("\n");

                ArrayList<String> members = mGroups.get(talkgroup);
                Collections.sort(members);

                for(String member : members)
                {
                    sb.append("  >");
                    sb.append(member);

                    sb.append("\n");
                }
            }
        }

        sb.append(DIVIDER2).append("All Idents: ");

        if(mIdents.isEmpty())
        {
            sb.append("None\n");
        }
        else
        {
            sb.append("\n");

            Iterator<String> it = mIdents.iterator();

            while(it.hasNext())
            {
                String ident = it.next();

                sb.append(ident);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

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
}