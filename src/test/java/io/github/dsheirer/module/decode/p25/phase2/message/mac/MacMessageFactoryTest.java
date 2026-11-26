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
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacRelease;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestExtended;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * JUnit tests for MAC structure lengths, which the factory uses to find the second and third structures in a PDU.
 * Lengths per TIA-102.BBAD-A Table 4 (outbound): unit to unit answer request abbreviated 8 and extended 12 octets,
 * telephone interconnect answer request 9 octets.
 */
public class MacMessageFactoryTest
{
    private static final int SACCH_MAX_INDEX = 168;
    private static final int[] SACCH_CRC = {168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179};
    private static final int[] PDU_TYPE = {0, 1, 2};
    private static final int RELEASE_TARGET = 4242;

    //MAC release: opcode 0x31, target address 4242 in octets 3-5.
    private static final int[] MAC_RELEASE = {0x31, 0x00, 0x00, 0x10, 0x92, 0x00, 0x00};

    /**
     * Builds a SACCH MAC ACTIVE PDU carrying the structure followed by a MAC release, with a valid CRC-12.
     */
    private static List<MacMessage> decode(int[] structure)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(180);
        message.setInt(MacPduType.MAC_4_ACTIVE.ordinal(), PDU_TYPE);

        int offset = MacMessageFactory.DEFAULT_MAC_STRUCTURE_INDEX;

        for(int octet : structure)
        {
            message.setByte(offset, (byte)octet);
            offset += 8;
        }

        for(int octet : MAC_RELEASE)
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

    private static void assertReleaseFollows(List<MacMessage> messages)
    {
        assertEquals(2, messages.size(), "Expected the structure and the MAC release that follows it: " + messages);
        MacRelease release = assertInstanceOf(MacRelease.class, messages.get(1).getMacStructure());
        assertEquals(RELEASE_TARGET, release.getTargetAddress().getValue());
    }

    @Test
    public void unitToUnitAnswerRequestAbbreviatedIs8Octets()
    {
        //Opcode, service options, target 1000, source 2000
        List<MacMessage> messages = decode(new int[]{0x45, 0x00, 0x00, 0x03, 0xE8, 0x00, 0x07, 0xD0});
        assertInstanceOf(UnitToUnitAnswerRequestAbbreviated.class, messages.get(0).getMacStructure());
        assertReleaseFollows(messages);
    }

    @Test
    public void unitToUnitAnswerRequestExtendedIs12Octets()
    {
        //Opcode, service options, target 1000, source SUID (WACN 0xBEE00, system 0x123, ID 2000)
        List<MacMessage> messages = decode(new int[]{0xC5, 0x00, 0x00, 0x03, 0xE8, 0xBE, 0xE0, 0x01, 0x23, 0x00,
            0x07, 0xD0});
        assertInstanceOf(UnitToUnitAnswerRequestExtended.class, messages.get(0).getMacStructure());
        assertReleaseFollows(messages);
    }

    @Test
    public void telephoneInterconnectAnswerRequestIs9OctetsWith24BitTarget()
    {
        //Opcode, digits 0123456789, target 6636321 (0x654321)
        List<MacMessage> messages = decode(new int[]{0x4A, 0x01, 0x23, 0x45, 0x67, 0x89, 0x65, 0x43, 0x21});
        TelephoneInterconnectAnswerRequest request = assertInstanceOf(TelephoneInterconnectAnswerRequest.class,
            messages.get(0).getMacStructure());
        assertEquals(0x654321, request.getTargetAddress().getValue());
        assertReleaseFollows(messages);
    }
}
