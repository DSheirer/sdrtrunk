/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdateTDMA;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdateVUHF;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SecondaryControlChannelBroadcastAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SystemServiceBroadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Tracks the network configuration details of a P25 Phase 2 network from the broadcast messages
 */
public class P25P2NetworkConfigurationMonitor
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2NetworkConfigurationMonitor.class);

    private Map<Integer,IFrequencyBand> mFrequencyBandMap = new HashMap<>();

    //Network Status Messages
    private NetworkStatusBroadcastAbbreviated mNetworkStatusBroadcastAbbreviated;
    private NetworkStatusBroadcastExtended mNetworkStatusBroadcastExtended;

    //Current Site Status Messages
    private RfssStatusBroadcastAbbreviated mRFSSStatusBroadcastAbbreviated;
    private RfssStatusBroadcastExtended mRFSSStatusBroadcastExtended;

    //Current Site Secondary Control Channels
    private Map<String,IChannelDescriptor> mSecondaryControlChannels = new TreeMap<>();

    //Current Site Services
    private SystemServiceBroadcast mSystemServiceBroadcast;

    //Neighbor Sites
    private Map<Integer,AdjacentStatusBroadcastAbbreviated> mNeighborSitesAbbreviated = new HashMap<>();
    private Map<Integer,AdjacentStatusBroadcastExtended> mNeighborSitesExtended = new HashMap<>();

    /**
     * Constructs an instance.
     */
    public P25P2NetworkConfigurationMonitor()
    {
    }

    /**
     * Processes network configuration messages.
     *
     * Note: message is expected to be valid (ie message.isValid() = true)
     */
    public void processMacMessage(MacMessage message)
    {
        MacStructure mac = message.getMacStructure();

        switch((mac.getOpcode()))
        {
            case PHASE1_115_IDENTIFIER_UPDATE_TDMA:
                if(mac instanceof FrequencyBandUpdateTDMA)
                {
                    FrequencyBandUpdateTDMA tdma = (FrequencyBandUpdateTDMA)mac;
                    mFrequencyBandMap.put(tdma.getIdentifier(), tdma);
                }
                break;
            case PHASE1_116_IDENTIFIER_UPDATE_V_UHF:
                if(mac instanceof FrequencyBandUpdateVUHF)
                {
                    FrequencyBandUpdateVUHF vhf = (FrequencyBandUpdateVUHF)mac;
                    mFrequencyBandMap.put(vhf.getIdentifier(), vhf);
                }
                break;
            case PHASE1_120_SYSTEM_SERVICE_BROADCAST:
                if(mac instanceof SystemServiceBroadcast)
                {
                    mSystemServiceBroadcast = (SystemServiceBroadcast)mac;
                }
                break;
            case PHASE1_121_SECONDARY_CONTROL_CHANNEL_BROADCAST_ABBREVIATED:
                if(mac instanceof SecondaryControlChannelBroadcastAbbreviated)
                {
                    SecondaryControlChannelBroadcastAbbreviated sccba = (SecondaryControlChannelBroadcastAbbreviated)mac;

                    for(IChannelDescriptor channel: sccba.getChannels())
                    {
                        mSecondaryControlChannels.put(channel.toString(), channel);
                    }
                }
                break;
            case PHASE1_122_RFSS_STATUS_BROADCAST_ABBREVIATED:
                if(mac instanceof RfssStatusBroadcastAbbreviated)
                {
                    mRFSSStatusBroadcastAbbreviated = (RfssStatusBroadcastAbbreviated)mac;
                }
                break;
            case PHASE1_123_NETWORK_STATUS_BROADCAST_ABBREVIATED:
                if(mac instanceof NetworkStatusBroadcastAbbreviated)
                {
                    mNetworkStatusBroadcastAbbreviated = (NetworkStatusBroadcastAbbreviated)mac;
                }
                break;
            case PHASE1_124_ADJACENT_STATUS_BROADCAST_ABBREVIATED:
                if(mac instanceof AdjacentStatusBroadcastAbbreviated)
                {
                    AdjacentStatusBroadcastAbbreviated asba = (AdjacentStatusBroadcastAbbreviated)mac;
                    mNeighborSitesAbbreviated.put((int)asba.getSite().getValue(), asba);
                }
                break;
            case PHASE1_125_IDENTIFIER_UPDATE:
                if(mac instanceof FrequencyBandUpdate)
                {
                    FrequencyBandUpdate band = (FrequencyBandUpdate)mac;
                    mFrequencyBandMap.put(band.getIdentifier(), band);
                }
                break;
            case PHASE1_233_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                if(mac instanceof SecondaryControlChannelBroadcastExplicit)
                {
                    SecondaryControlChannelBroadcastExplicit sccbe = (SecondaryControlChannelBroadcastExplicit)mac;

                    for(IChannelDescriptor channel: sccbe.getChannels())
                    {
                        mSecondaryControlChannels.put(channel.toString(), channel);
                    }
                }
                break;
            case PHASE1_250_RFSS_STATUS_BROADCAST_EXTENDED:
                if(mac instanceof RfssStatusBroadcastExtended)
                {
                    mRFSSStatusBroadcastExtended = (RfssStatusBroadcastExtended)mac;
                }
                break;
            case PHASE1_251_NETWORK_STATUS_BROADCAST_EXTENDED:
                if(mac instanceof NetworkStatusBroadcastExtended)
                {
                    mNetworkStatusBroadcastExtended = (NetworkStatusBroadcastExtended)mac;
                }
                break;
            case PHASE1_252_ADJACENT_STATUS_BROADCAST_EXTENDED:
                if(mac instanceof AdjacentStatusBroadcastExtended)
                {
                    AdjacentStatusBroadcastExtended asbe = (AdjacentStatusBroadcastExtended)mac;
                    mNeighborSitesExtended.put((int)asbe.getSite().getValue(), asbe);
                }
                break;
        }
    }

    public void reset()
    {
        mFrequencyBandMap.clear();
        mNetworkStatusBroadcastAbbreviated = null;
        mNetworkStatusBroadcastExtended = null;
        mRFSSStatusBroadcastAbbreviated = null;
        mRFSSStatusBroadcastExtended = null;
        mSecondaryControlChannels.clear();
        mSystemServiceBroadcast = null;
        mNeighborSitesAbbreviated.clear();
        mNeighborSitesExtended.clear();
    }

    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary - Decoder:P25 Phase 2");

        sb.append("\n\nNetwork\n");
        if(mNetworkStatusBroadcastAbbreviated != null)
        {
            sb.append("  NAC:").append(mNetworkStatusBroadcastAbbreviated.getNAC());
            sb.append(" WACN:").append(mNetworkStatusBroadcastAbbreviated.getWACN());
            sb.append(" SYSTEM:").append(mNetworkStatusBroadcastAbbreviated.getSystem());
            sb.append(" LRA:").append(mNetworkStatusBroadcastAbbreviated.getLRA());
        }
        else if(mNetworkStatusBroadcastExtended != null)
        {
            sb.append("  NAC:").append(mNetworkStatusBroadcastExtended.getNAC());
            sb.append(" WACN:").append(mNetworkStatusBroadcastExtended.getWACN());
            sb.append(" SYSTEM:").append(mNetworkStatusBroadcastExtended.getSystem());
            sb.append(" LRA:").append(mNetworkStatusBroadcastExtended.getLRA());
        }
        else
        {
            sb.append("  UNKNOWN");
        }

        sb.append("\n\nCurrent Site\n");
        if(mRFSSStatusBroadcastAbbreviated != null)
        {
            sb.append("  SYSTEM:").append(mRFSSStatusBroadcastAbbreviated.getSystem());
            sb.append(" SITE:").append(mRFSSStatusBroadcastAbbreviated.getSite());
            sb.append(" RF SUBSYSTEM:").append(mRFSSStatusBroadcastAbbreviated.getRFSS());
            sb.append(" LOCATION REGISTRATION AREA:").append(mRFSSStatusBroadcastAbbreviated.getLRA());
            sb.append("  PRI CONTROL CHANNEL:").append(mRFSSStatusBroadcastAbbreviated.getChannel());
            sb.append(" DOWNLINK:").append(mRFSSStatusBroadcastAbbreviated.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mRFSSStatusBroadcastAbbreviated.getChannel().getUplinkFrequency()).append("\n");
        }
        else if(mRFSSStatusBroadcastExtended != null)
        {
            sb.append("  SYSTEM:").append(mRFSSStatusBroadcastExtended.getSystem());
            sb.append(" SITE:").append(mRFSSStatusBroadcastExtended.getSite());
            sb.append(" RF SUBSYSTEM:").append(mRFSSStatusBroadcastExtended.getRFSS());
            sb.append(" LOCATION REGISTRATION AREA:").append(mRFSSStatusBroadcastExtended.getLRA());
            sb.append("  PRI CONTROL CHANNEL:").append(mRFSSStatusBroadcastExtended.getChannel());
            sb.append(" DOWNLINK:").append(mRFSSStatusBroadcastExtended.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mRFSSStatusBroadcastExtended.getChannel().getUplinkFrequency()).append("\n");
        }
        else
        {
            sb.append("  UNKNOWN");
        }

        if(!mSecondaryControlChannels.isEmpty())
        {
            List<String> channels = new ArrayList<>(mSecondaryControlChannels.keySet());
            Collections.sort(channels);

            for(String channel : channels)
            {
                IChannelDescriptor secondaryControlChannel = mSecondaryControlChannels.get(channel);

                if(secondaryControlChannel != null)
                {
                    sb.append("  SEC CONTROL CHANNEL:").append(secondaryControlChannel);
                    sb.append(" DOWNLINK:").append(secondaryControlChannel.getDownlinkFrequency());
                    sb.append(" UPLINK:").append(secondaryControlChannel.getUplinkFrequency()).append("\n");
                }
            }
        }

        if(mSystemServiceBroadcast != null)
        {
            sb.append("  AVAILABLE SERVICES:").append(mSystemServiceBroadcast.getAvailableServices());
            sb.append("  SUPPORTED SERVICES:").append(mSystemServiceBroadcast.getSupportedServices());
        }


        sb.append("\nNeighbor Sites\n");
        Set<Integer> sites = new TreeSet<>();
        sites.addAll(mNeighborSitesAbbreviated.keySet());
        sites.addAll(mNeighborSitesExtended.keySet());

        if(sites.isEmpty())
        {
            sb.append("  UNKNOWN");
        }
        else
        {
            List<Integer> sitesSorted = new ArrayList<>(sites);
            Collections.sort(sitesSorted);

            for(Integer site : sitesSorted)
            {
                if(mNeighborSitesAbbreviated.containsKey(site))
                {
                    AdjacentStatusBroadcastAbbreviated asb = mNeighborSitesAbbreviated.get(site);
                    sb.append("  SYSTEM:").append(asb.getSystem());
                    sb.append(" SITE:").append(asb.getSite());
                    sb.append(" LRA:").append(asb.getLRA());
                    sb.append(" RFSS:").append(asb.getRFSS());
                    sb.append(" CHANNEL:").append(asb.getChannel());
                    sb.append(" DOWNLINK:").append(asb.getChannel().getDownlinkFrequency());
                    sb.append(" UPLINK:").append(asb.getChannel().getUplinkFrequency());
                    sb.append(" STATUS:").append(asb.getSiteFlags()).append("\n");
                }
                else if(mNeighborSitesAbbreviated.containsKey(site))
                {
                    AdjacentStatusBroadcastAbbreviated asb = mNeighborSitesAbbreviated.get(site);
                    sb.append("  SYSTEM:").append(asb.getSystem());
                    sb.append(" SITE:").append(asb.getSite());
                    sb.append(" LRA:").append(asb.getLRA());
                    sb.append(" RFSS:").append(asb.getRFSS());
                    sb.append(" CHANNEL:").append(asb.getChannel());
                    sb.append(" DOWNLINK:").append(asb.getChannel().getDownlinkFrequency());
                    sb.append(" UPLINK:").append(asb.getChannel().getUplinkFrequency());
                    sb.append(" STATUS:").append(asb.getSiteFlags()).append("\n");
                }
                else
                {
                    sb.append(" SITE:").append(site).append(" NOT FOUND IN NEIGHBOR SITE MAPS\n");
                }
            }
        }

        sb.append("\nFrequency Bands\n");
        if(mFrequencyBandMap.isEmpty())
        {
            sb.append("  UNKNOWN");
        }
        else
        {
            List<Integer> ids = new ArrayList<>(mFrequencyBandMap.keySet());
            Collections.sort(ids);
            {
                for(Integer id : ids)
                {
                    sb.append("  ").append(formatFrequencyBand(mFrequencyBandMap.get(id))).append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Formats a frequency band
     */
    private String formatFrequencyBand(IFrequencyBand band)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("BAND:").append(band.getIdentifier());
        sb.append(" ").append(band.isTDMA() ? "TDMA" : "FDMA");
        sb.append(" BASE:").append(band.getBaseFrequency());
        sb.append(" BANDWIDTH:").append(band.getBandwidth());
        sb.append(" SPACING:").append(band.getChannelSpacing());
        sb.append(" TRANSMIT OFFSET:").append(band.getTransmitOffset());

        if(band.isTDMA())
        {
            sb.append(" TIMESLOTS:").append(band.getTimeslotCount());
        }

        return sb.toString();
    }

}
