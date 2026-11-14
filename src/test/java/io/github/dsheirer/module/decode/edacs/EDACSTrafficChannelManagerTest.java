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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.edacs.channel.EDACSChannel;
import io.github.dsheirer.module.decode.edacs.identifier.EDACSRadio;
import io.github.dsheirer.module.decode.edacs.identifier.EDACSTalkgroup;
import io.github.dsheirer.module.decode.edacs.message.EDACSMessage;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Checks that one EDACS call produces one call event, whether or not its traffic channel can be started.
 */
class EDACSTrafficChannelManagerTest
{
    private static final long FREQUENCY = 851175000L;

    /**
     * Stands in for ChannelProcessingManager.  Rejects starts synchronously with TUNER UNAVAILABLE, like
     * ChannelProcessingManager does when no tuner can reach the frequency.
     */
    private static class FakeChannelProcessingManager
    {
        private final EDACSTrafficChannelManager mManager;
        private final boolean mReject;
        private int mStartRequests;
        private Channel mLastChannel;

        FakeChannelProcessingManager(EDACSTrafficChannelManager manager, boolean reject)
        {
            mManager = manager;
            mReject = reject;
        }

        @Subscribe
        public void start(ChannelStartProcessingRequest request)
        {
            mStartRequests++;
            mLastChannel = request.getChannel();

            if(mReject)
            {
                mManager.getChannelEventListener().receive(new ChannelEvent(request.getChannel(),
                    ChannelEvent.Event.NOTIFICATION_PROCESSING_START_REJECTED, "TUNER UNAVAILABLE"));
            }
        }
    }

    private static EDACSTrafficChannelManager manager(Set<IDecodeEvent> events)
    {
        EDACSTrafficChannelManager manager = new EDACSTrafficChannelManager(4);
        manager.createTrafficChannels(new Channel("MBTA", Channel.ChannelType.STANDARD), new DecodeConfigEDACS());
        manager.addDecodeEventListener(events::add);
        return manager;
    }

    private static EDACSChannel lcn1()
    {
        EDACSChannel channel = new EDACSChannel(1);
        channel.setFrequencyLookup(lcn -> FREQUENCY);
        return channel;
    }

    private static void grant(EDACSTrafficChannelManager manager, EDACSChannel channel, int talkgroup, long timestamp)
    {
        grant(manager, channel, talkgroup, timestamp, false);
    }

    private static void grant(EDACSTrafficChannelManager manager, EDACSChannel channel, int talkgroup, long timestamp,
                              boolean update)
    {
        EDACSMessage message = new EDACSMessage(EDACSMessageType.DIGITAL_GROUP_CALL, new CorrectedBinaryMessage(40),
            timestamp);
        message.setUpdate(update);
        IdentifierCollection ic = new IdentifierCollection(List.of(new EDACSTalkgroup(talkgroup, Role.TO)));
        manager.processChannelGrant(message, ic, channel);
    }

    @Test
    void repeatedGrantsAfterRejectedStartUpdateOneEvent()
    {
        Set<IDecodeEvent> events = Collections.newSetFromMap(new IdentityHashMap<>());
        EDACSTrafficChannelManager manager = manager(events);
        FakeChannelProcessingManager cpm = new FakeChannelProcessingManager(manager, true);
        EventBus bus = new EventBus();
        bus.register(cpm);
        manager.setInterModuleEventBus(bus);

        EDACSChannel channel = lcn1();
        long start = System.currentTimeMillis();

        //EDACS repeats the grant several times a second for the whole call
        for(int x = 0; x < 10; x++)
        {
            grant(manager, channel, 530, start + (x * 200));
        }

        assertEquals(1, events.size(), "one call should produce one event");
        assertEquals(1, cpm.mStartRequests, "repeated grants should not request another traffic channel");
    }

    @Test
    void trafficVoiceUpdatesTheGrantEvent()
    {
        Set<IDecodeEvent> events = Collections.newSetFromMap(new IdentityHashMap<>());
        EDACSTrafficChannelManager manager = manager(events);
        EventBus bus = new EventBus();
        bus.register(new FakeChannelProcessingManager(manager, false));
        manager.setInterModuleEventBus(bus);

        EDACSChannel channel = lcn1();
        long start = System.currentTimeMillis();
        grant(manager, channel, 530, start);
        IDecodeEvent grantEvent = events.iterator().next();

        manager.processTrafficVoice(FREQUENCY, start + 1500);

        assertEquals(1, events.size(), "traffic voice should update the grant event, not add one");
        assertSame(grantEvent, events.iterator().next());
        assertEquals(1500, grantEvent.getDuration());
    }

    @Test
    void sameTalkgroupAfterStaleGapIsANewCall()
    {
        Set<IDecodeEvent> events = Collections.newSetFromMap(new IdentityHashMap<>());
        EDACSTrafficChannelManager manager = manager(events);
        EventBus bus = new EventBus();
        bus.register(new FakeChannelProcessingManager(manager, true));
        manager.setInterModuleEventBus(bus);

        EDACSChannel channel = lcn1();
        long start = System.currentTimeMillis();
        grant(manager, channel, 530, start);
        grant(manager, channel, 530, start + 10000);

        assertEquals(2, events.size(), "a grant after the stale threshold should start a new event");
    }

