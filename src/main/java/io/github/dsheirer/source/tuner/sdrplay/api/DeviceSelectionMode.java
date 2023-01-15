/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay.api;

import io.github.dsheirer.source.tuner.sdrplay.api.device.RspDuoMode;
import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;
import java.util.EnumSet;

/**
 * Enumeration of modes that indicate how each SDRplay RSP device can be selected
 */
public enum DeviceSelectionMode
{
    //All RSP devices
    SINGLE_TUNER_1("Single Tuner 1", RspDuoMode.SINGLE_TUNER, TunerSelect.TUNER_1),
    SINGLE_TUNER_2("Single Tuner 2", RspDuoMode.SINGLE_TUNER, TunerSelect.TUNER_2),
    MASTER_TUNER_1("Master - Tuner 1", RspDuoMode.MASTER, TunerSelect.TUNER_1),
    SLAVE_TUNER_2("Slave - Tuner 2", RspDuoMode.SLAVE, TunerSelect.TUNER_2);

    private String mDescription;
    private RspDuoMode mRspDuoMode;
    private TunerSelect mTunerSelect;

    /**
     * Private constructor
     * @param description of the mode
     * @param rspDuoMode that corresponds to the mode
     * @param tunerSelect tuner(s) that correspond to the mode
     */
    DeviceSelectionMode(String description, RspDuoMode rspDuoMode, TunerSelect tunerSelect)
    {
        mDescription = description;
        mRspDuoMode = rspDuoMode;
        mTunerSelect = tunerSelect;
    }

    /**
     * Set of all selection modes available for the RSPduo
     */
    public static final EnumSet<DeviceSelectionMode> MASTER_MODES = EnumSet.of(MASTER_TUNER_1);
    public static final EnumSet<DeviceSelectionMode> SLAVE_MODES = EnumSet.of(SLAVE_TUNER_2);
    public static final EnumSet<DeviceSelectionMode> SINGLE_TUNER_MODES = EnumSet.of(SINGLE_TUNER_1, SINGLE_TUNER_2);

    /**
     * RSPduo mode associated with the selection mode
     */
    public RspDuoMode getRspDuoMode()
    {
        return mRspDuoMode;
    }

    /**
     * Indicates if this mode is designated as a master mode.
     */
    public boolean isMasterMode()
    {
        return MASTER_MODES.contains(this);
    }

    /**
     * Indicates if this mode is designated as a slave mode.
     */
    public boolean isSlaveMode()
    {
        return SLAVE_MODES.contains(this);
    }

    /**
     * Indicates if this is a single-tuner mode
     */
    public boolean isSingleTunerMode()
    {
        return SINGLE_TUNER_MODES.contains(this);
    }

    /**
     * Tuner(s) associated with the selection mode
     */
    public TunerSelect getTunerSelect()
    {
        return mTunerSelect;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
