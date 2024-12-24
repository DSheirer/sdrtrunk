/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.sync;

/**
 * Base implementation of DMR sync detector that uses sync detection modes to activate or deactivate groups of DMR
 * sync patterns.  The default mode is AUTOMATIC to monitor for all sync patterns.  Additional modes can be used to
 * limit sync monitoring for smaller groups of sync patterns for base station, mobile stations and direct mode.
 */
public abstract class DMRSyncDetector
{
    protected DMRSyncDetectMode mMode = DMRSyncDetectMode.AUTOMATIC;
    protected DMRSyncPattern mDetectedPattern = DMRSyncPattern.BASE_STATION_DATA;

    /**
     * Sets the operating mode for this detector.
     * @param mode of sync detection.
     */
    public void setMode(DMRSyncDetectMode mode)
    {
        mMode = mode;
    }

    /**
     * Current mode of operation
     */
    public DMRSyncDetectMode getMode()
    {
        return mMode;
    }
}
