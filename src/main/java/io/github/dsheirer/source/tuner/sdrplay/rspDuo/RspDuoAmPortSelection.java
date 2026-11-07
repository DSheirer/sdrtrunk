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

package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import io.github.dsheirer.sdrplay.RspDuoAmPort;

/**
 * RSPduo tuner 1 AM port selection, used below 60 MHz.  Entry names are persisted in tuner configurations.
 */
public enum RspDuoAmPortSelection
{
    PORT_1(RspDuoAmPort.PORT_1, "HIGH Z"),
    PORT_2(RspDuoAmPort.PORT_2, "50Ω SMA");

    private final RspDuoAmPort mAmPort;
    private final String mDescription;

    RspDuoAmPortSelection(RspDuoAmPort amPort, String description)
    {
        mAmPort = amPort;
        mDescription = description;
    }

    /**
     * SDRplay API AM port for this entry
     */
    public RspDuoAmPort getAmPort()
    {
        return mAmPort;
    }

    /**
     * Lookup the entry that matches the SDRplay API AM port
     */
    public static RspDuoAmPortSelection fromAmPort(RspDuoAmPort amPort)
    {
        return amPort == RspDuoAmPort.PORT_1 ? PORT_1 : PORT_2;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