    @Test
    void updatesTrailingAChannelDropDoNotRestartTheChannel()
    {
        Set<IDecodeEvent> events = Collections.newSetFromMap(new IdentityHashMap<>());
        EDACSTrafficChannelManager manager = manager(events);
        FakeChannelProcessingManager cpm = new FakeChannelProcessingManager(manager, false);
        EventBus bus = new EventBus();
        bus.register(cpm);
        manager.setInterModuleEventBus(bus);

        EDACSChannel channel = lcn1();
        long start = System.currentTimeMillis();
        grant(manager, channel, 530, start);
        manager.processTrafficVoice(FREQUENCY, start + 1000);

        //The site drops the channel and the traffic channel is torn down
        manager.processTrafficChannelDrop(FREQUENCY, start + 2000);
        manager.getChannelEventListener().receive(new ChannelEvent(cpm.mLastChannel,
            ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));

        //Updates for the call that just ended are still arriving from the control channel
        grant(manager, channel, 530, start + 2300, true);
        grant(manager, channel, 530, start + 2900, true);
        assertEquals(1, cpm.mStartRequests, "trailing updates should not start the traffic channel again");
        assertEquals(1, events.size(), "trailing updates should not create another call event");

        //An update for another talkgroup, or a new assignment, on that LCN is a call to follow
        grant(manager, channel, 531, start + 3000, true);
        assertEquals(2, cpm.mStartRequests, "an update for another talkgroup is a call in progress to join");

        manager.processTrafficChannelDrop(FREQUENCY, start + 4000);
        manager.getChannelEventListener().receive(new ChannelEvent(cpm.mLastChannel,
            ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));
        grant(manager, channel, 531, start + 4500, false);
        assertEquals(3, cpm.mStartRequests, "a new assignment straight after a drop is a new call");

        manager.processTrafficChannelDrop(FREQUENCY, start + 6000);
        manager.getChannelEventListener().receive(new ChannelEvent(cpm.mLastChannel,
            ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));
        grant(manager, channel, 531, start + 9000, true);
        assertEquals(4, cpm.mStartRequests, "an update long after the drop is a call in progress to join");
    }

    private static void grant(EDACSTrafficChannelManager manager, EDACSChannel channel, int talkgroup, int source,
                              long timestamp, boolean update)
    {
        EDACSMessage message = new EDACSMessage(EDACSMessageType.DIGITAL_GROUP_CALL, new CorrectedBinaryMessage(40),
            timestamp);
        message.setUpdate(update);
        IdentifierCollection ic = new IdentifierCollection(List.of(new EDACSTalkgroup(talkgroup, Role.TO),
            new EDACSRadio(source, Role.FROM)));
        manager.processChannelGrant(message, ic, channel);
    }

    @Test
    void updateFromAnotherRadioAfterAChannelDropIsANewCall()
    {
        Set<IDecodeEvent> events = Collections.newSetFromMap(new IdentityHashMap<>());
        EDACSTrafficChannelManager manager = manager(events);
        FakeChannelProcessingManager cpm = new FakeChannelProcessingManager(manager, false);
        EventBus bus = new EventBus();
        bus.register(cpm);
        manager.setInterModuleEventBus(bus);

        EDACSChannel channel = lcn1();
        long start = System.currentTimeMillis();
        grant(manager, channel, 530, 102, start, false);
        manager.processTrafficVoice(FREQUENCY, start + 1000);
        manager.processTrafficChannelDrop(FREQUENCY, start + 2000);
        manager.getChannelEventListener().receive(new ChannelEvent(cpm.mLastChannel,
            ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));

        //A trailing update for the call that ended
        grant(manager, channel, 530, 102, start + 2300, true);
        assertEquals(1, cpm.mStartRequests, "a trailing update from the same radio should not restart the channel");

        //Another radio answers on the same LCN and talkgroup, and its assignment was not decoded
        grant(manager, channel, 530, 3599, start + 2600, true);
        assertEquals(2, cpm.mStartRequests, "an update from another radio is a new call to join");
        assertEquals(2, events.size());
    }

    @Test
    void assignmentBeforeTheDroppedChannelStopsStartsWhenItHasStopped()
    {
        Set<IDecodeEvent> events = Collections.newSetFromMap(new IdentityHashMap<>());
        EDACSTrafficChannelManager manager = manager(events);
        FakeChannelProcessingManager cpm = new FakeChannelProcessingManager(manager, false);
        EventBus bus = new EventBus();
        bus.register(cpm);
        manager.setInterModuleEventBus(bus);

        EDACSChannel channel = lcn1();
        long start = System.currentTimeMillis();
        grant(manager, channel, 530, 102, start, false);
        manager.processTrafficVoice(FREQUENCY, start + 1000);
        manager.processTrafficChannelDrop(FREQUENCY, start + 2000);
        Channel dropped = cpm.mLastChannel;

        //The next over on the same LCN and talkgroup is assigned before the dropped channel has finished stopping
        grant(manager, channel, 530, 3599, start + 2100, false);
        assertEquals(1, cpm.mStartRequests, "the new call waits for the dropped channel to stop");

        manager.getChannelEventListener().receive(new ChannelEvent(dropped,
            ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));
        assertEquals(2, cpm.mStartRequests, "the new call starts once the dropped channel has stopped");
        assertEquals(2, events.size(), "the new call has its own event");
    }

    @Test
    void trafficCallEndEndsTheGrantEvent()
    {
        Set<IDecodeEvent> events = Collections.newSetFromMap(new IdentityHashMap<>());
        EDACSTrafficChannelManager manager = manager(events);
        EventBus bus = new EventBus();
        bus.register(new FakeChannelProcessingManager(manager, false));
        manager.setInterModuleEventBus(bus);

        EDACSChannel channel = lcn1();
        long start = System.currentTimeMillis();
        grant(manager, channel, 530, start);
        manager.processTrafficVoice(FREQUENCY, start + 1000);
        manager.processTrafficCallEnd(FREQUENCY, start + 1400);

        IDecodeEvent event = events.iterator().next();
        assertEquals(1, events.size());
        assertEquals(1400, event.getDuration(), "the call should end when the voice stops");
    }
}
