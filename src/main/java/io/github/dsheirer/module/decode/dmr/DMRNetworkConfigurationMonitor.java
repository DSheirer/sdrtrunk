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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.site.SiteIdentifier;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.event.ActivitySummary;
import io.github.dsheirer.module.decode.dmr.identifier.DMRNetwork;
import io.github.dsheirer.module.decode.dmr.identifier.DMRSite;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
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
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ControlChannelSystemParameters;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.TrafficChannelSystemParameters;
import io.github.dsheirer.module.decode.dmr.message.type.Model;
import io.github.dsheirer.module.decode.dmr.message.type.SystemIdentityCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String MODE_CAPACITY_MAX_OPEN_SYSTEM = "Open System";
    private static final String MODE_CAPACITY_MAX_ADVANTAGE = "Advantage";
    private static final String CHANNEL_TYPE_CONTROL = "Control";
    private static final String CHANNEL_TYPE_TRAFFIC = "Traffic";
    private static final String UNKNOWN = "Unknown";

    private List<SiteIdentifier> mNeighborSites = new ArrayList<>();
    private Map<Integer,AdjacentSiteInformation> mTier3NeighborSites = new HashMap<>();
    private Map<Integer,DMRChannel> mObservedChannelMap = new HashMap<>();
    private DMRNetwork mDMRNetwork;
    private DMRSite mDMRSite;
    private Model mTier3Model;
    private String mBrand;
    private String mMode;
    private String mChannelType;
    private Integer mColorCodeTS1;
    private Integer mColorCodeTS2;
    private DMRChannel mCurrentChannel;
    private Channel mChannel;

    /**
     * Constructs an instance
     * @param channel configuration
     */
    public DMRNetworkConfigurationMonitor(Channel channel)
    {
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
     * Process a DMR message
     * @param message to process that has already been checked for isValid()
     */
    public void process(DMRMessage message)
    {
        if(message instanceof CSBKMessage csbk)
        {
            process(csbk);
        }
        else if(message instanceof LCMessage lc)
        {
            process(lc);
        }

        if(message instanceof DataMessage dm)
        {
            process(dm);
        }
    }

    /**
     * Processes data messages to capture the color code for each timeslot.
     * @param dm data message
     */
    public void process(DataMessage dm)
    {
        if(dm.getTimeslot() == 1)
        {
            mColorCodeTS1 = dm.getSlotType().getColorCode();
        }
        else if(dm.getTimeslot() == 2)
        {
            mColorCodeTS2 = dm.getSlotType().getColorCode();
        }
    }

    /**
     * Processes link control messages
     */
    public void process(LCMessage linkControl)
    {
        switch(linkControl.getOpcode())
        {
            case FULL_CAPACITY_MAX_GROUP_VOICE_CHANNEL_USER:
            case FULL_CAPACITY_MAX_TALKER_ALIAS_HEADER:
            case FULL_CAPACITY_MAX_TALKER_ALIAS_BLOCK_1:
            case FULL_CAPACITY_MAX_TALKER_ALIAS_BLOCK_2:
            case FULL_CAPACITY_MAX_TALKER_ALIAS_BLOCK_3:
                mBrand = BRAND_MOTOROLA_CAPACITY_MAX_TIER_3_TRUNKING;
                break;

            case SHORT_CONNECT_PLUS_CONTROL_CHANNEL:
                if(linkControl instanceof ConnectPlusControlChannel cpcc)
                {
                    mDMRNetwork = cpcc.getNetwork();
                    mDMRSite = cpcc.getSite();
                    mChannelType = CHANNEL_TYPE_CONTROL;
                    mBrand = BRAND_MOTOROLA_CONNECT_PLUS;
                }
                break;
            case SHORT_CONNECT_PLUS_TRAFFIC_CHANNEL:
                if(linkControl instanceof ConnectPlusTrafficChannel cptc)
                {
                    mDMRNetwork = cptc.getNetwork();
                    mDMRSite = cptc.getSite();
                    mChannelType = CHANNEL_TYPE_TRAFFIC;
                    mBrand = BRAND_MOTOROLA_CONNECT_PLUS;
                }
                break;
            case SHORT_STANDARD_CONTROL_CHANNEL_SYSTEM_PARAMETERS:
                if(linkControl instanceof ControlChannelSystemParameters cc)
                {
                    SystemIdentityCode sic = cc.getSystemIdentityCode();
                    mTier3Model = sic.getModel();
                    mDMRNetwork = sic.getNetwork();
                    mDMRSite = sic.getSite();
                    mChannelType = CHANNEL_TYPE_CONTROL;

                    if(mBrand == null)
                    {
                        mBrand = BRAND_TIER_3_TRUNKING;
                    }
                }
                break;
            case SHORT_STANDARD_TRAFFIC_CHANNEL_SYSTEM_PARAMETERS:
                if(linkControl instanceof TrafficChannelSystemParameters tc)
                {
                    SystemIdentityCode sic = tc.getSystemIdentityCode();
                    mTier3Model = sic.getModel();
                    mDMRNetwork = sic.getNetwork();
                    mDMRSite = sic.getSite();
                    mChannelType = CHANNEL_TYPE_TRAFFIC;
                }

                if(mBrand == null)
                {
                    mBrand = BRAND_TIER_3_TRUNKING;
                }
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
            case STANDARD_ANNOUNCEMENT:
                if(csbk instanceof AdjacentSiteInformation neighbor)
                {
                    mTier3NeighborSites.put(neighbor.getNeighborSystemIdentityCode().getSite().getValue(), neighbor);
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

                    mBrand = BRAND_HYTERA_TIER_3_TRUNKING;
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

                    mChannelType = CHANNEL_TYPE_CONTROL;
                    mBrand = BRAND_MOTOROLA_CAPACITY_MAX_TIER_3_TRUNKING;
                }
                break;
            case MOTOROLA_CAPMAX_CHANNEL_UPDATE_ADVANTAGE_MODE:
                mBrand = BRAND_MOTOROLA_CAPACITY_MAX_TIER_3_TRUNKING;
                mMode = MODE_CAPACITY_MAX_ADVANTAGE;
                break;
            case MOTOROLA_CAPMAX_CHANNEL_UPDATE_OPEN_MODE:
                mBrand = BRAND_MOTOROLA_CAPACITY_MAX_TIER_3_TRUNKING;
                mMode = MODE_CAPACITY_MAX_OPEN_SYSTEM;
                break;
            case MOTOROLA_CONPLUS_NEIGHBOR_REPORT:
                if(mNeighborSites.isEmpty() && csbk instanceof ConnectPlusNeighborReport)
                {
                    ConnectPlusNeighborReport cpnr = (ConnectPlusNeighborReport)csbk;
                    mNeighborSites.addAll(cpnr.getNeighbors());
                }
                mBrand = BRAND_MOTOROLA_CONNECT_PLUS;
                break;
            case MOTOROLA_CONPLUS_VOICE_CHANNEL_USER:
                if(csbk instanceof ConnectPlusVoiceChannelUser)
                {
                    DMRChannel channel = ((ConnectPlusVoiceChannelUser)csbk).getChannel();
                    addDmrChannel(channel);
                }
                mBrand = BRAND_MOTOROLA_CONNECT_PLUS;
                break;
        }
    }

    /**
     * Adds the DMR channel to the observed channel map
     */
    private void addDmrChannel(DMRChannel dmrChannel)
    {
        mObservedChannelMap.put(dmrChannel.getValue(), dmrChannel);
    }

    /**
     * Adds the network configuration sections to the activity summary.
     * @param summary to add to
     */
    public void addActivitySummary(ActivitySummary summary)
    {
        summary.section("Network");
        summary.field("Brand", mBrand == null ? BRAND_STANDARD : mBrand);

        if(mMode != null)
        {
            summary.field("Configured Mode", mMode);
        }

        summary.field("Channel Type", mChannelType != null ? mChannelType : UNKNOWN);
        summary.field("Color Code TS1", mColorCodeTS1 != null ? mColorCodeTS1 : UNKNOWN);
        summary.field("Color Code TS2", mColorCodeTS2 != null ? mColorCodeTS2 : UNKNOWN);

        if(mDMRNetwork != null)
        {
            summary.field("Network", mDMRNetwork);
        }

        if(mDMRSite != null)
        {
            summary.field("Site", mDMRSite);
        }

        if(mTier3Model != null)
        {
            summary.field("Network Model", mTier3Model);
        }

        if(!mObservedChannelMap.isEmpty())
        {
            summary.section("Observed Logical Slot Numbers (LSN)");
            ActivitySummary.Table table = summary.table("LSN", "Channel", "Timeslot", "Downlink");
            List<Integer> lsns = new ArrayList<>(mObservedChannelMap.keySet());
            Collections.sort(lsns);

            for(Integer lsn: lsns)
            {
                DMRChannel channel = mObservedChannelMap.get(lsn);

                if(channel != null)
                {
                    table.row(lsn, channel.getChannelNumber(), channel.getTimeslot(),
                        ActivitySummary.mhz(channel.getDownlinkFrequency()));
                }
            }
        }

        if(!mNeighborSites.isEmpty())
        {
            summary.section("Neighbor Sites");

            for(SiteIdentifier neighbor: mNeighborSites)
            {
                summary.text(neighbor);
            }
        }

        if(!mTier3NeighborSites.isEmpty())
        {
            summary.section("Tier III Neighbor Sites");
            ActivitySummary.Table table = summary.table("Network", "Site", "Model", "Channel", "Downlink");

            for(AdjacentSiteInformation site: mTier3NeighborSites.values())
            {
                SystemIdentityCode code = site.getNeighborSystemIdentityCode();
                DMRChannel channel = site.getNeighborChannel();
                table.row(code.getNetwork(), code.getSite(), code.getModel(), channel,
                    channel != null ? ActivitySummary.mhz(channel.getDownlinkFrequency()) : null);
            }
        }
    }
}
