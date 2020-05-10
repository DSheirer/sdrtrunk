/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer, Zhenyu Mao
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.channel.state.ChangeChannelTimeoutEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.channel.state.TimeslotDecoderState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.dmr.event.DMRDecodeEvent;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.header.HeaderMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.GroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.UnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.terminator.TerminatorMessage;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceOptions;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceMessage;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Decoder state for an DMR channel.  Maintains the call/data/idle state of the channel and produces events by
 * monitoring the decoded message stream.
 *
 */
public class DMRDecoderState extends TimeslotDecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRDecoderState.class);
    private static final long MAX_VALID_CALL_DURATION_MS = 30000;
    private ChannelType mChannelType;
    private DMRNetworkConfigurationMonitor mNetworkConfigurationMonitor;
    private DMRTrafficChannelManager mTrafficChannelManager;
    private DecodeEvent mCurrentCallEvent;
    private Integer mCurrentLSN;
    private Map<Integer,DecodeEvent> mDetectedCallEventsMap = new TreeMap<>();

    /**
     * Constructs an DMR decoder state with an optional traffic channel manager.
     * @param channel with configuration details
     * @param trafficChannelManager for handling traffic channel grants.
     */
    public DMRDecoderState(Channel channel, int timeslot, DMRTrafficChannelManager trafficChannelManager)
    {
        super(timeslot);
        mChannelType = channel.getChannelType();
        mTrafficChannelManager = trafficChannelManager;
        mNetworkConfigurationMonitor = new DMRNetworkConfigurationMonitor(timeslot);
    }

    /**
     * Identifies the decoder type
     */
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.DMR;
    }

    /**
     * Performs a full reset to prepare this object for reuse on a new channel
     */
    @Override
    public void reset()
    {
        super.reset();
        resetState();

        mCurrentLSN = null;
        mDetectedCallEventsMap.clear();
    }

