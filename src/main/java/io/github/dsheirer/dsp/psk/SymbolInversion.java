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
package io.github.dsheirer.dsp.psk;

import org.apache.commons.math3.util.FastMath;

public enum SymbolInversion
{
    DEGREES_90_PLUS(FastMath.PI / 2.0),
    DEGREES_90_MINUS(FastMath.PI / -2.0),
    DEGREES_180(FastMath.PI);

    private double mAngleRadians;

    SymbolInversion(double angleRadians)
    {
        mAngleRadians = angleRadians;
    }

    public double getError()
    {
        return mAngleRadians;
    }
}
