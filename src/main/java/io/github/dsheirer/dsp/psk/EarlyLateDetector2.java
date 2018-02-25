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

public class EarlyLateDetector2
{
    //45 degrees rotation to orient the symbol to a polar axis to make error calculation easy/efficient
    public static final Complex POSITIVE_OFFSET = Complex.fromAngle(Math.PI / 4.0d);
    private Complex mSymbol = new Complex(0,0);

    public float getError(Complex symbol)
    {
        mSymbol.setValues(symbol);
        mSymbol.multiply(POSITIVE_OFFSET);

        if(Math.abs(mSymbol.quadrature()) > Math.abs(mSymbol.inphase()))
        {
            return mSymbol.inphase();
        }
        else
        {
            return mSymbol.quadrature();
        }
    }
}
