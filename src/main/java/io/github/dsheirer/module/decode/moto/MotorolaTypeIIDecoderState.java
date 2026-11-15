/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

// Reference: OP25 tk_smartnet.py (boatbod/op25), Trunk Recorder smartnet_parser.cc
package io.github.dsheirer.module.decode.moto;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.moto.channel.MotorolaTypeIIChannel;
import io.github.dsheirer.module.decode.moto.message.MotorolaTypeIIMessage;
import io.github.dsheirer.module.decode.moto.message.MotorolaTypeIIMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MotorolaTypeIIDecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(MotorolaTypeIIDecoderState.class);
    private static final double[] CONNECT_TONES = {105.88, 76.60, 83.72, 90.00, 97.30, 116.13, 128.57, 138.46};

    private final MotorolaTypeIITrafficChannelManager mTrafficChannelManager;
    private final ChannelType mChannelType;
    private final Bandplan mBandplan;

    private int mSystemId;
    private int mSiteId;
    private double mConnectTone;
    private boolean mSystemIdDecoded;
    private boolean mSiteIdDecoded;
    private boolean mConnectToneDecoded;

    private int mTotalMessagesDecoded;
    private int mCrcErrors;

    public MotorolaTypeIIDecoderState(Channel channel, MotorolaTypeIITrafficChannelManager trafficChannelManager)
    {
        mTrafficChannelManager = trafficChannelManager;
        mChannelType = channel.isTrafficChannel() ? ChannelType.TRAFFIC : ChannelType.STANDARD;

        DecodeConfigMotorolaTypeII config = null;

        if(channel.getDecodeConfiguration() instanceof DecodeConfigMotorolaTypeII dcm)
        {
            config = dcm;
        }

        mBandplan = config != null ? new Bandplan(config.getBandplanType(),
                config.getObtBaseFrequency(), config.getObtSpacing(), config.getObtOffset()) :
                new Bandplan(BandplanType.EIGHT_HUNDRED_REBANDED);
    }

    public int getSystemId()
    {
        return mSystemId;
    }

    public int getSiteId()
    {
        return mSiteId;
    }

    public double getConnectTone()
    {
        return mConnectTone;
    }

    public void setConnectTone(double connectTone)
    {
        if(!mConnectToneDecoded)
        {
            mConnectTone = connectTone;
            mConnectToneDecoded = true;
            mLog.info("Moto T2 Connect Tone: {} Hz", connectTone);
        }
    }

    public Bandplan getBandplan()
    {
        return mBandplan;
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.MOTOROLA_TYPE_II;
    }

    @Override
    public void receive(IMessage message)
    {
        if(!(message instanceof MotorolaTypeIIMessage moto))
        {
            return;
        }

        if(!moto.isValid())
        {
            mCrcErrors++;
            mTotalMessagesDecoded++;
            return;
        }

        mTotalMessagesDecoded++;
        MotorolaTypeIIMessageType type = moto.getMessageType();

        switch(type)
        {
            case ANALOG_GROUP_GRANT:
            case DIGITAL_GROUP_GRANT:
            case ANALOG_PRIVATE_CALL:
            case DIGITAL_PRIVATE_CALL:
                processChannelGrant(moto);
                break;
            case GROUP_UPDATE:
                processGroupUpdate(moto);
                break;
            case SYSTEM_ID:
                processSystemId(moto);
                break;
            case AMSS:
                processAmss(moto);
                break;
            case NETWORK_STATUS:
            case SYSTEM_STATUS:
                processStatus(moto);
                break;
            case CONTROL_CHANNEL:
                processControlChannel(moto);
                break;
            case IDLE:
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
                break;
            default:
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
                break;
        }
    }

    private void processChannelGrant(MotorolaTypeIIMessage message)
    {
        int channelNumber = message.getChannelNumber();
        int talkgroup = message.getAddress();
        double frequencyMHz = mBandplan.getDownlinkFrequency(channelNumber);

        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        ic.remove(IdentifierClass.USER);
        ic.update(message.getIdentifiers());

        MotorolaTypeIIChannel channel = new MotorolaTypeIIChannel(channelNumber, mBandplan);

        String mode = message.getMessageType() == MotorolaTypeIIMessageType.DIGITAL_GROUP_GRANT ||
                message.getMessageType() == MotorolaTypeIIMessageType.DIGITAL_PRIVATE_CALL ? "DIGITAL" : "ANALOG";
        boolean privateCall = message.getMessageType() == MotorolaTypeIIMessageType.ANALOG_PRIVATE_CALL ||
                message.getMessageType() == MotorolaTypeIIMessageType.DIGITAL_PRIVATE_CALL;
        String callType = privateCall ? "Private Call" : "Group Grant";
        String addressLabel = privateCall ? " TO:" : " TG:";
        String freqStr = frequencyMHz > 0 ? String.format("%.4f MHz", frequencyMHz) : "unknown freq";
        DecodeEvent event = DecodeEvent.builder(DecodeEventType.CALL, message.getTimestamp())
                .channel(channel)
                .identifiers(ic)
                .details(mode + " " + callType + " CH:" + String.format("0x%03X", channelNumber) +
                        addressLabel + String.format("0x%04X", talkgroup) + " " + freqStr)
                .protocol(io.github.dsheirer.protocol.Protocol.MOTOROLA_TYPE_II)
                .build();
        broadcast(event);

        if(mTrafficChannelManager != null)
        {
            mTrafficChannelManager.processChannelGrant(message, ic, channel);
        }

        // Control channel stays in CONTROL state - CALL state is for traffic channels only
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
    }

    private void processGroupUpdate(MotorolaTypeIIMessage message)
    {
        int channelNumber = message.getChannelNumber();
        int talkgroup = message.getAddress();
        double frequencyMHz = mBandplan.getDownlinkFrequency(channelNumber);

        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        ic.remove(IdentifierClass.USER);
        ic.update(message.getIdentifiers());

        MotorolaTypeIIChannel channel = new MotorolaTypeIIChannel(channelNumber, mBandplan);

        String freqStr = frequencyMHz > 0 ? String.format("%.4f MHz", frequencyMHz) : "unknown freq";
        DecodeEvent event = DecodeEvent.builder(DecodeEventType.CALL_IN_PROGRESS, message.getTimestamp())
                .channel(channel)
                .identifiers(ic)
                .details("Group Update TG:" + String.format("0x%04X", talkgroup) + 
                         " CH:" + String.format("0x%03X", channelNumber) + " " + freqStr)
                .protocol(io.github.dsheirer.protocol.Protocol.MOTOROLA_TYPE_II)
                .build();
        broadcast(event);

        if(mTrafficChannelManager != null)
        {
            mTrafficChannelManager.processGroupUpdate(message, channel);
        }

        // Control channel stays in CONTROL state
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
    }

    private void processSystemId(MotorolaTypeIIMessage message)
    {
        mSystemId = message.getAddress();

        if(!mSystemIdDecoded)
        {
            mSystemIdDecoded = true;
            mLog.info("Moto T2 System ID decoded: 0x{}", Integer.toHexString(mSystemId));
        }

        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());

        DecodeEvent event = DecodeEvent.builder(DecodeEventType.STATUS, message.getTimestamp())
                .identifiers(ic)
                .details("System ID: " + String.format("0x%04X", mSystemId))
                .protocol(io.github.dsheirer.protocol.Protocol.MOTOROLA_TYPE_II)
                .build();
        broadcast(event);

        broadcast(new DecoderStateEvent(this, Event.START, State.CONTROL));
    }

    private void processAmss(MotorolaTypeIIMessage message)
    {
        mSiteId = message.getCommand() - 0x360 + 1;

        if(!mSiteIdDecoded)
        {
            mSiteIdDecoded = true;
            mLog.info("Moto T2 Site ID decoded: {} (from AMSS cmd 0x{})", mSiteId,
                    Integer.toHexString(message.getCommand()));
        }

        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());

        DecodeEvent event = DecodeEvent.builder(DecodeEventType.STATUS, message.getTimestamp())
                .identifiers(ic)
                .details("AMSS Site ID: " + mSiteId)
                .protocol(io.github.dsheirer.protocol.Protocol.MOTOROLA_TYPE_II)
                .build();
        broadcast(event);

        broadcast(new DecoderStateEvent(this, Event.START, State.CONTROL));
    }

    private void processStatus(MotorolaTypeIIMessage message)
    {
        int opcode = (message.getAddress() & 0xE000) >> 13;
        int data = message.getAddress() & 0x1FFF;

        if(opcode == 1)
        {
            int connectToneIndex = (data & 0x00E0) >> 5;
            double connectTone = CONNECT_TONES[connectToneIndex];

            setConnectTone(connectTone);
        }

        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
    }

    private void processControlChannel(MotorolaTypeIIMessage message)
    {
        int systemId = message.getAddress();
        mSystemId = systemId;
        mSiteId = 0;
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
    }

    @Override
    public void init()
    {
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Decoder:\tMotorola Type II\n");
        sb.append("System ID:\t").append(String.format("0x%04X", mSystemId)).append("\n");
        sb.append("Site ID:\t").append(mSiteId).append("\n");
        if(mConnectToneDecoded)
        {
            sb.append("Connect Tone:\t").append(mConnectTone).append(" Hz\n");
        }
        sb.append("Messages:\t").append(mTotalMessagesDecoded).append("\n");
        sb.append("CRC Errors:\t").append(mCrcErrors).append("\n");
        return sb.toString();
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        if(event.getEvent() == Event.REQUEST_RESET)
        {
            resetState();
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        resetState();
    }

    @Override
    public void start()
    {
        super.start();

        if(mChannelType == ChannelType.TRAFFIC)
        {
            broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
        }
    }

    protected void resetState()
    {
        super.resetState();
    }
}
