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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.health.CallOutcome;
import io.github.dsheirer.health.CallTally;
import io.github.dsheirer.health.HealthRegistry;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannel;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannelLookup;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNEncryptionKey;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallTimer;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.VoiceCallOption;
import io.github.dsheirer.protocol.Protocol;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * What an NXDN control channel's calls are counted as, for node health reports: each call once, by what finally
 * became of it.
 */
class NXDNTrafficChannelManagerCallOutcomeTest
{
    private static final EncryptionKeyIdentifier CLEAR = EncryptionKeyIdentifier.create(Protocol.NXDN,
        NXDNEncryptionKey.create(0, 0));
    private static final EncryptionKeyIdentifier ENCRYPTED = EncryptionKeyIdentifier.create(Protocol.NXDN,
        NXDNEncryptionKey.create(0x03, 0x2A));

    /**
     * Stands in for the channel processing manager: starts each traffic channel asked for, or, with no tuner, rejects
     * the request while it's made, as ChannelProcessingManager does when no tuner covers the frequency.
     */
    private static class Starter
    {
        private NXDNTrafficChannelManager mManager;
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

    private record Site(NXDNTrafficChannelManager manager, Starter starter, CallTally calls)
    {
        void call(NXDNChannel channel, int talkgroup, EncryptionKeyIdentifier encryption, long timestamp)
        {
            manager.processVoiceCall(List.of(NXDNRadioIdentifier.createFrom(1000 + talkgroup),
                    NXDNTalkgroupIdentifier.createTo(talkgroup), encryption), channel, CallType.GROUP_BROADCAST,
                encryption, timestamp, new VoiceCallOption(0), CallTimer.UNSPECIFIED);
        }
    }

    private static Site site(String name, int trafficChannels, boolean ignoreEncrypted)
    {
        Channel parent = new Channel(name);
        DecodeConfigNXDN config = new DecodeConfigNXDN();
        config.setTrafficChannelPoolSize(trafficChannels);
        config.setIgnoreEncryptedCalls(ignoreEncrypted);
        parent.setDecodeConfiguration(config);
        NXDNTrafficChannelManager manager = new NXDNTrafficChannelManager(parent);
        Starter starter = new Starter();
        starter.mManager = manager;
        EventBus eventBus = new EventBus();
        eventBus.register(starter);
        manager.setInterModuleEventBus(eventBus);
        return new Site(manager, starter, HealthRegistry.getInstance().calls(parent));
    }

    /**
     * A channel the channel map gives a frequency for.
     */
    private static NXDNChannel channel(int number, long frequency)
    {
        NXDNChannelLookup channel = new NXDNChannelLookup(number);
        channel.receive(null, Map.of(number, new ChannelFrequency(number, frequency, 0)));
        return channel;
    }

    @Test
    void a_followed_call_is_counted_once_however_many_assignments_it_has()
    {
        Site site = site("NXDN followed", 2, false);
        NXDNChannel channel = channel(12, 452_012_500L);
        site.call(channel, 91, CLEAR, 1_000);
        site.call(channel, 91, CLEAR, 1_500);
        site.call(channel, 91, CLEAR, 2_000);

        assertEquals(1, site.starter().mRequests);
        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.RECORDED));
    }

    @Test
    void a_call_no_tuner_can_take_is_counted_as_no_source_not_recorded()
    {
        Site site = site("NXDN no tuner", 2, false);
        site.starter().mNoTuner = true;
        NXDNChannel channel = channel(12, 452_012_500L);
        site.call(channel, 91, CLEAR, 1_000);

        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.NO_SOURCE), "rejected while it was requested");
        assertEquals(0, site.calls().get(CallOutcome.RECORDED));

        //The rejection lets the call's next assignment try again: a tuner turns up, and it's followed after all.
        site.starter().mNoTuner = false;
        site.call(channel, 91, CLEAR, 1_500);
        assertEquals(2, site.starter().mRequests);
        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.RECORDED));
        assertEquals(0, site.calls().get(CallOutcome.NO_SOURCE));
    }

    @Test
    void a_call_with_every_traffic_channel_busy_is_no_recorder()
    {
        Site site = site("NXDN busy", 1, false);
        site.call(channel(12, 452_012_500L), 91, CLEAR, 1_000);
        site.call(channel(13, 452_025_000L), 92, CLEAR, 1_100);

        assertEquals(2, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.RECORDED));
        assertEquals(1, site.calls().get(CallOutcome.NO_RECORDER));
    }

    @Test
    void a_call_on_a_channel_the_channel_map_lacks_is_no_frequency()
    {
        Site site = site("NXDN unmapped", 2, false);
        NXDNChannel unmapped = new NXDNChannelLookup(40);
        site.call(unmapped, 93, CLEAR, 1_000);
        site.call(unmapped, 93, CLEAR, 1_500);

        assertEquals(0, site.starter().mRequests);
        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.NO_FREQUENCY));
    }

    @Test
    void an_encrypted_call_the_user_chose_not_to_follow_is_encrypted()
    {
        Site site = site("NXDN encrypted", 2, true);
        site.call(channel(12, 452_012_500L), 94, ENCRYPTED, 1_000);

        assertEquals(0, site.starter().mRequests);
        assertEquals(1, site.calls().getStarted());
        assertEquals(1, site.calls().get(CallOutcome.ENCRYPTED));
    }
}
