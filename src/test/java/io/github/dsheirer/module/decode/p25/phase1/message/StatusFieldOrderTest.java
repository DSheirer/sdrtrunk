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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCStatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.UnconfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.StatusUpdate;
import org.junit.jupiter.api.Test;

/**
 * TIA-102.AABC-E 2.3.29: the 2-octet Status field carries User Status in the first (upper) octet and Unit Status in
 * the second (lower) octet.  Each message below carries Status = 0x1234, so User Status = 0x12 and Unit Status = 0x34.
 */
public class StatusFieldOrderTest
{
    private static CorrectedBinaryMessage load(String hex)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(hex.length() * 4);
        for(int x = 0; x < hex.length(); x++)
        {
            int nibble = Character.digit(hex.charAt(x), 16);
            for(int bit = 0; bit < 4; bit++)
            {
                if((nibble & (0x8 >> bit)) != 0)
                {
                    message.set(x * 4 + bit);
                }
            }
        }
        return message;
    }

    @Test
    void abbreviatedStatusUpdate()
    {
        //STS_UPDT: Status oct 2-3, target oct 4-6, source oct 7-9
        StatusUpdate update = new StatusUpdate(P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_1,
            load("180012340000C80000640000"), 0x3C0, 0L);
        assertEquals(0x12, (int)(Integer)update.getUserStatus().getValue());
        assertEquals(0x34, (int)(Integer)update.getUnitStatus().getValue());
    }

    @Test
    void extendedStatusUpdate()
    {
        //Header: target oct 3-5, source WACN b19-b4 oct 8-9.  Block 0: WACN b3-b0 + system, source ID, Status oct 5-6.
        PDUSequence sequence = new PDUSequence(new AMBTCHeader(load("37FD000000C88218BEE00000"), true), 0L, 0x3C0);
        sequence.addDataBlock(new TestBlock(load("086000006412340000000000")));
        sequence.addDataBlock(new TestBlock(load("64BEE008600000C800000000")));
        AMBTCStatusUpdate update = new AMBTCStatusUpdate(sequence, 0x3C0, 0L);
        assertEquals(0x12, (int)(Integer)update.getUserStatus().getValue());
        assertEquals(0x34, (int)(Integer)update.getUnitStatus().getValue());
    }

    private static class TestBlock extends UnconfirmedDataBlock
    {
        private final CorrectedBinaryMessage mMessage;

        private TestBlock(CorrectedBinaryMessage message)
        {
            super(new float[98]);
            mMessage = message;
        }

        @Override
        public CorrectedBinaryMessage getMessage()
        {
            return mMessage;
        }
    }
}
