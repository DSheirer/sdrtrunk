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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.mpt1327.DecodeConfigMPT1327;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.source.config.SourceConfigTuner;
import java.util.List;
import org.junit.jupiter.api.Test;

public class HealthRegistryTest
{
    /**
     * A channel's name, site and alias list are as they are now, not as they were when it was first seen: moving it
     * to an alias list that streams to the node puts it in the node's reports from the next one.
     */
    @Test
    void edits_to_a_channel_reach_its_reports()
    {
        Channel channel = new Channel("Fire dispatch");
        channel.setDecodeConfiguration(new DecodeConfigNBFM());
        channel.setAliasListName("Unstreamed");
        ChannelHealth health = HealthRegistry.getInstance().channel(channel);
        assertEquals("Unstreamed", health.totals(0).info().aliasListName());

        channel.setAliasListName("Broadcastify");
        channel.setName("Fire dispatch 1");
        channel.setSite("North");
        ChannelHealth.ChannelInfo info = health.totals(0).info();
        assertEquals("Broadcastify", info.aliasListName());
        assertEquals("Fire dispatch 1", info.name());
        assertEquals("North", info.site());
        assertEquals("fm", info.mode());
        assertSame(health, HealthRegistry.getInstance().channel(channel), "the same decoder: the same totals");
    }

    /**
     * A channel switched from MPT1327 to NBFM, when it starts again, is a conventional channel with new totals: not a
     * site with no calls, and not reported with what it measured as an MPT1327 control channel.
     */
    @Test
    void a_channel_whose_decoder_changed_starts_again_as_what_it_is_now()
    {
        Channel channel = new Channel("Was MPT1327");
        channel.setDecodeConfiguration(new DecodeConfigMPT1327());
        SourceConfigTuner source = new SourceConfigTuner();
        source.setFrequency(155_000_000);
        channel.setSourceConfiguration(source);
        HealthMonitor mpt = new HealthMonitor(channel);
        mpt.start(0);
        mpt.getMessageListener().receive(io.github.dsheirer.module.decode.mpt1327
            .MPT1327TrafficChannelManagerCallOutcomeTest.message(new int[]{28, 13, 77}));
        mpt.stop(60_000);
        ChannelHealth.Totals before = HealthRegistry.getInstance().channel(channel).totals(60_000);
        assertTrue(before.isSite());

        channel.setDecodeConfiguration(new DecodeConfigNBFM());
        HealthMonitor fm = new HealthMonitor(channel);
        ChannelHealth health = HealthRegistry.getInstance().channel(channel);
        assertEquals(before.generation() + 1, health.getGeneration());
        fm.start(100_000);
        ChannelHealth.Totals after = health.totals(400_000);

        assertFalse(after.isSite(), "not a site any more");
        assertNull(after.protocol());
        assertEquals(0, after.good());
        assertEquals("fm", after.info().mode());

        //A window from the MPT1327 totals to the NBFM ones: the channel, its 300 s as one, nothing taken from the other.
        JsonObject report = NodeReport.build(new NodeReporter.Window(new HealthSnapshot(60_000, 0, List.of(before),
            List.of()), new HealthSnapshot(400_000, 0, List.of(after), List.of()), false, 1), new JsonObject(), "1.0.0",
            i -> i.channelId() == channel.getChannelID());
        assertFalse(report.has("sites"));
        JsonObject reported = report.getAsJsonArray("channels").get(0).getAsJsonObject();
        assertEquals("fm", reported.get("mode").getAsString());
        assertEquals(300.0, reported.get("heardS").getAsDouble());
    }
}
