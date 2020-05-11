/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRDecoder;
import io.github.dsheirer.module.decode.dmr.message.data.lc.ShortLCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.*;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCAdjacentStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCRFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaBaseStationId;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Tracks the network configuration details of a P25 Phase 1 network from the broadcast messages
 */
public class DMRNetworkConfigurationMonitor
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRNetworkConfigurationMonitor.class);

    private Map<Integer,IFrequencyBand> mFrequencyBandMap = new HashMap<>();


    //Current Site Status Messages
    private Map<String,IChannelDescriptor> mSecondaryControlChannels = new TreeMap<>();


    //Current Site Services
    private String currentSiteNetwork = "";

    //Neighbor Sites
    private Map<Integer,AMBTCAdjacentStatusBroadcast> mAMBTCNeighborSites = new HashMap<>();
    private Map<Integer,LCAdjacentSiteStatusBroadcast> mLCNeighborSites = new HashMap<>();
    private Map<Integer,LCAdjacentSiteStatusBroadcastExplicit> mLCNeighborSitesExplicit = new HashMap<>();
    private Map<Integer,AdjacentStatusBroadcast> mTSBKNeighborSites = new HashMap<>();



    /**
     * Constructs a network configuration monitor.
     *
     */
    public DMRNetworkConfigurationMonitor()
    {


    }

    /**
     * Processes ShortLC network config
     */
    public void processShortLC(ShortLCMessage slc, int featId)
    {
        StringBuilder sb = new StringBuilder();
        if(slc.isValid()) {
            int pl = slc.getPayLoad();
            if(featId == 6) {
                sb.append("NetWork: " + ((pl & 0xfff000) >> 12) + "-" + ((pl & 0xf0) >> 4));
            } else {
                sb.append("Network undecoded");
            }
        }
        currentSiteNetwork = sb.toString();
    }

    /**
     * Processes Alternate Multi-Block Trunking Control (AMBTC) messages for network configuration details
     */
    public void process(AMBTCMessage ambtc)
    {

    }

    /**
     * Processes Link Control Word (LCW) messages with network configuration details
     */
    public void process(LinkControlWord lcw)
    {

    }

    public void reset()
    {
        mFrequencyBandMap.clear();
        mSecondaryControlChannels.clear();
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
            String hex = Integer.toHexString((Integer)identifier.getValue());

            while(hex.length() < width)
            {
                hex = "0" + hex;
            }

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

        sb.append("Activity Summary - Decoder:DMR ");

        sb.append("\n\nCurrent Site\n");

        if(currentSiteNetwork != null)
        {
            sb.append(" SITE:").append(currentSiteNetwork);
            // UPLINK
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
