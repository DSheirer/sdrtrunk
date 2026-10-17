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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannel;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannelLookup;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNEncryptionKey;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallAssignment;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallTimer;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.VoiceCallOption;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NXDNTrafficChannelManagerTest
{
    @Test
    void publishesVoiceAssignmentWithoutFrequencyMapEntry()
    {
        NXDNTrafficChannelManager manager = createManager();
        List<IDecodeEvent> events = new ArrayList<>();
        manager.addDecodeEventListener(events::add);

        manager.processVoiceCallAssignment(createUnmappedAssignment(12, 1_000));

        assertEquals(1, events.size());
        assertEquals(0, events.get(0).getChannelDescriptor().getDownlinkFrequency());
        assertEquals("CHANNEL-MODE [12] IS MISSING CHANNEL:FREQUENCY MAPPING",
                events.get(0).getChannelDescriptor().toString());
    }

    @Test
    void tracksUnmappedChannelAssignmentsAndKeepsLogicalChannelsSeparate()
    {
        NXDNTrafficChannelManager manager = createManager();
        List<IDecodeEvent> events = new ArrayList<>();
        manager.addDecodeEventListener(events::add);
        NXDNChannelLookup channel12 = new NXDNChannelLookup(12);
        NXDNChannelLookup channel13 = new NXDNChannelLookup(13);

        assertEquals(-13, NXDNTrafficChannelManager.getTrackerKey(channel12));
        assertEquals(-14, NXDNTrafficChannelManager.getTrackerKey(channel13));

        observe(manager, channel12, 1_000);
        observe(manager, channel12, 1_100);

        assertEquals(2, events.size());
        assertSame(events.get(0), events.get(1));
        assertSame(channel12, events.get(0).getChannelDescriptor());
        assertEquals(100, events.get(0).getDuration());

        observe(manager, channel13, 1_200);

        assertEquals(3, events.size());
        assertNotSame(events.get(0), events.get(2));
        assertSame(channel13, events.get(2).getChannelDescriptor());
    }

    @Test
    void suppressesResidualAssignmentsAfterTrafficRelease()
    {
        NXDNTrafficChannelManager manager = createManager();
        List<IDecodeEvent> events = new ArrayList<>();
        manager.addDecodeEventListener(events::add);
        NXDNChannelLookup channel = new NXDNChannelLookup(12);
        channel.receive(null, Map.of(12, new ChannelFrequency(12, 451_000_000, 456_000_000)));

        observe(manager, channel, 1_000);
        IDecodeEvent original = events.get(0);
        manager.processCallProgressUpdate(channel, 1_500);
        manager.processEndCall(channel, 1_600);
        observe(manager, channel, 1_700);

        assertEquals(4, events.size());
        assertSame(original, events.get(3));
        assertEquals(600, original.getDuration());

        observe(manager, channel, 4_701);

        assertEquals(5, events.size());
        assertNotSame(original, events.get(4));
    }

    private static NXDNTrafficChannelManager createManager()
    {
        DecodeConfigNXDN configuration = new DecodeConfigNXDN();
        configuration.setTrafficChannelPoolSize(0);
        Channel parent = new Channel("NXDN Test", Channel.ChannelType.STANDARD);
        parent.setDecodeConfiguration(configuration);
        return new NXDNTrafficChannelManager(parent);
    }

    private static void observe(NXDNTrafficChannelManager manager, NXDNChannel channel, long timestamp)
    {
        List<Identifier> identifiers = List.of(NXDNRadioIdentifier.createFrom(1001),
                NXDNTalkgroupIdentifier.createTo(2001),
                EncryptionKeyIdentifier.create(Protocol.NXDN, NXDNEncryptionKey.create(0, 0)));
        manager.processVoiceCall(identifiers, channel, CallType.GROUP_BROADCAST,
                (EncryptionKeyIdentifier)identifiers.get(2), timestamp, new VoiceCallOption(0), CallTimer.UNSPECIFIED);
    }

    private static VoiceCallAssignment createUnmappedAssignment(int channel, long timestamp)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(128);
        message.setInt(1001, IntField.length16(24));
        message.setInt(2001, IntField.length16(40));
        message.setInt(channel, IntField.length10(62));
        VoiceCallAssignment assignment = new VoiceCallAssignment(message, timestamp,
                NXDNMessageType.CONTROL_OUT_04_CC_VOICE_CALL_ASSIGNMENT, 1,
                LICH.RCCH_OUTBOUND_SINGLE_CAC_NORMAL);
        assignment.receive(new ChannelAccessInformation(new CorrectedBinaryMessage(8), 0), Map.of());
        return assignment;
    }
}
