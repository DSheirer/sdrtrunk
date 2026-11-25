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

package io.github.dsheirer.gui.playlist.radioreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.dsheirer.module.decode.squelch.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.squelch.dcs.DCSCode;
import org.junit.jupiter.api.Test;

public class RadioReferenceToneTest
{
    @Test
    public void ctcss()
    {
        assertEquals(CTCSSCode.TONE_1188, RadioReferenceTone.parse("118.8 PL").ctcssCode());
        assertEquals(CTCSSCode.TONE_1188, RadioReferenceTone.parse("118.8").ctcssCode());
        assertEquals(CTCSSCode.TONE_1000, RadioReferenceTone.parse("100.0 PL").ctcssCode());
        assertEquals(CTCSSCode.TONE_1000, RadioReferenceTone.parse("100 PL").ctcssCode());
        assertEquals(CTCSSCode.TONE_948, RadioReferenceTone.parse(" 94.8 pl ").ctcssCode());
        assertNull(RadioReferenceTone.parse("118.0 PL"));
    }

    @Test
    public void dcs()
    {
        assertEquals(DCSCode.N023, RadioReferenceTone.parse("023 DPL").dcsCode());
        assertEquals(DCSCode.N023, RadioReferenceTone.parse("D023N").dcsCode());
        assertEquals(DCSCode.N023, RadioReferenceTone.parse("D023 N").dcsCode());
        assertEquals(DCSCode.N411, RadioReferenceTone.parse("411 DPL").dcsCode());
        assertEquals(DCSCode.I754, RadioReferenceTone.parse("D754I").dcsCode());
        assertEquals(DCSCode.N754, RadioReferenceTone.parse("DPL 754").dcsCode());
        assertNull(RadioReferenceTone.parse("024 DPL"));
    }

    @Test
    public void accessCodes()
    {
        assertEquals(0x293, RadioReferenceTone.parse("NAC 293").accessCode());
        assertEquals(0x293, RadioReferenceTone.parse("$293").accessCode());
        assertEquals(0xF7E, RadioReferenceTone.parse("NAC F7E").accessCode());
        assertEquals(RadioReferenceTone.Type.NAC, RadioReferenceTone.parse("293 NAC").type());
        assertEquals(1, RadioReferenceTone.parse("CC1").accessCode());
        assertEquals(RadioReferenceTone.Type.COLOR_CODE, RadioReferenceTone.parse("CC 12").type());
        assertEquals(3, RadioReferenceTone.parse("CC 3 TG 300 SL 1").accessCode());
        assertEquals(1, RadioReferenceTone.parse("CC1 TG 1 TS 2").accessCode());
        assertEquals(5, RadioReferenceTone.parse("RAN 5 TG 100").accessCode());
        assertEquals(1, RadioReferenceTone.parse("RAN 1").accessCode());
        assertEquals(RadioReferenceTone.Type.RAN, RadioReferenceTone.parse("RAN1").type());
        assertEquals(13, RadioReferenceTone.parse("13 RAN").accessCode());
        assertEquals(RadioReferenceTone.Type.RAN, RadioReferenceTone.parse("11 RAN").type());
        assertEquals(7, RadioReferenceTone.parse("RAN-7").accessCode());
        assertEquals(2, RadioReferenceTone.parse("RAN: 2").accessCode());
        assertEquals(4, RadioReferenceTone.parse("4 RAN TG 0").accessCode());
        assertEquals(1, RadioReferenceTone.parse("1 CC").accessCode());
        assertEquals(RadioReferenceTone.Type.COLOR_CODE, RadioReferenceTone.parse("CC-3").type());
    }

    @Test
    public void noTone()
    {
        assertNull(RadioReferenceTone.parse(null));
        assertNull(RadioReferenceTone.parse(""));
        assertNull(RadioReferenceTone.parse("CSQ"));
        assertNull(RadioReferenceTone.parse("100.0 PL / 136.5 PL"));
        assertNull(RadioReferenceTone.parse("123"));
        assertNull(RadioReferenceTone.parse("CC 16"));
    }
}
