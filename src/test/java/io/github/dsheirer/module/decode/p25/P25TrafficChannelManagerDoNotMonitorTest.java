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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.P25Channel;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.source.config.SourceConfigTuner;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the P25 traffic channel manager doesn't allocate (ie tune) traffic channels for calls that are aliased
 * as Do Not Monitor, so that the tuner and traffic channel pool stay free for calls that are monitored.
 */
public class P25TrafficChannelManagerDoNotMonitorTest
{
    private static final long BASE = 851_000_000L;
    private static final long SPACING = 12_500L;
    private static final int TG_MONITORED = 100;
    private static final int TG_DO_NOT_MONITOR = 200;
    private static final int TG_DNM_RECORDED = 300;
    private static final int TG_DNM_STREAMED = 400;
    private static final int RADIO_DO_NOT_MONITOR = 9999;

    private static class Harness
    {
        private final P25TrafficChannelManager mManager;
        private final List<Long> mStartedFrequencies = new ArrayList<>();
        private final List<IDecodeEvent> mEvents = new ArrayList<>();

        Harness(DecodeConfigP25 config)
        {
            Channel parent = new Channel("control");
            parent.setDecodeConfiguration(config);
            mManager = new P25TrafficChannelManager(parent, aliasList());
            EventBus eventBus = new EventBus();
            eventBus.register(this);
            mManager.setInterModuleEventBus(eventBus);
            mManager.addDecodeEventListener(mEvents::add);
        }

        @Subscribe
        public void start(ChannelStartProcessingRequest request)
        {
            mStartedFrequencies.add(request.getChannel().getSourceConfiguration() instanceof
                    SourceConfigTuner sct ? sct.getFrequency() : -1L);
        }

        String lastDetails()
        {
            return mEvents.get(mEvents.size() - 1).getDetails();
        }
    }

    private static Alias talkgroupAlias(String name, int talkgroup, int priority)
    {
        Alias alias = new Alias(name);
        alias.addAliasID(new Talkgroup(Protocol.APCO25, talkgroup));
        alias.addAliasID(new Priority(priority));
        return alias;
    }

    private static AliasList aliasList()
    {
        AliasList list = new AliasList("test");
        list.addAlias(talkgroupAlias("monitored", TG_MONITORED, 1));
        list.addAlias(talkgroupAlias("dnm", TG_DO_NOT_MONITOR, Priority.DO_NOT_MONITOR));

        Alias recorded = talkgroupAlias("dnm recorded", TG_DNM_RECORDED, Priority.DO_NOT_MONITOR);
        recorded.setRecordable(true);
        list.addAlias(recorded);

        Alias streamed = talkgroupAlias("dnm streamed", TG_DNM_STREAMED, Priority.DO_NOT_MONITOR);
        streamed.addAliasID(new BroadcastChannel("stream"));
        list.addAlias(streamed);

        Alias radio = new Alias("dnm radio");
        radio.addAliasID(new Radio(Protocol.APCO25, RADIO_DO_NOT_MONITOR));
        radio.addAliasID(new Priority(Priority.DO_NOT_MONITOR));
        list.addAlias(radio);
        return list;
    }

    private static DecodeConfigP25Phase1 phase1(boolean ignoreDoNotMonitor, int poolSize)
    {
        DecodeConfigP25Phase1 config = new DecodeConfigP25Phase1();
        config.setIgnoreDoNotMonitorCalls(ignoreDoNotMonitor);
        config.setTrafficChannelPoolSize(poolSize);
        return config;
    }

    private static DecodeConfigP25Phase2 phase2(boolean ignoreDoNotMonitor)
    {
        DecodeConfigP25Phase2 config = new DecodeConfigP25Phase2();
        config.setIgnoreDoNotMonitorCalls(ignoreDoNotMonitor);
        return config;
    }

    private static IFrequencyBand band(int timeslots)
    {
        return new IFrequencyBand()
        {
            @Override public boolean isValid() {return true;}
            @Override public int getIdentifier() {return timeslots == 1 ? 1 : 2;}
            @Override public long getChannelSpacing() {return SPACING;}
            @Override public long getBaseFrequency() {return BASE;}
            @Override public int getBandwidth() {return 12500;}
            @Override public long getTransmitOffset() {return 0;}
            @Override public long getDownlinkFrequency(int channelNumber) {return BASE + (channelNumber / timeslots) * SPACING;}
            @Override public long getUplinkFrequency(int channelNumber) {return getDownlinkFrequency(channelNumber);}
            @Override public boolean isTDMA() {return timeslots > 1;}
            @Override public int getTimeslotCount() {return timeslots;}
        };
    }

