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

package io.github.dsheirer.module.decode.dmr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.health.CallOutcome;
import io.github.dsheirer.health.CallTally;
import io.github.dsheirer.health.HealthRegistry;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.module.decode.dmr.channel.DMRAbsoluteChannel;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.Opcode;
import org.junit.jupiter.api.Test;

/**
 * What a DMR control channel's calls are counted as, for node health reports: each call once, by what finally became
 * of it.
 */
class DMRTrafficChannelManagerCallOutcomeTest
{
    private static final long FREQUENCY_1 = 451_012_500L;
    private static final long FREQUENCY_2 = 451_037_500L;

    /**
     * Stands in for the channel processing manager: starts each traffic channel asked for, or, with no tuner, rejects
     * the request while it's made, as ChannelProcessingManager does when no tuner covers the frequency.
     */
    private static class Starter
    {
        private DMRTrafficChannelManager mManager;
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

    private record Site(DMRTrafficChannelManager manager, Starter starter, CallTally calls)
    {
        void grant(int number, int timeslot, long frequency, int talkgroup, long timestamp)
        {
            DMRAbsoluteChannel channel = new DMRAbsoluteChannel(number, timeslot, frequency, 0);
            MutableIdentifierCollection identifiers = new MutableIdentifierCollection();
            identifiers.update(DMRRadio.createFrom(1000 + talkgroup));
            identifiers.update(DMRTalkgroup.create(talkgroup));
            manager.processChannelGrant(channel, identifiers, Opcode.STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT,
                timestamp, false);
        }
    }

    private static Site site(String name, int trafficChannels)
    {
        Channel parent = new Channel(name);
        DecodeConfigDMR config = new DecodeConfigDMR();
        config.setTrafficChannelPoolSize(trafficChannels);
        parent.setDecodeConfiguration(config);
        DMRTrafficChannelManager manager = new DMRTrafficChannelManager(parent);
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
        Site site = site("DMR followed", 2);
        site.grant(1, 1, FREQUENCY_1, 91, 1_000);
        site.grant(1, 1, FREQUENCY_1, 91, 1_500);
        site.grant(1, 1, FREQUENCY_1, 91, 2_000);

        assertEquals(1, site.starter().mRequests);
        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.RECORDED));

        //A call on the other timeslot of the running traffic channel: followed by it.
        site.grant(1, 2, FREQUENCY_1, 92, 2_100);
        assertEquals(1, site.starter().mRequests);
        assertEquals(2, site.calls().getStarted());
        assertEquals(2, site.calls().get(CallOutcome.RECORDED));
    }

    @Test
    void a_call_no_tuner_can_take_is_counted_as_no_source_not_recorded()
    {
        Site site = site("DMR no tuner", 2);
        site.starter().mNoTuner = true;
        site.grant(1, 1, FREQUENCY_1, 91, 1_000);

        assertEquals(1, site.starter().mRequests);
        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.NO_SOURCE), "rejected while it was requested");
        assertEquals(0, site.calls().get(CallOutcome.RECORDED));
    }

    @Test
    void a_call_with_every_traffic_channel_busy_is_no_recorder()
    {
        Site site = site("DMR busy", 1);
        site.grant(1, 1, FREQUENCY_1, 91, 1_000);
        site.grant(2, 1, FREQUENCY_2, 92, 1_100);

        assertEquals(2, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.RECORDED));
        assertEquals(1, site.calls().get(CallOutcome.NO_RECORDER));
    }

    @Test
    void a_new_call_on_a_channel_is_told_from_the_last_by_who_its_to_and_a_pause()
    {
        Site site = site("DMR calls told apart", 2);
        site.grant(1, 1, FREQUENCY_1, 91, 1_000);
        site.grant(1, 1, FREQUENCY_1, 92, 1_500); //Someone else: a new call
        site.grant(1, 1, FREQUENCY_1, 92, 6_000); //After a pause: a new call
        assertEquals(3, site.calls().getStarted());
        assertEquals(3, site.calls().get(CallOutcome.RECORDED));
    }

    @Test
    void a_call_on_a_channel_without_a_frequency_is_no_frequency()
    {
        Site site = site("DMR unmapped", 2);
        site.grant(7, 1, 0, 93, 1_000);
        site.grant(7, 1, 0, 93, 1_500);

        assertEquals(0, site.starter().mRequests);
        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.NO_FREQUENCY));
    }
}
