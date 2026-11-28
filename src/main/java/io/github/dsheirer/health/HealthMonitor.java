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

package io.github.dsheirer.health;

import io.github.dsheirer.audio.squelch.ISquelchStateListener;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelConfigurationChangeNotification;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventListener;
import io.github.dsheirer.module.decode.ltrnet.message.LtrNetMessage;
import io.github.dsheirer.module.decode.ltrstandard.message.LTRMessage;
import io.github.dsheirer.module.decode.mpt1327.MPT1327Message;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.Direction;
import io.github.dsheirer.module.decode.nxdn.layer2.RFChannel;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.SiteInformation;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcast;
import io.github.dsheirer.module.decode.passport.PassportMessage;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;

/**
 * Measures how well a standard (not traffic) channel is heard, for node health reports: control messages decoded and
 * lost, seconds locked and the site's identity on a control channel; openings and seconds open on a conventional
 * channel.  Control messages are P25 Phase 1 TSBKs, P25 Phase 2 MAC messages, DMR CSBKs, NXDN control channel frames,
 * MPT1327 address codewords, and LTR, LTR-Net and Passport data words.  The channel's traffic channel manager counts
 * the calls (CallTally), except on LTR, LTR-Net and Passport, which have none: a channel of theirs follows the calls
 * on its own repeater, and the calls are counted here, from its decode events.  The totals are kept in the
 * HealthRegistry.
 *
 * It counts what the decoders already produce, so it costs a few counter updates per message.
 */
