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

package io.github.dsheirer.module.decode.p25.phase1.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.hdu.HDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDULCMessage;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the message validity of P25 Phase 1 messages that decode their error protected content on demand.
 * The validity must reflect the error correction result on the first isValid() call, before any other accessor has
 * decoded the content.
 */
public class P25P1MessageValidityTest
{
    /**
     * Creates a message with all-zero bits (a valid codeword for the linear Golay and Reed-Solomon codes).
     */
    private static CorrectedBinaryMessage getZeroMessage(P25P1DataUnitID dataUnitID)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(dataUnitID.getMessageLength());
        message.setSize(dataUnitID.getMessageLength());
        return message;
    }

    /**
     * Creates a message with random bits.
     */
    private static CorrectedBinaryMessage getRandomMessage(P25P1DataUnitID dataUnitID, long seed)
    {
        Random random = new Random(seed);
        CorrectedBinaryMessage message = getZeroMessage(dataUnitID);

        for(int x = 0; x < message.size(); x++)
        {
            message.set(x, random.nextBoolean());
        }

        return message;
    }

    /**
     * Test: a TDULC is valid when its link control word passes error correction.
     */
    @Test
    void testValidTDULC()
    {
        TDULCMessage tdulc = new TDULCMessage(getZeroMessage(P25P1DataUnitID.TERMINATOR_DATA_UNIT_LINK_CONTROL),
                0x293, 0L);
        assertTrue(tdulc.isValid(), "TDULC");
        assertTrue(tdulc.getLinkControlWord().isValid(), "Link control word");
    }

    /**
     * Test: a TDULC is invalid when its link control word fails error correction.
     */
    @Test
    void testInvalidTDULC()
    {
        for(long seed = 1; seed <= 10; seed++)
        {
            TDULCMessage tdulc = new TDULCMessage(getRandomMessage(
                    P25P1DataUnitID.TERMINATOR_DATA_UNIT_LINK_CONTROL, seed), 0x293, 0L);
            assertFalse(tdulc.isValid(), "TDULC with random bits, seed " + seed);
            assertFalse(tdulc.getLinkControlWord().isValid(), "Link control word, seed " + seed);
        }
    }

    /**
     * Test: an HDU is valid when its header data passes error correction.
     */
    @Test
    void testValidHDU()
    {
        HDUMessage hdu = new HDUMessage(getZeroMessage(P25P1DataUnitID.HEADER_DATA_UNIT), 0x293, 0L);
        assertTrue(hdu.isValid(), "HDU");
        assertTrue(hdu.getHeaderData().isValid(), "Header data");
    }

    /**
     * Test: an HDU is invalid when its header data fails error correction, including when isValid() is invoked before
     * the header data is accessed.
     */
    @Test
    void testInvalidHDU()
    {
        for(long seed = 1; seed <= 10; seed++)
        {
            HDUMessage hdu = new HDUMessage(getRandomMessage(P25P1DataUnitID.HEADER_DATA_UNIT, seed), 0x293, 0L);
            assertFalse(hdu.isValid(), "HDU with random bits, seed " + seed);
            assertFalse(hdu.getHeaderData().isValid(), "Header data, seed " + seed);
        }
    }
}
