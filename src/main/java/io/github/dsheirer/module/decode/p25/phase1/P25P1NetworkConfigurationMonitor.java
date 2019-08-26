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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.channel.IChannelDescriptor;
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
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.AdjacentStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SNDCPDataChannelAnnouncementExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SystemServiceBroadcast;
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

    //Current Site Data Channel
    private SNDCPDataChannelAnnouncementExplicit mSNDCPDataChannel;

    //Current Site Services
    private SystemServiceBroadcast mTSBKSystemServiceBroadcast;
    private LCSystemServiceBroadcast mLCSystemServiceBroadcast;

    //Neighbor Sites
    private Map<Integer,AMBTCAdjacentStatusBroadcast> mAMBTCNeighborSites = new HashMap<>();
    private Map<Integer,LCAdjacentSiteStatusBroadcast> mLCNeighborSites = new HashMap<>();
    private Map<Integer,LCAdjacentSiteStatusBroadcastExplicit> mLCNeighborSitesExplicit = new HashMap<>();
    private Map<Integer,AdjacentStatusBroadcast> mTSBKNeighborSites = new HashMap<>();

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
                case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
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

    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary - Decoder:P25 Phase 1 ").append(mModulation.getLabel());

        sb.append("\n\nNetwork\n");
        if(mTSBKNetworkStatusBroadcast != null)
        {
            sb.append("  NAC:").append(mTSBKNetworkStatusBroadcast.getNAC());
            sb.append(" WACN:").append(mTSBKNetworkStatusBroadcast.getWacn());
            sb.append(" SYSTEM:").append(mTSBKNetworkStatusBroadcast.getSystem());
            sb.append(" LRA:").append(mTSBKNetworkStatusBroadcast.getLocationRegistrationArea());
        }
        else if(mAMBTCNetworkStatusBroadcast != null)
        {
            sb.append("  NAC:").append(mAMBTCNetworkStatusBroadcast.getNAC());
            sb.append(" WACN:").append(mAMBTCNetworkStatusBroadcast.getWacn());
            sb.append(" SYSTEM:").append(mAMBTCNetworkStatusBroadcast.getSystem());
        }
        else if(mLCNetworkStatusBroadcast != null)
        {
            sb.append("  WACN:").append(mLCNetworkStatusBroadcast.getWACN());
            sb.append(" SYSTEM:").append(mLCNetworkStatusBroadcast.getSystem());
        }
        else if(mLCNetworkStatusBroadcastExplicit != null)
        {
            sb.append("  WACN:").append(mLCNetworkStatusBroadcastExplicit.getWACN());
            sb.append(" SYSTEM:").append(mLCNetworkStatusBroadcastExplicit.getSystem());
        }
        else
        {
            sb.append("  UNKNOWN");
        }

        sb.append("\n\nCurrent Site\n");
        if(mTSBKRFSSStatusBroadcast != null)
        {
            sb.append("  SYSTEM:").append(mTSBKRFSSStatusBroadcast.getSystem());
            sb.append(" SITE:").append(mTSBKRFSSStatusBroadcast.getSite());
            sb.append(" RF SUBSYSTEM:").append(mTSBKRFSSStatusBroadcast.getRfss());
            sb.append(" LOCATION REGISTRATION AREA:").append(mTSBKRFSSStatusBroadcast.getLocationRegistrationArea());
            sb.append("  STATUS:").append(mTSBKRFSSStatusBroadcast.isActiveNetworkConnectionToRfssControllerSite() ?
                "ACTIVE RFSS NETWORK CONNECTION\n" : "\n");
            sb.append("  PRI CONTROL CHANNEL:").append(mTSBKRFSSStatusBroadcast.getChannel());
            sb.append(" DOWNLINK:").append(mTSBKRFSSStatusBroadcast.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mTSBKRFSSStatusBroadcast.getChannel().getUplinkFrequency()).append("\n");
        }
        else if(mLCRFSSStatusBroadcast != null)
        {
            sb.append("  SYSTEM:").append(mLCRFSSStatusBroadcast.getSystem());
            sb.append(" SITE:").append(mLCRFSSStatusBroadcast.getSite());
            sb.append(" RF SUBSYSTEM:").append(mLCRFSSStatusBroadcast.getRfss());
            sb.append(" LOCATION REGISTRATION AREA:").append(mLCRFSSStatusBroadcast.getLocationRegistrationArea()).append("\n");
            sb.append("  PRI CONTROL CHANNEL:").append(mLCRFSSStatusBroadcast.getChannel());
            sb.append(" DOWNLINK:").append(mLCRFSSStatusBroadcast.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mLCRFSSStatusBroadcast.getChannel().getUplinkFrequency()).append("\n");
        }
        else if(mLCRFSSStatusBroadcastExplicit != null)
        {
            sb.append("  SITE:").append(mLCRFSSStatusBroadcastExplicit.getSite());
            sb.append(" RF SUBSYSTEM:").append(mLCRFSSStatusBroadcastExplicit.getRfss());
            sb.append(" LOCATION REGISTRATION AREA:").append(mLCRFSSStatusBroadcastExplicit.getLocationRegistrationArea()).append("\n");
            sb.append("  PRI CONTROL CHANNEL:").append(mLCRFSSStatusBroadcastExplicit.getChannel());
            sb.append(" DOWNLINK:").append(mLCRFSSStatusBroadcastExplicit.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mLCRFSSStatusBroadcastExplicit.getChannel().getUplinkFrequency()).append("\n");
        }
        else if(mAMBTCRFSSStatusBroadcast != null)
        {
            sb.append("  SYSTEM:").append(mAMBTCRFSSStatusBroadcast.getSystem());
            sb.append(" SITE:").append(mAMBTCRFSSStatusBroadcast.getSite());
            sb.append(" RF SUBSYSTEM:").append(mAMBTCRFSSStatusBroadcast.getRFSS());
            sb.append(" LOCATION REGISTRATION AREA:").append(mAMBTCRFSSStatusBroadcast.getLRA());
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

        if(mSNDCPDataChannel != null)
        {
            sb.append("  CURRENT DATA CHANNEL:").append(mSNDCPDataChannel.getChannel());
            sb.append(" DOWNLINK:").append(mSNDCPDataChannel.getChannel().getDownlinkFrequency());
            sb.append(" UPLINK:").append(mSNDCPDataChannel.getChannel().getUplinkFrequency()).append("\n");
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
            List<Integer> sitesSorted = new ArrayList<>(sites);
            Collections.sort(sitesSorted);

            for(Integer site : sitesSorted)
            {
                if(mAMBTCNeighborSites.containsKey(site))
                {
                    AMBTCAdjacentStatusBroadcast ambtc = mAMBTCNeighborSites.get(site);
                    sb.append("  SYSTEM:").append(ambtc.getSystem());
                    sb.append(" SITE:").append(ambtc.getSite());
                    sb.append(" LRA:").append(ambtc.getLocationRegistrationArea());
                    sb.append(" RFSS:").append(ambtc.getRfss());
                    sb.append(" CHANNEL:").append(ambtc.getChannel());
                    sb.append(" DOWNLINK:").append(ambtc.getChannel().getDownlinkFrequency());
                    sb.append(" UPLINK:").append(ambtc.getChannel().getUplinkFrequency()).append("\n");
                }
                if(mLCNeighborSites.containsKey(site))
                {
                    LCAdjacentSiteStatusBroadcast lc = mLCNeighborSites.get(site);
                    sb.append("  SYSTEM:").append(lc.getSystem());
                    sb.append(" SITE:").append(lc.getSite());
                    sb.append(" LRA:").append(lc.getLocationRegistrationArea());
                    sb.append(" RFSS:").append(lc.getRfss());
                    sb.append(" CHANNEL:").append(lc.getChannel());
                    sb.append(" DOWNLINK:").append(lc.getChannel().getDownlinkFrequency());
                    sb.append(" UPLINK:").append(lc.getChannel().getUplinkFrequency()).append("\n");

                }
                if(mLCNeighborSitesExplicit.containsKey(site))
                {
                    LCAdjacentSiteStatusBroadcastExplicit lce = mLCNeighborSitesExplicit.get(site);
                    sb.append("  SYSTEM:---");
                    sb.append(" SITE:").append(lce.getSite());
                    sb.append(" LRA:").append(lce.getLocationRegistrationArea());
                    sb.append(" RFSS:").append(lce.getRfss());
                    sb.append(" CHANNEL:").append(lce.getChannel());
                    sb.append(" DOWNLINK:").append(lce.getChannel().getDownlinkFrequency());
                    sb.append(" UPLINK:").append(lce.getChannel().getUplinkFrequency()).append("\n");
                }
                if(mTSBKNeighborSites.containsKey(site))
                {
                    AdjacentStatusBroadcast asb = mTSBKNeighborSites.get(site);
                    sb.append("  SYSTEM:").append(asb.getSystem());
                    sb.append(" SITE:").append(asb.getSite());
                    sb.append(" LRA:").append(asb.getLocationRegistrationArea());
                    sb.append(" RFSS:").append(asb.getRfss());
                    sb.append(" CHANNEL:").append(asb.getChannel());
                    sb.append(" DOWNLINK:").append(asb.getChannel().getDownlinkFrequency());
                    sb.append(" UPLINK:").append(asb.getChannel().getUplinkFrequency());
                    sb.append(" STATUS:").append(asb.getSiteFlags()).append("\n");
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
