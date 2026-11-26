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

package io.github.dsheirer.module.decode.dmr.message.data.lc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRCrcMaskManager;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.FullLCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.GroupVoiceChannelUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the full link control validity flag set by the link control message factory.
 */
public class LCMessageFactoryTest
{
    //RS(12,9) CRC masks for voice header and terminator full link control, DMR AI spec.
    private static final int VOICE_HEADER_MASK = 0x96;
    private static final int TERMINATOR_MASK = 0x99;
    private static final int[] CHECKSUM_5 = {72, 73, 74, 75, 76};

    //DMR RS(12,9) GF(256) field and generator, used only to build valid test codewords.
    private static final int PRIMITIVE_POLYNOMIAL = 0x11D;
    private static final int[] GENERATOR = {0x40, 0x38, 0x0e, 0x01};
    private static final int[] EXP = new int[255];
    private static final int[] LOG = new int[256];

    static
    {
        int value = 1;

        for(int i = 0; i < 255; i++)
        {
            EXP[i] = value;
            LOG[value] = i;
            value <<= 1;

            if(value >= 256)
            {
                value ^= PRIMITIVE_POLYNOMIAL;
            }
        }
    }

    private static int multiply(int a, int b)
    {
        return (a == 0 || b == 0) ? 0 : EXP[(LOG[a] + LOG[b]) % 255];
    }

    /**
     * Systematic RS(12,9) parity for the 9 message symbols, matching RS_12_9_DMR.
     */
    private static int[] parity(int[] data)
    {
        int[] checksum = new int[3];

        for(int i = 0; i < 9; i++)
        {
            int feedback = data[i] ^ checksum[0];
            checksum[0] = checksum[1] ^ multiply(GENERATOR[2], feedback);
            checksum[1] = checksum[2] ^ multiply(GENERATOR[1], feedback);
            checksum[2] = multiply(GENERATOR[0], feedback);
        }

        return checksum;
    }

    /**
     * 96-bit full link control (voice header or terminator) with valid, masked RS(12,9) parity.
     */
    private static CorrectedBinaryMessage fullLc(int[] data, int mask)
    {
        int[] checksum = parity(data);
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(96);

        for(int i = 0; i < 9; i++)
        {
            message.setByte(i * 8, (byte)data[i]);
        }

        for(int i = 0; i < 3; i++)
        {
            message.setByte((9 + i) * 8, (byte)(checksum[i] ^ mask));
        }

        return message;
    }

    /**
     * 77-bit embedded full link control with a valid 5-bit checksum (TS 102 361-1 B.3.11).
     */
    private static CorrectedBinaryMessage embeddedLc(int[] data)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(77);
        int sum = 0;

        for(int i = 0; i < 9; i++)
        {
            message.setByte(i * 8, (byte)data[i]);
            sum += data[i];
        }

        message.setInt(sum % 31, CHECKSUM_5);
        return message;
    }

    //Standard group voice channel user: FLCO 0, FID 0, service options 0, group 0x001234, source 0x56789A
    private static int[] groupVoice()
    {
        return new int[]{0x00, 0x00, 0x00, 0x00, 0x12, 0x34, 0x56, 0x78, 0x9A};
    }

    private static FullLCMessage decode(CorrectedBinaryMessage message, boolean isTerminator)
    {
        return new LCMessageFactory(new DMRCrcMaskManager(false)).createFull(message, 0L, 0, isTerminator);
    }

    /**
     * Error-free link control is valid on the first decode: voice header and terminator (RS(12,9)) and embedded
     * (5-bit checksum).
     */
    @Test
    public void cleanLinkControlIsValid()
    {
        FullLCMessage header = decode(fullLc(groupVoice(), VOICE_HEADER_MASK), false);
        assertTrue(header.isValid(), "Clean voice header link control should be valid");
        assertInstanceOf(GroupVoiceChannelUser.class, header);

        FullLCMessage terminator = decode(fullLc(groupVoice(), TERMINATOR_MASK), true);
        assertTrue(terminator.isValid(), "Clean terminator link control should be valid");

        FullLCMessage embedded = decode(embeddedLc(groupVoice()), false);
        assertTrue(embedded.isValid(), "Clean embedded link control should be valid");
    }

    /**
     * Link control that fails error detection is invalid: three RS(12,9) symbol errors (beyond its single symbol
     * correction) and a 5-bit checksum mismatch.
     */
    @Test
    public void corruptLinkControlIsInvalid()
    {
        CorrectedBinaryMessage header = fullLc(groupVoice(), VOICE_HEADER_MASK);

        for(int symbol : new int[]{2, 5, 8})
        {
            header.setByte(symbol * 8, (byte)(header.getByte(symbol * 8) ^ 0xFF));
        }

        assertFalse(decode(header, false).isValid(), "Uncorrectable voice header link control should be invalid");

        CorrectedBinaryMessage embedded = embeddedLc(groupVoice());
        embedded.setByte(24, (byte)(embedded.getByte(24) ^ 0x01));
        assertFalse(decode(embedded, false).isValid(), "Embedded link control with a bad checksum should be invalid");
    }
}
