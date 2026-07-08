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

package io.github.dsheirer.module.decode.nxdn.layer3.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.filter.SyncLossMessageFilter;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;

/**
 * NXDN decoder message filters
 */
public class NXDNMessageFilterSet extends FilterSet<IMessage>
{
    /**
     * Constructs an instance
     */
    public NXDNMessageFilterSet()
    {
        super("NXDN Message Filter");
        addFilter(new NXDNAudioMessageFilter());
        addFilter(new NXDNLayer2MessageFilter());
        addFilter(new NXDNLayer3MessageFilter("Proprietary & Talker Alias", NXDNMessageType.OTHER));
        addFilter(new NXDNLayer3MessageFilter("Outbound Control Broadcast", NXDNMessageType.CONTROL_OUT_BROADCAST));
        addFilter(new NXDNLayer3MessageFilter("Outbound Control Call Control", NXDNMessageType.CONTROL_OUT_CALL_CONTROL));
        addFilter(new NXDNLayer3MessageFilter("Outbound Control Mobility Management", NXDNMessageType.CONTROL_OUT_MOBILITY_MANAGEMENT));
        addFilter(new NXDNLayer3MessageFilter("Outbound Traffic Broadcast", NXDNMessageType.TRAFFIC_OUT_BROADCAST));
        addFilter(new NXDNLayer3MessageFilter("Outbound Traffic Call Control", NXDNMessageType.TRAFFIC_OUT_CALL_CONTROL));
        addFilter(new NXDNLayer3MessageFilter("Outbound Traffic Mobility Management", NXDNMessageType.TRAFFIC_OUT_MOBILITY_MANAGEMENT));
        addFilter(new NXDNLayer3MessageFilter("Outbound Type-D Broadcast", NXDNMessageType.TYPE_D_OUT_BROADCAST));
        addFilter(new NXDNLayer3MessageFilter("Outbound Type-D Call Control", NXDNMessageType.TYPE_D_OUT_CALL_CONTROL));
        addFilter(new NXDNLayer3MessageFilter("Outbound Type-D Mobility Management", NXDNMessageType.TYPE_D_OUT_MOBILITY_MANAGEMENT));
        addFilter(new NXDNLayer3MessageFilter("Outbound Type-D SCCH", NXDNMessageType.TYPE_D_OUT_SCCH));
        addFilter(new NXDNLayer3MessageFilter("Inbound Control Call Control", NXDNMessageType.CONTROL_IN_CALL_CONTROL));
        addFilter(new NXDNLayer3MessageFilter("Inbound Control Mobility Management", NXDNMessageType.CONTROL_IN_MOBILITY_MANAGEMENT));
        addFilter(new NXDNLayer3MessageFilter("Inbound Traffic Call Control", NXDNMessageType.TRAFFIC_IN_CALL_CONTROL));
        addFilter(new NXDNLayer3MessageFilter("Inbound Traffic Mobility Management", NXDNMessageType.TRAFFIC_IN_MOBILITY_MANAGEMENT));
        addFilter(new NXDNLayer3MessageFilter("Inbound Type-D Call Control", NXDNMessageType.TYPE_D_IN_CALL_CONTROL));
        addFilter(new NXDNLayer3MessageFilter("Inbound Type-D Mobility Management", NXDNMessageType.TYPE_D_IN_MOBILITY_MANAGEMENT));
        addFilter(new NXDNLayer3MessageFilter("Inbound Type-D SCCH", NXDNMessageType.TYPE_D_IN_SCCH));
        addFilter(new SyncLossMessageFilter());
    }

    /**
     * Override default to descope handling to NXDN or sync-loss messages.
     * @param message to test
     * @return true if the message can be processed
     */
    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof NXDNMessage || message instanceof SyncLossMessage;
    }
}
