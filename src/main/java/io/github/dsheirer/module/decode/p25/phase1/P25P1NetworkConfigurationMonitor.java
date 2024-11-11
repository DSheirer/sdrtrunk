/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCAdjacentSiteStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCAdjacentSiteStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCNetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCRFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCRFSSStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSystemServiceBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCAdjacentStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCRFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaBaseStationId;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaExplicitTDMADataChannelAnnouncement;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.AdjacentStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SNDCPDataChannelAnnouncementExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SystemServiceBroadcast;
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
 * Tracks the network configuration details of a P25 Phase 1 network from the broadcast messages
 */
public class P25P1NetworkConfigurationMonitor
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P1NetworkConfigurationMonitor.class);

    private Map<Integer,IFrequencyBand> mFrequencyBandMap = new HashMap<>();

    //Network Status Messages
    private AMBTCNetworkStatusBroadcast mAMBTCNetworkStatusBroadcast;
    private NetworkStatusBroadcast mTSBKNetworkStatusBroadcast;
    private LCNetworkStatusBroadcast mLCNetworkStatusBroadcast;
    private LCNetworkStatusBroadcastExplicit mLCNetworkStatusBroadcastExplicit;

    //Current Site Status Messagese
    private RFSSStatusBroadcast mTSBKRFSSStatusBroadcast;
    private AMBTCRFSSStatusBroadcast mAMBTCRFSSStatusBroadcast;
    private LCRFSSStatusBroadcast mLCRFSSStatusBroadcast;
    private LCRFSSStatusBroadcastExplicit mLCRFSSStatusBroadcastExplicit;

    //Current Site Secondary Control Channels
    private Map<String,IChannelDescriptor> mSecondaryControlChannels = new TreeMap<>();

    //Current Site Data Channel(s)
    private SNDCPDataChannelAnnouncementExplicit mSNDCPDataChannel;
    private Map<APCO25Channel, MotorolaExplicitTDMADataChannelAnnouncement> mTDMADataChannelMap = new HashMap<>();

    //Current Site Services
    private SystemServiceBroadcast mTSBKSystemServiceBroadcast;
    private LCSystemServiceBroadcast mLCSystemServiceBroadcast;

    //Neighbor Sites
    private Map<Integer,AMBTCAdjacentStatusBroadcast> mAMBTCNeighborSites = new HashMap<>();
    private Map<Integer,LCAdjacentSiteStatusBroadcast> mLCNeighborSites = new HashMap<>();
    private Map<Integer,LCAdjacentSiteStatusBroadcastExplicit> mLCNeighborSitesExplicit = new HashMap<>();
    private Map<Integer,AdjacentStatusBroadcast> mTSBKNeighborSites = new HashMap<>();

    private MotorolaBaseStationId mMotorolaBaseStationId;

    private P25P1Decoder.Modulation mModulation;

    /**
     * Constructs a network configuration monitor.
     *
     * @param modulation type used by the decoder
     */
    public P25P1NetworkConfigurationMonitor(P25P1Decoder.Modulation modulation)
    {
        mModulation = modulation;
    }

    /**
     * Processes TSBK network configuration messages
     */
    public void process(TSBKMessage tsbk)
    {
        switch(tsbk.getOpcode())
        {
            case OSP_IDENTIFIER_UPDATE:
            case OSP_IDENTIFIER_UPDATE_TDMA:
            case OSP_IDENTIFIER_UPDATE_VHF_UHF_BANDS:
                if(tsbk instanceof IFrequencyBand)
                {
                    IFrequencyBand frequencyBand = (IFrequencyBand)tsbk;
                    mFrequencyBandMap.put(frequencyBand.getIdentifier(), frequencyBand);
                }
                break;
            case OSP_NETWORK_STATUS_BROADCAST:
                if(tsbk instanceof NetworkStatusBroadcast)
                {
                    mTSBKNetworkStatusBroadcast = (NetworkStatusBroadcast)tsbk;
                }
                break;
            case OSP_SYSTEM_SERVICE_BROADCAST:
                if(tsbk instanceof SystemServiceBroadcast)
                {
                    mTSBKSystemServiceBroadcast = (SystemServiceBroadcast)tsbk;
                }
                break;
            case OSP_RFSS_STATUS_BROADCAST:
                if(tsbk instanceof RFSSStatusBroadcast)
                {
                    mTSBKRFSSStatusBroadcast = (RFSSStatusBroadcast)tsbk;
                }
                break;
            case OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST:
                if(tsbk instanceof SecondaryControlChannelBroadcast)
                {
                    SecondaryControlChannelBroadcast sccb = (SecondaryControlChannelBroadcast)tsbk;

                    for(IChannelDescriptor secondaryControlChannel : sccb.getChannels())
                    {
                        mSecondaryControlChannels.put(secondaryControlChannel.toString(), secondaryControlChannel);
                    }
                }
                break;
            case OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                if(tsbk instanceof SecondaryControlChannelBroadcastExplicit)
                {
                    SecondaryControlChannelBroadcastExplicit sccbe = (SecondaryControlChannelBroadcastExplicit)tsbk;
                    IChannelDescriptor channel = sccbe.getChannel();
                    mSecondaryControlChannels.put(channel.toString(), channel);
                }
                break;
            case OSP_ADJACENT_STATUS_BROADCAST:
                if(tsbk instanceof AdjacentStatusBroadcast)
                {
                    AdjacentStatusBroadcast asb = (AdjacentStatusBroadcast)tsbk;
                    mTSBKNeighborSites.put((int)asb.getSite().getValue(), asb);
                }
                break;
            case OSP_SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                if(tsbk instanceof SNDCPDataChannelAnnouncementExplicit)
                {
                    mSNDCPDataChannel = (SNDCPDataChannelAnnouncementExplicit)tsbk;
                }
                break;
            case MOTOROLA_OSP_BASE_STATION_ID:
                if(tsbk instanceof MotorolaBaseStationId)
                {
                    mMotorolaBaseStationId = (MotorolaBaseStationId)tsbk;
                }
                break;
            case MOTOROLA_OSP_TDMA_DATA_CHANNEL:
                if(tsbk instanceof MotorolaExplicitTDMADataChannelAnnouncement tdma && tdma.hasChannel())
                {
                    mTDMADataChannelMap.put(tdma.getChannel(), tdma);
                }
                break;
        }
    }

    /**
     * Processes Alternate Multi-Block Trunking Control (AMBTC) messages for network configuration details
     */
    public void process(AMBTCMessage ambtc)
    {
        switch(ambtc.getHeader().getOpcode())
        {
            case OSP_ADJACENT_STATUS_BROADCAST:
                if(ambtc instanceof AMBTCAdjacentStatusBroadcast)
                {
                    AMBTCAdjacentStatusBroadcast aasb = (AMBTCAdjacentStatusBroadcast)ambtc;
                    mAMBTCNeighborSites.put((int)aasb.getSite().getValue(), aasb);
                }
                break;
            case OSP_NETWORK_STATUS_BROADCAST:
                if(ambtc instanceof AMBTCNetworkStatusBroadcast)
                {
                    mAMBTCNetworkStatusBroadcast = (AMBTCNetworkStatusBroadcast)ambtc;
                }
                break;
            case OSP_RFSS_STATUS_BROADCAST:
                if(ambtc instanceof AMBTCRFSSStatusBroadcast)
                {
                    mAMBTCRFSSStatusBroadcast = (AMBTCRFSSStatusBroadcast)ambtc;
                }
                break;
//TODO: process the rest of the messages here
        }
    }

    /**
     * Processes Link Control Word (LCW) messages with network configuration details
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
                        LCAdjacentSiteStatusBroadcast assb = (LCAdjacentSiteStatusBroadcast)lcw;
                        mLCNeighborSites.put((int)assb.getSite().getValue(), assb);
                    }
                    break;
                case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
                    if(lcw instanceof LCAdjacentSiteStatusBroadcastExplicit)
                    {
                        LCAdjacentSiteStatusBroadcastExplicit assbe = (LCAdjacentSiteStatusBroadcastExplicit)lcw;
                        mLCNeighborSitesExplicit.put((int)assbe.getSite().getValue(), assbe);
                    }
                    break;
                case CHANNEL_IDENTIFIER_UPDATE:
                case CHANNEL_IDENTIFIER_UPDATE_VU:
                    if(lcw instanceof IFrequencyBand)
                    {
                        IFrequencyBand band = (IFrequencyBand)lcw;
                        mFrequencyBandMap.put(band.getIdentifier(), band);
                    }
                    break;
                case NETWORK_STATUS_BROADCAST:
                    if(lcw instanceof LCNetworkStatusBroadcast)
                    {
                        mLCNetworkStatusBroadcast = (LCNetworkStatusBroadcast)lcw;
                    }
                    break;
                case NETWORK_STATUS_BROADCAST_EXPLICIT:
                    if(lcw instanceof LCNetworkStatusBroadcastExplicit)
                    {
                        mLCNetworkStatusBroadcastExplicit = (LCNetworkStatusBroadcastExplicit)lcw;
                    }
                    break;
                case RFSS_STATUS_BROADCAST:
                    if(lcw instanceof LCRFSSStatusBroadcast)
                    {
                        mLCRFSSStatusBroadcast = (LCRFSSStatusBroadcast)lcw;
                    }
                    break;
                case RFSS_STATUS_BROADCAST_EXPLICIT:
                    if(lcw instanceof LCRFSSStatusBroadcastExplicit)
                    {
                        mLCRFSSStatusBroadcastExplicit = (LCRFSSStatusBroadcastExplicit)lcw;
                    }
                    break;
                case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                    if(lcw instanceof LCSecondaryControlChannelBroadcast)
                    {
                        LCSecondaryControlChannelBroadcast sccb = (LCSecondaryControlChannelBroadcast)lcw;

                        for(IChannelDescriptor channel : sccb.getChannels())
                        {
                            mSecondaryControlChannels.put(channel.toString(), channel);
                        }
                    }
                    break;
                case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                    if(lcw instanceof LCSecondaryControlChannelBroadcastExplicit)
                    {
                        LCSecondaryControlChannelBroadcastExplicit sccb = (LCSecondaryControlChannelBroadcastExplicit)lcw;
                        for(IChannelDescriptor channel : sccb.getChannels())
                        {
                            mSecondaryControlChannels.put(channel.toString(), channel);
                        }
                    }
                    break;
                case SYSTEM_SERVICE_BROADCAST:
                    if(lcw instanceof LCSystemServiceBroadcast)
                    {
                        mLCSystemServiceBroadcast = (LCSystemServiceBroadcast)lcw;
                    }
                    break;
            }

        }

    }

    public void reset()
    {
        mFrequencyBandMap.clear();
        mAMBTCNetworkStatusBroadcast = null;
        mTSBKNetworkStatusBroadcast = null;
        mLCNetworkStatusBroadcast = null;
        mLCNetworkStatusBroadcastExplicit = null;
        mTSBKRFSSStatusBroadcast = null;
        mLCRFSSStatusBroadcast = null;
        mLCRFSSStatusBroadcastExplicit = null;
        mSecondaryControlChannels.clear();
        mSNDCPDataChannel = null;
        mTSBKSystemServiceBroadcast = null;
        mLCSystemServiceBroadcast = null;
        mAMBTCNeighborSites = new HashMap<>();
        mLCNeighborSites.clear();
        mLCNeighborSitesExplicit.clear();
        mTSBKNeighborSites.clear();
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

    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary - Decoder:P25 Phase 1 ").append(mModulation.getLabel());

        sb.append("\n\nNetwork\n");
        if(mTSBKNetworkStatusBroadcast != null)
        {
            sb.append("  WACN:").append(format(mTSBKNetworkStatusBroadcast.getWacn(), 5));
            sb.append(" SYSTEM:").append(format(mTSBKNetworkStatusBroadcast.getSystem(), 3));
            sb.append(" NAC:").append(format(mTSBKNetworkStatusBroadcast.getNAC(), 3));
            sb.append(" LRA:").append(format(mTSBKNetworkStatusBroadcast.getLocationRegistrationArea(), 2));
        }
        else if(mAMBTCNetworkStatusBroadcast != null)
        {
            sb.append("  WACN:").append(format(mAMBTCNetworkStatusBroadcast.getWacn(), 5));
            sb.append(" SYSTEM:").append(format(mAMBTCNetworkStatusBroadcast.getSystem(), 3));
            sb.append(" NAC:").append(format(mAMBTCNetworkStatusBroadcast.getNAC(), 3));
        }
        else if(mLCNetworkStatusBroadcast != null)
        {
            sb.append("  WACN:").append(format(mLCNetworkStatusBroadcast.getWACN(), 5));
            sb.append(" SYSTEM:").append(format(mLCNetworkStatusBroadcast.getSystem(), 3));
        }
        else if(mLCNetworkStatusBroadcastExplicit != null)
        {
            sb.append("  WACN:").append(format(mLCNetworkStatusBroadcastExplicit.getWACN(), 5));
            sb.append(" SYSTEM:").append(format(mLCNetworkStatusBroadcastExplicit.getSystem(), 3));
        }
        else
        {
            sb.append("  UNKNOWN");
        }

        sb.append("\n\nCurrent Site\n");

        if(mTSBKRFSSStatusBroadcast != null)
        {
            sb.append("  SYSTEM:").append(format(mTSBKRFSSStatusBroadcast.getSystem(), 3));
            sb.append(" NAC:").append(format(mTSBKRFSSStatusBroadcast.getNAC(), 3));
            sb.append(" RFSS:").append(format(mTSBKRFSSStatusBroadcast.getRfss(), 2));
            sb.append(" SITE:").append(format(mTSBKRFSSStatusBroadcast.getSite(), 2));
            sb.append(" LRA:").append(format(mTSBKRFSSStatusBroadcast.getLocationRegistrationArea(), 2));
            sb.append("  STATUS:").append(mTSBKRFSSStatusBroadcast.isActiveNetworkConnectionToRfssControllerSite() ?
                "ACTIVE RFSS NETWORK CONNECTION\n" : "\n");
            sb.append("  PRI CONTROL CHANNEL:").append(mTSBKRFSSStatusBroadcast.getChannel());
            sb.append(" DOWNLINK:").append(mTSBKRFSSStatusBroadcast.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mTSBKRFSSStatusBroadcast.getChannel().getUplinkFrequency()).append("\n");
        }
        else if(mLCRFSSStatusBroadcast != null)
        {
            sb.append("  SYSTEM:").append(format(mLCRFSSStatusBroadcast.getSystem(), 3));
            sb.append(" RFSS:").append(format(mLCRFSSStatusBroadcast.getRfss(), 2));
            sb.append(" SITE:").append(format(mLCRFSSStatusBroadcast.getSite(), 2));
            sb.append(" LRA:").append(format(mLCRFSSStatusBroadcast.getLocationRegistrationArea(), 2)).append("\n");
            sb.append("  PRI CONTROL CHANNEL:").append(mLCRFSSStatusBroadcast.getChannel());
            sb.append(" DOWNLINK:").append(mLCRFSSStatusBroadcast.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mLCRFSSStatusBroadcast.getChannel().getUplinkFrequency()).append("\n");
        }
        else if(mLCRFSSStatusBroadcastExplicit != null)
        {
            sb.append("  RFSS:").append(mLCRFSSStatusBroadcastExplicit.getRfss());
            sb.append(" SITE:").append(format(mLCRFSSStatusBroadcastExplicit.getSite(), 2));
            sb.append(" LRA:").append(format(mLCRFSSStatusBroadcastExplicit.getLocationRegistrationArea(), 2)).append("\n");
            sb.append("  PRI CONTROL CHANNEL:").append(mLCRFSSStatusBroadcastExplicit.getChannel());
            sb.append(" DOWNLINK:").append(mLCRFSSStatusBroadcastExplicit.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mLCRFSSStatusBroadcastExplicit.getChannel().getUplinkFrequency()).append("\n");
        }
        else if(mAMBTCRFSSStatusBroadcast != null)
        {
            sb.append("  SYSTEM:").append(format(mAMBTCRFSSStatusBroadcast.getSystem(), 3));
            sb.append(" NAC:").append(format(mAMBTCRFSSStatusBroadcast.getNAC(), 3));
            sb.append(" RFSS:").append(format(mAMBTCRFSSStatusBroadcast.getRFSS(), 2));
            sb.append(" SITE:").append(format(mAMBTCRFSSStatusBroadcast.getSite(), 2));
            sb.append(" LRA:").append(format(mAMBTCRFSSStatusBroadcast.getLRA(), 2));
            sb.append("  STATUS:").append(mAMBTCRFSSStatusBroadcast.isActiveNetworkConnectionToRfssControllerSite() ?
                "ACTIVE RFSS NETWORK CONNECTION\n" : "\n");
            sb.append("  PRI CONTROL CHANNEL:").append(mAMBTCRFSSStatusBroadcast.getChannel());
            sb.append(" DOWNLINK:").append(mAMBTCRFSSStatusBroadcast.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mAMBTCRFSSStatusBroadcast.getChannel().getUplinkFrequency()).append("\n");
        }
        else
        {
            sb.append("  UNKNOWN");
        }

        if(!mSecondaryControlChannels.isEmpty())
        {
            mSecondaryControlChannels
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .filter(Objects::nonNull)
                    .forEach(entry -> {
                        sb.append("  SEC CONTROL CHANNEL:").append(entry.getValue());
                        sb.append(" DOWNLINK:").append(entry.getValue().getDownlinkFrequency());
                        sb.append(" UPLINK:").append(entry.getValue().getUplinkFrequency()).append("\n");
                    });
        }

        if(mSNDCPDataChannel != null)
        {
            sb.append("  CURRENT FDMA DATA CHANNEL:").append(mSNDCPDataChannel.getChannel());
            sb.append(" DOWNLINK:").append(mSNDCPDataChannel.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mSNDCPDataChannel.getChannel().getUplinkFrequency()).append("\n");
        }

        if(!mTDMADataChannelMap.isEmpty())
        {
            for(Map.Entry<APCO25Channel, MotorolaExplicitTDMADataChannelAnnouncement> entry: mTDMADataChannelMap.entrySet())
            {
                sb.append("  ACTIVE TDMA DATA CHANNEL:").append(entry.getKey());
                sb.append(" DOWNLINK:").append(entry.getKey().getDownlinkFrequency());
                sb.append(" UPLINK:").append(entry.getKey().getUplinkFrequency()).append("\n");
            }
        }

        if(mMotorolaBaseStationId != null)
        {
            sb.append("  STATION ID/LICENSE: ").append(mMotorolaBaseStationId.getCWID()).append("\n");
        }

        if(mTSBKSystemServiceBroadcast != null)
        {
            sb.append("  AVAILABLE SERVICES:").append(mTSBKSystemServiceBroadcast.getAvailableServices());
            sb.append("  SUPPORTED SERVICES:").append(mTSBKSystemServiceBroadcast.getSupportedServices());
        }
        else if(mLCSystemServiceBroadcast != null)
        {
            sb.append("  AVAILABLE SERVICES:").append(mLCSystemServiceBroadcast.getAvailableServices());
            sb.append("  SUPPORTED SERVICES:").append(mLCSystemServiceBroadcast.getSupportedServices());
        }

        sb.append("\nNeighbor Sites\n");
        Set<Integer> sites = new TreeSet<>();
        sites.addAll(mAMBTCNeighborSites.keySet());
        sites.addAll(mLCNeighborSites.keySet());
        sites.addAll(mLCNeighborSitesExplicit.keySet());
        sites.addAll(mTSBKNeighborSites.keySet());

        if(sites.isEmpty())
        {
            sb.append("  UNKNOWN");
        }
        else
        {
            sites
                    .stream()
                    .sorted()
                    .forEach(site -> {
                        if(mAMBTCNeighborSites.containsKey(site))
                        {
                            AMBTCAdjacentStatusBroadcast ambtc = mAMBTCNeighborSites.get(site);
                            sb.append("  SYSTEM:").append(format(ambtc.getSystem(), 3));
                            sb.append(" NAC:").append(format(ambtc.getNAC(), 3));
                            sb.append(" RFSS:").append(format(ambtc.getRfss(), 2));
                            sb.append(" SITE:").append(format(ambtc.getSite(), 2));
                            sb.append(" LRA:").append(format(ambtc.getLocationRegistrationArea(), 2));
                            sb.append(" CHANNEL:").append(ambtc.getChannel());
                            sb.append(" DOWNLINK:").append(ambtc.getChannel().getDownlinkFrequency());
                            sb.append(" UPLINK:").append(ambtc.getChannel().getUplinkFrequency()).append("\n");
                        }
                        if(mLCNeighborSites.containsKey(site))
                        {
                            LCAdjacentSiteStatusBroadcast lc = mLCNeighborSites.get(site);
                            sb.append("  SYSTEM:").append(format(lc.getSystem(), 3));
                            sb.append(" RFSS:").append(format(lc.getRfss(), 2));
                            sb.append(" SITE:").append(format(lc.getSite(), 2));
                            sb.append(" LRA:").append(format(lc.getLocationRegistrationArea(), 2));
                            sb.append(" CHANNEL:").append(lc.getChannel());
                            sb.append(" DOWNLINK:").append(lc.getChannel().getDownlinkFrequency());
                            sb.append(" UPLINK:").append(lc.getChannel().getUplinkFrequency()).append("\n");

                        }
                        if(mLCNeighborSitesExplicit.containsKey(site))
                        {
                            LCAdjacentSiteStatusBroadcastExplicit lce = mLCNeighborSitesExplicit.get(site);
                            sb.append("  SYSTEM:---");
                            sb.append(" RFSS:").append(format(lce.getRfss(), 2));
                            sb.append(" SITE:").append(format(lce.getSite(), 2));
                            sb.append(" LRA:").append(format(lce.getLocationRegistrationArea(), 2));
                            sb.append(" CHANNEL:").append(lce.getChannel());
                            sb.append(" DOWNLINK:").append(lce.getChannel().getDownlinkFrequency());
                            sb.append(" UPLINK:").append(lce.getChannel().getUplinkFrequency()).append("\n");
                        }
                        if(mTSBKNeighborSites.containsKey(site))
                        {
                            AdjacentStatusBroadcast asb = mTSBKNeighborSites.get(site);
                            sb.append("  SYSTEM:").append(format(asb.getSystem(), 3));
                            sb.append(" NAC:").append(format(asb.getNAC(), 3));
                            sb.append(" RFSS:").append(format(asb.getRfss(), 2));
                            sb.append(" SITE:").append(format(asb.getSite(), 2));
                            sb.append(" LRA:").append(format(asb.getLocationRegistrationArea(), 2));
                            sb.append(" CHANNEL:").append(asb.getChannel());
                            sb.append(" DOWNLINK:").append(asb.getChannel().getDownlinkFrequency());
                            sb.append(" UPLINK:").append(asb.getChannel().getUplinkFrequency());
                            sb.append(" STATUS:").append(asb.getSiteFlags()).append("\n");
                        }
                    });
        }

        sb.append("\nFrequency Bands\n");
        if(mFrequencyBandMap.isEmpty())
        {
            sb.append("  UNKNOWN");
        }
        else
        {
            mFrequencyBandMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sb.append("  ").append(formatFrequencyBand(entry.getValue())).append("\n"));
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
