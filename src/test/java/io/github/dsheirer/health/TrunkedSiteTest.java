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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.ltrnet.DecodeConfigLTRNet;
import io.github.dsheirer.module.decode.ltrstandard.DecodeConfigLTRStandard;
import io.github.dsheirer.module.decode.ltrstandard.message.Idle;
import io.github.dsheirer.module.decode.mpt1327.DecodeConfigMPT1327;
import io.github.dsheirer.module.decode.mpt1327.MPT1327TrafficChannelManagerCallOutcomeTest;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.passport.DecodeConfigPassport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Sites of the decoders that can be nothing but a trunked site's (P25 Phase 2 control channels, MPT1327, LTR, LTR-Net,
 * Passport): reported as sites from the start, deaf or not, with what each decoder measures.
 */
public class TrunkedSiteTest
{
    private static Channel channel(String name, DecodeConfiguration config)
    {
        Channel channel = new Channel(name);
        channel.setDecodeConfiguration(config);
        return channel;
    }

    private static JsonObject report(Channel channel, long now)
    {
        ChannelHealth.Totals totals = HealthRegistry.getInstance().channel(channel).totals(now);
        HealthSnapshot snapshot = new HealthSnapshot(now, now - 600_000, List.of(totals), List.of());
        return NodeReport.build(new NodeReporter.Window(null, snapshot, false, 0), new JsonObject(), "1.0.0",
            info -> info.channelId() == channel.getChannelID());
    }

    private static JsonObject site(Channel channel, long now)
    {
        return report(channel, now).getAsJsonArray("sites").get(0).getAsJsonObject();
    }

    @Test
    void a_deaf_site_is_reported_as_a_site_not_left_out()
    {
        for(DecodeConfiguration config: new DecodeConfiguration[]{new DecodeConfigMPT1327(),
            new DecodeConfigLTRStandard(), new DecodeConfigLTRNet(), new DecodeConfigPassport(),
            new DecodeConfigP25Phase2()})
        {
            Channel channel = channel("Deaf " + config.getDecoderType(), config);
            HealthMonitor monitor = new HealthMonitor(channel);
            HealthRegistry.getInstance().calls(channel); //Its traffic channel manager, where it has one
            monitor.start();
            JsonObject site = site(channel, System.currentTimeMillis() + 300_000);
            assertEquals(0.0, site.getAsJsonObject("cc").get("lockedS").getAsDouble(), config.getDecoderType() + "");
            assertEquals(0, site.getAsJsonObject("calls").get("started").getAsInt());
        }
    }

    @Test
    void a_p25_phase_2_control_channel_counts_mac_messages_and_says_who_it_is()
    {
        Channel channel = channel("TDMA control", new DecodeConfigP25Phase2());
        HealthMonitor monitor = new HealthMonitor(channel);
        HealthRegistry.getInstance().calls(channel);
        monitor.start();
        long t = System.currentTimeMillis();

        CorrectedBinaryMessage bits = new CorrectedBinaryMessage(180);
        bits.load(16, 20, 0xBEE00);
        bits.load(36, 12, 0x3A1);
        bits.load(76, 12, 0x293);

        for(int i = 0; i < 10; i++)
        {
            monitor.getMessageListener().receive(new MacMessage(1, DataUnitID.UNSCRAMBLED_LCCH, bits, t + i * 30L,
                new NetworkStatusBroadcastImplicit(bits, 0)));
        }

        MacMessage failed = new MacMessage(1, DataUnitID.UNSCRAMBLED_LCCH, bits, t + 400, null);
        failed.setValid(false);
        monitor.getMessageListener().receive(failed);

        JsonObject site = site(channel, t + 1_000);
        assertEquals("p25", site.get("protocol").getAsString());
        assertEquals(10, site.getAsJsonObject("cc").get("good").getAsInt());
        assertEquals(1, site.getAsJsonObject("cc").get("bad").getAsInt());
        assertEquals(Map.of("wacn", 0xBEE00, "sysId", 0x3A1, "nac", 0x293),
            HealthRegistry.getInstance().channel(channel).totals(t + 1_000).identity());
    }

    @Test
    void an_mpt1327_control_channel_counts_codewords_and_says_its_system()
    {
        Channel channel = channel("MPT control", new DecodeConfigMPT1327());
        HealthMonitor monitor = new HealthMonitor(channel);
        HealthRegistry.getInstance().calls(channel);
        monitor.start();

        //BCAST (type 284) with system identity code 4372.
        monitor.getMessageListener().receive(MPT1327TrafficChannelManagerCallOutcomeTest.message(
            new int[]{26, 15, 4372}, new int[]{41, 9, 284}));
        monitor.getMessageListener().receive(MPT1327TrafficChannelManagerCallOutcomeTest.message(
            new int[]{28, 13, 77}));

        JsonObject site = site(channel, System.currentTimeMillis() + 1_000);
        assertEquals("mpt1327", site.get("protocol").getAsString());
        assertEquals(2, site.getAsJsonObject("cc").get("good").getAsInt());
        assertEquals(0, site.getAsJsonObject("cc").get("bad").getAsInt(), "measured: none failed");
        assertEquals(4372, site.getAsJsonObject("id").get("sysId").getAsInt());
    }

    @Test
    void an_ltr_channel_counts_the_calls_it_follows_and_leaves_out_what_its_decoder_cant_say()
    {
        Channel channel = channel("LTR repeater 3", new DecodeConfigLTRStandard());
        HealthMonitor monitor = new HealthMonitor(channel);
        monitor.start();
        long now = System.currentTimeMillis();

        for(int i = 0; i < 5; i++)
        {
            monitor.getMessageListener().receive(new Idle(new CorrectedBinaryMessage(40), MessageDirection.OSW,
                CRC.PASSED));
        }

        //A call's event comes again and again as it goes on; another call is another event.
        DecodeEvent first = DecodeEvent.builder(DecodeEventType.CALL, now).build();
        DecodeEvent second = DecodeEvent.builder(DecodeEventType.CALL, now + 5_000).build();
        monitor.getDecodeEventListener().receive(first);
        monitor.getDecodeEventListener().receive(first);
        monitor.getDecodeEventListener().receive(DecodeEvent.builder(DecodeEventType.REGISTER, now).build());
        monitor.getDecodeEventListener().receive(second);
        monitor.getDecodeEventListener().receive(second);

        JsonObject site = site(channel, now + 1_000);
        assertEquals("ltr", site.get("protocol").getAsString());
        JsonObject cc = site.getAsJsonObject("cc");
        assertEquals(5, cc.get("good").getAsInt());
        assertFalse(cc.has("bad"), "the decoder passes on only the data words that decode");
        assertEquals(2, site.getAsJsonObject("calls").get("started").getAsInt());
        assertEquals(2, site.getAsJsonObject("calls").get("recorded").getAsInt());

        //Between transmissions it isn't locked, but it's still on its repeater.
        channel.setSourceConfiguration(new io.github.dsheirer.source.config.SourceConfigTuner());
        HealthRegistry.getInstance().channel(channel).setFrequency(451_012_500);
        assertNotNull(site(channel, now + 60_000).getAsJsonObject("cc").get("hz"));
    }
}
