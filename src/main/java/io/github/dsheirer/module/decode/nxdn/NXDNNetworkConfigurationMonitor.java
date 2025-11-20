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
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.ControlChannelInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.DigitalStationIDInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.FailureStatusInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.Neighbor;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.ServiceInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.SiteInformation;
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

    public NXDNNetworkConfigurationMonitor()
    {
    }

    public String getSummary()
    {
        StringBuilder sb = new StringBuilder();

        if(mDigitalStationIDInformation != null)
        {
            sb.append("CHANNEL STATION ID\n\t").append(mDigitalStationIDInformation).append("\n");
        }
        if(mServiceInformation != null)
        {
            sb.append("\nSERVICE\n  ").append(mServiceInformation).append("\n");
        }
        if(mSiteInformation != null)
        {
            sb.append("\nSITE\n  ").append(mSiteInformation).append("\n");
        }
        if(mFailureStatusInformation != null)
        {
            sb.append("\nFAILURE STATUS\n  ").append(mFailureStatusInformation).append("\n");
        }
        if(mControlChannelInformation != null)
        {
            sb.append("\nCONTROL\n  ").append(mControlChannelInformation).append("\n");
        }

        if(!mNeighborMap.isEmpty())
        {
            sb.append("\nNEIGHBORS\n");
            List<Integer> ids = new ArrayList<>(mNeighborMap.keySet());
            Collections.sort(ids);

            for(Integer id : ids)
            {
                sb.append("  ").append(mNeighborMap.get(id)).append("\n");
            }
        }
        else
        {
            sb.append("\nNEIGHBORS\n  NONE\n");
        }

        return sb.toString();
    }

    public void process(NXDNLayer3Message layer3)
    {
        switch(layer3.getMessageType())
        {
            case CONTROL_OUT_23_DIGITAL_STATION_ID_INFORMATION:
            case TRAFFIC_OUT_23_DIGITAL_STATION_ID_INFORMATION:
                mDigitalStationIDInformation = (DigitalStationIDInformation) layer3;
                break;
            case CONTROL_OUT_24_SITE_INFORMATION:
            case TRAFFIC_OUT_24_SITE_INFORMATION:
                mSiteInformation = (SiteInformation) layer3;
                break;
            case CONTROL_OUT_25_SERVICE_INFORMATION:
            case TRAFFIC_OUT_25_SERVICE_INFORMATION:
                mServiceInformation = (ServiceInformation)layer3;
                break;
            case CONTROL_OUT_26_CONTROL_CHANNEL_INFORMATION:
            case TRAFFIC_OUT_26_CONTROL_CHANNEL_INFORMATION:
                mControlChannelInformation = (ControlChannelInformation) layer3;
                break;
            case CONTROL_OUT_28_FAILURE_STATUS_INFORMATION:
            case TRAFFIC_OUT_28_FAILURE_STATUS_INFORMATION:
                mFailureStatusInformation = (FailureStatusInformation) layer3;
                break;
            case CONTROL_OUT_27_ADJACENT_SITE_INFORMATION:
            case TRAFFIC_OUT_27_ADJACENT_SITE_INFORMATION:
                AdjacentSiteInformation adjacent = (AdjacentSiteInformation) layer3;

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
                break;
        }
    }
}
