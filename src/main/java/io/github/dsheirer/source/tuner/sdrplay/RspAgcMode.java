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

import io.github.dsheirer.sdrplay.AgcMode;

/**
 * IF AGC mode for RSP tuners.  Entry names are persisted in tuner configurations.
 */
public enum RspAgcMode
{
    DISABLE(AgcMode.DISABLED, "DISABLE"),
    ENABLE(AgcMode.CONTROL_ENABLED, "ENABLE");

    private final AgcMode mAgcMode;
    private final String mDescription;

    RspAgcMode(AgcMode agcMode, String description)
    {
        mAgcMode = agcMode;
        mDescription = description;
    }

    /**
     * SDRplay API AGC mode for this entry
     */
    public AgcMode getAgcMode()
    {
        return mAgcMode;
    }

    /**
     * Lookup the entry that matches the SDRplay API AGC mode.
     * @param agcMode from the API
     * @return ENABLE for any of the enabled AGC modes, or DISABLE
     */
    public static RspAgcMode fromAgcMode(AgcMode agcMode)
    {
        return agcMode == null || agcMode == AgcMode.DISABLED ? DISABLE : ENABLE;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
