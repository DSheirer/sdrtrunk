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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.CRC16;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JUnit tests for finding the end of a reassembled Motorola talker alias.  The alias is the 56-bit WACN, system and
 * radio ID, then 16-bit encoded characters, then a CRC-16, then zero padding to the end of the last data block.
 */
public class LCMotorolaTalkerAliasAssemblerTest
{
    private static final int SUID_LENGTH = 56;
    private static final int PADDED_LENGTH = 4 * 44; //Four phase 1 data blocks

    /**
     * Alias payload (SUID and encoded characters) without the CRC.
     */
    private static BinaryMessage payload(int... chunks)
    {
        BinaryMessage message = new BinaryMessage(PADDED_LENGTH);
        message.load(0, 20, 0xBEE00);
        message.load(20, 12, 0x123);
        message.load(32, 24, 1234567);

        int offset = SUID_LENGTH;

        for(int chunk : chunks)
        {
            message.load(offset, 16, chunk);
            offset += 16;
        }

        return message;
    }

    /**
     * CRC-16/GSM (x^16 + x^12 + x^5 + 1, zero preset, inverted) over the first bit count bits.
     */
    private static int crc(BinaryMessage message, int bits)
    {
        int crc = 0;

        for(int offset = 0; offset < bits; offset += 8)
        {
            crc ^= (message.getByte(offset) & 0xFF) << 8;

            for(int bit = 0; bit < 8; bit++)
            {
                crc = ((crc & 0x8000) != 0) ? ((crc << 1) ^ 0x1021) & 0xFFFF : (crc << 1) & 0xFFFF;
            }
        }

        return crc ^ 0xFFFF;
    }

    /**
     * Padded alias with the encoded characters and their CRC.
     */
    private static BinaryMessage alias(int... chunks)
    {
        BinaryMessage message = payload(chunks);
        int end = SUID_LENGTH + chunks.length * 16;
        message.load(end, 16, crc(message, end));

        BinaryMessage unpadded = message.copy();
        unpadded.setSize(end + 16);
        assertTrue(CRC16.check(unpadded), "The test CRC must pass sdrtrunk's own CRC check");
        return message;
    }

    private static int crcOf(int... chunks)
    {
        return crc(payload(chunks), SUID_LENGTH + chunks.length * 16);
    }

    private static void assertTrimmed(BinaryMessage message, int characters)
    {
        LCMotorolaTalkerAliasAssembler.trimTalkerAliasLength(message);
        assertEquals(SUID_LENGTH + (characters + 1) * 16, message.size(), "Trimmed length");
        assertTrue(CRC16.check(message), "CRC check after trimming");
    }

    @Test
    public void aliasWithANonZeroCrcOctetIsTrimmedToItsCrc()
    {
        for(int last = 1; last < 0x10000; last++)
        {
            int crc = crcOf(0x1234, 0x5678, last);

            if((crc & 0xFF) != 0 && (crc & 0xFF00) != 0)
            {
                assertTrimmed(alias(0x1234, 0x5678, last), 3);
                return;
            }
        }

        fail("No test alias found");
    }

    @Test
    public void aliasWhoseCrcEndsInAZeroOctetKeepsIt()
    {
        for(int last = 1; last < 0x10000; last++)
        {
            int crc = crcOf(0x1234, 0x5678, last);

            if((crc & 0xFF) == 0 && (crc & 0xFF00) != 0)
            {
                assertTrimmed(alias(0x1234, 0x5678, last), 3);
                return;
            }
        }

        fail("No test alias found");
    }

    @Test
    public void aliasWhoseCrcIsZeroKeepsIt()
    {
        for(int last = 1; last < 0x10000; last++)
        {
            if(crcOf(0x1234, last) == 0)
            {
                assertTrimmed(alias(0x1234, last), 2);
                return;
            }
        }

        fail("No test alias found");
    }
}
