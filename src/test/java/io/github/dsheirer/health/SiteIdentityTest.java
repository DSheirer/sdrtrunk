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

package io.github.dsheirer.health;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.dmr.DMRNetworkConfigurationMonitor;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ControlChannelSystemParameters;
import io.github.dsheirer.module.decode.nxdn.DecodeConfigNXDN;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.SiteInformation;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A DMR or NXDN site's identity in node health reports: what its control channel says it is.
 */
public class SiteIdentityTest
{
    /**
     * A Tier III control channel's system parameters: network 257, site 5.
     */
    private static ControlChannelSystemParameters tier3(long timestamp)
    {
        CorrectedBinaryMessage bits = new CorrectedBinaryMessage(32);
        bits.load(0, 4, 2);
        bits.load(6, 9, 257);
        bits.load(15, 3, 5);
        return new ControlChannelSystemParameters(bits, timestamp, 1);
    }

    @Test
    void a_dmr_site_says_its_network_and_site()
    {
        Channel channel = new Channel("Tier III control");
        new DMRNetworkConfigurationMonitor(channel).process(tier3(1_000));
        assertEquals(Map.of("network", 257, "site", 5),
            HealthRegistry.getInstance().channel(channel).totals(1_000).identity());
    }

    @Test
    void a_traffic_channel_gives_no_identity()
    {
        Channel traffic = new Channel("Traffic", Channel.ChannelType.TRAFFIC);
        new DMRNetworkConfigurationMonitor(traffic).process(tier3(1_000));
        assertEquals(Map.of(), HealthRegistry.getInstance().channel(traffic).totals(1_000).identity(),
            "its site's control channel gives it");
    }

    @Test
    void an_nxdn_site_says_its_system_site_and_ran()
    {
        //Site information on a control channel: a global location (system 410, site 3), RAN 17.
        CorrectedBinaryMessage bits = new CorrectedBinaryMessage(176);
        bits.load(8, 2, 0);
        bits.load(10, 10, 410);
        bits.load(20, 12, 3);
        SiteInformation site = new SiteInformation(bits, 1_000, NXDNMessageType.CONTROL_OUT_24_BC_SITE_INFORMATION, 17,
            LICH.RCCH_OUTBOUND_SINGLE_CAC_NORMAL);
        site.setValid(true);

        Channel channel = new Channel("NXDN control");
        channel.setDecodeConfiguration(new DecodeConfigNXDN());
        HealthMonitor monitor = new HealthMonitor(channel);
        monitor.getMessageListener().receive(site);

        ChannelHealth.Totals totals = HealthRegistry.getInstance().channel(channel).totals(1_000);
        assertEquals(Map.of("system", 410, "site", 3, "ran", 17), totals.identity());
        assertEquals("nxdn", totals.protocol(), "and it's a control channel frame");
    }
}
