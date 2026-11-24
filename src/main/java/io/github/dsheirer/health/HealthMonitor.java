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
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.Direction;
import io.github.dsheirer.module.decode.nxdn.layer2.RFChannel;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.SiteInformation;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RFSSStatusBroadcast;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;

/**
 * Measures how well a standard (not traffic) channel is heard, for node health reports: control messages decoded and
 * lost (P25 TSBKs, DMR CSBKs, NXDN control channel frames), seconds locked and the site's identity on a control channel; openings and seconds open on a conventional
 * channel.  The channel's traffic channel manager counts the calls (CallTally).  The totals are kept in the
 * HealthRegistry.
 *
 * It counts what the decoders already produce, so it costs a few counter updates per message.
 */
public class HealthMonitor extends Module implements IMessageListener, ISquelchStateListener, ISourceEventListener
{
    private final ChannelHealth mHealth;
    private final Listener<IMessage> mMessageListener = this::receive;
    private final Listener<SquelchStateEvent> mSquelchListener = this::receive;
    private final Listener<SourceEvent> mSourceEventListener = this::receive;

    /**
     * @param channel the monitor measures (a standard channel)
     */
    public HealthMonitor(Channel channel)
    {
        mHealth = HealthRegistry.getInstance().channel(channel);
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

    private void receive(IMessage message)
    {
        if(message instanceof TSBKMessage tsbk)
        {
            mHealth.control("p25", tsbk.isValid(), tsbk.getTimestamp());

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
        else if(message instanceof CSBKMessage csbk)
        {
            mHealth.control("dmr", csbk.isValid(), csbk.getTimestamp());
        }
        else if(message instanceof NXDNMessage nxdn && nxdn.getLICH() != null &&
            nxdn.getLICH().getRFChannel() == RFChannel.RCCH && nxdn.getLICH().getDirection() == Direction.OUTBOUND)
        {
            //A control channel (CAC) frame carries one message, checked by its CRC.
            mHealth.control("nxdn", nxdn.isValid(), nxdn.getTimestamp());

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
        if(event.getEvent() == SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE && event.getValue() != null)
        {
            mHealth.setFrequency(event.getValue().longValue());
        }
    }

    @Override
    public void reset()
    {
    }

    @Override
    public void start()
    {
        mHealth.started(System.currentTimeMillis());
    }

    @Override
    public void stop()
    {
        mHealth.stopped(System.currentTimeMillis());
    }
}