/**
     * Resets any temporal state details
     */
    protected void resetState()
    {
        super.resetState();

        if(mCurrentCallEvent != null)
        {
            mCurrentCallEvent.end(System.currentTimeMillis());
            broadcast(mCurrentCallEvent);
            mCurrentCallEvent = null;
        }
    }

    /**
     * Primary message processing method.
     */
    @Override
    public void receive(IMessage message)
    {
        if(message.isValid() && message.getTimeslot() == getTimeslot())
        {
            if(message instanceof VoiceMessage)
            {
                processVoice((VoiceMessage)message);
            }
            else if(message instanceof DataMessage)
            {
                processData((DataMessage)message);
            }
            else if(message instanceof LCMessage)
            {
                processLinkControl((LCMessage)message);
            }
            else if(message instanceof DMRMessage)
            {
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));
            }
        }
    }

    /**
     * Processes voice messages
     */
    private void processVoice(VoiceMessage message)
    {
        updateCurrentCall(DecodeEventType.CALL, null, message.getTimestamp());
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL, getTimeslot()));
    }

    /**
     * Processes a voice header message
     */
    private void processHeader(HeaderMessage header)
    {
        switch(header.getSlotType().getDataType())
        {
            case VOICE_HEADER:
                broadcast(new DecoderStateEvent(this, Event.START, State.CALL, getTimeslot()));
                break;
            case PI_HEADER:
            case MBC_HEADER:
            case DATA_HEADER:
            case CSBK_ENC_HEADER:
            case MBC_ENC_HEADER:
            case DATA_ENC_HEADER:
            case CHANNEL_CONTROL_ENC_HEADER:
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA, getTimeslot()));
                break;
        }

        //Process the link control message to get the identifiers
        processLinkControl(header.getLCMessage());
    }

    /**
     * Process Data Messages
     */
    private void processData(DataMessage message)
    {
        switch(message.getSlotType().getDataType())
        {
            case CSBK:
                if(message instanceof CSBKMessage)
                {
                    processCSBK((CSBKMessage)message);
                }
                break;
            case VOICE_HEADER:
                if(message instanceof HeaderMessage)
                {
                    processVoiceHeader((HeaderMessage)message);
                }
                break;
            case PI_HEADER:
            case MBC_HEADER:
            case DATA_HEADER:
            case CSBK_ENC_HEADER:
            case MBC_ENC_HEADER:
            case DATA_ENC_HEADER:
            case CHANNEL_CONTROL_ENC_HEADER:
                if(message instanceof HeaderMessage)
                {
                    processHeader((HeaderMessage)message);
                }
                break;
            case SLOT_IDLE:
                closeCurrentCallEvent(message.getTimestamp());
                getIdentifierCollection().remove(IdentifierClass.USER);
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));
                break;
            case TLC:
                if(message instanceof TerminatorMessage)
                {
                    processTerminator((TerminatorMessage)message);
                }
                break;
            case MBC:
            case RATE_1_OF_2_DATA:
            case RATE_3_OF_4_DATA:
            case RATE_1_DATA:
            case RESERVED_15:
            case UNKNOWN:
            default:
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));
                break;
        }
    }

    /**
     * Process terminator with link control messages
     */
    private void processTerminator(TerminatorMessage terminator)
    {
        processLinkControl(terminator.getLCMessage());
        closeCurrentCallEvent(terminator.getTimestamp());
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));
    }

    /**
     * Process a voice header message
     */
    private void processVoiceHeader(HeaderMessage voiceHeader)
    {
        //Since the header by itself doesn't have much detail, we'll start the call event and rely on the link control
        //message to fill in the missing details
        updateCurrentCall(DecodeEventType.CALL, null, voiceHeader.getTimestamp());

        processLinkControl(voiceHeader.getLCMessage());
    }

    private void processCSBK(CSBKMessage csbk)
    {
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));

        switch(csbk.getOpcode())
        {
            case MOTOROLA_CONPLUS_VOICE_CHANNEL_USER:
                if(csbk instanceof ConnectPlusVoiceChannelUser)
                {
                    ConnectPlusVoiceChannelUser cpvcu = (ConnectPlusVoiceChannelUser)csbk;
                    processCallDetection(cpvcu.getLogicalSlotNumber(), cpvcu.getIdentifiers(), cpvcu.getTimestamp());
                }
                break;
        }

        mNetworkConfigurationMonitor.process(csbk);
    }

    /**
     * Processes Connect+ call detections.
     *
     * Note: once full support for Connect+ trunk tracking is implemented with the DMRTrafficChannelManager, this will
     * have to be modified to detect if the trunked channel was allocated or if the call was simply detected.  This
     * would be contingent on having a map of Logical Slot Numbers to frequency for the traffic channel manager to make
     * an allocation.
     *
     * @param lsn for the call event
     * @param identifiers for the call event
     * @param timestamp of the event or update
     */
    private void processCallDetection(int lsn, List<Identifier> identifiers, long timestamp)
    {
        //Check to see if there is a current call event to see if the detected call event is actually for this timeslot
        //and we can then identify the LSN for this timeslot
        if(mCurrentLSN == null && mCurrentCallEvent != null &&
           isSameCall(mCurrentCallEvent.getIdentifierCollection(), identifiers))
        {
            mCurrentLSN = lsn;
            mNetworkConfigurationMonitor.setCurrentLogicalSlotNumber(mCurrentLSN);
        }

        DecodeEvent event = mDetectedCallEventsMap.get(lsn);

        if(event == null)
        {
            event = DMRDecodeEvent.builder(timestamp)
                .timeslot(getTimeslot())
                .identifiers(new IdentifierCollection(identifiers))
                .eventDescription(DecodeEventType.CALL_DETECT.toString())
                .build();
            mDetectedCallEventsMap.put(lsn, event);
        }
        else
        {
            if(event.getIdentifierCollection() != null &&
               isSameCall(event.getIdentifierCollection(), identifiers) &&
                FastMath.abs(timestamp - event.getTimeStart()) < MAX_VALID_CALL_DURATION_MS)
            {
                event.update(timestamp);
            }
            else
            {
                event = DMRDecodeEvent.builder(timestamp)
                    .timeslot(getTimeslot())
                    .identifiers(new IdentifierCollection(identifiers))
                    .eventDescription(DecodeEventType.CALL_DETECT.toString())
                    .build();
                mDetectedCallEventsMap.put(lsn, event);
            }
        }

        //Only broadcast the call detect event if it doesn't match the current logical slot number
        if(mCurrentLSN == null || mCurrentLSN != lsn)
        {
            broadcast(event);
        }
    }

    /**
     * Indicates if the TO/FROM identifiers in the identifier collection match the TO/FROM identifiers in the list
     * of identifiers.
     * @param identifierCollection containing TO/FROM identifiers
     * @param identifiers containing TO/FROM identifiers
     * @return true if the TO/FROM identifiers in each collection match
     */
    private boolean isSameCall(IdentifierCollection identifierCollection, List<Identifier> identifiers)
    {
        IntegerIdentifier to = null;
        IntegerIdentifier from = null;

        for(Identifier identifier: identifierCollection.getIdentifiers(IdentifierClass.USER, Role.TO))
        {
            if(identifier instanceof IntegerIdentifier)
            {
                to = (IntegerIdentifier)identifier;
                break;
            }
        }

        for(Identifier identifier: identifierCollection.getIdentifiers(IdentifierClass.USER, Role.FROM))
        {
            if(identifier instanceof IntegerIdentifier)
            {
                from = (IntegerIdentifier)identifier;
                break;
            }
        }

        if(to == null || from == null)
        {
            return false;
        }

        boolean toMatch = false;
        boolean fromMatch = false;

        for(Identifier identifier: identifiers)
        {
            if(identifier.getRole() == Role.TO &&
               identifier instanceof IntegerIdentifier &&
               ((IntegerIdentifier)identifier).getValue() == to.getValue())
            {
                toMatch = true;
            }
            else if(identifier.getRole() == Role.FROM &&
                identifier instanceof IntegerIdentifier &&
                ((IntegerIdentifier)identifier).getValue() == from.getValue())
            {
                fromMatch = true;
            }
        }

        return toMatch & fromMatch;
    }

    /**
     * Processes Link Control Messages
     */
    private void processLinkControl(LCMessage message)
    {
        mNetworkConfigurationMonitor.process(message);

        switch(message.getOpcode())
        {
            case FULL_STANDARD_GROUP_VOICE_CHANNEL_USER:
                if(message instanceof GroupVoiceChannelUser)
                {
                    ServiceOptions serviceOptions = ((GroupVoiceChannelUser)message).getServiceOptions();
                    updateCurrentCall(serviceOptions.isEncrypted() ? DecodeEventType.CALL_GROUP_ENCRYPTED :
                        DecodeEventType.CALL_GROUP, serviceOptions.toString(), message.getTimestamp());
                }
                getIdentifierCollection().update(message.getIdentifiers());
                break;
            case FULL_STANDARD_UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                if(message instanceof UnitToUnitVoiceChannelUser)
                {
                    ServiceOptions serviceOptions = ((UnitToUnitVoiceChannelUser)message).getServiceOptions();
                    updateCurrentCall(serviceOptions.isEncrypted() ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED :
                        DecodeEventType.CALL_UNIT_TO_UNIT, serviceOptions.toString(), message.getTimestamp());
                }
                getIdentifierCollection().update(message.getIdentifiers());
                break;
            //SLC messages are all sent as timeslot 0 even though they apply to both timeslot 0 and 1, so we normally
            //don't process them.  However, Connect+ always uses timeslot zero for the control channel
            case SHORT_CONNECT_PLUS_CONTROL_CHANNEL:
                if(getTimeslot() == 0)
                {
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                }
                break;
        }
    }

    /**
     * Updates or creates a current call event.
     *
     * @param type of call that will be used as an event description
     * @param details of the call (optional)
     * @param timestamp of the message indicating a call or continuation
     */
    private void updateCurrentCall(DecodeEventType type, String details, long timestamp)
    {
        Event event = (mCurrentCallEvent == null ? Event.START : Event.CONTINUATION);

        if(mCurrentCallEvent == null)
        {
            mCurrentCallEvent = DMRDecodeEvent.builder(timestamp)
                .channel(getCurrentChannel())
                .eventDescription(type.toString())
                .details(details)
                .identifiers(getIdentifierCollection().copyOf())
                .build();

            broadcast(mCurrentCallEvent);

        }
        else
        {
            if(type != DecodeEventType.CALL)
            {
                mCurrentCallEvent.setEventDescription(type.toString());
                mCurrentCallEvent.setDetails(details);
            }

            mCurrentCallEvent.setIdentifierCollection(getIdentifierCollection().copyOf());
            mCurrentCallEvent.end(timestamp);
            broadcast(mCurrentCallEvent);
        }

        if(type == DecodeEventType.CALL_GROUP_ENCRYPTED || type == DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED)
        {
            broadcast(new DecoderStateEvent(this, event, State.ENCRYPTED));
        }
        else
        {
            broadcast(new DecoderStateEvent(this, event, State.CALL));
        }
    }

    /**
     * Ends/closes the current call event.
     *
     * @param timestamp of the message that indicates the event has ended.
     */
    private void closeCurrentCallEvent(long timestamp)
    {
        if(mCurrentCallEvent != null)
        {
            mCurrentCallEvent.end(timestamp);
            broadcast(mCurrentCallEvent);
            mCurrentCallEvent = null;
        }
    }

    @Override
    public String getActivitySummary()
    {
        return mNetworkConfigurationMonitor.getActivitySummary();
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case RESET:
                resetState();
                mNetworkConfigurationMonitor.reset();
                break;
            default:
                break;
        }
    }

    @Override
    public void start()
    {
        //Change the default (45-second) traffic channel timeout to 1 second
        if(mChannelType == ChannelType.TRAFFIC)
        {
            broadcast(new ChangeChannelTimeoutEvent(this, ChannelType.TRAFFIC, 1000));
        }
    }

    @Override
    public void init()
    {
    }

    @Override
    public void stop()
    {
    }
}
