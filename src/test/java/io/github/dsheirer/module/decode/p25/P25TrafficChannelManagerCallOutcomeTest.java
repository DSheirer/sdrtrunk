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

package io.github.dsheirer.module.decode.p25;

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
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.P25Channel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import org.junit.jupiter.api.Test;

/**
 * What a control channel's calls are counted as, for node health reports.
 */
public class P25TrafficChannelManagerCallOutcomeTest
{
    private static final long BASE = 851_000_000L;
    private static final long SPACING = 12_500L;

    /**
     * A channel processing manager with no tuner for any frequency: it rejects each start request while it's made, as
     * ChannelProcessingManager does when no tuner covers the frequency.
     */
    private static class NoTuner
    {
        private final P25TrafficChannelManager mManager;

        NoTuner(P25TrafficChannelManager manager)
        {
            mManager = manager;
        }

        @Subscribe
        public void start(ChannelStartProcessingRequest request)
        {
            mManager.getChannelEventListener().receive(new ChannelEvent(request.getChannel(),
                ChannelEvent.Event.NOTIFICATION_PROCESSING_START_REJECTED, "TUNER UNAVAILABLE"));
        }
    }

    private static APCO25Channel fdma(int channelNumber)
    {
        P25Channel channel = new P25Channel(1, channelNumber);
        channel.setFrequencyBand(new IFrequencyBand()
        {
            @Override public boolean isValid() {return true;}
            @Override public int getIdentifier() {return 1;}
            @Override public long getChannelSpacing() {return SPACING;}
            @Override public long getBaseFrequency() {return BASE;}
            @Override public int getBandwidth() {return 12500;}
            @Override public long getTransmitOffset() {return 0;}
            @Override public long getDownlinkFrequency(int number) {return BASE + number * SPACING;}
            @Override public long getUplinkFrequency(int number) {return getDownlinkFrequency(number);}
            @Override public boolean isTDMA() {return false;}
            @Override public int getTimeslotCount() {return 1;}
        });
        return new APCO25Channel(channel);
    }

    private static MutableIdentifierCollection talkgroup(int talkgroup)
    {
        MutableIdentifierCollection identifiers = new MutableIdentifierCollection();
        identifiers.update(APCO25Talkgroup.create(talkgroup));
        return identifiers;
    }

    /**
     * A P25 channel's calls are counted from its first grant: a conventional channel, which never grants one, has no
     * calls counted, so a report gives it as a channel whatever control messages it decodes, not as a deaf site.
     */
    @Test
    void calls_are_counted_from_the_first_grant()
    {
        Channel parent = new Channel("conventional P25");
        parent.setDecodeConfiguration(new DecodeConfigP25Phase1());
        P25TrafficChannelManager manager = new P25TrafficChannelManager(parent);
        EventBus eventBus = new EventBus();
        manager.setInterModuleEventBus(eventBus);
        io.github.dsheirer.health.ChannelHealth health = HealthRegistry.getInstance().channel(parent);
        org.junit.jupiter.api.Assertions.assertNull(health.totals(0).calls(), "no grant: not counted");

        manager.processP1ControlDirectedChannelGrant(fdma(10), new VoiceServiceOptions(0), talkgroup(100),
            Opcode.OSP_GROUP_VOICE_CHANNEL_GRANT, 1_000, "test");
        assertEquals(1, health.totals(0).calls().started());
    }

    @Test
    void a_call_no_tuner_can_take_is_counted_as_no_source_not_recorded()
    {
        Channel parent = new Channel("no tuner control");
        parent.setDecodeConfiguration(new DecodeConfigP25Phase1());
        P25TrafficChannelManager manager = new P25TrafficChannelManager(parent);
        EventBus eventBus = new EventBus();
        eventBus.register(new NoTuner(manager));
        manager.setInterModuleEventBus(eventBus);

        manager.processP1ControlDirectedChannelGrant(fdma(10), new VoiceServiceOptions(0), talkgroup(100),
            Opcode.OSP_GROUP_VOICE_CHANNEL_GRANT, 1_000, "test");

        CallTally calls = HealthRegistry.getInstance().calls(parent);
        assertEquals(1, calls.getStarted());
        assertEquals(1, calls.get(CallOutcome.NO_SOURCE), "rejected while it was requested");
        assertEquals(0, calls.get(CallOutcome.RECORDED));

        //The same call's next grant tries again, and is refused again: still one call, still noSource.
        manager.processP1ControlDirectedChannelGrant(fdma(10), new VoiceServiceOptions(0), talkgroup(100),
            Opcode.OSP_GROUP_VOICE_CHANNEL_GRANT, 1_500, "test");
        assertEquals(1, calls.getStarted());
        assertEquals(1, calls.get(CallOutcome.NO_SOURCE));
        assertEquals(0, calls.get(CallOutcome.RECORDED));
    }
}
