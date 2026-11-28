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

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelConfigurationChangeNotification;
import io.github.dsheirer.module.decode.mpt1327.DecodeConfigMPT1327;
import io.github.dsheirer.module.decode.mpt1327.MPT1327TrafficChannelManagerCallOutcomeTest;
import org.junit.jupiter.api.Test;

public class HealthMonitorTest
{
    /**
     * A DMR Capacity Plus rest channel that moves: the channel's processing chain becomes a traffic channel, keeping its
     * monitor, and a new chain (with a monitor of its own) starts for the channel.  The old monitor stops counting
     * toward the channel when its chain converts, and the channel keeps running on the new one, however the two
     * starts and stops interleave.
     */
    @Test
    void a_chain_that_becomes_a_traffic_channel_stops_counting_toward_the_channel()
    {
        Channel channel = new Channel("Rest channel");
        channel.setDecodeConfiguration(new DecodeConfigMPT1327());
        HealthMonitor old = new HealthMonitor(channel);
        old.start(0);
        old.getMessageListener().receive(message());

        HealthMonitor rest = new HealthMonitor(channel);
        rest.start(5_000); //The new chain starts before the old one converts
        old.channelChanged(new ChannelConfigurationChangeNotification(new Channel("T-Rest channel",
            Channel.ChannelType.TRAFFIC)));
        old.getMessageListener().receive(message()); //The traffic channel's: not the control channel's
        rest.getMessageListener().receive(message());
        old.stop(8_000); //The traffic channel stops

        ChannelHealth.Totals totals = HealthRegistry.getInstance().channel(channel).totals(20_000);
        assertEquals(20_000, totals.runningMs(), "running on the new chain");
        assertEquals(2, totals.good(), "the old chain's message before it converted, and the new chain's");

        rest.stop(30_000);
        assertEquals(30_000, HealthRegistry.getInstance().channel(channel).totals(40_000).runningMs());
    }

    private static io.github.dsheirer.module.decode.mpt1327.MPT1327Message message()
    {
        return MPT1327TrafficChannelManagerCallOutcomeTest.message(new int[]{28, 13, 77});
    }
}
