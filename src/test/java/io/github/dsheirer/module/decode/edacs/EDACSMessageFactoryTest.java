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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.edacs.message.EDACSMessage;
import org.junit.jupiter.api.Test;

/**
 * Tests the dynamic regroup detail message (MT1 0x1F, MT2 0xC) with messages captured from a live Extended Addressing
 * system with DSD-FME, which decodes them as the expected values below.
 */
class EDACSMessageFactoryTest
{
    /**
     * Creates the 40-bit message (28 data bits and 12 BCH bits, left as zero) for a 28-bit value.
     */
    private static CorrectedBinaryMessage message(int value)
    {
        StringBuilder sb = new StringBuilder();

        for(int x = 27; x >= 0; x--)
        {
            sb.append(((value >> x) & 1) == 1 ? '1' : '0');
        }

        sb.append("000000000000");
        return CorrectedBinaryMessage.load(sb.toString());
    }

    private static EDACSMessage create(int msg1, int msg2, boolean esk)
    {
        return EDACSMessageFactory.create(message(msg1), message(msg2), 0, esk);
    }

    @Test
    void activePatch()
    {
        //DSD-FME: MSG_1 [FE700F5] MSG_2 [600531E] SP-WGID: 245; Target: 0021278; Patch Active; OPT: 7; SSN: 0C;
        EDACSMessage message = create(0xFE700F5, 0x600531E, false);
        assertEquals(EDACSMessageType.DYNAMIC_REGROUP_DETAIL, message.getMessageType());
        assertEquals(245, message.getGroup());
        assertEquals(21278, message.getTarget());
        assertEquals(7, message.getRegroupOption(), "option was read one bit high (as 14) before");
        assertEquals(0x0C, message.getRegroupSsn());
        assertFalse(message.isRegroupDelete());

        //DSD-FME: MSG_1 [FE70051] MSG_2 [5000463] SP-WGID: 081; Target: 0001123; Patch Active; OPT: 7; SSN: 0A;
        message = create(0xFE70051, 0x5000463, false);
        assertEquals(81, message.getGroup());
        assertEquals(1123, message.getTarget());
        assertEquals(7, message.getRegroupOption());
        assertEquals(0x0A, message.getRegroupSsn());
    }

    @Test
    void patchDelete()
    {
        //DSD-FME edacs-fme.c: MSG_1 [FE00045] MSG_2 [FE00045] - the supergroup as its own target deletes the patch
        EDACSMessage message = create(0xFE00045, 0xFE00045, false);
        assertEquals(EDACSMessageType.DYNAMIC_REGROUP_DETAIL, message.getMessageType());
        assertEquals(69, message.getGroup());
        assertEquals(0, message.getRegroupOption());
        assertTrue(message.isRegroupDelete());
        assertTrue(message.getDetails().contains("Patch Delete"));
    }

    @Test
    void activePatchOnEskControlChannel()
    {
        //ESK inverts the first 8 bits of each message with 0xA0
        int mask = DecodeConfigEDACS.ESK_MASK << 20;
        EDACSMessage message = create(0xFE700F5 ^ mask, 0x600531E ^ mask, true);
        assertEquals(EDACSMessageType.DYNAMIC_REGROUP_DETAIL, message.getMessageType());
        assertEquals(245, message.getGroup());
        assertEquals(21278, message.getTarget());
        assertEquals(7, message.getRegroupOption());
        assertEquals(0x0C, message.getRegroupSsn());
    }
}
