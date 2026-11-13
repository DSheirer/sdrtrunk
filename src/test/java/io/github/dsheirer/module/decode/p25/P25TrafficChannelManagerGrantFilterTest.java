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
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
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
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import io.github.dsheirer.preference.decoder.TrafficChannelPreference;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.source.config.SourceConfigTuner;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the ignore data calls option for data grants that aren't data channel grants, and the ignore unaliased
 * talkgroups preference of the P25 traffic channel manager.
 */
public class P25TrafficChannelManagerGrantFilterTest
{
    private static final long BASE = 851_000_000L;
    private static final long SPACING = 12_500L;
    private static final int ALIASED_TALKGROUP = 100;
    private static final int UNALIASED_TALKGROUP = 200;

    /**
     * Captures traffic channel start requests and decode events.
     */
    private static class Harness
    {
        private final P25TrafficChannelManager mManager;
        private final List<Long> mStartedFrequencies = new ArrayList<>();
        private final List<IDecodeEvent> mEvents = new ArrayList<>();

        Harness(DecodeConfigP25 config, boolean ignoreUnaliased)
        {
            Channel parent = new Channel("control");
            parent.setDecodeConfiguration(config);
            mManager = new P25TrafficChannelManager(parent);
            mManager.setTalkgroupFilter(aliasList(), preference(ignoreUnaliased));
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

    private static AliasList aliasList()
    {
        Alias alias = new Alias("Aliased");
        alias.addAliasID(new Talkgroup(Protocol.APCO25, ALIASED_TALKGROUP));
        AliasList aliasList = new AliasList("test");
        aliasList.addAlias(alias);
        return aliasList;
    }

    /**
     * Preference with a fixed value so that the test doesn't read or write the user's stored preferences.
     */
    private static TrafficChannelPreference preference(boolean ignoreUnaliased)
    {
        return new TrafficChannelPreference(type -> {})
        {
            @Override
            public boolean isIgnoreUnaliasedTalkgroups()
            {
                return ignoreUnaliased;
            }
        };
    }

    private static DecodeConfigP25Phase1 phase1(boolean ignoreDataCalls)
    {
        DecodeConfigP25Phase1 config = new DecodeConfigP25Phase1();
        config.setIgnoreDataCalls(ignoreDataCalls);
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

    private static APCO25Channel tdma(int logicalChannel, int timeslot)
    {
        P25Channel channel = new P25Channel(2, logicalChannel * 2 + (timeslot - 1));
        channel.setFrequencyBand(band(2));
        return new APCO25Channel(channel);
    }

    private static MutableIdentifierCollection ic(int talkgroup)
    {
        MutableIdentifierCollection mic = new MutableIdentifierCollection();
        mic.update(APCO25Talkgroup.create(talkgroup));
        mic.update(APCO25RadioIdentifier.createFrom(1));
        return mic;
    }

    private static void p1Grant(Harness h, APCO25Channel channel, int tg)
    {
        h.mManager.processP1ControlDirectedChannelGrant(channel, VoiceServiceOptions.createUnencrypted(), ic(tg),
                Opcode.OSP_GROUP_VOICE_CHANNEL_GRANT, 1000, "test");
    }

    @Test
    void l3harrisPrivateDataGrantIsIgnoredWhenIgnoringDataCalls()
    {
        Harness h = new Harness(phase1(true), false);
        h.mManager.processP2ChannelGrant(fdma(10), null, ic(ALIASED_TALKGROUP),
                MacOpcode.L3HARRIS_A0_PRIVATE_DATA_CHANNEL_GRANT, 1000, "test");
        assertTrue(h.mStartedFrequencies.isEmpty());
        assertTrue(h.lastDetails().startsWith("IGNORED: PHASE 1 DATA CALL"), h.lastDetails());
    }

    @Test
    void l3harrisPrivateDataGrantIsTunedWhenNotIgnoringDataCalls()
    {
        Harness h = new Harness(phase1(false), false);
        h.mManager.processP2ChannelGrant(fdma(10), null, ic(ALIASED_TALKGROUP),
                MacOpcode.L3HARRIS_A0_PRIVATE_DATA_CHANNEL_GRANT, 1000, "test");
        assertEquals(List.of(BASE + 10 * SPACING), h.mStartedFrequencies);
    }

    @Test
    void phase1UnaliasedTalkgroupIsIgnoredWhenPreferenceEnabled()
    {
        Harness h = new Harness(phase1(false), true);
        p1Grant(h, fdma(10), UNALIASED_TALKGROUP);
        assertTrue(h.mStartedFrequencies.isEmpty());
        assertTrue(h.lastDetails().startsWith("IGNORED: PHASE 1 UNALIASED TALKGROUP"), h.lastDetails());
    }

    @Test
    void phase1AliasedTalkgroupIsTunedWhenPreferenceEnabled()
    {
        Harness h = new Harness(phase1(false), true);
        p1Grant(h, fdma(10), ALIASED_TALKGROUP);
        assertEquals(List.of(BASE + 10 * SPACING), h.mStartedFrequencies);
    }

    @Test
    void phase1UnaliasedTalkgroupIsTunedWhenPreferenceDisabled()
    {
        Harness h = new Harness(phase1(false), false);
        p1Grant(h, fdma(10), UNALIASED_TALKGROUP);
        assertEquals(List.of(BASE + 10 * SPACING), h.mStartedFrequencies);
    }

    @Test
    void phase2UnaliasedTalkgroupIsIgnoredWhenPreferenceEnabled()
    {
        Harness h = new Harness(new DecodeConfigP25Phase2(), true);
        h.mManager.processP2ChannelGrant(tdma(5, 1), VoiceServiceOptions.createUnencrypted(), ic(UNALIASED_TALKGROUP),
                MacOpcode.PHASE1_C0_GROUP_VOICE_CHANNEL_GRANT_EXPLICIT, 1000, "test");
        assertTrue(h.mStartedFrequencies.isEmpty());
        assertTrue(h.lastDetails().contains("UNALIASED TALKGROUP IGNORED"), h.lastDetails());
    }
}
