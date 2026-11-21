/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.module.decode.event.ActivitySummary;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.AdjacentSiteInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.AdjacentSiteInformationTypeD;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.ControlChannelInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.DigitalStationIDInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.FailureStatusInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.Neighbor;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.ServiceInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.SiteInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.scch.RepeaterIdle;
import io.github.dsheirer.module.decode.nxdn.layer3.scch.SiteID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Processes NXDN Layer 3 messages to assemble a snapshot of the site's broadcast configuration details
 */
public class NXDNNetworkConfigurationMonitor
{
    private ControlChannelInformation mControlChannelInformation;
    private DigitalStationIDInformation mDigitalStationIDInformation;
    private FailureStatusInformation mFailureStatusInformation;
    private ServiceInformation mServiceInformation;
    private SiteInformation mSiteInformation;
    private Map<Integer, Neighbor> mNeighborMap = new HashMap<>();

    private AdjacentSiteInformationTypeD mTypeDNeighborA;
    private AdjacentSiteInformationTypeD mTypeDNeighborB;
    private Integer mTypeDRepeater;
    private List<Integer> mTypeDObservedRepeaters = new ArrayList<>();
    private SiteID mTypeDSiteID;

    /**
     * Constructs an instance
     */
    public NXDNNetworkConfigurationMonitor()
    {
    }

    /**
     * Adds the network configuration sections to the activity summary.
     * @param summary to add to
     */
    public void addActivitySummary(ActivitySummary summary)
    {
        summary.section("Current Site");

        if(mSiteInformation != null)
        {
            summary.field("Site", mSiteInformation);
        }
        else if(mTypeDSiteID != null)
        {
            summary.field("Site", mTypeDSiteID);
        }
        else
        {
            summary.field("Site", ActivitySummary.UNKNOWN);
        }

        if(mDigitalStationIDInformation != null)
        {
            summary.field("Station ID", mDigitalStationIDInformation);
        }
        if(mServiceInformation != null)
        {
            summary.field("Services", mServiceInformation);
        }
        if(mControlChannelInformation != null)
        {
            summary.field("Control Channel", mControlChannelInformation);
        }
        if(mFailureStatusInformation != null)
        {
            summary.field("Failure Status", mFailureStatusInformation);
        }
        if(mTypeDRepeater != null)
        {
            summary.field("Type-D Current Repeater", mTypeDRepeater);
        }
        if(!mTypeDObservedRepeaters.isEmpty())
        {
            Collections.sort(mTypeDObservedRepeaters);
            summary.field("Type-D Observed Repeaters", mTypeDObservedRepeaters.stream().map(String::valueOf)
                .collect(Collectors.joining(", ")));
        }

        summary.section("Neighbor Sites");

        if(!mNeighborMap.isEmpty())
        {
            List<Integer> ids = new ArrayList<>(mNeighborMap.keySet());
            Collections.sort(ids);

            for(Integer id : ids)
            {
                summary.text(mNeighborMap.get(id));
            }
        }
        else if(mTypeDNeighborA != null || mTypeDNeighborB != null)
        {
            ActivitySummary.Table table = summary.table("System", "Site");

            if(mTypeDNeighborA != null)
            {
                table.row(mTypeDNeighborA.getSystemID1(), mTypeDNeighborA.getSite1());

                if(mTypeDNeighborA.hasSite2())
                {
                    table.row(mTypeDNeighborA.getSystemID2(), mTypeDNeighborA.getSite2());
                }
            }
            if(mTypeDNeighborB != null)
            {
                table.row(mTypeDNeighborB.getSystemID1(), mTypeDNeighborB.getSite1());

                if(mTypeDNeighborB.hasSite2())
                {
                    table.row(mTypeDNeighborB.getSystemID2(), mTypeDNeighborB.getSite2());
                }
            }
        }
    }

    public void process(NXDNLayer3Message layer3)
    {
        switch(layer3.getMessageType())
        {
            case CONTROL_OUT_23_BC_DIGITAL_STATION_ID_INFORMATION:
            case TRAFFIC_OUT_23_BC_DIGITAL_STATION_ID_INFORMATION:
            case TYPE_D_OUT_23_BC_DIGITAL_STATION_ID:
                if(layer3 instanceof DigitalStationIDInformation dsii)
                {
                    mDigitalStationIDInformation = dsii;
                }
                break;
            case CONTROL_OUT_24_BC_SITE_INFORMATION:
            case TRAFFIC_OUT_24_BC_SITE_INFORMATION:
                mSiteInformation = (SiteInformation) layer3;
                break;
            case CONTROL_OUT_25_BC_SERVICE_INFORMATION:
            case TRAFFIC_OUT_25_BC_SERVICE_INFORMATION:
            case TYPE_D_OUT_25_BC_SERVICE_INFORMATION:
                if(layer3 instanceof ServiceInformation sii)
                {
                    mServiceInformation = sii;
                }
                break;
            case CONTROL_OUT_26_BC_CONTROL_CHANNEL_INFORMATION:
            case TRAFFIC_OUT_26_BC_CONTROL_CHANNEL_INFORMATION:
                mControlChannelInformation = (ControlChannelInformation) layer3;
                break;
            case CONTROL_OUT_28_BC_FAILURE_STATUS_INFORMATION:
            case TRAFFIC_OUT_28_BC_FAILURE_STATUS_INFORMATION:
                mFailureStatusInformation = (FailureStatusInformation) layer3;
                break;
            case CONTROL_OUT_27_BC_ADJACENT_SITE_INFORMATION:
            case TRAFFIC_OUT_27_BC_ADJACENT_SITE_INFORMATION:
                if(layer3 instanceof AdjacentSiteInformation adjacent)
                {
                    if(adjacent.hasChannel1())
                    {
                        Neighbor n1 = adjacent.getNeighbor1();

                        if(n1 != null)
                        {
                            mNeighborMap.put(n1.id(), n1);

                            if(adjacent.hasChannel2())
                            {
                                Neighbor n2 = adjacent.getNeighbor2();

                                if(n2 != null)
                                {
                                    mNeighborMap.put(n2.id(), n2);

                                    if(adjacent.hasChannel3())
                                    {
                                        Neighbor n3 = adjacent.getNeighbor3();

                                        if(n3 != null)
                                        {
                                            mNeighborMap.put(n3.id(), n3);

                                            if(adjacent.hasChannel4())
                                            {
                                                Neighbor n4 = adjacent.getNeighbor4();

                                                if(n4 != null)
                                                {
                                                    mNeighborMap.put(n4.id(), n4);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            case TYPE_D_OUT_27_BC_ADJACENT_SITE_INFORMATION:
                if(layer3 instanceof AdjacentSiteInformationTypeD atd)
                {
                    if(atd.isIndex())
                    {
                        mTypeDNeighborB = atd;
                    }
                    else
                    {
                        mTypeDNeighborA = atd;
                    }
                }
                break;
            case TYPE_D_SCCH_OUT_INFO_4_REPEATER_IDLE:
                if(layer3 instanceof RepeaterIdle ri)
                {
                    mTypeDRepeater = ri.getRepeater();

                    if(!mTypeDObservedRepeaters.contains(ri.getRepeater2()))
                    {
                        mTypeDObservedRepeaters.add(ri.getRepeater2());
                    }
                }
                break;
            case TYPE_D_SCCH_OUT_INFO_4_SITE_ID:
        }
    }
}
