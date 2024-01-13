/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.module.decode.dmr;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.channel.state.ChangeChannelTimeoutEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.channel.state.TimeslotDecoderState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelConfigurationChangeNotification;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.dmr.event.DMRDecodeEvent;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraTrafficChannelTalkerStatus;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityMaxAloha;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityPlusNeighbors;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityPlusSiteStatus;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusDataChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Aloha;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Protect;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.acknowledge.Acknowledge;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy.Ahoy;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy.ServiceRadioCheck;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy.StunReviveKill;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.Announcement;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.VoteNowAdvice;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant.ChannelGrant;
import io.github.dsheirer.module.decode.dmr.message.data.header.HeaderMessage;
import io.github.dsheirer.module.decode.dmr.message.data.header.hytera.HyteraDataEncryptionHeader;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.GPSInformation;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.GroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.TalkerAliasComplete;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.UnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.hytera.HyteraGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.hytera.HyteraUnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola.CapacityMaxVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola.CapacityPlusEncryptedVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola.CapacityPlusWideAreaVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola.MotorolaGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.CapacityPlusRestChannel;
import io.github.dsheirer.module.decode.dmr.message.data.packet.DMRPacketMessage;
import io.github.dsheirer.module.decode.dmr.message.data.packet.UDTShortMessageService;
import io.github.dsheirer.module.decode.dmr.message.data.terminator.Terminator;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceOptions;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceEMBMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.EmbeddedParameters;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.EncryptionParameters;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.module.decode.ip.hytera.rrs.HyteraRrsPacket;
import io.github.dsheirer.module.decode.ip.hytera.sds.HyteraUnknownPacket;
import io.github.dsheirer.module.decode.ip.hytera.shortdata.HyteraShortDataPacket;
import io.github.dsheirer.module.decode.ip.hytera.sms.HyteraSmsPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.ars.ARSPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.LRRPPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.xcmp.XCMPPacket;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.source.tuner.channel.rotation.AddChannelRotationActiveStateRequest;
import io.github.dsheirer.util.PacketUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder state for an DMR channel.  Maintains the call/data/idle state of the channel and produces events by
 * monitoring the decoded message stream.
 */
