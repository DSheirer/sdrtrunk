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

import com.google.common.base.Joiner;
import io.github.dsheirer.identifier.site.SiteIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRNetwork;
import io.github.dsheirer.module.decode.dmr.identifier.DMRSite;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusNeighborReport;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola.ConnectPlusVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ConnectPlusControlChannel;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ConnectPlusTrafficChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks the network configuration details of a DMR network from the broadcast messages
 */
public class DMRNetworkConfigurationMonitor
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRNetworkConfigurationMonitor.class);
    private static final String BRAND_HYTERA = "Hytera";
    private static final String BRAND_MOTOROLA_CAPACITY_PLUS = "Motorola Capacity+";
    private static final String BRAND_MOTOROLA_CONNECT_PLUS = "Motorola Connect+";
    private static final String BRAND_MOTOROLA_IP_SITE_CONNECT = "Motorola IP Site Connect";
    private static final String BRAND_STANDARD = "Standard/Unknown";
    private static final String BRAND_TIER_3_TRUNKING = "Tier III Trunking";

    private List<SiteIdentifier> mNeighborSites = new ArrayList<>();
    private List<Integer> mLogicalSlotNumbers = new ArrayList<>();
    private DMRNetwork mDMRNetwork;
    private DMRSite mDMRSite;
    private String mBrand;
    private Integer mColorCode;
    private Integer mCurrentLSN;
    private int mTimeslot;

    /**
     * Constructs a network configuration monitor.
     *
     */
    public DMRNetworkConfigurationMonitor(int timeslot)
    {
        mTimeslot = timeslot;
    }

    /**
     * Sets the current logical slot number for a Motorola Connect+ channel
     */
    public void setCurrentLogicalSlotNumber(int lsn)
    {
        mCurrentLSN = lsn;
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
                    int lsn = ((ConnectPlusVoiceChannelUser)csbk).getLogicalSlotNumber();

                    if(!mLogicalSlotNumbers.contains(lsn))
                    {
                        mLogicalSlotNumbers.add(lsn);
                    }
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

    public void reset()
    {
        mNeighborSites.clear();
        mDMRNetwork = null;
        mDMRSite = null;
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

        //Connect+ Logical Slot Numbers
        if(!mLogicalSlotNumbers.isEmpty())
        {
            Collections.sort(mLogicalSlotNumbers);
            sb.append("\nObserved LSNs:").append(Joiner.on(",").join(mLogicalSlotNumbers));
        }

        //Connect+ current LSN
        if(mCurrentLSN != null)
        {
            sb.append("\nCurrent LSN:").append(mCurrentLSN);
        }

        if(!mNeighborSites.isEmpty())
        {
            for(SiteIdentifier neighbor: mNeighborSites)
            {
                sb.append("\nNeighbor:").append(neighbor);
            }
        }

        sb.append("\n\n");

        return sb.toString();
    }
}
