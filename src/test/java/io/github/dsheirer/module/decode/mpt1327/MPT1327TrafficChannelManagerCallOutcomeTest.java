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

package io.github.dsheirer.module.decode.mpt1327;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.controller.channel.map.ChannelRange;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.edac.CRCFleetsync;
import io.github.dsheirer.health.CallOutcome;
import io.github.dsheirer.health.CallTally;
import io.github.dsheirer.health.HealthRegistry;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.module.decode.mpt1327.identifier.MPT1327Talkgroup;
import org.junit.jupiter.api.Test;

/**
 * What an MPT1327 control channel's calls are counted as, for node health reports: each call once, by what finally
 * became of it.
 */
public class MPT1327TrafficChannelManagerCallOutcomeTest
{
    private static final int BLOCK_1_START = 20;

    /**
     * An MPT1327 message whose first codeword has these bits (positions in the message, from its sync), with a
     * checksum and parity that pass.
     * @param fields pairs of start position and value, each field as wide as its bits
     */
    public static MPT1327Message message(int[]... fields)
    {
        BinaryMessage bits = new BinaryMessage(BLOCK_1_START + 64 * 5);
        bits.set(BLOCK_1_START); //An address codeword

        for(int[] field: fields)
        {
            bits.load(field[0], field[1], field[2]);
        }

        //The codeword's 15-bit checksum and its even parity bit: whichever passes.
        for(int checksum = 0; checksum < (1 << 15); checksum++)
        {
            bits.load(BLOCK_1_START + 48, 15, checksum);
            bits.clear(BLOCK_1_START + 63);

            if(bits.get(BLOCK_1_START, BLOCK_1_START + 64).cardinality() % 2 != 0)
            {
                bits.set(BLOCK_1_START + 63);
            }

            if(CRCFleetsync.check(bits.get(BLOCK_1_START, BLOCK_1_START + 64)) == CRC.PASSED)
            {
                return new MPT1327Message(bits);
            }
        }

        throw new IllegalStateException("no checksum passes");
    }

    /**
     * A Go To Channel to a talkgroup.
     */
    static MPT1327Message goToChannel(int channel, int ident)
    {
        //Prefix 21-27, ident 1 (the talkgroup) 28-40, message type 41-49 under 256 (GTC), channel 43-52.
        return message(new int[]{21, 7, 1}, new int[]{28, 13, ident}, new int[]{41, 2, 0}, new int[]{43, 10, channel});
    }

    /**
     * Stands in for the channel processing manager: starts each traffic channel asked for, or, with no tuner, rejects
     * the request while it's made, as ChannelProcessingManager does when no tuner covers the frequency.
     */
    private static class Starter
    {
        private MPT1327TrafficChannelManager mManager;
        private boolean mNoTuner;
        private int mRequests;

        @Subscribe
        public void start(ChannelStartProcessingRequest request)
        {
            mRequests++;

            if(mNoTuner)
            {
                mManager.getChannelEventListener().receive(new ChannelEvent(request.getChannel(),
                    ChannelEvent.Event.NOTIFICATION_PROCESSING_START_REJECTED, "TUNER UNAVAILABLE"));
            }
        }
    }

    private record Site(MPT1327TrafficChannelManager manager, Starter starter, CallTally calls)
    {
        void grant(int channel, int ident)
        {
            MPT1327Message gtc = goToChannel(channel, ident);
            assertTrue(gtc.isValid());
            assertEquals(MPT1327Message.MPTMessageType.GTC, gtc.getMessageType());
            MutableIdentifierCollection identifiers = new MutableIdentifierCollection();
            identifiers.update(MPT1327Talkgroup.createTo(1, ident));
            manager.processChannelGrant(gtc, identifiers);
        }
    }

    /**
     * A site whose channel map gives channels 1 to 100 frequencies.
     */
    private static Site site(String name, int trafficChannels)
    {
        Channel parent = new Channel(name);
        DecodeConfigMPT1327 config = new DecodeConfigMPT1327();
        config.setTrafficChannelPoolSize(trafficChannels);
        parent.setDecodeConfiguration(config);
        ChannelMap map = new ChannelMap("Test");
        map.addRange(new ChannelRange(1, 100, 165_000_000, 12_500));
        MPT1327TrafficChannelManager manager = new MPT1327TrafficChannelManager(parent, map);
        Starter starter = new Starter();
        starter.mManager = manager;
        EventBus eventBus = new EventBus();
        eventBus.register(starter);
        manager.setInterModuleEventBus(eventBus);
        return new Site(manager, starter, HealthRegistry.getInstance().calls(parent));
    }

    @Test
    void a_followed_call_is_counted_once_however_many_grants_it_has()
    {
        Site site = site("MPT followed", 2);
        site.grant(10, 501);
        site.grant(10, 501);

        assertEquals(1, site.starter().mRequests);
        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.RECORDED));

        //Someone else on the channel: a new call.
        site.grant(10, 502);
        assertEquals(2, site.calls().getStarted());
        assertEquals(2, site.calls().get(CallOutcome.RECORDED));
    }

    @Test
    void a_call_no_tuner_can_take_is_counted_as_no_source_not_recorded()
    {
        Site site = site("MPT no tuner", 2);
        site.starter().mNoTuner = true;
        site.grant(10, 501);

        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.NO_SOURCE), "rejected while it was requested");
        assertEquals(0, site.calls().get(CallOutcome.RECORDED));
    }

    @Test
    void a_call_with_every_traffic_channel_busy_is_no_recorder()
    {
        Site site = site("MPT busy", 1);
        site.grant(10, 501);
        site.grant(11, 502);

        assertEquals(2, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.RECORDED));
        assertEquals(1, site.calls().get(CallOutcome.NO_RECORDER));
    }

    @Test
    void a_call_on_a_channel_the_map_has_no_frequency_for_is_no_frequency()
    {
        Site site = site("MPT unmapped", 2);
        site.grant(500, 501);

        assertEquals(0, site.starter().mRequests);
        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.NO_FREQUENCY));
    }
}
