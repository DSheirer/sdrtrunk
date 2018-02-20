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

import io.github.dsheirer.sample.complex.Complex;

public class DQPSKSymbolPhaseErrorCalculator extends QPSKSymbolPhaseErrorCalculator
{
    //Negative 45 degrees rotation to offset for differential additive 45 degrees of rotation
    public static final Complex DIFFERENTIAL_OFFSET = Complex.fromAngle(Math.PI / 4.0d);

    @Override
    public void adjust(Complex symbol)
    {
        symbol.multiply(DIFFERENTIAL_OFFSET);
    }

    @Override
    public float getPhaseError(Complex symbol)
    {
        Complex copy = symbol.copy();

        copy.multiply(DIFFERENTIAL_OFFSET);
        return super.getPhaseError(copy);
    }
}
