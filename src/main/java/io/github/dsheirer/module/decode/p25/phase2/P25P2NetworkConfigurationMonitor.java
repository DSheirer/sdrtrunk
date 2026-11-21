/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.channel.IBandChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.event.ActivitySummary;
import io.github.dsheirer.module.decode.p25.phase1.P25P1NetworkConfigurationMonitor;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastExtendedExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdateTDMAAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdateTDMAExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdateVUHF;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataChannelAnnouncement;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SecondaryControlChannelBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SystemServiceBroadcast;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the network configuration details of a P25 Phase 2 network from the broadcast messages
 */
public class P25P2NetworkConfigurationMonitor
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2NetworkConfigurationMonitor.class);

    private Map<Integer,IFrequencyBand> mFrequencyBandMap = new HashMap<>();

    //Network Status Messages
    private NetworkStatusBroadcastImplicit mNetworkStatusBroadcastImplicit;
    private NetworkStatusBroadcastExplicit mNetworkStatusBroadcastExplicit;

    //Current Site Status Messages
    private RfssStatusBroadcastImplicit mRFSSStatusBroadcastImplicit;
    private RfssStatusBroadcastExplicit mRFSSStatusBroadcastExplicit;

    //Current Site Secondary Control Channels
    private Map<String, IBandChannelDescriptor> mSecondaryControlChannels = new TreeMap<>();

    //SNDCP Data Channel
    private SNDCPDataChannelAnnouncement mSNDCPDataChannelAnnouncement;

    //Current Site Services
    private SystemServiceBroadcast mSystemServiceBroadcast;

    //Neighbor Sites
    private Map<Integer, AdjacentStatusBroadcastImplicit> mNeighborSitesAbbreviated = new HashMap<>();
    private Map<Integer, AdjacentStatusBroadcastExplicit> mNeighborSitesExtended = new HashMap<>();
    private Map<Integer, AdjacentStatusBroadcastExtendedExplicit> mNeighborSitesExtendedExplicit = new HashMap<>();

    /**
     * Constructs an instance.
     */
    public P25P2NetworkConfigurationMonitor()
    {
    }

    /**
     * Formats the identifier with an appended hexadecimal value when the identifier is an integer
     * @param identifier to format
     * @param width of the hex value with zero pre-padding
     * @return formatted identifier
     */
    private String format(Identifier identifier, int width)
    {
        if(identifier.getValue() instanceof Integer)
        {
            String hex = StringUtils.leftPad(Integer.toHexString((Integer)identifier.getValue()), width, '0');

            return hex.toUpperCase() + "[" + identifier.getValue() + "]";
        }
        else
        {
            return identifier.toString();
        }
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
            case PHASE1_73_IDENTIFIER_UPDATE_TDMA_ABBREVIATED:
                if(mac instanceof FrequencyBandUpdateTDMAAbbreviated tdma)
                {
                    mFrequencyBandMap.put(tdma.getIdentifier(), tdma);
                }
                break;
            case PHASE1_74_IDENTIFIER_UPDATE_V_UHF:
                if(mac instanceof FrequencyBandUpdateVUHF vhf)
                {
                    mFrequencyBandMap.put(vhf.getIdentifier(), vhf);
                }
                break;
            case PHASE1_78_SYSTEM_SERVICE_BROADCAST:
                if(mac instanceof SystemServiceBroadcast ssb)
                {
                    mSystemServiceBroadcast = ssb;
                }
                break;
            case PHASE1_79_SECONDARY_CONTROL_CHANNEL_BROADCAST_IMPLICIT:
                if(mac instanceof SecondaryControlChannelBroadcastImplicit sccba)
                {
                    for(IBandChannelDescriptor channel: sccba.getChannels())
                    {
                        mSecondaryControlChannels.put(channel.toString(), channel);
                    }
                }
                break;
            case PHASE1_7A_RFSS_STATUS_BROADCAST_IMPLICIT:
                if(mac instanceof RfssStatusBroadcastImplicit rsbe)
                {
                    mRFSSStatusBroadcastImplicit = rsbe;
                }
                break;
            case PHASE1_7B_NETWORK_STATUS_BROADCAST_IMPLICIT:
                if(mac instanceof NetworkStatusBroadcastImplicit nsbe)
                {
                    mNetworkStatusBroadcastImplicit = nsbe;
                }
                break;
            case PHASE1_7C_ADJACENT_STATUS_BROADCAST_IMPLICIT:
                if(mac instanceof AdjacentStatusBroadcastImplicit asba)
                {
                    mNeighborSitesAbbreviated.put((int)asba.getSite().getValue(), asba);
                }
                break;
            case PHASE1_7D_IDENTIFIER_UPDATE:
                if(mac instanceof FrequencyBandUpdate band)
                {
                    mFrequencyBandMap.put(band.getIdentifier(), band);
                }
                break;
            case PHASE1_D6_SNDCP_DATA_CHANNEL_ANNOUNCEMENT:
                if(mac instanceof SNDCPDataChannelAnnouncement s)
                {
                    mSNDCPDataChannelAnnouncement = s;
                }
                break;
            case PHASE1_E9_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                if(mac instanceof SecondaryControlChannelBroadcastExplicit sccbe)
                {
                    for(IBandChannelDescriptor channel: sccbe.getChannels())
                    {
                        mSecondaryControlChannels.put(channel.toString(), channel);
                    }
                }
                break;
            case PHASE1_F3_IDENTIFIER_UPDATE_TDMA_EXTENDED:
                if(mac instanceof FrequencyBandUpdateTDMAExtended tdma)
                {
                    mFrequencyBandMap.put(tdma.getIdentifier(), tdma);
                }
                break;
            case PHASE1_FA_RFSS_STATUS_BROADCAST_EXPLICIT:
                if(mac instanceof RfssStatusBroadcastExplicit rsbe)
                {
                    mRFSSStatusBroadcastExplicit = rsbe;
                }
                break;
            case PHASE1_FB_NETWORK_STATUS_BROADCAST_EXPLICIT:
                if(mac instanceof NetworkStatusBroadcastExplicit nsbe)
                {
                    mNetworkStatusBroadcastExplicit = nsbe;
                }
                break;
            case PHASE1_FC_ADJACENT_STATUS_BROADCAST_EXPLICIT:
                if(mac instanceof AdjacentStatusBroadcastExplicit asbe)
                {
                    mNeighborSitesExtended.put((int)asbe.getSite().getValue(), asbe);
                }
            case PHASE1_FE_ADJACENT_STATUS_BROADCAST_EXTENDED_EXPLICIT:
                if(mac instanceof AdjacentStatusBroadcastExtendedExplicit a)
                {
                    mNeighborSitesExtendedExplicit.put(a.getSite().getValue(), a);
                }
                break;
        }
    }

    public void reset()
    {
        mFrequencyBandMap.clear();
        mNetworkStatusBroadcastImplicit = null;
        mNetworkStatusBroadcastExplicit = null;
        mRFSSStatusBroadcastImplicit = null;
        mRFSSStatusBroadcastExplicit = null;
        mSecondaryControlChannels.clear();
        mSystemServiceBroadcast = null;
        mNeighborSitesAbbreviated.clear();
        mNeighborSitesExtended.clear();
    }

    /**
     * Adds the network configuration sections to the activity summary.
     * @param summary to add to
     */
    public void addActivitySummary(ActivitySummary summary)
    {
        summary.section("Network");
        if(mNetworkStatusBroadcastImplicit != null)
        {
            summary.field("WACN", format(mNetworkStatusBroadcastImplicit.getWACN(), 5));
            summary.field("System", format(mNetworkStatusBroadcastImplicit.getSystem(), 3));
            summary.field("NAC", format(mNetworkStatusBroadcastImplicit.getNAC(), 3));
            summary.field("LRA", format(mNetworkStatusBroadcastImplicit.getLRA(), 2));
        }
        else if(mNetworkStatusBroadcastExplicit != null)
        {
            summary.field("WACN", format(mNetworkStatusBroadcastExplicit.getWACN(), 5));
            summary.field("System", format(mNetworkStatusBroadcastExplicit.getSystem(), 3));
            summary.field("NAC", format(mNetworkStatusBroadcastExplicit.getNAC(), 3));
            summary.field("LRA", format(mNetworkStatusBroadcastExplicit.getLRA(), 2));
        }
        else
        {
            summary.text(ActivitySummary.UNKNOWN);
        }

        summary.section("Current Site");
        IBandChannelDescriptor primary = null;

        if(mRFSSStatusBroadcastImplicit != null)
        {
            summary.field("System", format(mRFSSStatusBroadcastImplicit.getSystem(), 3));
            summary.field("RFSS", format(mRFSSStatusBroadcastImplicit.getRFSS(), 2));
            summary.field("Site", format(mRFSSStatusBroadcastImplicit.getSite(), 2));
            summary.field("LRA", format(mRFSSStatusBroadcastImplicit.getLRA(), 2));
            primary = mRFSSStatusBroadcastImplicit.getChannel();
        }
        else if(mRFSSStatusBroadcastExplicit != null)
        {
            summary.field("System", format(mRFSSStatusBroadcastExplicit.getSystem(), 3));
            summary.field("RFSS", format(mRFSSStatusBroadcastExplicit.getRFSS(), 2));
            summary.field("Site", format(mRFSSStatusBroadcastExplicit.getSite(), 2));
            summary.field("LRA", format(mRFSSStatusBroadcastExplicit.getLRA(), 2));
            primary = mRFSSStatusBroadcastExplicit.getChannel();
        }
        else
        {
            summary.text(ActivitySummary.UNKNOWN);
        }

        if(mSystemServiceBroadcast != null)
        {
            summary.field("Available Services", mSystemServiceBroadcast.getAvailableServices());
            summary.field("Supported Services", mSystemServiceBroadcast.getSupportedServices());
        }

        if(primary != null || mSNDCPDataChannelAnnouncement != null || !mSecondaryControlChannels.isEmpty())
        {
            ActivitySummary.Table channels = summary.table("Channel Type", "Channel", "Downlink", "Uplink");
            channelRow(channels, "Primary Control", primary);

            for(IBandChannelDescriptor channel: mSecondaryControlChannels.values())
            {
                channelRow(channels, "Secondary Control", channel);
            }

            if(mSNDCPDataChannelAnnouncement != null)
            {
                channelRow(channels, "Data", mSNDCPDataChannelAnnouncement.getChannel());
            }
        }

        summary.section("Neighbor Sites");
        ActivitySummary.Table neighbors = summary.table("System", "RFSS", "Site", "LRA", "Channel", "Downlink",
            "Uplink", "Status");
        Set<Integer> sites = new TreeSet<>();
        sites.addAll(mNeighborSitesAbbreviated.keySet());
        sites.addAll(mNeighborSitesExtended.keySet());
        sites.addAll(mNeighborSitesExtendedExplicit.keySet());

        for(Integer site: sites)
        {
            if(mNeighborSitesAbbreviated.containsKey(site))
            {
                AdjacentStatusBroadcastImplicit asb = mNeighborSitesAbbreviated.get(site);
                neighbors.row(format(asb.getSystem(), 3), format(asb.getRFSS(), 2), format(asb.getSite(), 2),
                    format(asb.getLRA(), 2), asb.getChannel(),
                    ActivitySummary.mhz(asb.getChannel().getDownlinkFrequency()),
                    ActivitySummary.mhz(asb.getChannel().getUplinkFrequency()), asb.getSiteFlags());
            }
            else if(mNeighborSitesExtended.containsKey(site))
            {
                AdjacentStatusBroadcastExplicit asb = mNeighborSitesExtended.get(site);
                neighbors.row(format(asb.getSystem(), 3), format(asb.getRFSS(), 2), format(asb.getSite(), 2),
                    format(asb.getLRA(), 2), asb.getChannel(),
                    ActivitySummary.mhz(asb.getChannel().getDownlinkFrequency()),
                    ActivitySummary.mhz(asb.getChannel().getUplinkFrequency()), asb.getSiteFlags());
            }
            else if(mNeighborSitesExtendedExplicit.containsKey(site))
            {
                AdjacentStatusBroadcastExtendedExplicit asb = mNeighborSitesExtendedExplicit.get(site);
                neighbors.row(format(asb.getSystem(), 3), format(asb.getRFSS(), 2), format(asb.getSite(), 2),
                    format(asb.getLRA(), 2), asb.getChannel(),
                    ActivitySummary.mhz(asb.getChannel().getDownlinkFrequency()),
                    ActivitySummary.mhz(asb.getChannel().getUplinkFrequency()), asb.getSiteFlags());
            }
        }

        P25P1NetworkConfigurationMonitor.addFrequencyBands(summary, mFrequencyBandMap);
    }

    /**
     * Adds a channel row to the channel table.
     */
    private static void channelRow(ActivitySummary.Table table, String type, IBandChannelDescriptor channel)
    {
        if(channel != null)
        {
            table.row(type, channel, ActivitySummary.mhz(channel.getDownlinkFrequency()),
                ActivitySummary.mhz(channel.getUplinkFrequency()));
        }
    }


}
