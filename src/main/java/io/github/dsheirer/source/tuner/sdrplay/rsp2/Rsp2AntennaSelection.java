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

package io.github.dsheirer.source.tuner.sdrplay.rsp2;

import io.github.dsheirer.sdrplay.Rsp2AmPort;
import io.github.dsheirer.sdrplay.Rsp2Antenna;

/**
 * RSP2 antenna selection: the antenna and AM port combination for each of the three RSP2 antenna connectors.  Entry
 * names are persisted in tuner configurations.
 */
public enum Rsp2AntennaSelection
{
    ANT_A(Rsp2Antenna.A, Rsp2AmPort.PORT_2, "ANT A"),
    ANT_B(Rsp2Antenna.B, Rsp2AmPort.PORT_2, "ANT B"),
    HIGH_Z(Rsp2Antenna.B, Rsp2AmPort.PORT_1, "HIGH Z");

    private final Rsp2Antenna mAntenna;
    private final Rsp2AmPort mAmPort;
    private final String mDescription;

    Rsp2AntennaSelection(Rsp2Antenna antenna, Rsp2AmPort amPort, String description)
    {
        mAntenna = antenna;
        mAmPort = amPort;
        mDescription = description;
    }

    /**
     * Antenna setting for the selection.
     */
    public Rsp2Antenna getAntenna()
    {
        return mAntenna;
    }

    /**
     * AM Port for the selection
     */
    public Rsp2AmPort getAmPort()
    {
        return mAmPort;
    }

    /**
     * Indicates if this selection uses the high impedance (Hi-Z) port.
     */
    public boolean isHighZ()
    {
        return mAmPort == Rsp2AmPort.PORT_1;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
