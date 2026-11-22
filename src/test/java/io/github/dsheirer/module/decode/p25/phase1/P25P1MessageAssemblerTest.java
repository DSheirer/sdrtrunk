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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.symbol.Dibit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit tests for the P25 Phase 1 message assembler forced completion of place holder (failed NID) assemblies.
 */
public class P25P1MessageAssemblerTest
{
    private static final P25P1DataUnitID TDU = P25P1DataUnitID.TERMINATOR_DATA_UNIT;
    private static final P25P1DataUnitID TDULC = P25P1DataUnitID.TERMINATOR_DATA_UNIT_LINK_CONTROL;
    private static final P25P1DataUnitID TSBK = P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_1;

    /**
     * Creates a place holder assembly with the bit length.
     */
    private static P25P1MessageAssembler getPlaceHolder(int bitLength)
    {
        P25P1MessageAssembler assembler = new P25P1MessageAssembler(0x293, P25P1DataUnitID.PLACE_HOLDER);

        for(int x = 0; x < bitLength / 2; x++)
        {
            assembler.receive(Dibit.D01_PLUS_3);
        }

        return assembler;
    }

    /**
     * Forces completion of a place holder with the bit length.
     * @return the identified data unit ID
     */
    private static P25P1DataUnitID forceCompletion(int bitLength, P25P1DataUnitID previous, P25P1DataUnitID next)
    {
        P25P1MessageAssembler assembler = getPlaceHolder(bitLength);
        assembler.forceCompletion(previous, next);
        return assembler.getDataUnitID();
    }

    /**
     * Test: a place holder that's shorter than a missed TDU frame (ie inter-frame padding or timing slack ahead of the
     * next sync) isn't identified as a TDU, even when followed by an LDU1 (HDU resolution) or a TSBK.
     */
    @Test
    void testShortPlaceHolderDiscarded()
    {
        for(int length : new int[]{0, 2, 28, 100, 126})
        {
            P25P1MessageAssembler assembler = getPlaceHolder(length);
            assertEquals(0, assembler.forceCompletion(TDULC, TDULC), "Dropped bits at " + length);
            assertEquals(P25P1DataUnitID.PLACE_HOLDER, assembler.getDataUnitID(), "Length " + length);
            assertEquals(P25P1DataUnitID.PLACE_HOLDER, forceCompletion(length, P25P1DataUnitID.HEADER_DATA_UNIT,
                    P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1), "Length " + length + " before LDU1");
        }
    }

    /**
     * Test: a place holder with the length of a single missed TDU frame (28 null bits plus the next frame's 112 sync and
     * NID bits) is identified as a TDU.
     */
    @Test
    void testMissedTDU()
    {
        for(int length : new int[]{128, 136, 140})
        {
            assertEquals(TDU, forceCompletion(length, TDU, TDU), "Length " + length);
            assertEquals(TDU, forceCompletion(length, TDULC, TDU), "Length " + length);
        }

        assertEquals(P25P1DataUnitID.PLACE_HOLDER, forceCompletion(144, TDU, TDU), "Length 144");
    }

    /**
     * Test: a place holder with the length of a single missed TDULC frame (308 message and null bits plus the next
     * frame's 112 sync and NID bits) is identified as a TDULC.
     */
    @Test
    void testMissedTDULC()
    {
        for(int length : new int[]{408, 416, 420})
        {
            assertEquals(TDULC, forceCompletion(length, TDULC, TDULC), "Length " + length);
        }
    }

    /**
     * Test: a place holder that doesn't match a missed frame isn't identified as a TDU.
     */
    @Test
    void testUnidentifiedPlaceHolderDiscarded()
    {
        for(int length : new int[]{208, 276, 556, 696, 1000})
        {
            P25P1MessageAssembler assembler = getPlaceHolder(length);
            assertEquals(0, assembler.forceCompletion(TDULC, TDU), "Dropped bits at " + length);
            assertEquals(P25P1DataUnitID.PLACE_HOLDER, assembler.getDataUnitID(), "Length " + length);
        }

        //A place holder between voice frames that is too short for an LDU
        assertEquals(P25P1DataUnitID.PLACE_HOLDER, forceCompletion(1000, P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2,
                P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1), "Length 1000 between LDUs");
    }

    /**
     * Test: place holders are resolved from the next data unit ID before the missed frame lengths are applied, so a
     * place holder ahead of a TSBK is a TSBK, even when its length matches a missed TDULC frame.
     */
    @Test
    void testResolvedFromNextDataUnit()
    {
        assertEquals(TSBK, forceCompletion(350, TSBK, TSBK), "Missed TSBK");
        assertEquals(TSBK, forceCompletion(416, TSBK, TSBK), "Missed TSBK with TDULC length");
        assertEquals(P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2, forceCompletion(1560,
                P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1, P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1), "Missed LDU2");
    }
}