    private static APCO25Channel fdma(int channelNumber)
    {
        P25Channel channel = new P25Channel(1, channelNumber);
        channel.setFrequencyBand(band(1));
        return new APCO25Channel(channel);
    }

    private static APCO25Channel tdma(int logicalChannel, int timeslot)
    {
        P25Channel channel = new P25Channel(2, logicalChannel * 2 + (timeslot - 1));
        channel.setFrequencyBand(band(2));
        return new APCO25Channel(channel);
    }

    private static MutableIdentifierCollection ic(int talkgroup, Integer radio)
    {
        MutableIdentifierCollection mic = new MutableIdentifierCollection();
        mic.update(APCO25Talkgroup.create(talkgroup));
        if(radio != null)
        {
            mic.update(APCO25RadioIdentifier.createFrom(radio));
        }
        return mic;
    }

    private static void p1Grant(Harness h, APCO25Channel channel, int tg, Integer radio, long ts)
    {
        h.mManager.processP1ControlDirectedChannelGrant(channel, VoiceServiceOptions.createUnencrypted(), ic(tg, radio),
                Opcode.OSP_GROUP_VOICE_CHANNEL_GRANT, ts, "test");
    }

    private static void p1Update(Harness h, APCO25Channel channel, int tg, long ts)
    {
        h.mManager.processP1ControlAnnouncedTrafficUpdate(channel, null, ic(tg, null),
                Opcode.OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE, ts, "test");
    }

    private static void p2Grant(Harness h, APCO25Channel channel, int tg, Integer radio, long ts)
    {
        h.mManager.processP2ChannelGrant(channel, VoiceServiceOptions.createUnencrypted(), ic(tg, radio),
                MacOpcode.PHASE1_C0_GROUP_VOICE_CHANNEL_GRANT_EXPLICIT, ts, "test");
    }

    private static void p2Update(Harness h, APCO25Channel channel, int tg, long ts)
    {
        h.mManager.processP2ChannelUpdate(channel, new VoiceServiceOptions(0), ic(tg, null),
                MacOpcode.PHASE1_42_GROUP_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT, ts, "test");
    }

    @Test
    void optionIsEnabledByDefault()
    {
        assertTrue(new DecodeConfigP25Phase1().getIgnoreDoNotMonitorCalls());
        assertTrue(new DecodeConfigP25Phase2().getIgnoreDoNotMonitorCalls());
    }

    @Test
    void phase1DoNotMonitorGrantIsIgnoredIncludingLaterUpdates()
    {
        Harness h = new Harness(phase1(true, 20));
        p1Grant(h, fdma(10), TG_DO_NOT_MONITOR, 1, 1000);
        p1Update(h, fdma(10), TG_DO_NOT_MONITOR, 1500);
        p1Grant(h, fdma(10), TG_DO_NOT_MONITOR, 1, 2000);
        assertTrue(h.mStartedFrequencies.isEmpty());
        assertTrue(h.lastDetails().startsWith("IGNORED: DO NOT MONITOR"), h.lastDetails());
    }

    @Test
    void phase1MonitoredGrantIsTuned()
    {
        Harness h = new Harness(phase1(true, 20));
        p1Grant(h, fdma(10), TG_MONITORED, 1, 1000);
        assertEquals(List.of(BASE + 10 * SPACING), h.mStartedFrequencies);
    }

    @Test
    void phase1DoNotMonitorGrantIsTunedWhenOptionDisabled()
    {
        Harness h = new Harness(phase1(false, 20));
        p1Grant(h, fdma(10), TG_DO_NOT_MONITOR, 1, 1000);
        assertEquals(List.of(BASE + 10 * SPACING), h.mStartedFrequencies);
    }

    @Test
    void doNotMonitorCallThatIsRecordedIsStillTuned()
    {
        Harness h = new Harness(phase1(true, 20));
        p1Grant(h, fdma(10), TG_DNM_RECORDED, 1, 1000);
        assertEquals(List.of(BASE + 10 * SPACING), h.mStartedFrequencies);
    }

    @Test
    void doNotMonitorCallThatIsStreamedIsStillTuned()
    {
        Harness h = new Harness(phase1(true, 20));
        p1Grant(h, fdma(10), TG_DNM_STREAMED, 1, 1000);
        assertEquals(List.of(BASE + 10 * SPACING), h.mStartedFrequencies);
    }

    @Test
    void doNotMonitorRadioIsIgnored()
    {
        Harness h = new Harness(phase1(true, 20));
        p1Grant(h, fdma(10), TG_MONITORED, RADIO_DO_NOT_MONITOR, 1000);
        assertTrue(h.mStartedFrequencies.isEmpty());
        assertTrue(h.lastDetails().startsWith("IGNORED: DO NOT MONITOR"), h.lastDetails());
    }

