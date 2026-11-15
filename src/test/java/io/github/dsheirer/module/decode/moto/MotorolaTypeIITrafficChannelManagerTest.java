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

package io.github.dsheirer.module.decode.moto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.moto.channel.MotorolaTypeIIChannel;
import io.github.dsheirer.module.decode.moto.message.MotorolaTypeIIMessage;
import io.github.dsheirer.module.decode.moto.message.MotorolaTypeIIMessageType;
import io.github.dsheirer.source.config.SourceConfigTuner;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests which channel grants the traffic channel manager follows.
 */
class MotorolaTypeIITrafficChannelManagerTest
{
    private static final int TALKGROUP = 33200;
    private static final int RADIO = 55631;
    //856.7125 MHz in the rebanded 800 MHz bandplan
    private static final int CHANNEL_NUMBER = 228;

    /**
     * Captures traffic channel start requests and decode events.
     */
    private static class Harness
    {
        private final Bandplan mBandplan = new Bandplan(BandplanType.EIGHT_HUNDRED_REBANDED);
        private final MotorolaTypeIITrafficChannelManager mManager;
        private final List<Long> mStartedFrequencies = new ArrayList<>();
        private final List<IDecodeEvent> mEvents = new ArrayList<>();

        Harness()
        {
            Channel parent = new Channel("control");
            DecodeConfigMotorolaTypeII config = new DecodeConfigMotorolaTypeII();
            parent.setDecodeConfiguration(config);
            mManager = new MotorolaTypeIITrafficChannelManager(parent, config);
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

        void grant(MotorolaTypeIIMessageType type)
        {
            MotorolaTypeIIMessage message = new MotorolaTypeIIMessage(TALKGROUP, true, 0x308, CHANNEL_NUMBER, RADIO,
                null, true, type);
            MutableIdentifierCollection identifiers = new MutableIdentifierCollection();
            identifiers.update(message.getIdentifiers());
            mManager.processChannelGrant(message, identifiers, new MotorolaTypeIIChannel(CHANNEL_NUMBER, mBandplan));
        }

        String lastDetails()
        {
            return mEvents.get(mEvents.size() - 1).getDetails();
        }
    }

    @Test
    void analogGrantStartsATrafficChannel()
    {
        Harness harness = new Harness();
        harness.grant(MotorolaTypeIIMessageType.ANALOG_GROUP_GRANT);

        assertEquals(1, harness.mStartedFrequencies.size());
        assertTrue(harness.mStartedFrequencies.get(0) > 0, "traffic channel has the granted frequency");
        assertEquals(1, harness.mEvents.size());
    }

    @Test
    void digitalGrantIsReportedAndNotFollowed()
    {
        Harness harness = new Harness();
        harness.grant(MotorolaTypeIIMessageType.DIGITAL_GROUP_GRANT);

        assertTrue(harness.mStartedFrequencies.isEmpty(), "digital voice isn't sent to the analog decoder");
        assertEquals(1, harness.mEvents.size());
        assertEquals(MotorolaTypeIITrafficChannelManager.DIGITAL_VOICE_NOT_SUPPORTED, harness.lastDetails());

        //Repeated grants for the same call update the one event
        harness.grant(MotorolaTypeIIMessageType.DIGITAL_GROUP_GRANT);
        assertTrue(harness.mStartedFrequencies.isEmpty());
        assertEquals(2, harness.mEvents.size());
        assertEquals(harness.mEvents.get(0), harness.mEvents.get(1));
    }
}
