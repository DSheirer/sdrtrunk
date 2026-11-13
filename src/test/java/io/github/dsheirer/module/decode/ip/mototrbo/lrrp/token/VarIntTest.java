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
package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit tests for the LRRP 7-bit variable length integer reader.
 */
public class VarIntTest
{
    private static CorrectedBinaryMessage hex(String hex)
    {
        return CorrectedBinaryMessage.loadHex(hex);
    }

    @Test
    public void singleByteValue()
    {
        CorrectedBinaryMessage message = hex("64");
        assertEquals(1, VarInt.length(message, 0, 10));
        assertEquals(100, VarInt.value(message, 0, 10));
    }

    @Test
    public void twoByteValues()
    {
        //Values captured from Motorola triggered location start requests (interval 600 s, unknown parameter 3600)
        assertEquals(2, VarInt.length(hex("8458"), 0, 10));
        assertEquals(600, VarInt.value(hex("8458"), 0, 10));
        assertEquals(2, VarInt.length(hex("9C10"), 0, 10));
        assertEquals(3600, VarInt.value(hex("9C10"), 0, 10));

        //Values from the TRBONet captures quoted in node-dmr-lib LRRP.js: interval 250 s and distance 500 m
        assertEquals(250, VarInt.value(hex("817A"), 0, 10));
        assertEquals(500, VarInt.value(hex("8374"), 0, 10));
    }

    @Test
    public void valueAtOffset()
    {
        //Token identifier 0x31 followed by the value - read starting at bit 8
        CorrectedBinaryMessage message = hex("318458");
        assertEquals(2, VarInt.length(message, 8, 2));
        assertEquals(600, VarInt.value(message, 8, 2));
    }

    @Test
    public void limitBoundsTheRead()
    {
        CorrectedBinaryMessage message = hex("8458");

        //Only one byte available: the continuation bit is set but the read stops at the limit
        assertEquals(1, VarInt.length(message, 0, 1));
        assertEquals(4, VarInt.value(message, 0, 1));

        //No bytes available
        assertEquals(0, VarInt.length(message, 0, 0));
        assertEquals(0, VarInt.value(message, 0, 0));
    }

    @Test
    public void maximumLength()
    {
        //A run of continuation bits longer than MAX_BYTES stops at MAX_BYTES
        CorrectedBinaryMessage message = hex("FFFFFFFFFFFFFF");
        assertEquals(VarInt.MAX_BYTES, VarInt.length(message, 0, 10));
    }
}
