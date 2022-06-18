/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package com.github.dsheirer.sdrplay.async;

import com.github.dsheirer.sdrplay.UpdateReason;
import com.github.dsheirer.sdrplay.device.TunerSelect;

/**
 * Completed asynchronous update operation
 */
public class CompletedAsyncUpdate
{
    private TunerSelect mTunerSelect;
    private UpdateReason mUpdateReason;

    /**
     * Creates a completed update.
     */
    public CompletedAsyncUpdate(TunerSelect tunerSelect, UpdateReason updateReason)
    {
        mTunerSelect = tunerSelect;
        mUpdateReason = updateReason;
    }

    /**
     * Tuner(s) for the update
     */
    public TunerSelect getTunerSelect()
    {
        return mTunerSelect;
    }

    /**
     * UpdateReason that is being updated
     */
    public UpdateReason getUpdateReason()
    {
        return mUpdateReason;
    }
}
