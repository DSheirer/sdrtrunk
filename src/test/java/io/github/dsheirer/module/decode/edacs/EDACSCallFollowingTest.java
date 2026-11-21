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

package io.github.dsheirer.module.decode.edacs;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.heartbeat.Heartbeat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Call following with MBTA values - control channel LCN 10 (853.725 MHz), group 529 from unit 13254
 * on LCN 5 (851.9375 MHz), ProVoice.
 */
public class EDACSCallFollowingTest
{
    private static final double[] MBTA = {851.175, 851.225, 851.425, 851.475, 851.9375, 851.9875, 852.175, 852.25,
        853.6625, 853.725, 853.9, 851.0625, 851.45, 851.9625, 853.925};

    private final long[] mNow = {1_000_000L};
    private final List<ChannelStartProcessingRequest> mStarts = new ArrayList<>();
    private final List<ChannelEvent> mChannelEvents = new ArrayList<>();
    private final List<IDecodeEvent> mEvents = new ArrayList<>();
    private EDACSTrafficChannelManager mManager;
    private EDACSDecoderState mState;

    private void setup()
    {
        setup(false);
    }

    private void setup(boolean ignoreUnaliased)
    {
        DecodeConfigEDACS config = new DecodeConfigEDACS();
        config.setIgnoreUnaliasedTalkgroups(ignoreUnaliased);
        List<Long> frequencies = new ArrayList<>();
        for(double mhz : MBTA)
        {
            frequencies.add(Math.round(mhz * 1e6));
        }
        config.setLcnFrequencies(frequencies);
        Channel control = new Channel("MBTA", Channel.ChannelType.STANDARD);
        control.setDecodeConfiguration(config);

        mManager = new EDACSTrafficChannelManager(control);
        mManager.setClock(() -> mNow[0]);
        EventBus bus = new EventBus();
        bus.register(new Object()
        {
            @Subscribe
            public void start(ChannelStartProcessingRequest request)
            {
                mStarts.add(request);
            }
        });
        mManager.setInterModuleEventBus(bus);
        mManager.setChannelEventListener(mChannelEvents::add);
        mManager.addDecodeEventListener(mEvents::add);

        mState = new EDACSDecoderState(Channel.ChannelType.STANDARD, mManager, config);
        mState.receiveDecoderStateEvent(new DecoderStateEvent(this, DecoderStateEvent.Event.NOTIFICATION_SOURCE_FREQUENCY,
            State.CONTROL, 853_725_000L));
    }

    private void call(int mt1, int lcn, boolean update, int group, int source, boolean emergency)
    {
        int message1 = (mt1 << 23) | (lcn << 17) | ((update ? 1 : 0) << 16) | group;
        int message2 = (1 << 21) | ((emergency ? 1 : 0) << 20) | source;
        mState.receive(new EDACSMessage(message1, message2, mNow[0], false));
    }

    private void advance(long ms)
    {
        mNow[0] += ms;
        mManager.getHeartbeatListener().receive(new Heartbeat());
    }

    private static Identifier get(ChannelStartProcessingRequest request, Role role)
    {
        List<Identifier> identifiers = request.getIdentifierCollection().getIdentifiers(role);
        return identifiers.isEmpty() ? null : identifiers.get(0);
    }

    /**
     * Test: a ProVoice assignment starts a ProVoice voice channel on the LCN's frequency with group and unit; updates
     * keep it; it closes once the updates stop.
     */
    @Test
    void testProVoiceCall()
    {
        setup();
        call(EDACSMessage.MT1_DIGITAL_GROUP_VOICE, 5, false, 529, 13254, false);
        assertEquals(1, mStarts.size());
        ChannelStartProcessingRequest start = mStarts.get(0);
        assertEquals(851_937_500L, ((SourceConfigTuner)start.getChannel().getSourceConfiguration()).getFrequency());
        assertEquals(DecodeConfigEDACS.VoiceMode.PROVOICE,
            ((DecodeConfigEDACS)start.getChannel().getDecodeConfiguration()).getVoiceMode());
        assertEquals(529, get(start, Role.TO).getValue());
        assertEquals(Protocol.EDACS, get(start, Role.TO).getProtocol());
        assertEquals(13254, get(start, Role.FROM).getValue());
        assertEquals("PROVOICE", mEvents.get(0).getDetails());

        for(int x = 0; x < 20; x++)
        {
            advance(300);
            call(EDACSMessage.MT1_DIGITAL_GROUP_VOICE, 5, true, 529, 13254, false);
        }
        assertEquals(1, mStarts.size());
        assertTrue(mChannelEvents.isEmpty());

        advance(EDACSTrafficChannelManager.CALL_END_MS - 300);
        assertTrue(mChannelEvents.isEmpty());
        advance(600);
        assertEquals(1, mChannelEvents.size());
        assertEquals(ChannelEvent.Event.REQUEST_DISABLE, mChannelEvents.get(0).getEvent());
    }

