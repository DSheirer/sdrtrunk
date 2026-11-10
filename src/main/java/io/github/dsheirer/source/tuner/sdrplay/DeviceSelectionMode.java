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

package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.sdrplay.RspDuoMode;
import io.github.dsheirer.sdrplay.Tuner;

/**
 * RSP device selection mode.  All RSP devices use single tuner 1, except the RSPduo which can use either tuner as a
 * single tuner, or both tuners with tuner 1 as master and tuner 2 as slave.
 */
public enum DeviceSelectionMode
{
    SINGLE_TUNER_1("Single Tuner 1", RspDuoMode.SINGLE_TUNER, Tuner.A),
    SINGLE_TUNER_2("Single Tuner 2", RspDuoMode.SINGLE_TUNER, Tuner.B),
    MASTER_TUNER_1("Master - Tuner 1", RspDuoMode.MASTER, Tuner.A),
    SLAVE_TUNER_2("Slave - Tuner 2", RspDuoMode.SLAVE, Tuner.B);

    private final String mDescription;
    private final RspDuoMode mRspDuoMode;
    private final Tuner mTuner;

    DeviceSelectionMode(String description, RspDuoMode rspDuoMode, Tuner tuner)
    {
        mDescription = description;
        mRspDuoMode = rspDuoMode;
        mTuner = tuner;
    }

    /**
     * RSPduo mode associated with the selection mode
     */
    public RspDuoMode getRspDuoMode()
    {
        return mRspDuoMode;
    }

    /**
     * Tuner associated with the selection mode
     */
    public Tuner getTuner()
    {
        return mTuner;
    }

    /**
     * Indicates if this mode is designated as a master mode.
     */
    public boolean isMasterMode()
    {
        return this == MASTER_TUNER_1;
    }

    /**
     * Indicates if this mode is designated as a slave mode.
     */
    public boolean isSlaveMode()
    {
        return this == SLAVE_TUNER_2;
    }

    /**
     * Indicates if this is a single-tuner mode
     */
    public boolean isSingleTunerMode()
    {
        return this == SINGLE_TUNER_1 || this == SINGLE_TUNER_2;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