public class HealthMonitor extends Module implements IMessageListener, ISquelchStateListener, ISourceEventListener,
    IDecodeEventListener
{
    private final ChannelHealth mHealth;
    private final CallTally mCalls;
    private final Listener<IMessage> mMessageListener = this::receive;
    private final Listener<SquelchStateEvent> mSquelchListener = this::receive;
    private final Listener<SourceEvent> mSourceEventListener = this::receive;
    private final Listener<IDecodeEvent> mDecodeEventListener = this::receive;
    private IDecodeEvent mLastCall;
    private boolean mStarted;
    /** Its processing chain became a traffic channel: what it decodes now isn't the channel's. */
    private volatile boolean mDetached;

    /**
     * @param channel the monitor measures (a standard channel)
     */
    public HealthMonitor(Channel channel)
    {
        mHealth = HealthRegistry.getInstance().channel(channel);
        DecoderType decoderType = channel.getDecodeConfiguration() != null ?
            channel.getDecodeConfiguration().getDecoderType() : null;
        String protocol = trunkedProtocol(decoderType);

        if(protocol != null)
        {
            mHealth.setProtocol(protocol);
        }

        mCalls = decoderType == DecoderType.LTR || decoderType == DecoderType.LTR_NET ||
            decoderType == DecoderType.PASSPORT ? HealthRegistry.getInstance().calls(channel) : null;
    }

    /**
     * Protocol of a site whose channel has this decoder, as a node health report names it, when the decoder can be
     * nothing but a trunked site's (a standard P25 Phase 2 channel is a TDMA control channel), or null.
     */
    public static String trunkedProtocol(DecoderType decoderType)
    {
        if(decoderType == null)
        {
            return null;
        }

        return switch(decoderType)
        {
            case MPT1327 -> "mpt1327";
            case LTR -> "ltr";
            case LTR_NET -> "ltrnet";
            case PASSPORT -> "passport";
            case P25_PHASE2 -> "p25";
            default -> null;
        };
    }

    /**
     * Mode of a conventional channel with this decoder, as a node health report names it, or null when the decoder
     * can't be a conventional channel.
     */
    public static String conventionalMode(DecoderType decoderType)
    {
        if(decoderType == null)
        {
            return null;
        }

        return switch(decoderType)
        {
            case NBFM -> "fm";
            case AM -> "am";
            case P25_PHASE1 -> "p25";
            case DMR -> "dmr";
            case NXDN -> "nxdn";
            default -> null;
        };
    }

    @Override
    public Listener<IMessage> getMessageListener()
    {
        return mMessageListener;
    }

    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchListener;
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventListener;
    }

    @Override
    public Listener<IDecodeEvent> getDecodeEventListener()
    {
        return mDecodeEventListener;
    }

    /**
     * A call on an LTR, LTR-Net or Passport channel's repeater, which the channel follows itself: counted once, as
     * recorded, when its decode event first comes (the decoder sends it again as the call goes on).
     */
    private void receive(IDecodeEvent event)
    {
        if(!mDetached && mCalls != null && event != mLastCall && event.getEventType() == DecodeEventType.CALL)
        {
            mLastCall = event;
            mCalls.count(null, CallOutcome.RECORDED);
        }
    }

    private void receive(IMessage message)
    {
        if(mDetached)
        {
            return;
        }

        if(message instanceof TSBKMessage tsbk)
        {
            mHealth.control("p25", tsbk.isValid(), tsbk.getTimestamp(), System.currentTimeMillis());

            if(tsbk.isValid())
            {
                identity("nac", tsbk.getNAC());

                //This site's own broadcasts only: an adjacent status broadcast describes a neighbour.
                if(tsbk instanceof NetworkStatusBroadcast nsb)
                {
                    identity("wacn", nsb.getWacn());
                    identity("sysId", nsb.getSystem());
                }
                else if(tsbk instanceof RFSSStatusBroadcast rsb)
                {
                    identity("sysId", rsb.getSystem());
                    identity("rfss", rsb.getRfss());
                    identity("site", rsb.getSite());
                }
            }
        }
        else if(message instanceof MacMessage mac)
        {
            //A standard Phase 2 channel is a TDMA control channel.  A failed PDU is one bad message.
            mHealth.control("p25", mac.isValid(), mac.getTimestamp(), System.currentTimeMillis());

            if(mac.isValid())
            {
                identity(mac.getMacStructure());
            }
        }
        else if(message instanceof CSBKMessage csbk)
        {
            mHealth.control("dmr", csbk.isValid(), csbk.getTimestamp(), System.currentTimeMillis());
        }
        else if(message instanceof NXDNMessage nxdn && nxdn.getLICH() != null &&
            nxdn.getLICH().getRFChannel() == RFChannel.RCCH && nxdn.getLICH().getDirection() == Direction.OUTBOUND)
        {
            //A control channel (CAC) frame carries one message, checked by its CRC.
            mHealth.control("nxdn", nxdn.isValid(), nxdn.getTimestamp(), System.currentTimeMillis());

            //This site's own information (not an adjacent site's): its location and radio access number.
            if(nxdn.isValid() && nxdn instanceof SiteInformation site)
            {
                if(site.getLocationID() != null)
                {
                    identity("system", site.getLocationID().getSystem());
                    identity("site", site.getLocationID().getSiteOrIntegrator());
                }

                if(site.hasRAN())
                {
                    mHealth.identity("ran", site.getRAN());
                }
            }
        }
        else if(message instanceof MPT1327Message mpt)
        {
            //One message per address codeword framed, checked by its CRC.
            mHealth.control("mpt1327", mpt.isValid(), mpt.getTimestamp(), System.currentTimeMillis());

            //Its system identity code, from the system parameters broadcast (in the codeword the CRC checked).
            if(mpt.isValid() && mpt.getMessageType() == MPT1327Message.MPTMessageType.BCAST && mpt.getSiteID() != null)
            {
                try
                {
                    mHealth.identity("sysId", Integer.parseInt(mpt.getSiteID()));
                }
                catch(NumberFormatException nfe)
                {
                    //Not a number: left out
                }
            }
        }
        else if(message instanceof LTRMessage ltr)
        {
            mHealth.decoded("ltr", ltr.getTimestamp(), System.currentTimeMillis());
        }
        else if(message instanceof LtrNetMessage ltrNet)
        {
            mHealth.decoded("ltrnet", ltrNet.getTimestamp(), System.currentTimeMillis());
        }
        else if(message instanceof PassportMessage passport)
        {
            mHealth.decoded("passport", passport.getTimestamp(), System.currentTimeMillis());
        }
    }

    /**
     * The site's identity from its own Phase 2 broadcasts (not an adjacent site's).
     */
    private void identity(MacStructure structure)
    {
        if(structure instanceof NetworkStatusBroadcastImplicit nsb)
        {
            identity("wacn", nsb.getWACN());
            identity("sysId", nsb.getSystem());
            identity("nac", nsb.getNAC());
        }
        else if(structure instanceof NetworkStatusBroadcastExplicit nsb)
        {
            identity("wacn", nsb.getWACN());
            identity("sysId", nsb.getSystem());
            identity("nac", nsb.getNAC());
        }
        else if(structure instanceof RfssStatusBroadcast rsb)
        {
            identity("sysId", rsb.getSystem());
            identity("rfss", rsb.getRFSS());
            identity("site", rsb.getSite());
        }
    }

    private void identity(String name, Identifier<?> identifier)
    {
        if(identifier != null && identifier.getValue() instanceof Integer value)
        {
            mHealth.identity(name, value);
        }
    }

    private void receive(SquelchStateEvent event)
    {
        if(mDetached)
        {
            return;
        }

        if(event.getSquelchState() == SquelchState.UNSQUELCH)
        {
            mHealth.opened(System.currentTimeMillis());
        }
        else if(event.getSquelchState() == SquelchState.SQUELCH)
        {
            mHealth.closed(System.currentTimeMillis());
        }
    }

    private void receive(SourceEvent event)
    {
        if(!mDetached && event.getEvent() == SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE && event.getValue() != null)
        {
            mHealth.setFrequency(event.getValue().longValue());
        }
    }

    @Override
    public void reset()
    {
    }

    /**
     * Its processing chain became a traffic channel (a DMR Capacity Plus rest channel that moved: a new chain is
     * started for the channel, with a monitor of its own).  This one stops counting toward the channel, now.
     */
    @Subscribe
    public void channelChanged(ChannelConfigurationChangeNotification notification)
    {
        if(notification.getChannel() != null && notification.getChannel().isTrafficChannel())
        {
            detach(System.currentTimeMillis());
        }
    }

    synchronized void detach(long now)
    {
        mDetached = true;
        stop(now);
    }

    @Override
    public void start()
    {
        start(System.currentTimeMillis());
    }

    @Override
    public void stop()
    {
        stop(System.currentTimeMillis());
    }

    synchronized void start(long now)
    {
        if(!mStarted && !mDetached)
        {
            mStarted = true;
            mHealth.started(now);
        }
    }

    synchronized void stop(long now)
    {
        if(mStarted)
        {
            mStarted = false;
            mHealth.stopped(now);
        }
    }
}