    /**
     * The reported problem: with a single traffic channel available, a Do Not Monitor call used to occupy it for the
     * length of the call and a monitored call on another frequency was lost.
     */
    @Test
    void doNotMonitorCallLeavesTrafficChannelFreeForMonitoredCall()
    {
        Harness h = new Harness(phase1(true, 1));
        p1Grant(h, fdma(10), TG_DO_NOT_MONITOR, 1, 1000);
        p1Grant(h, fdma(20), TG_MONITORED, 2, 1100);
        assertEquals(List.of(BASE + 20 * SPACING), h.mStartedFrequencies);

        Harness before = new Harness(phase1(false, 1));
        p1Grant(before, fdma(10), TG_DO_NOT_MONITOR, 1, 1000);
        p1Grant(before, fdma(20), TG_MONITORED, 2, 1100);
        assertEquals(List.of(BASE + 10 * SPACING), before.mStartedFrequencies,
                "option off: the DNM call holds the only channel and the monitored call is not tuned");
    }

    @Test
    void phase2DoNotMonitorGrantIsIgnoredIncludingLaterUpdates()
    {
        Harness h = new Harness(phase2(true));
        p2Grant(h, tdma(5, 1), TG_DO_NOT_MONITOR, 1, 1000);
        p2Update(h, tdma(5, 1), TG_DO_NOT_MONITOR, 1500);
        assertTrue(h.mStartedFrequencies.isEmpty());
        assertTrue(h.lastDetails().startsWith("IGNORED: DO NOT MONITOR"), h.lastDetails());
    }

    @Test
    void phase2MonitoredCallOnOtherTimeslotTunesChannelOnce()
    {
        Harness h = new Harness(phase2(true));
        p2Grant(h, tdma(5, 1), TG_DO_NOT_MONITOR, 1, 1000);
        assertTrue(h.mStartedFrequencies.isEmpty());
        p2Grant(h, tdma(5, 2), TG_MONITORED, 2, 1100);
        assertEquals(List.of(BASE + 5 * SPACING), h.mStartedFrequencies);

        p2Update(h, tdma(5, 1), TG_DO_NOT_MONITOR, 1500);
        p2Update(h, tdma(5, 2), TG_MONITORED, 1500);
        assertEquals(1, h.mStartedFrequencies.size());
    }

    @Test
    void phase2DoNotMonitorGrantOnAlreadyTunedChannelIsNotLabeledIgnored()
    {
        Harness h = new Harness(phase2(true));
        p2Grant(h, tdma(5, 2), TG_MONITORED, 2, 1000);
        p2Grant(h, tdma(5, 1), TG_DO_NOT_MONITOR, 1, 1100);
        assertEquals(1, h.mStartedFrequencies.size());
        assertTrue(h.lastDetails().startsWith("PHASE 2 CHANNEL GRANT"), h.lastDetails());
    }

    @Test
    void managerWithoutAliasListTunesEverything()
    {
        Channel parent = new Channel("control");
        parent.setDecodeConfiguration(phase1(true, 20));
        P25TrafficChannelManager manager = new P25TrafficChannelManager(parent);
        List<Long> started = new ArrayList<>();
        EventBus eventBus = new EventBus();
        eventBus.register(new Object()
        {
            @Subscribe
            public void start(ChannelStartProcessingRequest request)
            {
                started.add(((SourceConfigTuner)request.getChannel().getSourceConfiguration()).getFrequency());
            }
        });
        manager.setInterModuleEventBus(eventBus);
        manager.processP1ControlDirectedChannelGrant(fdma(10), VoiceServiceOptions.createUnencrypted(),
                ic(TG_DO_NOT_MONITOR, 1), Opcode.OSP_GROUP_VOICE_CHANNEL_GRANT, 1000, "test");
        assertEquals(List.of(BASE + 10 * SPACING), started);
    }

    @Test
    void copyKeepsDoNotMonitorSetting()
    {
        DecodeConfigP25Phase1 p1 = (DecodeConfigP25Phase1)DecoderFactory.copy(phase1(false, 20));
        assertFalse(p1.getIgnoreDoNotMonitorCalls());
        DecodeConfigP25Phase2 p2 = (DecodeConfigP25Phase2)DecoderFactory.copy(phase2(false));
        assertFalse(p2.getIgnoreDoNotMonitorCalls());
        assertTrue(((DecodeConfigP25Phase2)DecoderFactory.copy(phase2(true))).getIgnoreDoNotMonitorCalls());
    }
}
