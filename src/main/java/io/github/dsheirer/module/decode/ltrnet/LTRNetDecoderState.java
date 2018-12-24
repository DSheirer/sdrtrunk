/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
package io.github.dsheirer.module.decode.ltrnet;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.identifier.esn.ESNIdentifier;
import io.github.dsheirer.identifier.talkgroup.LTRTalkgroup;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.ltrnet.channel.LtrNetChannel;
import io.github.dsheirer.module.decode.ltrnet.identifier.UniqueIdentifier;
import io.github.dsheirer.module.decode.ltrnet.message.LtrNetMessage;
import io.github.dsheirer.module.decode.ltrnet.message.isw.IswCallEnd;
import io.github.dsheirer.module.decode.ltrnet.message.isw.IswCallStart;
import io.github.dsheirer.module.decode.ltrnet.message.isw.IswUniqueId;
import io.github.dsheirer.module.decode.ltrnet.message.isw.RegistrationRequestEsnHigh;
import io.github.dsheirer.module.decode.ltrnet.message.isw.RegistrationRequestEsnLow;
import io.github.dsheirer.module.decode.ltrnet.message.isw.RequestAccess;
import io.github.dsheirer.module.decode.ltrnet.message.osw.ChannelMapHigh;
import io.github.dsheirer.module.decode.ltrnet.message.osw.ChannelMapLow;
import io.github.dsheirer.module.decode.ltrnet.message.osw.NeighborId;
import io.github.dsheirer.module.decode.ltrnet.message.osw.OswCallEnd;
import io.github.dsheirer.module.decode.ltrnet.message.osw.OswCallStart;
import io.github.dsheirer.module.decode.ltrnet.message.osw.ReceiveFrequencyHigh;
import io.github.dsheirer.module.decode.ltrnet.message.osw.ReceiveFrequencyLow;
import io.github.dsheirer.module.decode.ltrnet.message.osw.RegistrationAccept;
import io.github.dsheirer.module.decode.ltrnet.message.osw.SiteId;
import io.github.dsheirer.module.decode.ltrnet.message.osw.SystemIdle;
import io.github.dsheirer.module.decode.ltrnet.message.osw.TransmitFrequencyHigh;
import io.github.dsheirer.module.decode.ltrnet.message.osw.TransmitFrequencyLow;
import io.github.dsheirer.protocol.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class LTRNetDecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(LTRNetDecoderState.class);

    private ChannelMapHigh mChannelMapHigh;
    private ChannelMapLow mChannelMapLow;
    private Map<Integer,LtrNetChannel> mChannelMap = new HashMap<>();
    private Map<Integer,DecodeEvent> mCallDetectMap = new HashMap<>();
    private Map<Integer,NeighborId> mNeighborMap = new HashMap<>();
    private SiteId mCurrentSite;
    private int mCurrentChannelNumber;
    private Set<LTRTalkgroup> mTalkgroups = new TreeSet<>();
    private Set<UniqueIdentifier> mUniqueIdentifiers = new TreeSet<>();
    private Set<ESNIdentifier> mESNIdentifiers = new TreeSet<>();
    private DecodeEvent mCurrentCallEvent;
    private LTRTalkgroup mCurrentCallTalkgroup;

    public LTRNetDecoderState()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.LTR_NET;
    }

    @Override
    protected IChannelDescriptor getCurrentChannel()
    {
        if(mCurrentChannelNumber > 0)
        {
            return mChannelMap.get(mCurrentChannelNumber);
        }

        return null;
    }

    /**
     * Performs a full reset
     */
    public void reset()
    {
        mChannelMapHigh = null;
        mChannelMapLow = null;
        mChannelMap.clear();
        mCallDetectMap.clear();
        mNeighborMap.clear();
        mCurrentSite = null;
        mCurrentChannelNumber = 0;
        mTalkgroups.clear();
        mUniqueIdentifiers.clear();
        mESNIdentifiers.clear();
        resetState();
    }

    private void processCallEnd(int channel, LTRTalkgroup talkgroup, long timestamp)
    {
        setCurrentChannelNumber(channel);

        if(mCurrentCallTalkgroup != null && mCurrentCallTalkgroup.equals(talkgroup))
        {
            mCurrentCallEvent.end(timestamp);
            broadcast(mCurrentCallEvent);
            mCurrentCallEvent = null;
            mCurrentCallTalkgroup = null;
            getIdentifierCollection().remove(IdentifierClass.USER);
        }

        if(talkgroup.getTalkgroup() == 254)
        {
            //Process CW Station ID broadcasts as an active state
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
        }
        else
        {
            broadcast(new DecoderStateEvent(this, Event.END, State.CALL));
        }
    }

    private void processCallStart(int channel, LTRTalkgroup talkgroup, long timestamp, MessageDirection direction)
    {
        if(direction == MessageDirection.ISW)
        {
            setCurrentChannelNumber(channel);
        }

        if(channel == mCurrentChannelNumber || mCurrentChannelNumber == 0)
        {
            setCurrentChannelNumber(channel);

            if(mCurrentCallTalkgroup == null || !mCurrentCallTalkgroup.equals(talkgroup))
            {
                getIdentifierCollection().remove(IdentifierClass.USER);
                getIdentifierCollection().update(talkgroup);
                mCurrentCallEvent = DecodeEvent.builder(timestamp)
                    .protocol(Protocol.LTR_NET)
                    .channel(getCurrentChannel())
                    .identifiers(getIdentifierCollection().copyOf())
                    .build();
                mCurrentCallTalkgroup = talkgroup;
            }

            //This updates the timestamp
            mCurrentCallEvent.update(timestamp);

            if(talkgroup.getTalkgroup() == 253)
            {
                mCurrentCallEvent.setEventDescription("Register");
                broadcast(new DecoderStateEvent(this, Event.START, State.DATA));
            }
            else if(talkgroup.getTalkgroup() == 254)
            {
                mCurrentCallEvent.setEventDescription("FCC CWID");
                broadcast(new DecoderStateEvent(this, Event.START, State.DATA));
            }
            else
            {
                mCurrentCallEvent.setEventDescription("Call");
                broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
            }

            broadcast(mCurrentCallEvent);
        }
        else
        {
            DecodeEvent decodeEvent = mCallDetectMap.get(channel);

            if(decodeEvent == null)
            {
                MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                ic.remove(IdentifierClass.USER);
                ic.update(talkgroup);

                decodeEvent = DecodeEvent.builder(timestamp)
                    .eventDescription("Call Detect")
                    .channel(mChannelMap.get(channel))
                    .protocol(Protocol.LTR_NET)
                    .identifiers(ic)
                    .build();
                mCallDetectMap.put(channel, decodeEvent);
            }
            else
            {
                Identifier eventTalkgroup = decodeEvent.getIdentifierCollection()
                    .getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.TO);

                if(eventTalkgroup == null || !eventTalkgroup.equals(talkgroup) ||
                    (timestamp - decodeEvent.getTimeStart() - decodeEvent.getDuration() > 2000))
                {
                    MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    ic.remove(IdentifierClass.USER);
                    ic.update(talkgroup);

                    decodeEvent = DecodeEvent.builder(timestamp)
                        .eventDescription("Call Detect")
                        .channel(mChannelMap.get(channel))
                        .protocol(Protocol.LTR_NET)
                        .identifiers(ic)
                        .build();
                    mCallDetectMap.put(channel, decodeEvent);
                }
            }

            decodeEvent.update(timestamp);
            broadcast(decodeEvent);
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
        }
    }

    /**
     * Updates the current call event whenever any identifiers related to the call change
     */
    private void updateCurrentCallIdentifiers()
    {
        if(mCurrentCallEvent != null)
        {
            mCurrentCallEvent.setIdentifierCollection(getIdentifierCollection().copyOf());
            broadcast(mCurrentCallEvent);
        }
    }

    /**
     * Updates the uplink or receive frequency for the repeater identified by the Logical Channel Number (LCN).
     */
    private void updateReceiveFrequency(int channel, long frequency)
    {
        if(0 < channel && channel <= 20 && frequency > 0)
        {
            LtrNetChannel ltrNetChannel = mChannelMap.get(channel);

            if(ltrNetChannel == null)
            {
                ltrNetChannel = new LtrNetChannel(channel);
                ltrNetChannel.setUplink(frequency);
                mChannelMap.put(channel, ltrNetChannel);
            }
            else
            {
                ltrNetChannel.setUplink(frequency);
            }
        }
    }

    /**
     * Updates the downlink or transmit frequency for the repeater identified by the Logical Channel Number (LCN).
     */
    private void updateTransmitFrequency(int channel, long frequency)
    {
        if(0 < channel && channel <= 20 && frequency > 0)
        {
            LtrNetChannel ltrNetChannel = mChannelMap.get(channel);

            if(ltrNetChannel == null)
            {
                ltrNetChannel = new LtrNetChannel(channel);
                ltrNetChannel.setDownlink(frequency);
                mChannelMap.put(channel, ltrNetChannel);
            }
            else
            {
                ltrNetChannel.setDownlink(frequency);
            }
        }
    }

    @Override
    public void receive(IMessage message)
    {
        if(message.isValid() && message instanceof LtrNetMessage)
        {
            switch(((LtrNetMessage)message).getLtrNetMessageType())
            {
                case ISW_CALL_END:
                    if(message instanceof IswCallEnd)
                    {
                        IswCallEnd callEnd = (IswCallEnd)message;
                        mTalkgroups.add(callEnd.getTalkgroup());
                        processCallEnd(callEnd.getChannel(), callEnd.getTalkgroup(), callEnd.getTimestamp());
                    }
                    break;
                case ISW_CALL_START:
                    if(message instanceof IswCallStart)
                    {
                        IswCallStart callStart = (IswCallStart)message;
                        mTalkgroups.add(callStart.getTalkgroup());
                        processCallStart(callStart.getChannel(), callStart.getTalkgroup(), callStart.getTimestamp(),
                            MessageDirection.ISW);
                    }
                    break;
                case ISW_REGISTRATION_REQUEST_ESN_HIGH:
                    if(message instanceof RegistrationRequestEsnHigh)
                    {
                        RegistrationRequestEsnHigh registrationRequestEsn = (RegistrationRequestEsnHigh)message;

                        if(registrationRequestEsn.isCompleteEsn())
                        {
                            getIdentifierCollection().update(registrationRequestEsn.getESN());
                            mESNIdentifiers.add(registrationRequestEsn.getESN());
                        }

                        broadcast(DecodeEvent.builder(message.getTimestamp())
                            .eventDescription("Registration Request")
                            .details(registrationRequestEsn.toString())
                            .identifiers(getIdentifierCollection().copyOf())
                            .channel(getCurrentChannel())
                            .protocol(Protocol.LTR_NET)
                            .build());

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
                    }
                    break;
                case ISW_REGISTRATION_REQUEST_ESN_LOW:
                    if(message instanceof RegistrationRequestEsnLow)
                    {
                        RegistrationRequestEsnLow registrationRequestEsn = (RegistrationRequestEsnLow)message;

                        if(registrationRequestEsn.isCompleteEsn())
                        {
                            getIdentifierCollection().update(registrationRequestEsn.getESN());
                            mESNIdentifiers.add(registrationRequestEsn.getESN());
                        }

                        broadcast(DecodeEvent.builder(message.getTimestamp())
                            .eventDescription("Registration Request")
                            .details(registrationRequestEsn.toString())
                            .identifiers(getIdentifierCollection().copyOf())
                            .channel(getCurrentChannel())
                            .protocol(Protocol.LTR_NET)
                            .build());

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
                    }
                    break;
                case ISW_REQUEST_ACCESS:
                    if(message instanceof RequestAccess)
                    {
                        RequestAccess requestAccess = (RequestAccess)message;

                        getIdentifierCollection().update(requestAccess.getTalkgroup());
                        mTalkgroups.add(requestAccess.getTalkgroup());

                        broadcast(DecodeEvent.builder(message.getTimestamp())
                            .eventDescription("Access Request")
                            .details(requestAccess.toString())
                            .identifiers(getIdentifierCollection().copyOf())
                            .channel(getCurrentChannel())
                            .protocol(Protocol.LTR_NET)
                            .build());

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
                    }
                    break;
                case ISW_UNIQUE_ID:
                    if(message instanceof IswUniqueId)
                    {
                        IswUniqueId iswUniqueId = (IswUniqueId)message;
                        getIdentifierCollection().update(iswUniqueId.getUniqueID());
                        mUniqueIdentifiers.add(iswUniqueId.getUniqueID());
                        updateCurrentCallIdentifiers();
                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
                    }
                    break;
                case ISW_UNKNOWN:
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_CALL_END:
                    if(message instanceof OswCallEnd)
                    {
                        OswCallEnd callEnd = (OswCallEnd)message;
                        mTalkgroups.add(callEnd.getTalkgroup());
                        processCallEnd(callEnd.getChannel(), callEnd.getTalkgroup(), callEnd.getTimestamp());
                    }
                    break;
                case OSW_CALL_START:
                    if(message instanceof OswCallStart)
                    {
                        OswCallStart callStart = (OswCallStart)message;
                        mTalkgroups.add(callStart.getTalkgroup());
                        processCallStart(callStart.getChannel(), callStart.getTalkgroup(), callStart.getTimestamp(),
                            MessageDirection.OSW);
                    }
                    break;
                case OSW_CHANNEL_MAP_HIGH:
                    if(message instanceof ChannelMapHigh)
                    {
                        mChannelMapHigh = (ChannelMapHigh)message;
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_CHANNEL_MAP_LOW:
                    if(message instanceof ChannelMapLow)
                    {
                        mChannelMapLow = (ChannelMapLow)message;
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_SYSTEM_IDLE:
                    if(message instanceof SystemIdle)
                    {
                        setCurrentChannelNumber(((SystemIdle)message).getChannel());
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_NEIGHBOR_ID:
                    if(message instanceof NeighborId)
                    {
                        NeighborId neighborId = (NeighborId)message;
                        mNeighborMap.put(neighborId.getNeighborRank(), neighborId);
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_RECEIVE_FREQUENCY_HIGH:
                    if(message instanceof ReceiveFrequencyHigh)
                    {
                        ReceiveFrequencyHigh receiveFrequency = (ReceiveFrequencyHigh)message;
                        updateReceiveFrequency(receiveFrequency.getChannel(), receiveFrequency.getFrequency());
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_RECEIVE_FREQUENCY_LOW:
                    if(message instanceof ReceiveFrequencyLow)
                    {
                        ReceiveFrequencyLow receiveFrequency = (ReceiveFrequencyLow)message;
                        updateReceiveFrequency(receiveFrequency.getChannel(), receiveFrequency.getFrequency());
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_REGISTRATION_ACCEPT:
                    if(message instanceof RegistrationAccept)
                    {
                        RegistrationAccept registrationAccept = (RegistrationAccept)message;
                        mUniqueIdentifiers.add(registrationAccept.getUniqueID());
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(message.getIdentifiers());

                        broadcast(DecodeEvent.builder(message.getTimestamp())
                            .protocol(Protocol.LTR_NET)
                            .channel(getCurrentChannel())
                            .identifiers(ic)
                            .eventDescription("Registration Accept")
                            .details(registrationAccept.toString())
                            .build());
                    }
                    break;
                case OSW_SITE_ID:
                    if(message instanceof SiteId)
                    {
                        mCurrentSite = (SiteId)message;
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_TRANSMIT_FREQUENCY_HIGH:
                    if(message instanceof TransmitFrequencyHigh)
                    {
                        TransmitFrequencyHigh transmitFrequency = (TransmitFrequencyHigh)message;
                        updateTransmitFrequency(transmitFrequency.getChannel(), transmitFrequency.getFrequency());
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_TRANSMIT_FREQUENCY_LOW:
                    if(message instanceof TransmitFrequencyLow)
                    {
                        TransmitFrequencyLow transmitFrequency = (TransmitFrequencyLow)message;
                        updateTransmitFrequency(transmitFrequency.getChannel(), transmitFrequency.getFrequency());
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_UNKNOWN:
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
            }
        }
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary\n");
        sb.append("Decoder:\tLTR-Net\n\n");
        sb.append("Site:\t").append(mCurrentSite != null ? mCurrentSite.getSiteID() : "Unknown").append("\n");

        sb.append("\nLCNs (transmit / receive)\n");

        if(mChannelMapLow != null)
        {
            for(int channel: mChannelMapLow.getChannels())
            {
                LtrNetChannel ltrNetChannel = mChannelMap.get(channel);
                sb.append("  ").append(channel).append(": ").append(ltrNetChannel != null ? ltrNetChannel.description() :
                    "Unknown").append(mCurrentChannelNumber == channel ? " (Current)\n" : "\n");
            }
        }
        else
        {
            sb.append("Channel Map 1-10: Unknown\n");
        }

        if(mChannelMapHigh != null)
        {
            for(int channel: mChannelMapHigh.getChannels())
            {
                LtrNetChannel ltrNetChannel = mChannelMap.get(channel);
                sb.append("  ").append(channel).append(": ").append(ltrNetChannel != null ? ltrNetChannel.description() :
                    "Unknown").append(mCurrentChannelNumber == channel ? " (Current)\n" : "\n");
            }
        }
        else
        {
            sb.append("Channel Map 11-20: Unknown\n");
        }

        sb.append("\nNeighbor Sites (Rank: ID)\n");

        if(mNeighborMap.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            List<Integer> ranks = new ArrayList<>(mNeighborMap.keySet());
            Collections.sort(ranks);
            for(Integer rank: ranks)
            {
                sb.append("  ").append(rank).append(": ").append(mNeighborMap.get(rank).getNeighborID()).append("\n");
            }
        }

        sb.append("\nActive Talkgroups\n");

        if(mTalkgroups.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            List<LTRTalkgroup> talkgroups = new ArrayList<>(mTalkgroups);
            Collections.sort(talkgroups, Comparator.comparingInt(Identifier::getValue));

            for(LTRTalkgroup talkgroup: talkgroups)
            {
                sb.append("  ").append(talkgroup.formatted()).append("\n");
            }
        }

        sb.append("\nActive Radio Unique IDs\n");

        if(mUniqueIdentifiers.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            List<UniqueIdentifier> uniqueIdentifiers = new ArrayList<>(mUniqueIdentifiers);
            Collections.sort(uniqueIdentifiers, Comparator.comparingInt(Identifier::getValue));

            for(UniqueIdentifier uniqueIdentifier: uniqueIdentifiers)
            {
                sb.append("  ").append(uniqueIdentifier).append("\n");
            }
        }

        sb.append("\nActive ESNs\n");

        if(mESNIdentifiers.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            List<ESNIdentifier> esnIdentifiers = new ArrayList<>(mESNIdentifiers);
            Collections.sort(esnIdentifiers);

            for(ESNIdentifier esnIdentifier: esnIdentifiers)
            {
                sb.append("  ").append(esnIdentifier).append("\n");
            }
        }

        return sb.toString();
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

    /**
     * Resets the decoder state after a call or other decode event
     */
    protected void resetState()
    {
        super.resetState();
        mCurrentCallEvent = null;
        mCurrentCallTalkgroup = null;
    }

    public void setCurrentChannelNumber(int channelNumber)
    {
        if(0 < channelNumber && channelNumber <= 20)
        {
            mCurrentChannelNumber = channelNumber;

            LtrNetChannel currentChannel = mChannelMap.get(mCurrentChannelNumber);

            if(currentChannel != null)
            {
                getIdentifierCollection().update(DecoderLogicalChannelNameIdentifier
                    .create(String.valueOf(mCurrentChannelNumber), Protocol.LTR_NET));
            }
        }
    }

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
