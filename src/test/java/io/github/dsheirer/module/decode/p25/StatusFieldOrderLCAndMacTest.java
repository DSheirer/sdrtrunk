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

package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWordFactory;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCStatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCStatusUpdateExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateExtendedVCH;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * TIA-102.AABC-D 2.3.29 (Figure 2.3.29-1): the 16-bit Status field is User Status in octet s and Unit Status in octet
 * s+1.  The voice link control (TIA-102.AABF-D 7.3.18, 7.3.30) and Phase 2 MAC (TIA-102.BBAD-A 3.41, Figures 91-93)
 * status updates carry the same Status field.  Each message here carries Status 0x1234: User 0x12, Unit 0x34.
 */
public class StatusFieldOrderLCAndMacTest
{
    private static final int TARGET = 0x123456;
    private static final int SOURCE = 0x789ABC;
    private static final int WACN = 0xBEE00;
    private static final int SYSTEM = 0x38D;
    private static final int SOURCE_ID = 0x00ABCD;

    private static CorrectedBinaryMessage message(int... octets)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(octets.length * 8);

        for(int x = 0; x < octets.length; x++)
        {
            message.load(x * 8, 8, octets[x]);
        }

        return message;
    }

    private static void assertStatus(Object user, Object unit)
    {
        assertEquals(0x12, user);
        assertEquals(0x34, unit);
    }

    private static void assertSuid(APCO25FullyQualifiedRadioIdentifier suid)
    {
        assertEquals(WACN, suid.getWacn());
        assertEquals(SYSTEM, suid.getSystem());
        assertEquals(SOURCE_ID, suid.getRadio());
    }

    @Test
    public void linkControlStatusUpdate()
    {
        //AABF-D 7.3.18, LCO 20 SF=1: status octets 1-2, target 3-5, source 6-8
        LCStatusUpdate update = assertInstanceOf(LCStatusUpdate.class, LinkControlWordFactory.create(
            message(0x40 | 20, 0x12, 0x34, 0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC), 0, false));
        assertStatus(update.getUserStatus().getValue(), update.getUnitStatus().getValue());
        assertEquals(TARGET, update.getTargetAddress().getValue());
        assertEquals(SOURCE, update.getSourceAddress().getValue());
    }

    @Test
    public void linkControlStatusUpdateExtended()
    {
        //AABF-D 7.3.30, LCO 26 SF=1: status octets 1-2, target 3-5, source ID 6-8
        LCStatusUpdateExtended update = assertInstanceOf(LCStatusUpdateExtended.class, LinkControlWordFactory.create(
            message(0x40 | 26, 0x12, 0x34, 0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC), 0, false));
        assertStatus(update.getUserStatus().getValue(), update.getUnitStatus().getValue());
        assertEquals(TARGET, update.getTargetAddress().getValue());
    }

    @Test
    public void macStatusUpdateAbbreviated()
    {
        //BBAD-A Figure 91, MCO 0x58, length 10: status octets 3-4, target 5-7, source 8-10
        StatusUpdateAbbreviated update = new StatusUpdateAbbreviated(
            message(0x58, 10, 0x12, 0x34, 0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC), 0);
        assertStatus(update.getUserStatus().getValue(), update.getUnitStatus().getValue());
        assertEquals(TARGET, update.getTargetAddress().getValue());
        assertEquals(SOURCE, update.getSourceAddress().getValue());
    }

    @Test
    public void macStatusUpdateExtendedVCH()
    {
        //BBAD-A Figure 92, MCO 0xD8, length 14: status octets 3-4, target 5-7, source SUID 8-14
        StatusUpdateExtendedVCH update = new StatusUpdateExtendedVCH(
            message(0xD8, 14, 0x12, 0x34, 0x12, 0x34, 0x56, 0xBE, 0xE0, 0x03, 0x8D, 0x00, 0xAB, 0xCD), 0);
        assertStatus(update.getUserStatus().getValue(), update.getUnitStatus().getValue());
        assertEquals(TARGET, update.getTargetAddress().getValue());
        assertSuid(update.getSourceSUID());
    }

    @Test
    public void macStatusUpdateExtendedLCCH()
    {
        //BBAD-A Figure 93, MCO 0xD9, length 18, data length 25: status octets 4-5, target 6-8, source SUID 9-15,
        //source address 16-18
        StatusUpdateExtendedLCCH update = new StatusUpdateExtendedLCCH(
            message(0xD9, 18, 25, 0x12, 0x34, 0x12, 0x34, 0x56, 0xBE, 0xE0, 0x03, 0x8D, 0x00, 0xAB, 0xCD, 0x78, 0x9A,
                0xBC), 0);
        assertStatus(update.getUserStatus().getValue(), update.getUnitStatus().getValue());
        APCO25FullyQualifiedRadioIdentifier source = update.getSourceSUID();
        assertSuid(source);
        assertEquals(SOURCE, source.getValue());
    }
}
