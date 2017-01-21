/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package module.decode.ltrnet;

import alias.Alias;
import alias.AliasList;
import alias.id.AliasIDType;
import channel.metadata.AliasedIntegerAttributeMonitor;
import channel.metadata.AliasedStringAttributeMonitor;
import channel.metadata.Attribute;
import channel.metadata.AttributeChangeRequest;
import channel.state.DecoderState;
import channel.state.DecoderStateEvent;
import channel.state.DecoderStateEvent.Event;
import channel.state.State;
import message.Message;
import module.decode.DecoderType;
import module.decode.event.CallEvent;
import module.decode.event.CallEvent.CallEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StringUtils;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

public class LTRNetDecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(LTRNetDecoderState.class);

    private DecimalFormat mDecimalFormatter = new DecimalFormat("0.00000");

    private TreeSet<String> mTalkgroups = new TreeSet<String>();
    private TreeSet<String> mTalkgroupsFirstHeard = new TreeSet<String>();
    private TreeSet<String> mESNs = new TreeSet<String>();
    private TreeSet<Integer> mUniqueIDs = new TreeSet<Integer>();
    private TreeSet<String> mNeighborIDs = new TreeSet<String>();
    private TreeSet<String> mSiteIDs = new TreeSet<String>();
    private HashMap<Integer,Long> mReceiveFrequencies =
        new HashMap<Integer,Long>();
    private HashMap<Integer,Long> mTransmitFrequencies =
        new HashMap<Integer,Long>();
    private HashMap<Integer,String> mActiveCalls = new HashMap<Integer,String>();

    private AliasedStringAttributeMonitor mToAttribute;
    private AliasedIntegerAttributeMonitor mFromUIDAttribute;
    private AliasedStringAttributeMonitor mESNAttribute;
    private String mMessage;
    private String mMessageType;
    private int mChannelNumber;
    private long mFrequency = 0;

    public LTRNetDecoderState(AliasList aliasList)
    {
        super(aliasList);

        mToAttribute = new AliasedStringAttributeMonitor(Attribute.PRIMARY_ADDRESS_TO,
            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.TALKGROUP);
        mToAttribute.addIllegalValue("0-00-000");

        mFromUIDAttribute = new AliasedIntegerAttributeMonitor(Attribute.PRIMARY_ADDRESS_TO,
            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.LTR_NET_UID);

        mESNAttribute = new AliasedStringAttributeMonitor(Attribute.SECONDARY_ADDRESS_FROM,
            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.ESN);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.LTR_NET;
    }

    @Override
    public void receive(Message message)
    {
        if(message.isValid())
        {
            State state = State.IDLE;

            if(message instanceof LTRNetOSWMessage)
            {
                LTRNetOSWMessage ltr = (LTRNetOSWMessage) message;

                switch(ltr.getMessageType())
                {
                    case CA_ENDD:
                        if(mChannelNumber == 0)
                        {
                            setChannelNumber(ltr.getChannel());
                        }

						/* Process FCC Station ID Events */
                        if(ltr.getGroup() == 254)
                        {
                            if(mCurrentCallEvent == null ||
                                mCurrentCallEvent.getCallEventType() !=
                                    CallEventType.STATION_ID)
                            {
                                mCurrentCallEvent = new LTRCallEvent.Builder(
                                    DecoderType.LTR_NET, CallEventType.STATION_ID)
                                    .aliasList(getAliasList())
                                    .channel(String.valueOf(mChannelNumber))
                                    .frequency(mFrequency)
                                    .to(ltr.getTalkgroupID())
                                    .build();

                                broadcast(mCurrentCallEvent);

                                broadcast(new DecoderStateEvent(this, Event.START, State.DATA));
                            }
                            else
                            {
                                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
                            }
                        }
                        else
                        {
                            processCallEndMessage(ltr);
                        }
                        break;
                    case CA_STRT:
                        if(mChannelNumber == 0)
                        {
                            setChannelNumber(ltr.getChannel());
                        }

						/* If the call event channel matches our current channel
                         * then it's a call, otherwise it's a call detect. */
                        if(ltr.getChannel() == mChannelNumber)
                        {
                            processCallMessage(ltr);
                        }
                        else
                        {
                            processCallDetectMessage(ltr);
                        }

                        break;
                    case SY_IDLE:
                        if(mChannelNumber != ltr.getChannel())
                        {
                            setChannelNumber(ltr.getChannel());
                        }
                        break;
                    case MA_CHNH:
                        break;
                    case MA_CHNL:
                        break;
                    case FQ_RXHI:
                    case FQ_RXLO:
                        if(ltr.getFrequency() > 0)
                        {
                            mReceiveFrequencies.put(ltr.getHomeRepeater(), ltr.getFrequency());
                        }
                        break;
                    case FQ_TXHI:
                    case FQ_TXLO:
                        if(ltr.getFrequency() > 0)
                        {
                            mTransmitFrequencies.put(ltr.getHomeRepeater(), ltr.getFrequency());
                        }
                        break;
                    case ID_NBOR:
                        String neighborID = ltr.getNeighborID();

                        if(neighborID != null)
                        {
                            mNeighborIDs.add(neighborID);
                        }
                        break;
                    case ID_UNIQ:
                        state = State.DATA;

                        int uniqueID = ltr.getRadioUniqueID();

                        if(uniqueID != LTRNetOSWMessage.INT_NULL_VALUE)
                        {
                            mUniqueIDs.add(uniqueID);
                        }

                        if(getCurrentLTRCallEvent() == null)
                        {
                            mCurrentCallEvent = new LTRCallEvent.Builder(
                                DecoderType.LTR_NET, CallEventType.REGISTER)
                                .aliasList(getAliasList())
                                .channel(String.valueOf(mChannelNumber))
                                .frequency(mFrequency)
                                .from(String.valueOf(uniqueID))
                                .build();
                        }
                        else
                        {
                            mCurrentCallEvent.setFromID(
                                String.valueOf(ltr.getRadioUniqueID()));
                            mCurrentCallEvent.setDetails("Unique ID");

                            broadcast(mCurrentCallEvent);
                        }

                        mFromUIDAttribute.process(uniqueID);
                        break;
                    case ID_SITE:
                        String siteID = ltr.getSiteID();

                        if(siteID != null)
                        {
                            mSiteIDs.add(siteID);
                        }
                        break;
                    default:
                        break;
                }
            }
            else if(message instanceof LTRNetISWMessage)
            {
                LTRNetISWMessage ltr = ((LTRNetISWMessage) message);

                switch(ltr.getMessageType())
                {
                    case CA_STRT:
                        processCallMessage(ltr);
                        break;
                    case CA_ENDD:
                        processCallEndMessage(ltr);
                        break;
                    case ID_ESNH:
                    case ID_ESNL:
                        state = State.DATA;

                        String esn = ltr.getESN();

                        if(!esn.contains("xxxx"))
                        {
                            mESNs.add(ltr.getESN());
                        }

                        setMessageType("ESN");
                        mESNAttribute.process(ltr.getESN());

                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));

                        if(mCurrentCallEvent == null)
                        {
                            mCurrentCallEvent = new LTRCallEvent.Builder(
                                DecoderType.LTR_NET, CallEventType.REGISTER_ESN)
                                .aliasList(getAliasList())
                                .details("ESN:" + ltr.getESN())
                                .frequency(mFrequency)
                                .from(ltr.getESN())
                                .build();

                            broadcast(mCurrentCallEvent);
                        }

                        break;
                    case ID_UNIQ:
                        state = State.DATA;

                        int uniqueid = ltr.getRadioUniqueID();

                        if(uniqueid != LTRNetISWMessage.INT_NULL_VALUE)
                        {
                            mUniqueIDs.add(uniqueid);

                            setMessageType("REGISTER UID");
                            mFromUIDAttribute.process(uniqueid);

                            if(getCurrentLTRCallEvent() == null)
                            {
                                mCurrentCallEvent = new LTRCallEvent.Builder(
                                    DecoderType.LTR_NET, CallEventType.REGISTER)
                                    .aliasList(getAliasList())
                                    .channel(String.valueOf(mChannelNumber))
                                    .frequency(mFrequency)
                                    .from(String.valueOf(uniqueid))
                                    .build();
                            }
                            else
                            {
                                mCurrentCallEvent.setFromID(
                                    String.valueOf(ltr.getRadioUniqueID()));
                                mCurrentCallEvent.setDetails("Unique ID");

                                broadcast(mCurrentCallEvent);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public LTRCallEvent getCurrentLTRCallEvent()
    {
        if(mCurrentCallEvent != null)
        {
            return (LTRCallEvent) mCurrentCallEvent;
        }

        return null;
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary\n");
        sb.append("Decoder:\tLTR-Net\n\n");

        if(mSiteIDs.isEmpty())
        {
            sb.append("Site:\tUnknown\n");
        }
        else
        {
            Iterator<String> it = mSiteIDs.iterator();

            while(it.hasNext())
            {
                sb.append("Site:\t");

                String siteID = it.next();

                sb.append(siteID);

                if(hasAliasList())
                {
                    Alias siteAlias = getAliasList().getSiteID(String.valueOf(siteID));

                    if(siteAlias != null)
                    {
                        sb.append(" ");
                        sb.append(siteAlias.getName());
                    }
                }

                sb.append("\n");
            }
        }

        sb.append("\nLCNs (transmit | receive)\n");

        if(mReceiveFrequencies.isEmpty() && mTransmitFrequencies.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            for(int x = 1; x < 21; x++)
            {
                long rcv = 0;

                if(mReceiveFrequencies.containsKey(x))
                {
                    rcv = mReceiveFrequencies.get(x);
                }

                long xmt = 0;

                if(mTransmitFrequencies.containsKey(x))
                {
                    xmt = mTransmitFrequencies.get(x);
                }

                if(rcv > 0 || xmt > 0)
                {
                    if(x < 10)
                    {
                        sb.append(" ");
                    }

                    sb.append(x);
                    sb.append(": ");

                    if(xmt == 0)
                    {
                        sb.append("---.-----");
                    }
                    else
                    {
                        sb.append(mDecimalFormatter.format((double) xmt / 1E6d));
                    }
                    sb.append(" | ");

                    if(rcv == 0)
                    {
                        sb.append("---.-----");
                    }
                    else
                    {
                        sb.append(mDecimalFormatter.format((double) rcv / 1E6d));
                    }

                    if(x == mChannelNumber)
                    {
                        sb.append(" **");
                    }

                    sb.append("\n");
                }
            }
        }

        sb.append("\nTalkgroups\n");

        if(mTalkgroups.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            Iterator<String> it = mTalkgroups.iterator();

            while(it.hasNext())
            {
                String tgid = it.next();

                sb.append("  ");
                sb.append(tgid);
                sb.append(" ");

                if(hasAliasList())
                {
                    Alias alias = getAliasList().getTalkgroupAlias(tgid);

                    if(alias != null)
                    {
                        sb.append(alias.getName());
                    }
                }

                sb.append("\n");
            }
        }

        sb.append("\nRadio Unique IDs\n");

        if(mUniqueIDs.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            Iterator<Integer> it = mUniqueIDs.iterator();

            while(it.hasNext())
            {
                int uid = it.next();

                sb.append("  ");
                sb.append(uid);
                sb.append(" ");

                if(hasAliasList())
                {
                    Alias alias = getAliasList().getUniqueID(uid);

                    if(alias != null)
                    {
                        sb.append(alias.getName());
                    }
                }

                sb.append("\n");
            }
        }

        sb.append("\nESNs\n");

        if(mESNs.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            Iterator<String> it = mESNs.iterator();

            while(it.hasNext())
            {
                String esn = it.next();

                sb.append("  ");
                sb.append(esn);
                sb.append(" ");

                if(hasAliasList())
                {
                    Alias alias = getAliasList().getESNAlias(esn);

                    if(alias != null)
                    {
                        sb.append(alias.getName());
                    }
                }

                sb.append("\n");
            }
        }

        sb.append("\nNeighbor Sites\n");

        if(mNeighborIDs.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            Iterator<String> it = mNeighborIDs.iterator();

            while(it.hasNext())
            {
                String neighbor = it.next();

                sb.append("  ");
                sb.append(neighbor);
                sb.append(" ");

                if(hasAliasList())
                {
                    Alias alias = getAliasList().getSiteID(String.valueOf(neighbor));

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

    /**
     * Call Detect - current channel messages indicate a call on another channel
     */
    private void processCallDetectMessage(LTRNetMessage message)
    {
        if(!mActiveCalls.containsKey(message.getChannel()) ||
            !mActiveCalls.get(message.getChannel()).contentEquals(message.getTalkgroupID()))
        {
            mActiveCalls.put(message.getChannel(), message.getTalkgroupID());

            int channel = message.getChannel();

            long frequency = 0;

            if(mTransmitFrequencies.containsKey(channel))
            {
                frequency = mTransmitFrequencies.get(channel);
            }

            broadcast(new LTRCallEvent.Builder(DecoderType.LTR_NET, CallEventType.CALL_DETECT)
                .aliasList(getAliasList())
                .channel(String.valueOf(message.getChannel()))
                .frequency(frequency)
                .to(message.getTalkgroupID())
                .build());
        }

        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.IDLE));
    }

    /**
     * Indicates if the talkgroup is different than the talkgroup specified in
     * the current call event
     */
    private boolean isDifferentTalkgroup(String talkgroup)
    {
        return talkgroup != null &&
            mCurrentCallEvent != null &&
            mCurrentCallEvent.getToID() != null &&
            !mCurrentCallEvent.getToID().contentEquals(talkgroup);
    }

    private boolean isValidTalkgroup(String talkgroup)
    {
        return talkgroup != null && !talkgroup.matches("0-00-000");
    }

    private void processCallMessage(LTRNetMessage message)
    {
        int group = message.getGroup();

		/* Process call registration */
        if(group == 253)
        {
            setMessageType("REGISTER");
            broadcast(new DecoderStateEvent(this, Event.START, State.DATA));
        }
        /* Process call */
        else
        {
            String talkgroup = message.getTalkgroupID();

            if(isValidTalkgroup(talkgroup))
            {
                mToAttribute.process(talkgroup);

                final LTRCallEvent current = getCurrentLTRCallEvent();

    			//If this is a new call or the talkgroup is different from the current call, create a new call event
                if(current == null || isDifferentTalkgroup(talkgroup))
                {
    				/* Invalidate the current call */
                    if(current != null)
                    {
                        current.setValid(false);
                        broadcast(current);
                    }

                    /* A talkgroup must be seen at least once before it will be added
                     * to the mTalkgroups list that is used in the activity summary,
                     * so that we don't pollute the summary with one-off error talkgroups */
                    if(mTalkgroupsFirstHeard.contains(talkgroup))
                    {
                        mTalkgroups.add(talkgroup);
                    }
                    else
                    {
                        mTalkgroupsFirstHeard.add(talkgroup);
                    }

                    CallEvent callEvent = new LTRCallEvent.Builder(DecoderType.LTR_NET, CallEventType.CALL)
                        .aliasList(getAliasList())
                        .channel(String.valueOf(message.getChannel()))
                        .frequency(mFrequency)
                        .to(message.getTalkgroupID())
                        .build();

                    broadcast(callEvent);

                    mCurrentCallEvent = callEvent;

                    broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
                }

                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));
            }
        }
    }

    private void processCallEndMessage(LTRNetMessage message)
    {
        if(mCurrentCallEvent != null &&
            mCurrentCallEvent.getCallEventType() == CallEventType.CALL)
        {
            mCurrentCallEvent.setEnd(System.currentTimeMillis());
            broadcast(mCurrentCallEvent);
        }

        mCurrentCallEvent = null;

        broadcast(new DecoderStateEvent(this, Event.END, State.FADE));
    }

    /**
     * Performs a full reset
     */
    public void reset()
    {
        mChannelNumber = 0;

        mActiveCalls.clear();
        mESNs.clear();
        mNeighborIDs.clear();
        mReceiveFrequencies.clear();
        mSiteIDs.clear();
        mTalkgroups.clear();
        mTransmitFrequencies.clear();
        mUniqueIDs.clear();

        resetState();
    }

    /**
     * Resets the decoder state after a call or other decode event
     */
    private void resetState()
    {
        if(mCurrentCallEvent != null && mCurrentCallEvent
            .getCallEventType() == CallEventType.CALL)
        {
            mCurrentCallEvent.end();
            broadcast(mCurrentCallEvent);
        }

        mCurrentCallEvent = null;

        mToAttribute.reset();
        mESNAttribute.reset();
        mFromUIDAttribute.reset();
        mMessage = null;
        mMessageType = null;
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
            broadcast(new AttributeChangeRequest<String>(Attribute.MESSAGE, mMessageType));
        }
    }

    public String getMessageType()
    {
        return mMessageType;
    }

    private void setMessageType(String messageType)
    {
        if(!StringUtils.isEqual(mMessageType, messageType))
        {
            mMessageType = messageType;
            broadcast(new AttributeChangeRequest<String>(Attribute.MESSAGE_TYPE, mMessageType));
        }
    }

    public boolean hasChannelNumber()
    {
        return mChannelNumber != 0;
    }

    public int getChannelNumber()
    {
        return mChannelNumber;
    }

    public void setChannelNumber(int channelNumber)
    {
        if(mChannelNumber != channelNumber)
        {
            mChannelNumber = channelNumber;
            broadcast(new AttributeChangeRequest<String>(Attribute.CHANNEL_FREQUENCY_LABEL,
                "LCN:" + getChannelNumber()));
        }
    }

    @Override
    public void init()
    {
		/* No initialization steps required */
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

    @Override
    public void start(ScheduledExecutorService executor)
    {
    }

    @Override
    public void stop()
    {
    }
}
