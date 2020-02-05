/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.dsp.symbol.ISyncDetectListener;

/**
 * Listener interface to be notified each time a sync pattern has been detected and/or when the sync
 * has been lost.
 */
public interface IDMRSyncDetectListener extends ISyncDetectListener
{
    /**
     * Indicates that a sync pattern has been detected.
     *
     * @param bitErrors count for soft sync matching to indicate the number of bit positions
     * of the sequence that didn't fully match the sync pattern
     */
    void syncDetected(int bitErrors, DMRSyncPattern pattern);

    /**
     * Indicates that sync has been lost
     */
    void syncLost(int Processed);
}
