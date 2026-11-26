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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.UnconfirmedDataBlock;
import org.junit.jupiter.api.Test;

/**
 * MSG_UPDT extended format (TIA-102.AABC-E Fig 6.2.10-2): the target WACN is the 20 bits in the last data block
 * octets 1-3 (block bits 8-27).  WACN 0x92761 has bit 14 set and bit 13 clear, so a bit list that repeats 13 and skips
 * 14 decodes the wrong WACN.
 */
public class AMBTCMessageUpdateTest
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
    void targetWacnUsesAllTwentyBits()
    {
        PDUSequence sequence = new PDUSequence(new AMBTCHeader(load("37FD000000C8821C92760000"), true), 0L, 0x3C0);
        sequence.addDataBlock(new TestBlock(load("1E27000064ABCD0000000000")));
        sequence.addDataBlock(new TestBlock(load("6492761E270000C800000000")));
        AMBTCMessageUpdate update = new AMBTCMessageUpdate(sequence, 0x3C0, 0L);
        APCO25FullyQualifiedRadioIdentifier target = (APCO25FullyQualifiedRadioIdentifier)update.getTargetAddress();
        assertEquals(0x92761, target.getWacn());
        assertEquals(0xE27, target.getSystem());
        assertEquals(0xC8, target.getRadio());
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
