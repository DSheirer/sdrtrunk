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

    public String getSummary()
    {
        StringBuilder sb = new StringBuilder();

        if(mDigitalStationIDInformation != null)
        {
            sb.append("Current Channel Station ID\n  ").append(mDigitalStationIDInformation).append("\n");
        }
        if(mSiteInformation != null)
        {
            sb.append("\nCurrent Site\n  ").append(mSiteInformation).append("\n");
        }
        else if(mTypeDSiteID != null)
        {
            sb.append("\nCurrent Site\n  ").append(mTypeDSiteID).append("\n");
        }
        if(mServiceInformation != null)
        {
            sb.append("\nCurrent Site Services\n  ").append(mServiceInformation).append("\n");
        }
        if(mFailureStatusInformation != null)
        {
            sb.append("\nFailure Status\n  ").append(mFailureStatusInformation).append("\n");
        }
        if(mControlChannelInformation != null)
        {
            sb.append("\nCurrent Site Control Channel\n  ").append(mControlChannelInformation).append("\n");
        }

        if(mTypeDRepeater != null)
        {
            sb.append("\nType-D Current Repeater: ").append(mTypeDRepeater).append("\n");
        }

        if(!mTypeDObservedRepeaters.isEmpty())
        {
            Collections.sort(mTypeDObservedRepeaters);
            sb.append("\nType-D Observed Repeater Numbers:").append(mTypeDObservedRepeaters).append("\n");
        }

        if(!mNeighborMap.isEmpty())
        {
            sb.append("\nNeighbor Sites\n");
            List<Integer> ids = new ArrayList<>(mNeighborMap.keySet());
            Collections.sort(ids);

            for(Integer id : ids)
            {
                sb.append("  ").append(mNeighborMap.get(id)).append("\n");
            }
        }
        else if(mTypeDNeighborA != null || mTypeDNeighborB != null)
        {
            sb.append("\nNeighbor Sites\n");

            if(mTypeDNeighborA != null)
            {
                sb.append("  ").append(mTypeDNeighborA.getSystemID1()).append(" SITE:").append(mTypeDNeighborA.getSite1()).append("\n");

                if(mTypeDNeighborA.hasSite2())
                {
                    sb.append("  ").append(mTypeDNeighborA.getSystemID2()).append(" SITE:").append(mTypeDNeighborA.getSite2()).append("\n");
                }
            }
            if(mTypeDNeighborB != null)
            {
                sb.append("  ").append(mTypeDNeighborB.getSystemID1()).append(" SITE:").append(mTypeDNeighborB.getSite1()).append("\n");

                if(mTypeDNeighborB.hasSite2())
                {
                    sb.append("  ").append(mTypeDNeighborB.getSystemID2()).append(" SITE:").append(mTypeDNeighborB.getSite2()).append("\n");
                }
            }
        }
        else
        {
            sb.append("\nNeighbor Sites\n  NONE\n");
        }

        return sb.toString();
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
