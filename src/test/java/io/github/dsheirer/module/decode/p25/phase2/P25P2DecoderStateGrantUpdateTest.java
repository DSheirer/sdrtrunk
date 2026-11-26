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

package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessageFactory;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacPduType;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateMultipleExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateMultipleImplicit;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * JUnit tests for the Phase 2 group voice channel grant updates: each group's update goes to the traffic channel
 * manager with that group's own channel (TIA-102.BBAD-A 3.6).
 */
public class P25P2DecoderStateGrantUpdateTest
{
    private static final int TIMESLOT = 1;
    private static final int SACCH_MAX_INDEX = 168;
    private static final int[] SACCH_CRC = {168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179};
    private static final int[] PDU_TYPE = {0, 1, 2};

    /**
     * Traffic channel manager that records the channel of each channel update.
     */
    private static class RecordingManager extends P25TrafficChannelManager
    {
        private final List<APCO25Channel> mChannels = new ArrayList<>();

        RecordingManager(Channel channel)
        {
            super(channel);
        }

        @Override
        public void processP2ChannelUpdate(APCO25Channel channel, ServiceOptions serviceOptions, IdentifierCollection ic,
                                           MacOpcode macOpcode, long timestamp, String context)
        {
            mChannels.add(channel);
        }
    }

    /**
     * Decodes a SACCH MAC ACTIVE PDU carrying the structure, with a valid CRC-12.
     */
    private static MacMessage message(int... structure)
    {
        CorrectedBinaryMessage pdu = new CorrectedBinaryMessage(180);
        pdu.setInt(MacPduType.MAC_4_ACTIVE.ordinal(), PDU_TYPE);

        int offset = MacMessageFactory.DEFAULT_MAC_STRUCTURE_INDEX;

        for(int octet : structure)
        {
            pdu.setByte(offset, (byte)octet);
            offset += 8;
        }

        for(int crc = 0; crc < 4096; crc++)
        {
            pdu.setInt(crc, SACCH_CRC);

            if(CRCP25.crc12_SACCH(pdu))
            {
                break;
            }
        }

        List<MacMessage> messages = MacMessageFactory.create(TIMESLOT, DataUnitID.UNSCRAMBLED_SACCH, pdu, 0L,
            SACCH_MAX_INDEX);
        assertEquals(1, messages.size(), "Expected one MAC structure: " + messages);
        return messages.get(0);
    }

    /**
     * Channels reported to the traffic channel manager for the message, in order.
     */
    private static List<APCO25Channel> reported(MacMessage message)
    {
        Channel channel = new Channel("Test");
        RecordingManager manager = new RecordingManager(channel);
        new P25P2DecoderState(channel, TIMESLOT, manager, new PatchGroupManager()).receive(message);
        return manager.mChannels;
    }

    //16-bit channel field: 4-bit frequency band, 12-bit channel number
    private static int high(int channel)
    {
        return (channel >> 8) & 0xFF;
    }

    private static int low(int channel)
    {
        return channel & 0xFF;
    }

    @Test
    public void multipleImplicitUpdateUsesEachGroupsChannel()
    {
        int ch1 = 0x1064, ch2 = 0x10C8, ch3 = 0x112C;

        //0x05: three of service options, channel, group
        MacMessage message = message(0x05,
            0x00, high(ch1), low(ch1), 0x03, 0xE9,
            0x00, high(ch2), low(ch2), 0x03, 0xEA,
            0x00, high(ch3), low(ch3), 0x03, 0xEB);
        GroupVoiceChannelGrantUpdateMultipleImplicit update = assertInstanceOf(
            GroupVoiceChannelGrantUpdateMultipleImplicit.class, message.getMacStructure());
        assertNotEquals(update.getChannel1(), update.getChannel2());

        assertEquals(List.of(update.getChannel1(), update.getChannel2(), update.getChannel3()), reported(message));
    }

    @Test
    public void multipleExplicitUpdateUsesEachGroupsChannel()
    {
        int tx1 = 0x1064, rx1 = 0x1065, tx2 = 0x10C8, rx2 = 0x10C9;

        //0x25: two of service options, transmit channel, receive channel, group
        MacMessage message = message(0x25,
            0x00, high(tx1), low(tx1), high(rx1), low(rx1), 0x03, 0xE9,
            0x00, high(tx2), low(tx2), high(rx2), low(rx2), 0x03, 0xEA);
        GroupVoiceChannelGrantUpdateMultipleExplicit update = assertInstanceOf(
            GroupVoiceChannelGrantUpdateMultipleExplicit.class, message.getMacStructure());
        assertNotEquals(update.getChannel1(), update.getChannel2());

        assertEquals(List.of(update.getChannel1(), update.getChannel2()), reported(message));
    }

    @Test
    public void phase1ImplicitUpdateUsesEachGroupsChannel()
    {
        int ch1 = 0x1064, ch2 = 0x10C8;

        //0x42: two of channel, group
        MacMessage message = message(0x42, high(ch1), low(ch1), 0x03, 0xE9, high(ch2), low(ch2), 0x03, 0xEA);
        GroupVoiceChannelGrantUpdateImplicit update = assertInstanceOf(GroupVoiceChannelGrantUpdateImplicit.class,
            message.getMacStructure());
        assertNotEquals(update.getChannel1(), update.getChannel2());

        assertEquals(List.of(update.getChannel1(), update.getChannel2()), reported(message));
    }
}
