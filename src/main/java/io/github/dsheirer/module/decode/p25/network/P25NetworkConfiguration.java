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

package io.github.dsheirer.module.decode.p25.network;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCAdjacentSiteStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCAdjacentSiteStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCEncryptionParameterBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCFrequencyBandUpdate;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCFrequencyBandUpdateExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCNetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCRFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCRFSSStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCSecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCSecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCSystemServiceBroadcast;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the network configuration details of a P25 network from the broadcast messages
 */
public class P25NetworkConfiguration
{
    private final static Logger mLog = LoggerFactory.getLogger(P25NetworkConfiguration.class);

    private Map<Identifier,LinkControlWord> mSites = new HashMap<>();
    private Map<Identifier,List<IChannelDescriptor>> mSiteSecondaryControlChannelsMap = new HashMap<>();
    private LinkControlWord mEncryptionParameterBroadcast;
    private LinkControlWord mNetworkStatusBroadcast;
    private LinkControlWord mSystemServiceBroadcast;
    private Map<Integer,IFrequencyBand> mFrequencyBandMap = new HashMap<>();

    public P25NetworkConfiguration()
    {
    }

    public void process(TSBKMessage tsbk)
    {
        switch(tsbk.getOpcode())
        {
            //TODO:
        }
    }

    public void process(AMBTCMessage ambtc)
    {
        switch(ambtc.getHeader().getOpcode())
        {
            //TODO:
        }
    }

    /**
     * Processes
     *
     * @param lcw
     */
    public void process(LinkControlWord lcw)
    {
        if(lcw.isValid())
        {
            switch(lcw.getOpcode())
            {
                case ADJACENT_SITE_STATUS_BROADCAST:
                    if(lcw instanceof LCAdjacentSiteStatusBroadcast)
                    {
                        Identifier site = ((LCAdjacentSiteStatusBroadcast)lcw).getSite();
                        mSites.put(site, lcw);
                    }
                    break;
                case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
                    if(lcw instanceof LCAdjacentSiteStatusBroadcastExplicit)
                    {
                        Identifier site = ((LCAdjacentSiteStatusBroadcastExplicit)lcw).getSite();
                        mSites.put(site, lcw);
                    }
                    break;
                case CHANNEL_IDENTIFIER_UPDATE:
                    if(lcw instanceof LCFrequencyBandUpdate)
                    {
                        LCFrequencyBandUpdate fbu = (LCFrequencyBandUpdate)lcw;
                        int band = fbu.getIdentifier();
                        mFrequencyBandMap.put(band, fbu);
                    }
                    break;
                case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
                    if(lcw instanceof LCFrequencyBandUpdateExplicit)
                    {
                        LCFrequencyBandUpdateExplicit fbue = (LCFrequencyBandUpdateExplicit)lcw;
                        int band = fbue.getIdentifier();
                        mFrequencyBandMap.put(band, fbue);
                    }
                    break;
                case NETWORK_STATUS_BROADCAST:
                    if(lcw instanceof LCNetworkStatusBroadcast)
                    {
                        mNetworkStatusBroadcast = lcw;
                    }
                    break;
                case NETWORK_STATUS_BROADCAST_EXPLICIT:
                    if(lcw instanceof LCNetworkStatusBroadcastExplicit)
                    {
                        mNetworkStatusBroadcast = lcw;
                    }
                    break;
                case PROTECTION_PARAMETER_BROADCAST:
                    if(lcw instanceof LCEncryptionParameterBroadcast)
                    {
                        mEncryptionParameterBroadcast = lcw;
                    }
                    break;
                case RFSS_STATUS_BROADCAST:
                    if(lcw instanceof LCRFSSStatusBroadcast)
                    {
                        Identifier site = ((LCRFSSStatusBroadcast)lcw).getSite();
                        mSites.put(site, lcw);
                    }
                    break;
                case RFSS_STATUS_BROADCAST_EXPLICIT:
                    if(lcw instanceof LCRFSSStatusBroadcastExplicit)
                    {
                        Identifier site = ((LCRFSSStatusBroadcastExplicit)lcw).getSite();
                        mSites.put(site, lcw);
                    }
                    break;
                case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                    if(lcw instanceof LCSecondaryControlChannelBroadcast)
                    {
                        LCSecondaryControlChannelBroadcast sccb = (LCSecondaryControlChannelBroadcast)lcw;
                        Identifier site = sccb.getSite();
                        List<IChannelDescriptor> channels = mSiteSecondaryControlChannelsMap.get(site);
                        if(channels == null)
                        {
                            channels = new ArrayList<>();
                            channels.addAll(sccb.getChannels());
                            mSiteSecondaryControlChannelsMap.put(site, channels);
                        }
                        else
                        {
                            for(IChannelDescriptor channel : sccb.getChannels())
                            {
                                if(!channels.contains(channel))
                                {
                                    channels.add(channel);
                                }
                            }
                        }
                    }
                    break;
                case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                    if(lcw instanceof LCSecondaryControlChannelBroadcastExplicit)
                    {
                        LCSecondaryControlChannelBroadcastExplicit sccb = (LCSecondaryControlChannelBroadcastExplicit)lcw;
                        Identifier site = sccb.getSite();
                        List<IChannelDescriptor> channels = mSiteSecondaryControlChannelsMap.get(site);
                        if(channels == null)
                        {
                            channels = new ArrayList<>();
                            channels.addAll(sccb.getChannels());
                            mSiteSecondaryControlChannelsMap.put(site, channels);
                        }
                        else
                        {
                            for(IChannelDescriptor channel : sccb.getChannels())
                            {
                                if(!channels.contains(channel))
                                {
                                    channels.add(channel);
                                }
                            }
                        }
                    }
                    break;
                case SYSTEM_SERVICE_BROADCAST:
                    if(lcw instanceof LCSystemServiceBroadcast)
                    {
                        mSystemServiceBroadcast = lcw;
                    }
                    break;
            }

        }

    }
}
