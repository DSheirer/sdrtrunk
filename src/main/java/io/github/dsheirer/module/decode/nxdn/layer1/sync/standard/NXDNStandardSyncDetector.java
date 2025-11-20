/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer1.sync.standard;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.NXDNSyncDetector;

/**
 * NXDN sync detector for the standard sync pattern.
 */
public abstract class NXDNStandardSyncDetector extends NXDNSyncDetector
{
    public static final long STANDARD_SYNC_PATTERN = 0xCDF59;
    public static final long STANDARD_SYNC_MASK = 0xFFFFF;
    public static final int STANDARD_SYNC_DIBIT_LENGTH = 10;
    private static final Dibit[] DIBITS = toDibits(STANDARD_SYNC_PATTERN, STANDARD_SYNC_DIBIT_LENGTH);
    protected static final float[] SYMBOLS = toSymbols(DIBITS);

    @Override
    public int getSyncPatternDibitLength()
    {
        return STANDARD_SYNC_DIBIT_LENGTH;
    }

    @Override
    public Dibit[] getSyncDibits()
    {
        return DIBITS;
    }

    @Override
    public float[] getSyncSymbols()
    {
        return SYMBOLS;
    }
}
