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

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.IndirectGroupPagingWithoutPriority;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.IndividualPagingWithPriority;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PowerControlSignalQuality;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * JUnit tests for the paging MAC structures.  Per TIA-102.BBAD-A 3.37/3.38 (Tables 6 and 7) the LEN field is the number
 * of IDs less one: individual paging is 5/8/11/14 octets with 24-bit targets and indirect group paging is 4/6/8/10
 * octets with 16-bit group IDs, for LEN 0-3.
 */
public class MacPagingTest
{
    private static final int SACCH_MAX_INDEX = 168;
    private static final int[] SACCH_CRC = {168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179};
    private static final int[] PDU_TYPE = {0, 1, 2};
    private static final int FOLLOWER_TARGET = 4242;

    //Power control signal quality: opcode 0x30, target address 4242 in octets 2-4, RF level and BER in octet 5.
    private static final int[] POWER_CONTROL = {0x30, 0x00, 0x10, 0x92, 0x00};

    /**
     * Builds a SACCH MAC ACTIVE PDU carrying the structure followed by a power control structure, with a valid CRC-12.
     */
    private static List<MacMessage> decode(List<Integer> structure)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(180);
        message.setInt(MacPduType.MAC_4_ACTIVE.ordinal(), PDU_TYPE);

        int offset = MacMessageFactory.DEFAULT_MAC_STRUCTURE_INDEX;

        for(int octet : structure)
        {
            message.setByte(offset, (byte)octet);
            offset += 8;
        }

        for(int octet : POWER_CONTROL)
        {
            message.setByte(offset, (byte)octet);
            offset += 8;
        }

        //The CRC-12 covers bits 0-167 only, so search the 12-bit value that makes the PDU pass.
        for(int crc = 0; crc < 4096; crc++)
        {
            message.setInt(crc, SACCH_CRC);

            if(CRCP25.crc12_SACCH(message))
            {
                break;
            }
        }

        return MacMessageFactory.create(1, DataUnitID.UNSCRAMBLED_SACCH, message, 0L, SACCH_MAX_INDEX);
    }

    private static List<Object> values(List<Identifier> identifiers)
    {
        List<Object> values = new ArrayList<>();

        for(Identifier identifier : identifiers)
        {
            values.add(identifier.getValue());
        }

        return values;
    }

    private static void assertFollowerAligned(List<MacMessage> messages, int len)
    {
        assertEquals(2, messages.size(), "LEN " + len + ": expected the paging and the structure after it: " + messages);
        PowerControlSignalQuality follower = assertInstanceOf(PowerControlSignalQuality.class,
            messages.get(1).getMacStructure(), "LEN " + len);
        assertEquals(FOLLOWER_TARGET, follower.getTargetAddress().getValue(), "LEN " + len);
    }

    @Test
    public void individualPagingLengthAndTargetsForEachLen()
    {
        for(int len = 0; len <= 3; len++)
        {
            //Opcode 0x12, PRIO bits clear, LEN, then 24-bit targets 1001..1004
            List<Integer> structure = new ArrayList<>(List.of(0x12, len));
            List<Object> expected = new ArrayList<>();

            for(int id = 1; id <= len + 1; id++)
            {
                int target = 1000 + id;
                structure.addAll(List.of((target >> 16) & 0xFF, (target >> 8) & 0xFF, target & 0xFF));
                expected.add(target);
            }

            List<MacMessage> messages = decode(structure);
            IndividualPagingWithPriority paging = assertInstanceOf(IndividualPagingWithPriority.class,
                messages.get(0).getMacStructure(), "LEN " + len);
            assertEquals(len + 1, paging.getCount(), "LEN " + len);
            assertEquals(2 + 3 * (len + 1), paging.getLength(), "LEN " + len);
            assertEquals(expected, values(paging.getIdentifiers()), "LEN " + len);
            assertFollowerAligned(messages, len);
        }
    }

    @Test
    public void indirectGroupPagingLengthAndGroupsForEachLen()
    {
        for(int len = 0; len <= 3; len++)
        {
            //Opcode 0x11, LEN, then 16-bit group IDs 101..104
            List<Integer> structure = new ArrayList<>(List.of(0x11, len));
            List<Object> expected = new ArrayList<>();

            for(int id = 1; id <= len + 1; id++)
            {
                int group = 100 + id;
                structure.addAll(List.of((group >> 8) & 0xFF, group & 0xFF));
                expected.add(group);
            }

            List<MacMessage> messages = decode(structure);
            IndirectGroupPagingWithoutPriority paging = assertInstanceOf(IndirectGroupPagingWithoutPriority.class,
                messages.get(0).getMacStructure(), "LEN " + len);
            assertEquals(len + 1, paging.getCount(), "LEN " + len);
            assertEquals(2 + 2 * (len + 1), paging.getLength(), "LEN " + len);
            assertEquals(expected, values(paging.getIdentifiers()), "LEN " + len);
            assertFollowerAligned(messages, len);
        }
    }
}
