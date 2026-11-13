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
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
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
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import io.github.dsheirer.source.config.SourceConfigTuner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the ignore encrypted calls option of the P25 traffic channel manager.
 */
public class P25TrafficChannelManagerEncryptedTest
{
    private static final long BASE = 851_000_000L;
    private static final long SPACING = 12_500L;
    private static final int TALKGROUP_A = 100;
    private static final int TALKGROUP_B = 200;

    /**
     * Captures traffic channel start requests and decode events.
     */
    private static class Harness
    {
        private final P25TrafficChannelManager mManager;
        private final List<Long> mStartedFrequencies = new ArrayList<>();
        private final List<IDecodeEvent> mEvents = new ArrayList<>();

        Harness(DecodeConfigP25 config)
        {
            Channel parent = new Channel("control");
            parent.setDecodeConfiguration(config);
            mManager = new P25TrafficChannelManager(parent);
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

    private static DecodeConfigP25Phase1 phase1(boolean ignoreEncrypted)
    {
        DecodeConfigP25Phase1 config = new DecodeConfigP25Phase1();
        config.setIgnoreEncryptedCalls(ignoreEncrypted);
        return config;
    }

    private static DecodeConfigP25Phase2 phase2(boolean ignoreEncrypted)
    {
        DecodeConfigP25Phase2 config = new DecodeConfigP25Phase2();
        config.setIgnoreEncryptedCalls(ignoreEncrypted);
        return config;
    }

    /**
     * Frequency band where the downlink frequency for channel number N is BASE + (N / timeslots) * SPACING
     */
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

    /**
     * @param logicalChannel number (ie frequency index)
     * @param timeslot 1 or 2
     */
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

    private static void p1Grant(Harness h, APCO25Channel channel, ServiceOptions so, int tg, Integer radio, long ts)
    {
        h.mManager.processP1ControlDirectedChannelGrant(channel, so, ic(tg, radio), Opcode.OSP_GROUP_VOICE_CHANNEL_GRANT, ts, "test");
    }

    private static void p1Update(Harness h, APCO25Channel channel, int tg, long ts)
    {
        h.mManager.processP1ControlAnnouncedTrafficUpdate(channel, null, ic(tg, null),
                Opcode.OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE, ts, "test");
    }

    private static void p2Grant(Harness h, APCO25Channel channel, ServiceOptions so, int tg, Integer radio, long ts)
    {
        h.mManager.processP2ChannelGrant(channel, so, ic(tg, radio), MacOpcode.PHASE1_C0_GROUP_VOICE_CHANNEL_GRANT_EXPLICIT, ts, "test");
    }

    /**
     * Mirrors the decoder which uses empty (ie unencrypted) service options for updates that don't carry them.
     */
    private static void p2Update(Harness h, APCO25Channel channel, int tg, long ts)
    {
        h.mManager.processP2ChannelUpdate(channel, new VoiceServiceOptions(0), ic(tg, null),
                MacOpcode.PHASE1_42_GROUP_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT, ts, "test");
    }

    @Test
    void phase1EncryptedGrantIsTunedWhenOptionDisabled()
    {
        Harness h = new Harness(phase1(false));
        p1Grant(h, fdma(10), VoiceServiceOptions.createEncrypted(), TALKGROUP_A, 1, 1000);
        assertEquals(List.of(BASE + 10 * SPACING), h.mStartedFrequencies);
    }

    @Test
    void phase1EncryptedGrantIsIgnoredIncludingLaterUpdates()
    {
        Harness h = new Harness(phase1(true));
        p1Grant(h, fdma(10), VoiceServiceOptions.createEncrypted(), TALKGROUP_A, 1, 1000);
        p1Update(h, fdma(10), TALKGROUP_A, 1500);
        p1Grant(h, fdma(10), VoiceServiceOptions.createEncrypted(), TALKGROUP_A, 1, 2000);
        assertTrue(h.mStartedFrequencies.isEmpty());
        assertTrue(h.lastDetails().startsWith("IGNORED: ENCRYPTED"), h.lastDetails());
    }

    @Test
    void phase1ClearGrantIsTuned()
    {
        Harness h = new Harness(phase1(true));
        p1Grant(h, fdma(10), VoiceServiceOptions.createUnencrypted(), TALKGROUP_A, 1, 1000);
        assertEquals(List.of(BASE + 10 * SPACING), h.mStartedFrequencies);
    }

    @Test
    void phase1DifferentTalkerWithClearGrantIsTuned()
    {
        Harness h = new Harness(phase1(true));
        p1Grant(h, fdma(10), VoiceServiceOptions.createEncrypted(), TALKGROUP_A, 1, 1000);
        p1Grant(h, fdma(10), VoiceServiceOptions.createUnencrypted(), TALKGROUP_A, 2, 1500);
        assertEquals(List.of(BASE + 10 * SPACING), h.mStartedFrequencies);
    }

    @Test
    void phase2EncryptedGrantIsIgnoredIncludingLaterUpdates()
    {
        Harness h = new Harness(phase2(true));
        p2Grant(h, tdma(5, 1), VoiceServiceOptions.createEncrypted(), TALKGROUP_A, 1, 1000);
        p2Update(h, tdma(5, 1), TALKGROUP_A, 1500);
        assertTrue(h.mStartedFrequencies.isEmpty());
        assertTrue(h.lastDetails().startsWith("IGNORED: ENCRYPTED"), h.lastDetails());
    }

    @Test
    void phase2ClearCallOnOtherTimeslotTunesChannel()
    {
        Harness h = new Harness(phase2(true));
        p2Grant(h, tdma(5, 1), VoiceServiceOptions.createEncrypted(), TALKGROUP_A, 1, 1000);
        assertTrue(h.mStartedFrequencies.isEmpty());
        p2Grant(h, tdma(5, 2), VoiceServiceOptions.createUnencrypted(), TALKGROUP_B, 2, 1100);
        assertEquals(List.of(BASE + 5 * SPACING), h.mStartedFrequencies);

        //Further updates on either timeslot don't request another channel
        p2Update(h, tdma(5, 1), TALKGROUP_A, 1500);
        p2Update(h, tdma(5, 2), TALKGROUP_B, 1500);
        assertEquals(1, h.mStartedFrequencies.size());
    }

    @Test
    void phase2EncryptedGrantOnAlreadyTunedChannelIsNotLabeledIgnored()
    {
        Harness h = new Harness(phase2(true));
        p2Grant(h, tdma(5, 2), VoiceServiceOptions.createUnencrypted(), TALKGROUP_B, 2, 1000);
        p2Grant(h, tdma(5, 1), VoiceServiceOptions.createEncrypted(), TALKGROUP_A, 1, 1100);
        assertEquals(1, h.mStartedFrequencies.size());
        assertTrue(h.lastDetails().startsWith("PHASE 2 CHANNEL GRANT"), h.lastDetails());
    }

    @Test
    void phase2EncryptedGrantIsTunedWhenOptionDisabled()
    {
        Harness h = new Harness(phase2(false));
        p2Grant(h, tdma(5, 1), VoiceServiceOptions.createEncrypted(), TALKGROUP_A, 1, 1000);
        assertEquals(List.of(BASE + 5 * SPACING), h.mStartedFrequencies);
    }

    private static Set<IDecodeEvent> distinctEvents(Harness h, int timeslot)
    {
        Set<IDecodeEvent> events = Collections.newSetFromMap(new IdentityHashMap<>());
        for(IDecodeEvent event : h.mEvents)
        {
            if(event.getTimeslot() == timeslot)
            {
                events.add(event);
            }
        }
        return events;
    }

    @Test
    void phase2IgnoredDataCallOnTimeslot2IsTrackedOnTimeslot2()
    {
        DecodeConfigP25Phase2 config = phase2(false);
        config.setIgnoreDataCalls(true);
        Harness h = new Harness(config);
        h.mManager.processP1ControlDirectedChannelGrant(tdma(5, 2), null, ic(TALKGROUP_A, 1),
                Opcode.OSP_GROUP_DATA_CHANNEL_GRANT, 1000, "test");
        h.mManager.processP1ControlDirectedChannelGrant(tdma(5, 2), null, ic(TALKGROUP_A, 1),
                Opcode.OSP_GROUP_DATA_CHANNEL_GRANT, 1500, "test");
        assertTrue(h.mStartedFrequencies.isEmpty());
        assertEquals(1, distinctEvents(h, 2).size(), "repeated grant should update the timeslot 2 event");
        assertTrue(distinctEvents(h, 1).isEmpty());
    }

    @Test
    void phase2DataChannelBroadcastsBothTimeslotEvents()
    {
        Harness h = new Harness(phase2(false));
        h.mManager.processP2DataChannel(tdma(5, 1), 1000);
        assertEquals(1, distinctEvents(h, 1).size());
        assertEquals(1, distinctEvents(h, 2).size());
    }

    @Test
    void phase2CopyKeepsTrafficChannelSettings()
    {
        DecodeConfigP25Phase2 original = phase2(true);
        original.setIgnoreDataCalls(true);
        original.setTrafficChannelPoolSize(7);
        DecodeConfigP25Phase2 copy = (DecodeConfigP25Phase2)DecoderFactory.copy(original);
        assertTrue(copy.getIgnoreDataCalls());
        assertTrue(copy.getIgnoreEncryptedCalls());
        assertEquals(7, copy.getTrafficChannelPoolSize());

        DecodeConfigP25Phase2 defaults = (DecodeConfigP25Phase2)DecoderFactory.copy(phase2(false));
        assertFalse(defaults.getIgnoreDataCalls());
        assertFalse(defaults.getIgnoreEncryptedCalls());
    }
}
