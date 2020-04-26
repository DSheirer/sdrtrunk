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

public class GardnerDetector
{
    private float mErrorInphase;
    private float mErrorQuadrature;
    private float mError;

    /**
     * Gardner detector for recovering symbol timing error.
     */
    public GardnerDetector()
    {
    }


    /**
     * Calculates the symbol timing error by evaluating the previous, middle and current symbol instants.
     *
     * @param previous symbol
     * @param middle symbol (ie midway between the previous and current)
     * @param current symbol
     * @return symbol timing error
     */
    public float getError(Complex previous, Complex middle, Complex current)
    {
        mErrorInphase = (FastMath.abs(previous.inphase()) - FastMath.abs(current.inphase())) * FastMath.abs(middle.inphase());
        mErrorQuadrature = (FastMath.abs(previous.quadrature()) - FastMath.abs(current.quadrature())) * FastMath.abs(middle.quadrature());
        mError = normalize(mErrorInphase + mErrorQuadrature, 1.0f);

        return mError;
    }

    /**
     * Constrains timing error to +/- the maximum value and corrects any floating point invalid numbers
     */
    public static float normalize(float error, float maximum)
    {
        if(Float.isNaN(error))
        {
            return 0.0f;
        }
        else
        {
            return clip(error, maximum);
        }
    }

    /**
     * Constrains value to the range of ( -maximum <> maximum )
     */
    public static float clip(float value, float maximum)
    {
        if(value > maximum)
        {
            return maximum;
        }
        else if(value < -maximum)
        {
            return -maximum;
        }

        return value;
    }
}
