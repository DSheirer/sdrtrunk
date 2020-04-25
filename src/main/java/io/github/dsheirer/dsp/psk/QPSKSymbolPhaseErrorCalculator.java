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
import org.apache.commons.math3.util.FastMath;

/**
 * Calculates the phase error for a QPSK polar-orientation constellation
 */
public class QPSKSymbolPhaseErrorCalculator implements ISymbolPhaseErrorCalculator
{
    private float mPhaseError;

    @Override
    public float getPhaseError(Complex symbol)
    {
        mPhaseError = 0.0f;

        if(FastMath.abs(symbol.inphase()) > FastMath.abs(symbol.quadrature()))
        {
            if(symbol.inphase() > 0)
            {
                mPhaseError = -symbol.quadrature();
            }
            else
            {
                mPhaseError = symbol.quadrature();
            }
        }
        else
        {
            if(symbol.quadrature() > 0)
            {
                mPhaseError = symbol.inphase();
            }
            else
            {
                mPhaseError = -symbol.inphase();
            }
        }

        return mPhaseError;
    }
}