    /**
     * Test: an analog assignment starts an analog voice channel; status LCNs, LCNs missing from the list and the
     * control channel LCN start nothing.
     */
    @Test
    void testAnalogAndIgnored()
    {
        setup();
        call(EDACSMessage.MT1_ANALOG_GROUP_VOICE, 3, false, 273, 117, true);
        assertEquals(1, mStarts.size());
        assertEquals(DecodeConfigEDACS.VoiceMode.ANALOG,
            ((DecodeConfigEDACS)mStarts.get(0).getChannel().getDecodeConfiguration()).getVoiceMode());
        assertEquals("EMERGENCY ANALOG", mEvents.get(0).getDetails());

        call(EDACSMessage.MT1_DIGITAL_GROUP_VOICE, 28, false, 273, 3411, false); //status value
        call(EDACSMessage.MT1_DIGITAL_GROUP_VOICE, 10, false, 321, 113, false);  //the control channel itself
        assertEquals(1, mStarts.size());
        assertEquals(1, mEvents.size());

        call(EDACSMessage.MT1_DIGITAL_GROUP_VOICE, 20, false, 321, 113, false);  //no frequency for LCN 20
        assertEquals(1, mStarts.size());
        assertTrue(mEvents.get(mEvents.size() - 1).getDetails().contains("NO FREQUENCY FOR LCN 20"));
    }

    /**
     * Test: the voice channel for a call is ProVoice or analog per call - the same pooled channel switches
     */
    @Test
    void testModePerCall()
    {
        setup();
        call(EDACSMessage.MT1_ANALOG_GROUP_VOICE, 3, false, 273, 117, false);
        Channel voice = mStarts.get(0).getChannel();
        advance(EDACSTrafficChannelManager.CALL_END_MS + 500);
        mManager.getChannelEventListener().receive(new ChannelEvent(voice, ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));

        for(int x = 0; x < 30 && mStarts.size() < 30; x++)
        {
            call(EDACSMessage.MT1_DIGITAL_GROUP_VOICE, 2 + (x % 8), false, 600 + x, 100 + x, false);
        }

        assertTrue(mStarts.stream().filter(s -> s.getChannel() == voice).anyMatch(s ->
            ((DecodeConfigEDACS)s.getChannel().getDecodeConfiguration()).getVoiceMode() ==
                DecodeConfigEDACS.VoiceMode.PROVOICE), "pooled channel reused for ProVoice");
    }

    /**
     * Test: channel events for other systems' channels never wait on the manager's lock.  Cambridge 2026-11-10 08:45:
     * the EDACS control channel thread held the manager's lock while asking the channel processing manager to start a
     * voice channel, while a P25 channel thread held the channel processing manager's lock and was delivering a P25
     * channel event to this manager - a deadlock that stopped both systems.
     */
    @Test
    void testNoDeadlockWithChannelProcessingManager() throws Exception
    {
        setup();
        call(EDACSMessage.MT1_DIGITAL_GROUP_VOICE, 5, false, 529, 13254, false);
        assertEquals(1, mStarts.size());
        Channel voice = mStarts.get(0).getChannel();
        advance(EDACSTrafficChannelManager.CALL_END_MS + 500);
        assertEquals(ChannelEvent.Event.REQUEST_DISABLE, mChannelEvents.get(mChannelEvents.size() - 1).getEvent());
        mManager.getChannelEventListener().receive(new ChannelEvent(voice, ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));

        //Another thread holds the manager's lock - an event for another system's channel must not wait for it
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            synchronized(mManager)
            {
                locked.countDown();
                try
                {
                    release.await(5, TimeUnit.SECONDS);
                }
                catch(InterruptedException ie)
                {
                    //done
                }
            }
        });
        holder.start();
        assertTrue(locked.await(5, TimeUnit.SECONDS));
        Channel other = new Channel("P25", Channel.ChannelType.TRAFFIC);
        CountDownLatch delivered = new CountDownLatch(1);
        Thread p25 = new Thread(() -> {
            mManager.getChannelEventListener().receive(new ChannelEvent(other, ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));
            delivered.countDown();
        });
        p25.start();
        boolean done = delivered.await(2, TimeUnit.SECONDS);
        release.countDown();
        holder.join(5000);
        p25.join(5000);
        assertTrue(done, "event for another channel waited on the manager's lock");
    }

    /**
     * Test: "ignore unaliased talkgroups" - a call to a group with no alias is listed but no voice channel is started;
     * a group with an alias is followed; the activity summary shows the alias name.
     */
    @Test
    void testIgnoreUnaliasedTalkgroups()
    {
        setup(true);
        io.github.dsheirer.alias.AliasList aliasList = new io.github.dsheirer.alias.AliasList("test");
        io.github.dsheirer.alias.Alias alias = new io.github.dsheirer.alias.Alias("Red Line");
        alias.addAliasID(new io.github.dsheirer.alias.id.talkgroup.Talkgroup(Protocol.EDACS, 529));
        aliasList.addAlias(alias);
        mState.setAliasList(aliasList);

        call(EDACSMessage.MT1_DIGITAL_GROUP_VOICE, 5, false, 530, 13254, false);
        assertEquals(0, mStarts.size(), "no alias - not followed");
        assertEquals(1, mEvents.size(), "but listed");
        assertEquals("PROVOICE NO ALIAS - NOT FOLLOWED", mEvents.get(0).getDetails());
        assertEquals(1, mState.getUnaliasedCount());

        call(EDACSMessage.MT1_DIGITAL_GROUP_VOICE, 6, false, 529, 13254, false);
        assertEquals(1, mStarts.size(), "aliased group is followed");
        assertTrue(mState.getActivitySummary().contains("Red Line"), mState.getActivitySummary());
    }

    /**
     * Test: with the switch off (the default, and every existing playlist) an alias list changes nothing.
     */
    @Test
    void testUnaliasedFollowedByDefault()
    {
        setup();
        mState.setAliasList(new io.github.dsheirer.alias.AliasList("test"));
        call(EDACSMessage.MT1_DIGITAL_GROUP_VOICE, 5, false, 530, 13254, false);
        assertEquals(1, mStarts.size());
    }
}
