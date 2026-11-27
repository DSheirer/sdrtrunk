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
package io.github.dsheirer.module.decode.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.nxdn.DecodeConfigNXDN;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AccessCodeFilterTest
{
    private static AccessCodeFilter filter(AccessCodeFilter.Format format, AccessCodeFilter.Mode mode, Integer... values)
    {
        AccessCodeFilter filter = new AccessCodeFilter(format);
        filter.setEnabled(true);
        filter.setMode(mode);
        filter.setValueSet(Set.of(values));
        return filter;
    }

    @Test
    public void toStringMarksHexValues()
    {
        assertEquals("Access Code Filter: Include [0x100, 0x293]",
            filter(AccessCodeFilter.Format.HEX, AccessCodeFilter.Mode.INCLUDE, 0x100, 0x293).toString());
        assertEquals("Access Code Filter: Exclude [1, 2]",
            filter(AccessCodeFilter.Format.DECIMAL, AccessCodeFilter.Mode.EXCLUDE, 1, 2).toString());
    }

    @Test
    public void copiedChannelKeepsFilters()
    {
        DecodeConfigP25Phase1 p25 = new DecodeConfigP25Phase1();
        p25.setNacFilter(filter(AccessCodeFilter.Format.HEX, AccessCodeFilter.Mode.EXCLUDE, 0x293));
        DecodeConfigP25Phase1 p25Copy = (DecodeConfigP25Phase1)DecoderFactory.copy(p25);
        assertEquals(p25.getNacFilter().toString(), p25Copy.getNacFilter().toString());
        assertNotSame(p25.getNacFilter(), p25Copy.getNacFilter());

        DecodeConfigDMR dmr = new DecodeConfigDMR();
        dmr.setColorCodeFilter(filter(AccessCodeFilter.Format.DECIMAL, AccessCodeFilter.Mode.INCLUDE, 1));
        DecodeConfigDMR dmrCopy = (DecodeConfigDMR)DecoderFactory.copy(dmr);
        assertEquals(dmr.getColorCodeFilter().toString(), dmrCopy.getColorCodeFilter().toString());

        DecodeConfigNXDN nxdn = new DecodeConfigNXDN();
        nxdn.setRanFilter(filter(AccessCodeFilter.Format.DECIMAL, AccessCodeFilter.Mode.INCLUDE, 13));
        DecodeConfigNXDN nxdnCopy = (DecodeConfigNXDN)DecoderFactory.copy(nxdn);
        assertEquals(nxdn.getRanFilter().toString(), nxdnCopy.getRanFilter().toString());
    }
}
