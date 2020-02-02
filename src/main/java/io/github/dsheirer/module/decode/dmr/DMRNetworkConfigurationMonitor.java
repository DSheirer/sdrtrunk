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

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.site.SiteIdentifier;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.dmr.identifier.DMRNetwork;
import io.github.dsheirer.module.decode.dmr.identifier.DMRSite;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraAdjacentSiteInformation;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera.HyteraAnnouncement;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.CapacityMaxAloha;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusNeighborReport;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Aloha;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.AdjacentSiteInformation;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ConnectPlusControlChannel;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ConnectPlusTrafficChannel;
import io.github.dsheirer.module.decode.dmr.message.type.Model;
import io.github.dsheirer.module.decode.dmr.message.type.SystemIdentityCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the network configuration details of a DMR network from the broadcast messages
 */
public class DMRNetworkConfigurationMonitor
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRNetworkConfigurationMonitor.class);
    private static final String BRAND_HYTERA = "Hytera";
    private static final String BRAND_MOTOROLA_CAPACITY_PLUS = "Motorola Capacity+";
    private static final String BRAND_MOTOROLA_CONNECT_PLUS = "Motorola Connect+";
    private static final String BRAND_STANDARD = "Standard";
    private static final String BRAND_TIER_3_TRUNKING = "Tier III Trunking";
    private static final String BRAND_MOTOROLA_CAPACITY_MAX_TIER_3_TRUNKING = "Capacity Max Tier III Trunking";
    private static final String BRAND_HYTERA_TIER_3_TRUNKING = "Hytera Tier III Trunking";

    private List<SiteIdentifier> mNeighborSites = new ArrayList<>();
    private Map<Integer,AdjacentSiteInformation> mTier3NeighborSites = new HashMap<>();
    private List<DMRChannel> mObservedDmrChannels = new ArrayList<>();
    private DMRNetwork mDMRNetwork;
    private DMRSite mDMRSite;
    private Model mTier3Model;
    private String mBrand;
    private Integer mColorCode;
    private int mTimeslot;
    private DMRChannel mCurrentChannel;
    private Channel mChannel;

    /**
     * Constructs an instance
     *
     * @param timeslot to monitor
     * @param channel configuration
     */
    public DMRNetworkConfigurationMonitor(int timeslot, Channel channel)
    {
        mTimeslot = timeslot;
        mChannel = channel;
    }

    /**
     * Sets the current channel
     */
    public void setCurrentChannel(DMRChannel channel)
    {
        mCurrentChannel = channel;
    }

    /**
     * Processes link control messages
     */
    public void process(LCMessage linkControl)
    {
        switch(linkControl.getOpcode())
        {
            case SHORT_CONNECT_PLUS_CONTROL_CHANNEL:
                if((mDMRNetwork == null || mDMRSite == null) && linkControl instanceof ConnectPlusControlChannel)
                {
                    ConnectPlusControlChannel cpcc = (ConnectPlusControlChannel)linkControl;
                    mDMRNetwork = cpcc.getNetwork();
                    mDMRSite = cpcc.getSite();
                }
                if(mBrand == null)
                {
                    mBrand = BRAND_MOTOROLA_CONNECT_PLUS;
                }
                break;
            case SHORT_CONNECT_PLUS_TRAFFIC_CHANNEL:
                if((mDMRNetwork == null || mDMRSite == null) && linkControl instanceof ConnectPlusTrafficChannel)
                {
                    ConnectPlusTrafficChannel cptc = (ConnectPlusTrafficChannel)linkControl;
                    mDMRNetwork = cptc.getNetwork();
                    mDMRSite = cptc.getSite();
                }
                if(mBrand == null)
                {
                    mBrand = BRAND_MOTOROLA_CONNECT_PLUS;
                }
                break;
        }
    }

    /**
     * Processes Control Signalling Blocks (CSBK)
     */
    public void process(CSBKMessage csbk)
    {
        switch(csbk.getOpcode())
        {
            case STANDARD_ALOHA:
                if(csbk instanceof Aloha)
                {
                    if(mDMRNetwork == null || mDMRSite == null)
                    {
                        SystemIdentityCode sic = ((Aloha)csbk).getSystemIdentityCode();

                        if(mDMRNetwork == null)
                        {
                            mDMRNetwork = sic.getNetwork();
                        }
                        if(mDMRSite == null)
                        {
                            mDMRSite = sic.getSite();
                        }
                        if(mTier3Model == null)
                        {
                            mTier3Model = sic.getModel();
                        }
                    }

                    if(mBrand == null)
                    {
                        mBrand = BRAND_TIER_3_TRUNKING;
                    }
                }
                break;
            case HYTERA_08_ANNOUNCEMENT:
            case HYTERA_68_ANNOUNCEMENT:
                if(csbk instanceof HyteraAnnouncement)
                {
                    HyteraAnnouncement ha = (HyteraAnnouncement)csbk;

                    if(mBrand == null)
                    {
                        mBrand = BRAND_HYTERA_TIER_3_TRUNKING;
                    }

                    if(mDMRNetwork == null)
                    {
                        mDMRNetwork = ha.getSystemIdentityCode().getNetwork();
                    }
                    if(mDMRSite == null)
                    {
                        mDMRSite = ha.getSystemIdentityCode().getSite();
                    }
                    if(mTier3Model == null)
                    {
                        mTier3Model = ha.getSystemIdentityCode().getModel();
                    }

                    if(mBrand == null || mBrand != BRAND_HYTERA_TIER_3_TRUNKING)
                    {
                        mBrand = BRAND_HYTERA_TIER_3_TRUNKING;
                    }
                }
                if(csbk instanceof HyteraAdjacentSiteInformation)
                {
                    HyteraAdjacentSiteInformation hasi = (HyteraAdjacentSiteInformation)csbk;

                    int site = hasi.getNeighborSystemIdentityCode().getSite().getValue();

                    if(!mTier3NeighborSites.containsKey(site))
                    {
                        mTier3NeighborSites.put(site, hasi);
                    }
                }
                break;
            case MOTOROLA_CAPMAX_ALOHA:
                if(csbk instanceof CapacityMaxAloha)
                {
                    if(mDMRNetwork == null || mDMRSite == null)
                    {
                        SystemIdentityCode sic = ((CapacityMaxAloha)csbk).getSystemIdentityCode();

                        if(mDMRNetwork == null)
                        {
                            mDMRNetwork = sic.getNetwork();
                        }
                        if(mDMRSite == null)
                        {
                            mDMRSite = sic.getSite();
                        }
                        if(mTier3Model == null)
                        {
                            mTier3Model = sic.getModel();
                        }
                    }

                    if(mBrand == null || mBrand != BRAND_MOTOROLA_CAPACITY_MAX_TIER_3_TRUNKING)
                    {
                        mBrand = BRAND_MOTOROLA_CAPACITY_MAX_TIER_3_TRUNKING;
                    }
                }
                break;
            case MOTOROLA_CONPLUS_NEIGHBOR_REPORT:
                if(mNeighborSites.isEmpty() && csbk instanceof ConnectPlusNeighborReport)
                {
                    ConnectPlusNeighborReport cpnr = (ConnectPlusNeighborReport)csbk;
                    mNeighborSites.addAll(cpnr.getNeighbors());
                }
                if(mBrand == null)
                {
                    mBrand = BRAND_MOTOROLA_CONNECT_PLUS;
                }
                break;
            case MOTOROLA_CONPLUS_VOICE_CHANNEL_USER:
                if(csbk instanceof ConnectPlusVoiceChannelUser)
                {
                    DMRChannel channel = ((ConnectPlusVoiceChannelUser)csbk).getChannel();
                    addDmrChannel(channel);
                }
                if(mBrand == null)
                {
                    mBrand = BRAND_MOTOROLA_CONNECT_PLUS;
                }
                break;
        }

        if(mColorCode == null)
        {
            mColorCode = csbk.getSlotType().getColorCode();
        }
    }

    private void addDmrChannel(DMRChannel dmrChannel)
    {
        for(DMRChannel channel: mObservedDmrChannels)
        {
            if(channel.getValue() == dmrChannel.getValue())
            {
                return;
            }
        }

        mObservedDmrChannels.add(dmrChannel);
    }

    public void reset()
    {
        mColorCode = null;
        mDMRNetwork = null;
        mDMRSite = null;
        mNeighborSites.clear();
        mTier3NeighborSites.clear();
    }

    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary - Decoder:DMR ");
        sb.append("Timeslot: " + mTimeslot);

        //DMR System Brand Name
        sb.append("\n\nBrand:").append((mBrand == null ? BRAND_STANDARD : mBrand));

        ///Connect+ Network ID
        if(mDMRNetwork != null)
        {
            sb.append("\nNetwork:").append(mDMRNetwork);
        }

        //Connect+ Site ID
        if(mDMRSite != null)
        {
            sb.append("\nSite:").append(mDMRSite);
        }
        if(mTier3Model != null)
        {
            sb.append("\nNetwork Model").append(mTier3Model);
        }

        //Observed DMR Channels
        if(!mObservedDmrChannels.isEmpty())
        {
            sb.append("\nObserved DMR Channels:");

            for(DMRChannel dmrChannel: mObservedDmrChannels)
            {
                sb.append("\n\t").append(dmrChannel);
            }
        }

        if(!mNeighborSites.isEmpty())
        {
            sb.append("\nNeighbor Sites\n");

            for(SiteIdentifier neighbor: mNeighborSites)
            {
                sb.append("\tNeighbor: ").append(neighbor).append("\n");
            }

            for(Map.Entry<Integer,AdjacentSiteInformation> neighbor: mTier3NeighborSites.entrySet())
            {
                AdjacentSiteInformation site = neighbor.getValue();
                sb.append("\tNeighbor: ").append("Network:").append(site.getNeighborSystemIdentityCode().getNetwork());
                sb.append(" Site:").append(site.getNeighborSystemIdentityCode().getSite());
                sb.append(" ").append(site.getNeighborSystemIdentityCode().getModel());
                sb.append(" ").append(site.getNeighborChannel()).append("\n");
            }
        }

        sb.append("\n\n");

        return sb.toString();
    }
}
