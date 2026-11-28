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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.channel.IBandChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.event.ActivitySummary;
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
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCAdjacentStatusBroadcastUncoordinated;
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
    private Map<String, IBandChannelDescriptor> mSecondaryControlChannels = new TreeMap<>();

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

    private Modulation mModulation;

    /**
     * Constructs a network configuration monitor.
     *
     * @param modulation type used by the decoder
     */
    public P25P1NetworkConfigurationMonitor(Modulation modulation)
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

                    for(IBandChannelDescriptor secondaryControlChannel : sccb.getChannels())
                    {
                        mSecondaryControlChannels.put(secondaryControlChannel.toString(), secondaryControlChannel);
                    }
                }
                break;
            case OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                if(tsbk instanceof SecondaryControlChannelBroadcastExplicit)
                {
                    SecondaryControlChannelBroadcastExplicit sccbe = (SecondaryControlChannelBroadcastExplicit)tsbk;
                    IBandChannelDescriptor channel = sccbe.getChannel();
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
            case OSP_ADJACENT_STATUS_BROADCAST_UNCOORDINATED_BAND_PLAN:
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

                        for(IBandChannelDescriptor channel : sccb.getChannels())
                        {
                            mSecondaryControlChannels.put(channel.toString(), channel);
                        }
                    }
                    break;
                case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                    if(lcw instanceof LCSecondaryControlChannelBroadcastExplicit)
                    {
                        LCSecondaryControlChannelBroadcastExplicit sccb = (LCSecondaryControlChannelBroadcastExplicit)lcw;
                        for(IBandChannelDescriptor channel : sccb.getChannels())
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

    /**
     * Adds the network configuration sections to the activity summary.
     * @param summary to add to
     */
    public void addActivitySummary(ActivitySummary summary)
    {
        summary.section("Network");
        if(mTSBKNetworkStatusBroadcast != null)
        {
            summary.field("WACN", format(mTSBKNetworkStatusBroadcast.getWacn(), 5));
            summary.field("System", format(mTSBKNetworkStatusBroadcast.getSystem(), 3));
            summary.field("NAC", format(mTSBKNetworkStatusBroadcast.getNAC(), 3));
            summary.field("LRA", format(mTSBKNetworkStatusBroadcast.getLocationRegistrationArea(), 2));
        }
        else if(mAMBTCNetworkStatusBroadcast != null)
        {
            summary.field("WACN", format(mAMBTCNetworkStatusBroadcast.getWacn(), 5));
            summary.field("System", format(mAMBTCNetworkStatusBroadcast.getSystem(), 3));
            summary.field("NAC", format(mAMBTCNetworkStatusBroadcast.getNAC(), 3));
        }
        else if(mLCNetworkStatusBroadcast != null)
        {
            summary.field("WACN", format(mLCNetworkStatusBroadcast.getWACN(), 5));
            summary.field("System", format(mLCNetworkStatusBroadcast.getSystem(), 3));
        }
        else if(mLCNetworkStatusBroadcastExplicit != null)
        {
            summary.field("WACN", format(mLCNetworkStatusBroadcastExplicit.getWACN(), 5));
            summary.field("System", format(mLCNetworkStatusBroadcastExplicit.getSystem(), 3));
        }
        else
        {
            summary.text(ActivitySummary.UNKNOWN);
        }

        summary.section("Current Site");
        ActivitySummary.Table channels = null;

        if(mTSBKRFSSStatusBroadcast != null)
        {
            summary.field("System", format(mTSBKRFSSStatusBroadcast.getSystem(), 3));
            summary.field("NAC", format(mTSBKRFSSStatusBroadcast.getNAC(), 3));
            summary.field("RFSS", format(mTSBKRFSSStatusBroadcast.getRfss(), 2));
            summary.field("Site", format(mTSBKRFSSStatusBroadcast.getSite(), 2));
            summary.field("LRA", format(mTSBKRFSSStatusBroadcast.getLocationRegistrationArea(), 2));
            if(mTSBKRFSSStatusBroadcast.isActiveNetworkConnectionToRfssControllerSite())
            {
                summary.field("Status", "Active RFSS Network Connection");
            }
            channels = channelTable(summary);
            channelRow(channels, "Primary Control", mTSBKRFSSStatusBroadcast.getChannel());
        }
        else if(mLCRFSSStatusBroadcast != null)
        {
            summary.field("System", format(mLCRFSSStatusBroadcast.getSystem(), 3));
            summary.field("RFSS", format(mLCRFSSStatusBroadcast.getRfss(), 2));
            summary.field("Site", format(mLCRFSSStatusBroadcast.getSite(), 2));
            summary.field("LRA", format(mLCRFSSStatusBroadcast.getLocationRegistrationArea(), 2));
            channels = channelTable(summary);
            channelRow(channels, "Primary Control", mLCRFSSStatusBroadcast.getChannel());
        }
        else if(mLCRFSSStatusBroadcastExplicit != null)
        {
            summary.field("RFSS", mLCRFSSStatusBroadcastExplicit.getRfss());
            summary.field("Site", format(mLCRFSSStatusBroadcastExplicit.getSite(), 2));
            summary.field("LRA", format(mLCRFSSStatusBroadcastExplicit.getLocationRegistrationArea(), 2));
            channels = channelTable(summary);
            channelRow(channels, "Primary Control", mLCRFSSStatusBroadcastExplicit.getChannel());
        }
        else if(mAMBTCRFSSStatusBroadcast != null)
        {
            summary.field("System", format(mAMBTCRFSSStatusBroadcast.getSystem(), 3));
            summary.field("NAC", format(mAMBTCRFSSStatusBroadcast.getNAC(), 3));
            summary.field("RFSS", format(mAMBTCRFSSStatusBroadcast.getRFSS(), 2));
            summary.field("Site", format(mAMBTCRFSSStatusBroadcast.getSite(), 2));
            summary.field("LRA", format(mAMBTCRFSSStatusBroadcast.getLRA(), 2));
            if(mAMBTCRFSSStatusBroadcast.isActiveNetworkConnectionToRfssControllerSite())
            {
                summary.field("Status", "Active RFSS Network Connection");
            }
            channels = channelTable(summary);
            channelRow(channels, "Primary Control", mAMBTCRFSSStatusBroadcast.getChannel());
        }
        else
        {
            summary.text(ActivitySummary.UNKNOWN);
        }

        if(mMotorolaBaseStationId != null)
        {
            summary.field("Station ID", mMotorolaBaseStationId.getCWID());
        }

        if(mTSBKSystemServiceBroadcast != null)
        {
            summary.field("Available Services", mTSBKSystemServiceBroadcast.getAvailableServices());
            summary.field("Supported Services", mTSBKSystemServiceBroadcast.getSupportedServices());
        }
        else if(mLCSystemServiceBroadcast != null)
        {
            summary.field("Available Services", mLCSystemServiceBroadcast.getAvailableServices());
            summary.field("Supported Services", mLCSystemServiceBroadcast.getSupportedServices());
        }

        if(!mSecondaryControlChannels.isEmpty() || mSNDCPDataChannel != null || !mTDMADataChannelMap.isEmpty())
        {
            if(channels == null)
            {
                channels = channelTable(summary);
            }

            for(IBandChannelDescriptor channel: mSecondaryControlChannels.values())
            {
                channelRow(channels, "Secondary Control", channel);
            }

            if(mSNDCPDataChannel != null)
            {
                channelRow(channels, "FDMA Data (Current)", mSNDCPDataChannel.getChannel());
            }

            for(APCO25Channel channel: mTDMADataChannelMap.keySet())
            {
                channelRow(channels, "TDMA Data (Active)", channel);
            }
        }

        summary.section("Neighbor Sites");
        ActivitySummary.Table neighbors = summary.table("WACN", "System", "NAC", "RFSS", "Site", "LRA", "Channel",
            "Downlink", "Uplink", "Status");
        Set<Integer> sites = new TreeSet<>();
        sites.addAll(mAMBTCNeighborSites.keySet());
        sites.addAll(mLCNeighborSites.keySet());
        sites.addAll(mLCNeighborSitesExplicit.keySet());
        sites.addAll(mTSBKNeighborSites.keySet());

        for(Integer site: sites)
        {
            if(mAMBTCNeighborSites.containsKey(site))
            {
                AMBTCAdjacentStatusBroadcast ambtc = mAMBTCNeighborSites.get(site);
                String wacn = null;
                Object status = null;
                if(ambtc instanceof AMBTCAdjacentStatusBroadcastUncoordinated unc)
                {
                    wacn = String.valueOf(unc.getWacn());
                    status = unc.getSystemServiceClass();
                }
                neighbors.row(wacn, format(ambtc.getSystem(), 3), format(ambtc.getNAC(), 3),
                    format(ambtc.getRfss(), 2), format(ambtc.getSite(), 2),
                    format(ambtc.getLocationRegistrationArea(), 2), ambtc.getChannel(),
                    ActivitySummary.mhz(ambtc.getChannel().getDownlinkFrequency()),
                    ActivitySummary.mhz(ambtc.getChannel().getUplinkFrequency()), status);
            }
            if(mLCNeighborSites.containsKey(site))
            {
                LCAdjacentSiteStatusBroadcast lc = mLCNeighborSites.get(site);
                neighbors.row(null, format(lc.getSystem(), 3), null, format(lc.getRfss(), 2),
                    format(lc.getSite(), 2), format(lc.getLocationRegistrationArea(), 2), lc.getChannel(),
                    ActivitySummary.mhz(lc.getChannel().getDownlinkFrequency()),
                    ActivitySummary.mhz(lc.getChannel().getUplinkFrequency()), lc.getSiteFlags());
            }
            if(mLCNeighborSitesExplicit.containsKey(site))
            {
                LCAdjacentSiteStatusBroadcastExplicit lce = mLCNeighborSitesExplicit.get(site);
                neighbors.row(null, null, null, format(lce.getRfss(), 2), format(lce.getSite(), 2),
                    format(lce.getLocationRegistrationArea(), 2), lce.getChannel(),
                    ActivitySummary.mhz(lce.getChannel().getDownlinkFrequency()),
                    ActivitySummary.mhz(lce.getChannel().getUplinkFrequency()), lce.getSiteFlags());
            }
            if(mTSBKNeighborSites.containsKey(site))
            {
                AdjacentStatusBroadcast asb = mTSBKNeighborSites.get(site);
                neighbors.row(null, format(asb.getSystem(), 3), format(asb.getNAC(), 3), format(asb.getRfss(), 2),
                    format(asb.getSite(), 2), format(asb.getLocationRegistrationArea(), 2), asb.getChannel(),
                    ActivitySummary.mhz(asb.getChannel().getDownlinkFrequency()),
                    ActivitySummary.mhz(asb.getChannel().getUplinkFrequency()), asb.getSiteFlags());
            }
        }

        addFrequencyBands(summary, mFrequencyBandMap);
    }

    /**
     * Creates a channel table for the current site section.
     */
    private static ActivitySummary.Table channelTable(ActivitySummary summary)
    {
        return summary.table("Channel Type", "Channel", "Downlink", "Uplink");
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

    /**
     * Adds a frequency bands section to the activity summary.
     * @param summary to add to
     * @param bands map of band identifier to frequency band
     */
    public static void addFrequencyBands(ActivitySummary summary, Map<Integer,IFrequencyBand> bands)
    {
        summary.section("Frequency Bands");
        ActivitySummary.Table table = summary.table("Band", "Access", "Base (MHz)", "Bandwidth (kHz)",
            "Spacing (kHz)", "Tx Offset (MHz)", "Timeslots");

        bands.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
            .forEach(band -> table.row(band.getIdentifier(), band.isTDMA() ? "TDMA" : "FDMA",
                ActivitySummary.mhz(band.getBaseFrequency()), ActivitySummary.khz(band.getBandwidth()),
                ActivitySummary.khz(band.getChannelSpacing()), ActivitySummary.offsetMhz(band.getTransmitOffset()),
                band.isTDMA() ? band.getTimeslotCount() : null));
    }


}
