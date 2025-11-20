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

package io.github.dsheirer.module.decode.nxdn.sync.control;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.nxdn.sync.NXDNSyncDetector;

/**
 * NXDN sync detector for the extended sync pattern used on the outbound control channel
 */
public class NXDNControlSyncDetector extends NXDNSyncDetector
{
    public static final long CONTROL_SYNC_PATTERN = 0x5775FDCDF59L;
    public static final long CONTROL_SYNC_MASK = 0xFFFFFFFFFFFL;
    public static final int CONTROL_SYNC_DIBIT_LENGTH = 22;
    protected static final Dibit[] DIBITS = toDibits(CONTROL_SYNC_PATTERN, CONTROL_SYNC_DIBIT_LENGTH);
    protected static final float[] SYMBOLS = toSymbols(DIBITS);

    @Override
    public int getSyncPatternDibitLength()
    {
        return CONTROL_SYNC_DIBIT_LENGTH;
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
