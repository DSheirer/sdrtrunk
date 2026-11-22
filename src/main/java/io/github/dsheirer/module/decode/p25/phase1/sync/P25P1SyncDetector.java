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

package io.github.dsheirer.module.decode.p25.phase1.sync;

import io.github.dsheirer.dsp.c4fm.DibitSyncDetector;

/**
 * Base implementation of P25 Phase 1 sync detector.
 */
public abstract class P25P1SyncDetector extends DibitSyncDetector
{
    /**
     * P25 Phase 1 sync pattern: +33333 -3 +33 -33 +33 -3333 +3 -3 +3 -33333
     */
    public static final long SYNC_PATTERN = 0x5575F5FF77FFL;

    /**
     * Constructs an instance
     */
    public P25P1SyncDetector()
    {
        super(SYNC_PATTERN, 24);
    }
}
