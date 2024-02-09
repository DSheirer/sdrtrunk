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
package io.github.dsheirer.module.decode.passport;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageType;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.passport.identifier.PassportRadioId;
import io.github.dsheirer.module.decode.passport.identifier.PassportTalkgroup;
import io.github.dsheirer.protocol.Protocol;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PassportDecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(PassportDecoderState.class);

    private Set<PassportTalkgroup> mTalkgroupsFirstHeard = new HashSet<>();
    private Set<PassportTalkgroup> mTalkgroups = new TreeSet<>();
    private Set<PassportRadioId> mMobileIDs = new TreeSet<>();
    private Map<Integer,Long> mSiteLCNs = new HashMap<>();
    private Map<Integer,Long> mNeighborLCNs = new HashMap<>();
    private Map<Integer,DecodeEvent> mDetectedCalls = new HashMap<>();
    private DecodeEvent mCurrentDecodeEvent;
    private int mChannelNumber;
    private int mSiteNumber;
    private PassportBand mSiteBand;
    private long mFrequency;

    public PassportDecoderState()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.PASSPORT;
    }

    private void addTalkgroup(PassportTalkgroup talkgroup)
    {
        if(mTalkgroupsFirstHeard.contains(talkgroup))
        {
            mTalkgroups.add(talkgroup);
        }
        else
        {
            mTalkgroupsFirstHeard.add(talkgroup);
        }
    }

    /**
     * Compares the talkgroups for equality
     */
    private boolean isSameTalkgroup(Identifier id1, Identifier id2)
    {
        return Objects.equals(id1, id2);
    }

    /**
     * Retrieves the first identifier with a TO role.
     *
     * @param collection containing a TO identifier
     * @return TO identifier or null
     */
    private Identifier getToIdentifier(IdentifierCollection collection)
    {
        List<Identifier> identifiers = collection.getIdentifiers(Role.TO);

        if(identifiers.size() >= 1)
        {
            return identifiers.get(0);
        }

        return null;
    }


    @Override
    public void receive(IMessage message)
    {
        if(message instanceof PassportMessage && message.isValid())
        {
            PassportMessage passport = (PassportMessage)message;

            if(passport.isValid())
            {
                switch(passport.getMessageType())
                {
                    case CA_STRT:
                    case DA_STRT:
                        mSiteLCNs.put(passport.getLCN(), passport.getLCNFrequency());

                        PassportTalkgroup to = passport.getToIdentifier();
                        addTalkgroup(to);

                        if(mChannelNumber == 0)
                        {
                            setChannelNumber(passport.getLCN());
                        }

                        /* Call on this channel */
                        if(passport.getLCN() == mChannelNumber)
                        {
                            getIdentifierCollection().update(to);

                            if(mCurrentDecodeEvent != null)
                            {
                                if(isSameTalkgroup(getToIdentifier(mCurrentDecodeEvent.getIdentifierCollection()), to))
                                {
                                    mCurrentDecodeEvent.update(passport.getTimestamp());
                                    broadcast(mCurrentDecodeEvent);
                                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));
                                    return;
                                }
                                else
                                {
                                    mCurrentDecodeEvent.end(message.getTimestamp());
                                    broadcast(mCurrentDecodeEvent);
                                    mCurrentDecodeEvent = null;
                                }
                            }

                            DecodeEventType decodeEventType = passport.getMessageType() == MessageType.CA_STRT ?
                                    DecodeEventType.CALL : DecodeEventType.DATA_CALL;

                            mCurrentDecodeEvent = PassportDecodeEvent.builder(decodeEventType, passport.getTimestamp())
                                .identifiers(getIdentifierCollection().copyOf())
                                .details(passport.toString())
                                .build();
                            broadcast(mCurrentDecodeEvent);
                            broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
                        }
                        else
                        {
                            //Call Detection
                            int lcn = passport.getLCN();

                            DecodeEvent callDetect = mDetectedCalls.get(lcn);

                            if(callDetect == null ||
                                !isSameTalkgroup(to, getToIdentifier(callDetect.getIdentifierCollection())) ||
                                callDetect.getTimeStart() < (passport.getTimestamp() - 45000))
                            {
                                callDetect = PassportDecodeEvent.builder(DecodeEventType.CALL_DETECT, passport.getTimestamp())
                                    .identifiers(new IdentifierCollection(passport.getIdentifiers()))
//                                    .channel(...)
                                    .build();
                                mDetectedCalls.put(lcn, callDetect);
                            }
                            else
                            {
                                callDetect.update(passport.getTimestamp());
                            }

                            broadcast(callDetect);
                        }
                        break;
                    case CA_ENDD:
                    case DA_ENDD:
                        PassportTalkgroup endTalkgroup = passport.getToIdentifier();
                        addTalkgroup(endTalkgroup);

                        if(mCurrentDecodeEvent != null && isSameTalkgroup(endTalkgroup, getToIdentifier(mCurrentDecodeEvent.getIdentifierCollection())))
                        {
                            mCurrentDecodeEvent.end(passport.getTimestamp());
                            mCurrentDecodeEvent = null;
                        }

                        broadcast(new DecoderStateEvent(this, Event.END, State.CALL));
                        break;
                    case ID_RDIO:
                        PassportRadioId mobileId = passport.getFromIdentifier();
                        mMobileIDs.add(mobileId);
                        getIdentifierCollection().update(mobileId);

                        if(mCurrentDecodeEvent != null)
                        {
                            mCurrentDecodeEvent.setIdentifierCollection(getIdentifierCollection().copyOf());
                            mCurrentDecodeEvent.update(passport.getTimestamp());
                            broadcast(mCurrentDecodeEvent);
                        }
                        break;
                    case RA_REGI:
                        broadcast(PassportDecodeEvent.builder(DecodeEventType.REGISTER, passport.getTimestamp())
                            .identifiers(new IdentifierCollection(passport.getIdentifiers()))
                            .build());

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
                        break;
                    case SY_IDLE:
                        if(passport.getFree() != 0)
                        {
                            mNeighborLCNs.put(passport.getFree(), passport.getFreeFrequency());
                        }
                        setSiteNumber(passport.getSite());

                        PassportBand band = passport.getSiteBand();
                        setSiteBand(band);
                        setChannelNumber(getSiteBand().getChannel(mFrequency));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary\n");
        sb.append("Decoder:\tPassport\n\n");
        sb.append("Site Channels\n");

        if(mSiteLCNs.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            mSiteLCNs.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        sb.append("  " + entry.getKey());
                        sb.append("\t" + entry.getValue());
                        sb.append("\n");
                    });
        }

        sb.append("\nNeighbor Channels\n");

        if(mNeighborLCNs.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            mNeighborLCNs.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        sb.append("  " + entry.getKey());
                        sb.append("\t" + entry.getValue());
                        sb.append("\n");
                    });
        }

        sb.append("\nTalkgroups\n");

        if(mTalkgroups.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {

            for (PassportTalkgroup mTalkgroup : mTalkgroups) {
                sb.append("  ").append(mTalkgroup).append("\n");
            }
        }

        sb.append("\nMobile ID Numbers\n");

        if(mMobileIDs.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            for (PassportRadioId mMobileID : mMobileIDs) {
                sb.append("  ").append(mMobileID).append("\n");
            }
        }

        return sb.toString();
    }

    public void reset()
    {
        super.reset();
        getIdentifierCollection().remove(IdentifierClass.USER);
        mTalkgroupsFirstHeard.clear();
        mTalkgroups.clear();
        mMobileIDs.clear();
        mSiteLCNs.clear();
        mNeighborLCNs.clear();

        resetState();
    }

    @Override
    public void init() {}

    protected void resetState()
    {
        super.resetState();

        if(mCurrentDecodeEvent != null)
        {
            mCurrentDecodeEvent.end(System.currentTimeMillis());
            mCurrentDecodeEvent = null;
        }
    }

    public int getChannelNumber()
    {
        return mChannelNumber;
    }

    private void setChannelNumber(int channel)
    {
        if(mChannelNumber != channel)
        {
            mChannelNumber = channel;
            getIdentifierCollection().update(DecoderLogicalChannelNameIdentifier.create(String.valueOf(mChannelNumber),
                Protocol.PASSPORT));
        }
    }

    public int getSiteNumber()
    {
        return mSiteNumber;
    }

    private void setSiteNumber(int site)
    {
        if(mSiteNumber != site)
        {
            mSiteNumber = site;
        }
    }

    private PassportBand getSiteBand()
    {
        return mSiteBand;
    }

    private void setSiteBand(PassportBand band)
    {
        if(mSiteBand == null)
        {
            mSiteBand = band;
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
            case NOTIFICATION_SOURCE_FREQUENCY:
                mFrequency = event.getFrequency();
                break;
            default:
                break;
        }
    }
}
