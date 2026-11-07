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

package io.github.dsheirer.source.tuner.sdrplay.rspDx;

import io.github.dsheirer.sdrplay.RspDxAntenna;

/**
 * RSPdx antenna selection.  Entry names are persisted in tuner configurations.
 */
public enum RspDxAntennaSelection
{
    ANTENNA_A(RspDxAntenna.A, "ANT A (1 kHz to 2 GHz)"),
    ANTENNA_B(RspDxAntenna.B, "ANT B (1 kHz to 2 GHz)"),
    ANTENNA_C(RspDxAntenna.C, "ANT C (1 kHz to 200 MHz)");

    private final RspDxAntenna mAntenna;
    private final String mDescription;

    RspDxAntennaSelection(RspDxAntenna antenna, String description)
    {
        mAntenna = antenna;
        mDescription = description;
    }

    /**
     * SDRplay API antenna for this entry
     */
    public RspDxAntenna getAntenna()
    {
        return mAntenna;
    }

    /**
     * Lookup the entry that matches the SDRplay API antenna
     */
    public static RspDxAntennaSelection fromAntenna(RspDxAntenna antenna)
    {
        for(RspDxAntennaSelection selection: values())
        {
            if(selection.getAntenna() == antenna)
            {
                return selection;
            }
        }

        return ANTENNA_A;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
