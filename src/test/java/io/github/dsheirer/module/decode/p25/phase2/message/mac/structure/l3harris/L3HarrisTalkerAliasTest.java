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
package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests decoding P25 Phase 2 L3Harris talker alias MAC structures, using structures captured from a Duke Energy
 * Florida recording: opcode A8, vendor A4 (L3Harris), length 0x11 (17 octets), then 14 alias characters padded with
 * zero bytes.
 */
public class L3HarrisTalkerAliasTest
{
    /**
     * Creates the alias structure at bit offset 8 (after the MAC header octet) in a FACCH or SACCH sized message.
     */
    private static L3HarrisTalkerAlias create(int messageBits, int... structure)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(messageBits);

        for(int x = 0; x < structure.length && (x + 2) * 8 <= messageBits; x++)
        {
            message.load((x + 1) * 8, 8, structure[x]);
        }

        return new L3HarrisTalkerAlias(message, 8);
    }

    @Test
    public void allFourteenCharactersAreDecoded()
    {
        //FACCH: FL-DCC-02_StPe
        L3HarrisTalkerAlias alias = create(156, 0xA8, 0xA4, 0x11, 0x46, 0x4C, 0x2D, 0x44, 0x43, 0x43, 0x2D, 0x30, 0x32,
            0x5F, 0x53, 0x74, 0x50, 0x65, 0xC5);
        assertEquals("FL-DCC-02_StPe", alias.getAlias().getValue());

        //SACCH: LBVA-17168-SBK
        alias = create(180, 0xA8, 0xA4, 0x11, 0x4C, 0x42, 0x56, 0x41, 0x2D, 0x31, 0x37, 0x31, 0x36, 0x38, 0x2D, 0x53,
            0x42, 0x4B, 0x00, 0x00, 0x00, 0xA7);
        assertEquals("LBVA-17168-SBK", alias.getAlias().getValue());
    }

    @Test
    public void zeroPaddingEndsTheAlias()
    {
        //A shorter alias padded with zero bytes, followed by unrelated bytes after the structure
        L3HarrisTalkerAlias alias = create(156, 0xA8, 0xA4, 0x11, 0x46, 0x4C, 0x2D, 0x44, 0x43, 0x43, 0x2D, 0x30, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x44);
        assertEquals("FL-DCC-0", alias.getAlias().getValue());
    }
}