public class DMRDecoderState extends TimeslotDecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRDecoderState.class);
    private static final long MAX_VALID_CALL_DURATION_MS = 30000;
    private Channel mChannel;
    private DMRNetworkConfigurationMonitor mNetworkConfigurationMonitor;
    private DMRTrafficChannelManager mTrafficChannelManager;
    private DecodeEvent mCurrentCallEvent;
    private long mCurrentFrequency;
    private boolean mIgnoreCRCChecksums;
    private Map<DMRChannel,DecodeEvent> mDetectedCallEventsMap = new HashMap<>();
    private static final AddChannelRotationActiveStateRequest CAPACITY_PLUS_ACTIVE_STATE_REQUEST =
                            new AddChannelRotationActiveStateRequest(State.ACTIVE);

    /**
     * Constructs an DMR decoder state with an optional traffic channel manager.
     * @param channel with configuration details
     * @param timeslot for this decoder state (1 or 2)
     * @param trafficChannelManager for handling traffic channel grants.
     */
    public DMRDecoderState(Channel channel, int timeslot, DMRTrafficChannelManager trafficChannelManager)
    {
        super(timeslot);
        mChannel = channel;
        mTrafficChannelManager = trafficChannelManager;

        //The decoder state passes all messages to the network configuration monitor, so we only construct
        //the monitor for timeslot 1.
        if(timeslot == 1)
        {
            mNetworkConfigurationMonitor = new DMRNetworkConfigurationMonitor(channel);
        }

        //For RAS protected systems, allows user to ignore CRC checksums and still decode the system
        if(channel.getDecodeConfiguration() instanceof DecodeConfigDMR)
        {
            mIgnoreCRCChecksums = ((DecodeConfigDMR)channel.getDecodeConfiguration()).getIgnoreCRCChecksums();
        }
    }

    /**
     * Indicates if the message is valid or if the Ignore CRC Checksums feature is enabled.
     * @param message to check
     * @return true if ignore CRC checksums or if the message is valid, meaning the message has passed CRC check.
     */
    private boolean isValid(IMessage message)
    {
        return mIgnoreCRCChecksums || message.isValid();
    }

    /**
     * Indicates if this decoder state has an (optional) traffic channel manager.
     */
    private boolean hasTrafficChannelManager()
    {
        return mTrafficChannelManager != null;
    }

    /**
     * Processes channel configuration change notifications received over the processing chain event bus.  This is
     * primarily used for Capacity+ systems when the standard channel is converted to a traffic channel.  In response,
     * we nullify the traffic channel manager to ensure the traffic channel no longer behaves as a standard channel
     * regarding channel conversions and allocations.
     *
     * @param notification of channel configuration change
     */
    @Subscribe
    public void channelChanged(ChannelConfigurationChangeNotification notification)
    {
        if(notification.getChannel().isTrafficChannel())
        {
            mTrafficChannelManager = null;
        }
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

        mCurrentFrequency = 0;
        mDetectedCallEventsMap.clear();
    }

    /**
     * Resets any temporal state details
     */
    protected void resetState()
    {
        super.resetState();
        closeCurrentCallEvent(System.currentTimeMillis());
    }

    /**
     * Primary message processing method.
     */
    @Override
    public void receive(IMessage message)
    {
        if(message.getTimeslot() == getTimeslot())
        {
            if(message instanceof VoiceMessage voice)
            {
                processVoice(voice);
            }
            else if(message instanceof DataMessage data)
            {
                processData(data);
            }
            else if(isValid(message) && message instanceof LCMessage lcMessage)
            {
                processLinkControl(lcMessage, false);
            }
            else if(isValid(message) && message instanceof DMRPacketMessage packet)
            {
                processPacket(packet);
            }
            else if(message instanceof UDTShortMessageService sms)
            {
                processSMS(sms);
            }
            else if(message instanceof DMRMessage)
            {
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));
            }
        }
        //SLCO messages on timeslot 0 to catch capacity plus rest channel events
        else if(isValid(message) && message.getTimeslot() == 0 && message instanceof LCMessage lcMessage)
        {
            processLinkControl(lcMessage, false);
        }

        //Pass the message to the network configuration monitor, if this decoder state has a non-null instance
        if(mNetworkConfigurationMonitor != null && isValid(message) && message instanceof DMRMessage dmrMessage)
        {
            mNetworkConfigurationMonitor.process(dmrMessage);
        }
    }

    /**
     * Processes Capacity Plus rest channel notifications to detect when the rest channel has changed.  When a new
     * rest channel is specified that is different from the current channel, notify the traffic channel manager so that
     * it can convert the current channel to a traffic channel and start a new channel for the rest channel, using the
     * current channel's configuration details and the specified rest channel frequency.  This approach ensures that
     * there is no disruption to the currently processing channel and that the original channel configuration can be
     * recreated to follow the rest channel.
     *
     * @param restChannel currently indicated
     */
    private void updateRestChannel(DMRChannel restChannel)
    {
        //Only respond if this is a standard/control channel (not a traffic channel).
        if(mChannel.isStandardChannel() && mCurrentFrequency > 0 &&
            restChannel.getDownlinkFrequency() > 0 &&
            restChannel.getDownlinkFrequency() != mCurrentFrequency && mTrafficChannelManager != null)
        {
            mTrafficChannelManager.convertToTrafficChannel(mChannel, mCurrentFrequency, restChannel,
                mNetworkConfigurationMonitor);
        }
    }

    /**
     * Preloads the DMR network configuration monitor that is optionally delivered as preload data.  This is primarily
     * used during a Capacity plus REST channel rotation to pass the monitor from the previous REST channel to the
     * current REST channel to ensure data continuity.
     *
     * Note: the monitor is only assigned to the timeslot 1 decoder state since the decoder state passes all received
     * messages (Timeslots 0, 1, and 2) to the monitor.
     *
     * Note: this method is invoked over the Guava event but by the ChannelProcessingManager.
     *
     * @param preloadData containing a DMR network configuration monitor.
     */
    @Subscribe
    public void preload(DMRNetworkConfigurationPreloadData preloadData)
    {
        if(getTimeslot() == 1 && preloadData.hasData())
        {
            mNetworkConfigurationMonitor = preloadData.getData();
        }
    }

    /**
     * Processes a short data message carrying SMS text
     * @param sms
     */
    private void processSMS(UDTShortMessageService sms)
    {
        broadcast(new DecoderStateEvent(this, Event.START, State.DATA, getTimeslot()));

        DecodeEvent smsEvent = DMRDecodeEvent.builder(DecodeEventType.SMS, sms.getTimestamp())
                .details("MESSAGE: " + sms.getSMS())
                .identifiers(new IdentifierCollection(sms.getIdentifiers()))
                .timeslot(getTimeslot())
                .build();
        broadcast(smsEvent);
    }

    /**
     * Processes a packet message
     */
    private void processPacket(DMRPacketMessage packet)
    {
        broadcast(new DecoderStateEvent(this, Event.START, State.DATA, getTimeslot()));

        //Hytera SDS Long SMS message
        if(packet.getPacket() instanceof HyteraSmsPacket hyteraSmsPacket)
        {
            MutableIdentifierCollection mic = new MutableIdentifierCollection(packet.getIdentifiers());
            mic.remove(Form.RADIO);
            mic.remove(Form.TALKGROUP);
            mic.update(hyteraSmsPacket.getSource());
            mic.update(hyteraSmsPacket.getDestination());

            DecodeEvent smsEvent = DMRDecodeEvent.builder(DecodeEventType.SMS, packet.getTimestamp())
                    .identifiers(mic)
                    .timeslot(getTimeslot())
                    .details("SMS:" + hyteraSmsPacket.getSMS())
                    .build();
            broadcast(smsEvent);
        }
        //Hytera Radio Registration Service (RRS)
        else if(packet.getPacket() instanceof HyteraRrsPacket rrs)
        {
            MutableIdentifierCollection mic = new MutableIdentifierCollection(packet.getIdentifiers());

            StringBuilder sb = new StringBuilder();
            sb.append("HYTERA RRS REGISTER RADIO:");
            sb.append(rrs.getDestination());
            DecodeEvent shortDataEvent = DMRDecodeEvent.builder(DecodeEventType.RADIO_REGISTRATION_SERVICE, packet.getTimestamp())
                    .identifiers(mic)
                    .timeslot(getTimeslot())
                    .details(sb.toString())
                    .build();
            broadcast(shortDataEvent);
        }
        //Hytera Short Data
        else if(packet.getPacket() instanceof HyteraShortDataPacket hsdp)
        {
            MutableIdentifierCollection mic = new MutableIdentifierCollection(packet.getIdentifiers());

            StringBuilder sb = new StringBuilder();
            sb.append("HYTERA");

            if(hsdp.getPacketSequence().isEncrypted())
            {
                HyteraDataEncryptionHeader hdeh = (HyteraDataEncryptionHeader)hsdp.getPacketSequence().getProprietaryDataHeader();
                sb.append(" ENCRYPTED ALGORITHM:").append(hdeh.getAlgorithm());
                sb.append(" KEY:").append(hdeh.getKeyId());
                sb.append(" IV:").append(hdeh.getIV());
            }

            sb.append(" SHORT DATA:").append(hsdp.getMessage().toHexString());

            DecodeEvent shortDataEvent = DMRDecodeEvent.builder(DecodeEventType.RADIO_REGISTRATION_SERVICE, packet.getTimestamp())
                    .identifiers(mic)
                    .timeslot(getTimeslot())
                    .details(sb.toString())
                    .build();
            broadcast(shortDataEvent);
        }
        //Unknown Hytera Long Data Service Token Message
        else if(packet.getPacket() instanceof HyteraUnknownPacket hyteraUnknownPacket)
        {
            MutableIdentifierCollection mic = new MutableIdentifierCollection(packet.getIdentifiers());

            DecodeEvent unknownTokenEvent = DMRDecodeEvent.builder(DecodeEventType.UNKNOWN_PACKET, packet.getTimestamp())
                    .identifiers(mic)
                    .timeslot(getTimeslot())
                    .details("HYTERA LONG DATA UNK TOKEN MSG:" + hyteraUnknownPacket.getHeader().toString())
                    .build();
            broadcast(unknownTokenEvent);
        }
        //Motorola ARS
        else if(packet.getPacket() instanceof ARSPacket ars)
        {
            MutableIdentifierCollection mic = new MutableIdentifierCollection(packet.getIdentifiers());

            DecodeEvent shortDataEvent = DMRDecodeEvent.builder(DecodeEventType.RADIO_REGISTRATION_SERVICE, packet.getTimestamp())
                    .identifiers(mic)
                    .timeslot(getTimeslot())
                    .details(ars.toString())
                    .build();
            broadcast(shortDataEvent);
        }
        //Motorola LRRP
        else if(packet.getPacket() instanceof LRRPPacket lrrp)
        {
            MutableIdentifierCollection mic = new MutableIdentifierCollection(packet.getIdentifiers());

            DecodeEvent shortDataEvent = DMRDecodeEvent.builder(DecodeEventType.LRRP, packet.getTimestamp())
                    .identifiers(mic)
                    .timeslot(getTimeslot())
                    .details(lrrp.toString())
                    .build();
            broadcast(shortDataEvent);
        }
        //Motorola XCMP
        else if(packet.getPacket() instanceof XCMPPacket xcmp)
        {
            MutableIdentifierCollection mic = new MutableIdentifierCollection(packet.getIdentifiers());

            DecodeEvent shortDataEvent = DMRDecodeEvent.builder(DecodeEventType.XCMP, packet.getTimestamp())
                    .identifiers(mic)
                    .timeslot(getTimeslot())
                    .details(xcmp.toString())
                    .build();
            broadcast(shortDataEvent);
        }
        else
        {
            DecodeEvent packetEvent = DMRDecodeEvent.builder(DecodeEventType.DATA_PACKET, packet.getTimestamp())
                    .identifiers(new IdentifierCollection(packet.getIdentifiers()))
                    .timeslot(getTimeslot())
                    .details(packet.toString())
                    .build();

            broadcast(packetEvent);

        }

        GeoPosition geoPosition = PacketUtil.extractGeoPosition(packet.getPacket());

        if (geoPosition != null) {
            PlottableDecodeEvent plottableDecodeEvent = PlottableDecodeEvent.plottableBuilder(DecodeEventType.GPS, packet.getTimestamp())
                    .channel(getCurrentChannel())
                    .identifiers(new IdentifierCollection(packet.getIdentifiers()))
                    .protocol(Protocol.LRRP)
                    .location(geoPosition)
                    .build();

            broadcast(plottableDecodeEvent);
        }
    }

    /**
     * Processes voice messages
     */
    private void processVoice(VoiceMessage message)
    {
        if(message.getSyncPattern().isMobileSyncPattern())
        {
            if(message.getSyncPattern().isDirectMode())
            {
                updateCurrentCall(DecodeEventType.CALL, "DIRECT MODE", message.getTimestamp());
            }
            else
            {
                updateCurrentCall(DecodeEventType.CALL, "REPEATER", message.getTimestamp());
            }

            //Use the timeslot as the talkgroup identifier since DCDM & simple repeater modes don't use talkgroups
            getIdentifierCollection().update(DMRTalkgroup.create(getTimeslot()));
        }
        else
        {
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL, getTimeslot()));
        }

        if(message.getSyncPattern() == DMRSyncPattern.BS_VOICE_FRAME_F && message instanceof VoiceEMBMessage voiceEmb)
        {
            if(voiceEmb.hasEmbeddedParameters())
            {
                EmbeddedParameters embedded = voiceEmb.getEmbeddedParameters();

                if(embedded.getShortBurst() instanceof EncryptionParameters arc4)
                {
                    updateEncryptedCall(arc4, true, voiceEmb.getTimestamp());
                }
            }
        }
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
            case USB_DATA:
            case MBC_ENC_HEADER:
            case DATA_ENC_HEADER:
            case CHANNEL_CONTROL_ENC_HEADER:
                broadcast(new DecoderStateEvent(this, Event.START, State.DATA, getTimeslot()));
                break;
        }

        //Process the link control message to get the identifiers
        LCMessage lc = header.getLCMessage();

        if(isValid(lc))
        {
            processLinkControl(lc, false);
        }
    }

    /**
     * Process Data Messages
     *
     * Note: invalid messages are allowed to pass to this method.  Messages are selectively checked for isValid()
     * to overcome RAS implementation in certain systems.
     */
    private void processData(DataMessage message)
    {
        switch(message.getSlotType().getDataType())
        {
            case CSBK:
                if(isValid(message) && message instanceof CSBKMessage csbk)
                {
                    processCSBK(csbk);
                }
                break;
            case VOICE_HEADER:
                if(message instanceof HeaderMessage header)
                {
                    processVoiceHeader(header);
                }
                break;
            case USB_DATA:
                break;
            case PI_HEADER:
            case MBC_HEADER:
            case DATA_HEADER:
            case MBC_ENC_HEADER:
            case DATA_ENC_HEADER:
            case CHANNEL_CONTROL_ENC_HEADER:
                if(message instanceof HeaderMessage header)
                {
                    processHeader(header);
                }
                break;
            case SLOT_IDLE:
                closeCurrentCallEvent(message.getTimestamp());
                getIdentifierCollection().remove(IdentifierClass.USER);
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));
                break;
            case TLC:
                if(message instanceof Terminator)
                {
                    processTerminator((Terminator)message);
                }
                break;
            case RATE_1_OF_2_DATA:
            case RATE_3_OF_4_DATA:
            case RATE_1_DATA:
                broadcast(new DecoderStateEvent(this, Event.START, State.DATA, getTimeslot()));
                break;
            case MBC_BLOCK:
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
    private void processTerminator(Terminator terminator)
    {
        closeCurrentCallEvent(terminator.getTimestamp());
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));

        LCMessage lcMessage = terminator.getLCMessage();

        if(isValid(lcMessage))
        {
            processLinkControl(lcMessage, true);
        }
    }

    /**
     * Process a voice header message
     */
    private void processVoiceHeader(HeaderMessage voiceHeader)
    {
        LCMessage lcMessage = voiceHeader.getLCMessage();

        if(isValid(lcMessage))
        {
            processLinkControl(lcMessage, false);
        }
    }

    private void processCSBK(CSBKMessage csbk)
    {
        switch(csbk.getOpcode())
        {
            case STANDARD_ACKNOWLEDGE_RESPONSE_INBOUND_PAYLOAD:
            case STANDARD_ACKNOWLEDGE_RESPONSE_OUTBOUND_PAYLOAD:
                if(csbk instanceof Acknowledge)
                {
                    broadcast(getDecodeEvent(csbk, DecodeEventType.RESPONSE,
                            ((Acknowledge) csbk).getReason().toString()));
                }
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));
                break;
            case STANDARD_ACKNOWLEDGE_RESPONSE_INBOUND_TSCC:
            case STANDARD_ACKNOWLEDGE_RESPONSE_OUTBOUND_TSCC:
                if(csbk instanceof Acknowledge)
                {
                    broadcast(getDecodeEvent(csbk, DecodeEventType.RESPONSE,
                            ((Acknowledge) csbk).getReason().toString()));
                }
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            case STANDARD_AHOY:
                if(csbk instanceof Ahoy)
                {
                    switch(((Ahoy)csbk).getServiceKind())
                    {
                        case AUTHENTICATE_REGISTER_RADIO_CHECK_SERVICE:
                            broadcast(getDecodeEvent(csbk, DecodeEventType.COMMAND, DecodeEventType.REGISTER.toString()));
                            break;
                        case CANCEL_CALL_SERVICE:
                            broadcast(getDecodeEvent(csbk, DecodeEventType.COMMAND, "CANCEL CALL"));
                            break;
                        case SUPPLEMENTARY_SERVICE:
                            if(csbk instanceof StunReviveKill)
                            {
                                broadcast(getDecodeEvent(csbk, DecodeEventType.COMMAND,
                                        ((StunReviveKill)csbk).getCommand() + " RADIO"));
                            }
                            break;
                        case FULL_DUPLEX_MS_TO_MS_PACKET_CALL_SERVICE:
                        case FULL_DUPLEX_MS_TO_MS_VOICE_CALL_SERVICE:
                        case INDIVIDUAL_VOICE_CALL_SERVICE:
                        case INDIVIDUAL_PACKET_CALL_SERVICE:
                        case INDIVIDUAL_UDT_SHORT_DATA_CALL_SERVICE:
                        case TALKGROUP_PACKET_CALL_SERVICE:
                        case TALKGROUP_UDT_SHORT_DATA_CALL_SERVICE:
                        case TALKGROUP_VOICE_CALL_SERVICE:
                            if(csbk instanceof ServiceRadioCheck)
                            {
                                ServiceRadioCheck src = (ServiceRadioCheck)csbk;
                                broadcast(getDecodeEvent(csbk, DecodeEventType.RADIO_CHECK,
                                        src.getServiceDescription() + " SERVICE FOR " +
                                        (src.isTalkgroupTarget() ? "TALKGROUP" : "RADIO")));
                            }
                            break;
                    }
                }
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            case STANDARD_ALOHA:
                if(csbk instanceof Aloha)
                {
                    Aloha aloha = (Aloha)csbk;

                    if(aloha.hasRadioIdentifier())
                    {
                        broadcast(getDecodeEvent(csbk, DecodeEventType.RESPONSE, "Aloha Acknowledge"));
                        resetState();
                    }
                }
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            case STANDARD_ANNOUNCEMENT:
                if(csbk instanceof Announcement)
                {
                    switch(((Announcement)csbk).getAnnouncementType())
                    {
                        case MASS_REGISTRATION:
                            broadcast(getDecodeEvent(csbk, DecodeEventType.REGISTER, "MASS REGISTRATION"));
                            break;
                        case VOTE_NOW_ADVICE:
                            if(csbk instanceof VoteNowAdvice)
                            {
                                broadcast(getDecodeEvent(csbk, DecodeEventType.COMMAND,
                                        "VOTE NOW FOR " + ((VoteNowAdvice)csbk).getVotedSystemIdentityCode()));
                            }
                            break;
                    }
                }
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            case STANDARD_CLEAR:
                broadcast(new DecoderStateEvent(this, Event.END, State.CALL, getTimeslot()));
                resetState();
                break;
            case STANDARD_PREAMBLE:
                getIdentifierCollection().update(csbk.getIdentifiers());
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA, getTimeslot()));
                break;
            case STANDARD_PROTECT:
                if(csbk instanceof Protect)
                {
                    broadcast(getDecodeEvent(csbk, DecodeEventType.COMMAND,
                            "PROTECT: " + ((Protect)csbk).getProtectKind()));
                }
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL, getTimeslot()));
                break;
            case HYTERA_08_ANNOUNCEMENT:
            case HYTERA_68_ANNOUNCEMENT:
            case HYTERA_68_XPT_SITE_STATE:
                break;
            case HYTERA_08_TRAFFIC_CHANNEL_TALKER_STATUS:
                if(csbk instanceof HyteraTrafficChannelTalkerStatus status)
                {
                    if(status.isChannelActive())
                    {
                        getIdentifierCollection().update(status.getIdentifiers());
                        updateCurrentCall(DecodeEventType.CALL_GROUP, "HYTERA TIER 3 CALL", status.getTimestamp());
                    }
                    else
                    {
                        getIdentifierCollection().remove(Role.FROM);
                        getIdentifierCollection().update(status.getDestinationRadio());
                    }
                }
                break;
            case MOTOROLA_CAPPLUS_NEIGHBOR_REPORT:
                if(csbk instanceof CapacityPlusNeighbors)
                {
                    //Update state and rest channel
                    updateRestChannel(((CapacityPlusNeighbors)csbk).getRestChannel());
                }
                break;
            case MOTOROLA_CAPPLUS_SITE_STATUS:
                if(csbk instanceof CapacityPlusSiteStatus)
                {
                    CapacityPlusSiteStatus cpss = (CapacityPlusSiteStatus)csbk;

                    //Channel rotation monitor normally uses only CONTROL state, so when we detect that we're a
                    //Capacity plus system, add ACTIVE as an active state to the monitor.  This can be requested repeatedly.
                    getInterModuleEventBus().post(CAPACITY_PLUS_ACTIVE_STATE_REQUEST);

                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));

                    //Update state and rest channel
                    updateRestChannel(cpss.getRestChannel());
                }
                break;
            case MOTOROLA_CONPLUS_NEIGHBOR_REPORT:
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            case STANDARD_DUPLEX_PRIVATE_DATA_CHANNEL_GRANT:
            case STANDARD_PRIVATE_DATA_CHANNEL_GRANT_SINGLE_ITEM:
            case STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_MULTI_ITEM:
            case STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_SINGLE_ITEM:
                if(csbk instanceof ChannelGrant)
                {
                    ChannelGrant dataGrant = (ChannelGrant)csbk;
                    DMRChannel channel = dataGrant.getChannel();


                    if(hasTrafficChannelManager())
                    {
                        IdentifierCollection mergedIdentifiers = getMergedIdentifierCollection(csbk.getIdentifiers());
                        mTrafficChannelManager.processChannelGrant(channel, mergedIdentifiers, csbk.getOpcode(),
                            csbk.getTimestamp(), csbk.isEncrypted());
                    }
                    else
                    {
                        DecodeEvent event = mDetectedCallEventsMap.get(channel);

                        if(isStale(event, csbk.getTimestamp(), csbk.getIdentifiers()))
                        {
                            event = getDecodeEvent(csbk, DecodeEventType.DATA_CALL, channel,
                                    new IdentifierCollection(csbk.getIdentifiers()));
                            mDetectedCallEventsMap.put(channel, event);
                        }
                        else
                        {
                            //Update the ending timestamp for the event and rebroadcast
                            event.end(csbk.getTimestamp());
                        }

                        broadcast(event);
                    }
                }
                break;
            case STANDARD_BROADCAST_TALKGROUP_VOICE_CHANNEL_GRANT:
            case STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT:
                if(csbk instanceof ChannelGrant)
                {
                    ChannelGrant tgGrant = (ChannelGrant)csbk;
                    DMRChannel channel = tgGrant.getChannel();

                    if(hasTrafficChannelManager())
                    {
                        IdentifierCollection mergedIdentifiers = getMergedIdentifierCollection(csbk.getIdentifiers());

                        mTrafficChannelManager.processChannelGrant(channel, mergedIdentifiers,
                            csbk.getOpcode(), csbk.getTimestamp(), csbk.isEncrypted());
                    }
                    else
                    {
                        DecodeEvent event = mDetectedCallEventsMap.get(channel);

                        if(isStale(event, csbk.getTimestamp(), csbk.getIdentifiers()))
                        {
                            event = getDecodeEvent(csbk, DecodeEventType.CALL_GROUP, channel,
                                    new IdentifierCollection(csbk.getIdentifiers()));
                            mDetectedCallEventsMap.put(channel, event);
                        }
                        else
                        {
                            //Update the ending timestamp for the event and rebroadcast
                            event.end(csbk.getTimestamp());
                        }

                        broadcast(event);
                    }
                }
                break;
            case STANDARD_DUPLEX_PRIVATE_VOICE_CHANNEL_GRANT:
            case STANDARD_PRIVATE_VOICE_CHANNEL_GRANT:
                if(csbk instanceof ChannelGrant)
                {
                    ChannelGrant channelGrant = (ChannelGrant)csbk;
                    DMRChannel channel = channelGrant.getChannel();

                    if(hasTrafficChannelManager())
                    {
                        IdentifierCollection mergedIdentifiers = getMergedIdentifierCollection(csbk.getIdentifiers());

                        mTrafficChannelManager.processChannelGrant(channel, mergedIdentifiers,
                            csbk.getOpcode(), csbk.getTimestamp(), csbk.isEncrypted());
                    }
                    else
                    {
                        DecodeEvent event = mDetectedCallEventsMap.get(channel);

                        if(isStale(event, csbk.getTimestamp(), csbk.getIdentifiers()))
                        {
                            event = getDecodeEvent(csbk, DecodeEventType.CALL_UNIT_TO_UNIT, channel,
                                    new IdentifierCollection(csbk.getIdentifiers()));
                            mDetectedCallEventsMap.put(channel, event);
                        }
                        else
                        {
                            //Update the ending timestamp for the event and rebroadcast
                            event.end(csbk.getTimestamp());
                        }

                        broadcast(event);
                    }
                }
                break;
            case MOTOROLA_CAPMAX_ALOHA:
                if(csbk instanceof CapacityMaxAloha)
                {
                    CapacityMaxAloha cmAloha = (CapacityMaxAloha)csbk;

                    if(cmAloha.hasRadioIdentifier())
                    {
                        broadcast(getDecodeEvent(csbk, DecodeEventType.RESPONSE, "Aloha Acknowledge"));
                        resetState();
                    }
                }
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            case MOTOROLA_CONPLUS_DATA_CHANNEL_GRANT:
                if(csbk instanceof ConnectPlusDataChannelGrant)
                {
                    ConnectPlusDataChannelGrant cpdcg = (ConnectPlusDataChannelGrant)csbk;
                    DMRChannel channel = cpdcg.getChannel();

                    if(hasTrafficChannelManager())
                    {
                        IdentifierCollection mergedIdentifiers = getMergedIdentifierCollection(csbk.getIdentifiers());

                        mTrafficChannelManager.processChannelGrant(channel, mergedIdentifiers, csbk.getOpcode(),
                            csbk.getTimestamp(), csbk.isEncrypted());
                    }
                    else
                    {
                        DecodeEvent event = mDetectedCallEventsMap.get(channel);

                        if(isStale(event, csbk.getTimestamp(), csbk.getIdentifiers()))
                        {
                            event = getDecodeEvent(csbk, DecodeEventType.DATA_CALL, channel,
                                    new IdentifierCollection(csbk.getIdentifiers()));
                            mDetectedCallEventsMap.put(channel, event);
                        }
                        else
                        {
                            //Update the ending timestamp for the event and rebroadcast
                            event.end(csbk.getTimestamp());
                        }

                        broadcast(event);
                    }
                }
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            case MOTOROLA_CONPLUS_REGISTRATION_REQUEST:
                DecodeEvent event = DMRDecodeEvent.builder(DecodeEventType.REQUEST, csbk.getTimestamp())
                    .details("Registration Request")
                    .identifiers(new IdentifierCollection(csbk.getIdentifiers()))
                    .timeslot(getTimeslot())
                    .build();
                broadcast(event);
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            case MOTOROLA_CONPLUS_REGISTRATION_RESPONSE:
                DecodeEvent regRespEvent = DMRDecodeEvent.builder(DecodeEventType.RESPONSE, csbk.getTimestamp())
                    .details("Registration Response")
                    .identifiers(new IdentifierCollection(csbk.getIdentifiers()))
                    .timeslot(getTimeslot())
                    .build();
                broadcast(regRespEvent);
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            case MOTOROLA_CONPLUS_VOICE_CHANNEL_USER:
                if(csbk instanceof ConnectPlusVoiceChannelUser)
                {
                    ConnectPlusVoiceChannelUser cpvcu = (ConnectPlusVoiceChannelUser)csbk;
                    DMRChannel channel = cpvcu.getChannel();

                    if(hasTrafficChannelManager())
                    {
                        IdentifierCollection mergedIdentifiers = getMergedIdentifierCollection(csbk.getIdentifiers());

                        mTrafficChannelManager.processChannelGrant(channel, mergedIdentifiers, csbk.getOpcode(),
                            csbk.getTimestamp(), csbk.isEncrypted());
                    }
                    else
                    {
                        DecodeEvent detectedEvent = mDetectedCallEventsMap.get(channel);

                        if(isStale(detectedEvent, csbk.getTimestamp(), csbk.getIdentifiers()))
                        {
                            detectedEvent = getDecodeEvent(csbk, DecodeEventType.CALL_GROUP, channel,
                                    new IdentifierCollection(csbk.getIdentifiers()));
                            mDetectedCallEventsMap.put(channel, detectedEvent);
                        }
                        else
                        {
                            //Update the ending timestamp for the event and rebroadcast
                            detectedEvent.end(csbk.getTimestamp());
                        }

                        broadcast(detectedEvent);
                    }
                }
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            case MOTOROLA_CONPLUS_TALKGROUP_AFFILIATION:
                DecodeEvent affiliateEvent = DMRDecodeEvent.builder(DecodeEventType.AFFILIATE, csbk.getTimestamp())
                    .details("TALKGROUP AFFILIATION")
                    .identifiers(new IdentifierCollection(csbk.getIdentifiers()))
                    .timeslot(getTimeslot())
                    .build();
                broadcast(affiliateEvent);
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL, getTimeslot()));
                break;
            default:
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE, getTimeslot()));
                break;
        }
    }

    private DecodeEvent getDecodeEvent(CSBKMessage csbk, DecodeEventType decodeEventType, DMRChannel channel,
                                       IdentifierCollection identifierCollection) {
        return DMRDecodeEvent.builder(decodeEventType, csbk.getTimestamp())
                .channel(channel)
                .details(csbk.getOpcode().getLabel())
                .identifiers(identifierCollection)
                .timeslot(getTimeslot())
                .build();
    }

    private DecodeEvent getDecodeEvent(CSBKMessage csbk, DecodeEventType decodeEventType, String details) {
        return DMRDecodeEvent.builder(decodeEventType, csbk.getTimestamp())
                .identifiers(new IdentifierCollection(csbk.getIdentifiers()))
                .timeslot(getTimeslot())
                .details(details)
                .build();
    }

    /**
     * Creates a copy of the current identifier collection, removes any USER class identifiers and loads the identifiers
     * argument values into the collection.
     * @param identifiers to load into the collection.
     * @return copy identifier collection.
     */
    private IdentifierCollection getMergedIdentifierCollection(List<Identifier> identifiers)
    {
        MutableIdentifierCollection mic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        mic.remove(IdentifierClass.USER);
        mic.update(identifiers);
        return mic;
    }

    /**
     * Indicates if the event is a stale event, meaning that the event is null, or the event start exceeds the max
     * valid call duration threshold, or if the event identifiers don't match the current identifiers.
     *
     * @param event to check for staleness
     * @param timestamp to check the event against
     * @param currentIdentifiers to compare against the event
     * @return true if the event is stale.
     */
    private boolean isStale(DecodeEvent event, long timestamp, List<Identifier> currentIdentifiers)
    {
        if(event == null || (timestamp - event.getTimeStart() > MAX_VALID_CALL_DURATION_MS))
        {
            return true;
        }

        return !isSameCall(event.getIdentifierCollection(), currentIdentifiers);
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
     * @param isTerminator set to true when the link control is carried by a terminator
     */
    private void processLinkControl(LCMessage message, boolean isTerminator)
    {
        switch(message.getOpcode())
        {
            case FULL_ENCRYPTION_PARAMETERS:
                if(message instanceof io.github.dsheirer.module.decode.dmr.message.data.lc.full.EncryptionParameters ep)
                {
                    if(mCurrentCallEvent != null)
                    {
                        mCurrentCallEvent.setDetails(ep.getDetails());
                    }
                }
                break;
            case SHORT_CAPACITY_PLUS_REST_CHANNEL_NOTIFICATION:
                if(message instanceof CapacityPlusRestChannel)
                {
                    updateRestChannel(((CapacityPlusRestChannel)message).getRestChannel());
                }
                break;
            case FULL_CAPACITY_PLUS_ENCRYPTED_VOICE_CHANNEL_USER:
                if(message instanceof CapacityPlusEncryptedVoiceChannelUser cpgvcu)
                {
                    if(isTerminator)
                    {
                        getIdentifierCollection().remove(Role.FROM);
                        getIdentifierCollection().update(cpgvcu.getTalkgroup());
                    }
                    else
                    {
                        getIdentifierCollection().update(message.getIdentifiers());
                        ServiceOptions serviceOptions = cpgvcu.getServiceOptions();
                        updateCurrentCall(serviceOptions.isEncrypted() ? DecodeEventType.CALL_GROUP_ENCRYPTED :
                                DecodeEventType.CALL_GROUP, serviceOptions.toString(), message.getTimestamp());
                    }
                }
                break;
            case FULL_MOTOROLA_GROUP_VOICE_CHANNEL_USER:
                if(message instanceof MotorolaGroupVoiceChannelUser cpgvcu)
                {
                    if(isTerminator)
                    {
                        getIdentifierCollection().remove(Role.FROM);
                        getIdentifierCollection().update(cpgvcu.getTalkgroup());
                    }
                    else
                    {
                        getIdentifierCollection().update(message.getIdentifiers());
                        ServiceOptions serviceOptions = cpgvcu.getServiceOptions();
                        updateCurrentCall(serviceOptions.isEncrypted() ? DecodeEventType.CALL_GROUP_ENCRYPTED :
                            DecodeEventType.CALL_GROUP, serviceOptions.toString(), message.getTimestamp());
                    }
                }
                break;
            case FULL_CAPACITY_MAX_GROUP_VOICE_CHANNEL_USER:
                if(message instanceof CapacityMaxVoiceChannelUser cmvcu)
                {
                    if(isTerminator)
                    {
                        getIdentifierCollection().remove(Role.FROM);
                        getIdentifierCollection().update(cmvcu.getTalkgroup());
                    }
                    else
                    {
                        getIdentifierCollection().update(message.getIdentifiers());
                        ServiceOptions serviceOptions = cmvcu.getServiceOptions();
                        updateCurrentCall(serviceOptions.isEncrypted() ? DecodeEventType.CALL_GROUP_ENCRYPTED :
                                DecodeEventType.CALL_GROUP, serviceOptions.toString(), message.getTimestamp());
                    }
                }
                break;
            case FULL_CAPACITY_PLUS_WIDE_AREA_VOICE_CHANNEL_USER:
                if(message instanceof CapacityPlusWideAreaVoiceChannelUser)
                {
                    CapacityPlusWideAreaVoiceChannelUser cpuo4 = (CapacityPlusWideAreaVoiceChannelUser)message;

                    updateRestChannel(cpuo4.getRestChannel());

                    if(isTerminator)
                    {
                        getIdentifierCollection().remove(Role.FROM);
                        getIdentifierCollection().update(cpuo4.getTalkgroup());
                    }
                    else
                    {
                        getIdentifierCollection().update(message.getIdentifiers());
                    }
                }
                break;
            case FULL_HYTERA_GROUP_VOICE_CHANNEL_USER:
                if(message instanceof HyteraGroupVoiceChannelUser)
                {
                    HyteraGroupVoiceChannelUser hgvcu = (HyteraGroupVoiceChannelUser)message;

                    if(isTerminator)
                    {
                        getIdentifierCollection().remove(Role.FROM);
                        getIdentifierCollection().update(hgvcu.getTalkgroup());
                    }
                    else
                    {
                        getIdentifierCollection().update(message.getIdentifiers());
                        ServiceOptions serviceOptions = hgvcu.getServiceOptions();
                        updateCurrentCall(serviceOptions.isEncrypted() ? DecodeEventType.CALL_GROUP_ENCRYPTED :
                            DecodeEventType.CALL_GROUP, serviceOptions.toString(), message.getTimestamp());

                    }
                }
                break;
            case FULL_HYTERA_TERMINATOR:
            case FULL_STANDARD_TERMINATOR_DATA:
                getIdentifierCollection().update(message.getIdentifiers());
                break;
            case FULL_HYTERA_UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                if(message instanceof HyteraUnitToUnitVoiceChannelUser)
                {
                    HyteraUnitToUnitVoiceChannelUser huuvcu = (HyteraUnitToUnitVoiceChannelUser)message;

                    if(isTerminator)
                    {
                        getIdentifierCollection().remove(Role.FROM);
                        getIdentifierCollection().update(huuvcu.getTargetRadio());
                    }
                    else
                    {
                        getIdentifierCollection().update(message.getIdentifiers());
                        ServiceOptions serviceOptions = huuvcu.getServiceOptions();
                        updateCurrentCall(serviceOptions.isEncrypted() ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED :
                            DecodeEventType.CALL_UNIT_TO_UNIT, serviceOptions.toString(), message.getTimestamp());
                    }
                }
                break;
            case FULL_STANDARD_GROUP_VOICE_CHANNEL_USER:
                if(message instanceof GroupVoiceChannelUser)
                {
                    GroupVoiceChannelUser gvcu = (GroupVoiceChannelUser)message;

                    if(isTerminator)
                    {
                        getIdentifierCollection().remove(Role.FROM);
                        getIdentifierCollection().update(gvcu.getTalkgroup());
                    }
                    else
                    {
                        getIdentifierCollection().update(message.getIdentifiers());
                        ServiceOptions serviceOptions = gvcu.getServiceOptions();
                        updateCurrentCall(serviceOptions.isEncrypted() ? DecodeEventType.CALL_GROUP_ENCRYPTED :
                            DecodeEventType.CALL_GROUP, serviceOptions.toString(), message.getTimestamp());
                    }
                }
                break;
            case FULL_STANDARD_UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                if(message instanceof UnitToUnitVoiceChannelUser)
                {
                    UnitToUnitVoiceChannelUser uuvcu = (UnitToUnitVoiceChannelUser)message;

                    if(isTerminator)
                    {
                        getIdentifierCollection().remove(Role.FROM);
                        getIdentifierCollection().update(uuvcu.getTargetRadio());
                    }
                    else
                    {
                        getIdentifierCollection().update(message.getIdentifiers());
                        ServiceOptions serviceOptions = uuvcu.getServiceOptions();
                        updateCurrentCall(serviceOptions.isEncrypted() ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED :
                            DecodeEventType.CALL_UNIT_TO_UNIT, serviceOptions.toString(), message.getTimestamp());
                    }
                }
                break;
            case FULL_STANDARD_GPS_INFO:
                if(message instanceof GPSInformation gpsInformation)
                {
                    PlottableDecodeEvent plottableGPS = PlottableDecodeEvent.plottableBuilder(DecodeEventType.GPS, message.getTimestamp())
                            .channel(getCurrentChannel())
                            .details("LOCATION:" + gpsInformation.getGPSLocation())
                            .identifiers(new IdentifierCollection(getIdentifierCollection().getIdentifiers()))
                            .protocol(Protocol.DMR)
                            .location(gpsInformation.getPosition())
                            .build();

                    broadcast(plottableGPS);
                }
                break;
            case FULL_STANDARD_TALKER_ALIAS_COMPLETE:
                if(message instanceof TalkerAliasComplete tac && tac.hasTalkerAliasIdentifier())
                {
                    getIdentifierCollection().update(tac.getTalkerAliasIdentifier());
                }
                break;
        }
    }

    /**
     * Updates the current call with encryption information.
     * @param encryptionParameters decoded from the Voice Frame F
     * @param isGroup true for group or false for individual call.
     */
    private void updateEncryptedCall(EncryptionParameters encryptionParameters, boolean isGroup, long timestamp)
    {
        if(mCurrentCallEvent != null)
        {
            String details = mCurrentCallEvent.getDetails();;

            if(details == null)
            {
                details = encryptionParameters.toString();
            }
            else if(!details.contains(encryptionParameters.toString()) && !details.contains("ENCRYPTION"))
            {
                details += " " + encryptionParameters;
            }

            mCurrentCallEvent.setDetails(details);
        }
        else
        {
            mCurrentCallEvent = DMRDecodeEvent.builder(isGroup ? DecodeEventType.CALL_GROUP_ENCRYPTED :
                            DecodeEventType.CALL_ENCRYPTED, timestamp)
                    .channel(getCurrentChannel())
                    .details(encryptionParameters.toString())
                    .identifiers(getIdentifierCollection().copyOf())
                    .timeslot(getTimeslot())
                    .build();
            broadcast(mCurrentCallEvent);
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
            mCurrentCallEvent = DMRDecodeEvent.builder(type, timestamp)
                .channel(getCurrentChannel())
                .details(details)
                .identifiers(getIdentifierCollection().copyOf())
                .timeslot(getTimeslot())
                .build();

            broadcast(mCurrentCallEvent);
        }
        else
        {
            if(mCurrentCallEvent.getDetails() == null)
            {
                mCurrentCallEvent.setDetails(details);
            }

            mCurrentCallEvent.setIdentifierCollection(getIdentifierCollection().copyOf());
            mCurrentCallEvent.end(timestamp);
            broadcast(mCurrentCallEvent);
        }

        if(type == DecodeEventType.CALL_GROUP_ENCRYPTED || type == DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED)
        {
            broadcast(new DecoderStateEvent(this, event, State.ENCRYPTED, getTimeslot()));
        }
        else
        {
            broadcast(new DecoderStateEvent(this, event, State.CALL, getTimeslot()));
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
        if(mNetworkConfigurationMonitor != null)
        {
            return mNetworkConfigurationMonitor.getActivitySummary();
        }

        return "";
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_RESET:
                resetState();
                if(mNetworkConfigurationMonitor != null)
                {
                    mNetworkConfigurationMonitor.reset();
                }
                break;
            case NOTIFICATION_SOURCE_FREQUENCY:
                mCurrentFrequency = event.getFrequency();
                if(hasTrafficChannelManager())
                {
                    mTrafficChannelManager.setCurrentControlFrequency(mCurrentFrequency);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void start()
    {
        //Change the default (45-second) traffic channel timeout to 1 second
        if(mChannel.isTrafficChannel())
        {
            broadcast(new ChangeChannelTimeoutEvent(this, ChannelType.TRAFFIC, 1000, getTimeslot()));
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
