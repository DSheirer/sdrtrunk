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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner;

/**
 * RSP-2 Antenna
 */
public enum Rsp2AntennaSelection
{
    ANT_A(Rsp2Antenna.ANTENNA_A, Rsp2AmPort.PORT_2_SMA,"ANT A"),
    ANT_B(Rsp2Antenna.ANTENNA_B, Rsp2AmPort.PORT_2_SMA, "ANT B"),
    HIGH_Z(Rsp2Antenna.ANTENNA_B, Rsp2AmPort.PORT_1_HIGH_Z, "HIGH Z");

    private Rsp2Antenna mAntenna;
    private Rsp2AmPort mAmPort;
    private String mDescription;

    Rsp2AntennaSelection(Rsp2Antenna antenna, Rsp2AmPort amPort, String description)
    {
        mAntenna = antenna;
        mAmPort = amPort;
        mDescription = description;
    }

    /**
     * Antenna setting for the selection.
     * @return antenna
     */
    public Rsp2Antenna getAntenna()
    {
        return mAntenna;
    }

    /**
     * AM Port for the selection
     * @return am port
     */
    public Rsp2AmPort getAmPort()
    {
        return mAmPort;
    }

    @Override
    public String toString()
    {
        return mDescription;
    }
}
