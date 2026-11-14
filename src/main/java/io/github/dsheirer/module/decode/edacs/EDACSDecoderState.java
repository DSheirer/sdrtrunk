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

// Reference: DSD-FME edacs-fme.c
package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.edacs.channel.EDACSChannel;
import io.github.dsheirer.module.decode.edacs.message.EDACSMessage;
import io.github.dsheirer.module.decode.edacs.message.EDACSProVoiceEndMessage;
import io.github.dsheirer.module.decode.edacs.message.EDACSProVoiceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EDACSDecoderState extends DecoderState implements IMessageListener
{
    private final static Logger mLog = LoggerFactory.getLogger(EDACSDecoderState.class);

    private final EDACSTrafficChannelManager mTrafficChannelManager;
    private final ChannelType mChannelType;
    private final java.util.function.IntFunction<Long> mFrequencyLookup;

    private int mSystemId;
    private int mSiteId;
    private int mAreaCode;
    private int mCcLcn;
    private boolean mSystemIdDecoded;
    private boolean mSiteIdDecoded;
    private boolean mCcLcnDecoded;

    private int mTotalMessagesDecoded;
    private int mBchErrors;

    public EDACSDecoderState(Channel channel, EDACSTrafficChannelManager trafficChannelManager)
    {
        mTrafficChannelManager = trafficChannelManager;
        mChannelType = channel.isTrafficChannel() ? ChannelType.TRAFFIC : ChannelType.STANDARD;

        java.util.function.IntFunction<Long> lookup = null;
        if(channel.getDecodeConfiguration() instanceof DecodeConfigEDACS edacsConfig)
        {
            lookup = edacsConfig::getFrequency;
        }
        mFrequencyLookup = lookup;
    }

    public int getSystemId()
    {
        return mSystemId;
    }

    public int getSiteId()
    {
        return mSiteId;
    }

    public int getAreaCode()
    {
        return mAreaCode;
    }

    public int getCcLcn()
    {
        return mCcLcn;
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.EDACS;
    }

    @Override
    public void receive(IMessage message)
    {
        if(message instanceof EDACSProVoiceMessage proVoice)
        {
            processProVoiceTraffic(proVoice);
            return;
        }

        if(message instanceof SyncLossMessage syncLoss)
        {
            processProVoiceEnd(syncLoss.getTimestamp());
            return;
        }

        if(message instanceof EDACSProVoiceEndMessage end)
        {
            processProVoiceChannelDrop(end.getTimestamp());
            return;
        }

        if(!(message instanceof EDACSMessage edacs))
        {
            return;
        }

        if(!edacs.isValid())
        {
            mBchErrors++;
            mTotalMessagesDecoded++;
            return;
        }

        mTotalMessagesDecoded++;
        EDACSMessageType type = edacs.getMessageType();

        switch(type)
        {
            case GROUP_CALL:
            case DIGITAL_GROUP_CALL:
            case ANALOG_GROUP_CALL:
            case TDMA_GROUP_CALL:
            case ALL_CALL:
            case INDIVIDUAL_CALL:
                processChannelGrant(edacs);
                break;
            case SYSTEM_INFO:
                processSystemInfo(edacs);
                break;
            case EXTENDED_ADDRESSING:
                processExtendedAddressing(edacs);
                break;
            default:
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
                break;
        }

    }

    private void processChannelGrant(EDACSMessage message)
    {
        int lcn = message.getLCN();
        if(lcn <= 0 || lcn > 25)
        {
            return;
        }

        EDACSChannel edacsChannel = new EDACSChannel(lcn);
        edacsChannel.setFrequencyLookup(mFrequencyLookup);

        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        ic.remove(IdentifierClass.USER);
        ic.update(message.getIdentifiers());

        if(mTrafficChannelManager != null)
        {
            mTrafficChannelManager.processChannelGrant(message, ic, edacsChannel);
        }

        // Control channel stays in CONTROL state - CALL is for traffic channels only
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
    }

    /**
     * End of a ProVoice over (frames stopped).  Handled like P25 handles a TDU terminator: tell the traffic channel
     * manager the call ended and leave the CALL state, which closes the audio segment.
     */
    private void processProVoiceEnd(long timestamp)
    {
        if(mChannelType != ChannelType.TRAFFIC)
        {
            return;
        }

        if(mTrafficChannelManager != null)
        {
            mTrafficChannelManager.processTrafficCallEnd(getCurrentFrequency(), timestamp);
        }

        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE));
    }

    /**
     * The site dropped the working channel (dotting after voice).  Handled like P25 handles a network commanded call
     * termination (P25P1DecoderState LCCallTermination): end the call and broadcast END, which tears down the traffic
     * channel straight away instead of leaving it in Now Playing until the control channel grants go stale.
     */
    private void processProVoiceChannelDrop(long timestamp)
    {
        if(mChannelType != ChannelType.TRAFFIC)
        {
            return;
        }

        if(mTrafficChannelManager != null)
        {
            mTrafficChannelManager.processTrafficChannelDrop(getCurrentFrequency(), timestamp);
        }

        broadcast(new DecoderStateEvent(this, Event.END, State.FADE));
    }

    private void processProVoiceTraffic(EDACSProVoiceMessage message)
    {
        mTotalMessagesDecoded++;

        if(mChannelType != ChannelType.TRAFFIC || !message.isValid())
        {
            return;
        }

        //The control channel's traffic channel manager owns the call event (as P25 does) - report activity to it
        if(mTrafficChannelManager != null)
        {
            mTrafficChannelManager.processTrafficVoice(getCurrentFrequency(), message.getTimestamp());
        }

        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));
    }

    private void processSystemInfo(EDACSMessage message)
    {
        if(message.getSystemId() != 0)
        {
            mSystemId = message.getSystemId();
        }
        if(message.getCcLcn() != 0)
        {
            mCcLcn = message.getCcLcn();
        }

        if(!mSystemIdDecoded && mSystemId != 0)
        {
            mSystemIdDecoded = true;
            mLog.debug("EDACS System ID decoded: 0x{}", Integer.toHexString(mSystemId));
        }
        if(!mCcLcnDecoded && mCcLcn != 0)
        {
            mCcLcnDecoded = true;
            mLog.debug("EDACS CC LCN decoded: {}", mCcLcn);
        }

        broadcast(new DecoderStateEvent(this, Event.START, State.CONTROL));
    }

    private void processExtendedAddressing(EDACSMessage message)
    {
        mSiteId = message.getSiteId();
        mAreaCode = message.getArea();

        if(!mSiteIdDecoded && mSiteId != 0)
        {
            mSiteIdDecoded = true;
            mLog.debug("EDACS Site ID decoded: 0x{} ({}), Area 0x{} ({})",
                    Integer.toHexString(mSiteId), mSiteId,
                    Integer.toHexString(mAreaCode), mAreaCode);
        }

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
        sb.append("Decoder:\tEDACS\n");
        sb.append("System ID:\t").append(String.format("0x%04X", mSystemId)).append("\n");
        sb.append("Site ID:\t").append(mSiteId).append("\n");
        sb.append("Area:\t").append(mAreaCode).append("\n");
        sb.append("CC LCN:\t").append(mCcLcn).append("\n");
        sb.append("Messages:\t").append(mTotalMessagesDecoded).append("\n");
        sb.append("BCH Errors:\t").append(mBchErrors).append("\n");
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
