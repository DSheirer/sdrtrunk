/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.module.decode.ltrnet;

import io.github.dsheirer.module.decode.event.ActivitySummary;
import io.github.dsheirer.channel.IBandChannelDescriptor;
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
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.ltrnet.channel.LtrNetChannel;
import io.github.dsheirer.module.decode.ltrnet.identifier.LtrNetRadioIdentifier;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Set<LtrNetRadioIdentifier> mLtrNetRadioIdentifiers = new TreeSet<>();
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
    protected IBandChannelDescriptor getCurrentChannel()
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
        super.reset();
        mChannelMapHigh = null;
        mChannelMapLow = null;
        mChannelMap.clear();
        mCallDetectMap.clear();
        mNeighborMap.clear();
        mCurrentSite = null;
        mCurrentChannelNumber = 0;
        mTalkgroups.clear();
        mLtrNetRadioIdentifiers.clear();
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
                DecodeEventType decodeEventType = null;

                if(talkgroup.getTalkgroup() == 253)
                {
                    decodeEventType = DecodeEventType.REGISTER;
                }
                else if(talkgroup.getTalkgroup() == 254)
                {
                    decodeEventType = DecodeEventType.STATION_ID;
                }
                else
                {
                    decodeEventType = DecodeEventType.CALL;
                }

                getIdentifierCollection().remove(IdentifierClass.USER);
                getIdentifierCollection().update(talkgroup);
                mCurrentCallEvent = LTRNetDecodeEvent.builder(decodeEventType, timestamp)
                    .channel(getCurrentChannel())
                    .identifiers(getIdentifierCollection().copyOf())
                    .build();
                mCurrentCallTalkgroup = talkgroup;
            }

            //This updates the timestamp
            mCurrentCallEvent.update(timestamp);

            if(talkgroup.getTalkgroup() == 253)
            {
                broadcast(new DecoderStateEvent(this, Event.START, State.DATA));
            }
            else if(talkgroup.getTalkgroup() == 254)
            {
                broadcast(new DecoderStateEvent(this, Event.START, State.DATA));
            }
            else
            {
                broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
            }

            broadcast(mCurrentCallEvent);
        }
        else
        {
            DecodeEvent decodeEvent = mCallDetectMap.get(channel);

            if(decodeEvent == null)
            {
                decodeEvent = getDecodeEvent(talkgroup, channel, timestamp, DecodeEventType.CALL_DETECT);
                mCallDetectMap.put(channel, decodeEvent);
            }
            else
            {
                Identifier eventTalkgroup = decodeEvent.getIdentifierCollection()
                    .getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.TO);

                if(eventTalkgroup == null || !eventTalkgroup.equals(talkgroup) ||
                    (timestamp - decodeEvent.getTimeStart() - decodeEvent.getDuration() > 2000))
                {
                    decodeEvent = getDecodeEvent(talkgroup, channel, timestamp, DecodeEventType.CALL_DETECT);
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
                    if(message instanceof IswCallEnd callEnd)
                    {
                        mTalkgroups.add(callEnd.getTalkgroup());
                        processCallEnd(callEnd.getChannel(), callEnd.getTalkgroup(), callEnd.getTimestamp());
                    }
                    break;
                case ISW_CALL_START:
                    if(message instanceof IswCallStart callStart)
                    {
                        mTalkgroups.add(callStart.getTalkgroup());
                        processCallStart(callStart.getChannel(), callStart.getTalkgroup(), callStart.getTimestamp(),
                            MessageDirection.ISW);
                    }
                    break;
                case ISW_REGISTRATION_REQUEST_ESN_HIGH:
                    if(message instanceof RegistrationRequestEsnHigh registrationRequestEsn)
                    {
                        if(registrationRequestEsn.isCompleteEsn())
                        {
                            getIdentifierCollection().update(registrationRequestEsn.getESN());
                            mESNIdentifiers.add(registrationRequestEsn.getESN());
                        }

                        broadcast(getDecodeEvent(message, DecodeEventType.REQUEST, "REGISTER: " + registrationRequestEsn));

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
                    }
                    break;
                case ISW_REGISTRATION_REQUEST_ESN_LOW:
                    if(message instanceof RegistrationRequestEsnLow registrationRequestEsn)
                    {
                        if(registrationRequestEsn.isCompleteEsn())
                        {
                            getIdentifierCollection().update(registrationRequestEsn.getESN());
                            mESNIdentifiers.add(registrationRequestEsn.getESN());
                        }

                        broadcast(getDecodeEvent(message, DecodeEventType.REQUEST, "REGISTER: " + registrationRequestEsn));
                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
                    }
                    break;
                case ISW_REQUEST_ACCESS:
                    if(message instanceof RequestAccess requestAccess)
                    {
                        getIdentifierCollection().update(requestAccess.getTalkgroup());
                        mTalkgroups.add(requestAccess.getTalkgroup());
                        broadcast(getDecodeEvent(message, DecodeEventType.REQUEST, "ACCESS: " + requestAccess));
                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
                    }
                    break;
                case ISW_UNIQUE_ID:
                    if(message instanceof IswUniqueId iswUniqueId)
                    {
                        getIdentifierCollection().update(iswUniqueId.getUniqueID());
                        mLtrNetRadioIdentifiers.add(iswUniqueId.getUniqueID());
                        updateCurrentCallIdentifiers();
                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
                    }
                    break;
                case ISW_UNKNOWN:
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_CALL_END:
                    if(message instanceof OswCallEnd callEnd)
                    {
                        mTalkgroups.add(callEnd.getTalkgroup());
                        processCallEnd(callEnd.getChannel(), callEnd.getTalkgroup(), callEnd.getTimestamp());
                    }
                    break;
                case OSW_CALL_START:
                    if(message instanceof OswCallStart callStart)
                    {
                        mTalkgroups.add(callStart.getTalkgroup());
                        processCallStart(callStart.getChannel(), callStart.getTalkgroup(), callStart.getTimestamp(),
                            MessageDirection.OSW);
                    }
                    break;
                case OSW_CHANNEL_MAP_HIGH:
                    if(message instanceof ChannelMapHigh channelMapHigh)
                    {
                        mChannelMapHigh = channelMapHigh;
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_CHANNEL_MAP_LOW:
                    if(message instanceof ChannelMapLow channelMapLow)
                    {
                        mChannelMapLow = channelMapLow;
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_SYSTEM_IDLE:
                    if(message instanceof SystemIdle systemIdle)
                    {
                        setCurrentChannelNumber(systemIdle.getChannel());
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_NEIGHBOR_ID:
                    if(message instanceof NeighborId neighborId)
                    {
                        mNeighborMap.put(neighborId.getNeighborRank(), neighborId);
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_RECEIVE_FREQUENCY_HIGH:
                    if(message instanceof ReceiveFrequencyHigh receiveFrequency)
                    {
                        updateReceiveFrequency(receiveFrequency.getChannel(), receiveFrequency.getFrequency());
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_RECEIVE_FREQUENCY_LOW:
                    if(message instanceof ReceiveFrequencyLow receiveFrequency)
                    {
                        updateReceiveFrequency(receiveFrequency.getChannel(), receiveFrequency.getFrequency());
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_REGISTRATION_ACCEPT:
                    if(message instanceof RegistrationAccept registrationAccept)
                    {
                        mLtrNetRadioIdentifiers.add(registrationAccept.getUniqueID());
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(message.getIdentifiers());

                        broadcast(getDecodeEvent(message, DecodeEventType.RESPONSE, "REGISTRATION ACCEPT: " + registrationAccept));
                    }
                    break;
                case OSW_SITE_ID:
                    if(message instanceof SiteId siteId)
                    {
                        mCurrentSite = siteId;
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_TRANSMIT_FREQUENCY_HIGH:
                    if(message instanceof TransmitFrequencyHigh transmitFrequency)
                    {
                        updateTransmitFrequency(transmitFrequency.getChannel(), transmitFrequency.getFrequency());
                    }
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
                    break;
                case OSW_TRANSMIT_FREQUENCY_LOW:
                    if(message instanceof TransmitFrequencyLow transmitFrequency)
                    {
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

    private DecodeEvent getDecodeEvent(IMessage message, DecodeEventType decodeEventType, String details)
    {
        return LTRNetDecodeEvent.builder(decodeEventType, message.getTimestamp())
                .details(details)
                .identifiers(getIdentifierCollection().copyOf())
                .channel(getCurrentChannel())
                .build();
    }

    private DecodeEvent getDecodeEvent(LTRTalkgroup talkgroup, int channel, long timestamp, DecodeEventType decodeEventType)
    {
        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        ic.remove(IdentifierClass.USER);
        ic.update(talkgroup);

        return LTRNetDecodeEvent.builder(decodeEventType, timestamp)
                .channel(mChannelMap.get(channel))
                .identifiers(ic)
                .build();
    }

    @Override
    public String getActivitySummary()
    {
        ActivitySummary summary = new ActivitySummary("LTR-Net");
        summary.section("Site");
        summary.field("Site", mCurrentSite != null ? mCurrentSite.getSiteID() : null);

        summary.section("LCNs");
        ActivitySummary.Table lcns = summary.table("LCN", "Transmit / Receive", "Current");

        if(mChannelMapLow != null)
        {
            addChannels(lcns, mChannelMapLow.getChannels());
        }
        else
        {
            lcns.row("1-10", ActivitySummary.UNKNOWN);
        }

        if(mChannelMapHigh != null)
        {
            addChannels(lcns, mChannelMapHigh.getChannels());
        }
        else
        {
            lcns.row("11-20", ActivitySummary.UNKNOWN);
        }

        summary.section("Neighbor Sites");
        ActivitySummary.Table neighbors = summary.table("Rank", "Site ID");
        List<Integer> ranks = new ArrayList<>(mNeighborMap.keySet());
        Collections.sort(ranks);

        for(Integer rank: ranks)
        {
            neighbors.row(rank, mNeighborMap.get(rank).getNeighborID());
        }

        summary.section("Active Talkgroups");
        List<LTRTalkgroup> talkgroups = new ArrayList<>(mTalkgroups);
        talkgroups.sort(Comparator.comparingInt(Identifier::getValue));

        for(LTRTalkgroup talkgroup: talkgroups)
        {
            summary.text(talkgroup.formatted());
        }

        summary.section("Active Radio Unique IDs");
        List<LtrNetRadioIdentifier> ltrNetRadioIdentifiers = new ArrayList<>(mLtrNetRadioIdentifiers);
        ltrNetRadioIdentifiers.sort(Comparator.comparingInt(Identifier::getValue));

        for(LtrNetRadioIdentifier ltrNetRadioIdentifier : ltrNetRadioIdentifiers)
        {
            summary.text(ltrNetRadioIdentifier);
        }

        summary.section("Active ESNs");
        List<ESNIdentifier> esnIdentifiers = new ArrayList<>(mESNIdentifiers);
        Collections.sort(esnIdentifiers);

        for(ESNIdentifier esnIdentifier: esnIdentifiers)
        {
            summary.text(esnIdentifier);
        }

        return summary.toString();
    }

    /**
     * Adds a row for each channel to the LCN table.
     */
    private void addChannels(ActivitySummary.Table table, List<Integer> channels)
    {
        for(int channel: channels)
        {
            LtrNetChannel ltrNetChannel = mChannelMap.get(channel);
            table.row(channel, ltrNetChannel != null ? ltrNetChannel.description() : ActivitySummary.UNKNOWN,
                mCurrentChannelNumber == channel ? "Yes" : null);
        }
    }

    @Override
    public void init() {}

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
            case REQUEST_RESET:
                resetState();
                break;
            default:
                break;
        }
    }
}
